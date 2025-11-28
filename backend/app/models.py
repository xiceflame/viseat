"""
数据模型 - 完整的用户数据关联体系

数据关系图:
┌──────────┐
│   User   │ ← 用户（核心）
└────┬─────┘
     │ 1:N
     ├──────────────┬──────────────┐
     ▼              ▼              ▼
┌─────────┐   ┌──────────┐   ┌──────────┐
│ Device  │   │DailyStats│   │ApiLog   │
└─────────┘   └──────────┘   └──────────┘

┌──────────┐
│   User   │
└────┬─────┘
     │ 1:N
     ▼
┌────────────┐
│MealSession │ ← 用餐会话
└────┬───────┘
     │ 1:N
     ▼
┌────────────┐     ┌─────────────────┐
│MealSnapshot│────►│ NutritionAdvice │ ← 营养建议
└────┬───────┘     └─────────────────┘
     │ 1:N
     ▼
┌────────────┐
│SnapshotFood│ ← 识别的食物
└────────────┘
"""

from sqlalchemy import Column, String, DateTime, Float, Integer, ForeignKey, JSON, Text, Boolean, Index
from sqlalchemy.orm import relationship
from datetime import datetime
from .db import Base


# ==================== 用户与设备 ====================

class User(Base):
    """用户 - 核心实体，所有数据的归属
    
    对应前端 UserProfile 字段:
    - age, gender, height, weight, bmi
    - activityLevel (sedentary/light/moderate/active/very_active)
    - healthGoal (lose_weight/gain_muscle/maintain), targetWeight
    - healthConditions (糖尿病、高血压、高血脂等)
    - dietaryPreferences (低油、低盐、素食等)
    """
    __tablename__ = "users"

    id = Column(String, primary_key=True, index=True)  # UUID
    
    # 设备标识（首次注册时生成）
    device_id = Column(String, unique=True, index=True)  # 眼镜设备ID
    phone_id = Column(String, index=True, nullable=True)  # 手机设备ID
    
    # 基本信息（对应前端 UserProfile）
    nickname = Column(String, nullable=True)
    avatar_url = Column(String, nullable=True)
    gender = Column(String, nullable=True)  # male/female/other
    age = Column(Integer, nullable=True)  # 年龄
    birth_year = Column(Integer, nullable=True)  # 出生年份（备用）
    height_cm = Column(Float, nullable=True)  # 身高(cm)
    weight_kg = Column(Float, nullable=True)  # 体重(kg)
    bmi = Column(Float, nullable=True)  # BMI（自动计算）
    
    # 活动量（对应前端 activityLevel）
    activity_level = Column(String, default="moderate")  # sedentary/light/moderate/active/very_active
    
    # 健康目标（对应前端 healthGoal）
    health_goal = Column(String, default="maintain")  # lose_weight/gain_muscle/maintain
    target_weight = Column(Float, nullable=True)  # 目标体重
    
    # 每日营养目标（根据个人信息计算）
    target_calories = Column(Float, default=2000.0)
    target_protein = Column(Float, default=60.0)
    target_carbs = Column(Float, default=250.0)
    target_fat = Column(Float, default=65.0)
    
    # 健康状况（对应前端 healthConditions）
    # ["diabetes", "hypertension", "hyperlipidemia", "gout", "kidney_disease"]
    health_conditions = Column(JSON, nullable=True)
    
    # 饮食偏好（对应前端 dietaryPreferences）
    # ["low_oil", "low_salt", "low_sugar", "vegetarian", "vegan", "halal"]
    dietary_preferences = Column(JSON, nullable=True)
    allergies = Column(JSON, nullable=True)  # 过敏原 ["peanut", "seafood", "dairy"]
    
    # 累计统计
    total_meals = Column(Integer, default=0)
    total_calories = Column(Float, default=0.0)
    total_days_active = Column(Integer, default=0)
    
    # 时间戳
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    last_active_at = Column(DateTime, default=datetime.utcnow)
    
    # 关系
    sessions = relationship("MealSession", back_populates="user", cascade="all, delete-orphan")
    daily_stats = relationship("DailyNutrition", back_populates="user", cascade="all, delete-orphan")
    
    def calculate_bmi(self):
        """计算 BMI"""
        if self.height_cm and self.weight_kg and self.height_cm > 0:
            height_m = self.height_cm / 100
            self.bmi = round(self.weight_kg / (height_m ** 2), 1)
            return self.bmi
        return None
    
    def calculate_target_calories(self):
        """根据个人信息计算每日目标热量（基于 Mifflin-St Jeor 公式）"""
        if not all([self.age, self.height_cm, self.weight_kg, self.gender]):
            return self.target_calories
        
        # 基础代谢率 (BMR)
        if self.gender == "male":
            bmr = 10 * self.weight_kg + 6.25 * self.height_cm - 5 * self.age + 5
        else:
            bmr = 10 * self.weight_kg + 6.25 * self.height_cm - 5 * self.age - 161
        
        # 活动系数
        activity_factors = {
            "sedentary": 1.2,      # 久坐
            "light": 1.375,        # 轻度活动
            "moderate": 1.55,      # 中度活动
            "active": 1.725,       # 高度活动
            "very_active": 1.9     # 极高活动
        }
        factor = activity_factors.get(self.activity_level, 1.55)
        
        # 总消耗热量 (TDEE)
        tdee = bmr * factor
        
        # 根据健康目标调整
        if self.health_goal == "lose_weight":
            self.target_calories = round(tdee - 500, 0)  # 减重：减少500卡
        elif self.health_goal == "gain_muscle":
            self.target_calories = round(tdee + 300, 0)  # 增肌：增加300卡
        else:
            self.target_calories = round(tdee, 0)  # 维持
        
        # 计算宏量营养素目标
        self.target_protein = round(self.weight_kg * 1.6, 0)  # 蛋白质: 1.6g/kg
        self.target_fat = round(self.target_calories * 0.25 / 9, 0)  # 脂肪: 25%热量
        self.target_carbs = round((self.target_calories - self.target_protein * 4 - self.target_fat * 9) / 4, 0)
        
        return self.target_calories
    
    def to_profile_dict(self):
        """转换为前端 UserProfile 格式"""
        return {
            "user_id": self.id,
            "device_id": self.device_id,
            "nickname": self.nickname,
            "gender": self.gender,
            "age": self.age,
            "height": self.height_cm,
            "weight": self.weight_kg,
            "bmi": self.bmi,
            "activity_level": self.activity_level,
            "health_goal": self.health_goal,
            "target_weight": self.target_weight,
            "target_calories": self.target_calories,
            "target_protein": self.target_protein,
            "target_carbs": self.target_carbs,
            "target_fat": self.target_fat,
            "health_conditions": self.health_conditions or [],
            "dietary_preferences": self.dietary_preferences or [],
            "allergies": self.allergies or [],
            "total_meals": self.total_meals,
            "total_calories": self.total_calories,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }


class Device(Base):
    """设备注册 - 眼镜/手机"""
    __tablename__ = "devices"

    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, ForeignKey("users.id"), index=True, nullable=True)
    
    device_type = Column(String, nullable=False)  # glasses/phone
    device_model = Column(String, nullable=True)  # Rokid Max Pro
    os_version = Column(String, nullable=True)
    app_version = Column(String, nullable=True)
    
    # 配对信息
    paired_device_id = Column(String, nullable=True)
    
    is_active = Column(Boolean, default=True)
    last_seen_at = Column(DateTime, default=datetime.utcnow)
    created_at = Column(DateTime, default=datetime.utcnow)


# ==================== 用餐会话 ====================

class MealSession(Base):
    """用餐会话 - 一次完整的用餐过程"""
    __tablename__ = "meal_sessions"

    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, ForeignKey("users.id"), index=True, nullable=False)
    
    # 会话信息
    meal_type = Column(String, default="meal")  # breakfast/lunch/dinner/snack
    status = Column(String, default="active", index=True)  # active/paused/completed
    
    # 时间
    start_time = Column(DateTime, default=datetime.utcnow)
    end_time = Column(DateTime, nullable=True)
    duration_minutes = Column(Integer, nullable=True)
    
    # 配置
    auto_capture_interval = Column(Integer, default=300)  # 自动拍照间隔（秒）
    
    # 汇总数据（会话结束时计算）
    total_calories = Column(Float, default=0.0)
    total_protein = Column(Float, default=0.0)
    total_carbs = Column(Float, default=0.0)
    total_fat = Column(Float, default=0.0)
    food_count = Column(Integer, default=0)
    
    # 基线数据（用于增量计算）
    baseline_foods = Column(JSON, nullable=True)  # 首次识别的食物列表
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # 关系
    user = relationship("User", back_populates="sessions")
    snapshots = relationship("MealSnapshot", back_populates="session", cascade="all, delete-orphan")
    advices = relationship("NutritionAdvice", back_populates="session", cascade="all, delete-orphan")
    
    __table_args__ = (
        Index('idx_session_user_time', 'user_id', 'start_time'),
    )


class MealSnapshot(Base):
    """用餐快照 - 会话中的每次拍照识别"""
    __tablename__ = "meal_snapshots"

    id = Column(String, primary_key=True, index=True)
    session_id = Column(String, ForeignKey("meal_sessions.id", ondelete="CASCADE"), index=True, nullable=False)
    
    # 图片信息
    image_url = Column(Text, default="")
    image_path = Column(String, nullable=True)  # 本地存储路径
    image_size = Column(Integer, default=0)  # 文件大小（bytes）
    
    # 识别信息
    snapshot_type = Column(String, default="manual")  # manual/auto/baseline
    captured_at = Column(DateTime, default=datetime.utcnow, index=True)
    
    # VLM 结果
    model = Column(String, default="qwen-vl-max")
    raw_json = Column(JSON, nullable=True)  # 原始 VLM 返回
    processing_time_ms = Column(Integer, default=0)
    
    # 营养汇总
    total_calories = Column(Float, default=0.0)
    total_protein = Column(Float, default=0.0)
    total_carbs = Column(Float, default=0.0)
    total_fat = Column(Float, default=0.0)
    
    # 增量计算（相对上一次快照）
    delta_calories = Column(Float, default=0.0)  # 本次消耗的热量
    
    # 关系
    session = relationship("MealSession", back_populates="snapshots")
    foods = relationship("SnapshotFood", back_populates="snapshot", cascade="all, delete-orphan")


class SnapshotFood(Base):
    """快照中的食物 - 每个识别出的食材"""
    __tablename__ = "snapshot_foods"

    id = Column(String, primary_key=True, index=True)
    snapshot_id = Column(String, ForeignKey("meal_snapshots.id", ondelete="CASCADE"), index=True, nullable=False)
    
    # 食物信息
    name = Column(String, nullable=False)  # 英文名
    chinese_name = Column(String, nullable=True)
    category = Column(String, nullable=True)  # meal/snack/beverage/fruit
    cooking_method = Column(String, nullable=True)  # raw/steam/stir-fry/deep-fry
    
    # 重量与营养
    weight_g = Column(Float, default=0.0)
    calories_kcal = Column(Float, default=0.0)
    protein_g = Column(Float, default=0.0)
    carbs_g = Column(Float, default=0.0)
    fat_g = Column(Float, default=0.0)
    
    # 识别置信度
    confidence = Column(Float, default=0.0)
    bbox = Column(JSON, nullable=True)  # 边界框 [x, y, w, h]
    
    # 营养数据来源
    nutrition_source = Column(String, nullable=True)  # china/usda/foodstruct/fallback
    
    # 用户编辑标记
    is_edited = Column(Boolean, default=False)  # 是否被用户编辑过
    edited_at = Column(DateTime, nullable=True)  # 编辑时间
    
    # 关系
    snapshot = relationship("MealSnapshot", back_populates="foods")


# ==================== 营养建议 ====================

class NutritionAdvice(Base):
    """营养建议 - AI 生成的饮食建议"""
    __tablename__ = "nutrition_advices"

    id = Column(String, primary_key=True, index=True)
    session_id = Column(String, ForeignKey("meal_sessions.id", ondelete="CASCADE"), index=True, nullable=True)
    user_id = Column(String, ForeignKey("users.id"), index=True, nullable=False)
    
    # 建议类型
    advice_type = Column(String, default="meal")  # meal/daily/weekly
    trigger = Column(String, nullable=True)  # meal_end/over_target/user_request
    
    # 建议内容
    summary = Column(Text, nullable=True)  # 简短总结
    details = Column(JSON, nullable=True)  # 详细建议列表
    warnings = Column(JSON, nullable=True)  # 警告（如超标提醒）
    suggestions = Column(JSON, nullable=True)  # 改善建议
    
    # 上下文
    context_data = Column(JSON, nullable=True)  # 生成建议时的营养数据
    
    # 用户反馈
    is_helpful = Column(Boolean, nullable=True)  # 用户评价
    user_feedback = Column(Text, nullable=True)
    
    created_at = Column(DateTime, default=datetime.utcnow)
    
    # 关系
    session = relationship("MealSession", back_populates="advices")


# ==================== 每日统计 ====================

class DailyNutrition(Base):
    """每日营养统计 - 按天聚合"""
    __tablename__ = "daily_nutrition"

    id = Column(String, primary_key=True)  # user_id_date
    user_id = Column(String, ForeignKey("users.id"), index=True, nullable=False)
    date = Column(String, index=True, nullable=False)  # 2025-11-26
    
    # 用餐次数
    breakfast_count = Column(Integer, default=0)
    lunch_count = Column(Integer, default=0)
    dinner_count = Column(Integer, default=0)
    snack_count = Column(Integer, default=0)
    
    # 营养摄入
    total_calories = Column(Float, default=0.0)
    total_protein = Column(Float, default=0.0)
    total_carbs = Column(Float, default=0.0)
    total_fat = Column(Float, default=0.0)
    
    # 目标完成度（百分比）
    calories_pct = Column(Float, default=0.0)
    protein_pct = Column(Float, default=0.0)
    carbs_pct = Column(Float, default=0.0)
    fat_pct = Column(Float, default=0.0)
    
    # Top 食物
    top_foods = Column(JSON, nullable=True)  # [{"name": "rice", "calories": 300}]
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # 关系
    user = relationship("User", back_populates="daily_stats")
    
    __table_args__ = (
        Index('idx_daily_user_date', 'user_id', 'date'),
    )


# ==================== API 调用日志 ====================

class ApiLog(Base):
    """API 调用日志 - 用于分析和调试"""
    __tablename__ = "api_logs"
    
    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, index=True, nullable=True)
    device_id = Column(String, index=True, nullable=True)
    session_id = Column(String, index=True, nullable=True)
    
    # 请求
    endpoint = Column(String, index=True, nullable=False)
    method = Column(String, default="POST")
    request_size = Column(Integer, default=0)
    
    # 响应
    status_code = Column(Integer, default=200)
    response_time_ms = Column(Integer, default=0)
    error_message = Column(Text, nullable=True)
    
    # 业务数据（脱敏）
    food_count = Column(Integer, nullable=True)
    total_calories = Column(Float, nullable=True)
    
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
    
    __table_args__ = (
        Index('idx_api_logs_user_date', 'user_id', 'created_at'),
    )
