"""Schemas for assistant APIs."""
from typing import Literal

from pydantic import BaseModel, Field


class AssistantChatRequest(BaseModel):
    public_session_id: str | None = Field(default=None)
    message: str = Field(..., min_length=1, max_length=4000)
    context: dict | None = Field(default=None)


class SlotCandidate(BaseModel):
    date: str
    time: str
    capacity: int
    booked_count: int


class DoctorCard(BaseModel):
    doctor_id: int
    doctor_name: str
    department: str
    title: str
    introduction: str
    recommend_reason: str
    slot_candidates: list[SlotCandidate] = []


class MedicineCard(BaseModel):
    medicine_id: int
    name: str
    spec: str | None = None
    unit: str | None = None
    price: float
    stock: int
    default_quantity: int = 1
    max_quantity: int
    recommend_reason: str


class AssistantStatePayload(BaseModel):
    public_session_id: str
    intent: Literal[
        "diagnosis_only",
        "appointment_only",
        "medicine_only",
        "appointment_and_medicine",
    ]
    answer: str
    reasoning_summary: str
    doctor_cards: list[DoctorCard] = []
    medicine_cards: list[MedicineCard] = []


class ActionItem(BaseModel):
    medicine_id: int
    quantity: int = Field(..., ge=1)


class AssistantActionRequest(BaseModel):
    public_session_id: str
    action_type: Literal["confirm_appointment", "create_medicine_order"]
    payload: dict


class AssistantActionResponse(BaseModel):
    success: bool
    message: str
    data: dict | None = None


class ChatSessionItem(BaseModel):
    public_session_id: str
    title: str | None = None
    status: str
    last_message_at: str
    created_at: str


class ChatSessionListResponse(BaseModel):
    items: list[ChatSessionItem]
    total: int
    page: int
    page_size: int
    total_pages: int


class ChatMessageItem(BaseModel):
    seq_no: int
    role: str
    message_kind: str
    text: str
    cards: dict | None = None
    action_payload: dict | None = None
    created_at: str


class ChatMessageListResponse(BaseModel):
    items: list[ChatMessageItem]
    total: int
