# Requirements Document

## Introduction

本功能为手机App端提供模拟数据分析能力，用于演示和测试眼镜端的食物识别功能。系统支持两种模式：单图识别模式和用餐监测模式。

单图识别模式允许用户选择预设的食物图片（可乐、薯片）进行模拟识别，模拟数据直接使用眼镜端保存的真实API响应（rawResponse），确保数据格式完全一致。

用餐监测模式模拟完整的用餐流程，包含开始用餐（首图）、用餐中（自动拍摄图）、结束用餐（结束图）三个阶段，使用预设的用餐图片和对应的模拟营养数据。

所有模拟识别结果与眼镜端数据格式一致，可在手机端历史记录中查看。

## Glossary

- **Demo_Simulation_System**: 演示模拟系统，负责管理模拟数据和模拟识别流程
- **Single_Image_Mode**: 单图识别模式，每次选择一张预设图片进行模拟识别
- **Meal_Monitoring_Mode**: 用餐监测模式，模拟完整的用餐监测流程（三阶段）
- **Raw_Response**: 眼镜端保存的API原始响应JSON，包含完整的识别结果
- **VisionAnalyzeResponse**: 视觉分析响应数据模型，手机端用于解析和显示识别结果
- **Simulation_Session**: 模拟会话，管理用餐监测模式的状态和进度
- **Auto_Capture**: 自动拍摄，用餐监测模式中的中间自动拍摄阶段

## Requirements

### Requirement 1: 演示模式入口

**User Story:** As a 测试人员, I want to 在手机App中访问演示模式, so that I can 进行模拟数据分析测试。

#### Acceptance Criteria

1. WHEN 用户在主页点击演示模式按钮 THEN Demo_Simulation_System SHALL 显示演示模式选择界面
2. WHEN 演示模式选择界面显示 THEN Demo_Simulation_System SHALL 提供单图识别和用餐监测两种模式选项
3. WHEN 用户选择任一模式 THEN Demo_Simulation_System SHALL 导航到对应的模拟界面

### Requirement 2: 单图识别模式

**User Story:** As a 测试人员, I want to 选择预设食物图片进行单次识别, so that I can 测试单图识别功能和查看识别结果。

#### Acceptance Criteria

1. WHEN 用户进入单图识别模式 THEN Demo_Simulation_System SHALL 显示可选的预设食物列表（可乐、薯片）
2. WHEN 用户选择可乐图片 THEN Demo_Simulation_System SHALL 加载 coke.jpg 图片并显示预览
3. WHEN 用户选择薯片图片 THEN Demo_Simulation_System SHALL 加载 chips.jpg 图片并显示预览
4. WHEN 用户点击开始识别按钮 THEN Demo_Simulation_System SHALL 触发模拟识别流程并显示分析中状态（1-2秒延迟）
5. WHEN 模拟识别完成 THEN Demo_Simulation_System SHALL 解析预设的 Raw_Response JSON 并显示营养分析结果
6. WHEN 识别结果显示 THEN Demo_Simulation_System SHALL 将结果保存到本地数据库并同步到历史记录列表

### Requirement 3: 模拟数据格式

**User Story:** As a 开发人员, I want to 使用与眼镜端一致的数据格式, so that I can 确保手机端能正确解析和显示识别结果。

#### Acceptance Criteria

1. WHEN 模拟数据加载 THEN Demo_Simulation_System SHALL 使用眼镜端保存的 Raw_Response JSON 格式
2. WHEN 解析模拟数据 THEN Demo_Simulation_System SHALL 将 JSON 转换为 VisionAnalyzeResponse 对象
3. WHEN 模拟数据包含 raw_llm 字段 THEN Demo_Simulation_System SHALL 提取 foods 数组和 suggestion 字段
4. WHEN 模拟数据包含 snapshot 字段 THEN Demo_Simulation_System SHALL 提取 nutrition 对象（calories、protein、carbs、fat）
5. WHEN 模拟数据包含 image_url 字段 THEN Demo_Simulation_System SHALL 使用该 URL 显示食物图片

### Requirement 4: 预设模拟数据配置

**User Story:** As a 测试人员, I want to 配置和更新模拟数据, so that I can 使用最新的眼镜端识别结果进行测试。

#### Acceptance Criteria

1. WHEN Demo_Simulation_System 初始化 THEN Demo_Simulation_System SHALL 从 assets/demo 目录加载预设的 JSON 数据文件
2. WHEN 可乐模拟数据加载 THEN Demo_Simulation_System SHALL 读取 coke_response.json 文件
3. WHEN 薯片模拟数据加载 THEN Demo_Simulation_System SHALL 读取 chips_response.json 文件
4. WHEN JSON 文件不存在 THEN Demo_Simulation_System SHALL 使用内置的默认模拟数据
5. WHEN 模拟数据更新 THEN Demo_Simulation_System SHALL 支持通过替换 JSON 文件更新数据

### Requirement 5: 用餐监测模式入口

**User Story:** As a 测试人员, I want to 启动用餐监测模拟流程, so that I can 测试完整的用餐监测功能。

#### Acceptance Criteria

1. WHEN 用户进入用餐监测模式 THEN Demo_Simulation_System SHALL 显示用餐监测模拟界面
2. WHEN 用餐监测界面显示 THEN Demo_Simulation_System SHALL 显示三个阶段的进度指示器（开始、用餐中、结束）
3. WHEN 用户点击开始用餐按钮 THEN Demo_Simulation_System SHALL 创建新的模拟会话并进入用餐开始阶段

### Requirement 6: 用餐开始阶段

**User Story:** As a 测试人员, I want to 模拟用餐开始的首图识别, so that I can 测试用餐会话的创建和基线数据设置。

#### Acceptance Criteria

1. WHEN 用餐开始阶段触发 THEN Demo_Simulation_System SHALL 加载用餐开始图片（用餐开始.jpg）
2. WHEN 首图识别完成 THEN Demo_Simulation_System SHALL 返回完整餐食的基线营养数据
3. WHEN 首图识别完成 THEN Demo_Simulation_System SHALL 创建用餐会话并记录开始时间
4. WHEN 首图识别完成 THEN Demo_Simulation_System SHALL 自动进入用餐中阶段

### Requirement 7: 用餐中阶段（自动拍摄）

**User Story:** As a 测试人员, I want to 模拟用餐中的自动拍摄识别, so that I can 测试用餐进度追踪功能。

#### Acceptance Criteria

1. WHEN 用餐中阶段触发 THEN Demo_Simulation_System SHALL 加载用餐中图片（用餐中.jpg）
2. WHEN 用餐中识别完成 THEN Demo_Simulation_System SHALL 返回剩余食物的营养数据
3. WHEN 用餐中识别完成 THEN Demo_Simulation_System SHALL 计算并显示已消耗的热量
4. WHEN 用餐中识别完成 THEN Demo_Simulation_System SHALL 更新用餐进度显示

### Requirement 8: 用餐结束阶段

**User Story:** As a 测试人员, I want to 模拟用餐结束的最终识别, so that I can 测试用餐总结和报告生成。

#### Acceptance Criteria

1. WHEN 用户点击结束用餐按钮 THEN Demo_Simulation_System SHALL 加载用餐结束图片
2. WHEN 结束识别完成 THEN Demo_Simulation_System SHALL 计算总消耗热量和消耗比例
3. WHEN 结束识别完成 THEN Demo_Simulation_System SHALL 生成用餐总结报告
4. WHEN 结束识别完成 THEN Demo_Simulation_System SHALL 将完整用餐记录保存到历史列表
5. WHEN 用餐会话结束 THEN Demo_Simulation_System SHALL 显示用餐完成界面和统计数据

### Requirement 9: 用餐监测模拟数据

**User Story:** As a 测试人员, I want to 查看用餐监测的完整模拟数据, so that I can 验证用餐追踪的准确性。

#### Acceptance Criteria

1. WHEN 用餐开始识别完成 THEN Demo_Simulation_System SHALL 返回基线热量（从 meal_start_response.json 读取）
2. WHEN 用餐中识别完成 THEN Demo_Simulation_System SHALL 返回剩余热量（从 meal_middle_response.json 读取）
3. WHEN 用餐结束识别完成 THEN Demo_Simulation_System SHALL 返回最终剩余热量（从 meal_end_response.json 读取）
4. WHEN 用餐会话结束 THEN Demo_Simulation_System SHALL 计算消耗比例（基线热量 - 最终剩余热量）/ 基线热量

### Requirement 10: 数据同步与显示

**User Story:** As a 测试人员, I want to 在手机端查看所有模拟识别结果, so that I can 验证数据同步功能。

#### Acceptance Criteria

1. WHEN 单图识别完成 THEN Demo_Simulation_System SHALL 将结果保存到 MealSession 数据库并显示在历史列表
2. WHEN 用餐监测完成 THEN Demo_Simulation_System SHALL 将完整会话保存到数据库并显示在历史列表
3. WHEN 用户点击历史记录项 THEN Demo_Simulation_System SHALL 导航到食物详情页显示完整营养分析
4. WHEN 模拟数据保存 THEN Demo_Simulation_System SHALL 使用与真实识别相同的 Repository 方法保存数据

### Requirement 11: 模拟图片资源管理

**User Story:** As a 开发人员, I want to 管理演示模式的图片资源, so that I can 确保模拟功能正常运行。

#### Acceptance Criteria

1. WHEN Demo_Simulation_System 初始化 THEN Demo_Simulation_System SHALL 从 assets/demo 目录加载预设图片（coke.jpg、chips.jpg）
2. WHEN 用餐监测模式初始化 THEN Demo_Simulation_System SHALL 加载三张用餐图片（用餐开始.jpg、用餐中.jpg、结束图）
3. WHEN 图片加载失败 THEN Demo_Simulation_System SHALL 显示错误提示并禁用对应的模拟选项
4. WHEN 图片资源更新 THEN Demo_Simulation_System SHALL 支持通过替换 assets/demo 目录下的文件更新图片

### Requirement 12: 演示模式UI界面

**User Story:** As a 测试人员, I want to 通过直观的界面操作演示模式, so that I can 方便地进行模拟测试。

#### Acceptance Criteria

1. WHEN 用户进入演示模式界面 THEN Demo_Simulation_System SHALL 显示清晰的模式选择卡片（单图识别、用餐监测）
2. WHEN 单图识别模式激活 THEN Demo_Simulation_System SHALL 显示食物选择列表和图片预览区域
3. WHEN 用餐监测模式激活 THEN Demo_Simulation_System SHALL 显示三阶段进度指示器和当前阶段状态
4. WHEN 模拟识别进行中 THEN Demo_Simulation_System SHALL 显示加载动画和状态文字
5. WHEN 模拟识别完成 THEN Demo_Simulation_System SHALL 显示结果卡片并提供查看详情按钮
