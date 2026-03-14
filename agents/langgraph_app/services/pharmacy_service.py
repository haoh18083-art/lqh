"""Pharmacy query and order services."""
from __future__ import annotations

from decimal import Decimal
from math import ceil
from random import randint

from sqlalchemy import bindparam, text
from sqlalchemy.orm import Session

from core.config import now_sh
from models.medicine_order import MedicineOrder
from models.medicine_order_item import MedicineOrderItem


class PharmacyService:
    @staticmethod
    def list_medicines(
        db: Session,
        *,
        page: int,
        page_size: int,
        search: str | None = None,
        category: str | None = None,
    ) -> dict:
        where = ["is_active = 1"]
        params: dict[str, object] = {}

        if search:
            where.append("(name LIKE :search OR code LIKE :search)")
            params["search"] = f"%{search}%"
        if category:
            where.append("category = :category")
            params["category"] = category

        where_sql = " AND ".join(where)
        total = db.execute(text(f"SELECT COUNT(*) FROM medicines WHERE {where_sql}"), params).scalar() or 0

        params.update({"offset": (page - 1) * page_size, "limit": page_size})
        rows = db.execute(
            text(
                f"""
                SELECT id, code, name, category, spec, unit, stock, threshold, price,
                       supplier, manufacturer, approval_number, is_active
                FROM medicines
                WHERE {where_sql}
                ORDER BY id DESC
                LIMIT :offset, :limit
                """
            ),
            params,
        ).mappings().all()

        items = [
            {
                "id": int(row["id"]),
                "code": row["code"],
                "name": row["name"],
                "category": row["category"] or "其他",
                "specification": row["spec"],
                "unit": row["unit"],
                "stock": int(row["stock"] or 0),
                "threshold": int(row["threshold"] or 0),
                "price": float(row["price"] or 0),
                "supplier": row["supplier"],
                "manufacturer": row["manufacturer"],
                "approval_number": row["approval_number"],
                "is_active": bool(row["is_active"]),
            }
            for row in rows
        ]

        return {
            "items": items,
            "total": int(total),
            "page": page,
            "page_size": page_size,
            "total_pages": ceil(total / page_size) if total else 0,
        }

    @staticmethod
    def create_order(db: Session, *, student_id: int, items: list[dict]) -> dict:
        if not items:
            raise RuntimeError("药品列表不能为空")

        medicine_ids = [int(i["medicine_id"]) for i in items]
        qty_map = {int(i["medicine_id"]): int(i["quantity"]) for i in items}

        rows = db.execute(
            text(
                """
                SELECT id, name, spec, unit, stock, is_active, price
                FROM medicines
                WHERE id IN :ids
                FOR UPDATE
                """
            ).bindparams(bindparam("ids", expanding=True)),
            {"ids": medicine_ids},
        ).mappings().all()

        medicine_map = {int(row["id"]): row for row in rows}
        if len(medicine_map) != len(set(medicine_ids)):
            raise RuntimeError("存在无效药品")

        total_amount = Decimal("0")
        for mid in medicine_ids:
            row = medicine_map[mid]
            quantity = qty_map[mid]
            if not bool(row["is_active"]):
                raise RuntimeError(f"药品已下架: {row['name']}")
            if int(row["stock"] or 0) < quantity:
                raise RuntimeError(f"库存不足: {row['name']}")
            unit_price = Decimal(str(row["price"] or 0))
            total_amount += unit_price * quantity

        order_no = f"ORD{now_sh().strftime('%Y%m%d%H%M%S')}{randint(10, 99)}"
        order = MedicineOrder(
            order_no=order_no,
            student_id=student_id,
            status="completed",
            total_amount=total_amount,
        )
        db.add(order)
        db.flush()

        for mid in medicine_ids:
            row = medicine_map[mid]
            quantity = qty_map[mid]
            unit_price = Decimal(str(row["price"] or 0))
            total_price = unit_price * quantity

            db.add(
                MedicineOrderItem(
                    order_id=order.id,
                    medicine_id=mid,
                    medicine_name_snapshot=row["name"],
                    spec_snapshot=row["spec"],
                    unit=row["unit"],
                    unit_price=unit_price,
                    quantity=quantity,
                    total_price=total_price,
                )
            )

            db.execute(
                text("UPDATE medicines SET stock = stock - :quantity WHERE id = :id"),
                {"quantity": quantity, "id": mid},
            )

            db.execute(
                text(
                    """
                    INSERT INTO inventory_movements (medicine_id, delta, reason, ref_type, ref_id, created_at)
                    VALUES (:medicine_id, :delta, :reason, :ref_type, :ref_id, :created_at)
                    """
                ),
                {
                    "medicine_id": mid,
                    "delta": -quantity,
                    "reason": "student_purchase",
                    "ref_type": "medicine_order",
                    "ref_id": order.id,
                    "created_at": now_sh(),
                },
            )

        db.commit()
        return PharmacyService.get_order_by_id(db, student_id=student_id, order_id=int(order.id))

    @staticmethod
    def list_orders(
        db: Session,
        *,
        student_id: int,
        page: int,
        page_size: int,
        status: str | None = None,
        keyword: str | None = None,
        date_from: str | None = None,
        date_to: str | None = None,
    ) -> dict:
        where = ["student_id = :student_id"]
        params: dict[str, object] = {"student_id": student_id}

        if status:
            where.append("status = :status")
            params["status"] = status
        if keyword:
            where.append("order_no LIKE :keyword")
            params["keyword"] = f"%{keyword}%"
        if date_from:
            where.append("DATE(created_at) >= :date_from")
            params["date_from"] = date_from
        if date_to:
            where.append("DATE(created_at) <= :date_to")
            params["date_to"] = date_to

        where_sql = " AND ".join(where)
        total = db.execute(text(f"SELECT COUNT(*) FROM medicine_orders WHERE {where_sql}"), params).scalar() or 0

        params.update({"offset": (page - 1) * page_size, "limit": page_size})
        orders = db.execute(
            text(
                f"""
                SELECT id, order_no, status, total_amount, created_at
                FROM medicine_orders
                WHERE {where_sql}
                ORDER BY created_at DESC
                LIMIT :offset, :limit
                """
            ),
            params,
        ).mappings().all()

        order_ids = [int(o["id"]) for o in orders]
        item_rows = []
        if order_ids:
            item_rows = db.execute(
                text(
                    """
                    SELECT id, order_id, medicine_id, medicine_name_snapshot, spec_snapshot,
                           unit, unit_price, quantity, total_price
                    FROM medicine_order_items
                    WHERE order_id IN :order_ids
                    ORDER BY id ASC
                    """
                ).bindparams(bindparam("order_ids", expanding=True)),
                {"order_ids": order_ids},
            ).mappings().all()

        items_by_order: dict[int, list[dict]] = {oid: [] for oid in order_ids}
        for item in item_rows:
            items_by_order[int(item["order_id"])].append(
                {
                    "id": int(item["id"]),
                    "medicine_id": int(item["medicine_id"]) if item["medicine_id"] is not None else None,
                    "medicine_name": item["medicine_name_snapshot"],
                    "specification": item["spec_snapshot"],
                    "quantity": int(item["quantity"]),
                    "unit": item["unit"],
                    "price": float(item["unit_price"] or 0),
                }
            )

        result = []
        for order in orders:
            oid = int(order["id"])
            result.append(
                {
                    "id": oid,
                    "order_no": order["order_no"],
                    "purchase_date": order["created_at"],
                    "total_amount": float(order["total_amount"] or 0),
                    "status": order["status"],
                    "items": items_by_order.get(oid, []),
                }
            )

        return {
            "items": result,
            "total": int(total),
            "page": page,
            "page_size": page_size,
            "total_pages": ceil(total / page_size) if total else 0,
        }

    @staticmethod
    def get_order_by_id(db: Session, *, student_id: int, order_id: int) -> dict:
        order = db.execute(
            text(
                """
                SELECT id, order_no, status, total_amount, created_at
                FROM medicine_orders
                WHERE id = :order_id AND student_id = :student_id
                """
            ),
            {"order_id": order_id, "student_id": student_id},
        ).mappings().first()

        if not order:
            raise RuntimeError("订单不存在")

        items = db.execute(
            text(
                """
                SELECT id, medicine_id, medicine_name_snapshot, spec_snapshot,
                       unit, unit_price, quantity, total_price
                FROM medicine_order_items
                WHERE order_id = :order_id
                ORDER BY id ASC
                """
            ),
            {"order_id": order_id},
        ).mappings().all()

        return {
            "id": int(order["id"]),
            "order_no": order["order_no"],
            "purchase_date": order["created_at"],
            "total_amount": float(order["total_amount"] or 0),
            "status": order["status"],
            "items": [
                {
                    "id": int(item["id"]),
                    "medicine_id": int(item["medicine_id"]) if item["medicine_id"] is not None else None,
                    "medicine_name": item["medicine_name_snapshot"],
                    "specification": item["spec_snapshot"],
                    "quantity": int(item["quantity"]),
                    "unit": item["unit"],
                    "price": float(item["unit_price"] or 0),
                }
                for item in items
            ],
        }
