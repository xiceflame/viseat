package com.rokid.nutrition.demo

/**
 * 演示场景配置
 * 
 * 用于定义录制视频时的完整演示流程，包括：
 * - 每个阶段的时长（毫秒）
 * - 显示的数据（食物名、热量、营养等）
 * - 动画效果控制
 * 
 * 使用方法：
 * 1. 创建 DemoScenario 实例，配置所需数据
 * 2. 启动 DemoActivity
 * 3. 录屏捕获完整流程
 * 4. 后期将黑色背景抠除，叠加到实拍画面
 */
data class DemoScenario(
    // ===== 品牌展示阶段 =====
    val showSplash: Boolean = true,
    val splashDurationMs: Long = 3000,  // 开屏动画时长
    
    // ===== 连接阶段 =====
    val showConnecting: Boolean = false,  // 是否显示连接动画
    val connectingDurationMs: Long = 2000,
    
    // ===== 空闲阶段（显示品牌标题）=====
    val idleDurationMs: Long = 2000,  // 空闲等待时长
    
    // ===== 拍照阶段 =====
    val capturingDurationMs: Long = 1500,  // 取景框动画时长
    
    // ===== 全屏预览阶段（可选）=====
    val showFullscreenPreview: Boolean = false,
    val previewDurationMs: Long = 2000,
    
    // ===== 分析阶段 =====
    val processingPhases: List<ProcessingStep> = listOf(
        ProcessingStep("正在上传...", 1500),
        ProcessingStep("识别食物中...", 2000),
        ProcessingStep("计算热量...", 1500),
        ProcessingStep("分析营养成分...", 1500)
    ),
    
    // ===== 结果数据 =====
    val foodName: String = "红烧肉 · 米饭",
    val calories: Int = 650,
    val protein: Int = 25,
    val carbs: Int = 80,
    val fat: Int = 28,
    val suggestion: String = "建议搭配蔬菜，营养更均衡",
    
    // ===== 结果显示阶段 =====
    val resultDurationMs: Long = 5000,  // 结果展示时长
    
    // ===== 用餐监测阶段（可选）=====
    val showMealMonitoring: Boolean = false,
    val monitoringDurationMs: Long = 10000,  // 监测展示时长
    val monitoringCalories: Int = 650,  // 监测期间累计热量
    
    // ===== 用餐总结阶段（可选）=====
    val showMealSummary: Boolean = false,
    val summaryDurationMs: Long = 8000,
    val summaryTotalCalories: Int = 850,
    val summaryProtein: Int = 32,
    val summaryCarbs: Int = 95,
    val summaryFat: Int = 35,
    val summaryDurationMinutes: Int = 15,
    val summaryMessage: String = "本餐营养均衡，建议保持"
) {
    companion object {
        /**
         * 预设场景：快速识别演示
         * 适合展示基本的拍照→识别→结果流程
         */
        fun quickRecognition() = DemoScenario(
            splashDurationMs = 2000,
            idleDurationMs = 1500,
            capturingDurationMs = 1200,
            processingPhases = listOf(
                ProcessingStep("识别中...", 2500)
            ),
            resultDurationMs = 4000
        )
        
        /**
         * 预设场景：完整流程演示
         * 展示从启动到用餐结束的完整流程
         */
        fun fullDemo() = DemoScenario(
            showSplash = true,
            splashDurationMs = 3000,
            showConnecting = true,
            connectingDurationMs = 2000,
            idleDurationMs = 2000,
            capturingDurationMs = 1500,
            showFullscreenPreview = true,
            previewDurationMs = 2000,
            processingPhases = listOf(
                ProcessingStep("正在上传...", 1500),
                ProcessingStep("识别食物中...", 2000),
                ProcessingStep("计算热量...", 1500),
                ProcessingStep("分析营养成分...", 1500)
            ),
            resultDurationMs = 5000,
            showMealMonitoring = true,
            monitoringDurationMs = 8000,
            showMealSummary = true,
            summaryDurationMs = 6000
        )
        
        /**
         * 预设场景：分析过程详细展示
         * 着重展示AI分析的各个阶段
         */
        fun detailedAnalysis() = DemoScenario(
            showSplash = false,
            idleDurationMs = 1000,
            capturingDurationMs = 2000,
            processingPhases = listOf(
                ProcessingStep("正在上传图片...", 2000),
                ProcessingStep("AI 正在识别食物...", 3000),
                ProcessingStep("计算热量数据...", 2500),
                ProcessingStep("分析营养成分...", 2500),
                ProcessingStep("生成饮食建议...", 2000)
            ),
            resultDurationMs = 6000
        )
        
        /**
         * 预设场景：用餐监测演示
         * 展示用餐过程中的实时监测
         */
        fun mealSession() = DemoScenario(
            showSplash = false,
            idleDurationMs = 1000,
            capturingDurationMs = 1200,
            processingPhases = listOf(
                ProcessingStep("识别中...", 2000)
            ),
            resultDurationMs = 3000,
            showMealMonitoring = true,
            monitoringDurationMs = 15000,
            monitoringCalories = 850,
            showMealSummary = true,
            summaryDurationMs = 8000,
            summaryTotalCalories = 850,
            summaryDurationMinutes = 12
        )
    }
}

/**
 * 处理阶段步骤
 */
data class ProcessingStep(
    val message: String,  // 显示的状态文字
    val durationMs: Long  // 持续时长（毫秒）
)
