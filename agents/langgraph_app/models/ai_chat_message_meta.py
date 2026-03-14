"""AI chat message metadata model."""
from sqlalchemy import Column, DateTime, Enum, Integer, String, UniqueConstraint
from sqlalchemy.dialects.mysql import BIGINT

from core.config import now_sh
from models.base import Base


class AIChatMessageMeta(Base):
    __tablename__ = "ai_chat_messages_meta"
    __table_args__ = (
        UniqueConstraint("session_id", "seq_no", name="uk_ai_chat_session_seq"),
    )

    id = Column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    session_id = Column(BIGINT(unsigned=True), nullable=False, index=True)
    seq_no = Column(Integer, nullable=False)
    role = Column(Enum("user", "assistant", "system", "action", name="ai_chat_role"), nullable=False)
    message_kind = Column(
        Enum("text", "cards", "action_result", "error", name="ai_chat_message_kind"),
        nullable=False,
        default="text",
    )
    mongo_message_id = Column(String(24), nullable=True, index=True)
    storage_status = Column(
        Enum("pending", "stored", "failed", name="ai_chat_storage_status"),
        nullable=False,
        default="pending",
        index=True,
    )
    token_count = Column(Integer, nullable=True)
    created_at = Column(DateTime, nullable=False, default=now_sh)
