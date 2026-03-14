"""LangGraph-based assistant orchestration."""
from __future__ import annotations

import json
from datetime import date, timedelta
from typing import Literal, TypedDict

import httpx
from langchain_openai import ChatOpenAI
from langgraph.graph import END, START, StateGraph
from sqlalchemy import text
from sqlalchemy.orm import Session

from core.config import settings
from services.llm_settings import LLMSettingsService

IntentType = Literal[
    "diagnosis_only",
    "appointment_only",
    "medicine_only",
    "appointment_and_medicine",
]


class AgentState(TypedDict):
    user_query: str
    intent: IntentType
    doctor_cards: list[dict]
    medicine_cards: list[dict]
    answer: str
    reasoning_summary: str


SYMPTOM_DEPT_HINTS = {
    "咽": "耳鼻喉科",
    "鼻": "耳鼻喉科",
    "耳": "耳鼻喉科",
    "牙": "口腔科",
    "胃": "内科",
    "腹": "内科",
    "发热": "内科",
    "发烧": "内科",
    "胸": "内科",
    "眼": "眼科",
}


class AgentGraphService:
    def __init__(self, db: Session, access_token: str):
        self.db = db
        self.access_token = access_token
        self.model = self._build_model()

    def _build_model(self) -> ChatOpenAI | None:
        cfg = LLMSettingsService.get_runtime_llm_config(self.db)
        if not cfg:
            return None
        return ChatOpenAI(
            api_key=cfg.api_key,
            base_url=cfg.base_url,
            model=cfg.model,
            temperature=cfg.temperature,
        )

    @staticmethod
    def _classify_intent(query: str) -> IntentType:
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
    def _extract_dept_hint(query: str) -> str | None:
        for token, dept in SYMPTOM_DEPT_HINTS.items():
            if token in query:
                return dept
        return None

    def _query_doctors(self, query: str, limit: int = 3) -> list[dict]:
        dept_hint = self._extract_dept_hint(query)
        params: dict[str, object] = {"limit": limit}

        where_sql = "u.is_active = 1"
        if dept_hint:
            where_sql += " AND d.name = :dept_hint"
            params["dept_hint"] = dept_hint

        rows = self.db.execute(
            text(
                f"""
                SELECT doc.id AS doctor_id,
                       u.full_name AS doctor_name,
                       d.name AS department,
                       doc.title,
                       doc.introduction
                FROM doctors doc
                JOIN users u ON u.id = doc.user_id
                LEFT JOIN departments d ON d.id = doc.department_id
                WHERE {where_sql}
                ORDER BY doc.id DESC
                LIMIT :limit
                """
            ),
            params,
        ).mappings().all()

        result = []
        for row in rows:
            result.append(
                {
                    "doctor_id": int(row["doctor_id"]),
                    "doctor_name": row["doctor_name"] or "",
                    "department": row["department"] or "未知科室",
                    "title": row["title"] or "医师",
                    "introduction": row["introduction"] or "暂无简介",
                    "recommend_reason": "根据症状关键词与科室匹配推荐",
                    "slot_candidates": self._query_slot_candidates(int(row["doctor_id"])),
                }
            )
        return result

    def _query_slot_candidates(self, doctor_id: int) -> list[dict]:
        today = date.today()
        date_from = today.isoformat()
        date_to = (today + timedelta(days=7)).isoformat()
        headers = {"Authorization": f"Bearer {self.access_token}"}
        params = {"doctor_id": doctor_id, "date_from": date_from, "date_to": date_to}

        try:
            with httpx.Client(timeout=15) as client:
                resp = client.get(
                    f"{settings.BACKEND_API_BASE_URL.rstrip('/')}/appointments/slots",
                    params=params,
                    headers=headers,
                )
            if resp.status_code >= 400:
                return []
            body = resp.json()
            slots = body.get("data", body)
            available = [s for s in slots if s.get("status") == "available"]
            return [
                {
                    "date": item.get("date"),
                    "time": item.get("time"),
                    "capacity": int(item.get("capacity") or 0),
                    "booked_count": int(item.get("booked_count") or 0),
                }
                for item in available[:3]
            ]
        except Exception:
            return []

    def _query_medicines(self, query: str, limit: int = 5) -> list[dict]:
        rows = self.db.execute(
            text(
                """
                SELECT id, name, spec, unit, stock, price
                FROM medicines
                WHERE is_active = 1 AND stock > 0
                ORDER BY stock DESC, id DESC
                LIMIT :limit
                """
            ),
            {"limit": limit},
        ).mappings().all()

        results = []
        for row in rows:
            stock = int(row["stock"] or 0)
            results.append(
                {
                    "medicine_id": int(row["id"]),
                    "name": row["name"],
                    "spec": row["spec"],
                    "unit": row["unit"],
                    "price": float(row["price"] or 0),
                    "stock": stock,
                    "default_quantity": 1,
                    "max_quantity": stock,
                    "recommend_reason": "结合症状与常备药可及性推荐",
                }
            )
        return results

    @staticmethod
    def _truncate_text(value: str, limit: int) -> str:
        text = value.strip()
        if len(text) <= limit:
            return text
        return f"{text[:limit]}..."

    @staticmethod
    def _summarize_cards(cards: dict | None) -> str:
        if not cards:
            return ""

        parts: list[str] = []
        doctor_cards = cards.get("doctor_cards")
        if isinstance(doctor_cards, list) and doctor_cards:
            names = []
            for card in doctor_cards[:2]:
                if isinstance(card, dict):
                    name = card.get("doctor_name")
                    dept = card.get("department")
                    if name and dept:
                        names.append(f"{name}({dept})")
                    elif name:
                        names.append(str(name))
            if names:
                parts.append(f"医生建议: {', '.join(names)}")

        medicine_cards = cards.get("medicine_cards")
        if isinstance(medicine_cards, list) and medicine_cards:
            names = []
            for card in medicine_cards[:3]:
                if isinstance(card, dict) and card.get("name"):
                    names.append(str(card["name"]))
            if names:
                parts.append(f"药品建议: {', '.join(names)}")

        return "；".join(parts)

    def _format_history_context(self, history_context: list[dict] | None) -> str:
        if not history_context:
            return "无"

        lines: list[str] = []
        total_len = 0
        max_total_len = 3000

        for item in history_context[-20:]:
            role = str(item.get("role") or "")
            role_text = "用户" if role == "user" else ("助手" if role == "assistant" else "操作")

            text_value = self._truncate_text(str(item.get("text") or ""), 300)
            cards_summary = ""
            cards = item.get("cards")
            if isinstance(cards, dict):
                cards_summary = self._summarize_cards(cards)

            action_summary = ""
            action_payload = item.get("action_payload")
            if isinstance(action_payload, dict):
                action_type = action_payload.get("action_type")
                if action_type == "confirm_appointment":
                    action_summary = "动作: 挂号执行"
                elif action_type == "create_medicine_order":
                    action_summary = "动作: 购药下单执行"
                elif action_type:
                    action_summary = f"动作: {action_type}"

            content_parts = [part for part in [text_value, cards_summary, action_summary] if part]
            if not content_parts:
                continue

            line = f"{role_text}: {'；'.join(content_parts)}"
            next_len = total_len + len(line)
            if next_len > max_total_len:
                break

            lines.append(line)
            total_len = next_len

        return "\n".join(lines) if lines else "无"

    def _generate_text(
        self,
        query: str,
        intent: IntentType,
        doctor_cards: list[dict],
        medicine_cards: list[dict],
        history_context: list[dict] | None = None,
    ) -> tuple[str, str]:
        if self.model is None:
            return self._fallback_text(intent, history_context=history_context)

        history_text = self._format_history_context(history_context)
        prompt = (
            "你是校园医疗AI助手。"
            "请基于用户问题与候选数据，输出JSON，字段为answer和reasoning_summary。"
            "不要输出markdown，不要编造数据。"
            f"\n用户问题: {query}"
            f"\n最近对话上下文(按时间顺序):\n{history_text}"
            f"\n意图: {intent}"
            f"\n医生候选: {json.dumps(doctor_cards, ensure_ascii=False)}"
            f"\n药品候选: {json.dumps(medicine_cards, ensure_ascii=False)}"
        )
        try:
            response = self.model.invoke(prompt)
            content = response.content if isinstance(response.content, str) else ""
            data = json.loads(content)
            answer = str(data.get("answer") or "")
            summary = str(data.get("reasoning_summary") or "")
            if answer.strip() and summary.strip():
                return answer.strip(), summary.strip()
        except Exception:
            pass

        return self._fallback_text(intent, history_context=history_context)

    @staticmethod
    def _fallback_text(intent: IntentType, history_context: list[dict] | None = None) -> tuple[str, str]:
        context_prefix = ""
        if history_context:
            last_user_text = ""
            for item in reversed(history_context):
                if item.get("role") == "user" and str(item.get("text") or "").strip():
                    last_user_text = str(item.get("text") or "").strip()
                    break
            if last_user_text:
                snippet = last_user_text[:40]
                context_prefix = f"结合你之前提到的“{snippet}”，"

        if intent == "diagnosis_only":
            return (
                f"{context_prefix}我已根据你的描述给出初步建议：先补水休息、监测症状变化；如出现持续高热或明显加重，请尽快线下就医。",
                "当前诉求以问诊建议为主，未触发挂号或购药执行。",
            )
        if intent == "appointment_only":
            return (
                f"{context_prefix}我已为你筛选可参考的医生与候选时段，你可以直接选择时段完成挂号。",
                "根据症状关键词匹配医生科室，并优先给出近期可预约时段。",
            )
        if intent == "medicine_only":
            return (
                f"{context_prefix}我已推荐可购买药品，你可以调整数量后下单。若症状持续或加重，请及时就医。",
                "优先推荐在库且启用的药品，并保留用户确认数量。",
            )
        return (
            f"{context_prefix}我已同时给出医生候选与药品建议，你可以分别确认挂号和购药。",
            "根据问诊内容同时识别出就诊与用药需求，分别生成可执行建议。",
        )

    def invoke(self, user_query: str, history_context: list[dict] | None = None) -> dict:
        def classify_node(state: AgentState) -> dict:
            return {"intent": self._classify_intent(state["user_query"])}

        def gather_node(state: AgentState) -> dict:
            intent = state["intent"]
            doctor_cards: list[dict] = []
            medicine_cards: list[dict] = []

            if intent in {"appointment_only", "appointment_and_medicine"}:
                doctor_cards = self._query_doctors(state["user_query"])
            if intent in {"medicine_only", "appointment_and_medicine"}:
                medicine_cards = self._query_medicines(state["user_query"])

            return {"doctor_cards": doctor_cards, "medicine_cards": medicine_cards}

        def respond_node(state: AgentState) -> dict:
            answer, summary = self._generate_text(
                query=state["user_query"],
                intent=state["intent"],
                doctor_cards=state["doctor_cards"],
                medicine_cards=state["medicine_cards"],
                history_context=history_context,
            )
            return {"answer": answer, "reasoning_summary": summary}

        graph = StateGraph(AgentState)
        graph.add_node("classify", classify_node)
        graph.add_node("gather", gather_node)
        graph.add_node("respond", respond_node)
        graph.add_edge(START, "classify")
        graph.add_edge("classify", "gather")
        graph.add_edge("gather", "respond")
        graph.add_edge("respond", END)

        app = graph.compile()
        result = app.invoke(
            {
                "user_query": user_query,
                "intent": "diagnosis_only",
                "doctor_cards": [],
                "medicine_cards": [],
                "answer": "",
                "reasoning_summary": "",
            }
        )

        return {
            "intent": result["intent"],
            "answer": result["answer"],
            "reasoning_summary": result["reasoning_summary"],
            "doctor_cards": result["doctor_cards"],
            "medicine_cards": result["medicine_cards"],
        }
