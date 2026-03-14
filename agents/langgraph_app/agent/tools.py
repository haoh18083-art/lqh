"""Tool definitions for the campus assistant agent."""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, timedelta
import re
from typing import Any

import httpx
from langchain.tools import tool
from sqlalchemy import text
from sqlalchemy.orm import Session

from agent.types import IntentType
from core.config import settings


SYMPTOM_DEPT_HINTS: dict[str, str] = {
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


@dataclass
class ToolBundle:
    tools: list[Any]
    expected_tools: set[str] = field(default_factory=set)
    called_tools: set[str] = field(default_factory=set)
    doctor_cards: list[dict] = field(default_factory=list)
    medicine_cards: list[dict] = field(default_factory=list)

    def can_stream_answer(self) -> bool:
        return self.expected_tools.issubset(self.called_tools)


def _extract_department_hint(query: str) -> str | None:
    for token, dept in SYMPTOM_DEPT_HINTS.items():
        if token in query:
            return dept
    return None


def _extract_keyword(query: str) -> str:
    value = query.strip()
    if len(value) <= 1:
        return ""

    # 仅在明确提到药名/规格时才启用关键词过滤，避免症状类问题误过滤掉全部候选。
    candidates = re.split(r"[，。,.、；;！？!?\s]+", value)
    marker_words = ("片", "胶囊", "颗粒", "口服液", "阿莫", "布洛芬", "头孢", "维C")
    for item in candidates:
        token = item.strip()
        if len(token) < 2 or len(token) > 12:
            continue
        if any(marker in token for marker in marker_words):
            return token

    return ""


def _query_slot_candidates(doctor_id: int, access_token: str) -> list[dict]:
    date_from = date.today().isoformat()
    date_to = (date.today() + timedelta(days=7)).isoformat()
    headers = {"Authorization": f"Bearer {access_token}"}
    params = {"doctor_id": doctor_id, "date_from": date_from, "date_to": date_to}

    try:
        with httpx.Client(timeout=15) as client:
            response = client.get(
                f"{settings.BACKEND_API_BASE_URL.rstrip('/')}/appointments/slots",
                params=params,
                headers=headers,
            )
        if response.status_code >= 400:
            return []

        body = response.json()
        raw_slots = body.get("data", body) if isinstance(body, dict) else body
        if not isinstance(raw_slots, list):
            return []

        available_slots: list[dict] = []
        for item in raw_slots:
            if not isinstance(item, dict):
                continue
            if item.get("status") != "available":
                continue

            slot_date = item.get("date") or item.get("visit_date")
            slot_time = item.get("time")
            if not slot_date or not slot_time:
                continue

            available_slots.append(
                {
                    "date": str(slot_date),
                    "time": str(slot_time),
                    "capacity": int(item.get("capacity") or 0),
                    "booked_count": int(item.get("booked_count") or 0),
                }
            )

        return available_slots[:3]
    except Exception:
        return []


def _query_doctors(db: Session, access_token: str, query: str, limit: int = 3) -> list[dict]:
    dept_hint = _extract_department_hint(query)

    rows = db.execute(
        text(
            """
            SELECT doc.id AS doctor_id,
                   u.full_name AS doctor_name,
                   COALESCE(d.name, doc.department, '未知科室') AS department,
                   doc.title AS title,
                   doc.introduction AS introduction
            FROM doctors doc
            JOIN users u ON u.id = doc.user_id
            LEFT JOIN departments d ON d.id = doc.department_id
            WHERE u.is_active = 1
              AND (d.id IS NULL OR d.is_active = 1)
              AND (:dept_hint IS NULL OR d.name = :dept_hint OR doc.department = :dept_hint)
            ORDER BY
              CASE
                WHEN :dept_hint IS NOT NULL AND (d.name = :dept_hint OR doc.department = :dept_hint) THEN 0
                ELSE 1
              END,
              doc.id DESC
            LIMIT :limit
            """
        ),
        {
            "dept_hint": dept_hint,
            "limit": int(limit),
        },
    ).mappings().all()

    cards: list[dict] = []
    for row in rows:
        doctor_id = int(row["doctor_id"])
        cards.append(
            {
                "doctor_id": doctor_id,
                "doctor_name": str(row.get("doctor_name") or ""),
                "department": str(row.get("department") or "未知科室"),
                "title": str(row.get("title") or "医师"),
                "introduction": str(row.get("introduction") or "暂无简介"),
                "recommend_reason": (
                    "根据症状关键词、科室匹配和可预约号源综合推荐"
                    if dept_hint
                    else "根据医生信息和可预约号源综合推荐"
                ),
                "slot_candidates": _query_slot_candidates(doctor_id, access_token),
            }
        )
    return cards


def _query_medicines(db: Session, query: str, limit: int = 5) -> list[dict]:
    keyword = _extract_keyword(query)
    like_keyword = f"%{keyword}%" if keyword else ""

    rows = db.execute(
        text(
            """
            SELECT id, name, category, spec, unit, stock, price
            FROM medicines
            WHERE is_active = 1
              AND stock > 0
            ORDER BY
              CASE
                WHEN :like_keyword <> '' AND name LIKE :like_keyword THEN 0
                ELSE 1
              END,
              stock DESC,
              id DESC
            LIMIT :limit
            """
        ),
        {
            "like_keyword": like_keyword,
            "limit": int(limit),
        },
    ).mappings().all()

    cards: list[dict] = []
    for row in rows:
        stock = int(row["stock"] or 0)
        cards.append(
            {
                "medicine_id": int(row["id"]),
                "name": str(row["name"]),
                "spec": row.get("spec"),
                "unit": row.get("unit"),
                "price": float(row["price"] or 0),
                "stock": stock,
                "default_quantity": 1,
                "max_quantity": stock,
                "recommend_reason": "优先推荐在库且启用药品，并结合用户诉求关键词筛选",
            }
        )

    return cards


def build_tools(db: Session, access_token: str, intent: IntentType) -> ToolBundle:
    bundle = ToolBundle(tools=[])

    @tool("search_doctors")
    def search_doctors(query: str) -> dict:
        """根据用户问题查询医生简介与可预约号源候选。"""
        cards = _query_doctors(db=db, access_token=access_token, query=query)
        bundle.called_tools.add("search_doctors")
        bundle.doctor_cards = cards
        return {"total": len(cards), "doctor_cards": cards}

    @tool("search_medicines")
    def search_medicines(query: str) -> dict:
        """根据用户问题查询可购买药品候选。"""
        cards = _query_medicines(db=db, query=query)
        bundle.called_tools.add("search_medicines")
        bundle.medicine_cards = cards
        return {"total": len(cards), "medicine_cards": cards}

    if intent == "appointment_only":
        bundle.tools = [search_doctors]
        bundle.expected_tools = {"search_doctors"}
    elif intent == "medicine_only":
        bundle.tools = [search_medicines]
        bundle.expected_tools = {"search_medicines"}
    elif intent == "appointment_and_medicine":
        bundle.tools = [search_doctors, search_medicines]
        bundle.expected_tools = {"search_doctors", "search_medicines"}
    else:
        bundle.tools = []
        bundle.expected_tools = set()

    return bundle
