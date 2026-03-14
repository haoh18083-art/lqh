"""Runtime settings for langgraph app."""
from datetime import datetime
from zoneinfo import ZoneInfo

from pydantic_settings import BaseSettings


def now_sh() -> datetime:
    """Return current time in Asia/Shanghai timezone."""
    return datetime.now(ZoneInfo("Asia/Shanghai"))


class Settings(BaseSettings):
    MYSQL_HOST: str = "mysql"
    MYSQL_PORT: int = 3306
    MYSQL_DATABASE: str = "campus_medical"
    MYSQL_USER: str = "medical_user"
    MYSQL_PASSWORD: str = "medical_password"

    MONGO_HOST: str = "mongo"
    MONGO_PORT: int = 27017
    MONGO_DATABASE: str = "campus_medical"
    MONGO_APP_USER: str = "medical_app"
    MONGO_APP_PASSWORD: str = "medical_app_password"

    SECRET_KEY: str
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    SETTINGS_ENCRYPTION_KEY: str | None = None

    BACKEND_API_BASE_URL: str = "http://backend:8000/api/v1"

    DEEPSEEK_API_KEY: str | None = None
    DEEPSEEK_BASE_URL: str = "https://api.deepseek.com"
    MODEL_NAME: str = "deepseek-chat"
    MODEL_TEMPERATURE: float = 0

    APP_TIMEZONE: str = "Asia/Shanghai"

    @property
    def MYSQL_URL(self) -> str:
        return (
            "mysql+pymysql://"
            f"{self.MYSQL_USER}:{self.MYSQL_PASSWORD}@{self.MYSQL_HOST}:{self.MYSQL_PORT}/{self.MYSQL_DATABASE}"
            "?charset=utf8mb4"
        )

    @property
    def MONGO_URL(self) -> str:
        return (
            f"mongodb://{self.MONGO_APP_USER}:{self.MONGO_APP_PASSWORD}"
            f"@{self.MONGO_HOST}:{self.MONGO_PORT}/{self.MONGO_DATABASE}"
        )

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
