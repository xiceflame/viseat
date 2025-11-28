# Requirements Document

## Introduction

本文档定义了 Rokid 营养助手手机端应用的功能需求。手机端应用作为眼镜与后端服务之间的网络中枢，负责蓝牙通信、API 调用、用户档案管理、统计展示和设备管理等核心功能。

## Glossary

- **Phone App**: 运行在 Android 手机上的营养助手应用，使用 CXR-M SDK 与眼镜通信
- **CXR-M SDK**: Rokid 提供的手机端 SDK，用于蓝牙连接和自定义协议通信
- **Glasses**: Rokid AR 眼镜，作为瘦客户端负责拍照和显示
- **Backend**: FastAPI 后端服务，提供 VLM 识别、营养计算和会话管理
- **Meal Session**: 用餐会话，从开始进餐到结束进餐的完整过程
- **Nutrition Result**: 营养识别结果，包含食物名称、热量、蛋白质、碳水、脂肪等信息
- **User Profile**: 用户档案，包含年龄、BMI、健康状况、饮食习惯等个性化信息
- **Caps**: Rokid SDK 中的数据封装格式，用于蓝牙消息传输

## Requirements

### Requirement 1: 蓝牙连接管理

**User Story:** As a user, I want to connect my phone to Rokid glasses via Bluetooth, so that I can use the nutrition tracking features.

#### Acceptance Criteria

1. WHEN the user opens the app THEN the Phone App SHALL display available Rokid glasses devices for pairing
2. WHEN the user selects a glasses device THEN the Phone App SHALL initiate Bluetooth connection using CXR-M SDK
3. WHILE the Bluetooth connection is active THEN the Phone App SHALL display the connection status in the UI
4. IF the Bluetooth connection fails THEN the Phone App SHALL display an error message with retry option
5. WHEN the Bluetooth connection is lost THEN the Phone App SHALL attempt automatic reconnection within 30 seconds

### Requirement 2: 图片接收与处理

**User Story:** As a user, I want my phone to receive food photos from my glasses, so that the photos can be analyzed for nutrition information.

#### Acceptance Criteria

1. WHEN the glasses send an image via Bluetooth THEN the Phone App SHALL receive and decode the image data within 3 seconds
2. WHEN an image is received THEN the Phone App SHALL validate the image format (JPEG) and size before processing
3. IF the received image is corrupted THEN the Phone App SHALL request retransmission from the glasses
4. WHEN an image is successfully received THEN the Phone App SHALL store it temporarily for API upload

### Requirement 3: 后端 API 通信

**User Story:** As a user, I want my phone to communicate with the backend server, so that food images can be analyzed and nutrition data can be retrieved.

#### Acceptance Criteria

1. WHEN an image is ready for analysis THEN the Phone App SHALL upload the image to the backend `/api/v1/upload` endpoint
2. WHEN the image is uploaded THEN the Phone App SHALL call `/api/v1/vision/analyze` with the image URL
3. WHEN the backend returns analysis results THEN the Phone App SHALL parse the JSON response into NutritionResult objects
4. IF the backend returns a non-food image error (HTTP 400) THEN the Phone App SHALL notify the user to retake the photo
5. WHEN network request fails THEN the Phone App SHALL retry up to 3 times with exponential backoff

### Requirement 4: 营养结果传输到眼镜

**User Story:** As a user, I want to see nutrition results on my glasses, so that I can view the information hands-free while eating.

#### Acceptance Criteria

1. WHEN nutrition analysis is complete THEN the Phone App SHALL send the result to glasses via Bluetooth CustomCmd
2. WHEN sending results THEN the Phone App SHALL format data using Caps protocol with food name, calories, protein, carbs, fat, and suggestion
3. WHEN sending AR view data THEN the Phone App SHALL construct CustomView JSON with proper layout and styling
4. IF result transmission fails THEN the Phone App SHALL retry sending within 5 seconds

### Requirement 5: 用餐会话管理

**User Story:** As a user, I want to track my entire meal from start to finish, so that I can get a complete nutrition summary.

#### Acceptance Criteria

1. WHEN the glasses send a "start_meal" command THEN the Phone App SHALL call `/api/v1/meal/start` to create a new session
2. WHILE a meal session is active THEN the Phone App SHALL track the session ID and elapsed time
3. WHEN the glasses send a photo during an active session THEN the Phone App SHALL call `/api/v1/meal/update` with the current snapshot
4. WHEN the glasses send an "end_meal" command THEN the Phone App SHALL call `/api/v1/meal/end` to finalize the session
5. WHEN a session ends THEN the Phone App SHALL display the meal summary including total calories, duration, and consumption ratio

### Requirement 6: 用户档案管理

**User Story:** As a user, I want to set up my health profile, so that I can receive personalized nutrition advice.

#### Acceptance Criteria

1. WHEN the user first launches the app THEN the Phone App SHALL prompt for profile setup (age, height, weight, health conditions)
2. WHEN profile data is entered THEN the Phone App SHALL calculate and store BMI automatically
3. WHEN the user has health conditions (e.g., fatty liver) THEN the Phone App SHALL include this in API requests for personalized suggestions
4. WHEN the user wants to update profile THEN the Phone App SHALL provide an edit interface in settings
5. WHEN profile is saved THEN the Phone App SHALL persist data to local Room database

### Requirement 7: 统计与历史记录

**User Story:** As a user, I want to view my eating statistics and history, so that I can track my nutrition habits over time.

#### Acceptance Criteria

1. WHEN the user opens statistics page THEN the Phone App SHALL display daily calorie intake summary
2. WHEN viewing weekly statistics THEN the Phone App SHALL show a chart of daily calorie trends
3. WHEN the user views meal history THEN the Phone App SHALL list past meal sessions with date, type, and total calories
4. WHEN the user selects a historical meal THEN the Phone App SHALL display detailed nutrition breakdown
5. WHEN statistics data is requested THEN the Phone App SHALL aggregate data from local Room database

### Requirement 8: 本地数据持久化

**User Story:** As a user, I want my meal data to be saved locally, so that I can access it even without internet connection.

#### Acceptance Criteria

1. WHEN a meal session is created THEN the Phone App SHALL store session data in Room database
2. WHEN nutrition results are received THEN the Phone App SHALL cache the results locally
3. WHEN the app starts THEN the Phone App SHALL load cached data for offline viewing
4. WHEN local data conflicts with server data THEN the Phone App SHALL prefer server data and update local cache

### Requirement 9: 设备管理

**User Story:** As a user, I want to manage my connected Rokid glasses, so that I can configure device settings and view device status.

#### Acceptance Criteria

1. WHEN the user opens device management THEN the Phone App SHALL display connected glasses information (name, battery, firmware)
2. WHEN multiple glasses are paired THEN the Phone App SHALL allow switching between devices
3. WHEN the user wants to unpair THEN the Phone App SHALL provide disconnect and forget device options
4. WHEN device settings change THEN the Phone App SHALL sync settings to the glasses via Bluetooth

### Requirement 10: 错误处理与用户反馈

**User Story:** As a user, I want clear feedback when something goes wrong, so that I can understand and resolve issues.

#### Acceptance Criteria

1. WHEN any operation fails THEN the Phone App SHALL display a user-friendly error message in Chinese
2. WHEN network is unavailable THEN the Phone App SHALL show offline mode indicator and queue operations
3. WHEN Bluetooth is disabled THEN the Phone App SHALL prompt user to enable Bluetooth
4. WHEN processing takes longer than 5 seconds THEN the Phone App SHALL display a loading indicator with progress
5. IF a critical error occurs THEN the Phone App SHALL log the error details for debugging
