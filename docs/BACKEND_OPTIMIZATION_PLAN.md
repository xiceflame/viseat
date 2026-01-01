# VisEat 后端识别与分析优化方案

> 基于竞品研究和代码分析的技术优化路线

---

## 📋 当前实现分析

### 现有架构

```
用户图片 → Qwen-VL识别 → JSON解析 → food_mapper映射 → nutrition_db查询 → 营养计算
```

### 现有优势
- ✅ 基线差分追踪算法（业界首创）
- ✅ 动态基线自适应更新
- ✅ 6300+ 食物营养数据库
- ✅ 多层匹配策略（精确→模糊→默认）

### 识别到的问题

| 问题 | 影响 | 优先级 |
|------|------|--------|
| VLM 份量估算不准确 | 热量误差 ±30% | 🔴 高 |
| 同义词覆盖不足 | 匹配失败率 ~15% | 🔴 高 |
| 无用户反馈学习 | 无法持续优化 | 🟡 中 |
| 无烹饪方法修正 | 热量偏差 ~20% | 🟡 中 |
| 中国菜品拆解弱 | 复合菜品不准 | 🟡 中 |

---

## 🔧 优化方案一：VLM Prompt 增强（提升份量估算）

### 问题分析
当前 Prompt 只让 VLM 估算"剩余百分比"，缺乏参照物校准。

### 优化策略：引入参照物校准

```python
# 优化后的 Prompt（增加参照物估算）
system_prompt = """
你是食品识别与份量估算专家。

**份量估算技巧**（提高准确性）：
1. 参照餐具大小：
   - 标准饭碗：直径12cm，满碗米饭约200g
   - 标准盘子：直径20-25cm
   - 汤勺：一勺约15ml
   - 筷子长度：约25cm（可作为尺寸参照）

2. 常见食物重量参照：
   - 一个鸡蛋：约50g
   - 一块麻将大小的肉：约30g
   - 一个拳头大小的米饭：约150g
   - 一根香肠：约50g
   - 一片培根：约15g

3. 视觉体积估算：
   - 先估计食物体积（立方厘米）
   - 根据食物密度换算重量
   - 蔬菜密度低（~0.3g/cm³）
   - 肉类密度高（~1.0g/cm³）

请在 JSON 中增加估算依据：
{
  "ingredients": [
    {
      "name_en": "beef",
      "name_cn": "牛肉",
      "weight_g": 150,
      "confidence": 0.8,
      "estimation_basis": "约5块麻将大小，每块30g"
    }
  ]
}
"""
```

### 实现代码

```python
# 在 main.py 的 vision_analyze 中添加

PORTION_REFERENCE_PROMPT = """
**份量估算参照表**：
| 参照物 | 对应重量 |
|--------|----------|
| 标准饭碗满碗米饭 | 200g |
| 拳头大小米饭 | 150g |
| 麻将大小肉块 | 30g |
| 一个鸡蛋 | 50g |
| 手掌大小肉排 | 100g |
| 一根筷子长度 | 25cm（尺寸参照）|

请基于图片中餐具和食物的相对大小进行估算。
"""
```

---

## 🔧 优化方案二：同义词扩展与语义匹配

### 问题分析
当前 `FOOD_ALIASES` 虽有 400+ 条，但 VLM 返回的名称变化多端。

### 优化策略：三层匹配

```
第一层：精确匹配（现有）
第二层：同义词扩展（新增）
第三层：向量语义匹配（新增）
```

### 新增同义词扩展表

```python
# 新增到 food_mapper.py

# 中国菜品同义词扩展（解决命名不一致）
DISH_SYNONYMS = {
    # 米饭类
    "米饭": ["白米饭", "蒸米饭", "大米饭", "白饭", "steamed rice", "white rice", "rice"],
    "炒饭": ["蛋炒饭", "扬州炒饭", "什锦炒饭", "fried rice", "egg fried rice"],
    
    # 面食类
    "面条": ["挂面", "拉面", "手工面", "noodles", "拌面", "汤面"],
    "馒头": ["白馒头", "蒸馒头", "steamed bun", "mantou"],
    
    # 肉类表述
    "牛肉": ["beef", "牛腩", "牛腱", "牛筋", "牛里脊", "牛排"],
    "猪肉": ["pork", "五花肉", "里脊肉", "瘦肉", "肉片", "肉丝"],
    "鸡肉": ["chicken", "鸡胸", "鸡腿", "鸡翅", "鸡块"],
    
    # 蔬菜类
    "青菜": ["绿叶菜", "蔬菜", "菜", "greens", "vegetables", "green vegetables"],
    "土豆": ["马铃薯", "洋芋", "potato", "potatoes"],
    
    # 豆腐类
    "豆腐": ["嫩豆腐", "老豆腐", "tofu", "bean curd"],
}

def expand_synonyms(name: str) -> List[str]:
    """扩展食材同义词"""
    name_lower = name.lower()
    results = [name, name_lower]
    
    for canonical, synonyms in DISH_SYNONYMS.items():
        if name_lower in [s.lower() for s in synonyms] or name_lower == canonical.lower():
            results.extend([canonical] + synonyms)
            break
    
    return list(set(results))
```

### 向量语义匹配（进阶）

```python
# 使用简化的编辑距离匹配（不依赖外部模型）
from difflib import SequenceMatcher

def fuzzy_match_food(query: str, candidates: List[str], threshold: float = 0.6) -> Optional[str]:
    """模糊匹配食材名"""
    best_match = None
    best_score = 0
    
    query_lower = query.lower()
    for candidate in candidates:
        score = SequenceMatcher(None, query_lower, candidate.lower()).ratio()
        if score > best_score and score >= threshold:
            best_score = score
            best_match = candidate
    
    return best_match
```

---

## 🔧 优化方案三：烹饪方法热量修正

### 问题分析
同样的食材，不同烹饪方法热量差异巨大：
- 100g 鸡胸肉（蒸）：133 kcal
- 100g 鸡胸肉（油炸）：~220 kcal

### 优化策略：烹饪方法修正系数

```python
# 新增到 food_mapper.py

COOKING_METHOD_MULTIPLIERS = {
    # 低热量烹饪
    "steam": 1.0,      # 蒸
    "boil": 1.0,       # 煮
    "raw": 1.0,        # 生食
    "blanch": 1.0,     # 焯水
    
    # 中等热量
    "stir_fry": 1.15,  # 炒（少油）
    "braise": 1.2,     # 红烧
    "stew": 1.1,       # 炖
    "grill": 1.05,     # 烤（无额外油）
    
    # 高热量烹饪
    "deep_fry": 1.5,   # 油炸
    "pan_fry": 1.3,    # 煎
    "roast": 1.2,      # 烘烤（带油）
}

def adjust_for_cooking_method(nutrition: dict, cooking_method: str) -> dict:
    """根据烹饪方法调整热量"""
    multiplier = COOKING_METHOD_MULTIPLIERS.get(cooking_method, 1.0)
    
    # 主要调整热量和脂肪
    return {
        "calories": round(nutrition["calories"] * multiplier, 1),
        "protein": nutrition["protein"],  # 蛋白质不变
        "carbs": nutrition["carbs"],      # 碳水不变
        "fat": round(nutrition["fat"] * multiplier, 1),  # 脂肪随热量变化
        "confidence": nutrition.get("confidence", 0.8),
    }
```

---

## 🔧 优化方案四：用户反馈学习机制

### 问题分析
YAZIO 的"越用越准"依赖用户反馈，我们目前没有这个机制。

### 优化策略：构建反馈闭环

```python
# 新增 feedback_learning.py

from dataclasses import dataclass
from typing import Dict, List
import json
from pathlib import Path

@dataclass
class FoodCorrection:
    """用户修正记录"""
    original_name: str      # VLM 原始识别
    corrected_name: str     # 用户修正后
    original_weight: float  # 原始重量估算
    corrected_weight: float # 用户修正重量
    timestamp: str
    user_id: str

class FeedbackLearner:
    """用户反馈学习器"""
    
    def __init__(self, data_path: Path):
        self.data_path = data_path
        self.corrections: List[FoodCorrection] = []
        self._load()
    
    def record_correction(self, correction: FoodCorrection):
        """记录用户修正"""
        self.corrections.append(correction)
        self._save()
        
        # 统计同一修正出现次数
        key = f"{correction.original_name}→{correction.corrected_name}"
        count = sum(1 for c in self.corrections 
                   if f"{c.original_name}→{c.corrected_name}" == key)
        
        # 出现3次以上，自动加入映射表
        if count >= 3:
            self._add_to_aliases(correction.original_name, correction.corrected_name)
    
    def get_weight_correction_factor(self, food_name: str) -> float:
        """获取重量修正系数（基于历史数据）"""
        relevant = [c for c in self.corrections 
                   if c.original_name.lower() == food_name.lower()]
        
        if len(relevant) < 3:
            return 1.0  # 数据不足，不修正
        
        # 计算平均修正比例
        ratios = [c.corrected_weight / c.original_weight 
                 for c in relevant if c.original_weight > 0]
        return sum(ratios) / len(ratios) if ratios else 1.0
```

### API 端点

```python
# 新增到 main.py

@app.post("/api/v1/feedback/correction")
async def submit_food_correction(
    original_name: str,
    corrected_name: str,
    original_weight: float,
    corrected_weight: float,
    user_id: str = "anonymous"
):
    """提交食物识别修正"""
    correction = FoodCorrection(
        original_name=original_name,
        corrected_name=corrected_name,
        original_weight=original_weight,
        corrected_weight=corrected_weight,
        timestamp=datetime.now().isoformat(),
        user_id=user_id
    )
    learner.record_correction(correction)
    return {"status": "recorded", "message": "感谢您的反馈，这将帮助我们提高准确性"}
```

---

## 🔧 优化方案五：中国复合菜品拆解

### 问题分析
复合菜品（如"鱼香肉丝"）难以直接匹配数据库。

### 优化策略：菜品配方库

```python
# 新增 dish_recipes.py

# 常见中国菜品配方（食材组成）
CHINESE_DISH_RECIPES = {
    "鱼香肉丝": {
        "ingredients": [
            {"name": "猪肉", "weight_ratio": 0.5},
            {"name": "木耳", "weight_ratio": 0.15},
            {"name": "笋", "weight_ratio": 0.15},
            {"name": "胡萝卜", "weight_ratio": 0.1},
            {"name": "油", "weight_ratio": 0.1},
        ],
        "cooking_method": "stir_fry",
        "typical_portion_g": 200,
    },
    "宫保鸡丁": {
        "ingredients": [
            {"name": "鸡肉", "weight_ratio": 0.5},
            {"name": "花生", "weight_ratio": 0.15},
            {"name": "青椒", "weight_ratio": 0.15},
            {"name": "油", "weight_ratio": 0.1},
            {"name": "辣椒", "weight_ratio": 0.1},
        ],
        "cooking_method": "stir_fry",
        "typical_portion_g": 200,
    },
    "红烧肉": {
        "ingredients": [
            {"name": "五花肉", "weight_ratio": 0.85},
            {"name": "糖", "weight_ratio": 0.05},
            {"name": "酱油", "weight_ratio": 0.05},
            {"name": "油", "weight_ratio": 0.05},
        ],
        "cooking_method": "braise",
        "typical_portion_g": 150,
    },
    "番茄炒蛋": {
        "ingredients": [
            {"name": "鸡蛋", "weight_ratio": 0.5},
            {"name": "番茄", "weight_ratio": 0.4},
            {"name": "油", "weight_ratio": 0.1},
        ],
        "cooking_method": "stir_fry",
        "typical_portion_g": 200,
    },
    "麻婆豆腐": {
        "ingredients": [
            {"name": "豆腐", "weight_ratio": 0.7},
            {"name": "猪肉", "weight_ratio": 0.15},
            {"name": "油", "weight_ratio": 0.1},
            {"name": "辣椒", "weight_ratio": 0.05},
        ],
        "cooking_method": "braise",
        "typical_portion_g": 200,
    },
    # ... 可扩展更多菜品
}

def decompose_dish(dish_name: str, total_weight: float) -> List[Dict]:
    """将复合菜品拆解为食材列表"""
    recipe = CHINESE_DISH_RECIPES.get(dish_name)
    if not recipe:
        return None
    
    return [
        {
            "name": ing["name"],
            "weight_g": total_weight * ing["weight_ratio"],
        }
        for ing in recipe["ingredients"]
    ]
```

---

## 🔧 优化方案六：置信度阈值与重试机制

### 问题分析
当 VLM 识别置信度低时，应该提示用户或重试。

### 优化策略

```python
# 在 vision_analyze 中添加

CONFIDENCE_THRESHOLD = 0.6  # 置信度阈值

async def vision_analyze_with_retry(req: VisionAnalyzeRequest, max_retries: int = 2):
    """带重试的视觉分析"""
    
    for attempt in range(max_retries + 1):
        result = await vision_analyze(req)
        
        # 检查整体置信度
        foods = result.get("snapshot", {}).get("foods", [])
        avg_confidence = sum(f.get("confidence", 0.5) for f in foods) / max(len(foods), 1)
        
        if avg_confidence >= CONFIDENCE_THRESHOLD:
            return result
        
        if attempt < max_retries:
            # 重试时使用更详细的 prompt
            logger.info(f"识别置信度低 ({avg_confidence:.2f})，尝试重试...")
            req.question = "请更仔细地观察图片，尽量识别所有食材并给出准确的重量估算"
    
    # 低置信度警告
    result["low_confidence_warning"] = True
    result["suggestion"] = f"识别置信度较低 ({avg_confidence:.2f})，建议手动确认食物和份量"
    
    return result
```

---

## 📊 优化效果预估

| 优化项 | 预期效果 | 实现难度 | 优先级 |
|--------|----------|----------|--------|
| Prompt 份量参照 | 重量误差 -15% | ⭐ 低 | P0 |
| 同义词扩展 | 匹配率 +20% | ⭐ 低 | P0 |
| 烹饪方法修正 | 热量误差 -10% | ⭐⭐ 中 | P1 |
| 用户反馈学习 | 长期持续优化 | ⭐⭐ 中 | P1 |
| 菜品配方库 | 复合菜品准确率 +30% | ⭐⭐ 中 | P1 |
| 置信度重试 | 极端情况准确率 +15% | ⭐ 低 | P2 |

---

## 🚀 实施计划

### 第一周：Prompt 优化 + 同义词扩展

1. 更新 `vision_analyze` 的 system_prompt，加入份量参照表
2. 扩展 `FOOD_ALIASES` 和 `DISH_SYNONYMS`
3. 实现 `expand_synonyms()` 函数
4. 单元测试验证

### 第二周：烹饪方法修正 + 菜品配方库

1. 实现 `COOKING_METHOD_MULTIPLIERS`
2. 创建 `CHINESE_DISH_RECIPES` 初始版本（50+ 常见菜品）
3. 整合到 `calculate_nutrition()` 流程

### 第三周：用户反馈机制

1. 实现 `FeedbackLearner` 类
2. 添加反馈 API 端点
3. 与前端/AR 眼镜端对接

### 第四周：测试与调优

1. 使用真实场景测试
2. 收集误差数据
3. 调整阈值和参数

---

*文档版本: v1.0*  
*更新日期: 2025年11月30日*
