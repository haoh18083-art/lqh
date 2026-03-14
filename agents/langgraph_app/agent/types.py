"""Type declarations for campus assistant agent."""
from __future__ import annotations

from typing import Literal, TypedDict


IntentType = Literal[
    "diagnosis_only",
    "appointment_only",
    "medicine_only",
    "appointment_and_medicine",
]


class AgentInvokeResult(TypedDict):
    intent: IntentType
    answer: str
    reasoning_summary: str
    doctor_cards: list[dict]
    medicine_cards: list[dict]


class TokenStreamEvent(TypedDict):
    type: Literal["token"]
    text: str


class StateStreamEvent(TypedDict):
    type: Literal["state"]
    result: AgentInvokeResult


AgentStreamEvent = TokenStreamEvent | StateStreamEvent
