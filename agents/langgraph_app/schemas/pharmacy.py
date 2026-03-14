"""Schemas for pharmacy APIs."""
from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


OrderStatus = Literal["completed", "pending", "cancelled"]


class PharmacyMedicineItem(BaseModel):
    id: int
    code: str | None = None
    name: str
    category: str
    specification: str | None = None
    unit: str | None = None
    stock: int
    threshold: int
    price: float
    supplier: str | None = None
    manufacturer: str | None = None
    approval_number: str | None = None
    is_active: bool


class PharmacyMedicineListResponse(BaseModel):
    items: list[PharmacyMedicineItem]
    total: int
    page: int
    page_size: int
    total_pages: int


class PharmacyOrderCreateItem(BaseModel):
    medicine_id: int
    quantity: int = Field(..., ge=1)


class PharmacyOrderCreateRequest(BaseModel):
    items: list[PharmacyOrderCreateItem]


class PharmacyOrderItemResponse(BaseModel):
    id: int
    medicine_id: int | None = None
    medicine_name: str
    specification: str | None = None
    quantity: int
    unit: str | None = None
    price: float


class PharmacyOrderResponse(BaseModel):
    id: int
    order_no: str
    purchase_date: datetime
    total_amount: float
    status: OrderStatus
    items: list[PharmacyOrderItemResponse]


class PharmacyOrderListResponse(BaseModel):
    items: list[PharmacyOrderResponse]
    total: int
    page: int
    page_size: int
    total_pages: int
