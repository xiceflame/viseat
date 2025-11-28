# Rokid 营养助手 - 眼镜端应用（瘦客户端）

> **版本**: 3.0.0 | **架构**: 瘦客户端 | **通信**: 蓝牙

## 📱 项目概述

眼镜端应用，直接运行在 Rokid AR 眼镜上。**不直接联网**，通过蓝牙与手机通信。

### 核心功能

| 功能 | 说明 |
|------|------|
| 📸 **拍照** | 侧键短按拍照，图片通过蓝牙发送给手机 |
| ⏱️ **5分钟定时器** | 自动拍照并发送（无感监测） |
| 👁️ **AR显示** | 接收手机返回的营养信息并叠加显示 |
| 🔊 **TTS播报** | 语音播报识别结果和建议 |

### 操作说明

| 操作 | 功能 |
|------|------|
| **侧键短按** | 拍照（主动拍照，重置5分钟计时器） |
| **侧键长按** | 开始/结束用餐 |

## 🛠️ 开发环境搭建

### 方式一：使用 Android Studio（推荐）

#### 1. 安装 Android Studio
```bash
# 使用 Homebrew 安装
brew install --cask android-studio

# 或者手动下载
# 访问: https://developer.android.com/studio
```

#### 2. 首次启动配置
1. 打开 Android Studio
2. 选择 "Standard" 安装类型
3. 等待 SDK、模拟器等组件下载完成
4. 配置 Android SDK 路径（通常在 `~/Library/Android/sdk`）

#### 3. 打开项目
```bash
# 在 Android Studio 中选择 "Open"
# 导航到: /Users/linjunjie/CascadeProjects/RokidAI/android
```

#### 4. 同步 Gradle
- Android Studio 会自动提示同步 Gradle
- 点击 "Sync Now" 等待依赖下载完成

#### 5. 运行应用
- 连接 Android 设备或启动模拟器
- 点击工具栏的 "Run" 按钮（绿色三角形）

### 方式二：命令行开发

#### 1. 安装 JDK 17
```bash
# 使用 Homebrew 安装
brew install openjdk@17

# 配置环境变量（添加到 ~/.zshrc）
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc

# 验证安装
java -version
```

#### 2. 安装 Android SDK Command Line Tools
```bash
# 创建 SDK 目录
mkdir -p ~/Library/Android/sdk

# 下载 Command Line Tools
cd ~/Library/Android/sdk
wget https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip
unzip commandlinetools-mac-9477386_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true

# 配置环境变量（添加到 ~/.zshrc）
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH' >> ~/.zshrc
echo 'export PATH=$ANDROID_HOME/platform-tools:$PATH' >> ~/.zshrc
echo 'export PATH=$ANDROID_HOME/emulator:$PATH' >> ~/.zshrc
source ~/.zshrc
```

#### 3. 安装必要的 SDK 组件
```bash
# 接受许可协议
yes | sdkmanager --licenses

# 安装必要组件
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
sdkmanager "system-images;android-34;google_apis;arm64-v8a"
```

#### 4. 构建项目
```bash
cd /Users/linjunjie/CascadeProjects/RokidAI/android

# 赋予 gradlew 执行权限
chmod +x gradlew

# 构建项目
./gradlew build

# 安装到设备
./gradlew installDebug
```

## 📦 项目结构

```
android/
├── app/src/main/kotlin/com/rokid/nutrition/
│   ├── MainActivity.kt           # 主界面（拍照、显示、交互）
│   ├── CameraManager.kt          # 相机管理
│   ├── RokidManager.kt           # Rokid SDK（TTS、AR显示）
│   ├── Config.kt                 # 配置（蓝牙UUID、定时器等）
│   └── bluetooth/
│       ├── BluetoothSender.kt    # 发送图片/指令到手机
│       └── BluetoothReceiver.kt  # 接收手机返回的结果
├── build.gradle.kts
└── settings.gradle.kts
```

## 🔧 配置说明

### 蓝牙通信配置

```kotlin
// Config.kt
object Config {
    // 蓝牙服务 UUID（需与手机端一致）
    const val BLUETOOTH_SERVICE_UUID = "00009100-0000-1000-8000-00805f9b34fb"
    
    // 自动拍照间隔（5分钟）
    const val AUTO_CAPTURE_INTERVAL_MS = 5 * 60 * 1000L
}
```

### 权限说明

应用需要以下权限：
- ✅ 相机（CAMERA）
- ✅ 蓝牙（BLUETOOTH, BLUETOOTH_CONNECT）

## 🎯 技术架构

```
眼镜端 --蓝牙(Caps)--> 手机端 --HTTPS--> 后端
```

### SDK 说明
- **CXR-S SDK**: 眼镜端开发 SDK ✅
- **CXR-M SDK**: 手机端使用（本项目不使用）

### 核心组件
- **BluetoothSender** - 发送图片/指令到手机
- **BluetoothReceiver** - 接收手机返回的结果
- **RokidManager** - TTS播报、AR显示
- **CameraManager** - 相机管理

## 🚀 开发状态

### ✅ 已完成
- [x] 项目结构重构（瘦客户端架构）
- [x] 蓝牙通信模块（BluetoothSender/Receiver）
- [x] 相机拍照功能
- [x] 5分钟自动拍照定时器
- [x] 侧键交互（短按拍照/长按开始结束）

### ⏳ 待完成
- [ ] 启用 CXR-S SDK（蓝牙实际连接）
- [ ] AR 显示适配
- [ ] TTS 语音播报优化

## 🧪 测试

### 单元测试
```bash
./gradlew test
```

### UI 测试
```bash
./gradlew connectedAndroidTest
```

## 📱 调试

### 查看日志
```bash
adb logcat | grep "RokidNutrition"
```

### 安装 APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 卸载应用
```bash
adb uninstall com.rokid.nutrition
```

## 🔍 常见问题

### Q1: Gradle 同步失败
```bash
# 清理缓存
./gradlew clean

# 重新下载依赖
./gradlew build --refresh-dependencies
```

### Q2: 模拟器无法访问本地服务
使用 `10.0.2.2` 代替 `localhost`，这是模拟器访问宿主机的特殊 IP。

### Q3: 真机无法访问本地服务
确保手机和电脑在同一 WiFi 网络，使用电脑的局域网 IP（如 `192.168.1.100`）。

### Q4: 权限被拒绝
在设置中手动授予应用所需权限，或在代码中添加运行时权限请求。

## 📚 参考资料

- [Android 开发者文档](https://developer.android.com/)
- [Kotlin 官方文档](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose 教程](https://developer.android.com/jetpack/compose)
- [Rokid 开发者中心](https://developer.rokid.com/)

## 📞 技术支持

如有问题，请查看项目根目录的 `ROKID_DEVELOPMENT_GUIDE.md`。

---

**更新时间**: 2025-11-19
**版本**: 1.0.0
