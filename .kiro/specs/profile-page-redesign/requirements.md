# Requirements Document

## Introduction

本需求文档定义了 VISEAT 食智 App 的"我的页面"重新设计功能。设计一个简洁、美观、信息层级清晰的个人中心页面，聚焦于用户档案管理和今日营养追踪。

设计原则：
1. **简洁优先** - 只展示核心信息，避免信息过载
2. **视觉层级** - 通过卡片和颜色区分不同功能区域
3. **一目了然** - 用户打开页面即可看到最重要的信息
4. **操作便捷** - 常用功能一键可达

## Glossary

- **Profile Page**: 我的页面，用户个人中心
- **User Profile**: 用户档案，包含年龄、性别、身高、体重、健康状况、饮食偏好等
- **BMI**: 身体质量指数，体重(kg)/身高(m)²
- **Daily Nutrition**: 今日营养摄入，包括热量、蛋白质、碳水化合物、脂肪
- **Health Conditions**: 健康状况，如糖尿病、高血压等
- **Dietary Preferences**: 饮食偏好，如低油、低盐、素食等
- **Target Calories**: 每日热量目标，基于用户档案计算

## Requirements

### Requirement 1: 用户信息头部

**User Story:** As a user, I want to see my profile summary in an attractive header, so that I can quickly identify my account and key metrics.

#### Acceptance Criteria

1. WHEN the user opens the Profile Page THEN the Profile Page SHALL display a gradient header card with user avatar (gender-based icon)
2. WHEN the user has profile data THEN the Profile Page SHALL display nickname, gender, age, height, and weight in a compact format
3. WHEN the user has BMI calculated THEN the Profile Page SHALL display BMI value with color-coded status indicator (green/yellow/orange/red)
4. WHEN the user taps the header card THEN the Profile Page SHALL open the full profile edit dialog
5. WHEN the user has no profile THEN the Profile Page SHALL display "点击设置个人信息" prompt with a setup icon
6. WHEN the user edits profile THEN the Profile Page SHALL allow the user to input a nickname (昵称)

### Requirement 2: 今日营养进度

**User Story:** As a user, I want to see my daily nutrition progress prominently, so that I can track my intake at a glance.

#### Acceptance Criteria

1. WHEN the user opens the Profile Page THEN the Profile Page SHALL display a circular progress indicator showing calories consumed vs target
2. WHEN the user opens the Profile Page THEN the Profile Page SHALL display current calories and target calories as text below the progress
3. WHEN calories exceed 80% of target THEN the Profile Page SHALL change the progress color to warning (orange)
4. WHEN calories exceed 100% of target THEN the Profile Page SHALL change the progress color to alert (red)
5. WHEN the user opens the Profile Page THEN the Profile Page SHALL display today's meal count with a meal icon
6. WHEN the user taps the nutrition progress card THEN the Profile Page SHALL navigate to the Stats page

### Requirement 3: 健康档案卡片

**User Story:** As a user, I want to view and edit my health profile, so that I can receive personalized nutrition advice.

#### Acceptance Criteria

1. WHEN the user opens the Profile Page THEN the Profile Page SHALL display health conditions section with condition chips
2. WHEN the user opens the Profile Page THEN the Profile Page SHALL display dietary preferences section with preference chips
3. WHEN the user has no health conditions THEN the Profile Page SHALL display "暂无" placeholder text
4. WHEN the user has no dietary preferences THEN the Profile Page SHALL display "暂无" placeholder text
5. WHEN the user taps the health card THEN the Profile Page SHALL open the edit dialog scrolled to health section

### Requirement 4: 设置列表

**User Story:** As a user, I want to access app settings and information, so that I can customize and understand the app.

#### Acceptance Criteria

1. WHEN the user opens the Profile Page THEN the Profile Page SHALL display a settings list with icon, title, and chevron
2. WHEN the user taps "调试日志" THEN the Profile Page SHALL navigate to the debug log screen
3. WHEN the user taps "关于应用" THEN the Profile Page SHALL expand to show app name, version, and description inline
4. WHEN the user taps "清除本地数据" THEN the Profile Page SHALL show a confirmation dialog with warning message
5. WHEN the user confirms data clear THEN the Profile Page SHALL clear local data and show success toast

### Requirement 5: 健康目标展示

**User Story:** As a user, I want to see my health goal and target weight, so that I can stay motivated.

#### Acceptance Criteria

1. WHEN the user has a health goal set THEN the Profile Page SHALL display the goal (减重/增肌/维持) in the header area
2. WHEN the user has a target weight set THEN the Profile Page SHALL display target weight alongside current weight
3. WHEN the user is in weight loss mode THEN the Profile Page SHALL show weight difference to target

