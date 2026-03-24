# Requirements Document

## Introduction

本需求文档描述了 VISEAT 食智 App 的 UI 界面重新设计，目标是打造一个完整的健康管理应用体验。重点包括：ProfileScreen（我的页面）重新设计、HomeScreen（首页）健康目标 TIPS 集成、StatsScreen（统计页面）优化，结合已收集的用户信息（Onboarding 数据）提供个性化的健康管理体验。

## Glossary

- **ProfileScreen**: 我的页面，展示用户档案、健康目标、体重追踪等
- **HomeScreen**: 首页，展示眼镜连接状态、用餐记录、AI 健康洞察
- **StatsScreen**: 统计页面，展示营养数据统计和趋势
- **UserProfile**: 用户档案数据，包含身体数据、健康目标、饮食偏好等
- **AI 健康洞察**: 基于用户数据生成的个性化健康建议
- **健康目标进度**: 用户设定的减重/增肌/维持目标的完成进度

## Requirements

### Requirement 1: ProfileScreen 重新设计

**User Story:** As a user, I want to see my complete health profile in a visually appealing and organized way, so that I can easily track my health goals and manage my personal information.

#### Acceptance Criteria

1. WHEN a user opens the ProfileScreen THEN the system SHALL display a hero card showing user avatar, nickname, basic stats (age, gender, height, weight), BMI indicator, and health goal badge
2. WHEN the user has set a weight goal THEN the system SHALL display a goal progress card showing current weight, target weight, progress percentage, and estimated completion date
3. WHEN the user views the ProfileScreen THEN the system SHALL display a daily nutrition summary card showing today's calorie intake vs target with a circular progress indicator
4. WHEN the user has weight records THEN the system SHALL display a weight tracking card with mini trend chart and quick-add button
5. WHEN the user views health profile THEN the system SHALL display diet type, allergens, health conditions, and dietary preferences in organized chips
6. WHEN the user taps on any editable section THEN the system SHALL open the corresponding edit dialog

### Requirement 2: HomeScreen 健康目标 TIPS 集成

**User Story:** As a user, I want to see personalized health tips on the home screen based on my goals and eating habits, so that I can make better food choices throughout the day.

#### Acceptance Criteria

1. WHEN a user opens the HomeScreen THEN the system SHALL display an AI insights card with personalized health tips based on user profile and recent meals
2. WHEN the user has an active health goal THEN the system SHALL display a goal reminder card showing daily progress and motivational message
3. WHEN the user's calorie intake approaches the target THEN the system SHALL display a warning tip suggesting lighter meal options
4. WHEN the user has dietary restrictions THEN the system SHALL display relevant tips about avoiding allergens or following diet type
5. WHEN the user taps refresh on tips THEN the system SHALL fetch new personalized recommendations from the backend

### Requirement 3: StatsScreen 营养统计优化

**User Story:** As a user, I want to see comprehensive nutrition statistics that relate to my health goals, so that I can understand my eating patterns and make adjustments.

#### Acceptance Criteria

1. WHEN a user opens the StatsScreen THEN the system SHALL display a calorie progress ring showing consumed vs target calories with goal-based color coding
2. WHEN the user views nutrition stats THEN the system SHALL display macro nutrients (protein, carbs, fat) with progress bars showing actual vs recommended amounts based on user's diet type
3. WHEN the user has a weight goal THEN the system SHALL display a goal progress section showing weight change trend and projected goal completion
4. WHEN the user views weekly stats THEN the system SHALL display a bar chart showing daily calorie intake with target line overlay
5. WHEN the user's intake exceeds targets THEN the system SHALL highlight the excess in warning colors and provide adjustment suggestions

### Requirement 4: 数据一致性和个性化

**User Story:** As a user, I want all screens to reflect my personal data and goals consistently, so that I have a unified health management experience.

#### Acceptance Criteria

1. WHEN user profile data changes THEN the system SHALL update all screens (Profile, Home, Stats) to reflect the new data immediately
2. WHEN the user completes onboarding THEN the system SHALL use the collected data (health goal, body data, diet preferences) to personalize all UI elements
3. WHEN calculating nutrition targets THEN the system SHALL use the user's BMR, activity level, and health goal to determine personalized calorie and macro targets
4. WHEN displaying progress THEN the system SHALL calculate percentages based on user's specific targets rather than generic defaults

### Requirement 5: 视觉设计一致性

**User Story:** As a user, I want a visually consistent and modern design across all screens, so that the app feels professional and easy to use.

#### Acceptance Criteria

1. WHEN displaying cards THEN the system SHALL use consistent Apple-style rounded corners (16dp), shadows, and spacing
2. WHEN showing progress indicators THEN the system SHALL use consistent color coding: green for on-track, orange for approaching limit, red for exceeded
3. WHEN displaying health goal badges THEN the system SHALL use distinct colors and icons for each goal type (lose_weight: blue, maintain: green, gain_muscle: orange)
4. WHEN showing nutrition data THEN the system SHALL use consistent color scheme: protein (blue), carbs (orange), fat (purple), calories (teal)

### Requirement 6: OnboardingScreen 输入方式优化

**User Story:** As a user, I want to input my body data using intuitive wheel pickers instead of sliders or manual text input, so that the input experience is more natural and precise.

#### Acceptance Criteria

1. WHEN inputting height THEN the system SHALL display a wheel picker (NumberPicker/WheelPicker) with values from 100cm to 220cm in 1cm increments
2. WHEN inputting weight THEN the system SHALL display a wheel picker with values from 30kg to 200kg in 0.5kg increments
3. WHEN inputting target weight THEN the system SHALL display a wheel picker with values based on current weight range
4. WHEN inputting birth date THEN the system SHALL display a date wheel picker with year, month, day columns
5. WHEN inputting age THEN the system SHALL calculate automatically from birth date or display a wheel picker with values from 10 to 100
6. WHEN the user scrolls the wheel picker THEN the system SHALL provide haptic feedback and smooth scrolling animation
7. WHEN displaying wheel pickers THEN the system SHALL highlight the selected value in the center with larger font and distinct color

