"""Assistant routes with SSE streaming and actions."""
from __future__ import annotations

import json
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import StreamingResponse
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session

from agent.core import CampusAssistantAgent
from core.security import get_current_student_context
from db.mysql import SessionLocal, get_db
from schemas.assistant import (
    AssistantActionRequest,
    AssistantActionResponse,
    AssistantChatRequest,
    ChatMessageListResponse,
    ChatSessionListResponse,
)
from services.appointment_proxy import AppointmentProxyService
from services.chat_storage import ChatStorageService
from services.pharmacy_service import PharmacyService


router = APIRouter(prefix="/assistant", tags=["assistant"])
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")


def _sse_event(event: str, data: dict) -> str:
    return f"event: {event}\ndata: {json.dumps(data, ensure_ascii=False)}\n\n"


def _persist_assistant_message(
    *,
    student_id: int,
    public_session_id: str,
    result: dict[str, Any],
) -> None:
    db = SessionLocal()
    try:
        session = ChatStorageService.get_or_create_session(
            db,
            student_id=student_id,
            public_session_id=public_session_id,
        )
        ChatStorageService.save_message(
            db,
            session=session,
            role="assistant",
            message_kind="cards" if (result["doctor_cards"] or result["medicine_cards"]) else "text",
            text=result["answer"],
            cards={
                "intent": result["intent"],
                "reasoning_summary": result["reasoning_summary"],
                "doctor_cards": result["doctor_cards"],
                "medicine_cards": result["medicine_cards"],
            },
        )
    finally:
        db.close()


def _persist_error_message(
    *,
    student_id: int,
    public_session_id: str,
    error_message: str,
) -> None:
    db = SessionLocal()
    try:
        session = ChatStorageService.get_or_create_session(
            db,
            student_id=student_id,
            public_session_id=public_session_id,
        )
        ChatStorageService.save_message(
            db,
            session=session,
            role="assistant",
            message_kind="error",
            text=error_message,
        )
    finally:
        db.close()


@router.post("/chat/stream")
async def chat_stream(
    payload: AssistantChatRequest,
    student_ctx: dict = Depends(get_current_student_context),
    access_token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
):
    try:
        session = ChatStorageService.get_or_create_session(
            db,
            student_id=student_ctx["student_id"],
            public_session_id=payload.public_session_id,
            title=(payload.message[:40] if payload.message else None),
        )
        history_context = ChatStorageService.get_recent_context(
            db,
            session=session,
            limit=20,
        )
        ChatStorageService.save_message(
            db,
            session=session,
            role="user",
            message_kind="text",
            text=payload.message,
        )
        agent_service = CampusAssistantAgent(db=db, access_token=access_token)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"生成助手回复失败: {exc}") from exc

    public_session_id = session.public_session_id
    student_id = int(student_ctx["student_id"])
    user_message = payload.message

    async def event_generator():
        try:
            final_result: dict[str, Any] | None = None
            async for stream_event in agent_service.stream_reply(
                user_input=user_message,
                chat_history=history_context,
            ):
                if stream_event["type"] == "token":
                    yield _sse_event("token", {"text": stream_event["text"]})
                elif stream_event["type"] == "state":
                    final_result = stream_event["result"]

            if not final_result:
                raise RuntimeError("Agent 未返回最终状态")

            state_payload = {
                "public_session_id": public_session_id,
                "intent": final_result["intent"],
                "answer": final_result["answer"],
                "reasoning_summary": final_result["reasoning_summary"],
                "doctor_cards": final_result["doctor_cards"],
                "medicine_cards": final_result["medicine_cards"],
            }
            yield _sse_event("state", state_payload)
            yield _sse_event("done", {"public_session_id": public_session_id})

            try:
                _persist_assistant_message(
                    student_id=student_id,
                    public_session_id=public_session_id,
                    result=final_result,
                )
            except Exception:
                # 持久化失败不影响本次流式返回
                pass
        except Exception as exc:
            error_text = f"AI 服务异常: {exc}"
            yield _sse_event(
                "error",
                {
                    "message": error_text,
                    "code": "AGENT_RUNTIME_ERROR",
                },
            )
            yield _sse_event("done", {"public_session_id": public_session_id})
            try:
                _persist_error_message(
                    student_id=student_id,
                    public_session_id=public_session_id,
                    error_message=error_text,
                )
            except Exception:
                pass

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.post("/actions/execute", response_model=AssistantActionResponse)
def execute_action(
    payload: AssistantActionRequest,
    student_ctx: dict = Depends(get_current_student_context),
    access_token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
):
    try:
        session = ChatStorageService.get_or_create_session(
            db,
            student_id=student_ctx["student_id"],
            public_session_id=payload.public_session_id,
        )

        if payload.action_type == "confirm_appointment":
            result = AppointmentProxyService.confirm_appointment(access_token, payload.payload)
            message = "挂号成功"
        elif payload.action_type == "create_medicine_order":
            items = payload.payload.get("items") or []
            result = PharmacyService.create_order(db, student_id=student_ctx["student_id"], items=items)
            message = "购药下单成功"
        else:
            raise RuntimeError("不支持的动作类型")

        ChatStorageService.save_message(
            db,
            session=session,
            role="action",
            message_kind="action_result",
            text=message,
            action_payload={
                "action_type": payload.action_type,
                "result": result,
            },
        )

        return AssistantActionResponse(success=True, message=message, data=result)
    except RuntimeError as exc:
        db.rollback()
        return AssistantActionResponse(success=False, message=str(exc), data=None)
    except Exception as exc:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.get("/sessions", response_model=ChatSessionListResponse)
def list_sessions(
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    student_ctx: dict = Depends(get_current_student_context),
    db: Session = Depends(get_db),
):
    return ChatStorageService.list_sessions(
        db,
        student_id=student_ctx["student_id"],
        page=page,
        page_size=page_size,
    )


@router.get("/sessions/{public_session_id}/messages", response_model=ChatMessageListResponse)
def list_messages(
    public_session_id: str,
    student_ctx: dict = Depends(get_current_student_context),
    db: Session = Depends(get_db),
):
    try:
        data = ChatStorageService.list_messages(
            db,
            student_id=student_ctx["student_id"],
            public_session_id=public_session_id,
        )
        return {"items": data["items"], "total": data["total"]}
    except RuntimeError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.delete("/sessions/{public_session_id}")
def delete_session(
    public_session_id: str,
    student_ctx: dict = Depends(get_current_student_context),
    db: Session = Depends(get_db),
):
    ChatStorageService.soft_delete_session(
        db,
        student_id=student_ctx["student_id"],
        public_session_id=public_session_id,
    )
    return {"success": True}
