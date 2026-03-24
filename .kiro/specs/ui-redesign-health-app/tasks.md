# Implementation Plan

## 1. 基础组件和工具函数

- [x] 1.1 创建进度颜色编码工具函数
  - 实现 `getProgressColorState()` 函数
  - 实现阈值判断逻辑（<80% 绿色, 80-100% 橙色, >100% 红色）
  - _Requirements: 3.1, 3.5, 5.2_

- [ ]* 1.2 编写进度颜色编码属性测试
  - **Property 1: Progress Color Coding Consistency**
  - **Validates: Requirements 3.1, 3.5, 5.2**

- [x] 1.3 创建健康目标类型枚举和颜色映射
  - 实现 `HealthGoalType` 枚举
  - 实现目标类型到颜色的映射（lose_weight: 蓝色, maintain: 绿色, gain_muscle: 橙色）
  - _Requirements: 5.3_

- [ ]* 1.4 编写健康目标颜色映射属性测试
  - **Property 5: Health Goal Badge Color Mapping**
  - **Validates: Requirements 5.3**

- [x] 1.5 创建营养素类型枚举和颜色映射
  - 实现 `NutrientType` 枚举
  - 实现营养素到颜色的映射（protein: 蓝色, carbs: 橙色, fat: 紫色, calories: 青色）
  - _Requirements: 5.4_

- [ ]* 1.6 编写营养素颜色映射属性测试
  - **Property 6: Nutrient Color Mapping**
  - **Validates: Requirements 5.4**

## 2. BMI 和营养计算逻辑

- [x] 2.1 优化 BMI 计算和状态判断
  - 确保 `UserProfile.bmi` 计算正确
  - 实现 `getBMIStatus()` 函数返回正确的状态
  - _Requirements: 1.1, 4.3_

- [ ]* 2.2 编写 BMI 计算属性测试
  - **Property 2: BMI Calculation Correctness**
  - **Validates: Requirements 1.1, 4.3**

- [x] 2.3 实现宏量营养素比例计算
  - 根据饮食类型返回正确的蛋白质/碳水/脂肪比例
  - 默认: 25%/50%/25%, 低碳水: 30%/20%/50%, 生酮: 25%/5%/70%
  - _Requirements: 3.2, 4.3_

- [ ]* 2.4 编写宏量营养素比例属性测试
  - **Property 3: Macro Nutrient Ratio by Diet Type**
  - **Validates: Requirements 3.2, 4.3**

- [x] 2.5 优化每日热量目标计算
  - 确保 `calculateDailyCalories()` 使用 BMR * 活动系数 + 目标调整
  - 目标调整: lose_weight -500, maintain 0, gain_muscle +300
  - _Requirements: 4.3, 4.4_

- [ ]* 2.6 编写每日热量目标计算属性测试
  - **Property 4: Daily Calorie Target Calculation**
  - **Validates: Requirements 4.3, 4.4**

## 3. 体重目标进度计算

- [x] 3.1 实现体重目标进度计算函数
  - 减重: (startWeight - currentWeight) / (startWeight - targetWeight) * 100
  - 增肌: (currentWeight - startWeight) / (targetWeight - startWeight) * 100
  - _Requirements: 1.2, 3.3_

- [ ]* 3.2 编写体重目标进度属性测试
  - **Property 7: Weight Goal Progress Calculation**
  - **Validates: Requirements 1.2, 3.3**

## 4. Checkpoint - 确保所有计算逻辑测试通过
- Ensure all tests pass, ask the user if questions arise.

## 5. OnboardingScreen 滚轮选择器

- [x] 5.1 创建基础 WheelPicker 组件
  - 实现滚轮滚动效果
  - 实现选中项高亮显示（大字体、不同颜色）
  - 实现触觉反馈
  - _Requirements: 6.6, 6.7_

- [x] 5.2 创建 HeightWheelPicker 组件
  - 范围: 100-220cm, 步进: 1cm
  - _Requirements: 6.1_

- [x] 5.3 创建 WeightWheelPicker 组件
  - 范围: 30-200kg, 步进: 0.5kg
  - _Requirements: 6.2_

- [x] 5.4 创建 DateWheelPicker 组件
  - 包含年、月、日三列
  - 年份范围: 1920 - 当前年份
  - _Requirements: 6.4_

- [ ]* 5.5 编写滚轮选择器范围属性测试
  - **Property 9: Wheel Picker Range Validation**
  - **Validates: Requirements 6.1, 6.2, 6.5**

- [x] 5.6 实现年龄自动计算
  - 从出生日期计算年龄
  - _Requirements: 6.5_

- [ ]* 5.7 编写年龄计算属性测试
  - **Property 10: Age Calculation from Birth Date**
  - **Validates: Requirements 6.5**

- [x] 5.8 集成滚轮选择器到 OnboardingScreen
  - 替换现有的身高、体重、出生日期输入方式
  - 添加目标体重滚轮选择器
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

## 6. Checkpoint - 确保 Onboarding 滚轮选择器测试通过
- Ensure all tests pass, ask the user if questions arise.

## 7. ProfileScreen 重新设计

- [x] 7.1 优化 UserHeroCard 组件
  - 确保显示头像、昵称、基本信息、BMI 指示器、健康目标徽章
  - 使用正确的健康目标颜色
  - _Requirements: 1.1, 5.3_

- [x] 7.2 创建 GoalProgressCard 组件
  - 显示当前体重、目标体重、进度百分比
  - 计算并显示预计完成日期
  - 仅在设置了目标体重时显示
  - _Requirements: 1.2_

- [x] 7.3 优化 DailyNutritionSummaryCard 组件
  - 使用环形进度图显示热量摄入
  - 使用正确的进度颜色编码
  - _Requirements: 1.3, 5.2_

- [x] 7.4 优化 HealthProfileCard 组件
  - 使用 Chips 展示饮食类型、过敏原、健康状况、饮食偏好
  - 组织化布局
  - _Requirements: 1.5_

- [x] 7.5 集成所有卡片到 ProfileScreen
  - 按正确顺序排列卡片
  - 实现条件渲染逻辑
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [ ]* 7.6 编写条件卡片渲染属性测试
  - **Property 12: Conditional Card Rendering**
  - **Validates: Requirements 1.2, 1.4, 2.4**

## 8. HomeScreen 健康目标 TIPS 集成

- [x] 8.1 创建 GoalReminderCard 组件
  - 显示每日进度和激励信息
  - 仅在设置了健康目标时显示
  - _Requirements: 2.2_

- [x] 8.2 创建 CalorieWarningTip 组件
  - 当热量摄入 >= 目标的 80% 时显示警告
  - 建议选择更轻的餐食
  - _Requirements: 2.3_

- [ ]* 8.3 编写热量警告阈值属性测试
  - **Property 8: Calorie Warning Threshold**
  - **Validates: Requirements 2.3**

- [x] 8.4 创建 DietaryRestrictionTips 组件
  - 根据过敏原显示避免提示
  - 根据饮食类型显示相关建议
  - _Requirements: 2.4_

- [ ] 8.5 集成健康 TIPS 到 HomeScreen
  - 在 AIInsightsCard 下方添加 GoalReminderCard
  - 添加 CalorieWarningTip 条件显示
  - 添加 DietaryRestrictionTips
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 8.6 实现刷新建议功能
  - 点击刷新按钮获取新的个性化建议
  - _Requirements: 2.5_

## 9. Checkpoint - 确保 ProfileScreen 和 HomeScreen 测试通过
- Ensure all tests pass, ask the user if questions arise.

## 10. StatsScreen 营养统计优化

- [x] 10.1 优化 CalorieProgressRing 组件
  - 使用目标颜色编码
  - 添加动画效果
  - _Requirements: 3.1, 5.2_

- [x] 10.2 优化 MacroNutrientsCard 组件
  - 根据用户饮食类型显示推荐比例
  - 显示实际摄入 vs 推荐量的进度条
  - _Requirements: 3.2_

- [x] 10.3 创建 WeightGoalProgressCard 组件
  - 显示体重变化趋势
  - 显示预计目标完成日期
  - 仅在设置了体重目标时显示
  - _Requirements: 3.3_

- [x] 10.4 优化 WeeklyCalorieChart 组件
  - 添加目标线叠加
  - 超标日期使用警告颜色
  - _Requirements: 3.4, 3.5_

- [x] 10.5 集成所有组件到 StatsScreen
  - 按正确顺序排列组件
  - 实现条件渲染逻辑
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

## 11. 数据一致性和个性化

- [x] 11.1 确保跨页面数据一致性
  - 验证 Profile、Home、Stats 页面使用相同的数据源
  - 实现数据更新后的即时刷新
  - _Requirements: 4.1_

- [ ]* 11.2 编写跨页面数据一致性属性测试
  - **Property 11: Profile Data Consistency Across Screens**
  - **Validates: Requirements 4.1, 4.2**

- [x] 11.3 集成 Onboarding 数据到所有页面
  - 确保 Onboarding 收集的数据用于个性化所有 UI 元素
  - _Requirements: 4.2_

## 12. Final Checkpoint - 确保所有测试通过
- Ensure all tests pass, ask the user if questions arise.
