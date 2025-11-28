# Implementation Plan: Rokid Nutrition Phone App

## 项目路径: `android-phone/`

- [x] 1. 项目初始化与基础配置
  - [x] 1.1 创建 Android 项目结构
    - 创建 `android-phone/` 目录
    - 配置 `settings.gradle.kts` 添加 Rokid Maven 仓库
    - 配置 `app/build.gradle.kts` 添加依赖（CXR-M SDK, Retrofit, Room, Compose）
    - 创建 `AndroidManifest.xml` 配置权限和服务
    - _Requirements: 1.1, 1.2_

  - [x] 1.2 创建常量配置文件
    - 创建 `Config.kt` 定义蓝牙 UUID、消息名称、API 地址
    - 确保与眼镜端 `Config.kt` 消息协议一致
    - _Requirements: 1.1_

- [x] 2. 蓝牙通信模块（CXR-M SDK）
  - [x] 2.1 实现 BluetoothManager
    - 封装 `CxrApi.getInstance()` 调用
    - 实现 BLE 扫描、配对、连接流程
    - 实现 `BluetoothStatusCallback` 处理连接状态
    - 实现 `setCustomCmdListener` 接收眼镜消息
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 2.3 实现消息接收处理
    - 解析 `nutrition_image` 消息（图片数据）
    - 解析 `nutrition_command` 消息（start_meal/end_meal）
    - 使用 SharedFlow 发布接收到的数据
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 2.4 实现消息发送功能
    - 实现 `sendNutritionResult()` 发送营养结果
    - 实现 `sendSessionStatus()` 发送会话状态
    - 实现 `formatFoodName()` 格式化菜品描述
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 2.5 实现 AR 视图功能
    - 实现 `openARView()` 打开自定义视图
    - 实现 `updateARView()` 更新视图内容
    - 实现 `closeARView()` 关闭视图
    - 构建营养信息 JSON 布局
    - _Requirements: 4.3_

  - [x] 2.6 实现 GlassesConnectionService
    - 创建前台服务保持蓝牙连接
    - 实现 Service Binder 暴露 BluetoothManager
    - 配置通知渠道和通知内容
    - _Requirements: 1.5_

- [x] 3. Checkpoint - 蓝牙模块测试
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. 网络通信模块（Retrofit）
  - [x] 4.1 创建 ApiService 接口
    - 定义 `/api/v1/upload` 图片上传接口
    - 定义 `/api/v1/vision/analyze` 视觉分析接口
    - 定义 `/api/v1/meal/*` 会话管理接口
    - 定义 `/api/v1/chat/nutrition` 对话接口
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 4.2 创建 API 响应数据类
    - 创建 `VisionAnalyzeResponse` 及相关类
    - 创建 `MealStartResponse`, `MealUpdateResponse`, `MealEndResponse`
    - 创建 `ChatNutritionResponse`
    - 确保与后端 main.py 返回格式一致
    - _Requirements: 3.3_

  - [x] 4.3 实现 NetworkManager
    - 配置 OkHttpClient（超时、日志）
    - 配置 Retrofit 实例
    - 实现 `uploadAndAnalyze()` 上传并分析图片
    - 实现重试逻辑（3次，指数退避）
    - _Requirements: 3.1, 3.2, 3.4, 3.5_

- [x] 5. Checkpoint - 网络模块测试
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. 数据库模块（Room）
  - [x] 6.1 创建 Room Entities
    - 创建 `UserProfileEntity` 用户档案
    - 创建 `MealSessionEntity` 用餐会话
    - 创建 `MealSnapshotEntity` 用餐快照
    - 创建 `SnapshotFoodEntity` 快照食物
    - _Requirements: 6.5, 8.1, 8.2_

  - [x] 6.2 创建 DAOs
    - 创建 `UserProfileDao` 用户档案操作
    - 创建 `MealSessionDao` 会话操作（含统计查询）
    - 创建 `MealSnapshotDao` 快照操作
    - 创建 `SnapshotFoodDao` 食物操作
    - _Requirements: 7.5, 8.1, 8.2, 8.3_

  - [x] 6.3 创建 AppDatabase
    - 配置数据库版本和实体
    - 实现单例模式
    - _Requirements: 8.1_

- [x] 7. 业务逻辑层（Repository + ViewModel）
  - [x] 7.1 创建 MealSessionRepository
    - 封装会话的创建、更新、结束逻辑
    - 协调网络请求和本地存储
    - 处理数据冲突（服务器优先）
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 8.4_

  - [x] 7.2 创建 UserProfileRepository
    - 实现用户档案的保存和读取
    - 实现 BMI 自动计算
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 7.3 创建 StatisticsRepository
    - 实现每日热量统计
    - 实现每周数据聚合
    - 实现历史记录查询
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 8. Checkpoint - 业务逻辑测试
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. UI 层（Jetpack Compose）
  - [x] 9.1 创建 HomeScreen
    - 显示蓝牙连接状态
    - 显示当前会话信息
    - 显示最新识别结果
    - 提供手动拍照按钮
    - _Requirements: 1.3, 5.2_

  - [x] 9.2 创建 HomeViewModel
    - 管理连接状态 StateFlow
    - 管理当前会话 StateFlow
    - 处理图片接收和命令接收
    - 协调蓝牙和网络操作
    - _Requirements: 2.1, 3.1, 4.1, 5.1_

  - [x] 9.3 创建 StatsScreen
    - 显示每日热量摄入
    - 显示每周热量趋势图表
    - 显示营养分布饼图
    - _Requirements: 7.1, 7.2_

  - [x] 9.4 创建 StatsViewModel
    - 加载每日/每周统计数据
    - 格式化图表数据
    - _Requirements: 7.1, 7.2, 7.5_

  - [x] 9.5 创建 HistoryScreen
    - 列表展示历史用餐记录
    - 支持点击查看详情
    - _Requirements: 7.3, 7.4_

  - [x] 9.6 创建 ProfileScreen
    - 首次启动引导用户填写档案
    - 显示和编辑用户信息
    - 显示 BMI 计算结果
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 9.7 创建 DeviceScreen
    - 显示已连接眼镜信息
    - 显示电量、固件版本
    - 提供断开连接选项
    - _Requirements: 9.1, 9.2, 9.3, 9.4_

  - [x] 9.8 创建 Navigation
    - 配置底部导航栏
    - 配置页面路由
    - _Requirements: 7.3_

- [x] 10. 错误处理与用户反馈
  - [x] 10.1 创建 ErrorHandler
    - 实现网络错误处理
    - 实现蓝牙错误处理
    - 生成中文错误消息
    - _Requirements: 10.1, 10.2, 10.3_

  - [x] 10.2 实现离线队列
    - 实现操作队列存储
    - 实现网络恢复后重放
    - _Requirements: 10.2_

  - [x] 10.3 实现加载状态显示
    - 创建 LoadingIndicator 组件
    - 在长时间操作时显示进度
    - _Requirements: 10.4, 10.5_

- [x] 11. 集成与联调
  - [x] 11.1 集成蓝牙和网络模块
    - 在 HomeViewModel 中协调蓝牙接收和 API 调用
    - 实现完整的图片处理流程
    - _Requirements: 2.1, 3.1, 4.1_

  - [x] 11.2 集成会话管理
    - 处理 start_meal 命令
    - 处理 end_meal 命令
    - 自动更新会话状态
    - _Requirements: 5.1, 5.3, 5.4_

  - [x] 11.3 创建 Application 类
    - 初始化依赖注入
    - 初始化数据库
    - 启动蓝牙服务
    - _Requirements: 1.1_

- [x] 12. Final Checkpoint - 完整功能测试
  - Ensure all tests pass, ask the user if questions arise.
