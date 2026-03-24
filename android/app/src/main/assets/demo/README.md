# 演示模式资源文件

## 目录结构

```
demo/
├── README.md                    # 本文件
├── coke.jpg                     # 可乐图片
├── coke_response.json           # 可乐分析结果（真实API数据）
├── chips.jpg                    # 薯片图片
├── chips_response.json          # 薯片分析结果（真实API数据）
├── meal_start.jpg               # 用餐开始图片 ✅
├── meal_start_response.json     # 用餐开始分析结果 ✅
├── meal_middle.jpg              # 用餐中图片 ✅
├── meal_middle_response.json    # 用餐中分析结果 ✅
├── meal_end.jpg                 # 用餐结束图片 ✅
└── meal_end_response.json       # 用餐结束分析结果 ✅
```

## 演示模式

### 单图识别模式（SINGLE_IMAGE）

**不需要网络**，直接使用预设的 JSON 响应数据：
- `coke.jpg` + `coke_response.json` - 可乐（111kcal）
- `chips.jpg` + `chips_response.json` - 薯片（536kcal）

数据来源：2025-12-01 18:40 真实 API 分析结果

### 用餐监测模式（MEAL_MONITORING）- 当前默认

**不需要网络**，使用预设的后端真实响应数据：
- `meal_start.jpg` + `meal_start_response.json` - 用餐开始（mode=start，建立基线）
- `meal_middle.jpg` + `meal_middle_response.json` - 用餐中（mode=update，计算已消耗）
- `meal_end.jpg` + `meal_end_response.json` - 用餐结束（mode=end，计算总消耗）

数据来源：2025-12-01 真实 API 分析结果

### 用餐数据汇总

| 阶段 | 剩余热量 | 已消耗热量 | 消耗比例 |
|------|----------|------------|----------|
| 开始 | 1092 kcal | 0 kcal | 0% |
| 中途 | 756 kcal | 546 kcal | 50% |
| 结束 | 62 kcal | 1030 kcal | 94% |

### 识别的食物
- 馄饨汤 (150g → 15g)
- 炸物/天妇罗 (80g → 0g) ✅ 吃完
- 煎饼 (100g → 10g)
- 肉片汤 (100g → 0g) ✅ 吃完

流程：
1. **start 阶段**：拍照 → 预览 → 显示初始食物和热量(1092kcal) → 进入用餐监测
2. **update 阶段**：后台拍照（不显示预览）→ 显示已消耗热量(546kcal) → 计时器从 5:00 开始
3. **end 阶段**：拍照 → 显示总消耗热量(1030kcal) → 计时器停止在 33:27
4. **用餐总结**：显示总消耗热量(1030kcal)、用餐时长(33分钟)和营养建议

数据结构：
- start: 返回 `snapshot.nutrition.calories` (基线热量)
- update: 返回 `consumed.nutrition.calories` (已消耗热量)
- end: 返回 `consumed.nutrition.calories` (总消耗热量)

## 切换演示模式

修改 `DemoActivity.kt` 中的 `demoMode` 变量：

```kotlin
// 单图识别模式（使用本地数据）
private var demoMode = DemoMode.SINGLE_IMAGE

// 用餐监测模式（使用真实API）
private var demoMode = DemoMode.MEAL_MONITORING
```

## 图片要求

- 格式：JPEG
- 建议分辨率：1280x960 或更高
- 用餐监测模式会自动压缩到 160KB

## 获取分析结果

演示结束后，分析结果会保存到：
```
/data/data/com.rokid.nutrition/files/demo_analyze_results.json
```

获取方法：
```bash
adb shell run-as com.rokid.nutrition cat /data/data/com.rokid.nutrition/files/demo_analyze_results.json
```
