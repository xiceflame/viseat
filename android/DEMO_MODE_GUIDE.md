# VISEAT AR眼镜界面演示模式

用于录制AR眼镜界面动画，支持**真实API分析**预设图片。

## 功能特点

- **纯黑背景** - 便于后期抠像（将黑色设为透明）
- **绿色UI** - 只使用 #40FF5E，与真实AR眼镜显示一致
- **点击控制** - 每次点击进入下一步，完全手动控制节奏
- **真实分析** - 使用后端API分析预设图片，结果真实可信
- **复用主程序UI** - 与真实界面100%一致

## 操作方式

**点击屏幕** 或 **按任意键** → 进入下一个步骤

右上角会显示当前步骤和状态（录制时可隐藏）

## 快速开始

### 1. 准备预设图片

将两张食物图片放到：
```
android/app/src/main/assets/demo/coke.jpg   # 可乐图片
android/app/src/main/assets/demo/chips.jpg  # 薯片图片
```

图片要求：
- 格式：JPEG
- 建议分辨率：1280x960 或更高
- 确保食物清晰可见

### 2. 运行演示模式

```bash
# 安装应用
cd android
./gradlew installDebug

# 启动演示模式
adb shell am start -n com.rokid.nutrition/.demo.DemoActivity
```

### 3. 演示流程（双图连续识别）

| 步骤 | 内容 | 控制方式 |
|------|------|----------|
| 1 | 开屏动画 | 点击 |
| 2 | 等待连接 | 点击 |
| 3 | 首页待机 | 点击 |
| **第一次识别：可乐** | | |
| 4 | 拍照取景[可乐] | 点击 |
| 5 | 预览[可乐] | **3秒后自动** |
| 6 | 分析[可乐] | **自动进行** |
| 7 | 结果[可乐] | 点击 |
| 8 | 返回待机 | 点击 |
| **第二次识别：薯片** | | |
| 9 | 拍照取景[薯片] | 点击 |
| 10 | 预览[薯片] | **3秒后自动** |
| 11 | 分析[薯片] | **自动进行** |
| 12 | 结果[薯片] | 点击 |
| 13 | 结束 | 点击 |

**注意**：分析过程中不能跳过，需等待API返回结果。

### 4. 录屏命令

```bash
# 录制视频（推荐分辨率 480x640）
adb shell screenrecord --size 480x640 /sdcard/demo.mp4

# 按 Ctrl+C 停止录制，然后拉取文件
adb pull /sdcard/demo.mp4 ./
```

## 自定义场景

编辑 `DemoScenario.kt` 中的配置：

```kotlin
val scenario = DemoScenario(
    // 开屏动画
    showSplash = true,
    splashDurationMs = 3000,  // 3秒
    
    // 空闲阶段
    idleDurationMs = 2000,    // 2秒
    
    // 拍照阶段
    capturingDurationMs = 1500,
    
    // 分析阶段（可添加多个步骤）
    processingPhases = listOf(
        ProcessingStep("正在上传...", 1500),
        ProcessingStep("识别食物中...", 2000),
        ProcessingStep("计算热量...", 1500),
        ProcessingStep("分析营养成分...", 1500)
    ),
    
    // 结果数据
    foodName = "红烧肉 · 米饭",
    calories = 650,
    protein = 25,
    carbs = 80,
    fat = 28,
    suggestion = "建议搭配蔬菜，营养更均衡",
    
    // 结果显示时长
    resultDurationMs = 5000,
    
    // 用餐监测（可选）
    showMealMonitoring = true,
    monitoringDurationMs = 10000,
    monitoringCalories = 650,
    
    // 用餐总结（可选）
    showMealSummary = true,
    summaryDurationMs = 8000,
    summaryTotalCalories = 850,
    summaryDurationMinutes = 15,
    summaryMessage = "本餐营养均衡，建议保持"
)
```

## 后期制作指南

演示模式使用**白色背景 + 绿色光晕**，模拟真实AR眼镜画面效果。

### Premiere Pro 抠像

1. 导入录制的视频
2. 应用效果 → 键控 → **颜色键（Color Key）**
3. 选择白色作为键控颜色
4. 调整容差，保留绿色光晕
5. 叠加到实拍画面

### After Effects 抠像

1. 导入视频
2. 使用 **Keylight** 效果，选择白色作为键控颜色
3. 或使用 **混合模式：正片叠底（Multiply）**
4. 调整边缘羽化保留光晕效果

### DaVinci Resolve 抠像

1. 导入视频到时间线
2. 打开 **Color** 页面
3. 使用 **Qualifier** 选择白色区域
4. 反转选区，应用透明
5. 调整边缘保留光晕

## 颜色规范

AR眼镜**只能使用一种颜色**，通过透明度区分层次：

```
主绿色:   #40FF5E  (RGB: 64, 255, 94)
演示背景: #FFFFFF  (白色，便于抠图)
真机背景: #000000  (纯黑)

透明度层次：
- 100% #FF40FF5E - 主要内容、按下状态
- 80%  #CC40FF5E - 选中状态、重要文字
- 40%  #6640FF5E - 常态、次要内容
- 20%  #3340FF5E - 提示、背景装饰
```

**禁止使用：**
- 其他颜色（红、黄、蓝等）
- 渐变效果
- 大面积高亮色块

## 分辨率说明

Rokid AR眼镜显示参数：
- **可视区域**: 480 × 640 像素
- **建议显示区域**: 480 × 400 像素（避开边缘）
- **安全边距**: 上下各 80dp

建议录制时使用 480×640 分辨率，与真实眼镜显示一致。

## 注意事项

1. **录制前关闭调试信息** - 在 `DemoActivity.kt` 中设置 `if (false)` 隐藏调试信息
2. **使用Android模拟器** - 设置分辨率为 480×640，与眼镜一致
3. **录制时确保屏幕常亮** - 演示模式已自动处理
4. **后期抠像时注意边缘** - 适当添加羽化避免硬边
