@file:OptIn(ExperimentalMaterial3Api::class)

package com.rokid.nutrition.phone.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.domain.*
import com.rokid.nutrition.phone.ui.component.*
import com.rokid.nutrition.phone.ui.component.HeightWheelPicker
import com.rokid.nutrition.phone.ui.component.WeightWheelPicker
import com.rokid.nutrition.phone.ui.component.DateWheelPicker
import com.rokid.nutrition.phone.ui.theme.*
import com.rokid.nutrition.phone.ui.viewmodel.OnboardingState
import com.rokid.nutrition.phone.ui.viewmodel.OnboardingStep
import java.time.LocalDate

/**
 * Onboarding 主屏幕
 */
@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onStartOnboarding: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSetNickname: (String) -> Unit,
    onSetHealthGoal: (HealthGoal) -> Unit,
    onSetGender: (Gender) -> Unit,
    onSetBirthDate: (Int, Int, Int) -> Unit,
    onSetHeight: (Float) -> Unit,
    onSetWeight: (Float) -> Unit,
    onSetTargetWeight: (Float) -> Unit,
    onSetTargetDate: (LocalDate) -> Unit,
    onSetActivityLevel: (ActivityLevel) -> Unit,
    onSetDietType: (DietType) -> Unit,
    onToggleAllergen: (Allergen) -> Unit,
    onClearAllergens: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 进度指示器（欢迎页不显示）
            if (state.currentStep != OnboardingStep.WELCOME) {
                OnboardingProgressIndicator(
                    currentStep = state.currentStepIndex - 1,
                    totalSteps = state.totalSteps
                )
            }
            
            // 步骤内容
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        nickname = state.nickname,
                        onNicknameChange = onSetNickname,
                        onStart = onStartOnboarding
                    )
                    OnboardingStep.GOAL -> GoalSelectionStep(
                        selectedGoal = state.healthGoal,
                        onGoalSelected = onSetHealthGoal,
                        onNext = onNextStep,
                        onBack = onPreviousStep
                    )
                    OnboardingStep.BODY_DATA -> BodyDataStep(
                        gender = state.gender,
                        birthYear = state.birthYear,
                        birthMonth = state.birthMonth,
                        birthDay = state.birthDay,
                        height = state.height,
                        weight = state.weight,
                        bmi = state.bmi,
                        age = state.age,
                        onGenderSelected = onSetGender,
                        onBirthDateChanged = onSetBirthDate,
                        onHeightChanged = onSetHeight,
                        onWeightChanged = onSetWeight,
                        onNext = onNextStep,
                        onBack = onPreviousStep
                    )
                    OnboardingStep.TARGET_WEIGHT -> state.healthGoal?.let { goal ->
                        TargetWeightStep(
                            currentWeight = state.weight,
                            healthGoal = goal,
                            targetWeight = state.targetWeight,
                            targetDate = state.targetDate,
                            weeklyRate = state.weeklyRate,
                            isWeeklyRateSafe = state.isWeeklyRateSafe,
                            onTargetWeightChanged = onSetTargetWeight,
                            onTargetDateSelected = onSetTargetDate,
                            onNext = onNextStep,
                            onBack = onPreviousStep
                        )
                    }
                    OnboardingStep.ACTIVITY -> ActivityLevelStep(
                        selectedLevel = state.activityLevel,
                        onLevelSelected = onSetActivityLevel,
                        onNext = onNextStep,
                        onBack = onPreviousStep
                    )
                    OnboardingStep.DIETARY -> DietaryPreferencesStep(
                        dietType = state.dietType,
                        allergens = state.allergens,
                        onDietTypeSelected = onSetDietType,
                        onAllergenToggled = onToggleAllergen,
                        onClearAllergens = onClearAllergens,
                        onNext = onNextStep,
                        onBack = onPreviousStep
                    )
                    OnboardingStep.SUMMARY -> SummaryStep(
                        state = state,
                        onConfirm = onComplete,
                        onBack = onPreviousStep
                    )
                }
            }
        }
        
        // 跳过按钮
        if (state.currentStep != OnboardingStep.WELCOME && state.currentStep != OnboardingStep.SUMMARY) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("跳过", color = AppleGray1)
            }
        }
        
        // 加载指示器
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppleTeal)
            }
        }
    }
}


// ==================== 欢迎页 ====================

@Composable
private fun WelcomeStep(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo with a glow effect
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = AppleTeal.copy(alpha = 0.1f),
                modifier = Modifier.size(140.dp)
            ) {}
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = AppleTeal,
                modifier = Modifier.size(100.dp),
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "欢迎使用 VisEat",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "让 AI 成为您的私人营养师",
            style = MaterialTheme.typography.headlineSmall,
            color = AppleTeal,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "每一餐都吃得明白，每个人都活得健康",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 昵称输入
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "我们该如何称呼您？",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChange,
                    placeholder = { Text("输入您的昵称（可选）", color = AppleGray2) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleTeal,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppleTeal),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("开启健康之旅", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

// ==================== 目标选择 ====================

@Composable
private fun GoalSelectionStep(
    selectedGoal: HealthGoal?,
    onGoalSelected: (HealthGoal) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        StepHeader(
            title = "您的健康目标是什么？",
            subtitle = "选择一个最符合您当前需求的目标"
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
        
        AnimatedVisibility(visible = selectedGoal != null) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                
                val message = when (selectedGoal) {
                    HealthGoal.LOSE_WEIGHT -> "我们将帮助您科学减重，每周建议减少 0.5-1kg"
                    HealthGoal.MAINTAIN -> "我们将帮助您保持均衡饮食，维持理想状态"
                    HealthGoal.GAIN_MUSCLE -> "我们将帮助您合理增肌，确保营养充足"
                    null -> ""
                }
                MotivationalMessage(message = message)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CapabilityHighlight(
                    icon = Icons.Rounded.Psychology,
                    text = "VisEat 的 AI 将根据您的目标，为每一餐提供个性化建议"
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = selectedGoal != null
        )
    }
}


// ==================== 身体数据 ====================

@Composable
private fun BodyDataStep(
    gender: Gender?,
    birthYear: Int,
    birthMonth: Int,
    birthDay: Int,
    height: Float,
    weight: Float,
    bmi: Float?,
    age: Int?,
    onGenderSelected: (Gender) -> Unit,
    onBirthDateChanged: (Int, Int, Int) -> Unit,
    onHeightChanged: (Float) -> Unit,
    onWeightChanged: (Float) -> Unit,
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
            title = "告诉我们一些基本信息",
            subtitle = "这些信息将帮助我们计算您的营养需求"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 性别选择
        Text("您的性别", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GenderCard(
                icon = Icons.Rounded.Male,
                label = "男",
                isSelected = gender == Gender.MALE,
                onClick = { onGenderSelected(Gender.MALE) },
                modifier = Modifier.weight(1f),
                isMale = true
            )
            GenderCard(
                icon = Icons.Rounded.Female,
                label = "女",
                isSelected = gender == Gender.FEMALE,
                onClick = { onGenderSelected(Gender.FEMALE) },
                modifier = Modifier.weight(1f),
                isMale = false
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 年龄选择
        Text("您的年龄", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val currentAge = age ?: (currentYear - birthYear)
                
                Row(verticalAlignment = Alignment.Bottom) {
                    // 移除这里的文本显示，因为 AgeWheelPicker 内部自带了显示和单位
                }
                AgeWheelPicker(
                    selectedAge = currentAge,
                    onAgeChange = { newAge ->
                        val newYear = currentYear - newAge
                        onBirthDateChanged(newYear, birthMonth, birthDay)
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 身高选择
        Text("您的身高", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeightWheelPicker(
                    selectedHeight = height,
                    onHeightChange = onHeightChanged,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 体重选择
        Text("您的体重", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WeightWheelPicker(
                    selectedWeight = weight,
                    onWeightChange = onWeightChanged,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        // BMI 显示
        AnimatedVisibility(visible = bmi != null) {
            Column {
                Spacer(modifier = Modifier.height(24.dp))
                bmi?.let { BMIIndicator(bmi = it) }

                Spacer(modifier = Modifier.height(16.dp))
                
                CapabilityHighlight(
                    icon = Icons.Rounded.Storage,
                    text = "基于 6,300+ 食物数据库，我们能精准计算您的营养需求"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = gender != null && height > 0 && weight > 0
        )
    }
}

// ==================== 目标体重 ====================

@Composable
private fun TargetWeightStep(
    currentWeight: Float,
    healthGoal: HealthGoal,
    targetWeight: Float?,
    targetDate: LocalDate?,
    weeklyRate: Float?,
    isWeeklyRateSafe: Boolean,
    onTargetWeightChanged: (Float) -> Unit,
    onTargetDateSelected: (LocalDate) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val isLoseWeight = healthGoal == HealthGoal.LOSE_WEIGHT
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader(
            title = if (isLoseWeight) "您的目标体重" else "您的增肌目标",
            subtitle = "设定一个合理的目标，我们会帮您制定计划"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 当前体重
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AppleGray5
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("当前体重", color = AppleGray1)
                Text("${currentWeight.toInt()} kg", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 目标体重选择
        val tw = targetWeight ?: currentWeight
        Text(if (isLoseWeight) "目标体重" else "增肌目标", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 体重差
                val diff = kotlin.math.abs(currentWeight - tw)
                if (diff > 0.1f) {
                    Text(
                        text = "目标：${if (isLoseWeight) "减" else "增"} ${String.format("%.1f", diff)} kg",
                        style = MaterialTheme.typography.titleSmall,
                        color = AppleTeal.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                TargetWeightWheelPicker(
                    currentWeight = currentWeight,
                    selectedTargetWeight = tw,
                    onTargetWeightChange = onTargetWeightChanged,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 目标日期选择
        Text("计划达成日期", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        
        val minDate = LocalDate.now().plusWeeks(2)
        val presetDates = listOf(
            "3个月后" to LocalDate.now().plusMonths(3),
            "6个月后" to LocalDate.now().plusMonths(6),
            "1年后" to LocalDate.now().plusYears(1)
        )
        
        // 判断是否为自定义日期（不在预设选项中）
        val isCustomDate = targetDate != null && presetDates.none { it.second == targetDate }
        var showDatePicker by remember { mutableStateOf(false) }
        
        // 预设选项 + 自定时间
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presetDates.forEach { (label, date) ->
                    FilterChip(
                        onClick = { onTargetDateSelected(date) },
                        label = { Text(label, fontSize = 12.sp) },
                        selected = targetDate == date
                    )
                }
            }
            
            // 自定时间选项
            FilterChip(
                onClick = { showDatePicker = true },
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isCustomDate && targetDate != null) {
                                "${targetDate.year}年${targetDate.monthValue}月${targetDate.dayOfMonth}日"
                            } else {
                                "自定时间"
                            },
                            fontSize = 12.sp
                        )
                    }
                },
                selected = isCustomDate,
                leadingIcon = if (isCustomDate) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        
        // 日期选择器弹窗
        if (showDatePicker) {
            CustomDatePickerDialog(
                initialDate = targetDate ?: LocalDate.now().plusMonths(3),
                minDate = minDate,
                onDateSelected = { date ->
                    onTargetDateSelected(date)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
        
        // 每周速率
        AnimatedVisibility(visible = weeklyRate != null && weeklyRate > 0) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isWeeklyRateSafe) AppleTeal.copy(alpha = 0.1f) else AppleOrange.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("每周约${if (isLoseWeight) "减" else "增"}")
                        Text(
                            "${String.format("%.1f", weeklyRate)} kg",
                            fontWeight = FontWeight.Bold,
                            color = if (isWeeklyRateSafe) AppleTeal else AppleOrange
                        )
                    }
                }
                
                if (!isWeeklyRateSafe) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyWarning(
                        message = if (isLoseWeight) {
                            "每周减重超过 1kg 可能不健康，建议延长目标日期"
                        } else {
                            "每周增重超过 0.5kg 可能导致脂肪堆积，建议延长目标日期"
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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


// ==================== 活动量 ====================

@Composable
private fun ActivityLevelStep(
    selectedLevel: ActivityLevel?,
    onLevelSelected: (ActivityLevel) -> Unit,
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
            title = "您的日常活动量",
            subtitle = "这将帮助我们计算您每天消耗的热量"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ActivityLevel.entries.forEach { level ->
            ActivityLevelCard(
                icon = when (level) {
                    ActivityLevel.SEDENTARY -> Icons.Rounded.Chair
                    ActivityLevel.LIGHT -> Icons.Rounded.DirectionsWalk
                    ActivityLevel.MODERATE -> Icons.Rounded.DirectionsRun
                    ActivityLevel.ACTIVE -> Icons.Rounded.FitnessCenter
                    ActivityLevel.VERY_ACTIVE -> Icons.Rounded.SportsGymnastics
                },
                title = level.displayName,
                description = level.description,
                isSelected = selectedLevel == level,
                onClick = { onLevelSelected(level) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        AnimatedVisibility(visible = selectedLevel != null) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                CapabilityHighlight(
                    icon = Icons.Rounded.AutoAwesome,
                    text = "结合您的活动量，AI 将动态调整每日营养目标"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        NavigationButtons(
            onBack = onBack,
            onNext = onNext,
            nextEnabled = selectedLevel != null
        )
    }
}

// ==================== 饮食偏好 ====================

@Composable
private fun DietaryPreferencesStep(
    dietType: DietType,
    allergens: Set<Allergen>,
    onDietTypeSelected: (DietType) -> Unit,
    onAllergenToggled: (Allergen) -> Unit,
    onClearAllergens: () -> Unit,
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
        
        DietType.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { type ->
                    PreferenceChip(
                        onClick = { onDietTypeSelected(type) },
                        label = type.displayName,
                        isSelected = dietType == type,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 过敏原
        Text("食物过敏（可多选）", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreferenceChip(
                onClick = onClearAllergens,
                label = "无过敏",
                isSelected = allergens.isEmpty(),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(2f))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Allergen.entries.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { allergen ->
                    PreferenceChip(
                        onClick = { onAllergenToggled(allergen) },
                        label = allergen.displayName,
                        isSelected = allergen in allergens,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        CapabilityHighlight(
            icon = Icons.Rounded.VerifiedUser,
            text = "我们整合了中国疾控中心等权威数据，为您提供最适合的饮食建议"
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        NavigationButtons(
            onBack = onBack,
            onNext = onNext
        )
    }
}

// ==================== 总结页 ====================

@Composable
private fun SummaryStep(
    state: OnboardingState,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    // 动画状态
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // 入场动画
    val iconScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 200),
        label = "contentAlpha"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景装饰 - 渐变光晕
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppleTeal.copy(alpha = 0.08f),
                            AppleTeal.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // 成功图标 - 带动画和光晕
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(iconScale),
                contentAlignment = Alignment.Center
            ) {
                // 外层光晕
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppleTeal.copy(alpha = 0.2f),
                                    AppleTeal.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                // 中层圆环
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = AppleTeal.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 图标
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = AppleTeal,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // 标题 - 带淡入动画
            Column(
                modifier = Modifier.alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 您的专属计划已生成",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "基于您的身体数据为您智能定制",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppleGray1,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // 每日热量目标卡片 - 带设计感
            state.nutritionGoals?.let { goals ->
                // 热量主卡片
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = AppleTeal.copy(alpha = 0.15f),
                            spotColor = AppleTeal.copy(alpha = 0.1f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 热量环形进度指示
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 背景圆环
                            Canvas(modifier = Modifier.size(140.dp)) {
                                val strokeWidth = 12.dp.toPx()
                                val padding = 8.dp.toPx()
                                
                                drawCircle(
                                    color = AppleGray5,
                                    radius = size.minDimension / 2 - padding,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                                // 进度弧
                                drawArc(
                                    color = AppleTeal,
                                    startAngle = -90f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round
                                    ),
                                    topLeft = Offset(padding, padding),
                                    size = Size(
                                        size.width - padding * 2,
                                        size.height - padding * 2
                                    )
                                )
                            }
                            // 中心数字
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${goals.dailyCalories}",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "kcal/天",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppleGray1
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 分割线
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(1.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            AppleGray4,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // 宏量营养素
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MacroItemEnhanced(
                                label = "蛋白质",
                                value = "${goals.proteinGrams}",
                                unit = "g",
                                color = ProteinCyan
                            )
                            MacroItemEnhanced(
                                label = "碳水",
                                value = "${goals.carbsGrams}",
                                unit = "g",
                                color = CarbsAmber
                            )
                            MacroItemEnhanced(
                                label = "脂肪",
                                value = "${goals.fatGrams}",
                                unit = "g",
                                color = ApplePurple
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 提示文字
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(contentAlpha)
                        .background(
                            color = AppleTeal.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = AppleTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "您可以随时在设置中调整您的营养目标",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))
            
            // 主按钮 - 带渐变和阴影
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = AppleTeal.copy(alpha = 0.3f),
                        spotColor = AppleTeal.copy(alpha = 0.2f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
            ) {
                Icon(
                    Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始使用", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(onClick = onBack) {
                Text(
                    "返回修改",
                    color = AppleGray1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MacroItemEnhanced(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 小圆点指示器
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = AppleGray1,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppleGray1
        )
    }
}

@Composable
private fun PremiumMacroItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = PremiumColors.Ink,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PremiumColors.InkMuted,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun MacroItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppleGray1
        )
    }
}

// ==================== 自定义日期选择器 ====================

@Composable
private fun CustomDatePickerDialog(
    initialDate: LocalDate,
    minDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedDay by remember { mutableIntStateOf(initialDate.dayOfMonth) }
    
    // 计算有效的日期范围
    val currentYear = LocalDate.now().year
    val maxYear = currentYear + 5
    val years = (minDate.year..maxYear).toList()
    
    // 根据选中的年月计算可用的天数
    val daysInMonth = remember(selectedYear, selectedMonth) {
        LocalDate.of(selectedYear, selectedMonth, 1).lengthOfMonth()
    }
    
    // 确保日期有效
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "选择目标日期",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                // 日期预览
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppleTeal.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${selectedYear}年${selectedMonth}月${selectedDay}日",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppleTeal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 年月日选择器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 年份选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("年", style = MaterialTheme.typography.labelSmall, color = AppleGray1)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPickerColumn(
                            items = years,
                            selectedItem = selectedYear,
                            onItemSelected = { selectedYear = it },
                            suffix = ""
                        )
                    }
                    
                    // 月份选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("月", style = MaterialTheme.typography.labelSmall, color = AppleGray1)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPickerColumn(
                            items = (1..12).toList(),
                            selectedItem = selectedMonth,
                            onItemSelected = { selectedMonth = it },
                            suffix = ""
                        )
                    }
                    
                    // 日期选择
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("日", style = MaterialTheme.typography.labelSmall, color = AppleGray1)
                        Spacer(modifier = Modifier.height(4.dp))
                        NumberPickerColumn(
                            items = (1..daysInMonth).toList(),
                            selectedItem = selectedDay.coerceAtMost(daysInMonth),
                            onItemSelected = { selectedDay = it },
                            suffix = ""
                        )
                    }
                }
                
                // 最小日期提示
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "目标日期需要至少在2周后",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppleGray1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val selectedLocalDate = try {
                LocalDate.of(selectedYear, selectedMonth, selectedDay)
            } catch (e: Exception) {
                LocalDate.of(selectedYear, selectedMonth, 1)
            }
            val isValid = selectedLocalDate >= minDate
            
            Button(
                onClick = {
                    if (isValid) {
                        onDateSelected(selectedLocalDate)
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = AppleGray1)
            }
        }
    )
}

@Composable
private fun NumberPickerColumn(
    items: List<Int>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    suffix: String
) {
    val scrollState = rememberScrollState()
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    
    LaunchedEffect(selectedIndex) {
        scrollState.animateScrollTo(selectedIndex * 40)
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppleGray5,
        modifier = Modifier.width(70.dp)
    ) {
        Column(
            modifier = Modifier
                .height(120.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                Surface(
                    onClick = { onItemSelected(item) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) AppleTeal.copy(alpha = 0.15f) else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$item$suffix",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) AppleTeal else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
