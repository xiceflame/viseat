"""
Rokid 营养助手 - 多数据源营养数据库管理模块

数据来源:
1. 中国食物成分表（第6版）- 1838种中国食物，32个营养字段
2. USDA Foundation Foods - 美国农业部基础食物数据
3. FoodStruct - 国际通用食物营养数据

融合策略:
- 中文食物名优先匹配中国食物成分表
- 英文食物名优先匹配 USDA/FoodStruct
- 同名食物取权威数据源（中国 > USDA > FoodStruct）
- 支持别名映射和模糊匹配
"""

import json
import csv
import logging
from pathlib import Path
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field, asdict
from enum import Enum
import re

logger = logging.getLogger(__name__)


class DataSource(Enum):
    """数据来源枚举"""
    CHINA_FOOD_COMPOSITION = "china_food_composition"  # 中国食物成分表
    USDA_FOUNDATION = "usda_foundation"                # USDA Foundation Foods
    FOODSTRUCT = "foodstruct"                          # FoodStruct
    BUILTIN = "builtin"                                # 内置数据
    CATEGORY_DEFAULT = "category_default"              # 分类默认值
    FALLBACK = "fallback"                              # 默认回退值


# 分类默认营养值（每100g）- 作为最终兜底
# 基于各类食物的平均热量设计
CATEGORY_NUTRITION_DEFAULTS = {
    "snack": {
        "calories": 500, "protein": 6, "carbs": 55, "fat": 28,
        "description": "零食（油炸/烘焙类平均）"
    },
    "meal": {
        "calories": 150, "protein": 10, "carbs": 15, "fat": 6,
        "description": "正餐（混合食材平均）"
    },
    "beverage": {
        "calories": 45, "protein": 0, "carbs": 11, "fat": 0,
        "description": "饮料（含糖饮料平均）"
    },
    "dessert": {
        "calories": 350, "protein": 4, "carbs": 50, "fat": 15,
        "description": "甜点（蛋糕/饼干平均）"
    },
    "fruit": {
        "calories": 50, "protein": 0.5, "carbs": 12, "fat": 0.2,
        "description": "水果（常见水果平均）"
    },
    "packaged": {
        "calories": 450, "protein": 8, "carbs": 50, "fat": 22,
        "description": "包装食品（加工食品平均）"
    },
}


@dataclass
class NutritionData:
    """完整营养数据模型 - 保留所有可用字段"""
    
    # 基本信息
    food_code: str = ""           # 食物编码
    food_name: str = ""           # 食物名称（原始）
    food_name_en: str = ""        # 英文名称
    food_name_cn: str = ""        # 中文名称
    category: str = ""            # 食物类别
    source: DataSource = DataSource.FALLBACK  # 数据来源
    
    # 核心营养素（每100g）
    edible: float = 100.0         # 可食部分比例 (%)
    water: float = 0.0            # 水分 (g)
    energy_kcal: float = 0.0      # 能量 (kcal)
    energy_kj: float = 0.0        # 能量 (kJ)
    protein: float = 0.0          # 蛋白质 (g)
    fat: float = 0.0              # 脂肪 (g)
    carbohydrate: float = 0.0     # 碳水化合物 (g)
    dietary_fiber: float = 0.0    # 膳食纤维 (g)
    cholesterol: float = 0.0      # 胆固醇 (mg)
    ash: float = 0.0              # 灰分 (g)
    
    # 维生素
    vitamin_a: float = 0.0        # 维生素A (μg RAE)
    carotene: float = 0.0         # 胡萝卜素 (μg)
    retinol: float = 0.0          # 视黄醇 (μg)
    thiamin: float = 0.0          # 维生素B1/硫胺素 (mg)
    riboflavin: float = 0.0       # 维生素B2/核黄素 (mg)
    niacin: float = 0.0           # 维生素B3/烟酸 (mg)
    vitamin_c: float = 0.0        # 维生素C (mg)
    vitamin_e_total: float = 0.0  # 维生素E总量 (mg)
    vitamin_e_alpha: float = 0.0  # α-生育酚 (mg)
    vitamin_e_beta: float = 0.0   # β-生育酚 (mg)
    vitamin_e_gamma: float = 0.0  # γ-生育酚 (mg)
    
    # 矿物质
    calcium: float = 0.0          # 钙 Ca (mg)
    phosphorus: float = 0.0       # 磷 P (mg)
    potassium: float = 0.0        # 钾 K (mg)
    sodium: float = 0.0           # 钠 Na (mg)
    magnesium: float = 0.0        # 镁 Mg (mg)
    iron: float = 0.0             # 铁 Fe (mg)
    zinc: float = 0.0             # 锌 Zn (mg)
    selenium: float = 0.0         # 硒 Se (μg)
    copper: float = 0.0           # 铜 Cu (mg)
    manganese: float = 0.0        # 锰 Mn (mg)
    
    # 附加信息
    gi_value: Optional[float] = None  # 升糖指数
    remark: str = ""              # 备注
    
    def to_simple_dict(self) -> Dict[str, float]:
        """返回简化的营养字典（用于热量计算）"""
        return {
            "calories": self.energy_kcal,
            "protein": self.protein,
            "carbs": self.carbohydrate,
            "fat": self.fat,
        }
    
    def to_full_dict(self) -> Dict[str, Any]:
        """返回完整的营养字典"""
        return asdict(self)


class NutritionDatabase:
    """
    多数据源营养数据库
    
    融合策略:
    1. 优先级: 中国食物成分表 > USDA > FoodStruct > 内置 > Fallback
    2. 中文名匹配优先使用中国数据源
    3. 英文名匹配优先使用 USDA/FoodStruct
    4. 支持别名映射（rice -> 白米饭, pork -> 猪肉）
    """
    
    # 默认回退值（当所有数据源都找不到时）
    FALLBACK_NUTRITION = NutritionData(
        food_name="unknown",
        energy_kcal=100.0,
        protein=5.0,
        carbohydrate=15.0,
        fat=3.0,
        source=DataSource.FALLBACK,
    )
    
    def __init__(self, data_dir: Optional[Path] = None):
        self.data_dir = data_dir or Path(__file__).resolve().parents[1] / "data"
        
        # 主数据库：食物名 -> NutritionData
        self._db: Dict[str, NutritionData] = {}
        
        # 别名映射：别名 -> 标准名
        self._aliases: Dict[str, str] = {}
        
        # 标准食物名列表（供 VLM 参考）
        self._standard_names_cn: List[str] = []
        self._standard_names_en: List[str] = []
        
        # 数据源统计
        self._source_stats: Dict[DataSource, int] = {}
        
    def load_all(self) -> None:
        """加载所有数据源"""
        logger.info("开始加载营养数据库...")
        
        # 1. 加载中国食物成分表（最高优先级）
        self._load_china_food_composition()
        
        # 2. 加载 USDA Foundation Foods
        self._load_usda_foundation()
        
        # 3. 加载 FoodStruct
        self._load_foodstruct()
        
        # 4. 加载内置别名映射
        self._load_builtin_aliases()
        
        # 5. 构建标准名称列表
        self._build_standard_names()
        
        logger.info(f"营养数据库加载完成: {len(self._db)} 种食物")
        for source, count in self._source_stats.items():
            logger.info(f"  - {source.value}: {count} 种")
    
    def _load_china_food_composition(self) -> None:
        """加载中国食物成分表"""
        json_dir = self.data_dir / "china-food-composition-data" / "json_data"
        if not json_dir.exists():
            logger.warning(f"中国食物成分表目录不存在: {json_dir}")
            return
        
        count = 0
        for json_file in json_dir.glob("*.json"):
            try:
                with open(json_file, "r", encoding="utf-8") as f:
                    foods = json.load(f)
                
                for item in foods:
                    nutrition = self._parse_china_food(item)
                    if nutrition:
                        key = self._normalize_key(nutrition.food_name_cn or nutrition.food_name)
                        self._db[key] = nutrition
                        count += 1
            except Exception as e:
                logger.error(f"加载 {json_file} 失败: {e}")
        
        self._source_stats[DataSource.CHINA_FOOD_COMPOSITION] = count
        logger.info(f"加载中国食物成分表: {count} 种食物")
    
    def _parse_china_food(self, item: Dict) -> Optional[NutritionData]:
        """解析中国食物成分表数据"""
        try:
            def safe_float(v, default=0.0):
                if v is None or v == "" or v == "Tr" or v == "—":
                    return default
                try:
                    return float(v)
                except:
                    return default
            
            return NutritionData(
                food_code=item.get("foodCode", ""),
                food_name=item.get("foodName", ""),
                food_name_cn=item.get("foodName", ""),
                source=DataSource.CHINA_FOOD_COMPOSITION,
                edible=safe_float(item.get("edible"), 100.0),
                water=safe_float(item.get("water")),
                energy_kcal=safe_float(item.get("energyKCal")),
                energy_kj=safe_float(item.get("energyKJ")),
                protein=safe_float(item.get("protein")),
                fat=safe_float(item.get("fat")),
                carbohydrate=safe_float(item.get("CHO")),
                dietary_fiber=safe_float(item.get("dietaryFiber")),
                cholesterol=safe_float(item.get("cholesterol")),
                ash=safe_float(item.get("ash")),
                vitamin_a=safe_float(item.get("vitaminA")),
                carotene=safe_float(item.get("carotene")),
                retinol=safe_float(item.get("retinol")),
                thiamin=safe_float(item.get("thiamin")),
                riboflavin=safe_float(item.get("riboflavin")),
                niacin=safe_float(item.get("niacin")),
                vitamin_c=safe_float(item.get("vitaminC")),
                vitamin_e_total=safe_float(item.get("vitaminETotal")),
                vitamin_e_alpha=safe_float(item.get("vitaminE1")),
                vitamin_e_beta=safe_float(item.get("vitaminE2")),
                vitamin_e_gamma=safe_float(item.get("vitaminE3")),
                calcium=safe_float(item.get("Ca")),
                phosphorus=safe_float(item.get("P")),
                potassium=safe_float(item.get("K")),
                sodium=safe_float(item.get("Na")),
                magnesium=safe_float(item.get("Mg")),
                iron=safe_float(item.get("Fe")),
                zinc=safe_float(item.get("Zn")),
                selenium=safe_float(item.get("Se")),
                copper=safe_float(item.get("Cu")),
                manganese=safe_float(item.get("Mn")),
                remark=item.get("remark", ""),
            )
        except Exception as e:
            logger.error(f"解析中国食物数据失败: {e}")
            return None
    
    def _load_usda_foundation(self) -> None:
        """加载 USDA Foundation Foods"""
        # 查找 Foundation Foods CSV 目录
        fdc_dirs = list(self.data_dir.glob("FoodData_Central_foundation*"))
        if not fdc_dirs:
            logger.warning("USDA Foundation Foods 目录不存在")
            return
        
        fdc_dir = fdc_dirs[0]
        food_file = fdc_dir / "food.csv"
        nutrient_file = fdc_dir / "nutrient.csv"
        food_nutrient_file = fdc_dir / "food_nutrient.csv"
        
        if not all(f.exists() for f in [food_file, nutrient_file, food_nutrient_file]):
            logger.warning(f"USDA 数据文件不完整: {fdc_dir}")
            return
        
        # 1. 读取营养素定义
        nutrient_ids = {}  # name -> id
        target_nutrients = {
            "Energy": "energy_kcal",
            "Protein": "protein", 
            "Carbohydrate, by difference": "carbohydrate",
            "Total lipid (fat)": "fat",
            "Fiber, total dietary": "dietary_fiber",
            "Cholesterol": "cholesterol",
            "Calcium, Ca": "calcium",
            "Iron, Fe": "iron",
            "Sodium, Na": "sodium",
            "Vitamin C, total ascorbic acid": "vitamin_c",
            "Vitamin A, RAE": "vitamin_a",
        }
        
        with open(nutrient_file, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                name = row.get("name", "")
                if name in target_nutrients:
                    nutrient_ids[row.get("id")] = target_nutrients[name]
        
        # 2. 读取食物营养数据
        food_nutrients: Dict[str, Dict[str, float]] = {}
        with open(food_nutrient_file, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                nutrient_id = row.get("nutrient_id")
                if nutrient_id in nutrient_ids:
                    fdc_id = row.get("fdc_id")
                    if fdc_id not in food_nutrients:
                        food_nutrients[fdc_id] = {}
                    try:
                        food_nutrients[fdc_id][nutrient_ids[nutrient_id]] = float(row.get("amount", 0))
                    except:
                        pass
        
        # 3. 读取食物信息并创建 NutritionData
        count = 0
        with open(food_file, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                fdc_id = row.get("fdc_id")
                nutrients = food_nutrients.get(fdc_id)
                if not nutrients:
                    continue
                
                description = row.get("description", "").lower()
                key = self._normalize_key(description)
                
                # 如果中国数据库已有，跳过（中国数据优先）
                if key in self._db:
                    continue
                
                nutrition = NutritionData(
                    food_code=fdc_id,
                    food_name=description,
                    food_name_en=description,
                    source=DataSource.USDA_FOUNDATION,
                    energy_kcal=nutrients.get("energy_kcal", 0),
                    protein=nutrients.get("protein", 0),
                    carbohydrate=nutrients.get("carbohydrate", 0),
                    fat=nutrients.get("fat", 0),
                    dietary_fiber=nutrients.get("dietary_fiber", 0),
                    cholesterol=nutrients.get("cholesterol", 0),
                    calcium=nutrients.get("calcium", 0),
                    iron=nutrients.get("iron", 0),
                    sodium=nutrients.get("sodium", 0),
                    vitamin_c=nutrients.get("vitamin_c", 0),
                    vitamin_a=nutrients.get("vitamin_a", 0),
                )
                
                self._db[key] = nutrition
                count += 1
        
        self._source_stats[DataSource.USDA_FOUNDATION] = count
        logger.info(f"加载 USDA Foundation Foods: {count} 种食物")
    
    def _load_foodstruct(self) -> None:
        """加载 FoodStruct 数据"""
        csv_file = self.data_dir / "foodstruct_nutritional_facts 2.csv"
        if not csv_file.exists():
            logger.warning(f"FoodStruct 数据文件不存在: {csv_file}")
            return
        
        count = 0
        with open(csv_file, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                name = row.get("Food Name", "").lower()
                key = self._normalize_key(name)
                
                # 如果已有数据，跳过
                if key in self._db:
                    continue
                
                try:
                    nutrition = NutritionData(
                        food_name=name,
                        food_name_en=name,
                        category=row.get("Category Name", ""),
                        source=DataSource.FOODSTRUCT,
                        energy_kcal=float(row.get("Calories", 0) or 0),
                        protein=float(row.get("Protein", 0) or 0),
                        carbohydrate=float(row.get("Carbs", 0) or 0),
                        fat=float(row.get("Fats", 0) or 0),
                        dietary_fiber=float(row.get("Fiber", 0) or 0),
                        cholesterol=float(row.get("Cholesterol", 0) or 0),
                        calcium=float(row.get("Calcium", 0) or 0) * 1000,  # g -> mg
                        sodium=float(row.get("Sodium", 0) or 0) * 1000,
                        potassium=float(row.get("Potassium", 0) or 0) * 1000,
                        iron=float(row.get("Iron", 0) or 0) * 1000,
                    )
                    self._db[key] = nutrition
                    count += 1
                except Exception as e:
                    logger.debug(f"解析 FoodStruct 行失败: {e}")
        
        self._source_stats[DataSource.FOODSTRUCT] = count
        logger.info(f"加载 FoodStruct: {count} 种食物")
    
    def _load_builtin_aliases(self) -> None:
        """加载内置别名映射"""
        # 英文 -> 中文标准名映射
        aliases = {
            # 主食
            "rice": "籼米",
            "white rice": "籼米",
            "steamed rice": "籼米",
            "fried rice": "籼米",
            "noodles": "挂面（标准粉）",
            "noodle": "挂面（标准粉）",
            "bread": "面包",
            "wheat": "小麦粉（标准粉）",
            
            # 肉类
            "pork": "猪肉（代表值，fat30g)",
            "pork belly": "猪肉（肥瘦）",
            "pork loin": "猪肉（瘦）",
            "beef": "牛肉（代表值）",
            "chicken": "鸡（代表值）",
            "chicken breast": "鸡胸",
            "chicken meat": "鸡（代表值）",
            "duck": "鸭（代表值）",
            "lamb": "羊肉（代表值）",
            "meat": "猪肉（代表值，fat30g)",
            
            # 蔬菜
            "carrot": "胡萝卜",
            "potato": "马铃薯",
            "tomato": "番茄",
            "cabbage": "卷心菜",
            "chinese cabbage": "大白菜（小白口）",
            "bok choy": "大白菜（小白口）",
            "spinach": "菠菜",
            "broccoli": "西兰花",
            "green vegetable": "大白菜（小白口）",
            "green vegetables": "大白菜（小白口）",
            "leafy greens": "菠菜",
            "onion": "洋葱",
            "green onion": "大葱",
            "scallion": "大葱",
            "garlic": "大蒜",
            "ginger": "姜",
            "cucumber": "黄瓜",
            "eggplant": "茄子",
            "pepper": "辣椒（青、尖）",
            "mushroom": "香菇（鲜）",
            
            # 蛋奶
            "egg": "鸡蛋",
            "eggs": "鸡蛋",
            "milk": "牛奶",
            "whole milk": "全脂牛奶",
            
            # 豆制品
            "tofu": "豆腐（北）",
            "soy": "大豆（黄豆）",
            "soybean": "大豆（黄豆）",
            
            # 水果
            "apple": "苹果（代表值）",
            "banana": "香蕉",
            "orange": "橙（代表值）",
            "grape": "葡萄（代表值）",
            
            # 海鲜
            "fish": "草鱼",
            "shrimp": "虾（代表值）",
            "crab": "蟹（代表值）",
            
            # 油脂
            "oil": "花生油",
            "vegetable oil": "花生油",
            "peanut oil": "花生油",
            
            # 零食/加工食品
            "sugar": "白砂糖",
            "plum": "话梅",
            "dried plum": "话梅",
            "preserved plum": "话梅",
            "candy": "糖果",
            "chocolate": "巧克力",
            "cookie": "饼干",
            "cookies": "饼干",
            "biscuit": "饼干",
            "biscuits": "饼干",
            "chips": "Potato chips",  # 使用 FoodStruct 高热量数据
            "potato chips": "Potato chips",
            "corn chips": "Corn chips",  # 玉米片 538 kcal/100g
            "fried corn chips": "Corn chips",
            "tortilla chips": "Tortilla chips",
            "薯片": "Potato chips",
            "crackers": "苏打饼干",
            "nuts": "坚果",
            "peanut": "花生（炒）",
            "peanuts": "花生（炒）",
            "fried peanuts": "花生（炒）",
            "roasted peanuts": "花生（炒）",
            "almond": "杏仁",
            "almonds": "杏仁",
            "walnut": "核桃",
            "walnuts": "核桃",
            "cashew": "腰果",
            "cashew nuts": "腰果",
            "raisin": "葡萄干",
            "raisins": "葡萄干",
            "dried fruit": "葡萄干",
            "jerky": "牛肉干",
            "beef jerky": "牛肉干",
            "pork jerky": "猪肉脯",
            "seaweed": "紫菜（干）",
            "nori": "紫菜（干）",
            "snack mix": "Corn chips",  # 零食混合包按玉米片热量计算 538 kcal/100g
            "mixed nuts": "坚果",
            
            # 饮料
            "juice": "橙汁",
            "orange juice": "橙汁",
            "apple juice": "苹果汁",
            "cola": "可乐",
            "soda": "碳酸饮料",
            "tea": "茶",
            "coffee": "咖啡",
            "yogurt": "酸奶",
            
            # 调味料
            "salt": "食盐",
            "soy sauce": "酱油",
            "vinegar": "醋",
            "starch": "淀粉",
            "flour": "小麦粉（标准粉）",
            "corn starch": "玉米淀粉",
        }
        
        for alias, standard_name in aliases.items():
            key = self._normalize_key(alias)
            self._aliases[key] = self._normalize_key(standard_name)
        
        logger.info(f"加载别名映射: {len(aliases)} 条")
    
    def _build_standard_names(self) -> None:
        """构建标准食物名列表"""
        for key, nutrition in self._db.items():
            if nutrition.food_name_cn:
                self._standard_names_cn.append(nutrition.food_name_cn)
            if nutrition.food_name_en:
                self._standard_names_en.append(nutrition.food_name_en)
        
        logger.info(f"标准名列表: {len(self._standard_names_cn)} 中文, {len(self._standard_names_en)} 英文")
    
    def _normalize_key(self, name: str) -> str:
        """标准化食物名作为查询键"""
        if not name:
            return ""
        # 转小写，去除空格和特殊字符
        key = name.strip().lower()
        # 移除括号内容（如"猪肉（代表值）" -> "猪肉"）用于模糊匹配
        return key
    
    def lookup(self, name: str, category: str = None) -> Tuple[NutritionData, bool]:
        """
        查询食物营养数据
        
        参数:
            name: 食物名称
            category: 食物分类（snack/meal/beverage/dessert/fruit/packaged），用于兜底
        
        返回: (NutritionData, is_exact_match)
        
        查询优先级:
            1. 精确匹配
            2. 别名匹配
            3. 模糊匹配（包含关系）
            4. 分类默认值（如果提供了 category）
            5. 通用默认值
        """
        if not name:
            return self._get_category_default(category) if category else self.FALLBACK_NUTRITION, False
        
        key = self._normalize_key(name)
        
        # 1. 精确匹配
        if key in self._db:
            return self._db[key], True
        
        # 2. 别名匹配
        if key in self._aliases:
            alias_key = self._aliases[key]
            if alias_key in self._db:
                return self._db[alias_key], True
        
        # 3. 模糊匹配（包含关系）
        for db_key, nutrition in self._db.items():
            if key in db_key or db_key in key:
                return nutrition, False
        
        # 4. 分类默认值
        if category:
            logger.info(f"未找到食物 '{name}'，使用分类默认值: {category}")
            return self._get_category_default(category), False
        
        # 5. 返回通用默认值
        logger.debug(f"未找到食物: {name}")
        return self.FALLBACK_NUTRITION, False
    
    def _get_category_default(self, category: str) -> NutritionData:
        """根据分类获取默认营养值"""
        cat_key = category.lower() if category else "meal"
        defaults = CATEGORY_NUTRITION_DEFAULTS.get(cat_key, CATEGORY_NUTRITION_DEFAULTS["meal"])
        
        return NutritionData(
            food_name=f"[{cat_key}类默认值]",
            food_name_cn=defaults.get("description", f"{cat_key}类食物"),
            category=cat_key,
            source=DataSource.CATEGORY_DEFAULT,
            energy_kcal=defaults["calories"],
            protein=defaults["protein"],
            carbohydrate=defaults["carbs"],
            fat=defaults["fat"],
        )
    
    def get_standard_food_names(self, language: str = "cn") -> List[str]:
        """获取标准食物名列表（供 VLM 参考）"""
        if language == "cn":
            return self._standard_names_cn[:500]  # 返回前500个常用
        return self._standard_names_en[:500]
    
    def get_stats(self) -> Dict[str, Any]:
        """获取数据库统计信息"""
        return {
            "total_foods": len(self._db),
            "sources": {s.value: c for s, c in self._source_stats.items()},
            "aliases": len(self._aliases),
            "standard_names_cn": len(self._standard_names_cn),
            "standard_names_en": len(self._standard_names_en),
        }


# 全局单例
_nutrition_db: Optional[NutritionDatabase] = None


def get_nutrition_db() -> NutritionDatabase:
    """获取营养数据库单例"""
    global _nutrition_db
    if _nutrition_db is None:
        _nutrition_db = NutritionDatabase()
        _nutrition_db.load_all()
    return _nutrition_db


def lookup_nutrition(name: str) -> Dict[str, float]:
    """
    查询食物营养（简化接口，兼容旧代码）
    返回: {"calories": ..., "protein": ..., "carbs": ..., "fat": ...}
    """
    db = get_nutrition_db()
    nutrition, _ = db.lookup(name)
    return nutrition.to_simple_dict()


def lookup_nutrition_full(name: str) -> Dict[str, Any]:
    """查询食物完整营养数据"""
    db = get_nutrition_db()
    nutrition, _ = db.lookup(name)
    return nutrition.to_full_dict()
