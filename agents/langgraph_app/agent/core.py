"""Campus assistant agent powered by LangChain create_agent."""
from __future__ import annotations

from typing import Any, AsyncIterator

from langchain.agents import create_agent
from langchain_openai import ChatOpenAI
from sqlalchemy.orm import Session

from agent.prompts import build_system_prompt
from agent.tools import ToolBundle, build_tools
from agent.types import AgentInvokeResult, AgentStreamEvent, IntentType
from services.llm_settings import LLMSettingsService


class CampusAssistantAgent:
    def __init__(self, db: Session, access_token: str):
        self.db = db
        self.access_token = access_token
        self.model = self._build_model()

    def _build_model(self) -> ChatOpenAI:
        cfg = LLMSettingsService.get_runtime_llm_config(self.db)
        if not cfg:
            raise RuntimeError("LLM 配置未就绪，请先在系统设置中配置可用模型")
        return ChatOpenAI(
            api_key=cfg.api_key,
            base_url=cfg.base_url,
            model=cfg.model,
            temperature=cfg.temperature,
        )

    @staticmethod
    def classify_intent(query: str) -> IntentType:
        q = query.lower()
        has_appointment = any(k in q for k in ["挂号", "预约", "医生", "科室", "就诊"])
        has_medicine = any(k in q for k in ["买药", "购药", "药", "药品", "购买"])

        if has_appointment and has_medicine:
            return "appointment_and_medicine"
        if has_appointment:
            return "appointment_only"
        if has_medicine:
            return "medicine_only"
        return "diagnosis_only"

    @staticmethod
    def _truncate_text(value: str, limit: int) -> str:
        text = value.strip()
        if len(text) <= limit:
            return text
        return f"{text[:limit]}..."

    def _format_history_item(self, item: dict) -> str:
        role = str(item.get("role") or "")
        role_text = "用户" if role == "user" else ("助手" if role == "assistant" else "操作")
        text_value = self._truncate_text(str(item.get("text") or ""), 300)

        parts: list[str] = []
        if text_value:
            parts.append(text_value)

        cards = item.get("cards")
        if isinstance(cards, dict):
            doctor_cards = cards.get("doctor_cards")
            medicine_cards = cards.get("medicine_cards")
            card_parts: list[str] = []
            if isinstance(doctor_cards, list) and doctor_cards:
                card_parts.append(f"医生建议{len(doctor_cards)}条")
            if isinstance(medicine_cards, list) and medicine_cards:
                card_parts.append(f"药品建议{len(medicine_cards)}条")
            if card_parts:
                parts.append("；".join(card_parts))

        action_payload = item.get("action_payload")
        if isinstance(action_payload, dict):
            action_type = action_payload.get("action_type")
            if action_type == "confirm_appointment":
                parts.append("动作: 挂号执行")
            elif action_type == "create_medicine_order":
                parts.append("动作: 购药下单执行")

        if not parts:
            return ""
        return f"{role_text}: {'；'.join(parts)}"

    def _build_messages(self, user_input: str, chat_history: list[dict] | None) -> list[dict]:
        messages: list[dict] = []

        if chat_history:
            for item in chat_history[-20:]:
                line = self._format_history_item(item)
                if not line:
                    continue
                role = str(item.get("role") or "")
                if role == "user":
                    messages.append({"role": "user", "content": line})
                else:
                    messages.append({"role": "assistant", "content": line})

        messages.append({"role": "user", "content": user_input})
        return messages

    @staticmethod
    def _extract_text_from_content(content: Any) -> str:
        if isinstance(content, str):
            return content

        if isinstance(content, list):
            parts: list[str] = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                    continue
                if isinstance(item, dict):
                    if item.get("type") == "text" and isinstance(item.get("text"), str):
                        parts.append(item["text"])
                        continue
                    if isinstance(item.get("content"), str):
                        parts.append(item["content"])
                        continue
            return "".join(parts)

        if hasattr(content, "text") and isinstance(getattr(content, "text"), str):
            return str(getattr(content, "text"))

        return ""

    def _extract_token_text(self, event: dict[str, Any]) -> str:
        data = event.get("data")
        if not isinstance(data, dict):
            return ""

        chunk = data.get("chunk")
        if chunk is None:
            return ""

        if hasattr(chunk, "content"):
            return self._extract_text_from_content(getattr(chunk, "content"))
        return self._extract_text_from_content(chunk)

    def _extract_final_answer_from_output(self, output: Any) -> str:
        if isinstance(output, str):
            return output.strip()

        if isinstance(output, dict):
            messages = output.get("messages")
            if isinstance(messages, list):
                for message in reversed(messages):
                    text_value = self._extract_final_answer_from_output(message)
                    if text_value:
                        return text_value

            final_answer = output.get("output") or output.get("final_output")
            if isinstance(final_answer, str) and final_answer.strip():
                return final_answer.strip()
            if isinstance(final_answer, dict):
                text_value = self._extract_final_answer_from_output(final_answer)
                if text_value:
                    return text_value

            content = output.get("content")
            text_value = self._extract_text_from_content(content)
            if text_value.strip():
                return text_value.strip()
            return ""

        if hasattr(output, "content"):
            text_value = self._extract_text_from_content(getattr(output, "content"))
            return text_value.strip()

        return ""

    @staticmethod
    def _build_reasoning_summary(
        intent: IntentType,
        answer: str,
        doctor_cards: list[dict],
        medicine_cards: list[dict],
    ) -> str:
        if len(answer) <= 120:
            return answer

        if intent == "diagnosis_only":
            return "本轮主要进行问诊答疑，未触发挂号或购药工具。"
        if intent == "appointment_only":
            return f"已结合医生简介与号源信息推荐 {len(doctor_cards)} 位候选医生。"
        if intent == "medicine_only":
            return f"已结合库存与启用状态推荐 {len(medicine_cards)} 个候选药品。"
        return (
            f"已综合输出 {len(doctor_cards)} 位医生候选与 "
            f"{len(medicine_cards)} 个药品候选，可分别执行挂号与购药。"
        )

    @staticmethod
    def _can_emit_token(intent: IntentType, bundle: ToolBundle) -> bool:
        if intent == "diagnosis_only":
            return True
        return bool(bundle.called_tools)

    async def stream_reply(
        self,
        *,
        user_input: str,
        chat_history: list[dict] | None = None,
    ) -> AsyncIterator[AgentStreamEvent]:
        intent = self.classify_intent(user_input)
        tool_bundle = build_tools(self.db, self.access_token, intent)
        system_prompt = build_system_prompt(intent)
        agent = create_agent(
            model=self.model,
            tools=tool_bundle.tools,
            system_prompt=system_prompt,
            name="campus_assistant",
        )

        input_payload = {"messages": self._build_messages(user_input=user_input, chat_history=chat_history)}

        emitted_tokens: list[str] = []
        final_output_candidate: str = ""

        async for event in agent.astream_events(input_payload, version="v2"):
            event_name = str(event.get("event") or "")

            if event_name == "on_chat_model_stream":
                token_text = self._extract_token_text(event)
                if token_text and self._can_emit_token(intent, tool_bundle):
                    emitted_tokens.append(token_text)
                    yield {"type": "token", "text": token_text}
                continue

            if event_name == "on_chain_end":
                data = event.get("data")
                if isinstance(data, dict):
                    candidate = self._extract_final_answer_from_output(data.get("output"))
                    if candidate:
                        final_output_candidate = candidate

        answer = "".join(emitted_tokens).strip() or final_output_candidate.strip()
        if not answer:
            raise RuntimeError("模型未返回可用文本，请重试")

        result: AgentInvokeResult = {
            "intent": intent,
            "answer": answer,
            "reasoning_summary": self._build_reasoning_summary(
                intent=intent,
                answer=answer,
                doctor_cards=tool_bundle.doctor_cards,
                medicine_cards=tool_bundle.medicine_cards,
            ),
            "doctor_cards": tool_bundle.doctor_cards,
            "medicine_cards": tool_bundle.medicine_cards,
        }
        yield {"type": "state", "result": result}
