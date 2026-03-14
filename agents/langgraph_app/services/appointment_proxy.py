"""Proxy appointment creation to backend."""
from typing import Any

import httpx

from core.config import settings


class AppointmentProxyService:
    @staticmethod
    def confirm_appointment(access_token: str, payload: dict[str, Any]) -> dict[str, Any]:
        request_body = {
            "doctor_id": payload["doctor_id"],
            "date": payload["date"],
            "time": payload["time"],
            "symptoms": payload.get("symptoms"),
        }

        headers = {"Authorization": f"Bearer {access_token}"}
        with httpx.Client(timeout=15) as client:
            response = client.post(
                f"{settings.BACKEND_API_BASE_URL.rstrip('/')}/appointments",
                json=request_body,
                headers=headers,
            )

        if response.status_code >= 400:
            try:
                detail = response.json()
            except Exception:
                detail = {"message": response.text}
            raise RuntimeError(detail.get("error", {}).get("message") or detail.get("message") or "挂号失败")

        data = response.json()
        return data.get("data", data)
