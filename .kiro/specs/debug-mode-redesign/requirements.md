# Requirements Document

## Introduction

本功能对手机端App的调试模式进行重新设计和优化。主要包括：
1. 将调试模式入口移至"我的"页面底部，整合调试日志和演示模式
2. 同步眼镜端最新的JSON演示数据文件
3. 修复图片显示方向问题（逆时针旋转90度）
4. 在用餐详情页面中显示拍摄的图片

## Glossary

- **Debug Mode（调试模式）**: 开发者和测试人员使用的功能模块，包含调试日志查看和演示模式
- **Demo Mode（演示模式）**: 模拟眼镜端食物识别功能的测试模式，支持单图识别和用餐监测
- **Profile Screen（我的页面）**: App底部导航的"我的"标签页，显示用户信息和设置
- **Meal Detail（用餐详情）**: 显示单次用餐的食物识别结果、营养数据和照片的页面
- **Image Rotation（图片旋转）**: 由于眼镜摄像头方向，上传的图片需要逆时针旋转90度才能正确显示
- **Demo JSON Files（演示JSON文件）**: 存储在assets/demo目录下的预设API响应数据

## Requirements

### Requirement 1

**User Story:** As a developer, I want to access debug mode from the Profile screen, so that I can easily test and troubleshoot the app without navigating through multiple screens.

#### Acceptance Criteria

1. WHEN a user navigates to the Profile screen THEN THE system SHALL display a "Debug Mode" section at the bottom of the page
2. WHEN a user taps on the Debug Mode section THEN THE system SHALL expand to show two options: "Debug Log" and "Demo Mode"
3. WHEN a user taps on "Debug Log" THEN THE system SHALL navigate to the existing debug log screen
4. WHEN a user taps on "Demo Mode" THEN THE system SHALL navigate to the demo mode screen

### Requirement 2

**User Story:** As a tester, I want the demo mode to use the latest JSON data from the glasses app, so that I can test with accurate and up-to-date mock responses.

#### Acceptance Criteria

1. WHEN the phone app is built THEN THE system SHALL include the latest demo JSON files from the glasses app assets
2. WHEN demo mode loads meal data THEN THE system SHALL parse JSON files containing: snapshot_id, session_id, raw_llm (with foods array), snapshot (with nutrition data), and suggestion fields
3. WHEN demo mode displays food items THEN THE system SHALL show dish_name_cn (Chinese name), cooking_method, total_weight_g, and category for each food
4. WHEN demo mode displays nutrition data THEN THE system SHALL show calories, protein, carbs, and fat values from the snapshot.nutrition object

### Requirement 3

**User Story:** As a user, I want uploaded images to display correctly rotated, so that I can see the food photos in the proper orientation.

#### Acceptance Criteria

1. WHEN displaying a meal photo in the app THEN THE system SHALL rotate the image 90 degrees counter-clockwise
2. WHEN displaying a photo in the meal detail screen THEN THE system SHALL apply the same rotation transformation
3. WHEN displaying a photo in the photo viewer dialog THEN THE system SHALL apply the same rotation transformation
4. WHEN displaying a photo thumbnail in history lists THEN THE system SHALL apply the same rotation transformation

### Requirement 4

**User Story:** As a user, I want to see the meal photo in the food detail screen, so that I can visually reference what I ate alongside the nutritional information.

#### Acceptance Criteria

1. WHEN a user opens a meal detail screen THEN THE system SHALL display the associated meal photo at the top of the screen
2. WHEN a meal has a photo available THEN THE system SHALL show the photo with proper rotation applied
3. WHEN a user taps on the meal photo THEN THE system SHALL open a full-screen photo viewer
4. IF a meal has no associated photo THEN THE system SHALL display a placeholder or hide the photo section gracefully

### Requirement 5

**User Story:** As a developer, I want a unified debug mode interface, so that all debugging and testing tools are organized in one place.

#### Acceptance Criteria

1. WHEN the Debug Mode section is displayed THEN THE system SHALL show it as a collapsible card with an icon and title
2. WHEN the Debug Mode section is expanded THEN THE system SHALL display options with appropriate icons for each function
3. WHEN the Debug Mode section is collapsed THEN THE system SHALL show only the header with an expand indicator
4. WHILE in release build THEN THE system SHALL hide the Debug Mode section from regular users
