from datetime import datetime

from sqlalchemy import Column, DateTime, Float, Index, Integer, JSON, String

from .db import Base
from .models import ApiLog, User


class ImageRecord(Base):
    __tablename__ = "image_records"

    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, index=True, nullable=True)
    session_id = Column(String, index=True, nullable=True)

    filename = Column(String, nullable=False)
    storage_path = Column(String, nullable=False)
    file_size = Column(Integer, default=0)

    retention_days = Column(Integer, default=30)
    expires_at = Column(DateTime, nullable=True, index=True)

    status = Column(String, default="active", index=True)
    archived_at = Column(DateTime, nullable=True)
    deleted_at = Column(DateTime, nullable=True)

    # 识别结果
    analysis_result = Column(JSON, nullable=True)

    created_at = Column(DateTime, default=datetime.utcnow, index=True)

    __table_args__ = (
        Index("idx_image_records_user_created_at", "user_id", "created_at"),
    )


class DailyStats(Base):
    __tablename__ = "daily_stats"

    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, index=True, nullable=True)
    date = Column(String, index=True, nullable=False)

    api_calls = Column(Integer, default=0)
    meals_recorded = Column(Integer, default=0)
    total_calories = Column(Float, default=0.0)
    avg_calories_per_meal = Column(Float, default=0.0)
    category_distribution = Column(JSON, nullable=True)
    avg_response_time_ms = Column(Float, default=0.0)
    error_rate = Column(Float, default=0.0)

    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    __table_args__ = (
        Index("idx_daily_stats_user_date", "user_id", "date"),
    )
