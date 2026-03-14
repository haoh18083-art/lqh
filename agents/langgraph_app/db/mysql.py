"""MySQL session management."""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from core.config import settings


engine = create_engine(
    settings.MYSQL_URL,
    pool_size=10,
    max_overflow=20,
    pool_recycle=3600,
    pool_pre_ping=True,
)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
