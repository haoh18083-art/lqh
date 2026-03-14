"""Medicine order item ORM model."""
from sqlalchemy import Column, ForeignKey, Integer, Numeric, String
from sqlalchemy.dialects.mysql import BIGINT

from models.base import Base


class MedicineOrderItem(Base):
    __tablename__ = "medicine_order_items"

    id = Column(BIGINT(unsigned=True), primary_key=True, autoincrement=True)
    order_id = Column(BIGINT(unsigned=True), ForeignKey("medicine_orders.id", ondelete="CASCADE"), nullable=False, index=True)
    medicine_id = Column(BIGINT(unsigned=True), nullable=True, index=True)
    medicine_name_snapshot = Column(String(100), nullable=False)
    spec_snapshot = Column(String(100), nullable=True)
    unit = Column(String(20), nullable=True)
    unit_price = Column(Numeric(10, 2), nullable=False, default=0)
    quantity = Column(Integer, nullable=False, default=1)
    total_price = Column(Numeric(10, 2), nullable=False, default=0)
