# Rokid 营养助手 - 营养数据来源说明

## 概述

本系统整合了多个权威营养数据源，通过智能融合策略提供准确、全面的食物营养信息。

## 数据来源

### 1. 中国食物成分表（第6版）
- **来源**: 中国疾病预防控制中心营养与健康所
- **数据集**: [china-food-composition-data](https://github.com/Sanotsu/china-food-composition-data)
- **覆盖**: 1838 种中国常见食物
- **字段**: 32 个营养字段
  - 基础: 能量(kcal/kJ)、蛋白质、脂肪、碳水化合物
  - 膳食纤维、胆固醇、水分、灰分
  - 维生素: A、胡萝卜素、视黄醇、B1、B2、B3、C、E
  - 矿物质: 钙、磷、钾、钠、镁、铁、锌、硒、铜、锰
- **优先级**: 最高（中文食物名匹配时优先使用）
- **特点**: 
  - 针对中国饮食习惯
  - 包含中式菜品和食材
  - 数据权威可靠

### 2. USDA Foundation Foods
- **来源**: 美国农业部 FoodData Central
- **网址**: https://fdc.nal.usda.gov/download-datasets
- **版本**: 2025-04-24
- **覆盖**: 基础食材的详细营养数据
- **字段**: 能量、蛋白质、碳水、脂肪、膳食纤维、胆固醇、矿物质、维生素等
- **优先级**: 次高（英文食物名匹配时使用）
- **特点**:
  - 实验室精确测量数据
  - 覆盖原材料和基础食材
  - 国际通用标准

### 3. FoodStruct 数据集
- **来源**: FoodStruct.com
- **文件**: `foodstruct_nutritional_facts 2.csv`
- **覆盖**: 国际通用食物营养数据
- **字段**: 热量、蛋白质、碳水、脂肪、纤维、矿物质等
- **优先级**: 第三
- **特点**:
  - 覆盖面广
  - 包含加工食品

### 4. 内置数据库
- **来源**: 手动整理的常见中文食物
- **覆盖**: 16 种常见中国食物
- **优先级**: 第四
- **特点**: 快速回退，保证基础查询

## 数据融合策略

### 查询优先级
```
1. 中国食物成分表 (CHINA_FOOD_COMPOSITION)
   ↓ 未找到
2. USDA Foundation Foods (USDA_FOUNDATION)
   ↓ 未找到
3. FoodStruct (FOODSTRUCT)
   ↓ 未找到
4. 内置数据库 (BUILTIN)
   ↓ 未找到
5. 默认回退值 (FALLBACK)
   - calories: 100 kcal/100g
   - protein: 5g/100g
   - carbs: 15g/100g
   - fat: 3g/100g
```

### 别名映射
系统内置英文到中文的食材映射，例如：
- `rice` → `籼米`
- `pork` → `猪肉（代表值）`
- `chicken` → `鸡（代表值）`
- `carrot` → `胡萝卜`
- `egg` → `鸡蛋`

### 同名食物处理
当多个数据源包含同名食物时：
1. **中文名匹配**: 优先使用中国食物成分表
2. **英文名匹配**: 优先使用 USDA Foundation Foods
3. **模糊匹配**: 支持包含关系匹配（如 "chicken breast" 匹配 "鸡胸"）

## VLM 食材识别规范

为确保 VLM 返回的食材名能准确匹配数据库，系统要求 VLM 使用标准化的食材名称：

### 英文标准名
```
rice, pork, beef, chicken, carrot, potato, tomato, cabbage, 
egg, tofu, onion, garlic, ginger, pepper, mushroom, fish, shrimp
```

### 中文标准名
```
米饭, 猪肉, 牛肉, 鸡肉, 胡萝卜, 土豆, 番茄, 白菜,
鸡蛋, 豆腐, 洋葱, 大蒜, 生姜, 辣椒, 香菇, 鱼, 虾
```

### 烹饪方式标准
```
stir-fried（炒）, steamed（蒸）, boiled（煮）, 
braised（红烧）, deep-fried（炸）, raw（生）
```

## 数据统计

| 数据源 | 食物数量 | 主要特点 |
|--------|----------|----------|
| 中国食物成分表 | ~1838 | 中国食材，32个营养字段 |
| USDA Foundation | ~500+ | 基础食材，实验室数据 |
| FoodStruct | ~3000+ | 国际食物，广覆盖 |
| 内置数据库 | 16 | 常见中文食物 |
| **总计** | **~5000+** | 多源融合 |

## 更新日志

### 2025-11-26
- 新增中国食物成分表（第6版）数据集
- 新增 USDA Foundation Foods CSV 数据集
- 实现多数据源智能融合策略
- 优化 VLM prompt，返回标准化食材名称
- 创建 `nutrition_db.py` 统一数据管理模块

### 2025-11-25
- 初始版本，支持 FoodStruct 数据集
- 实现基础营养查询功能

## 文件结构

```
backend/data/
├── china-food-composition-data/     # 中国食物成分表
│   └── json_data/                   # JSON 格式数据文件
├── FoodData_Central_foundation.../  # USDA Foundation Foods
│   ├── food.csv
│   ├── nutrient.csv
│   └── food_nutrient.csv
├── foodstruct_nutritional_facts 2.csv  # FoodStruct 数据
├── nutrition_cn.csv                 # 中文食物（旧）
├── nutrition_usda.csv               # USDA 食物（旧）
└── NUTRITION_DATA_SOURCES.md        # 本文档
```

## API 接口

### 查询营养数据
```
GET /api/v1/nutrition/lookup?name=猪肉
```

### 获取数据库统计
```
GET /api/v1/nutrition/stats
```

## 许可证

- 中国食物成分表数据: 来源于公开整理，仅供学术研究
- USDA Foundation Foods: Public Domain (CC0)
- FoodStruct: 参考数据，商用请确认许可
