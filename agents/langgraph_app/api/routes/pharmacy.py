"""Pharmacy routes."""
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from core.security import get_current_student_context
from db.mysql import get_db
from schemas.pharmacy import (
    PharmacyMedicineListResponse,
    PharmacyOrderCreateRequest,
    PharmacyOrderListResponse,
    PharmacyOrderResponse,
)
from services.pharmacy_service import PharmacyService


router = APIRouter(prefix="/pharmacy", tags=["pharmacy"])


@router.get("/medicines", response_model=PharmacyMedicineListResponse)
def list_medicines(
    search: str | None = Query(default=None),
    category: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    _: dict = Depends(get_current_student_context),
    db: Session = Depends(get_db),
):
    return PharmacyService.list_medicines(
        db,
        page=page,
        page_size=page_size,
        search=search,
        category=category,
    )


@router.post("/orders", response_model=PharmacyOrderResponse)
def create_order(
    payload: PharmacyOrderCreateRequest,
    student_ctx: dict = Depends(get_current_student_context),
    db: Session = Depends(get_db),
):
    try:
        return PharmacyService.create_order(
            db,
            student_id=student_ctx["student_id"],
            items=[item.model_dump() for item in payload.items],
        )
    except RuntimeError as exc:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.get("/orders", response_model=PharmacyOrderListResponse)
def list_orders(
    status: str | None = Query(default=None),
    keyword: str | None = Query(default=None),
    date_from: str | None = Query(default=None),
    date_to: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=200),
    student_ctx: dict = Depends(get_current_student_context),
    db: Session = Depends(get_db),
):
    return PharmacyService.list_orders(
        db,
        student_id=student_ctx["student_id"],
        page=page,
        page_size=page_size,
        status=status,
        keyword=keyword,
        date_from=date_from,
        date_to=date_to,
    )


@router.get("/orders/{order_id}", response_model=PharmacyOrderResponse)
def get_order(
    order_id: int,
    student_ctx: dict = Depends(get_current_student_context),
    db: Session = Depends(get_db),
):
    try:
        return PharmacyService.get_order_by_id(
            db,
            student_id=student_ctx["student_id"],
            order_id=order_id,
        )
    except RuntimeError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
