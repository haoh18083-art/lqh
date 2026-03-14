"""LangGraph app FastAPI entrypoint."""
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from api.routes.assistant import router as assistant_router
from api.routes.health import router as health_router
from api.routes.pharmacy import router as pharmacy_router
from db.mongo import close_mongo
from db.mysql import engine
from models.base import Base
from models.ai_chat_message_meta import AIChatMessageMeta  # noqa: F401
from models.ai_chat_session import AIChatSession  # noqa: F401
from models.medicine_order import MedicineOrder  # noqa: F401
from models.medicine_order_item import MedicineOrderItem  # noqa: F401


@asynccontextmanager
async def lifespan(_: FastAPI):
    Base.metadata.create_all(bind=engine)
    yield
    close_mongo()


app = FastAPI(
    title="Campus LangGraph Assistant",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)
app.include_router(assistant_router, prefix="/api/v1")
app.include_router(pharmacy_router, prefix="/api/v1")
