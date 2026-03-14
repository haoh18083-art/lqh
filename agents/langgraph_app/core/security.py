"""JWT helpers and auth dependency."""
from typing import Any

from jose import JWTError, jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import text
from sqlalchemy.orm import Session

from core.config import settings
from db.mysql import get_db


oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")


def verify_token(token: str) -> dict[str, Any]:
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
    except JWTError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="无效认证令牌") from exc

    if payload.get("type") not in {None, "access"}:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="令牌类型错误")
    return payload


def get_current_student_context(
    token: str = Depends(oauth2_scheme), db: Session = Depends(get_db)
) -> dict[str, int]:
    payload = verify_token(token)
    user_id_raw = payload.get("sub")
    if not user_id_raw:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="令牌缺少用户信息")

    user_id = int(user_id_raw)
    row = db.execute(
        text(
            """
            SELECT u.id AS user_id, s.id AS student_id
            FROM users u
            JOIN students s ON s.user_id = u.id
            WHERE u.id = :user_id AND u.role = 'student' AND u.is_active = 1
            """
        ),
        {"user_id": user_id},
    ).mappings().first()

    if not row:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="当前用户无学生权限")

    return {"user_id": int(row["user_id"]), "student_id": int(row["student_id"])}
