"""
数据管理服务 - 图片清理、日志记录、统计聚合
"""

import os
import uuid
import logging
from datetime import datetime, timedelta
from pathlib import Path
from typing import Optional, Dict, Any, List
from sqlalchemy.orm import Session
from sqlalchemy import func

from .models_extended import ApiLog, ImageRecord, DailyStats, User

logger = logging.getLogger(__name__)


class DataManager:
    """数据管理器"""
    
    def __init__(self, db: Session, upload_dir: str = "uploads"):
        self.db = db
        self.upload_dir = Path(upload_dir)
    
    # ==================== API 日志 ====================
    
    def log_api_call(
        self,
        endpoint: str,
        user_id: Optional[str] = None,
        device_id: Optional[str] = None,
        session_id: Optional[str] = None,
        method: str = "POST",
        request_size: int = 0,
        status_code: int = 200,
        response_time_ms: int = 0,
        food_count: Optional[int] = None,
        total_calories: Optional[float] = None,
        error_message: Optional[str] = None,
    ) -> ApiLog:
        """记录 API 调用"""
        log = ApiLog(
            id=str(uuid.uuid4()),
            user_id=user_id,
            device_id=device_id,
            session_id=session_id,
            endpoint=endpoint,
            method=method,
            request_size=request_size,
            status_code=status_code,
            response_time_ms=response_time_ms,
            food_count=food_count,
            total_calories=total_calories,
            error_message=error_message,
        )
        self.db.add(log)
        self.db.commit()
        return log
    
    # ==================== 图片管理 ====================
    
    def register_image(
        self,
        filename: str,
        storage_path: str,
        user_id: Optional[str] = None,
        session_id: Optional[str] = None,
        file_size: int = 0,
        retention_days: int = 30,
    ) -> ImageRecord:
        """注册上传的图片"""
        expires_at = datetime.utcnow() + timedelta(days=retention_days)
        
        record = ImageRecord(
            id=str(uuid.uuid4()),
            user_id=user_id,
            session_id=session_id,
            filename=filename,
            storage_path=storage_path,
            file_size=file_size,
            retention_days=retention_days,
            expires_at=expires_at,
        )
        self.db.add(record)
        self.db.commit()
        return record
    
    def cleanup_expired_images(self, dry_run: bool = False) -> Dict[str, Any]:
        """清理过期图片"""
        now = datetime.utcnow()
        expired = self.db.query(ImageRecord).filter(
            ImageRecord.status == "active",
            ImageRecord.expires_at < now
        ).all()
        
        result = {
            "total_expired": len(expired),
            "deleted_count": 0,
            "freed_bytes": 0,
            "errors": []
        }
        
        for record in expired:
            try:
                file_path = self.upload_dir / record.storage_path
                if file_path.exists() and not dry_run:
                    file_path.unlink()
                    result["freed_bytes"] += record.file_size
                
                if not dry_run:
                    record.status = "deleted"
                    record.deleted_at = now
                
                result["deleted_count"] += 1
                
            except Exception as e:
                result["errors"].append(f"{record.filename}: {str(e)}")
        
        if not dry_run:
            self.db.commit()
        
        logger.info(f"图片清理: 删除 {result['deleted_count']} 个，释放 {result['freed_bytes'] / 1024 / 1024:.2f} MB")
        return result
    
    def archive_old_images(self, days_old: int = 7) -> int:
        """归档旧图片（移动到归档目录）"""
        cutoff = datetime.utcnow() - timedelta(days=days_old)
        count = self.db.query(ImageRecord).filter(
            ImageRecord.status == "active",
            ImageRecord.created_at < cutoff
        ).update({"status": "archived", "archived_at": datetime.utcnow()})
        self.db.commit()
        return count
    
    def get_storage_stats(self) -> Dict[str, Any]:
        """获取存储统计"""
        stats = self.db.query(
            ImageRecord.status,
            func.count(ImageRecord.id).label("count"),
            func.sum(ImageRecord.file_size).label("total_size")
        ).group_by(ImageRecord.status).all()
        
        result = {"by_status": {}}
        total_size = 0
        total_count = 0
        
        for status, count, size in stats:
            size = size or 0
            result["by_status"][status] = {
                "count": count,
                "size_mb": round(size / 1024 / 1024, 2)
            }
            total_size += size
            total_count += count
        
        result["total_count"] = total_count
        result["total_size_mb"] = round(total_size / 1024 / 1024, 2)
        
        # 检查实际磁盘使用
        if self.upload_dir.exists():
            actual_size = sum(f.stat().st_size for f in self.upload_dir.rglob("*") if f.is_file())
            result["disk_usage_mb"] = round(actual_size / 1024 / 1024, 2)
        
        return result
    
    # ==================== 统计聚合 ====================
    
    def aggregate_daily_stats(self, date: Optional[str] = None, user_id: Optional[str] = None) -> DailyStats:
        """聚合每日统计"""
        if date is None:
            date = datetime.utcnow().strftime("%Y-%m-%d")
        
        start = datetime.strptime(date, "%Y-%m-%d")
        end = start + timedelta(days=1)
        
        # 查询当日 API 日志
        query = self.db.query(ApiLog).filter(
            ApiLog.created_at >= start,
            ApiLog.created_at < end
        )
        if user_id:
            query = query.filter(ApiLog.user_id == user_id)
        
        logs = query.all()
        
        # 计算统计
        api_calls = len(logs)
        total_calories = sum(log.total_calories or 0 for log in logs)
        vision_logs = [l for l in logs if "vision" in l.endpoint]
        meals_recorded = len(vision_logs)
        avg_calories = total_calories / meals_recorded if meals_recorded > 0 else 0
        
        def _group_from_endpoint(endpoint: str) -> str:
            if not endpoint:
                return "unknown"
            path = endpoint.split("?", 1)[0].strip()
            parts = [p for p in path.split("/") if p]
            if len(parts) >= 3 and parts[0] == "api" and parts[1].startswith("v"):
                return parts[2]
            if parts:
                return parts[0]
            return "unknown"

        # API 分类统计（按 /api/v1/<group> 聚合）
        category_dist = {}
        for log in logs:
            group = _group_from_endpoint(log.endpoint)
            category_dist[group] = category_dist.get(group, 0) + 1
        
        # 性能指标
        response_times = [log.response_time_ms for log in logs if log.response_time_ms]
        avg_response = sum(response_times) / len(response_times) if response_times else 0
        error_count = len([l for l in logs if l.status_code >= 400])
        error_rate = error_count / api_calls if api_calls > 0 else 0
        
        # 创建或更新统计记录
        stat_id = f"{user_id or 'global'}_{date}"
        stats = self.db.query(DailyStats).filter(DailyStats.id == stat_id).first()
        
        if not stats:
            stats = DailyStats(id=stat_id, user_id=user_id, date=date)
            self.db.add(stats)
        
        stats.api_calls = api_calls
        stats.meals_recorded = meals_recorded
        stats.total_calories = total_calories
        stats.avg_calories_per_meal = avg_calories
        stats.category_distribution = category_dist
        stats.avg_response_time_ms = avg_response
        stats.error_rate = error_rate
        
        self.db.commit()
        return stats
    
    # ==================== 用户数据导出 ====================
    
    def export_user_data(self, user_id: str) -> Dict[str, Any]:
        """导出用户数据（GDPR 合规）"""
        user = self.db.query(User).filter(User.id == user_id).first()
        if not user:
            return {"error": "User not found"}
        
        # 获取用户的所有数据
        logs = self.db.query(ApiLog).filter(ApiLog.user_id == user_id).all()
        images = self.db.query(ImageRecord).filter(ImageRecord.user_id == user_id).all()
        stats = self.db.query(DailyStats).filter(DailyStats.user_id == user_id).all()
        
        return {
            "user": {
                "id": user.id,
                "nickname": user.nickname,
                "created_at": user.created_at.isoformat() if user.created_at else None,
                "total_meals": user.total_meals,
                "total_calories": user.total_calories,
            },
            "api_logs": [
                {
                    "endpoint": log.endpoint,
                    "created_at": log.created_at.isoformat() if log.created_at else None,
                    "food_count": log.food_count,
                    "total_calories": log.total_calories,
                }
                for log in logs
            ],
            "images": [
                {
                    "filename": img.filename,
                    "created_at": img.created_at.isoformat() if img.created_at else None,
                    "status": img.status,
                }
                for img in images
            ],
            "daily_stats": [
                {
                    "date": stat.date,
                    "meals_recorded": stat.meals_recorded,
                    "total_calories": stat.total_calories,
                }
                for stat in stats
            ],
            "exported_at": datetime.utcnow().isoformat(),
        }
    
    def delete_user_data(self, user_id: str, delete_images: bool = True) -> Dict[str, int]:
        """删除用户数据（GDPR 合规）"""
        result = {"logs": 0, "images": 0, "stats": 0}
        
        # 删除图片文件
        if delete_images:
            images = self.db.query(ImageRecord).filter(ImageRecord.user_id == user_id).all()
            for img in images:
                try:
                    file_path = self.upload_dir / img.storage_path
                    if file_path.exists():
                        file_path.unlink()
                except Exception:
                    pass
            result["images"] = len(images)
        
        # 删除数据库记录
        result["logs"] = self.db.query(ApiLog).filter(ApiLog.user_id == user_id).delete()
        self.db.query(ImageRecord).filter(ImageRecord.user_id == user_id).delete()
        result["stats"] = self.db.query(DailyStats).filter(DailyStats.user_id == user_id).delete()
        
        self.db.commit()
        return result


# ==================== 安全配置 ====================

class SecurityConfig:
    """安全配置"""
    
    # 图片保留策略
    IMAGE_RETENTION_DAYS = 30  # 默认保留30天
    IMAGE_MAX_SIZE_MB = 10     # 最大图片大小
    
    # 敏感字段（不记录到日志）
    SENSITIVE_FIELDS = [
        "password", "token", "api_key", "secret",
        "birth_date", "phone", "email", "address"
    ]
    
    # 允许的图片格式
    ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "webp", "heic"}
    
    # API 限流
    RATE_LIMIT_PER_MINUTE = 60
    RATE_LIMIT_PER_DAY = 1000
    
    @classmethod
    def sanitize_log_data(cls, data: Dict[str, Any]) -> Dict[str, Any]:
        """清理日志数据中的敏感信息"""
        if not isinstance(data, dict):
            return data
        
        sanitized = {}
        for key, value in data.items():
            if any(s in key.lower() for s in cls.SENSITIVE_FIELDS):
                sanitized[key] = "[REDACTED]"
            elif isinstance(value, dict):
                sanitized[key] = cls.sanitize_log_data(value)
            else:
                sanitized[key] = value
        return sanitized
