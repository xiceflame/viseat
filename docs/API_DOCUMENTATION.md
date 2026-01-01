# VisEat (Rokid智能营养助手) API 接口文档

本文档列出了 VisEat 系统后端提供的所有 API 接口。

## 基础信息
- **Base URL**: `http://<server-ip>:8000`
- **版本**: 1.0.0
- **说明**: 智能营养助手后端服务，支持食物识别、营养分析、个人健康管理及 AI 对话。

---

## 1. 核心业务接口

### 1.1 食物识别与视觉分析
- **POST `/api/v1/vision/upload`**
  - **功能**: 上传食物图片。
  - **参数**: `file` (Multipart), `user_id` (Query, 可选), `session_id` (Query, 可选)。
  - **返回**: 图片 URL 及文件名。

- **POST `/api/v1/vision/analyze`**
  - **功能**: 调用 Qwen-VL 对图片进行多食材拆解。
  - **参数**: `VisionAnalyzeRequest` (JSON)，包含 `image_url` 或 `image_base64`，以及 `mode` (start/update/end)。
  - **返回**: 识别出的食物列表、估算重量及个性化建议。

- **POST `/api/v1/vision/analyze_meal_update`**
  - **功能**: 带容错逻辑的用餐更新分析（检测加菜、拍摄异常等）。

### 1.2 用餐会话管理
- **POST `/api/v1/meal/start`**
  - **功能**: 开始一次用餐会话。
  - **参数**: `SnapshotPayload` (包含初始食物列表和图片)。
  - **返回**: `session_id`。

- **POST `/api/v1/meal/update`**
  - **功能**: 自动监测更新（由眼镜定期调用）。
  - **返回**: 当前剩余热量、已消耗热量、摄入比例及建议。

- **POST `/api/v1/meal/end`**
  - **功能**: 结束用餐会话。
  - **返回**: 本餐总结（用于眼镜）、详细营养分析（用于手机）、下一餐建议。

- **GET `/api/v1/meal/session/{session_id}`**
  - **功能**: 获取指定会话详情。

- **GET `/api/v1/meal/sessions`**
  - **功能**: 获取用户的用餐历史列表。

### 1.3 营养查询
- **POST `/api/v1/food/nutrition`**
  - **功能**: 根据名称和重量查询营养成分。

- **GET `/api/v1/food/search`**
  - **功能**: 关键词搜索食物库。

- **GET `/api/v1/nutrition/barcode`**
  - **功能**: 条码扫描查询（接入 Open Food Facts）。

---

## 2. 用户与健康管理

### 2.1 用户档案
- **POST `/api/v1/user/register`**
  - **功能**: 用户注册或登录（基于 device_id）。

- **GET `/api/v1/user/profile`**
  - **功能**: 获取用户个人资料（BMI、目标热量等）。

- **PUT `/api/v1/user/profile`**
  - **功能**: 更新用户资料及健康目标。

### 2.2 统计与建议
- **GET `/api/v1/user/{user_id}/stats`**
  - **功能**: 获取用户历史统计数据。

- **GET `/api/v1/stats/daily`**
  - **功能**: 获取指定日期的营养摄入汇总。

- **POST `/api/v1/chat/nutrition`**
  - **功能**: AI 营养师对话接口（支持上下文关联）。

- **GET `/api/v1/users/{user_id}/personalized-tips`**
  - **功能**: 获取针对用户饮食习惯生成的个性化健康建议。

---

## 3. 管理员接口 (需 Basic Auth)

### 3.1 仪表盘数据
- **GET `/api/v1/admin/dashboard/overview`**
  - **功能**: 24小时业务与运维指标总览。

- **GET `/api/v1/admin/dashboard/traffic`**
  - **功能**: 流量趋势与热门接口。

- **GET `/api/v1/admin/dashboard/users`**
  - **功能**: 用户列表概况。

- **GET `/api/v1/admin/dashboard/recent-images`**
  - **功能**: 最近上传图片及 AI 分析结果审核。

### 3.2 系统维护
- **GET `/api/v1/admin/storage/stats`**
  - **功能**: 磁盘占用情况。

- **POST `/api/v1/admin/storage/cleanup`**
  - **功能**: 清理过期图片。

---

## 4. 系统辅助接口
- **GET `/health`**: 系统健康检查。
- **GET `/metrics`**: Prometheus 指标。
- **GET `/dashboard`**: 管理后台入口。
- **GET `/users-admin`**: 用户管理页面。
