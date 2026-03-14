"""Hybrid chat storage service (MySQL metadata + Mongo content)."""
from __future__ import annotations

from math import ceil
from uuid import uuid4

from bson import ObjectId
from sqlalchemy import func
from sqlalchemy.orm import Session

from core.config import now_sh
from db.mongo import get_mongo_db
from models.ai_chat_message_meta import AIChatMessageMeta
from models.ai_chat_session import AIChatSession


class ChatStorageService:
    @staticmethod
    def get_or_create_session(
        db: Session,
        *,
        student_id: int,
        public_session_id: str | None,
        title: str | None = None,
    ) -> AIChatSession:
        if public_session_id:
            existing = (
                db.query(AIChatSession)
                .filter(
                    AIChatSession.public_session_id == public_session_id,
                    AIChatSession.student_id == student_id,
                    AIChatSession.deleted_at.is_(None),
                )
                .first()
            )
            if existing:
                return existing

        mongo_db = get_mongo_db()
        public_id = public_session_id or str(uuid4())
        doc = {
            "public_session_id": public_id,
            "student_id": student_id,
            "created_at": now_sh(),
            "updated_at": now_sh(),
        }
        insert_res = mongo_db.ai_conversations.insert_one(doc)

        session = AIChatSession(
            public_session_id=public_id,
            student_id=student_id,
            mongo_conversation_id=str(insert_res.inserted_id),
            title=title,
            status="active",
            last_message_at=now_sh(),
        )
        db.add(session)
        db.commit()
        db.refresh(session)
        return session

    @staticmethod
    def _next_seq(db: Session, session_id: int) -> int:
        seq = (
            db.query(func.max(AIChatMessageMeta.seq_no))
            .filter(AIChatMessageMeta.session_id == session_id)
            .scalar()
        )
        return int(seq or 0) + 1

    @staticmethod
    def save_message(
        db: Session,
        *,
        session: AIChatSession,
        role: str,
        message_kind: str,
        text: str,
        cards: dict | None = None,
        action_payload: dict | None = None,
        token_count: int | None = None,
    ) -> AIChatMessageMeta:
        seq_no = ChatStorageService._next_seq(db, session.id)
        meta = AIChatMessageMeta(
            session_id=session.id,
            seq_no=seq_no,
            role=role,
            message_kind=message_kind,
            storage_status="pending",
            token_count=token_count,
        )
        db.add(meta)
        db.commit()
        db.refresh(meta)

        mongo_db = get_mongo_db()
        try:
            conversation_id = ObjectId(session.mongo_conversation_id)
            doc = {
                "conversation_id": conversation_id,
                "session_mysql_id": session.id,
                "message_meta_mysql_id": meta.id,
                "seq_no": seq_no,
                "role": role,
                "text": text,
                "cards": cards,
                "action_payload": action_payload,
                "created_at": now_sh(),
            }
            insert_res = mongo_db.ai_messages.insert_one(doc)

            meta.mongo_message_id = str(insert_res.inserted_id)
            meta.storage_status = "stored"
            session.last_message_at = now_sh()
            db.add(meta)
            db.add(session)
            db.commit()
            db.refresh(meta)
            return meta
        except Exception:
            meta.storage_status = "failed"
            db.add(meta)
            db.commit()
            raise

    @staticmethod
    def list_sessions(db: Session, *, student_id: int, page: int, page_size: int) -> dict:
        query = (
            db.query(AIChatSession)
            .filter(AIChatSession.student_id == student_id, AIChatSession.deleted_at.is_(None))
        )
        total = query.count()
        items = (
            query.order_by(AIChatSession.last_message_at.desc())
            .offset((page - 1) * page_size)
            .limit(page_size)
            .all()
        )
        return {
            "items": [
                {
                    "public_session_id": it.public_session_id,
                    "title": it.title,
                    "status": it.status,
                    "last_message_at": it.last_message_at.isoformat(),
                    "created_at": it.created_at.isoformat(),
                }
                for it in items
            ],
            "total": total,
            "page": page,
            "page_size": page_size,
            "total_pages": ceil(total / page_size) if total else 0,
        }

    @staticmethod
    def list_messages(db: Session, *, student_id: int, public_session_id: str) -> dict:
        session = (
            db.query(AIChatSession)
            .filter(
                AIChatSession.student_id == student_id,
                AIChatSession.public_session_id == public_session_id,
                AIChatSession.deleted_at.is_(None),
            )
            .first()
        )
        if not session:
            raise RuntimeError("会话不存在")

        metas = (
            db.query(AIChatMessageMeta)
            .filter(AIChatMessageMeta.session_id == session.id)
            .order_by(AIChatMessageMeta.seq_no.asc())
            .all()
        )

        mongo_db = get_mongo_db()
        message_map: dict[int, dict] = {}
        for meta in metas:
            if meta.mongo_message_id:
                mongo_doc = mongo_db.ai_messages.find_one({"_id": ObjectId(meta.mongo_message_id)})
                if mongo_doc:
                    message_map[meta.id] = mongo_doc

        items = []
        for meta in metas:
            doc = message_map.get(meta.id, {})
            items.append(
                {
                    "seq_no": meta.seq_no,
                    "role": meta.role,
                    "message_kind": meta.message_kind,
                    "text": doc.get("text") or "",
                    "cards": doc.get("cards"),
                    "action_payload": doc.get("action_payload"),
                    "created_at": meta.created_at.isoformat(),
                }
            )

        return {"items": items, "total": len(items), "session": session}

    @staticmethod
    def get_recent_context(
        db: Session,
        *,
        session: AIChatSession,
        limit: int = 20,
    ) -> list[dict]:
        metas = (
            db.query(AIChatMessageMeta)
            .filter(
                AIChatMessageMeta.session_id == session.id,
                AIChatMessageMeta.role.in_(["user", "assistant", "action"]),
            )
            .order_by(AIChatMessageMeta.seq_no.desc())
            .limit(limit)
            .all()
        )

        if not metas:
            return []

        mongo_db = get_mongo_db()
        context: list[dict] = []
        for meta in reversed(metas):
            text = ""
            cards = None
            action_payload = None

            if meta.mongo_message_id:
                try:
                    mongo_doc = mongo_db.ai_messages.find_one({"_id": ObjectId(meta.mongo_message_id)})
                except Exception:
                    mongo_doc = None

                if mongo_doc:
                    text = str(mongo_doc.get("text") or "")
                    cards = mongo_doc.get("cards")
                    action_payload = mongo_doc.get("action_payload")

            context.append(
                {
                    "role": meta.role,
                    "message_kind": meta.message_kind,
                    "text": text,
                    "cards": cards,
                    "action_payload": action_payload,
                }
            )

        return context

    @staticmethod
    def soft_delete_session(db: Session, *, student_id: int, public_session_id: str) -> None:
        session = (
            db.query(AIChatSession)
            .filter(
                AIChatSession.student_id == student_id,
                AIChatSession.public_session_id == public_session_id,
                AIChatSession.deleted_at.is_(None),
            )
            .first()
        )
        if not session:
            return

        session.deleted_at = now_sh()
        session.status = "archived"
        db.add(session)
        db.commit()
