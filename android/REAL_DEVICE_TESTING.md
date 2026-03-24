# VISEAT 食智 - 眼镜端真机测试指南

## 📱 按键交互设计

为避免与 Rokid 系统按键冲突，本应用使用以下交互方案：

### 当前方案：音量键控制

| 按键 | 操作 | 功能 |
|------|------|------|
| **音量+ 短按** | < 800ms | 拍照 |
| **音量+ 长按** | ≥ 800ms | 开始/结束用餐 |
| **音量- 短按** | < 800ms | 重复播报上次结果 |
| **音量- 长按** | ≥ 800ms | 取消当前操作 |
| **触控板点击** | - | 拍照 |
| **触控板左滑** | - | 显示上次识别结果（5秒后自动关闭，再次左滑关闭） |
| **触控板右滑** | - | 用餐中：直接结束用餐 |
| **相机键** | - | 拍照（如有独立相机键）|

### 如果仍有冲突

在 Rokid 系统设置中：
```
设置 → 按键设置 → 关闭系统级按键响应
```

---

## 🔧 启用真实 SDK

在 Rokid 设备上编译前，需要修改两个文件：

### 1. BluetoothSender.kt

```kotlin
// 文件: app/src/main/kotlin/com/rokid/nutrition/bluetooth/BluetoothSender.kt

// 步骤 1: 取消 import 注释
import com.rokid.cxr.service.bridge.CXRServiceBridge
import com.rokid.cxr.service.bridge.Caps

// 步骤 2: 将 USE_REAL_SDK 改为 true
private const val USE_REAL_SDK = true

// 步骤 3: 取消 cxrBridge 实例化注释
private val cxrBridge = CXRServiceBridge()

// 步骤 4: 取消 initialize()、sendImage()、sendCommand() 中的代码块注释
```

### 2. BluetoothReceiver.kt

```kotlin
// 文件: app/src/main/kotlin/com/rokid/nutrition/bluetooth/BluetoothReceiver.kt

// 步骤 1: 取消 import 注释
import com.rokid.cxr.service.bridge.CXRServiceBridge
import com.rokid.cxr.service.bridge.Caps

// 步骤 2: 将 USE_REAL_SDK 改为 true
private const val USE_REAL_SDK = true

// 步骤 3: 取消 cxrBridge 实例化注释
private val cxrBridge = CXRServiceBridge()

// 步骤 4: 取消 initialize()、release() 中的代码块注释
```

---

## 📦 编译和安装

### 在 Rokid 设备上编译

```bash
cd android
./gradlew assembleDebug
```

### 安装到眼镜

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 启动应用

```bash
adb shell am start -n com.rokid.nutrition/.MainActivity
```

---

## 🔗 蓝牙配对

1. 确保手机端安装了配套 APP（使用 CXR-M SDK）
2. 在 Rokid 系统设置中配对手机
3. 启动眼镜端应用，等待 "已连接到手机" 提示

---

## 🧪 测试流程

### 基本功能测试

1. **连接测试**
   - 启动应用，检查是否显示 "已连接: [手机名称]"
   - TTS 应播报 "已连接到手机"

2. **拍照测试**
   - 短按音量+，检查是否拍照并发送
   - 状态应显示 "发送中..." → "等待识别结果..."

3. **用餐流程测试**
   - 长按音量+ 开始用餐
   - TTS 应播报 "开始用餐，正在拍照"
   - 等待 5 分钟，检查自动拍照是否触发
   - 长按音量+ 结束用餐

### 三阶段测试

| 阶段 | 操作 | 预期结果 |
|------|------|----------|
| 拍照识别 | 短按音量+ | 取景框动画 → 识别菜品中 → 热量计算中 → 营养分析中 → 显示结果 |
| 开始用餐 | 长按音量+ | TTS: "开始用餐记录"（需先识别到餐品） |
| 监测 | 等待 5 分钟 | 自动拍照，更新营养数据 |
| 结束 | 长按音量+ | TTS: "用餐结束"，显示总摄入 |

### 新增功能测试

| 功能 | 预期行为 |
|------|----------|
| **品牌标题** | 初始界面显示 "VISEAT" 大字 + "食智" 小字 |
| **取景动画** | 拍照时显示四角标记 + 扫描线动画 |
| **科技感处理** | 依次显示：🍽️识别菜品 → 🔥热量计算 → 🧪营养分析 |
| **进度条** | 4段式进度条，随处理阶段递进 |
| **非餐品警告** | 未识别到餐品时显示 ⚠️ + "请重新拍照" |
| **10秒熄屏** | 无操作10秒后允许系统熄屏，任意按键唤醒 |
| **用餐限制** | 必须先识别到餐品才能开始用餐记录 |

---

## 🐛 调试

### 查看日志

```bash
adb logcat | grep -E "(CXRManager|CXRReceiver|MainActivity)"
```

### 常见问题

1. **"未连接手机"**
   - 检查蓝牙配对状态
   - 确保手机端 APP 已启动

2. **按键无响应**
   - 检查 Rokid 系统按键设置
   - 尝试使用触控板点击

3. **SDK 编译错误**
   - 确保在 Rokid 设备上编译
   - 检查 SDK 依赖是否正确配置

---

## 📝 注意事项

- 开发环境（Mac/PC）使用模拟模式，无需真实 SDK
- 真机测试前必须取消注释并启用 SDK
- 手机端 APP 需要同步开发（使用 CXR-M SDK）
