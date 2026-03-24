# 智能健康提示系统 - 任务清单

## 实现计划

基于后端已实现的个性化建议 API，客户端需要完成以下工作：
1. 实现 API 调用层
2. 实现数据仓库层
3. 改造 UI 组件为自动轮播
4. 集成到首页

---

- [x] 1. 实现 NetworkManager 中的个性化建议 API 调用
  - [x] 1.1 添加 getPersonalizedTips 方法
    - 调用 `GET /api/v1/users/{user_id}/personalized-tips`
    - 返回 `Result<PersonalizedTipsResponse>`
    - _Requirements: 4.1, 4.2_
  - [x] 1.2 添加 refreshPersonalizedTips 方法
    - 调用 `POST /api/v1/users/{user_id}/personalized-tips/refresh`
    - 支持传入 trigger 和 meal_session_id
    - _Requirements: 4.1_
  - [ ]* 1.3 添加单元测试验证 API 调用
    - 测试成功响应解析
    - 测试错误处理
    - _Requirements: 4.1_

- [x] 2. 实现 SmartTipsRepository 数据仓库
  - [x] 2.1 创建 SmartTipsRepository 类
    - 封装 NetworkManager 调用
    - 管理提示缓存
    - 提供 Flow 数据流
    - _Requirements: 4.1, 4.2, 4.3_
  - [x] 2.2 实现提示缓存逻辑
    - 缓存最近获取的提示
    - 支持离线显示
    - _Requirements: 4.1_
  - [ ]* 2.3 添加属性测试验证提示优先级排序
    - **Property 4: 提示优先级排序**
    - **Validates: Requirements 4.4**
    - _Requirements: 4.4_

- [x] 3. 改造 SmartHealthTipsCard UI 组件
  - [x] 3.1 实现自动轮播功能
    - 每5秒自动切换到下一条提示
    - 使用 HorizontalPager 实现
    - _Requirements: 2.1, 2.4_
  - [x] 3.2 实现触摸暂停/恢复逻辑
    - 触摸时暂停自动轮播
    - 手指离开3秒后恢复
    - _Requirements: 2.2, 2.3_
  - [x] 3.3 移除反馈按钮，简化界面
    - 删除"这条建议有帮助吗？"区域
    - 保留底部圆点指示器
    - _Requirements: 3.1, 3.2_
  - [x] 3.4 实现单条提示时的特殊处理
    - 不显示轮播指示器
    - 不进行自动轮播
    - _Requirements: 2.5_
  - [ ]* 3.5 添加属性测试验证轮播循环
    - **Property 1: 轮播循环**
    - **Validates: Requirements 2.4**
    - _Requirements: 2.4_

- [x] 4. 实现图标和颜色映射
  - [x] 4.1 创建 TipStyleMapper 工具类
    - 根据 category 映射图标
    - 根据 category 映射颜色主题
    - _Requirements: 3.4_
  - [ ]* 4.2 添加属性测试验证映射一致性
    - **Property 2: 图标映射一致性**
    - **Property 3: 颜色映射一致性**
    - **Validates: Requirements 3.4**
    - _Requirements: 3.4_

- [x] 5. 集成到 HomeScreen
  - [x] 5.1 在 HomeViewModel 中集成 SmartTipsRepository
    - 添加 tips StateFlow
    - 实现 loadTips 和 refreshTips 方法
    - _Requirements: 4.1, 4.2_
  - [x] 5.2 替换 HomeScreen 中的 AIInsightsCard
    - 使用新的 SmartHealthTipsCard
    - 传入从 ViewModel 获取的提示数据
    - _Requirements: 1.1, 2.1_
  - [x] 5.3 实现刷新时机
    - 首页加载时获取提示
    - 用餐结束后刷新提示
    - 日期变更时刷新提示
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 6. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户

- [x] 7. 清理旧代码
  - [x] 7.1 删除或重构 PersonalizedTipsViewModel
    - 移除未使用的 PersonalizedTipsRepository 引用
    - 整合到 HomeViewModel 中
    - _Requirements: 1.1_
  - [x] 7.2 删除旧的 AIInsightsCard 中的反馈相关代码
    - 移除 onFeedback 回调
    - 移除 TipContent 中的反馈按钮
    - _Requirements: 3.1_

- [x] 8. Final Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户

---

## 优先级说明

### P0 (必须完成)
- 任务 1: API 调用层
- 任务 2: 数据仓库层
- 任务 3: UI 组件改造
- 任务 5: 集成到首页

### P1 (重要)
- 任务 4: 图标颜色映射
- 任务 7: 清理旧代码

### P2 (可选)
- 属性测试（标记为 * 的子任务）

---

## 技术说明

### 自动轮播实现方案

使用 Compose 的 `HorizontalPager` + `LaunchedEffect` 实现：

```kotlin
@Composable
fun SmartHealthTipsCard(
    tips: List<PersonalizedTip>,
    autoScrollInterval: Long = 5000L
) {
    val pagerState = rememberPagerState { tips.size }
    var isPaused by remember { mutableStateOf(false) }
    
    // 自动轮播
    LaunchedEffect(pagerState, isPaused) {
        if (!isPaused && tips.size > 1) {
            while (true) {
                delay(autoScrollInterval)
                val nextPage = (pagerState.currentPage + 1) % tips.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPaused = true
                    tryAwaitRelease()
                    delay(3000)
                    isPaused = false
                }
            )
        }
    ) { page ->
        TipContent(tips[page])
    }
}
```

### 提示类型与样式映射

| category | 图标 | 颜色 |
|----------|------|------|
| nutrition | Restaurant | AppleTeal |
| timing | Schedule | AppleBlue |
| habit | FitnessCenter | ApplePurple |
| warning | Warning | AppleOrange |
| encouragement | EmojiEvents | AppleGreen |
