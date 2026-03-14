"""LLM config loader from MySQL system settings with env fallback."""
from dataclasses import dataclass

from sqlalchemy import text
from sqlalchemy.orm import Session

from core.config import settings
from core.encryption import SettingsEncryptionError, decrypt_secret


@dataclass
class RuntimeLLMConfig:
    base_url: str
    model: str
    api_key: str
    temperature: float


class LLMSettingsService:
    @staticmethod
    def get_runtime_llm_config(db: Session) -> RuntimeLLMConfig | None:
        row = db.execute(
            text(
                """
                SELECT base_url, model, api_key_encrypted, is_configured
                FROM system_settings
                WHERE category = 'llm' AND setting_key = 'default_provider'
                LIMIT 1
                """
            )
        ).mappings().first()

        if row and row.get("is_configured") and row.get("api_key_encrypted"):
            try:
                api_key = decrypt_secret(row["api_key_encrypted"])
                return RuntimeLLMConfig(
                    base_url=(row.get("base_url") or settings.DEEPSEEK_BASE_URL).rstrip("/"),
                    model=(row.get("model") or settings.MODEL_NAME),
                    api_key=api_key,
                    temperature=settings.MODEL_TEMPERATURE,
                )
            except SettingsEncryptionError:
                pass

        if settings.DEEPSEEK_API_KEY:
            return RuntimeLLMConfig(
                base_url=settings.DEEPSEEK_BASE_URL.rstrip("/"),
                model=settings.MODEL_NAME,
                api_key=settings.DEEPSEEK_API_KEY,
                temperature=settings.MODEL_TEMPERATURE,
            )

        return None
