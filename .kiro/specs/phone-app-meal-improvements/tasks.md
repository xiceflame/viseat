# Implementation Plan

- [x] 1. UI 重组 - 移除快捷操作栏
  - [x] 1.1 修改 HomeScreen.kt 移除 QuickActionsCard
    - 删除 `if (isConnected) { QuickActionsCard(...) }` 代码块
    - 删除 QuickActionsCard composable 函数（如果不再使用）
    - _Requirements: 1.1, 1.2_

  - [x] 1.2 修改 MealStatusCard 添加连接状态检查
    - 添加 `isConnected` 和 `isProcessing` 参数
    - 在"开始用餐"按钮添加 `enabled = isConnected && !isProcessing`
    - 处理中显示 CircularProgressIndicator
    - _Requirements: 1.3, 1.4, 2.5, 2.6_

- [x] 2. 基线照片拍摄流程
  - [x] 2.1 修改 HomeViewModel.startMealSessionManually()
    - 添加眼镜连接检查
    - 调用 `bluetoothManager.takeGlassPhoto()` 拍摄基线照片
    - 处理拍照失败情况
    - _Requirements: 2.1, 2.4, 2.5_

  - [x] 2.2 新增 handleBaselineImageReceived() 方法
    - 复用 `networkManager.uploadAndAnalyzeWithProgress()` 上传分析
    - 调用现有的 `startMealSessionInternal()` 创建会话
    - 更新 UI 状态
    - _Requirements: 2.2, 2.3, 2.6_

  - [ ]* 2.3 编写属性测试：基线照片流程
    - **Property 2: Baseline photo triggers session creation**
    - **Property 3: Failed capture prevents session creation**
    - **Validates: Requirements 2.2, 2.3, 2.4**

- [x] 3. 用餐记录删除功能
  - [x] 3.1 修改 Daos.kt 添加删除方法
    - MealSessionDao 添加 `deleteSession(sessionId: String)`
    - MealSnapshotDao 添加 `deleteSnapshotsForSession(sessionId: String)`
    - MealSnapshotDao 添加 `getSnapshotIdsForSession(sessionId: String)`
    - _Requirements: 4.2_

  - [x] 3.2 修改 MealSessionRepository 添加 deleteSession()
    - 删除快照食物（使用现有 `deleteFoodsForSnapshot`）
    - 删除快照
    - 删除会话
    - _Requirements: 4.2_

  - [x] 3.3 修改 DailyNutritionTracker 添加 subtractNutrition()
    - 减去热量、蛋白质、碳水、脂肪
    - 减少用餐次数
    - 确保值不为负
    - _Requirements: 4.5_

  - [x] 3.4 修改 ApiService 和 NetworkManager 添加删除 API
    - ApiService 添加 `deleteMealSession()` 接口
    - NetworkManager 添加 `deleteMealSession()` 方法
    - 添加 DeleteMealResponse 数据类
    - _Requirements: 4.3_

  - [x] 3.5 修改 HomeViewModel 添加 deleteMealSession()
    - 调用 repository 删除本地数据
    - 判断是否今天的记录，更新 DailyNutritionTracker
    - 调用后端 API，失败时记录日志
    - _Requirements: 4.2, 4.3, 4.5, 4.6_

  - [x] 3.6 新增 DeleteMealConfirmDialog 组件
    - 显示删除确认对话框
    - 显示会话热量信息
    - 确认和取消按钮
    - _Requirements: 4.1, 4.7_

  - [x] 3.7 修改 HomeScreen 添加长按删除支持
    - 修改 RecentMealsCardWithPhotos 添加 onSessionLongPress 参数
    - 修改 MealSessionItemWithPhoto 使用 combinedClickable
    - 添加 HomeUiState 的 showDeleteDialog 和 sessionToDelete 字段
    - 在 HomeScreen 中显示 DeleteMealConfirmDialog
    - _Requirements: 4.1, 4.7_

  - [ ]* 3.8 编写属性测试：删除功能
    - **Property 6: Deletion removes from local database**
    - **Property 7: Today's deletion updates daily tracker**
    - **Validates: Requirements 4.2, 4.5**

- [x] 4. Checkpoint - 确保所有测试通过
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. 最新识别编辑功能
  - [x] 5.1 确认 FoodDetailScreen 编辑功能可用
    - 验证 latestSessionId 正确传递到导航
    - 验证 FoodDetailScreen 可以加载和编辑数据
    - _Requirements: 5.1, 5.2_

  - [x] 5.2 修改 HomeScreen 的 onLatestResultClick 导航
    - 使用 latestSessionId 导航到 food_detail 页面
    - 确保编辑后数据同步回 HomeScreen
    - _Requirements: 5.1, 5.6_

  - [x] 5.3 修改 LatestResultCardWithPhoto 添加编辑提示
    - 添加"点击编辑"文字提示
    - 添加编辑图标
    - _Requirements: 6.1_

  - [ ]* 5.4 编写属性测试：编辑功能
    - **Property 8: Food edit persists locally**
    - **Property 9: Nutrition totals recalculation**
    - **Validates: Requirements 5.4, 6.3**

- [x] 6. Final Checkpoint - 确保所有测试通过
  - Ensure all tests pass, ask the user if questions arise.
