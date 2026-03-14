"""MongoDB helpers."""
from pymongo import MongoClient

from core.config import settings


_mongo_client: MongoClient | None = None


def get_mongo_db():
    global _mongo_client
    if _mongo_client is None:
        _mongo_client = MongoClient(
            settings.MONGO_URL,
            maxPoolSize=50,
            minPoolSize=5,
            maxIdleTimeMS=30000,
            connectTimeoutMS=5000,
            serverSelectionTimeoutMS=5000,
        )
    return _mongo_client[settings.MONGO_DATABASE]


def close_mongo() -> None:
    global _mongo_client
    if _mongo_client is not None:
        _mongo_client.close()
        _mongo_client = None
