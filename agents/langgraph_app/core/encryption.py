"""Encryption helpers for settings."""
from cryptography.fernet import Fernet, InvalidToken

from core.config import settings


class SettingsEncryptionError(RuntimeError):
    pass


def _cipher() -> Fernet:
    key = settings.SETTINGS_ENCRYPTION_KEY
    if not key:
        raise SettingsEncryptionError("SETTINGS_ENCRYPTION_KEY 未配置")
    try:
        return Fernet(key.encode("utf-8"))
    except Exception as exc:
        raise SettingsEncryptionError("SETTINGS_ENCRYPTION_KEY 格式无效") from exc


def decrypt_secret(value: str) -> str:
    if not value:
        raise SettingsEncryptionError("缺少加密密钥")
    try:
        return _cipher().decrypt(value.encode("utf-8")).decode("utf-8")
    except InvalidToken as exc:
        raise SettingsEncryptionError("密钥解密失败") from exc
