# Requirements Document

## Introduction

本需求文档定义了 VISEAT 食智 App 的增强版个人信息管理和目标系统。参考 YAZIO 等主流营养管理应用，打造一个全面的用户健康档案和目标追踪系统，为 AI 提供丰富的用户上下文，生成更精准的个性化饮食健康建议。

核心目标：
1. **引导式体验** - 通过分步引导流程，让用户轻松完成信息设置，同时展示产品价值
2. **全面的用户画像** - 收集更丰富的健康信息，支持 AI 个性化建议
3. **科学的目标设定** - 基于用户数据自动计算营养目标，支持手动调整
4. **可视化进度追踪** - 体重趋势、营养达成率、目标进度一目了然
5. **智能健康洞察** - 结合 AI 分析用户数据，提供个性化健康建议
6. **心理激励设计** - 在关键节点展示产品能力，增强用户信心和使用动力

## Glossary

- **Enhanced Profile**: 增强版用户档案，包含基础信息、身体数据、健康目标、饮食偏好、过敏原等
- **Nutrition Goals**: 营养目标，包括每日热量、蛋白质、碳水化合物、脂肪的目标值
- **Macro Distribution**: 宏量营养素分配比例（蛋白质/碳水/脂肪百分比）
- **Weight Tracking**: 体重追踪，记录体重变化历史
- **Goal Progress**: 目标进度，展示用户向目标体重/营养目标的进展
- **Health Insights**: 健康洞察，AI 基于用户数据生成的个性化建议
- **BMR**: 基础代谢率 (Basal Metabolic Rate)
- **TDEE**: 每日总能量消耗 (Total Daily Energy Expenditure)
- **Calorie Deficit/Surplus**: 热量缺口/盈余，用于减重/增重

## Requirements

### Requirement 1: 引导式设置流程 (Onboarding)

**User Story:** As a new user, I want to be guided through profile setup step by step, so that I can easily complete my information without feeling overwhelmed.

#### Acceptance Criteria

1. WHEN a new user opens the app for the first time THEN the Onboarding_System SHALL display a welcome screen with VisEat branding and value proposition
2. WHEN the user starts onboarding THEN the Onboarding_System SHALL present a multi-step wizard with progress indicator showing current step and total steps
3. WHEN the user completes each step THEN the Onboarding_System SHALL animate transition to next step with encouraging feedback
4. WHEN the user is on any step THEN the Onboarding_System SHALL allow going back to previous steps without losing entered data
5. WHEN the user wants to skip onboarding THEN the Onboarding_System SHALL allow skipping with option to complete later from profile page
6. WHEN onboarding is skipped THEN the Onboarding_System SHALL use default values and show reminder badge on profile tab

### Requirement 2: 引导步骤一 - 健康目标选择

**User Story:** As a user, I want to first tell the system my health goal, so that I feel the app understands my needs from the start.

#### Acceptance Criteria

1. WHEN the user reaches goal selection step THEN the Onboarding_System SHALL display three visually distinct goal cards: "减重瘦身", "维持健康", "增肌塑形"
2. WHEN the user selects a goal THEN the Onboarding_System SHALL highlight the selected card with animation and show a motivational message
3. WHEN "减重瘦身" is selected THEN the Onboarding_System SHALL display message "我们将帮助您科学减重，每周建议减少0.5-1kg"
4. WHEN "增肌塑形" is selected THEN the Onboarding_System SHALL display message "我们将帮助您合理增肌，确保营养充足"
5. WHEN "维持健康" is selected THEN the Onboarding_System SHALL display message "我们将帮助您保持均衡饮食，维持理想状态"
6. WHEN goal is selected THEN the Onboarding_System SHALL show a capability highlight: "VisEat 的 AI 将根据您的目标，为每一餐提供个性化建议"

### Requirement 3: 引导步骤二 - 基础身体数据

**User Story:** As a user, I want to input my body measurements in a friendly way, so that the system can calculate my needs accurately.

#### Acceptance Criteria

1. WHEN the user reaches body data step THEN the Onboarding_System SHALL display gender selection with friendly icons (not clinical)
2. WHEN gender is selected THEN the Onboarding_System SHALL display birthdate picker with wheel-style selector
3. WHEN birthdate is selected THEN the Onboarding_System SHALL automatically calculate and display age with friendly text "您今年 XX 岁"
4. WHEN age is displayed THEN the Onboarding_System SHALL show height input with slider (140-220cm) and numeric display
5. WHEN height is set THEN the Onboarding_System SHALL show weight input with slider (30-150kg) and numeric display
6. WHEN height and weight are both set THEN the Onboarding_System SHALL calculate and display BMI with color-coded status and friendly explanation
7. WHEN BMI is displayed THEN the Onboarding_System SHALL show capability highlight: "基于 6,300+ 食物数据库，我们能精准计算您的营养需求"

### Requirement 4: 引导步骤三 - 目标体重设定

**User Story:** As a user with weight goals, I want to set my target weight and timeline, so that I have a clear objective to work toward.

#### Acceptance Criteria

1. WHEN health goal is "减重瘦身" or "增肌塑形" THEN the Onboarding_System SHALL display target weight input step
2. WHEN health goal is "维持健康" THEN the Onboarding_System SHALL skip target weight step
3. WHEN target weight step is shown THEN the Onboarding_System SHALL display current weight and allow setting target weight with slider
4. WHEN target weight is set THEN the Onboarding_System SHALL calculate weight difference and display "目标：减/增 X kg"
5. WHEN target weight is set THEN the Onboarding_System SHALL prompt for target date with calendar picker (minimum 2 weeks from now)
6. WHEN target date is set THEN the Onboarding_System SHALL calculate weekly rate and display "每周约减/增 X kg"
7. WHEN weekly rate exceeds safe limits (>1kg/week loss or >0.5kg/week gain) THEN the Onboarding_System SHALL show warning with suggested safer timeline
8. WHEN target is set THEN the Onboarding_System SHALL show capability highlight: "我们的基线差分算法能精准追踪您的每一餐摄入，误差降低60%"

### Requirement 5: 引导步骤四 - 活动量评估

**User Story:** As a user, I want to describe my activity level, so that the system can calculate my calorie needs accurately.

#### Acceptance Criteria

1. WHEN the user reaches activity step THEN the Onboarding_System SHALL display five activity level cards with icons and descriptions
2. WHEN activity level options are shown THEN the Onboarding_System SHALL display: "久坐办公" (1.2), "轻度活动" (1.375), "中度活动" (1.55), "高度活动" (1.725), "专业运动" (1.9)
3. WHEN each option is shown THEN the Onboarding_System SHALL include example description (e.g., "每周运动3-5次，每次30分钟以上")
4. WHEN activity level is selected THEN the Onboarding_System SHALL highlight selection and show estimated daily calorie burn
5. WHEN activity is set THEN the Onboarding_System SHALL show capability highlight: "结合您的活动量，AI 将动态调整每日营养目标"

### Requirement 6: 引导步骤五 - 饮食偏好

**User Story:** As a user, I want to specify my dietary preferences and restrictions, so that the AI can give me suitable recommendations.

#### Acceptance Criteria

1. WHEN the user reaches dietary preferences step THEN the Onboarding_System SHALL display diet type selection with visual cards
2. WHEN diet type options are shown THEN the Onboarding_System SHALL include: "无限制", "素食", "纯素", "低碳水", "生酮", "地中海饮食"
3. WHEN diet type is selected THEN the Onboarding_System SHALL show allergen selection as multi-select chips
4. WHEN allergen options are shown THEN the Onboarding_System SHALL include: "无过敏", "麸质", "乳制品", "坚果", "海鲜", "鸡蛋", "大豆"
5. WHEN preferences are set THEN the Onboarding_System SHALL show capability highlight: "我们整合了中国疾控中心等权威数据，为您提供最适合的饮食建议"

### Requirement 7: 引导完成 - 个性化计划展示

**User Story:** As a user completing onboarding, I want to see my personalized plan, so that I feel confident the app understands me.

#### Acceptance Criteria

1. WHEN the user completes all onboarding steps THEN the Onboarding_System SHALL display a personalized summary screen
2. WHEN summary is shown THEN the Onboarding_System SHALL display calculated daily calorie target with breakdown (BMR + activity - deficit/surplus)
3. WHEN summary is shown THEN the Onboarding_System SHALL display macro targets: protein (g), carbs (g), fat (g) with visual pie chart
4. WHEN summary is shown THEN the Onboarding_System SHALL display estimated goal achievement date based on user inputs
5. WHEN summary is shown THEN the Onboarding_System SHALL display a motivational message: "您的专属营养计划已生成！戴上眼镜，开始您的健康之旅"
6. WHEN summary is shown THEN the Onboarding_System SHALL show VisEat core value: "让 AI 成为您的私人营养师，每一餐都吃得明白"
7. WHEN the user confirms THEN the Onboarding_System SHALL save all data and navigate to home screen with celebration animation

### Requirement 8: 增强版用户基础信息（非引导模式）

**User Story:** As an existing user, I want to edit my profile information from the profile page, so that I can update my data anytime.

#### Acceptance Criteria

1. WHEN the user opens profile edit from profile page THEN the Enhanced_Profile_System SHALL display all fields in a scrollable form
2. WHEN editing profile THEN the Enhanced_Profile_System SHALL pre-fill all existing values
3. WHEN the user changes height or weight THEN the Enhanced_Profile_System SHALL recalculate BMI in real-time
4. WHEN the user saves profile THEN the Enhanced_Profile_System SHALL validate all required fields before saving
5. WHEN validation fails THEN the Enhanced_Profile_System SHALL display specific error messages for each invalid field
6. WHEN profile is saved THEN the Enhanced_Profile_System SHALL recalculate all nutrition targets based on new data

### Requirement 9: 营养目标自动计算

**User Story:** As a user, I want the system to automatically calculate my daily nutrition targets, so that I have science-based goals to follow.

#### Acceptance Criteria

1. WHEN user profile is complete THEN the Enhanced_Profile_System SHALL calculate BMR using Mifflin-St Jeor formula
2. WHEN BMR is calculated THEN the Enhanced_Profile_System SHALL calculate TDEE by multiplying BMR with activity level factor
3. WHEN health goal is "lose weight" THEN the Enhanced_Profile_System SHALL set calorie target as TDEE minus calorie deficit (300-500 kcal)
4. WHEN health goal is "gain muscle" THEN the Enhanced_Profile_System SHALL set calorie target as TDEE plus calorie surplus (200-300 kcal)
5. WHEN calorie target is set THEN the Enhanced_Profile_System SHALL calculate macro targets using default distribution (protein 25%, carbs 50%, fat 25%)
6. WHEN the user views nutrition goals THEN the Enhanced_Profile_System SHALL display daily targets for calories, protein (g), carbs (g), and fat (g)

### Requirement 10: 营养目标手动调整

**User Story:** As a user, I want to manually adjust my nutrition targets, so that I can customize goals based on my preferences or professional advice.

#### Acceptance Criteria

1. WHEN the user opens nutrition goals settings THEN the Enhanced_Profile_System SHALL display current targets with edit option
2. WHEN the user adjusts calorie target THEN the Enhanced_Profile_System SHALL recalculate macro targets proportionally
3. WHEN the user adjusts macro distribution percentages THEN the Enhanced_Profile_System SHALL ensure total equals 100%
4. WHEN macro percentages do not sum to 100% THEN the Enhanced_Profile_System SHALL display error and prevent saving
5. WHEN the user saves custom targets THEN the Enhanced_Profile_System SHALL mark them as "custom" and preserve them across profile updates
6. WHEN the user wants to reset to calculated values THEN the Enhanced_Profile_System SHALL provide a "reset to recommended" button

### Requirement 11: 体重追踪记录

**User Story:** As a user, I want to log my weight regularly, so that I can track my progress toward my goal weight.

#### Acceptance Criteria

1. WHEN the user opens weight tracking THEN the Enhanced_Profile_System SHALL display current weight and goal weight
2. WHEN the user adds a weight entry THEN the Enhanced_Profile_System SHALL record weight value and timestamp
3. WHEN weight entry is saved THEN the Enhanced_Profile_System SHALL update current weight in user profile
4. WHEN the user views weight history THEN the Enhanced_Profile_System SHALL display a line chart showing weight trend over time
5. WHEN weight history has data THEN the Enhanced_Profile_System SHALL calculate and display average weekly change
6. WHEN current weight reaches goal weight THEN the Enhanced_Profile_System SHALL display congratulations message and suggest new goal

### Requirement 12: 目标进度展示

**User Story:** As a user, I want to see my progress toward my goals, so that I can stay motivated and adjust my behavior.

#### Acceptance Criteria

1. WHEN the user opens profile page THEN the Enhanced_Profile_System SHALL display goal progress card with visual indicator
2. WHEN health goal is weight-related THEN the Enhanced_Profile_System SHALL show weight progress as percentage toward target
3. WHEN the user has nutrition data for today THEN the Enhanced_Profile_System SHALL show today's macro achievement percentages
4. WHEN any macro exceeds 100% of target THEN the Enhanced_Profile_System SHALL highlight it with warning color
5. WHEN the user taps progress card THEN the Enhanced_Profile_System SHALL navigate to detailed stats page

### Requirement 13: 饮食偏好和限制（详细设置）

**User Story:** As a user, I want to specify my dietary preferences and restrictions, so that the AI can provide suitable food recommendations.

#### Acceptance Criteria

1. WHEN the user opens dietary settings THEN the Enhanced_Profile_System SHALL display diet type options (omnivore, vegetarian, vegan, pescatarian, keto, low-carb)
2. WHEN the user selects diet type THEN the Enhanced_Profile_System SHALL adjust default macro distribution accordingly
3. WHEN the user opens allergen settings THEN the Enhanced_Profile_System SHALL display common allergens as selectable chips (gluten, dairy, nuts, shellfish, eggs, soy)
4. WHEN the user selects allergens THEN the Enhanced_Profile_System SHALL store them for AI advice generation
5. WHEN the user opens food preferences THEN the Enhanced_Profile_System SHALL allow selecting disliked foods from common categories

### Requirement 14: 活动量详细设置

**User Story:** As a user, I want to accurately describe my activity level, so that the system can calculate appropriate calorie needs.

#### Acceptance Criteria

1. WHEN the user opens activity settings THEN the Enhanced_Profile_System SHALL display five activity level options with descriptions
2. WHEN the user selects activity level THEN the Enhanced_Profile_System SHALL display the corresponding activity factor (1.2 to 1.9)
3. WHEN activity level changes THEN the Enhanced_Profile_System SHALL recalculate TDEE and nutrition targets
4. WHEN the user has exercise data THEN the Enhanced_Profile_System SHALL suggest appropriate activity level based on data

### Requirement 15: AI 健康洞察

**User Story:** As a user, I want to receive AI-powered health insights based on my profile and eating history, so that I can make better dietary decisions.

#### Acceptance Criteria

1. WHEN the user opens profile page THEN the Enhanced_Profile_System SHALL display an AI insights card with personalized tips
2. WHEN generating insights THEN the Enhanced_Profile_System SHALL consider user profile, health goals, recent eating patterns, and nutrition gaps
3. WHEN the user has consistent calorie excess THEN the Enhanced_Profile_System SHALL suggest portion control strategies
4. WHEN the user has protein deficiency pattern THEN the Enhanced_Profile_System SHALL suggest protein-rich food options
5. WHEN the user taps insights card THEN the Enhanced_Profile_System SHALL show detailed analysis with actionable recommendations
6. WHEN generating AI insights THEN the Enhanced_Profile_System SHALL call backend API with user context for LLM-based advice

### Requirement 16: 数据同步和持久化

**User Story:** As a user, I want my profile and goals to be saved and synced, so that I don't lose my data.

#### Acceptance Criteria

1. WHEN the user saves profile changes THEN the Enhanced_Profile_System SHALL persist data to local database immediately
2. WHEN network is available THEN the Enhanced_Profile_System SHALL sync profile data to backend
3. WHEN sync fails THEN the Enhanced_Profile_System SHALL queue the update for retry
4. WHEN the user reinstalls app THEN the Enhanced_Profile_System SHALL restore profile from backend using device ID
5. WHEN weight entry is added THEN the Enhanced_Profile_System SHALL sync to backend for cross-device access

