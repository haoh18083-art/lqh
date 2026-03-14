"""Medicine order ORM model."""
from sqlalchemy import Column, DateTime, Enum, Numeric, String
from sqlalchemy.dialects.mysql import BIGINT

from core.config import now_sh
from models.base import Base


class MedicineOrder(Base):
    __tablename__ = "medicine_orders"

    id = Column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    order_no = Column(String(32), nullable=False, unique=True, index=True)
    student_id = Column(BIGINT(unsigned=True), nullable=False, index=True)
    status = Column(
        Enum("completed", "pending", "cancelled", name="medicine_order_status"),
        nullable=False,
        default="completed",
        index=True,
    )
    total_amount = Column(Numeric(10, 2), nullable=False, default=0)
    created_at = Column(DateTime, nullable=False, default=now_sh)
    updated_at = Column(DateTime, nullable=False, default=now_sh, onupdate=now_sh)
