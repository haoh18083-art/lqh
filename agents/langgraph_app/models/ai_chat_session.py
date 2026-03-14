"""AI chat session metadata model."""

from sqlalchemy import Column, DateTime, Enum, String
from sqlalchemy.dialects.mysql import BIGINT

from core.config import now_sh
from models.base import Base


class AIChatSession(Base):
    __tablename__ = "ai_chat_sessions"

    id = Column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    public_session_id = Column(String(64), nullable=False, unique=True, index=True)
    student_id = Column(BIGINT(unsigned=True), nullable=False, index=True)
    mongo_conversation_id = Column(String(24), nullable=False, index=True)
    title = Column(String(200), nullable=True)
    status = Column(
        Enum("active", "archived", name="ai_chat_session_status"),
        nullable=False,
        default="active",
        index=True,
    )
    last_message_at = Column(DateTime, nullable=False, default=now_sh, index=True)
    deleted_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, nullable=False, default=now_sh)
    updated_at = Column(DateTime, nullable=False, default=now_sh, onupdate=now_sh)
