# 营养计算准确性提升方案

## 当前问题

1. **VLM 识别不稳定** - 同一图片多次识别结果不同
2. **食材名映射困难** - 手动维护别名不可扩展
3. **重量估算波动大** - 图片难以准确判断份量

## 解决方案

### 方案1：混合策略（推荐，短期可实现）

**核心思路**：VLM 识别食材 + 数据库查营养 + VLM 兜底

```
1. VLM 识别食材名和重量
2. 用食材名查本地营养数据库
3. 如果查不到，让 VLM 直接估算该食材的营养
4. 汇总计算总营养
```

**优点**：
- 有数据库的食材用准确数据
- 没有的食材用 VLM 知识兜底
- 不需要维护完整的别名映射

### 方案2：语义匹配（中期）

**核心思路**：用 Embedding 做语义相似度匹配

```
1. 预计算所有食材名的 Embedding 向量
2. VLM 返回的食材名也计算 Embedding
3. 用余弦相似度找最接近的食材
```

**优点**：
- "海鲜拼盘" 可以匹配到 "虾"
- "蔬菜" 可以匹配到 "白菜"
- 不需要手动维护别名

### 方案3：用户反馈学习（长期）

**核心思路**：记录用户修改，逐步优化

```
1. 用户修改识别结果时保存到数据库
2. 下次识别类似食物时参考历史修改
3. 积累足够数据后可以微调模型
```

## 实施建议

### 第一阶段：混合策略（1-2天）

修改 `calculate_nutrition` 函数：

```python
def calculate_nutrition_v2(foods: List[Dict], vlm_client) -> Dict:
    total = {"calories": 0, "protein": 0, "carbs": 0, "fat": 0}
    
    for food in foods:
        name = food.get("name")
        weight = food.get("weight_g", 0)
        
        # 1. 先查数据库
        nutrition, exact = nutrition_db.lookup(name)
        
        if exact or nutrition.source != "fallback":
            # 数据库有数据，直接用
            total["calories"] += nutrition.energy_kcal * weight / 100
            total["protein"] += nutrition.protein * weight / 100
            # ...
        else:
            # 2. 数据库没有，让 VLM 估算
            vlm_nutrition = ask_vlm_for_nutrition(vlm_client, name, weight)
            total["calories"] += vlm_nutrition["calories"]
            total["protein"] += vlm_nutrition["protein"]
            # ...
    
    return total
```

### 第二阶段：语义匹配（1周）

1. 使用阿里云的 text-embedding-v3 模型
2. 预计算 6000+ 食材的 Embedding
3. 查询时用向量相似度匹配

### 第三阶段：用户反馈（持续）

1. 记录用户的编辑历史
2. 分析常见的识别错误
3. 定期更新别名映射或微调模型
