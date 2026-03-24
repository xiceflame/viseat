# Design Document: Enhanced Profile & Goals System

## Overview

本设计文档描述了 VISEAT 食智 App 的增强版个人信息管理和目标系统。核心设计理念是通过精心设计的引导式体验（Onboarding），让用户在轻松愉快的氛围中完成个人信息设置，同时在关键节点展示产品核心能力，建立用户信任和使用动力。

### 设计原则

1. **渐进式披露** - 不一次性展示所有字段，而是分步引导，降低认知负担
2. **即时反馈** - 每一步输入都有视觉反馈，让用户感受到进展
3. **情感化设计** - 使用友好的语言和动画，创造愉悦的设置体验
4. **价值传递** - 在关键节点展示产品能力，让用户理解"为什么要填这些信息"
5. **灵活性** - 允许跳过，但提供完成激励

### 用户体验流程

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Onboarding 用户旅程                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   欢迎页 → 目标选择 → 身体数据 → 目标体重 → 活动量 → 饮食偏好 → 计划展示  │
│     │         │          │          │         │         │          │
│     ↓         ↓          ↓          ↓         ↓         ↓          │
│   品牌     激励文案    BMI计算    安全检查   热量估算   权威数据    营养目标  │
│   价值     AI能力      数据库     算法优势   AI调整    数据来源    个性化    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Architecture

### 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Enhanced Profile System                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   UI Layer (Compose)                                                │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  OnboardingScreen    ProfileScreen    GoalProgressCard      │   │
│   │  ├─ WelcomeStep      ├─ UserHeader    ├─ WeightProgress     │   │
│   │  ├─ GoalStep         ├─ NutritionCard ├─ MacroProgress      │   │
│   │  ├─ BodyDataStep     ├─ HealthCard    └─ InsightsCard       │   │
│   │  ├─ TargetStep       ├─ SettingsCard                        │   │
│   │  ├─ ActivityStep     └─ EditDialog                          │   │
│   │  ├─ DietaryStep                                             │   │
│   │  └─ SummaryStep                                             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│   ViewModel Layer                                                   │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  OnboardingViewModel    ProfileViewModel    GoalsViewModel   │   │
│   │  ├─ onboardingState     ├─ profileState     ├─ nutritionGoals│   │
│   │  ├─ currentStep         ├─ weightHistory    ├─ weightProgress│   │
│   │  └─ stepData            └─ aiInsights       └─ macroProgress │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│   Domain Layer                                                      │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  NutritionCalculator    WeightTracker    GoalProgressTracker │   │
│   │  ├─ calculateBMR()      ├─ addEntry()    ├─ getProgress()   │   │
│   │  ├─ calculateTDEE()     ├─ getTrend()    ├─ getMacroStatus()│   │
│   │  ├─ calculateMacros()   └─ getAvgChange()└─ checkGoalReached│   │
│   │  └─ validateGoal()                                          │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                              ↓                                      │
│   Data Layer                                                        │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │  EnhancedProfileRepository    WeightEntryRepository          │   │
│   │  ├─ saveProfile()             ├─ addWeightEntry()           │   │
│   │  ├─ getProfile()              ├─ getWeightHistory()         │   │
│   │  └─ syncToBackend()           └─ syncWeightData()           │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Onboarding 引导流程

#### 1.1 OnboardingScreen - 主容器

```kotlin
@Composable
fun OnboardingScreen(
    onComplete: (EnhancedProfile) -> Unit,
    onSkip: () -> Unit
) {
    val viewModel: OnboardingViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景渐变
        GradientBackground()
        
        Column {
            // 进度指示器
            OnboardingProgressIndicator(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps
            )
            
            // 步骤内容
            AnimatedContent(targetState = state.currentStep) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(onStart = viewModel::startOnboarding)
                    OnboardingStep.GOAL -> GoalSelectionStep(...)
                    OnboardingStep.BODY_DATA -> BodyDataStep(...)
                    OnboardingStep.TARGET_WEIGHT -> TargetWeightStep(...)
                    OnboardingStep.ACTIVITY -> ActivityLevelStep(...)
                    OnboardingStep.DIETARY -> DietaryPreferencesStep(...)
                    OnboardingStep.SUMMARY -> SummaryStep(...)
                }
            }
        }
        
        // 跳过按钮
        if (state.canSkip) {
            SkipButton(onClick = onSkip)
        }
    }
}
```

#### 1.2 WelcomeStep - 欢迎页

```kotlin
@Composable
fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo 动画
        AnimatedLogo()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 标题
        Text(
            text = "欢迎使用 VisEat",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 价值主张
        Text(
            text = "让 AI 成为您的私人营养师",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "每一餐都吃得明白，每个人都活得健康",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 开始按钮
        PrimaryButton(
            text = "开始设置",
            onClick = onStart
        )
    }
}
```

#### 1.3 GoalSelectionStep - 目标选择

```kotlin
@Composable
fun GoalSelectionStep(
    selectedGoal: HealthGoal?,
    onGoalSelected: (HealthGoal) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // 步骤标题
        StepHeader(
            title = "您的健康目标是什么？",
            subtitle = "选择一个最符合您当前需求的目标"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 目标卡片
        GoalCard(
            icon = Icons.Rounded.TrendingDown,
            title = "减重瘦身",
            description = "科学减脂，健康瘦身",
            isSelected = selectedGoal == HealthGoal.LOSE_WEIGHT,
            onClick = { onGoalSelected(HealthGoal.LOSE_WEIGHT) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        GoalCard(
            icon = Icons.Rounded.Balance,
            title = "维持健康",
            description = "保持均衡，维持理想状态",
            isSelected = selectedGoal == HealthGoal.MAINTAIN,
            onClick = { onGoalSelected(HealthGoal.MAINTAIN) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        GoalCard(
            icon = Icons.Rounded.FitnessCenter,
            title = "增肌塑形",
            description = "增加肌肉，塑造体型",
            isSelected = selectedGoal == HealthGoal.GAIN_MUSCLE,
            onClick = { onGoalSelected(HealthGoal.GAIN_MUSCLE) }
        )
        
        // 选中后显示激励文案
        AnimatedVisibility(visible = selectedGoal != null) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                
                // 激励文案
                MotivationalMessage(goal = selectedGoal!!)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 产品能力展示
                CapabilityHighlight(
                    icon = Icons.Rounded.Psychology,
                    text = "VisEat 的 AI 将根据您的目标，为每一餐提供个性化建议"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 下一步按钮
        PrimaryButton(
            text = "下一步",
            enabled = selectedGoal != null,
            onClick = onNext
        )
    }
}

@Composable
private fun MotivationalMessage(goal: HealthGoal) {
    val message = when (goal) {
        HealthGoal.LOSE_WEIGHT -> "我们将帮助您科学减重，每周建议减少 0.5-1kg"
        HealthGoal.MAINTAIN -> "我们将帮助您保持均衡饮食，维持理想状态"
        HealthGoal.GAIN_MUSCLE -> "我们将帮助您合理增肌，确保营养充足"
    }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
```

#### 1.4 BodyDataStep - 身体数据输入

```kotlin
@Composable
fun BodyDataStep(
    gender: Gender?,
    birthDate: LocalDate?,
    height: Float?,
    weight: Float?,
    onGenderSelected: (Gender) -> Unit,
    onBirthDateSelected: (LocalDate) -> Unit,
    onHeightChanged: (Float) -> Unit,
    onWeightChanged: (Float) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val age = birthDate?.let { calculateAge(it) }
    val bmi = if (height != null && weight != null) calculateBMI(height, weight) else null
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        StepHeader(
            title = "告诉我们一些基本信息",
            subtitle = "这些信息将帮助我们计算您的营养需求"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 性别选择 - 友好图标
        Text("您的性别", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GenderCard(
                icon = Icons.Rounded.Male,
                label = "男",
                isSelected = gender == Gender.MALE,
                onClick = { onGenderSelected(Gender.MALE) },
                modifier = Modifier.weight(1f)
            )
            GenderCard(
                icon = Icons.Rounded.Female,
                label = "女",
                isSelected = gender == Gender.FEMALE,
                onClick = { onGenderSelected(Gender.FEMALE) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 出生日期 - 滚轮选择器
        Text("您的出生日期", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        DateWheelPicker(
            selectedDate = birthDate,
            onDateSelected = onBirthDateSelected
        )
        
        // 年龄显示
        AnimatedVisibility(visible = age != null) {
            Text(
                text = "您今年 $age 岁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 身高滑块
        Text("您的身高", style = MaterialTheme.typography.labelLarge)
        HeightSlider(
            value = height ?: 170f,
            onValueChange = onHeightChanged,
            valueRange = 140f..220f
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 体重滑块
        Text("您的体重", style = MaterialTheme.typography.labelLarge)
        WeightSlider(
            value = weight ?: 65f,
            onValueChange = onWeightChanged,
            valueRange = 30f..150f
        )
        
        // BMI 显示
        AnimatedVisibility(visible = bmi != null) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                BMIIndicator(bmi = bmi!!)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 产品能力展示
                CapabilityHighlight(
                    icon = Icons.Rounded.Storage,
                    text = "基于 6,300+ 食物数据库，我们能精准计算您的营养需求"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 导航按钮
        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = gender != null && birthDate != null && height != null && weight != null
        )
    }
}


#### 1.5 TargetWeightStep - 目标体重设定

```kotlin
@Composable
fun TargetWeightStep(
    currentWeight: Float,
    healthGoal: HealthGoal,
    targetWeight: Float?,
    targetDate: LocalDate?,
    onTargetWeightChanged: (Float) -> Unit,
    onTargetDateSelected: (LocalDate) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val weightDiff = targetWeight?.let { currentWeight - it }
    val weeklyRate = if (targetWeight != null && targetDate != null) {
        calculateWeeklyRate(currentWeight, targetWeight, targetDate)
    } else null
    val isSafeRate = weeklyRate?.let { isWeeklyRateSafe(it, healthGoal) } ?: true
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        StepHeader(
            title = if (healthGoal == HealthGoal.LOSE_WEIGHT) "您的目标体重" else "您的增肌目标",
            subtitle = "设定一个合理的目标，我们会帮您制定计划"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 当前体重显示
        CurrentWeightDisplay(weight = currentWeight)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 目标体重滑块
        Text("目标体重", style = MaterialTheme.typography.labelLarge)
        TargetWeightSlider(
            currentWeight = currentWeight,
            targetWeight = targetWeight ?: currentWeight,
            healthGoal = healthGoal,
            onValueChange = onTargetWeightChanged
        )
        
        // 体重差显示
        AnimatedVisibility(visible = weightDiff != null && weightDiff != 0f) {
            val diffText = if (healthGoal == HealthGoal.LOSE_WEIGHT) {
                "目标：减 ${abs(weightDiff!!).toInt()} kg"
            } else {
                "目标：增 ${abs(weightDiff!!).toInt()} kg"
            }
            Text(
                text = diffText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 目标日期选择
        Text("计划达成日期", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        TargetDatePicker(
            selectedDate = targetDate,
            minDate = LocalDate.now().plusWeeks(2),
            onDateSelected = onTargetDateSelected
        )
        
        // 每周速率显示
        AnimatedVisibility(visible = weeklyRate != null) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                
                WeeklyRateDisplay(
                    rate = weeklyRate!!,
                    healthGoal = healthGoal,
                    isSafe = isSafeRate
                )
                
                // 安全警告
                if (!isSafeRate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyWarning(
                        message = if (healthGoal == HealthGoal.LOSE_WEIGHT) {
                            "每周减重超过 1kg 可能不健康，建议延长目标日期"
                        } else {
                            "每周增重超过 0.5kg 可能导致脂肪堆积，建议延长目标日期"
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 产品能力展示
                CapabilityHighlight(
                    icon = Icons.Rounded.Analytics,
                    text = "我们的基线差分算法能精准追踪您的每一餐摄入，误差降低 60%"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = targetWeight != null && targetDate != null
        )
    }
}
```

#### 1.6 ActivityLevelStep - 活动量评估

```kotlin
@Composable
fun ActivityLevelStep(
    selectedLevel: ActivityLevel?,
    onLevelSelected: (ActivityLevel) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        StepHeader(
            title = "您的日常活动量",
            subtitle = "这将帮助我们计算您每天消耗的热量"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 活动量选项
        ActivityLevelCard(
            icon = Icons.Rounded.Chair,
            title = "久坐办公",
            description = "几乎不运动，长时间坐着工作",
            factor = "1.2",
            isSelected = selectedLevel == ActivityLevel.SEDENTARY,
            onClick = { onLevelSelected(ActivityLevel.SEDENTARY) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ActivityLevelCard(
            icon = Icons.Rounded.DirectionsWalk,
            title = "轻度活动",
            description = "每周运动 1-3 次，或日常步行较多",
            factor = "1.375",
            isSelected = selectedLevel == ActivityLevel.LIGHT,
            onClick = { onLevelSelected(ActivityLevel.LIGHT) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ActivityLevelCard(
            icon = Icons.Rounded.DirectionsRun,
            title = "中度活动",
            description = "每周运动 3-5 次，每次 30 分钟以上",
            factor = "1.55",
            isSelected = selectedLevel == ActivityLevel.MODERATE,
            onClick = { onLevelSelected(ActivityLevel.MODERATE) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ActivityLevelCard(
            icon = Icons.Rounded.FitnessCenter,
            title = "高度活动",
            description = "每周运动 6-7 次，或从事体力劳动",
            factor = "1.725",
            isSelected = selectedLevel == ActivityLevel.ACTIVE,
            onClick = { onLevelSelected(ActivityLevel.ACTIVE) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ActivityLevelCard(
            icon = Icons.Rounded.SportsGymnastics,
            title = "专业运动",
            description = "每天高强度训练，或专业运动员",
            factor = "1.9",
            isSelected = selectedLevel == ActivityLevel.VERY_ACTIVE,
            onClick = { onLevelSelected(ActivityLevel.VERY_ACTIVE) }
        )
        
        // 选中后显示热量估算
        AnimatedVisibility(visible = selectedLevel != null) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                
                // 产品能力展示
                CapabilityHighlight(
                    icon = Icons.Rounded.AutoAwesome,
                    text = "结合您的活动量，AI 将动态调整每日营养目标"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = selectedLevel != null
        )
    }
}
```

#### 1.7 DietaryPreferencesStep - 饮食偏好

```kotlin
@Composable
fun DietaryPreferencesStep(
    dietType: DietType?,
    allergens: Set<Allergen>,
    onDietTypeSelected: (DietType) -> Unit,
    onAllergenToggled: (Allergen) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader(
            title = "您的饮食偏好",
            subtitle = "帮助我们为您推荐更合适的食物"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 饮食类型
        Text("饮食类型", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(12.dp))
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DietTypeChip(
                label = "无限制",
                isSelected = dietType == DietType.OMNIVORE,
                onClick = { onDietTypeSelected(DietType.OMNIVORE) }
            )
            DietTypeChip(
                label = "素食",
                isSelected = dietType == DietType.VEGETARIAN,
                onClick = { onDietTypeSelected(DietType.VEGETARIAN) }
            )
            DietTypeChip(
                label = "纯素",
                isSelected = dietType == DietType.VEGAN,
                onClick = { onDietTypeSelected(DietType.VEGAN) }
            )
            DietTypeChip(
                label = "低碳水",
                isSelected = dietType == DietType.LOW_CARB,
                onClick = { onDietTypeSelected(DietType.LOW_CARB) }
            )
            DietTypeChip(
                label = "生酮",
                isSelected = dietType == DietType.KETO,
                onClick = { onDietTypeSelected(DietType.KETO) }
            )
            DietTypeChip(
                label = "地中海饮食",
                isSelected = dietType == DietType.MEDITERRANEAN,
                onClick = { onDietTypeSelected(DietType.MEDITERRANEAN) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 过敏原
        Text("食物过敏（可多选）", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(12.dp))
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AllergenChip(
                label = "无过敏",
                isSelected = allergens.isEmpty(),
                onClick = { /* Clear all allergens */ }
            )
            AllergenChip(
                label = "麸质",
                isSelected = Allergen.GLUTEN in allergens,
                onClick = { onAllergenToggled(Allergen.GLUTEN) }
            )
            AllergenChip(
                label = "乳制品",
                isSelected = Allergen.DAIRY in allergens,
                onClick = { onAllergenToggled(Allergen.DAIRY) }
            )
            AllergenChip(
                label = "坚果",
                isSelected = Allergen.NUTS in allergens,
                onClick = { onAllergenToggled(Allergen.NUTS) }
            )
            AllergenChip(
                label = "海鲜",
                isSelected = Allergen.SHELLFISH in allergens,
                onClick = { onAllergenToggled(Allergen.SHELLFISH) }
            )
            AllergenChip(
                label = "鸡蛋",
                isSelected = Allergen.EGGS in allergens,
                onClick = { onAllergenToggled(Allergen.EGGS) }
            )
            AllergenChip(
                label = "大豆",
                isSelected = Allergen.SOY in allergens,
                onClick = { onAllergenToggled(Allergen.SOY) }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 产品能力展示
        CapabilityHighlight(
            icon = Icons.Rounded.VerifiedUser,
            text = "我们整合了中国疾控中心等权威数据，为您提供最适合的饮食建议"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = dietType != null
        )
    }
}
```

#### 1.8 SummaryStep - 个性化计划展示

```kotlin
@Composable
fun SummaryStep(
    profile: EnhancedProfile,
    nutritionGoals: NutritionGoals,
    estimatedGoalDate: LocalDate?,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 庆祝动画
        LottieAnimation(
            composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.celebration)),
            modifier = Modifier.size(120.dp)
        )
        
        Text(
            text = "您的专属营养计划已生成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 每日热量目标卡片
        CalorieTargetCard(
            targetCalories = nutritionGoals.dailyCalories,
            bmr = nutritionGoals.bmr,
            activityCalories = nutritionGoals.activityCalories,
            goalAdjustment = nutritionGoals.goalAdjustment
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 宏量营养素目标
        MacroTargetsCard(
            protein = nutritionGoals.proteinGrams,
            carbs = nutritionGoals.carbsGrams,
            fat = nutritionGoals.fatGrams
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 目标达成日期
        if (estimatedGoalDate != null) {
            GoalDateCard(
                targetDate = estimatedGoalDate,
                healthGoal = profile.healthGoal
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // 核心价值展示
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "让 AI 成为您的私人营养师",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "戴上眼镜，开始您的健康之旅\n每一餐都吃得明白",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 开始使用按钮
        PrimaryButton(
            text = "开始使用",
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text("返回修改")
        }
    }
}
```

### 2. 通用 UI 组件

#### 2.1 CapabilityHighlight - 产品能力展示

```kotlin
@Composable
fun CapabilityHighlight(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
```

#### 2.2 BMIIndicator - BMI 指示器

```kotlin
@Composable
fun BMIIndicator(bmi: Float) {
    val status = getBMIStatus(bmi)
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = status.color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "BMI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.1f", bmi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = status.color
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = status.color
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // BMI 范围条
            BMIRangeBar(currentBMI = bmi)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = status.suggestion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

#### 2.3 OnboardingProgressIndicator - 进度指示器

```kotlin
@Composable
fun OnboardingProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
            )
        }
    }
}
```


## Data Models

### 核心数据模型

```kotlin
/**
 * 增强版用户档案
 */
data class EnhancedProfile(
    val id: String = UUID.randomUUID().toString(),
    val nickname: String? = null,
    val gender: Gender,
    val birthDate: LocalDate,
    val height: Float,  // cm
    val weight: Float,  // kg
    val bmi: Float,
    val healthGoal: HealthGoal,
    val targetWeight: Float? = null,
    val targetDate: LocalDate? = null,
    val activityLevel: ActivityLevel,
    val dietType: DietType = DietType.OMNIVORE,
    val allergens: Set<Allergen> = emptySet(),
    val healthConditions: List<String> = emptyList(),
    val dietaryPreferences: List<String> = emptyList(),
    val isOnboardingCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val age: Int get() = calculateAge(birthDate)
}

/**
 * 健康目标
 */
enum class HealthGoal(val displayName: String) {
    LOSE_WEIGHT("减重瘦身"),
    MAINTAIN("维持健康"),
    GAIN_MUSCLE("增肌塑形")
}

/**
 * 性别
 */
enum class Gender(val displayName: String) {
    MALE("男"),
    FEMALE("女")
}

/**
 * 活动量等级
 */
enum class ActivityLevel(
    val displayName: String,
    val description: String,
    val factor: Float
) {
    SEDENTARY("久坐办公", "几乎不运动，长时间坐着工作", 1.2f),
    LIGHT("轻度活动", "每周运动 1-3 次，或日常步行较多", 1.375f),
    MODERATE("中度活动", "每周运动 3-5 次，每次 30 分钟以上", 1.55f),
    ACTIVE("高度活动", "每周运动 6-7 次，或从事体力劳动", 1.725f),
    VERY_ACTIVE("专业运动", "每天高强度训练，或专业运动员", 1.9f)
}

/**
 * 饮食类型
 */
enum class DietType(
    val displayName: String,
    val proteinRatio: Float,
    val carbsRatio: Float,
    val fatRatio: Float
) {
    OMNIVORE("无限制", 0.25f, 0.50f, 0.25f),
    VEGETARIAN("素食", 0.20f, 0.55f, 0.25f),
    VEGAN("纯素", 0.18f, 0.57f, 0.25f),
    LOW_CARB("低碳水", 0.30f, 0.30f, 0.40f),
    KETO("生酮", 0.25f, 0.05f, 0.70f),
    MEDITERRANEAN("地中海饮食", 0.20f, 0.45f, 0.35f)
}

/**
 * 过敏原
 */
enum class Allergen(val displayName: String) {
    GLUTEN("麸质"),
    DAIRY("乳制品"),
    NUTS("坚果"),
    SHELLFISH("海鲜"),
    EGGS("鸡蛋"),
    SOY("大豆")
}

/**
 * 营养目标
 */
data class NutritionGoals(
    val dailyCalories: Int,
    val bmr: Int,
    val activityCalories: Int,
    val goalAdjustment: Int,  // 正数为盈余，负数为缺口
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val proteinRatio: Float,
    val carbsRatio: Float,
    val fatRatio: Float,
    val isCustom: Boolean = false
)

/**
 * 体重记录
 */
data class WeightEntry(
    val id: String = UUID.randomUUID().toString(),
    val weight: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

/**
 * BMI 状态
 */
enum class BMIStatus(
    val label: String,
    val color: Color,
    val suggestion: String,
    val minBMI: Float,
    val maxBMI: Float
) {
    UNDERWEIGHT("偏瘦", Color(0xFF2196F3), "建议适当增加营养摄入", 0f, 18.5f),
    NORMAL("正常", Color(0xFF4CAF50), "继续保持健康饮食", 18.5f, 24f),
    OVERWEIGHT("偏胖", Color(0xFFFFC107), "建议控制热量摄入", 24f, 28f),
    OBESE("肥胖", Color(0xFFFF5722), "建议咨询营养师", 28f, Float.MAX_VALUE)
}
```

### 计算工具类

```kotlin
/**
 * 营养计算器
 */
object NutritionCalculator {
    
    /**
     * 计算 BMI
     * BMI = 体重(kg) / 身高(m)²
     */
    fun calculateBMI(heightCm: Float, weightKg: Float): Float {
        require(heightCm > 0) { "Height must be positive" }
        require(weightKg > 0) { "Weight must be positive" }
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }
    
    /**
     * 获取 BMI 状态
     */
    fun getBMIStatus(bmi: Float): BMIStatus = when {
        bmi < 18.5f -> BMIStatus.UNDERWEIGHT
        bmi < 24f -> BMIStatus.NORMAL
        bmi < 28f -> BMIStatus.OVERWEIGHT
        else -> BMIStatus.OBESE
    }
    
    /**
     * 计算基础代谢率 (BMR) - Mifflin-St Jeor 公式
     */
    fun calculateBMR(
        gender: Gender,
        weightKg: Float,
        heightCm: Float,
        age: Int
    ): Int {
        val bmr = if (gender == Gender.MALE) {
            10 * weightKg + 6.25 * heightCm - 5 * age + 5
        } else {
            10 * weightKg + 6.25 * heightCm - 5 * age - 161
        }
        return bmr.toInt()
    }
    
    /**
     * 计算每日总能量消耗 (TDEE)
     */
    fun calculateTDEE(bmr: Int, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.factor).toInt()
    }
    
    /**
     * 计算每日热量目标
     */
    fun calculateDailyCalories(
        tdee: Int,
        healthGoal: HealthGoal
    ): Pair<Int, Int> {  // (calories, adjustment)
        return when (healthGoal) {
            HealthGoal.LOSE_WEIGHT -> {
                val deficit = 400  // 400 kcal 缺口
                Pair(tdee - deficit, -deficit)
            }
            HealthGoal.GAIN_MUSCLE -> {
                val surplus = 250  // 250 kcal 盈余
                Pair(tdee + surplus, surplus)
            }
            HealthGoal.MAINTAIN -> Pair(tdee, 0)
        }
    }
    
    /**
     * 计算宏量营养素目标
     */
    fun calculateMacros(
        dailyCalories: Int,
        dietType: DietType
    ): Triple<Int, Int, Int> {  // (protein, carbs, fat) in grams
        val proteinCalories = dailyCalories * dietType.proteinRatio
        val carbsCalories = dailyCalories * dietType.carbsRatio
        val fatCalories = dailyCalories * dietType.fatRatio
        
        // 蛋白质和碳水: 4 kcal/g, 脂肪: 9 kcal/g
        val proteinGrams = (proteinCalories / 4).toInt()
        val carbsGrams = (carbsCalories / 4).toInt()
        val fatGrams = (fatCalories / 9).toInt()
        
        return Triple(proteinGrams, carbsGrams, fatGrams)
    }
    
    /**
     * 计算完整的营养目标
     */
    fun calculateNutritionGoals(profile: EnhancedProfile): NutritionGoals {
        val bmr = calculateBMR(profile.gender, profile.weight, profile.height, profile.age)
        val tdee = calculateTDEE(bmr, profile.activityLevel)
        val (dailyCalories, adjustment) = calculateDailyCalories(tdee, profile.healthGoal)
        val (protein, carbs, fat) = calculateMacros(dailyCalories, profile.dietType)
        
        return NutritionGoals(
            dailyCalories = dailyCalories,
            bmr = bmr,
            activityCalories = tdee - bmr,
            goalAdjustment = adjustment,
            proteinGrams = protein,
            carbsGrams = carbs,
            fatGrams = fat,
            proteinRatio = profile.dietType.proteinRatio,
            carbsRatio = profile.dietType.carbsRatio,
            fatRatio = profile.dietType.fatRatio
        )
    }
    
    /**
     * 计算每周体重变化率
     */
    fun calculateWeeklyRate(
        currentWeight: Float,
        targetWeight: Float,
        targetDate: LocalDate
    ): Float {
        val weightDiff = abs(currentWeight - targetWeight)
        val daysUntilTarget = ChronoUnit.DAYS.between(LocalDate.now(), targetDate)
        val weeksUntilTarget = daysUntilTarget / 7f
        return if (weeksUntilTarget > 0) weightDiff / weeksUntilTarget else 0f
    }
    
    /**
     * 检查每周变化率是否安全
     */
    fun isWeeklyRateSafe(weeklyRate: Float, healthGoal: HealthGoal): Boolean {
        return when (healthGoal) {
            HealthGoal.LOSE_WEIGHT -> weeklyRate <= 1.0f  // 每周最多减 1kg
            HealthGoal.GAIN_MUSCLE -> weeklyRate <= 0.5f  // 每周最多增 0.5kg
            HealthGoal.MAINTAIN -> true
        }
    }
    
    /**
     * 计算年龄
     */
    fun calculateAge(birthDate: LocalDate): Int {
        return Period.between(birthDate, LocalDate.now()).years
    }
    
    /**
     * 验证宏量比例总和
     */
    fun validateMacroRatios(protein: Float, carbs: Float, fat: Float): Boolean {
        val total = protein + carbs + fat
        return abs(total - 1.0f) < 0.01f  // 允许 1% 误差
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: BMI Calculation Correctness

*For any* valid height (140-220 cm) and weight (30-150 kg), the calculated BMI SHALL equal weight / (height/100)² with precision to one decimal place.

**Validates: Requirements 3.6, 8.3**

### Property 2: BMI Status Mapping

*For any* BMI value, the status returned by `getBMIStatus()` SHALL match the defined thresholds: UNDERWEIGHT for BMI < 18.5, NORMAL for 18.5 ≤ BMI < 24, OVERWEIGHT for 24 ≤ BMI < 28, OBESE for BMI ≥ 28.

**Validates: Requirements 3.6**

### Property 3: Age Calculation from Birthdate

*For any* valid birthdate, the calculated age SHALL equal the number of complete years between the birthdate and today.

**Validates: Requirements 3.3**

### Property 4: BMR Calculation (Mifflin-St Jeor)

*For any* valid profile (gender, weight, height, age), the BMR SHALL be calculated as:
- Male: 10 × weight + 6.25 × height - 5 × age + 5
- Female: 10 × weight + 6.25 × height - 5 × age - 161

**Validates: Requirements 9.1**

### Property 5: TDEE Calculation

*For any* BMR and activity level, the TDEE SHALL equal BMR × activity factor, where factors are: SEDENTARY=1.2, LIGHT=1.375, MODERATE=1.55, ACTIVE=1.725, VERY_ACTIVE=1.9.

**Validates: Requirements 9.2**

### Property 6: Calorie Target Based on Goal

*For any* TDEE and health goal:
- LOSE_WEIGHT → target = TDEE - 400
- GAIN_MUSCLE → target = TDEE + 250
- MAINTAIN → target = TDEE

**Validates: Requirements 9.3, 9.4**

### Property 7: Macro Calculation from Calories

*For any* daily calorie target and diet type, the macro grams SHALL be calculated as:
- Protein = (calories × proteinRatio) / 4
- Carbs = (calories × carbsRatio) / 4
- Fat = (calories × fatRatio) / 9

**Validates: Requirements 9.5**

### Property 8: Weekly Rate Calculation

*For any* current weight, target weight, and target date, the weekly rate SHALL equal |currentWeight - targetWeight| / weeksUntilTarget.

**Validates: Requirements 4.6**

### Property 9: Weekly Rate Safety Check

*For any* weekly rate and health goal:
- LOSE_WEIGHT: safe if rate ≤ 1.0 kg/week
- GAIN_MUSCLE: safe if rate ≤ 0.5 kg/week
- MAINTAIN: always safe

**Validates: Requirements 4.7**

### Property 10: Macro Ratio Sum Validation

*For any* set of macro ratios (protein, carbs, fat), the validation SHALL pass if and only if the sum equals 1.0 (within 1% tolerance).

**Validates: Requirements 10.3, 10.4**

### Property 11: Onboarding State Preservation

*For any* onboarding step with entered data, navigating back and then forward SHALL preserve all previously entered data.

**Validates: Requirements 1.4**

### Property 12: Progress Indicator Accuracy

*For any* onboarding step N of total T steps, the progress indicator SHALL show N completed segments and T-N incomplete segments.

**Validates: Requirements 1.2**

## Error Handling

### 输入验证

| 字段 | 验证规则 | 错误消息 |
|------|----------|----------|
| 身高 | 140-220 cm | "请输入有效身高（140-220cm）" |
| 体重 | 30-150 kg | "请输入有效体重（30-150kg）" |
| 出生日期 | 年龄 10-100 岁 | "请输入有效出生日期" |
| 目标体重 | 与当前体重差 ≤ 50kg | "目标体重设置不合理" |
| 目标日期 | ≥ 2 周后 | "目标日期至少需要 2 周后" |
| 宏量比例 | 总和 = 100% | "营养比例总和必须为 100%" |

### 网络错误处理

```kotlin
sealed class ProfileSyncResult {
    object Success : ProfileSyncResult()
    data class Error(val message: String) : ProfileSyncResult()
    object Offline : ProfileSyncResult()
}

// 离线时队列保存
class OfflineProfileQueue {
    fun queueProfileUpdate(profile: EnhancedProfile)
    fun processQueueWhenOnline()
}
```

### 默认值策略

| 场景 | 默认值 |
|------|--------|
| 跳过引导 | 性别=男, 年龄=25, 身高=170, 体重=65, 活动量=中度, 目标=维持 |
| 未设置饮食类型 | 无限制 (OMNIVORE) |
| 未设置过敏原 | 空集合 |

## Testing Strategy

### 单元测试

1. **BMI 计算测试**
   - 测试正常范围值
   - 测试边界值（最小/最大身高体重）
   - 测试 BMI 状态分类边界

2. **营养目标计算测试**
   - 测试 BMR 计算（男/女）
   - 测试 TDEE 计算（各活动等级）
   - 测试热量目标（各健康目标）
   - 测试宏量计算（各饮食类型）

3. **体重目标验证测试**
   - 测试每周速率计算
   - 测试安全速率检查

### 属性测试 (Property-Based Testing)

使用 Kotest 的 Property-Based Testing：

```kotlin
class NutritionCalculatorPropertyTest : FunSpec({
    
    // Property 1: BMI Calculation
    test("BMI calculation is correct for all valid inputs") {
        checkAll(
            Arb.float(140f..220f),  // height
            Arb.float(30f..150f)    // weight
        ) { height, weight ->
            val bmi = NutritionCalculator.calculateBMI(height, weight)
            val expected = weight / ((height / 100f).pow(2))
            bmi shouldBe (expected plusOrMinus 0.1f)
        }
    }
    
    // Property 2: BMI Status Mapping
    test("BMI status mapping follows defined thresholds") {
        checkAll(Arb.float(10f..50f)) { bmi ->
            val status = NutritionCalculator.getBMIStatus(bmi)
            when {
                bmi < 18.5f -> status shouldBe BMIStatus.UNDERWEIGHT
                bmi < 24f -> status shouldBe BMIStatus.NORMAL
                bmi < 28f -> status shouldBe BMIStatus.OVERWEIGHT
                else -> status shouldBe BMIStatus.OBESE
            }
        }
    }
    
    // Property 10: Macro Ratio Validation
    test("Macro ratio validation passes only when sum is 1.0") {
        checkAll(
            Arb.float(0f..1f),
            Arb.float(0f..1f),
            Arb.float(0f..1f)
        ) { p, c, f ->
            val isValid = NutritionCalculator.validateMacroRatios(p, c, f)
            val sum = p + c + f
            isValid shouldBe (abs(sum - 1.0f) < 0.01f)
        }
    }
})
```

### UI 测试

1. **引导流程测试**
   - 测试步骤导航（前进/后退）
   - 测试数据保持
   - 测试跳过功能

2. **表单验证测试**
   - 测试必填字段验证
   - 测试输入范围验证
   - 测试错误消息显示

### 集成测试

1. **数据持久化测试**
   - 测试本地保存
   - 测试后端同步
   - 测试离线队列

2. **端到端流程测试**
   - 完整引导流程
   - 档案编辑流程
   - 体重记录流程
