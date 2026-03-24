# 智能健康提示系统需求规范

## Introduction

重新设计首页的 AI 健康洞察模块，使其能够基于用户真实数据生成个性化、有意义的健康提示。提示内容应该高度结合用户的实际行为数据，而不是随机生成的通用建议。同时优化 UI 交互，采用自动滚动轮播的方式展示多条建议，移除生硬的反馈按钮。

## Glossary

- **Smart_Health_Tips_System**: 智能健康提示系统，基于用户数据生成个性化健康建议的模块
- **User_Data_Context**: 用户数据上下文，包括进食记录、体重变化、营养摄入等用户行为数据
- **Tip_Generator**: 提示生成器，根据用户数据上下文生成相应的健康提示
- **Auto_Carousel**: 自动轮播组件，自动滚动展示多条提示的 UI 组件

## Requirements

### Requirement 1

**User Story:** As a 健康管理用户, I want 看到基于我真实数据的健康提示, so that 我能获得有针对性的健康建议而不是通用的泛泛之谈。

#### Acceptance Criteria

1. WHEN 用户没有任何进食记录 THEN Smart_Health_Tips_System SHALL 显示引导性提示"开始记录第一顿美食吧！戴上眼镜，开启健康之旅 🍽️"
2. WHEN 用户最近一餐的进食时间少于10分钟 THEN Smart_Health_Tips_System SHALL 显示提示"根据最近的进食时间观察，细嚼慢咽有助于消化哦～"
3. WHEN 用户最近7天的高热量食物（单餐超过800kcal）占比超过50% THEN Smart_Health_Tips_System SHALL 显示提示"最近高热量食物吃的有点多，注意控制哦！"
4. WHEN 用户今日热量摄入已超过目标的80% THEN Smart_Health_Tips_System SHALL 显示提示"今日热量即将达标，晚餐建议选择清淡食物"
5. WHEN 用户连续3天蛋白质摄入不足目标的60% THEN Smart_Health_Tips_System SHALL 显示提示"蛋白质摄入偏低，建议多吃鸡蛋、鱼肉、豆制品"
6. WHEN 用户体重目标是减重且本周体重有下降 THEN Smart_Health_Tips_System SHALL 显示鼓励提示"本周减重进展不错，继续保持！💪"
7. WHEN 用户超过6小时未进食 THEN Smart_Health_Tips_System SHALL 显示提示"距离上次进食已超过6小时，记得按时吃饭哦"

### Requirement 2

**User Story:** As a 健康管理用户, I want 健康提示以自动轮播的方式展示, so that 我能轻松浏览多条建议而无需手动操作。

#### Acceptance Criteria

1. WHEN 有多条健康提示时 THEN Auto_Carousel SHALL 每5秒自动切换到下一条提示
2. WHEN 用户触摸提示卡片时 THEN Auto_Carousel SHALL 暂停自动轮播
3. WHEN 用户手指离开提示卡片3秒后 THEN Auto_Carousel SHALL 恢复自动轮播
4. WHEN 轮播到最后一条提示后 THEN Auto_Carousel SHALL 循环回到第一条提示
5. WHEN 只有一条提示时 THEN Auto_Carousel SHALL 不显示轮播指示器且不进行轮播

### Requirement 3

**User Story:** As a 健康管理用户, I want 健康提示界面简洁友好, so that 我能快速获取信息而不被多余的交互元素干扰。

#### Acceptance Criteria

1. THE Smart_Health_Tips_System SHALL 移除"这条建议有帮助吗？"的反馈按钮
2. THE Smart_Health_Tips_System SHALL 使用底部圆点指示器显示当前提示位置
3. WHEN 提示内容较长时 THEN Smart_Health_Tips_System SHALL 最多显示2行文字并使用省略号
4. THE Smart_Health_Tips_System SHALL 为不同类型的提示使用不同的图标和颜色主题

### Requirement 4

**User Story:** As a 健康管理用户, I want 提示内容实时反映我的最新数据, so that 建议始终是相关和及时的。

#### Acceptance Criteria

1. WHEN 用户完成一次进食记录后 THEN Tip_Generator SHALL 在30秒内更新相关提示
2. WHEN 用户记录新的体重数据后 THEN Tip_Generator SHALL 更新体重相关的提示
3. WHEN 日期变更时 THEN Tip_Generator SHALL 重新计算并更新所有提示
4. THE Tip_Generator SHALL 根据提示的优先级排序，重要提示优先显示

## Design Principles

### 1. 数据驱动
- 所有提示必须基于用户真实数据生成
- 没有数据时显示引导性提示，而不是通用建议
- 提示内容应该具体、可操作

### 2. 简洁友好
- 移除不必要的交互元素
- 使用自然语言，避免生硬的表达
- 适当使用 emoji 增加亲和力

### 3. 及时响应
- 数据变化后及时更新提示
- 提示内容与用户当前状态高度相关

## Technical Requirements

### 数据依赖
- 进食记录数据（时间、热量、营养素）
- 体重记录数据
- 用户目标设置（目标体重、目标热量等）
- 用户档案信息

### 性能要求
- 提示生成时间 < 100ms
- 轮播动画流畅（60fps）
- 内存占用优化

## Priority

### P0 (必须有)
- 基于数据的提示生成逻辑
- 自动轮播 UI
- 移除反馈按钮

### P1 (应该有)
- 提示优先级排序
- 数据变化后实时更新
- 丰富的提示类型

### P2 (可以有)
- 提示历史记录
- 用户偏好学习
- 更多个性化维度
