from fastapi import FastAPI, HTTPException, Depends, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import os
from datetime import datetime
import shutil
from dotenv import load_dotenv
from sqlalchemy.orm import Session
from pathlib import Path
import csv
import logging
import uuid
from typing import Optional, List, Dict, Any
import requests
import json
from openai import OpenAI
from pydantic import BaseModel

# 导入营养数据库模块
from .nutrition_db import get_nutrition_db, lookup_nutrition as db_lookup_nutrition, NutritionDatabase

# 加载环境变量
load_dotenv()

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 确保上传目录存在
UPLOAD_DIR = Path("uploads")
UPLOAD_DIR.mkdir(exist_ok=True)

# 创建FastAPI应用
app = FastAPI(
    title="Rokid Nutrition API",
    description="智能营养助手后端服务",
    version="1.0.0"
)

# 挂载静态文件目录，用于访问上传的图片
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")

from .db import get_db, init_db
from . import models

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应该限制具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 外部服务配置
OPEN_FOOD_FACTS_BASE_URL = os.getenv("OPEN_FOOD_FACTS_BASE_URL", "https://world.openfoodfacts.org")

QWEN_API_KEY = os.getenv("QWEN_API_KEY") or os.getenv("DASHSCOPE_API_KEY")
QWEN_BASE_URL = os.getenv("QWEN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
QWEN_VL_MODEL = os.getenv("QWEN_VL_MODEL", "qwen3-vl-plus")
QWEN_TEXT_MODEL = os.getenv("QWEN_TEXT_MODEL", "qwen-plus")

_qwen_client: Optional[OpenAI] = None
if QWEN_API_KEY:
    _qwen_client = OpenAI(api_key=QWEN_API_KEY, base_url=QWEN_BASE_URL)
else:
    logger.warning("QWEN_API_KEY / DASHSCOPE_API_KEY 未配置，Qwen 相关接口不可用。")


def get_qwen_client() -> OpenAI:
    if not _qwen_client:
        raise RuntimeError("QWEN_API_KEY / DASHSCOPE_API_KEY 未配置，Qwen 接口不可用")
    return _qwen_client


@app.on_event("startup")
def _startup():
    init_db()
    # 初始化多数据源营养数据库
    nutrition_db = get_nutrition_db()
    stats = nutrition_db.get_stats()
    logger.info(f"营养数据库已加载: {stats['total_foods']} 种食物")
    for source, count in stats['sources'].items():
        logger.info(f"  - {source}: {count} 种")
    # 兼容旧代码：同时加载旧的 CSV
    load_external_nutrition_db()

# 简化的营养数据库（后续可扩展为真实数据库）
NUTRITION_DB = {
    "米饭": {"calories": 116, "protein": 2.6, "carbs": 25.9, "fat": 0.3},
    "白米饭": {"calories": 116, "protein": 2.6, "carbs": 25.9, "fat": 0.3},
    "鸡胸肉": {"calories": 165, "protein": 31, "carbs": 0, "fat": 3.6},
    "西兰花": {"calories": 34, "protein": 2.8, "carbs": 7, "fat": 0.4},
    "牛肉": {"calories": 250, "protein": 26, "carbs": 0, "fat": 15},
    "猪肉": {"calories": 242, "protein": 27, "carbs": 0, "fat": 14},
    "鸡蛋": {"calories": 143, "protein": 13, "carbs": 1.1, "fat": 9},
    "番茄": {"calories": 18, "protein": 0.9, "carbs": 3.9, "fat": 0.2},
    "黄瓜": {"calories": 15, "protein": 0.7, "carbs": 3.6, "fat": 0.1},
    "土豆": {"calories": 77, "protein": 2, "carbs": 17, "fat": 0.1},
    "面条": {"calories": 137, "protein": 4.5, "carbs": 28, "fat": 0.5},
    "馒头": {"calories": 221, "protein": 7, "carbs": 47, "fat": 1.1},
    "豆腐": {"calories": 76, "protein": 8, "carbs": 4.3, "fat": 3.7},
    "牛奶": {"calories": 54, "protein": 3, "carbs": 5, "fat": 3.2},
    "苹果": {"calories": 52, "protein": 0.3, "carbs": 14, "fat": 0.2},
    "香蕉": {"calories": 89, "protein": 1.1, "carbs": 23, "fat": 0.3},
}

NUTRITION_DB_EXT: Dict[str, Dict[str, float]] = {}
OFF_CACHE: Dict[str, Dict[str, float]] = {}

def normalize_food_key(s: str) -> str:
    return s.strip().lower()

SYNONYMS = {
    # 主食
    "rice": "白米饭",
    "white rice": "白米饭",
    "steamed rice": "白米饭",
    "noodles": "面条",
    "noodle": "面条",
    
    # 肉类 - 通用名映射到具体肉类
    "meat": "pork",  # 通用肉类默认猪肉
    "pork": "pork",
    "beef": "牛肉",
    "chicken": "chicken meat",
    "chicken breast": "鸡胸肉",
    "chicken meat": "chicken meat",
    
    # 蔬菜
    "green vegetable": "chinese cabbage",  # 通用绿叶菜 → 白菜/青菜
    "green vegetables": "chinese cabbage",
    "leafy greens": "chinese cabbage",
    "vegetable": "chinese cabbage",
    "vegetables": "chinese cabbage",
    "bok choy": "chinese cabbage",
    "cabbage": "cabbage",
    "broccoli": "西兰花",
    "spinach": "spinach",
    
    # 其他
    "egg": "鸡蛋",
    "eggs": "鸡蛋",
    "tofu": "豆腐",
    "tomato": "番茄",
    "cucumber": "黄瓜",
    "apple": "苹果",
    "banana": "香蕉",
    "milk": "牛奶",
}


def get_cooking_factor(method: Optional[str]) -> float:
    """根据烹饪方式返回热量加权系数，默认 1.0。

    这里同时支持中英文描述，例如：
    - raw/生食 -> 1.0
    - steam/steamed/清蒸/水煮 -> 1.0
    - braise/红烧/炖 -> 1.2
    - pan-fry/煎, stir-fry/炒 -> 1.3
    - deep-fry/油炸/炸 -> 2.0
    """
    if not method:
        return 1.0

    key = method.strip().lower()

    mapping = {
        # 生食/基础
        "raw": 1.0,
        "生食": 1.0,

        # 清淡烹饪
        "steam": 1.0,
        "steamed": 1.0,
        "清蒸": 1.0,
        "boil": 1.0,
        "boiled": 1.0,
        "water_boiled": 1.0,
        "水煮": 1.0,

        # 红烧/炖煮
        "braise": 1.2,
        "braised": 1.2,
        "stew": 1.2,
        "stewed": 1.2,
        "红烧": 1.2,
        "炖": 1.2,

        # 煎炒
        "pan_fry": 1.3,
        "pan-fry": 1.3,
        "pan fried": 1.3,
        "fry": 1.3,
        "fried": 1.3,
        "stir_fry": 1.3,
        "stir-fry": 1.3,
        "stir fry": 1.3,
        "炒": 1.3,
        "煎": 1.3,

        # 深炸
        "deep_fry": 2.0,
        "deep-fry": 2.0,
        "deep fried": 2.0,
        "油炸": 2.0,
        "炸": 2.0,
    }

    return mapping.get(key, 1.0)

def load_external_nutrition_db():
    try:
        base = Path(__file__).resolve().parents[1] / "data"
        candidates = [
            base / "nutrition_db.csv",
            base / "nutrition_cn.csv",
            base / "nutrition_usda.csv",
            base / "foodstruct_nutritional_facts 2.csv",
        ]
        for fp in candidates:
            if fp.exists():
                with fp.open("r", encoding="utf-8") as f:
                    reader = csv.DictReader(f)
                    def _get_float(row: Dict[str, str], keys: List[str]) -> float:
                        for key in keys:
                            if key in row and row[key] not in (None, ""):
                                try:
                                    return float(row[key])
                                except Exception:
                                    continue
                        return 0.0

                    for row in reader:
                        name = (
                            row.get("name")
                            or row.get("Food Name")
                            or row.get("food")
                            or row.get("Food")
                        )
                        if not name:
                            continue
                        k = normalize_food_key(name)
                        try:
                            calories = _get_float(row, [
                                "calories_per_100g",
                                "Calories",
                                "calories",
                            ])
                            protein = _get_float(row, [
                                "protein_per_100g",
                                "Protein",
                                "protein",
                            ])
                            carbs = _get_float(row, [
                                "carbs_per_100g",
                                "Carbs",
                                "carbohydrates",
                                "carbohydrate",
                                "Net carbs",
                            ])
                            fat = _get_float(row, [
                                "fat_per_100g",
                                "Fats",
                                "fat",
                                "Fat",
                            ])
                            NUTRITION_DB_EXT[k] = {
                                "calories": calories,
                                "protein": protein,
                                "carbs": carbs,
                                "fat": fat,
                            }
                        except Exception:
                            continue
    except Exception:
        pass


def get_off_nutrition_for_barcode(barcode: str) -> Optional[Dict[str, float]]:
    """从 Open Food Facts 查询条码对应的每100g营养信息"""
    if not barcode:
        return None
    if barcode in OFF_CACHE:
        return OFF_CACHE[barcode]
    try:
        url = f"{OPEN_FOOD_FACTS_BASE_URL.rstrip('/')}/api/v2/product/{barcode}.json"
        resp = requests.get(url, timeout=5)
        if resp.status_code != 200:
            return None
        data = resp.json()
        product = data.get("product") or {}
        nutr = product.get("nutriments") or {}

        def _first(*keys):
            for k in keys:
                if k in nutr and nutr[k] is not None:
                    return nutr[k]
            return None

        cal = _first("energy-kcal_100g", "energy-kcal_value", "energy_100g")
        protein = _first("proteins_100g")
        carbs = _first("carbohydrates_100g")
        fat = _first("fat_100g")
        if cal is None:
            return None
        result = {
            "calories": float(cal),
            "protein": float(protein) if protein is not None else 0.0,
            "carbs": float(carbs) if carbs is not None else 0.0,
            "fat": float(fat) if fat is not None else 0.0,
        }
        OFF_CACHE[barcode] = result
        return result
    except Exception as e:
        logger.warning(f"查询 Open Food Facts 失败: {e}")
        return None

class FoodInput(BaseModel):
    name: str
    weight_g: float
    barcode: Optional[str] = None
    cooking_method: Optional[str] = None
    category: Optional[str] = None  # 食物分类：snack/meal/beverage/dessert/fruit
    calories_per_100g: Optional[float] = None
    protein_per_100g: Optional[float] = None
    carbs_per_100g: Optional[float] = None
    fat_per_100g: Optional[float] = None


class NutritionTotals(BaseModel):
    calories: float
    protein: float
    carbs: float
    fat: float


class SnapshotPayload(BaseModel):
    foods: List[FoodInput]
    nutrition: Optional[NutritionTotals] = None


# ==================== 智能建议系统模型 ====================

class MealContextPayload(BaseModel):
    """用餐上下文"""
    session_id: str
    start_time: Optional[int] = None  # 毫秒时间戳
    duration_minutes: float = 0  # 用餐时长（分钟）
    recognition_count: int = 0  # 识别次数
    total_consumed_so_far: float = 0  # 已消耗热量


class DailyContextPayload(BaseModel):
    """今日上下文"""
    total_calories_today: float = 0  # 今日总热量
    total_protein_today: float = 0  # 今日总蛋白质
    total_carbs_today: float = 0  # 今日总碳水
    total_fat_today: float = 0  # 今日总脂肪
    meal_count_today: int = 0  # 今日用餐次数
    last_meal_hours_ago: float = 0  # 距上次用餐小时数


class UserProfilePayload(BaseModel):
    """用户健康档案（用于请求）"""
    age: Optional[int] = None
    gender: Optional[str] = None  # "male" / "female"
    bmi: Optional[float] = None
    activity_level: Optional[str] = None  # "sedentary", "light", "moderate", "active"
    health_goal: Optional[str] = None  # "lose", "maintain", "gain"
    target_weight: Optional[float] = None
    target_calories: Optional[float] = None
    health_conditions: Optional[List[str]] = None  # ["糖尿病", "高血压"]
    dietary_preferences: Optional[List[str]] = None  # ["低盐", "素食"]


class MealEndRequest(BaseModel):
    """结束用餐请求（增强版）"""
    session_id: str
    final_snapshot: Optional[SnapshotPayload] = None
    meal_context: Optional[MealContextPayload] = None
    daily_context: Optional[DailyContextPayload] = None
    user_profile: Optional[UserProfilePayload] = None


class MealSummaryResponse(BaseModel):
    """用餐总结（用于眼镜显示）"""
    total_calories: float
    total_protein: float
    total_carbs: float
    total_fat: float
    duration_minutes: float
    rating: str  # "good" | "fair" | "poor"
    short_advice: str  # ≤20字符，用于眼镜显示


class MealAdviceResponse(BaseModel):
    """详细建议（用于手机显示）"""
    summary: str  # 一句话总结
    suggestions: List[str]  # 2-4条具体建议
    highlights: List[str]  # 本餐亮点
    warnings: List[str]  # 需要注意的问题


class NextMealSuggestionResponse(BaseModel):
    """下一餐建议"""
    recommended_time: str  # 如 "4小时后"
    meal_type: str  # 如 "晚餐"
    calorie_budget: float  # 建议热量
    focus_nutrients: List[str]  # 建议补充的营养
    avoid: List[str]  # 建议避免的食物


class BaselineFood(BaseModel):
    """基线食物信息"""
    dish_name: str
    dish_name_cn: str
    ingredients: List[Dict[str, Any]]  # [{"name_en": "pork", "weight_g": 100}]
    total_weight_g: float = 0


class VisionAnalyzeRequest(BaseModel):
    """视觉分析请求：接收图片 URL，调用 Qwen-VL 做多食材拆解。"""

    image_url: str
    question: Optional[str] = None
    locale: str = "zh"
    # 基线模式参数（用于会话中的 Update）
    mode: str = "start"  # "start" = 单图识别, "update" = 基于基线识别剩余/增量
    baseline_foods: Optional[List[BaselineFood]] = None  # 基线食物列表
    reference_image_url: Optional[str] = None  # 参考图片URL（双图对比模式）


def _food_input_to_dict(food: FoodInput) -> Dict[str, Any]:
    return {
        "name": food.name,
        "weight": food.weight_g,
        "weight_g": food.weight_g,
        "barcode": food.barcode,
        "cooking_method": food.cooking_method,
        "category": food.category,
        "calories_per_100g": food.calories_per_100g,
        "protein_per_100g": food.protein_per_100g,
        "carbs_per_100g": food.carbs_per_100g,
        "fat_per_100g": food.fat_per_100g,
    }


def _calc_food_calories(food: FoodInput) -> float:
    data = _food_input_to_dict(food)
    totals = calculate_nutrition([data])
    return totals["calories"]

 


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "Rokid Nutrition API",
        "status": "running",
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat()
    }


@app.get("/health")
async def health_check():
    """健康检查"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "nutrition_db_loaded": len(NUTRITION_DB_EXT) > 0
    }


# ==================== 用户注册 API ====================

class UserRegisterRequest(BaseModel):
    """用户注册请求"""
    device_id: str
    device_type: str = "phone"  # phone/glasses
    device_model: Optional[str] = None
    app_version: Optional[str] = None


@app.post("/api/v1/user/register")
async def register_user(req: UserRegisterRequest, db: Session = Depends(get_db)):
    """
    用户注册/登录接口
    
    - 如果 device_id 已存在，返回现有用户信息
    - 如果是新设备，创建新用户
    
    Returns:
        user_id: 用户唯一ID
        device_id: 设备ID
        is_new_user: 是否为新用户
        token: 认证令牌（当前为简化版本）
    """
    try:
        # 检查是否已存在用户
        existing_user = db.query(models.User).filter(
            models.User.device_id == req.device_id
        ).first()
        
        if existing_user:
            # 更新最后活跃时间
            existing_user.last_active_at = datetime.utcnow()
            db.commit()
            
            logger.info(f"用户登录: user_id={existing_user.id}, device_id={req.device_id}")
            return {
                "user_id": existing_user.id,
                "device_id": req.device_id,
                "is_new_user": False,
                "token": f"token_{existing_user.id}",  # 简化版本的 token
                "message": "登录成功"
            }
        
        # 创建新用户
        user_id = str(uuid.uuid4())
        new_user = models.User(
            id=user_id,
            device_id=req.device_id,
            created_at=datetime.utcnow(),
            last_active_at=datetime.utcnow()
        )
        db.add(new_user)
        
        # 注册设备
        device = models.Device(
            id=req.device_id,
            user_id=user_id,
            device_type=req.device_type,
            device_model=req.device_model,
            app_version=req.app_version,
            is_active=True,
            last_seen_at=datetime.utcnow(),
            created_at=datetime.utcnow()
        )
        db.add(device)
        
        db.commit()
        
        logger.info(f"新用户注册: user_id={user_id}, device_id={req.device_id}")
        return {
            "user_id": user_id,
            "device_id": req.device_id,
            "is_new_user": True,
            "token": f"token_{user_id}",  # 简化版本的 token
            "message": "注册成功"
        }
        
    except Exception as e:
        logger.error(f"用户注册失败: {e}", exc_info=True)
        db.rollback()
        raise HTTPException(status_code=500, detail=f"用户注册失败: {str(e)}")


# ==================== 用户档案 API ====================

class UserProfileUpdate(BaseModel):
    """用户档案更新请求 - 对应前端 UserProfile"""
    nickname: Optional[str] = None
    gender: Optional[str] = None  # male/female/other
    age: Optional[int] = None
    height: Optional[float] = None  # cm
    weight: Optional[float] = None  # kg
    activity_level: Optional[str] = None  # sedentary/light/moderate/active/very_active
    health_goal: Optional[str] = None  # lose_weight/gain_muscle/maintain
    target_weight: Optional[float] = None
    health_conditions: Optional[List[str]] = None  # ["diabetes", "hypertension"]
    dietary_preferences: Optional[List[str]] = None  # ["low_oil", "low_salt", "vegetarian"]
    allergies: Optional[List[str]] = None  # ["peanut", "seafood"]


@app.get("/api/v1/user/profile")
async def get_user_profile(
    user_id: Optional[str] = None,
    device_id: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """
    获取用户档案
    
    可通过 user_id 或 device_id 查询
    
    Returns:
        用户档案信息，包含个人信息、健康目标、营养目标等
    """
    try:
        user = None
        if user_id:
            user = db.query(models.User).filter(models.User.id == user_id).first()
        elif device_id:
            user = db.query(models.User).filter(models.User.device_id == device_id).first()
        
        if not user:
            raise HTTPException(status_code=404, detail="用户不存在")
        
        return {
            "success": True,
            "profile": user.to_profile_dict()
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"获取用户档案失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"获取用户档案失败: {str(e)}")


@app.put("/api/v1/user/profile")
async def update_user_profile(
    user_id: str,
    profile: UserProfileUpdate,
    db: Session = Depends(get_db)
):
    """
    更新用户档案
    
    - 自动计算 BMI
    - 根据个人信息自动计算每日营养目标
    
    Args:
        user_id: 用户ID
        profile: 要更新的档案字段
        
    Returns:
        更新后的完整用户档案
    """
    try:
        user = db.query(models.User).filter(models.User.id == user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="用户不存在")
        
        # 更新字段
        update_data = profile.dict(exclude_unset=True)
        
        # 字段映射（前端字段 -> 数据库字段）
        field_mapping = {
            "height": "height_cm",
            "weight": "weight_kg"
        }
        
        for key, value in update_data.items():
            db_field = field_mapping.get(key, key)
            if hasattr(user, db_field):
                setattr(user, db_field, value)
        
        # 自动计算 BMI
        if user.height_cm and user.weight_kg:
            user.calculate_bmi()
        
        # 自动计算营养目标
        if any(k in update_data for k in ["age", "gender", "height", "weight", "activity_level", "health_goal"]):
            user.calculate_target_calories()
        
        user.updated_at = datetime.utcnow()
        db.commit()
        db.refresh(user)
        
        logger.info(f"用户档案更新: user_id={user_id}, fields={list(update_data.keys())}")
        
        return {
            "success": True,
            "message": "档案更新成功",
            "profile": user.to_profile_dict()
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"更新用户档案失败: {e}", exc_info=True)
        db.rollback()
        raise HTTPException(status_code=500, detail=f"更新用户档案失败: {str(e)}")


@app.post("/api/v1/user/profile/sync")
async def sync_user_profile(
    user_id: str,
    profile: UserProfileUpdate,
    db: Session = Depends(get_db)
):
    """
    同步用户档案（从前端同步到后端）
    
    - 如果用户不存在则创建
    - 如果用户存在则更新
    - 用于前端本地档案与后端同步
    """
    try:
        user = db.query(models.User).filter(models.User.id == user_id).first()
        
        if not user:
            # 用户不存在，可能是从本地恢复的数据
            raise HTTPException(
                status_code=404, 
                detail="用户不存在，请先调用 /api/v1/user/register 注册"
            )
        
        # 更新字段
        update_data = profile.dict(exclude_unset=True)
        field_mapping = {"height": "height_cm", "weight": "weight_kg"}
        
        for key, value in update_data.items():
            db_field = field_mapping.get(key, key)
            if hasattr(user, db_field) and value is not None:
                setattr(user, db_field, value)
        
        # 自动计算
        user.calculate_bmi()
        user.calculate_target_calories()
        user.updated_at = datetime.utcnow()
        
        db.commit()
        db.refresh(user)
        
        logger.info(f"用户档案同步: user_id={user_id}")
        
        return {
            "success": True,
            "message": "档案同步成功",
            "profile": user.to_profile_dict()
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"同步用户档案失败: {e}", exc_info=True)
        db.rollback()
        raise HTTPException(status_code=500, detail=f"同步失败: {str(e)}")


@app.get("/api/v1/user/{user_id}/stats")
async def get_user_stats(user_id: str, db: Session = Depends(get_db)):
    """
    获取用户统计数据
    
    Returns:
        - 总用餐次数
        - 总摄入热量
        - 活跃天数
        - 最近7天数据
    """
    try:
        user = db.query(models.User).filter(models.User.id == user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="用户不存在")
        
        # 获取最近7天的每日统计
        from datetime import timedelta
        today = datetime.utcnow().date()
        week_ago = today - timedelta(days=7)
        
        daily_stats = db.query(models.DailyNutrition).filter(
            models.DailyNutrition.user_id == user_id,
            models.DailyNutrition.date >= week_ago.isoformat()
        ).order_by(models.DailyNutrition.date.desc()).all()
        
        return {
            "user_id": user_id,
            "total_meals": user.total_meals,
            "total_calories": user.total_calories,
            "total_days_active": user.total_days_active,
            "target_calories": user.target_calories,
            "weekly_stats": [
                {
                    "date": stat.date,
                    "calories": stat.total_calories,
                    "protein": stat.total_protein,
                    "carbs": stat.total_carbs,
                    "fat": stat.total_fat,
                    "meals": stat.breakfast_count + stat.lunch_count + stat.dinner_count + stat.snack_count
                }
                for stat in daily_stats
            ]
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"获取用户统计失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"获取统计失败: {str(e)}")


@app.post("/api/v1/upload")
async def upload_image(file: UploadFile = File(...)):
    """
    上传图片接口
    
    Args:
        file: 图片文件
        
    Returns:
        image_url: 图片的可访问 URL
    """
    try:
        # 生成唯一文件名
        file_ext = os.path.splitext(file.filename)[1] if file.filename else ".jpg"
        filename = f"{uuid.uuid4()}{file_ext}"
        file_path = UPLOAD_DIR / filename
        
        # 保存文件
        with file_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        # 构造返回 URL
        # 注意：生产环境中应使用配置的域名或对象存储 URL
        # 这里为了 MVP 方便，使用请求的主机地址
        # 实际调用时，眼镜端会收到相对路径或需要拼接 Base URL
        return {
            "filename": filename,
            "url": f"/uploads/{filename}",
            "message": "上传成功"
        }
    except Exception as e:
        logger.error(f"图片上传失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="图片上传失败")



@app.post("/api/v1/nutrition/aggregate")
async def aggregate_nutrition(payload: SnapshotPayload):
    """根据食物列表聚合营养成分"""
    try:
        foods_payload = [_food_input_to_dict(f) for f in payload.foods]
        totals = calculate_nutrition(foods_payload)

        enriched = []
        for f in payload.foods:
            item_total = calculate_nutrition([_food_input_to_dict(f)])
            weight = f.weight_g
            if weight > 0:
                ratio = weight / 100.0
                cal100 = item_total["calories"] / ratio
                pro100 = item_total["protein"] / ratio
                carb100 = item_total["carbs"] / ratio
                fat100 = item_total["fat"] / ratio
            else:
                cal100 = pro100 = carb100 = fat100 = None
            enriched.append({
                "name": f.name,
                "weight_g": weight,
                "barcode": f.barcode,
                "calories_per_100g": round(cal100, 2) if cal100 is not None else None,
                "protein_per_100g": round(pro100, 2) if pro100 is not None else None,
                "carbs_per_100g": round(carb100, 2) if carb100 is not None else None,
                "fat_per_100g": round(fat100, 2) if fat100 is not None else None,
                "calories": item_total["calories"],
                "protein": item_total["protein"],
                "carbs": item_total["carbs"],
                "fat": item_total["fat"],
            })

        return {
            "foods": enriched,
            "total": totals,
        }
    except Exception as e:
        logger.error(f"聚合营养计算失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="营养聚合计算失败")


def calculate_nutrition(foods: list) -> dict:
    """
    计算食物的总营养成分
    
    Args:
        foods: 食物列表，每个食物包含name和weight
        
    Returns:
        总营养成分字典
    """
    total = {
        "calories": 0,
        "protein": 0,
        "carbs": 0,
        "fat": 0
    }
    
    for food in foods:
        name = food.get('name', '')
        weight = float(food.get('weight', food.get('weight_g', 0)) or 0)
        barcode = food.get('barcode')
        cal100 = food.get('calories_per_100g')
        pro100 = food.get('protein_per_100g')
        carb100 = food.get('carbs_per_100g')
        fat100 = food.get('fat_per_100g')

        # 条码优先：若有条码，优先通过 Open Food Facts 获取每100g营养
        if barcode:
            off_data = get_off_nutrition_for_barcode(str(barcode))
            if off_data:
                weight_ratio = weight / 100
                total['calories'] += round(off_data['calories'] * weight_ratio, 1)
                total['protein'] += round(off_data['protein'] * weight_ratio, 1)
                total['carbs'] += round(off_data['carbs'] * weight_ratio, 1)
                total['fat'] += round(off_data['fat'] * weight_ratio, 1)
                continue

        if all(v is not None for v in [cal100, pro100, carb100, fat100]):
            weight_ratio = weight / 100
            total['calories'] += round(float(cal100) * weight_ratio, 1)
            total['protein'] += round(float(pro100) * weight_ratio, 1)
            total['carbs'] += round(float(carb100) * weight_ratio, 1)
            total['fat'] += round(float(fat100) * weight_ratio, 1)
            continue

        # 使用多数据源营养数据库查询（支持分类兜底）
        # 优先级: 精确匹配 > 别名匹配 > 模糊匹配 > 分类默认值 > 通用默认值
        category = food.get("category")  # 获取分类用于兜底
        nutrition_db = get_nutrition_db()
        nutrition_result, is_exact = nutrition_db.lookup(name, category=category)
        
        # 转换为简单字典格式
        data = {
            "calories": nutrition_result.energy_kcal,
            "protein": nutrition_result.protein,
            "carbs": nutrition_result.carbohydrate,
            "fat": nutrition_result.fat,
        }
        
        # 注意：不再使用零食热量修正逻辑
        # 原因：该逻辑假设"snack 分类且低热量=加工零食"，但会误伤水果等天然食物
        # 依赖 VLM 正确返回 category (meal/snack/beverage/dessert/fruit)
        # 以及营养数据库中的真实数据
        
        # 如果是 fallback 且无分类，尝试旧的查询方式
        if nutrition_result.source.value == "fallback" and not category:
            key = normalize_food_key(name)
            if key in NUTRITION_DB_EXT:
                data = NUTRITION_DB_EXT[key]
            elif key in SYNONYMS:
                mapped = SYNONYMS[key]
                mapped_key = normalize_food_key(mapped)
                if mapped_key in NUTRITION_DB_EXT:
                    data = NUTRITION_DB_EXT[mapped_key]
                elif mapped in NUTRITION_DB:
                    data = NUTRITION_DB[mapped]
            elif name in NUTRITION_DB:
                data = NUTRITION_DB[name]
        
        weight_ratio = weight / 100
        factor = get_cooking_factor(food.get("cooking_method"))
        total['calories'] += round(data['calories'] * weight_ratio * factor, 1)
        total['protein'] += round(data['protein'] * weight_ratio, 1)
        total['carbs'] += round(data['carbs'] * weight_ratio, 1)
        total['fat'] += round(data['fat'] * weight_ratio, 1)
    
    return total


@app.get("/api/v1/nutrition/lookup")
async def lookup_nutrition_api(name: str, full: bool = False):
    """
    查询食物营养信息
    
    Args:
        name: 食物名称（中文或英文）
        full: 是否返回完整营养数据（32个字段）
    """
    nutrition_db = get_nutrition_db()
    nutrition_data, is_exact = nutrition_db.lookup(name)
    
    if nutrition_data.source.value == "fallback" and not is_exact:
        # 尝试旧的查询方式
        key = normalize_food_key(name)
        data = None
        if key in NUTRITION_DB_EXT:
            data = NUTRITION_DB_EXT[key]
        elif key in SYNONYMS:
            mapped = SYNONYMS[key]
            if mapped in NUTRITION_DB:
                data = NUTRITION_DB[mapped]
        elif name in NUTRITION_DB:
            data = NUTRITION_DB[name]
        
        if not data:
            raise HTTPException(status_code=404, detail="未找到该食物的营养信息")
        return {"name": name, "per_100g": data, "source": "legacy"}
    
    if full:
        return {
            "name": name,
            "matched_name": nutrition_data.food_name,
            "source": nutrition_data.source.value,
            "is_exact_match": is_exact,
            "nutrition": nutrition_data.to_full_dict(),
        }
    
    return {
        "name": name,
        "matched_name": nutrition_data.food_name,
        "source": nutrition_data.source.value,
        "is_exact_match": is_exact,
        "per_100g": nutrition_data.to_simple_dict(),
    }


@app.get("/api/v1/nutrition/stats")
async def get_nutrition_db_stats():
    """
    获取营养数据库统计信息
    
    返回各数据源的食物数量和别名映射数量
    """
    nutrition_db = get_nutrition_db()
    stats = nutrition_db.get_stats()
    return {
        "total_foods": stats["total_foods"],
        "sources": stats["sources"],
        "aliases": stats["aliases"],
        "description": "多数据源营养数据库，整合了中国食物成分表、USDA Foundation Foods、FoodStruct 等数据源",
    }


@app.get("/api/v1/nutrition/barcode")
async def get_nutrition_by_barcode(barcode: str):
    data = get_off_nutrition_for_barcode(barcode)
    if not data:
        raise HTTPException(status_code=404, detail="未找到该条码的营养信息")
    return {
        "barcode": barcode,
        "per_100g": data,
        "source": "openfoodfacts",
    }


@app.post("/api/v1/vision/analyze")
async def vision_analyze(req: VisionAnalyzeRequest):
    """调用 Qwen-VL 对餐盘图片进行多食材拆解，并返回营养快照。

    模式：
    - mode="start": 单图识别，返回完整食物列表（默认）
    - mode="update" + baseline_foods: 基于基线食物列表，识别当前剩余量
    - mode="update" + reference_image_url: 双图对比，直接输出消耗增量 delta
    """
    try:
        client = get_qwen_client()
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))

    # 根据模式选择不同的 prompt 和调用方式
    is_update_mode = req.mode == "update"
    use_dual_image = is_update_mode and req.reference_image_url
    use_baseline_list = is_update_mode and req.baseline_foods and not use_dual_image

    if use_dual_image:
        # === 双图对比模式：输出 delta_consumed_g ===
        system_prompt = (
            "你是食品消耗监测员。对比[参考图像(用餐开始时)]与[最新图像(当前)]，"
            "识别本轮被吃掉的食物增量，严格按照下列 JSON 结构返回：\n"
            "{\n"
            "  \"is_food\": true,\n"
            "  \"delta\": [\n"
            "    {\n"
            "      \"dish_name\": \"English dish name\",\n"
            "      \"dish_name_cn\": \"中文菜名\",\n"
            "      \"cooking_method\": \"stir-fry\",\n"
            "      \"ingredients\": [\n"
            "        {\"name_en\": \"chicken\", \"delta_consumed_g\": 60.0, \"confidence\": 0.9}\n"
            "      ]\n"
            "    }\n"
            "  ]\n"
            "}\n\n"
            "**核心要求**：\n"
            "1. 严格只输出 JSON，不要任何解释。\n"
            "2. 对比两张图片，只输出有明显减少的食材及其 delta_consumed_g（克）。\n"
            "3. name_en 使用标准英文食材名（如 pork, rice, egg, chicken breast）。\n"
            "4. 如果食物完全吃完，delta_consumed_g 等于该食材的全部重量。\n"
            "5. 如果没有明显变化，返回空的 delta 数组。\n"
        )
        user_instruction = "第一张是参考图像（用餐开始时），第二张是当前图像。请对比两张图片，输出被吃掉的食物增量。"
        
        messages = [
            {"role": "system", "content": [{"type": "text", "text": system_prompt}]},
            {"role": "user", "content": [
                {"type": "image_url", "image_url": {"url": req.reference_image_url}},
                {"type": "image_url", "image_url": {"url": req.image_url}},
                {"type": "text", "text": user_instruction},
            ]},
        ]
        
    elif use_baseline_list:
        # === 基线列表模式：基于已知食物识别剩余量 ===
        baseline_info = json.dumps([{
            "dish_name": f.dish_name,
            "dish_name_cn": f.dish_name_cn,
            "ingredients": f.ingredients,
            "total_weight_g": f.total_weight_g
        } for f in req.baseline_foods], ensure_ascii=False)
        
        system_prompt = (
            "你是食品消耗监测员。用户提供了用餐开始时的基线食物列表，请根据当前图片识别每种食物的剩余量。\n"
            f"**基线食物列表**：\n{baseline_info}\n\n"
            "请严格按照下列 JSON 结构返回当前剩余情况：\n"
            "{\n"
            "  \"is_food\": true,\n"
            "  \"foods\": [\n"
            "    {\n"
            "      \"dish_name\": \"与基线相同的英文名\",\n"
            "      \"dish_name_cn\": \"与基线相同的中文名\",\n"
            "      \"cooking_method\": \"与基线相同\",\n"
            "      \"ingredients\": [\n"
            "        {\"name_en\": \"与基线相同\", \"weight_g\": 当前剩余克数, \"confidence\": 0.9}\n"
            "      ],\n"
            "      \"total_weight_g\": 当前剩余总重,\n"
            "      \"confidence\": 0.9\n"
            "    }\n"
            "  ]\n"
            "}\n\n"
            "**核心要求**：\n"
            "1. 严格只输出 JSON，不要任何解释。\n"
            "2. 必须使用与基线完全相同的 dish_name、dish_name_cn、name_en（保持一致性）。\n"
            "3. weight_g 填写当前图片中该食材的剩余重量（克），已吃完的填 0。\n"
            "4. 如果发现新食物（加菜），可以添加新条目。\n"
        )
        user_instruction = "请根据当前图片，识别基线食物的剩余量。"
        
        messages = [
            {"role": "system", "content": [{"type": "text", "text": system_prompt}]},
            {"role": "user", "content": [
                {"type": "image_url", "image_url": {"url": req.image_url}},
                {"type": "text", "text": user_instruction},
            ]},
        ]
        
    else:
        # === 标准单图识别模式（Start）===
        system_prompt = (
            "你是一名专业的营养分析助手。请根据用户提供的图片，识别图中的食物，"
            "并将结果严格按照下列 JSON 结构返回，不要输出任何额外的说明文字：\n"
            "{\n"
            "  \"is_food\": true,\n"
            "  \"suggestion\": \"一句简短的餐饮建议\",\n"
            "  \"foods\": [\n"
            "    {\n"
            "      \"dish_name\": \"English dish name\",\n"
            "      \"dish_name_cn\": \"中文菜名\",\n"
            "      \"category\": \"meal/snack/beverage/dessert/fruit\",\n"
            "      \"cooking_method\": \"raw/steam/boil/braise/stir-fry/deep-fry/bake/grill\",\n"
            "      \"barcode\": null,\n"
            "      \"ingredients\": [\n"
            "        {\"name_en\": \"ingredient\", \"name_cn\": \"食材\", \"weight_g\": 100.0, \"confidence\": 0.9}\n"
            "      ],\n"
            "      \"total_weight_g\": 200.0,\n"
            "      \"confidence\": 0.9\n"
            "    }\n"
            "  ]\n"
            "}\n\n"
            "**核心要求**：\n"
            "1. 必须输出严格合法的 JSON，不要有任何额外文字。\n"
            "2. dish_name 用英文，dish_name_cn 用中文。\n"
            "3. ingredients 中 name_en 必须使用精确的英文食材名：\n"
            "   - 油炸零食用: fried corn chips, fried peanuts, potato chips\n"
            "   - 坚果用: roasted peanuts, cashew nuts, almonds\n"
            "   - 肉干用: beef jerky, pork jerky\n"
            "   - 饼干用: cookies, crackers, biscuits\n"
            "4. weight_g 估算要求：\n"
            "   - 参考包装大小，小袋零食约50-80g，中袋100-150g，大袋200-300g\n"
            "   - 餐盘食物参考餐盘、碗、筷子估算\n"
            "5. cooking_method：零食通常是 deep-fry（油炸）或 bake（烘焙），不要用 raw。\n"
            "6. category 分类：meal=正餐/主食, snack=零食小吃, beverage=饮品, dessert=甜点, fruit=水果。\n"
            "7. 只有图片中完全没有食物时才设 is_food 为 false，包装食品、零食、饮料都是食物。\n"
            "8. 如能看到条码数字或包装上的重量标识，填入 barcode 字段。\n"
            "9. suggestion 字段请根据识别到的食物给出一句简短的餐饮建议（20字以内），例如'搭配蔬菜更健康'、'蛋白质充足'、'注意控制油脂摄入'等。\n"
        )
        user_instruction = "请根据图片识别所有可见的主要食材，并严格按照上述 JSON 结构输出。"
        if req.question:
            user_instruction += f"\n补充说明：{req.question}"
        
        messages = [
            {"role": "system", "content": [{"type": "text", "text": system_prompt}]},
            {"role": "user", "content": [
                {"type": "image_url", "image_url": {"url": req.image_url}},
                {"type": "text", "text": user_instruction},
            ]},
        ]

    try:
        completion = client.chat.completions.create(
            model=QWEN_VL_MODEL,
            messages=messages,
        )
    except Exception as e:
        logger.error(f"调用 Qwen-VL 失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"调用 Qwen-VL 失败: {str(e)}")

    # 提取文本内容
    try:
        message = completion.choices[0].message
        content = message.content
        if isinstance(content, str):
            text = content.strip()
        else:
            parts = []
            for part in content:
                if isinstance(part, dict) and part.get("type") == "text":
                    parts.append(part.get("text", ""))
            text = "\n".join(parts).strip()
    except Exception as e:
        logger.error(f"解析 Qwen-VL 响应失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="解析 Qwen-VL 响应失败")

    # 尝试从返回内容中提取 JSON
    llm_json = None
    try:
        # 先尝试直接解析
        llm_json = json.loads(text)
    except json.JSONDecodeError:
        # 尝试提取 JSON 块（可能包含 markdown 代码块）
        import re
        json_match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', text)
        if json_match:
            try:
                llm_json = json.loads(json_match.group(1))
            except:
                pass
        
        if llm_json is None:
            # 尝试找到 { 开头的 JSON
            json_start = text.find('{')
            if json_start >= 0:
                try:
                    llm_json = json.loads(text[json_start:])
                except:
                    pass
        
        if llm_json is None:
            logger.error(f"Qwen-VL 返回内容非合法 JSON: {text[:500]}")
            raise HTTPException(status_code=500, detail=f"Qwen-VL 返回内容非合法 JSON: {text[:200]}")

    # 检查是否为食物
    if not llm_json.get("is_food", True):
        raise HTTPException(status_code=400, detail="未检测到食物，请拍摄清晰的食物照片")

    # 根据模式处理响应
    food_inputs: List[FoodInput] = []
    
    # 双图对比模式：处理 delta 数组
    if use_dual_image and "delta" in llm_json:
        for dish in llm_json.get("delta", []) or []:
            method = dish.get("cooking_method")
            for ing in dish.get("ingredients", []) or []:
                name = ing.get("name_en") or ing.get("name") or ""
                weight = ing.get("delta_consumed_g") or 0
                try:
                    weight_val = float(weight)
                except Exception:
                    weight_val = 0.0
                if not name or weight_val <= 0:
                    continue
                food_inputs.append(FoodInput(name=name, weight_g=weight_val, cooking_method=method))
        
        # delta 模式可能返回空数组（没有变化）
        if not food_inputs:
            return {
                "raw_llm": llm_json,
                "mode": "delta",
                "suggestion": "",
                "snapshot": {
                    "foods": [],
                    "nutrition": {"calories": 0, "protein": 0, "carbs": 0, "fat": 0},
                },
                "message": "未检测到明显的食物消耗变化",
            }
        
        foods_payload = [_food_input_to_dict(f) for f in food_inputs]
        totals = calculate_nutrition(foods_payload)
        
        return {
            "raw_llm": llm_json,
            "mode": "delta",
            "suggestion": llm_json.get("suggestion", ""),
            "snapshot": {
                "foods": [f.dict() for f in food_inputs],
                "nutrition": totals,  # 这是本轮消耗的营养
            },
        }
    
    # 标准模式和基线列表模式：处理 foods 数组
    for dish in llm_json.get("foods", []) or []:
        method = dish.get("cooking_method")
        category = dish.get("category")  # 获取分类用于兜底查询
        for ing in dish.get("ingredients", []) or []:
            name = ing.get("name_en") or ing.get("name") or ""
            weight = ing.get("weight_g") or ing.get("delta_consumed_g") or 0
            try:
                weight_val = float(weight)
            except Exception:
                weight_val = 0.0
            if not name or weight_val <= 0:
                continue
            food_inputs.append(FoodInput(name=name, weight_g=weight_val, cooking_method=method, category=category))

    if not food_inputs:
        raise HTTPException(status_code=500, detail="未能从图像中识别出有效食材")

    foods_payload = [_food_input_to_dict(f) for f in food_inputs]
    totals = calculate_nutrition(foods_payload)

    # 提取 suggestion（如果有）
    suggestion = llm_json.get("suggestion", "")
    
    return {
        "raw_llm": llm_json,
        "mode": "start" if not is_update_mode else "update",
        "suggestion": suggestion,
        "snapshot": {
            "foods": [f.dict() for f in food_inputs],
            "nutrition": totals,
        },
    }


# ==================== 容错分析 API ====================

class MealUpdateRequest(BaseModel):
    """带容错的会话更新请求"""
    image_url: str
    baseline_foods: List[BaselineFood]  # 首次识别的基线
    last_foods: Optional[List[BaselineFood]] = None  # 上一次有效识别（可选）
    meal_context: Optional[MealContextPayload] = None  # 用餐上下文（可选）


def generate_eating_pace_advice(meal_context: Optional[MealContextPayload]) -> str:
    """根据用餐上下文生成进食速度建议"""
    if not meal_context:
        return "开始记录用餐"
    
    duration = meal_context.duration_minutes
    consumed = meal_context.total_consumed_so_far
    
    if duration <= 0:
        return "开始用餐"
    
    speed = consumed / duration  # kcal/min
    
    if speed > 50:
        return "进食速度较快，建议放慢节奏，细嚼慢咽"
    elif speed > 30:
        return "进食速度适中，继续保持"
    else:
        return "进食节奏良好，有助于消化吸收"


def generate_progress_summary(meal_context: Optional[MealContextPayload], baseline_calories: float = 0) -> str:
    """生成用餐进度总结"""
    if not meal_context:
        return "用餐进行中"
    
    consumed = meal_context.total_consumed_so_far
    duration = meal_context.duration_minutes
    
    if baseline_calories > 0:
        ratio = consumed / baseline_calories * 100
        return f"已摄入 {int(consumed)} kcal，约占本餐 {int(ratio)}%"
    else:
        return f"已摄入 {int(consumed)} kcal，用餐 {int(duration)} 分钟"


def _compare_foods(baseline: List[dict], current: List[dict]) -> dict:
    """对比基线和当前识别结果，返回容错分析"""
    warnings = []
    adjustments = []
    
    baseline_map = {}
    for f in baseline:
        for ing in f.get("ingredients", []):
            key = ing.get("name_en", "").lower()
            if key:
                baseline_map[key] = ing.get("weight_g", 0)
    
    current_map = {}
    for f in current:
        for ing in f.get("ingredients", []):
            key = ing.get("name_en", "").lower()
            if key:
                current_map[key] = ing.get("weight_g", 0)
    
    # 检查每种食材的变化
    for key, baseline_weight in baseline_map.items():
        current_weight = current_map.get(key, 0)
        
        if current_weight == 0 and baseline_weight > 0:
            # 食材完全消失 - 可能没拍到
            warnings.append({
                "type": "missing",
                "ingredient": key,
                "message": f"{key} 未检测到，可能未拍摄到该食材",
                "suggestion": "keep_last"  # 建议保持上次值
            })
        elif current_weight > baseline_weight * 1.5:
            # 食材增加超过50% - 可能加菜
            warnings.append({
                "type": "increase",
                "ingredient": key,
                "message": f"{key} 重量增加，可能是加菜",
                "suggestion": "update_baseline"  # 建议更新基线
            })
            adjustments.append({
                "action": "add_to_baseline",
                "ingredient": key,
                "delta": current_weight - baseline_weight
            })
    
    # 检查新出现的食材（加菜）
    for key, current_weight in current_map.items():
        if key not in baseline_map and current_weight > 0:
            warnings.append({
                "type": "new_food",
                "ingredient": key,
                "message": f"检测到新食材 {key}，可能是加菜",
                "suggestion": "add_to_baseline"
            })
            adjustments.append({
                "action": "add_new",
                "ingredient": key,
                "weight": current_weight
            })
    
    return {
        "warnings": warnings,
        "adjustments": adjustments,
        "has_issues": len(warnings) > 0
    }


@app.post("/api/v1/vision/analyze_meal_update")
async def analyze_meal_update(req: MealUpdateRequest):
    """
    带容错逻辑的会话更新分析
    
    1. 使用基线列表模式调用 VLM
    2. 分析结果与基线的差异
    3. 返回容错建议
    
    前端根据建议决定：
    - skip: 识别失败，使用上次数据
    - accept: 正常结果，直接使用
    - adjust: 需要调整（加菜等）
    """
    # 构造 vision/analyze 请求
    analyze_req = VisionAnalyzeRequest(
        image_url=req.image_url,
        mode="update",
        baseline_foods=req.baseline_foods
    )
    
    try:
        result = await vision_analyze(analyze_req)
    except HTTPException as e:
        if e.status_code == 400 and "未检测到食物" in str(e.detail):
            # 没有检测到食物 - 建议跳过
            return {
                "status": "skip",
                "reason": "no_food_detected",
                "message": "未检测到食物，建议使用上次数据",
                "use_last": True,
                "raw_llm": None,
                "snapshot": None,
                # 新增：进食速度建议和进度总结
                "eating_pace_advice": generate_eating_pace_advice(req.meal_context),
                "progress_summary": generate_progress_summary(req.meal_context)
            }
        raise
    
    # 分析结果与基线的差异
    baseline_data = [{
        "dish_name": f.dish_name,
        "ingredients": f.ingredients
    } for f in req.baseline_foods]
    
    current_data = result.get("raw_llm", {}).get("foods", [])
    
    comparison = _compare_foods(baseline_data, current_data)
    
    # 计算基线总热量（用于进度总结）
    baseline_calories = sum(
        sum(ing.get("weight_g", 0) * 2 for ing in f.ingredients)  # 粗略估算每克2卡
        for f in req.baseline_foods
    )
    
    # 生成进食速度建议和进度总结
    eating_pace_advice = generate_eating_pace_advice(req.meal_context)
    progress_summary = generate_progress_summary(req.meal_context, baseline_calories)
    
    # 根据分析结果决定状态
    if comparison["has_issues"]:
        missing_count = sum(1 for w in comparison["warnings"] if w["type"] == "missing")
        total_baseline = len([ing for f in baseline_data for ing in f.get("ingredients", [])])
        
        if missing_count > total_baseline * 0.5:
            # 超过50%的食材消失 - 可能拍摄问题
            return {
                "status": "skip",
                "reason": "too_many_missing",
                "message": f"超过半数食材未检测到({missing_count}/{total_baseline})，建议使用上次数据",
                "use_last": True,
                "comparison": comparison,
                "raw_llm": result.get("raw_llm"),
                "snapshot": result.get("snapshot"),
                # 新增：进食速度建议和进度总结
                "eating_pace_advice": eating_pace_advice,
                "progress_summary": progress_summary
            }
        else:
            # 部分问题，返回调整建议
            return {
                "status": "adjust",
                "reason": "partial_issues",
                "message": "检测结果存在异常，请查看调整建议",
                "use_last": False,
                "comparison": comparison,
                "raw_llm": result.get("raw_llm"),
                "snapshot": result.get("snapshot"),
                # 新增：进食速度建议和进度总结
                "eating_pace_advice": eating_pace_advice,
                "progress_summary": progress_summary
            }
    
    # 正常结果
    return {
        "status": "accept",
        "reason": "normal",
        "message": "识别正常",
        "use_last": False,
        "comparison": comparison,
        "raw_llm": result.get("raw_llm"),
        "snapshot": result.get("snapshot"),
        # 新增：进食速度建议和进度总结
        "eating_pace_advice": eating_pace_advice,
        "progress_summary": progress_summary
    }


# ==================== 用餐会话管理API ====================

@app.post("/api/v1/meal/start")
async def start_meal_session(
    snapshot: SnapshotPayload,
    user_id: str = "default_user",
    meal_type: str = "lunch",
    auto_interval: int = 300,
    db: Session = Depends(get_db)
):
    """
    开始用餐会话
    
    Args:
        snapshot: 用餐开始时的食物与营养数据
        user_id: 用户ID
        meal_type: 用餐类型 (breakfast/lunch/dinner/snack)
        auto_interval: 自动监测间隔(秒)，默认5分钟
    """
    try:
        logger.info(f"开始用餐会话: 用户={user_id}, 类型={meal_type}")

        session_id = str(uuid.uuid4())

        foods_payload = [_food_input_to_dict(f) for f in snapshot.foods]
        if snapshot.nutrition:
            totals = {
                "calories": snapshot.nutrition.calories,
                "protein": snapshot.nutrition.protein,
                "carbs": snapshot.nutrition.carbs,
                "fat": snapshot.nutrition.fat,
            }
        else:
            totals = calculate_nutrition(foods_payload)

        db_session = models.MealSession(
            id=session_id,
            user_id=user_id,
            status="active",
            start_time=datetime.utcnow(),
            auto_capture_interval=auto_interval,
        )
        db.add(db_session)
        db.flush()

        snapshot_id = str(uuid.uuid4())
        db_snapshot = models.MealSnapshot(
            id=snapshot_id,
            session_id=session_id,
            image_url="",
            captured_at=datetime.utcnow(),
            model="external",
            raw_json={
                "foods": [f.dict() for f in snapshot.foods],
                "nutrition": totals,
            },
            total_kcal=totals["calories"],
        )
        db.add(db_snapshot)

        for f in snapshot.foods:
            cal = _calc_food_calories(f)
            db.add(models.SnapshotFood(
                id=str(uuid.uuid4()),
                snapshot_id=snapshot_id,
                name=f.name,
                chinese_name=None,
                weight_g=f.weight_g,
                calories_kcal=cal,
                confidence=0.0,
                bbox=None,
            ))

        db.commit()

        return {
            "session_id": session_id,
            "status": "active",
            "initial_kcal": totals["calories"],
            # 完整营养数据
            "initial_nutrition": totals,
            "auto_capture_interval": auto_interval,
        }
        
    except Exception as e:
        logger.error(f"开始用餐会话失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"开始用餐会话失败: {str(e)}")



def calculate_dynamic_stats(session_id: str, db: Session):
    """
    动态计算会话统计，支持中途加菜/发现新食物。
    
    逻辑：
    1. 遍历所有快照。
    2. 维护一个 seen_foods 集合 (name -> max_weight)。
    3. 对于每种食物，如果是新出现的，将其初始热量计入 total_served。
       如果是已存在的，暂不处理"续杯"逻辑（MVP简化，假设只会变少；若变多可能是误差，除非显著增加）。
       这里采用 simplified 策略：Total Served = Sum(Initial Weight/Kcal of each unique food when it FIRST appeared).
    
    Returns:
        dict: {
            "total_served_kcal": float,
            "current_remaining_kcal": float,
            "total_consumed_kcal": float,
            "consumption_ratio": float,
            "duration_minutes": float,
            "total_served_protein_g": float,
            "total_served_carbs_g": float,
            "total_served_fat_g": float,
            "total_consumed_protein_g": float,
            "total_consumed_carbs_g": float,
            "total_consumed_fat_g": float
        }
    """
    snapshots = db.query(models.MealSnapshot)\
        .filter(models.MealSnapshot.session_id == session_id)\
        .order_by(models.MealSnapshot.captured_at.asc())\
        .all()
    
    if not snapshots:
        return {
            "total_served_kcal": 0.0,
            "current_remaining_kcal": 0.0,
            "total_consumed_kcal": 0.0,
            "consumption_ratio": 0.0,
            "duration_minutes": 0.0,
            "total_served_protein_g": 0.0,
            "total_served_carbs_g": 0.0,
            "total_served_fat_g": 0.0,
            "total_consumed_protein_g": 0.0,
            "total_consumed_carbs_g": 0.0,
            "total_consumed_fat_g": 0.0
        }

    # 1. 计算 Total Served (动态累加) - 热量和营养成分
    seen_foods = set()
    total_served_kcal = 0.0
    total_served_protein = 0.0
    total_served_carbs = 0.0
    total_served_fat = 0.0
    
    for snap in snapshots:
        # 获取该快照下的所有食物详情
        foods = db.query(models.SnapshotFood).filter(models.SnapshotFood.snapshot_id == snap.id).all()
        
        # 从 raw_json 获取营养信息用于计算每种食物的营养比例
        snap_nutrition = {}
        if snap.raw_json and isinstance(snap.raw_json, dict):
            snap_nutrition = snap.raw_json.get("nutrition", {})
        snap_total_kcal = snap.total_kcal or 0.0
        snap_protein = snap_nutrition.get("protein", 0.0) if snap_nutrition else 0.0
        snap_carbs = snap_nutrition.get("carbs", 0.0) if snap_nutrition else 0.0
        snap_fat = snap_nutrition.get("fat", 0.0) if snap_nutrition else 0.0
        
        for food in foods:
            key = normalize_food_key(food.name)
            if key not in seen_foods:
                # 发现新食物！累加其热量到总上菜量
                food_kcal = food.calories_kcal or 0.0
                total_served_kcal += food_kcal
                
                # 按热量比例分配营养成分
                if snap_total_kcal > 0 and food_kcal > 0:
                    ratio = food_kcal / snap_total_kcal
                    total_served_protein += snap_protein * ratio
                    total_served_carbs += snap_carbs * ratio
                    total_served_fat += snap_fat * ratio
                
                seen_foods.add(key)
    
    # 2. 获取当前剩余
    last_snapshot = snapshots[-1]
    current_remaining_kcal = last_snapshot.total_kcal or 0.0
    
    # 从最后一个快照获取当前剩余的营养成分
    last_nutrition = {}
    if last_snapshot.raw_json and isinstance(last_snapshot.raw_json, dict):
        last_nutrition = last_snapshot.raw_json.get("nutrition", {})
    current_remaining_protein = last_nutrition.get("protein", 0.0) if last_nutrition else 0.0
    current_remaining_carbs = last_nutrition.get("carbs", 0.0) if last_nutrition else 0.0
    current_remaining_fat = last_nutrition.get("fat", 0.0) if last_nutrition else 0.0
    
    # 3. 计算消耗
    # 注意：如果测量误差导致 current > total_served (比如一开始识别小了，后面识别大了)，需要 clamp
    if current_remaining_kcal > total_served_kcal:
         # 这种情况可能是 refil 或 误差，MVP 简单处理：假设 Total Served 至少是 Current
         total_served_kcal = current_remaining_kcal
         total_served_protein = max(total_served_protein, current_remaining_protein)
         total_served_carbs = max(total_served_carbs, current_remaining_carbs)
         total_served_fat = max(total_served_fat, current_remaining_fat)
         
    total_consumed_kcal = max(0.0, total_served_kcal - current_remaining_kcal)
    total_consumed_protein = max(0.0, total_served_protein - current_remaining_protein)
    total_consumed_carbs = max(0.0, total_served_carbs - current_remaining_carbs)
    total_consumed_fat = max(0.0, total_served_fat - current_remaining_fat)
    
    # 4. 其他统计
    start_time = snapshots[0].captured_at
    end_time = last_snapshot.captured_at
    duration_minutes = (end_time - start_time).total_seconds() / 60.0
    
    consumption_ratio = 0.0
    if total_served_kcal > 0:
        consumption_ratio = total_consumed_kcal / total_served_kcal
        
    return {
        "total_served_kcal": round(total_served_kcal, 1),
        "current_remaining_kcal": round(current_remaining_kcal, 1),
        "total_consumed_kcal": round(total_consumed_kcal, 1),
        "consumption_ratio": round(consumption_ratio, 3),
        "duration_minutes": round(duration_minutes, 1),
        # 新增：营养成分总量
        "total_served_protein_g": round(total_served_protein, 1),
        "total_served_carbs_g": round(total_served_carbs, 1),
        "total_served_fat_g": round(total_served_fat, 1),
        "total_consumed_protein_g": round(total_consumed_protein, 1),
        "total_consumed_carbs_g": round(total_consumed_carbs, 1),
        "total_consumed_fat_g": round(total_consumed_fat, 1)
    }


@app.post("/api/v1/meal/update")
async def update_meal_session(
    session_id: str,
    snapshot: SnapshotPayload,
    db: Session = Depends(get_db)
):
    """
    更新用餐会话（自动监测调用）
    """
    try:
        db_session = db.query(models.MealSession).filter(models.MealSession.id == session_id).first()
        if not db_session:
            raise HTTPException(status_code=404, detail="会话不存在")
        if db_session.status != "active":
            raise HTTPException(status_code=400, detail="会话已结束或暂停")
        
        # 1. 计算新快照的营养
        foods_payload = [_food_input_to_dict(f) for f in snapshot.foods]
        if snapshot.nutrition:
            totals = {
                "calories": snapshot.nutrition.calories,
                "protein": snapshot.nutrition.protein,
                "carbs": snapshot.nutrition.carbs,
                "fat": snapshot.nutrition.fat,
            }
        else:
            totals = calculate_nutrition(foods_payload)

        # 2. 获取上一次快照用于计算"本次增量"
        last_snapshot = db.query(models.MealSnapshot)\
            .filter(models.MealSnapshot.session_id == session_id)\
            .order_by(models.MealSnapshot.captured_at.desc())\
            .first()
        last_calories = last_snapshot.total_kcal if last_snapshot else 0

        # 3. 保存当前快照
        snap_id = str(uuid.uuid4())
        db_snapshot = models.MealSnapshot(
            id=snap_id,
            session_id=session_id,
            image_url="",
            captured_at=datetime.utcnow(),
            model="external",
            raw_json={
                "foods": [f.dict() for f in snapshot.foods],
                "nutrition": totals,
            },
            total_kcal=totals["calories"],
        )
        db.add(db_snapshot)
        for f in snapshot.foods:
            cal = _calc_food_calories(f)
            db.add(models.SnapshotFood(
                id=str(uuid.uuid4()),
                snapshot_id=snap_id,
                name=f.name,
                chinese_name=None,
                weight_g=f.weight_g,
                calories_kcal=cal,
                confidence=0.0,
                bbox=None,
            ))
        db.commit()

        # 4. 使用动态统计逻辑重新计算
        stats = calculate_dynamic_stats(session_id, db)
        
        current_kcal = stats["current_remaining_kcal"]
        total_consumed = stats["total_consumed_kcal"]
        consumption_ratio = stats["consumption_ratio"]
        duration_minutes = stats["duration_minutes"]
        
        # 5. 计算"自上次更新以来的消耗"
        # 注意：如果是加菜了，current_kcal 可能 > last_calories，这里的 consumed_since_last 可能是负数
        # 但前端展示通常关心的是"净消耗"，或者我们可以单独计算"本次摄入" vs "本次加菜"
        # 这里简化：直接展示净变化，负数代表"加菜了"
        net_change = last_calories - current_kcal
        
        advice_list = []
        eating_speed = (total_consumed / duration_minutes) if duration_minutes > 0 else 0
        if eating_speed > 50:
            advice_list.append("进食速度较快，建议放慢节奏")
        elif eating_speed < 15 and total_consumed > 50:
            advice_list.append("进食速度适中，有助于控制食量")
            
        if consumption_ratio > 0.8:
            advice_list.append("已经吃了大部分，建议稍作休息")
        
        if net_change < -50:
            advice_list.append("检测到新食物或加菜，已更新总摄入量")
            
        advice = " | ".join(advice_list) if advice_list else "进食节奏良好，继续保持"

        return {
            "session_id": session_id,
            # 手机端期望的扁平格式
            "current_remaining_kcal": current_kcal,
            "total_served_kcal": stats["total_served_kcal"],
            "consumed_kcal": total_consumed,
            "consumption_ratio": stats["consumption_ratio"],
            "duration_minutes": stats["duration_minutes"],
            "suggestion": advice,
            # 新增：营养成分
            "consumed_protein": stats["total_consumed_protein_g"],
            "consumed_carbs": stats["total_consumed_carbs_g"],
            "consumed_fat": stats["total_consumed_fat_g"],
            # 额外信息（向后兼容）
            "current_status": {
                "remaining_calories": current_kcal,
                "consumed_total": total_consumed,
                "consumption_ratio": stats["consumption_ratio"],
                "duration_minutes": stats["duration_minutes"]
            },
            "since_last_update": {
                "net_calories_change": round(net_change, 1),
                "time_elapsed_minutes": db_session.auto_capture_interval / 60
            },
            "snapshots_count": db.query(models.MealSnapshot).filter(models.MealSnapshot.session_id == session_id).count()
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"更新用餐会话失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"更新用餐会话失败: {str(e)}")


@app.post("/api/v1/meal/end")
async def end_meal_session(
    req: MealEndRequest,
    db: Session = Depends(get_db)
):
    """
    结束用餐会话（增强版）
    
    接收完整的用餐上下文，使用 LLM 生成个性化营养建议
    
    请求参数：
    - session_id: 会话ID（必填）
    - final_snapshot: 最终快照（可选）
    - meal_context: 用餐上下文（可选）
    - daily_context: 今日上下文（可选）
    - user_profile: 用户档案（可选）
    
    返回：
    - meal_summary: 用餐总结（用于眼镜显示）
    - advice: 详细建议（用于手机显示）
    - next_meal_suggestion: 下一餐建议
    """
    session_id = req.session_id
    final_snapshot = req.final_snapshot
    meal_context = req.meal_context
    daily_context = req.daily_context
    user_profile = req.user_profile
    
    try:
        db_session = db.query(models.MealSession).filter(models.MealSession.id == session_id).first()
        if not db_session:
            raise HTTPException(status_code=404, detail="会话不存在")
        logger.info(f"结束用餐会话: {session_id}, 有上下文: meal={meal_context is not None}, daily={daily_context is not None}, user={user_profile is not None}")

        if final_snapshot:
            foods_payload = [_food_input_to_dict(f) for f in final_snapshot.foods]
            if final_snapshot.nutrition:
                totals = {
                    "calories": final_snapshot.nutrition.calories,
                    "protein": final_snapshot.nutrition.protein,
                    "carbs": final_snapshot.nutrition.carbs,
                    "fat": final_snapshot.nutrition.fat,
                }
            else:
                totals = calculate_nutrition(foods_payload)

            snap_id = str(uuid.uuid4())
            db_snapshot = models.MealSnapshot(
                id=snap_id,
                session_id=session_id,
                image_url="",
                captured_at=datetime.utcnow(),
                model="external",
                raw_json={
                    "foods": [f.dict() for f in final_snapshot.foods],
                    "nutrition": totals,
                },
                total_kcal=totals["calories"],
            )
            db.add(db_snapshot)
            for f in final_snapshot.foods:
                cal = _calc_food_calories(f)
                db.add(models.SnapshotFood(
                    id=str(uuid.uuid4()),
                    snapshot_id=snap_id,
                    name=f.name,
                    chinese_name=None,
                    weight_g=f.weight_g,
                    calories_kcal=cal,
                    confidence=0.0,
                    bbox=None,
                ))
        
        # 3. 更新会话状态
        db_session.end_time = datetime.utcnow()
        db_session.status = "completed"
        db.commit()
        
        # 4. 计算最终统计 (使用动态逻辑)
        stats = calculate_dynamic_stats(session_id, db)
        
        # 使用传入的 meal_context 时长，如果有的话
        duration_minutes = stats["duration_minutes"]
        if meal_context and meal_context.duration_minutes > 0:
            duration_minutes = meal_context.duration_minutes
        
        # 计算营养成分百分比（用于饼状图）
        total_protein = stats["total_consumed_protein_g"]
        total_carbs = stats["total_consumed_carbs_g"]
        total_fat = stats["total_consumed_fat_g"]
        total_macro = total_protein + total_carbs + total_fat
        
        nutrition_breakdown = {
            "protein_g": total_protein,
            "carbs_g": total_carbs,
            "fat_g": total_fat,
            "protein_percent": round(total_protein / total_macro * 100, 1) if total_macro > 0 else 0,
            "carbs_percent": round(total_carbs / total_macro * 100, 1) if total_macro > 0 else 0,
            "fat_percent": round(total_fat / total_macro * 100, 1) if total_macro > 0 else 0
        }
        
        final_stats = {
            "total_served": stats["total_served_kcal"],
            "total_consumed": stats["total_consumed_kcal"],
            "consumption_ratio": stats["consumption_ratio"],
            "duration_minutes": duration_minutes,
            "average_eating_speed": round(stats["total_consumed_kcal"] / duration_minutes, 1) if duration_minutes > 0 else 0,
            "waste_ratio": round(1 - stats["consumption_ratio"], 3),
            "snapshots_count": db.query(models.MealSnapshot).filter(models.MealSnapshot.session_id == session_id).count(),
            # 营养成分总量
            "total_protein": total_protein,
            "total_carbs": total_carbs,
            "total_fat": total_fat,
            # 营养成分百分比
            "nutrition_breakdown": nutrition_breakdown
        }
        
        # 5. 生成最终报告（文本格式）
        final_report = generate_meal_report(final_stats)
        
        # 6. 生成智能餐饮建议（使用 LLM + 降级策略）
        smart_advice = await generate_smart_meal_advice(
            final_stats, 
            nutrition_breakdown, 
            meal_context, 
            daily_context, 
            user_profile
        )
        
        # 7. 构建 meal_summary（用于眼镜显示）
        meal_summary = {
            "total_calories": stats["total_consumed_kcal"],
            "total_protein": total_protein,
            "total_carbs": total_carbs,
            "total_fat": total_fat,
            "duration_minutes": duration_minutes,
            "rating": smart_advice.get("rating", "fair"),
            "short_advice": smart_advice.get("short_advice", "用餐记录已完成")
        }
        
        # 8. 构建 advice（详细建议，用于手机显示）
        advice = {
            "summary": smart_advice.get("summary", "用餐记录已完成"),
            "suggestions": smart_advice.get("suggestions", []),
            "highlights": smart_advice.get("highlights", []),
            "warnings": smart_advice.get("warnings", [])
        }
        
        # 9. 构建 next_meal_suggestion（下一餐建议）
        next_meal = smart_advice.get("next_meal", {})
        next_meal_suggestion = {
            "recommended_time": next_meal.get("recommended_time", "4小时后"),
            "meal_type": next_meal.get("meal_type", "下一餐"),
            "calorie_budget": next_meal.get("calorie_budget", 600),
            "focus_nutrients": next_meal.get("focus_nutrients", ["蔬菜", "膳食纤维"]),
            "avoid": next_meal.get("avoid", ["高糖食物"])
        }
        
        return {
            "session_id": session_id,
            "status": "ended",
            # 手机端期望的扁平格式（向后兼容）
            "total_consumed_kcal": stats["total_consumed_kcal"],
            "consumption_ratio": stats["consumption_ratio"],
            "duration_minutes": int(duration_minutes),
            "report": final_report,
            # 营养成分总量
            "total_protein": total_protein,
            "total_carbs": total_carbs,
            "total_fat": total_fat,
            # 营养成分百分比（用于饼状图）
            "nutrition_breakdown": nutrition_breakdown,
            # ===== 新增：智能建议系统 =====
            # 用餐总结（用于眼镜显示）
            "meal_summary": meal_summary,
            # 详细建议（用于手机显示）
            "advice": advice,
            # 下一餐建议
            "next_meal_suggestion": next_meal_suggestion,
            # ===== 兼容字段 =====
            "final_stats": final_stats,
            "message": "用餐记录已完成"
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"结束用餐会话失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"结束用餐会话失败: {str(e)}")


@app.get("/api/v1/meal/session/{session_id}")
async def get_meal_session(session_id: str, db: Session = Depends(get_db)):
    """获取用餐会话详情"""
    db_session = db.query(models.MealSession).filter(models.MealSession.id == session_id).first()
    if not db_session:
        raise HTTPException(status_code=404, detail="会话不存在")
        
    # 使用动态逻辑获取进度
    stats = calculate_dynamic_stats(session_id, db)
    
    return {
        "session": {
            "id": db_session.id,
            "user_id": db_session.user_id,
            "status": db_session.status,
            "start_time": db_session.start_time.isoformat() if isinstance(db_session.start_time, datetime) else db_session.start_time,
            "end_time": db_session.end_time if isinstance(db_session.end_time, str) else (db_session.end_time.isoformat() if db_session.end_time else None),
            "auto_capture_interval": db_session.auto_capture_interval,
        },
        "progress": {
            "total_served_calories": stats["total_served_kcal"],
            "current_calories": stats["current_remaining_kcal"],
            "total_consumed": stats["total_consumed_kcal"],
            "consumption_ratio": stats["consumption_ratio"],
            "duration_minutes": stats["duration_minutes"]
        }
    }


@app.get("/api/v1/meal/sessions")
async def list_meal_sessions(user_id: str = "default_user", status: str = None, db: Session = Depends(get_db)):
    """获取用户的用餐会话列表"""
    q = db.query(models.MealSession).filter(models.MealSession.user_id == user_id)
    if status:
        q = q.filter(models.MealSession.status == status)
    rows = q.order_by(models.MealSession.start_time.desc()).all()
    result = []
    for s in rows:
        first_snapshot = db.query(models.MealSnapshot).filter(models.MealSnapshot.session_id == s.id).order_by(models.MealSnapshot.captured_at.asc()).first()
        last_snapshot = db.query(models.MealSnapshot).filter(models.MealSnapshot.session_id == s.id).order_by(models.MealSnapshot.captured_at.desc()).first()
        initial_kcal = first_snapshot.total_kcal if first_snapshot else 0
        current_kcal = last_snapshot.total_kcal if last_snapshot else 0
        result.append({
            "session_id": s.id,
            "start_time": s.start_time.isoformat() if isinstance(s.start_time, datetime) else s.start_time,
            "end_time": s.end_time if isinstance(s.end_time, str) else (s.end_time.isoformat() if s.end_time else None),
            "status": s.status,
            "initial_calories": initial_kcal,
            "current_calories": current_kcal,
            "snapshots_count": db.query(models.MealSnapshot).filter(models.MealSnapshot.session_id == s.id).count()
        })
    return {"sessions": result, "total": len(result)}


# ==================== 食物数据编辑 API ====================

class UpdateFoodRequest(BaseModel):
    """更新食物数据请求"""
    food_id: str
    weight_g: Optional[float] = None
    calories_kcal: Optional[float] = None
    protein_g: Optional[float] = None
    carbs_g: Optional[float] = None
    fat_g: Optional[float] = None
    edited_at: int  # 毫秒时间戳


@app.put("/api/v1/meal/food/{food_id}")
async def update_food(food_id: str, request: UpdateFoodRequest, db: Session = Depends(get_db)):
    """
    更新食物营养数据
    
    用于同步用户在手机端编辑的食物数据
    
    Args:
        food_id: 食物ID
        request: 更新数据
        
    Returns:
        success: 是否成功
        message: 消息
        updated_at: 更新时间戳
    """
    try:
        # 查找食物记录
        food = db.query(models.SnapshotFood).filter(
            models.SnapshotFood.id == food_id
        ).first()
        
        if not food:
            raise HTTPException(status_code=404, detail=f"食物不存在: {food_id}")
        
        # 更新字段
        if request.weight_g is not None:
            food.weight_g = request.weight_g
        if request.calories_kcal is not None:
            food.calories_kcal = request.calories_kcal
        if request.protein_g is not None:
            food.protein_g = request.protein_g
        if request.carbs_g is not None:
            food.carbs_g = request.carbs_g
        if request.fat_g is not None:
            food.fat_g = request.fat_g
        
        # 记录编辑时间
        food.edited_at = datetime.fromtimestamp(request.edited_at / 1000)
        food.is_edited = True
        
        db.commit()
        
        logger.info(f"食物数据已更新: food_id={food_id}")
        
        return {
            "success": True,
            "message": "更新成功",
            "updated_at": int(datetime.utcnow().timestamp() * 1000)
        }
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"更新食物数据失败: {e}", exc_info=True)
        db.rollback()
        raise HTTPException(status_code=500, detail=f"更新失败: {str(e)}")


@app.get("/api/v1/stats/daily")
async def get_daily_stats(
    user_id: str = "default_user",
    date: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """
    获取用户某一天的营养统计
    
    Args:
        user_id: 用户ID
        date: 日期（格式: YYYY-MM-DD），默认为今天
        
    Returns:
        {
            "date": "2025-11-25",
            "total_calories": 1850,
            "total_protein": 75.5,
            "total_carbs": 220.3,
            "total_fat": 65.2,
            "target_calories": 2000,
            "meals": [
                {"meal_type": "breakfast", "calories": 450, "time": "08:30"},
                {"meal_type": "lunch", "calories": 780, "time": "12:15"},
                {"meal_type": "dinner", "calories": 620, "time": "18:45"}
            ],
            "nutrition_breakdown": {
                "protein_percent": 16.3,
                "carbs_percent": 47.6,
                "fat_percent": 36.1
            }
        }
    """
    from datetime import date as date_type
    
    # 解析日期
    if date:
        try:
            target_date = datetime.strptime(date, "%Y-%m-%d").date()
        except ValueError:
            raise HTTPException(status_code=400, detail="日期格式错误，请使用 YYYY-MM-DD")
    else:
        target_date = datetime.utcnow().date()
    
    # 查询当天的所有已完成会话
    start_of_day = datetime.combine(target_date, datetime.min.time())
    end_of_day = datetime.combine(target_date, datetime.max.time())
    
    sessions = db.query(models.MealSession).filter(
        models.MealSession.user_id == user_id,
        models.MealSession.start_time >= start_of_day,
        models.MealSession.start_time <= end_of_day
    ).order_by(models.MealSession.start_time.asc()).all()
    
    # 汇总统计
    total_calories = 0.0
    total_protein = 0.0
    total_carbs = 0.0
    total_fat = 0.0
    meals = []
    
    for session in sessions:
        stats = calculate_dynamic_stats(session.id, db)
        consumed_kcal = stats["total_consumed_kcal"]
        consumed_protein = stats["total_consumed_protein_g"]
        consumed_carbs = stats["total_consumed_carbs_g"]
        consumed_fat = stats["total_consumed_fat_g"]
        
        total_calories += consumed_kcal
        total_protein += consumed_protein
        total_carbs += consumed_carbs
        total_fat += consumed_fat
        
        # 根据时间判断餐类型
        hour = session.start_time.hour if session.start_time else 12
        if hour < 10:
            meal_type = "breakfast"
        elif hour < 14:
            meal_type = "lunch"
        elif hour < 17:
            meal_type = "snack"
        else:
            meal_type = "dinner"
        
        meals.append({
            "session_id": session.id,
            "meal_type": meal_type,
            "calories": consumed_kcal,
            "protein": consumed_protein,
            "carbs": consumed_carbs,
            "fat": consumed_fat,
            "time": session.start_time.strftime("%H:%M") if session.start_time else None,
            "status": session.status
        })
    
    # 计算营养成分百分比
    total_macro = total_protein + total_carbs + total_fat
    nutrition_breakdown = {
        "protein_percent": round(total_protein / total_macro * 100, 1) if total_macro > 0 else 0,
        "carbs_percent": round(total_carbs / total_macro * 100, 1) if total_macro > 0 else 0,
        "fat_percent": round(total_fat / total_macro * 100, 1) if total_macro > 0 else 0
    }
    
    # 计算目标完成度（默认目标 2000 kcal）
    target_calories = 2000.0  # TODO: 从用户档案获取
    calories_ratio = round(total_calories / target_calories * 100, 1) if target_calories > 0 else 0
    
    return {
        "date": target_date.isoformat(),
        "user_id": user_id,
        "total_calories": round(total_calories, 1),
        "total_protein": round(total_protein, 1),
        "total_carbs": round(total_carbs, 1),
        "total_fat": round(total_fat, 1),
        "target_calories": target_calories,
        "calories_ratio": calories_ratio,
        "meals": meals,
        "meals_count": len(meals),
        "nutrition_breakdown": nutrition_breakdown
    }


def generate_meal_report(final_stats: dict) -> str:
    """生成最终用餐报告"""
    consumption_ratio = final_stats["consumption_ratio"]
    duration = final_stats["duration_minutes"]
    eating_speed = final_stats["average_eating_speed"]
    
    report_parts = []
    
    # 摄入量评价
    if consumption_ratio > 0.9:
        report_parts.append("✅ 用餐完整，营养摄入充足")
    elif consumption_ratio > 0.7:
        report_parts.append("✅ 用餐适量，摄入量合理")
    elif consumption_ratio > 0.5:
        report_parts.append("⚠️ 摄入较少，注意营养补充")
    else:
        report_parts.append("⚠️ 摄入很少，可能影响营养")
    
    # 用餐时长评价
    if 10 <= duration <= 25:
        report_parts.append("✅ 用餐时长适中")
    elif duration < 10:
        report_parts.append("⚠️ 用餐较快，建议细嚼慢咽")
    else:
        report_parts.append("⚠️ 用餐时间较长")
    
    # 进食速度评价
    if 20 <= eating_speed <= 40:
        report_parts.append("✅ 进食速度健康")
    elif eating_speed > 40:
        report_parts.append("⚠️ 进食速度较快")
    else:
        report_parts.append("✅ 进食速度较慢，有助消化")
    
    return " | ".join(report_parts)


# ==================== 智能建议生成系统 ====================

def calculate_meal_rating(
    final_stats: dict,
    nutrition_breakdown: dict,
    daily_context: Optional[DailyContextPayload] = None,
    user_profile: Optional[UserProfilePayload] = None
) -> str:
    """计算用餐评级"""
    score = 100
    
    duration = final_stats.get("duration_minutes", 0)
    total_consumed = final_stats.get("total_consumed", 0)
    protein_pct = nutrition_breakdown.get("protein_percent", 0)
    fat_pct = nutrition_breakdown.get("fat_percent", 0)
    
    # 用餐时长评分
    if duration < 10:
        score -= 20  # 太快
    elif duration > 45:
        score -= 10  # 太慢
    
    # 营养均衡评分
    if protein_pct < 15:
        score -= 15  # 蛋白质不足
    elif protein_pct > 35:
        score -= 5  # 蛋白质过高
    
    if fat_pct > 40:
        score -= 15  # 脂肪过高
    
    # 今日摄入评分
    if daily_context:
        target_calories = 2000
        if user_profile and user_profile.target_calories:
            target_calories = user_profile.target_calories
        
        total_today = daily_context.total_calories_today + total_consumed
        if total_today > target_calories * 1.2:
            score -= 20  # 严重超标
        elif total_today > target_calories * 1.1:
            score -= 10  # 轻微超标
    
    # 返回评级
    if score >= 80:
        return "good"
    elif score >= 60:
        return "fair"
    else:
        return "poor"


def generate_fallback_advice(
    final_stats: dict,
    nutrition_breakdown: dict,
    daily_context: Optional[DailyContextPayload] = None,
    user_profile: Optional[UserProfilePayload] = None
) -> dict:
    """LLM 调用失败时的规则引擎降级策略"""
    
    suggestions = []
    highlights = []
    warnings = []
    
    # 获取数据
    duration = final_stats.get("duration_minutes", 0)
    total_consumed = final_stats.get("total_consumed", 0)
    protein_pct = nutrition_breakdown.get("protein_percent", 0)
    carbs_pct = nutrition_breakdown.get("carbs_percent", 0)
    fat_pct = nutrition_breakdown.get("fat_percent", 0)
    protein_g = nutrition_breakdown.get("protein_g", 0)
    carbs_g = nutrition_breakdown.get("carbs_g", 0)
    fat_g = nutrition_breakdown.get("fat_g", 0)
    
    # 用餐时长评价
    if duration < 10:
        warnings.append("用餐时间较短，建议细嚼慢咽")
        suggestions.append(f"用餐时长 {int(duration)} 分钟，节奏较快，建议放慢进食速度")
    elif 15 <= duration <= 30:
        highlights.append("用餐节奏健康")
        suggestions.append(f"用餐时长 {int(duration)} 分钟，节奏适中，有助于消化吸收")
    elif duration > 40:
        warnings.append("用餐时间较长，注意食物温度")
    
    # 营养比例评价
    if protein_pct >= 20 and protein_pct <= 30:
        highlights.append("蛋白质充足")
    elif protein_pct < 15:
        suggestions.append("蛋白质偏低，建议增加鱼肉蛋奶摄入")
    
    if fat_pct > 40:
        warnings.append("脂肪偏高，建议减少油炸食品")
    elif fat_pct <= 30:
        highlights.append("脂肪控制合理")
    
    if 45 <= carbs_pct <= 60:
        highlights.append("碳水比例适中")
    elif carbs_pct > 65:
        suggestions.append("碳水偏高，可适当增加蔬菜比例")
    
    # 今日摄入评价
    if daily_context:
        total_today = daily_context.total_calories_today + total_consumed
        target = 2000
        if user_profile and user_profile.target_calories:
            target = user_profile.target_calories
        
        remaining = target - total_today
        if remaining > 0:
            suggestions.append(f"今日已摄入 {int(total_today)} kcal，剩余配额 {int(remaining)} kcal")
        else:
            warnings.append(f"今日已超标 {int(-remaining)} kcal，建议控制下一餐")
        
        # 蛋白质摄入
        daily_protein = daily_context.total_protein_today + protein_g
        suggestions.append(f"今日蛋白质摄入已达 {int(daily_protein)}g")
    
    # 如果没有特别建议，给一个通用的
    if not suggestions:
        suggestions.append("继续保持健康饮食习惯")
    
    if not highlights:
        highlights.append("用餐记录完成")
    
    # 生成总结
    if total_consumed > 600:
        summary = f"本餐摄入 {int(total_consumed)} 千卡，热量充足"
    elif total_consumed > 300:
        summary = f"本餐摄入 {int(total_consumed)} 千卡，摄入适中"
    else:
        summary = f"本餐摄入 {int(total_consumed)} 千卡，摄入较少"
    
    # 如果有亮点，修改总结
    if highlights and not warnings:
        summary = f"本餐营养均衡，{highlights[0]}"
    elif warnings:
        summary = f"本餐需注意：{warnings[0]}"
    
    # 生成简短建议（≤20字符）
    if warnings:
        short_advice = warnings[0][:20]
    elif highlights:
        short_advice = f"{highlights[0]}，继续保持！"[:20]
    else:
        short_advice = "用餐记录已完成"
    
    # 计算评级
    rating = calculate_meal_rating(final_stats, nutrition_breakdown, daily_context, user_profile)
    
    # 下一餐建议
    next_meal = generate_fallback_next_meal(daily_context, user_profile, total_consumed)
    
    return {
        "rating": rating,
        "short_advice": short_advice,
        "summary": summary,
        "suggestions": suggestions[:4],  # 最多4条
        "highlights": highlights[:3],
        "warnings": warnings[:2],
        "next_meal": next_meal
    }


def generate_fallback_next_meal(
    daily_context: Optional[DailyContextPayload],
    user_profile: Optional[UserProfilePayload],
    current_meal_calories: float
) -> dict:
    """生成下一餐建议（降级版）"""
    from datetime import datetime
    
    current_hour = datetime.now().hour
    
    # 判断下一餐类型
    if current_hour < 10:
        next_meal_type = "午餐"
        recommended_time = "3-4小时后"
    elif current_hour < 14:
        next_meal_type = "晚餐"
        recommended_time = "5-6小时后"
    elif current_hour < 18:
        next_meal_type = "晚餐"
        recommended_time = "2-3小时后"
    else:
        next_meal_type = "早餐"
        recommended_time = "明早"
    
    # 计算热量预算
    target = 2000
    if user_profile and user_profile.target_calories:
        target = user_profile.target_calories
    
    total_today = current_meal_calories
    if daily_context:
        total_today += daily_context.total_calories_today
    
    remaining = max(0, target - total_today)
    
    # 根据剩余热量计算下一餐预算
    if next_meal_type == "早餐":
        calorie_budget = target * 0.3  # 早餐约30%
    elif next_meal_type == "午餐":
        calorie_budget = min(remaining * 0.6, target * 0.35)
    else:
        calorie_budget = min(remaining, target * 0.35)
    
    # 建议补充的营养
    focus_nutrients = ["蔬菜", "膳食纤维"]
    avoid = []
    
    if daily_context:
        if daily_context.total_protein_today < 40:
            focus_nutrients.append("优质蛋白")
        if daily_context.total_fat_today > 50:
            avoid.append("油炸食品")
            avoid.append("高脂肪食物")
    
    if not avoid:
        avoid = ["高糖食物", "过度加工食品"]
    
    return {
        "recommended_time": recommended_time,
        "meal_type": next_meal_type,
        "calorie_budget": round(calorie_budget, 0),
        "focus_nutrients": focus_nutrients,
        "avoid": avoid
    }


async def generate_llm_meal_advice(
    final_stats: dict,
    nutrition_breakdown: dict,
    meal_context: Optional[MealContextPayload] = None,
    daily_context: Optional[DailyContextPayload] = None,
    user_profile: Optional[UserProfilePayload] = None
) -> Optional[dict]:
    """使用 LLM 生成个性化营养建议"""
    
    if not _qwen_client:
        logger.warning("Qwen 客户端未配置，使用降级策略")
        return None
    
    # 构建提示词
    total_calories = final_stats.get("total_consumed", 0)
    duration = final_stats.get("duration_minutes", 0)
    eating_speed = final_stats.get("average_eating_speed", 0)
    protein_g = nutrition_breakdown.get("protein_g", 0)
    carbs_g = nutrition_breakdown.get("carbs_g", 0)
    fat_g = nutrition_breakdown.get("fat_g", 0)
    
    # 目标热量
    target_calories = 2000
    if user_profile and user_profile.target_calories:
        target_calories = user_profile.target_calories
    
    # 今日数据
    daily_calories = daily_context.total_calories_today if daily_context else 0
    daily_protein = daily_context.total_protein_today if daily_context else 0
    daily_carbs = daily_context.total_carbs_today if daily_context else 0
    daily_fat = daily_context.total_fat_today if daily_context else 0
    meal_count = daily_context.meal_count_today if daily_context else 1
    hours_ago = daily_context.last_meal_hours_ago if daily_context else 0
    
    # 用户信息
    age = user_profile.age if user_profile else None
    gender = "男性" if user_profile and user_profile.gender == "male" else ("女性" if user_profile and user_profile.gender == "female" else "未知")
    bmi = user_profile.bmi if user_profile else None
    activity_level = {
        "sedentary": "久坐少动",
        "light": "轻度活动",
        "moderate": "中等活动",
        "active": "活跃运动"
    }.get(user_profile.activity_level if user_profile else "", "未知")
    health_goal = {
        "lose": "减重",
        "maintain": "维持体重",
        "gain": "增重"
    }.get(user_profile.health_goal if user_profile else "", "维持健康")
    health_conditions = ", ".join(user_profile.health_conditions) if user_profile and user_profile.health_conditions else "无"
    dietary_preferences = ", ".join(user_profile.dietary_preferences) if user_profile and user_profile.dietary_preferences else "无"
    
    system_prompt = """你是一位专业营养师和健康管理专家。请根据用户的用餐数据生成个性化的营养建议。

要求：
1. 语气友好、鼓励，避免批评性语言
2. 建议具体、可执行
3. 考虑用户的健康目标和身体状况
4. short_advice 必须 ≤20 个中文字符（用于眼镜显示）
5. 返回严格的 JSON 格式，不要有任何额外文字"""

    user_prompt = f"""请根据以下用餐数据生成个性化建议：

## 本餐数据
- 总热量：{total_calories:.0f} kcal
- 蛋白质：{protein_g:.1f} g
- 碳水化合物：{carbs_g:.1f} g
- 脂肪：{fat_g:.1f} g
- 用餐时长：{duration:.0f} 分钟
- 进食速度：{eating_speed:.1f} kcal/分钟

## 今日摄入
- 今日总热量：{daily_calories + total_calories:.0f} kcal（目标 {target_calories:.0f} kcal）
- 今日蛋白质：{daily_protein + protein_g:.1f} g
- 今日碳水：{daily_carbs + carbs_g:.1f} g
- 今日脂肪：{daily_fat + fat_g:.1f} g
- 今日用餐次数：{meal_count + 1}
- 距上次用餐：{hours_ago:.1f} 小时

## 用户信息
- 年龄：{age if age else '未知'} 岁
- 性别：{gender}
- BMI：{bmi if bmi else '未知'}
- 活动水平：{activity_level}
- 健康目标：{health_goal}
- 健康状况：{health_conditions}
- 饮食偏好：{dietary_preferences}

请生成以下 JSON 格式的建议：
{{
  "rating": "good/fair/poor",
  "short_advice": "≤20字的简短建议",
  "summary": "一句话总结",
  "suggestions": ["建议1", "建议2", "建议3"],
  "highlights": ["亮点1", "亮点2"],
  "warnings": ["警告1"] 或 [],
  "next_meal": {{
    "recommended_time": "X小时后",
    "meal_type": "早餐/午餐/晚餐/加餐",
    "calorie_budget": 数字,
    "focus_nutrients": ["营养1", "营养2"],
    "avoid": ["食物1", "食物2"]
  }}
}}"""

    try:
        completion = _qwen_client.chat.completions.create(
            model=QWEN_TEXT_MODEL,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            temperature=0.7,
            max_tokens=1000
        )
        
        response_text = completion.choices[0].message.content.strip()
        
        # 尝试解析 JSON
        try:
            # 先尝试直接解析
            result = json.loads(response_text)
        except json.JSONDecodeError:
            # 尝试提取 JSON 块
            import re
            json_match = re.search(r'```(?:json)?\s*([\s\S]*?)\s*```', response_text)
            if json_match:
                result = json.loads(json_match.group(1))
            else:
                # 尝试找到 { 开头的 JSON
                json_start = response_text.find('{')
                if json_start >= 0:
                    result = json.loads(response_text[json_start:])
                else:
                    logger.error(f"LLM 返回内容非合法 JSON: {response_text[:200]}")
                    return None
        
        # 验证必要字段
        required_fields = ["rating", "short_advice", "summary", "suggestions"]
        for field in required_fields:
            if field not in result:
                logger.warning(f"LLM 响应缺少字段: {field}")
                return None
        
        # 确保 short_advice 不超过20字符
        if len(result.get("short_advice", "")) > 20:
            result["short_advice"] = result["short_advice"][:20]
        
        # 确保数组字段存在
        result.setdefault("highlights", [])
        result.setdefault("warnings", [])
        result.setdefault("next_meal", generate_fallback_next_meal(daily_context, user_profile, total_calories))
        
        logger.info(f"LLM 建议生成成功: rating={result['rating']}")
        return result
        
    except Exception as e:
        logger.error(f"LLM 调用失败: {e}", exc_info=True)
        return None


async def generate_smart_meal_advice(
    final_stats: dict,
    nutrition_breakdown: dict,
    meal_context: Optional[MealContextPayload] = None,
    daily_context: Optional[DailyContextPayload] = None,
    user_profile: Optional[UserProfilePayload] = None
) -> dict:
    """
    生成智能餐饮建议（主入口）
    
    优先使用 LLM，失败时降级到规则引擎
    """
    # 尝试 LLM 生成
    llm_result = await generate_llm_meal_advice(
        final_stats, nutrition_breakdown, meal_context, daily_context, user_profile
    )
    
    if llm_result:
        return llm_result
    
    # 降级到规则引擎
    logger.info("使用降级策略生成建议")
    return generate_fallback_advice(final_stats, nutrition_breakdown, daily_context, user_profile)


def generate_meal_advice(final_stats: dict, nutrition_breakdown: dict) -> dict:
    """
    生成餐饮建议（兼容旧接口）
    
    返回格式符合前端期望：
    {
        "summary": "简短总结",
        "suggestions": ["建议1", "建议2", ...]
    }
    """
    result = generate_fallback_advice(final_stats, nutrition_breakdown)
    return {
        "summary": result["summary"],
        "suggestions": result["suggestions"]
    }


class UserProfile(BaseModel):
    """用户健康档案"""
    age: Optional[int] = None
    gender: Optional[str] = None  # "male" / "female"
    height_cm: Optional[float] = None
    weight_kg: Optional[float] = None
    bmi: Optional[float] = None
    health_conditions: Optional[List[str]] = None  # ["轻度脂肪肝", "高血压"]
    dietary_preferences: Optional[List[str]] = None  # ["低油", "低盐", "素食"]
    activity_level: Optional[str] = None  # "sedentary", "light", "moderate", "active"
    target_calories: Optional[float] = None


class MealContext(BaseModel):
    """当前用餐上下文"""
    session_id: Optional[str] = None
    total_calories: Optional[float] = None
    total_protein: Optional[float] = None
    total_carbs: Optional[float] = None
    total_fat: Optional[float] = None
    foods: Optional[List[str]] = None  # ["红烧肉", "米饭"]


class ChatNutritionRequest(BaseModel):
    query: str
    context: Optional[str] = None
    user_id: Optional[str] = "default_user"
    # 新增：用户档案（用于个性化建议）
    user_profile: Optional[UserProfile] = None
    # 新增：当前用餐上下文
    meal_context: Optional[MealContext] = None
    # 新增：消息类型
    message_type: Optional[str] = "chat"  # "chat", "meal_start", "meal_end"


class ChatNutritionResponse(BaseModel):
    answer: str
    suggested_actions: List[str] = []
    # 新增：针对当前用餐的建议
    meal_advice: Optional[str] = None


def _build_user_profile_prompt(profile: UserProfile) -> str:
    """构建用户档案的提示词片段"""
    parts = []
    
    if profile.age:
        parts.append(f"年龄: {profile.age}岁")
    if profile.gender:
        gender_cn = "男性" if profile.gender == "male" else "女性"
        parts.append(f"性别: {gender_cn}")
    if profile.bmi:
        bmi_status = ""
        if profile.bmi < 18.5:
            bmi_status = "偏瘦"
        elif profile.bmi < 24:
            bmi_status = "正常"
        elif profile.bmi < 28:
            bmi_status = "偏胖"
        else:
            bmi_status = "肥胖"
        parts.append(f"BMI: {profile.bmi} ({bmi_status})")
    if profile.health_conditions:
        parts.append(f"健康状况: {', '.join(profile.health_conditions)}")
    if profile.dietary_preferences:
        parts.append(f"饮食偏好: {', '.join(profile.dietary_preferences)}")
    if profile.activity_level:
        level_cn = {
            "sedentary": "久坐少动",
            "light": "轻度活动",
            "moderate": "中等活动",
            "active": "活跃运动"
        }.get(profile.activity_level, profile.activity_level)
        parts.append(f"活动水平: {level_cn}")
    if profile.target_calories:
        parts.append(f"每日目标热量: {profile.target_calories} kcal")
    
    return "\n".join(parts) if parts else ""


def _build_meal_context_prompt(meal: MealContext) -> str:
    """构建用餐上下文的提示词片段"""
    parts = []
    
    if meal.foods:
        parts.append(f"当前餐食: {', '.join(meal.foods)}")
    if meal.total_calories:
        parts.append(f"热量: {meal.total_calories} kcal")
    if meal.total_protein:
        parts.append(f"蛋白质: {meal.total_protein}g")
    if meal.total_carbs:
        parts.append(f"碳水化合物: {meal.total_carbs}g")
    if meal.total_fat:
        parts.append(f"脂肪: {meal.total_fat}g")
    
    return "\n".join(parts) if parts else ""


@app.post("/api/v1/chat/nutrition", response_model=ChatNutritionResponse)
async def chat_nutrition(request: ChatNutritionRequest):
    """
    智能营养助手对话接口 (基于 Qwen-Plus)
    
    支持个性化建议：
    - user_profile: 用户健康档案（年龄、BMI、健康状况、饮食偏好）
    - meal_context: 当前用餐上下文（食物、热量、营养成分）
    - message_type: 消息类型（chat/meal_start/meal_end）
    
    示例请求:
    {
        "query": "分析本餐",
        "user_profile": {
            "age": 45,
            "bmi": 26.5,
            "health_conditions": ["轻度脂肪肝"],
            "dietary_preferences": ["低油"]
        },
        "meal_context": {
            "foods": ["红烧肉", "米饭"],
            "total_calories": 780
        },
        "message_type": "meal_start"
    }
    """
    try:
        client = get_qwen_client()
        
        # 构建系统提示词
        system_prompt = """你是一位专业的 AI 营养师和健康管理专家。
你的任务是根据用户的健康档案和当前餐食情况，给出个性化的饮食建议。

请遵循以下原则：
1. 回答简洁明了，适合语音播报（1-2句话为佳，避免过长列表）。
2. 语气亲切、鼓励。
3. 根据用户的健康状况给出针对性建议（如脂肪肝患者建议低油饮食）。
4. 如果用户询问具体的医疗建议，请提醒咨询专业医生。
5. 结合当前餐食的营养成分进行分析。
"""
        
        messages = [{"role": "system", "content": system_prompt}]
        
        # 添加用户档案上下文
        if request.user_profile:
            profile_prompt = _build_user_profile_prompt(request.user_profile)
            if profile_prompt:
                messages.append({
                    "role": "system",
                    "content": f"【用户健康档案】\n{profile_prompt}"
                })
        
        # 添加用餐上下文
        if request.meal_context:
            meal_prompt = _build_meal_context_prompt(request.meal_context)
            if meal_prompt:
                messages.append({
                    "role": "system",
                    "content": f"【当前用餐情况】\n{meal_prompt}"
                })
        
        # 添加旧版 context（向后兼容）
        if request.context:
            messages.append({
                "role": "system",
                "content": f"【补充信息】\n{request.context}"
            })
        
        # 根据消息类型调整查询
        user_query = request.query
        if request.message_type == "meal_start":
            user_query = f"我刚开始用餐，{request.query}。请根据我的健康档案给出简短的饮食建议。"
        elif request.message_type == "meal_end":
            user_query = f"我刚吃完这顿饭，{request.query}。请根据我的健康档案评价这顿饭并给出建议。"
        
        messages.append({"role": "user", "content": user_query})
        
        completion = client.chat.completions.create(
            model=QWEN_TEXT_MODEL,
            messages=messages,
            temperature=0.7,
            max_tokens=500
        )
        
        answer = completion.choices[0].message.content
        
        # 提取建议动作
        actions = []
        if "喝水" in answer or "饮水" in answer:
            actions.append("记录饮水")
        if "运动" in answer or "活动" in answer:
            actions.append("查看运动建议")
        if "蔬菜" in answer or "蓬菜" in answer:
            actions.append("添加蔬菜")
        if "减少" in answer and ("油" in answer or "脂肪" in answer):
            actions.append("选择低脂食物")
        
        # 生成针对用餐的简短建议
        meal_advice = None
        if request.meal_context and request.user_profile:
            # 根据用户健康状况生成特定建议
            health_conditions = request.user_profile.health_conditions or []
            if "脂肪肝" in " ".join(health_conditions):
                if request.meal_context.total_fat and request.meal_context.total_fat > 30:
                    meal_advice = "建议减少油脂摄入"
            if "高血压" in " ".join(health_conditions):
                meal_advice = meal_advice or "建议减少盐分摄入"
        
        return ChatNutritionResponse(
            answer=answer,
            suggested_actions=actions,
            meal_advice=meal_advice
        )

    except Exception as e:
        logger.error(f"Chat API Error: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"AI 服务暂时不可用: {str(e)}")

# ==================== 数据管理 API ====================

@app.get("/api/v1/admin/storage/stats")
async def get_storage_stats():
    """获取存储统计信息"""
    upload_dir = Path("uploads")
    if not upload_dir.exists():
        return {"error": "Upload directory not found"}
    
    files = list(upload_dir.glob("*"))
    total_size = sum(f.stat().st_size for f in files if f.is_file())
    
    # 按日期分组
    by_date = {}
    for f in files:
        if f.is_file():
            date = datetime.fromtimestamp(f.stat().st_mtime).strftime("%Y-%m-%d")
            if date not in by_date:
                by_date[date] = {"count": 0, "size": 0}
            by_date[date]["count"] += 1
            by_date[date]["size"] += f.stat().st_size
    
    return {
        "total_files": len([f for f in files if f.is_file()]),
        "total_size_mb": round(total_size / 1024 / 1024, 2),
        "by_date": by_date,
        "oldest_file": min((f.stat().st_mtime for f in files if f.is_file()), default=None),
        "newest_file": max((f.stat().st_mtime for f in files if f.is_file()), default=None),
    }


@app.post("/api/v1/admin/storage/cleanup")
async def cleanup_old_images(days: int = 30, dry_run: bool = True):
    """清理过期图片
    
    Args:
        days: 保留最近多少天的图片
        dry_run: 是否仅预览（不实际删除）
    """
    upload_dir = Path("uploads")
    if not upload_dir.exists():
        return {"error": "Upload directory not found"}
    
    cutoff = datetime.now().timestamp() - (days * 24 * 60 * 60)
    files = list(upload_dir.glob("*"))
    
    to_delete = []
    for f in files:
        if f.is_file() and f.stat().st_mtime < cutoff:
            to_delete.append({
                "filename": f.name,
                "size": f.stat().st_size,
                "modified": datetime.fromtimestamp(f.stat().st_mtime).isoformat()
            })
    
    freed_bytes = sum(f["size"] for f in to_delete)
    
    if not dry_run:
        for f in to_delete:
            try:
                (upload_dir / f["filename"]).unlink()
            except Exception as e:
                logger.error(f"Failed to delete {f['filename']}: {e}")
    
    return {
        "dry_run": dry_run,
        "files_to_delete": len(to_delete),
        "freed_mb": round(freed_bytes / 1024 / 1024, 2),
        "files": to_delete[:20],  # 只返回前20个
        "message": "预览模式，未实际删除" if dry_run else f"已删除 {len(to_delete)} 个文件"
    }


@app.get("/api/v1/admin/api-stats")
async def get_api_stats():
    """获取 API 使用统计（基于日志估算）"""
    # 这里简化实现，实际应该从数据库读取
    return {
        "message": "API 统计功能需要启用数据库日志",
        "tip": "运行 alembic upgrade head 创建扩展表后可用",
        "endpoints": {
            "/api/v1/vision/analyze": {"description": "图像识别", "avg_time_ms": "~5000"},
            "/api/v1/nutrition/lookup": {"description": "营养查询", "avg_time_ms": "~50"},
            "/api/v1/meal/start": {"description": "开始用餐", "avg_time_ms": "~100"},
        }
    }


# ==================== 定时任务 ====================

from contextlib import asynccontextmanager
import asyncio

async def scheduled_cleanup():
    """定时清理任务（每天凌晨3点执行）"""
    while True:
        now = datetime.now()
        # 计算到凌晨3点的秒数
        target = now.replace(hour=3, minute=0, second=0, microsecond=0)
        if now >= target:
            target = target.replace(day=target.day + 1)
        wait_seconds = (target - now).total_seconds()
        
        await asyncio.sleep(wait_seconds)
        
        try:
            logger.info("开始执行定时清理任务")
            upload_dir = Path("uploads")
            if upload_dir.exists():
                cutoff = datetime.now().timestamp() - (30 * 24 * 60 * 60)
                count = 0
                for f in upload_dir.glob("*"):
                    if f.is_file() and f.stat().st_mtime < cutoff:
                        f.unlink()
                        count += 1
                logger.info(f"清理完成，删除 {count} 个过期图片")
        except Exception as e:
            logger.error(f"清理任务失败: {e}")


# 启动时运行清理任务（可选）
# @app.on_event("startup")
# async def start_cleanup_task():
#     asyncio.create_task(scheduled_cleanup())


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=os.getenv("HOST", "0.0.0.0"),
        port=int(os.getenv("PORT", 8000)),
        reload=os.getenv("DEBUG", "True") == "True"
    )
