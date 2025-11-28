# Requirements Document

## Introduction

本功能为手机端营养追踪应用增加两个核心能力：
1. **照片展示**：用户每次拍照识别的食物照片应在"最新识别"、"最近识别"等详情页面中可见，让用户能够直观回顾自己的饮食记录
2. **饮食数据编辑**：所有饮食数据（包括总重量、热量、营养成分等）支持用户手动修改，修改后的数据需同步保存到本地数据库和云端服务器

## Glossary

- **Meal_Photo_Edit_System**: 负责照片展示和饮食数据编辑的系统模块
- **MealSnapshot**: 用餐快照，包含拍摄的照片和识别的食物数据
- **NutritionData**: 营养数据，包括热量(calories)、蛋白质(protein)、碳水化合物(carbs)、脂肪(fat)
- **LocalStorage**: 本地 Room 数据库存储
- **CloudSync**: 云端数据同步服务
- **FoodItem**: 单个食物项，包含名称、重量、营养成分等信息

## Requirements

### Requirement 1: 照片存储与展示

**User Story:** As a user, I want to see the photos I took during meal recognition in the detail pages, so that I can visually review my dietary records.

#### Acceptance Criteria

1. WHEN a user captures a food photo for recognition THEN THE Meal_Photo_Edit_System SHALL store the photo path locally alongside the meal snapshot data
2. WHEN a user views the "latest recognition" detail page THEN THE Meal_Photo_Edit_System SHALL display the captured photo at the top of the page
3. WHEN a user views the meal history list THEN THE Meal_Photo_Edit_System SHALL display a thumbnail of the meal photo for each record
4. WHEN the local photo file is unavailable THEN THE Meal_Photo_Edit_System SHALL display a placeholder image and attempt to load from the cloud URL
5. WHEN displaying meal photos THEN THE Meal_Photo_Edit_System SHALL support pinch-to-zoom and full-screen viewing
6. WHEN a user views a photo in full-screen mode THEN THE Meal_Photo_Edit_System SHALL provide a download button to save the photo to the device gallery

### Requirement 2: 饮食数据编辑

**User Story:** As a user, I want to edit the recognized food data (weight, calories, nutrients), so that I can correct any recognition errors and maintain accurate dietary records.

#### Acceptance Criteria

1. WHEN a user taps on a food item in the detail page THEN THE Meal_Photo_Edit_System SHALL display an editable form with current values for weight, calories, protein, carbs, and fat
2. WHEN a user modifies the total weight of a food item THEN THE Meal_Photo_Edit_System SHALL automatically recalculate the nutritional values proportionally
3. WHEN a user directly edits individual nutritional values THEN THE Meal_Photo_Edit_System SHALL accept the manual override without automatic recalculation
4. WHEN a user saves edited data THEN THE Meal_Photo_Edit_System SHALL validate that all numerical values are non-negative before saving
5. WHEN a user cancels editing THEN THE Meal_Photo_Edit_System SHALL discard all changes and restore the original values

### Requirement 3: 本地数据持久化

**User Story:** As a user, I want my edited data to be saved locally, so that I can access my corrected records even when offline.

#### Acceptance Criteria

1. WHEN a user saves edited food data THEN THE Meal_Photo_Edit_System SHALL persist the changes to the local Room database within 500 milliseconds
2. WHEN the app restarts THEN THE Meal_Photo_Edit_System SHALL load the most recent edited values from local storage
3. WHEN a user edits data THEN THE Meal_Photo_Edit_System SHALL store both the original AI-recognized values and the user-edited values
4. WHEN displaying data THEN THE Meal_Photo_Edit_System SHALL show user-edited values when available, falling back to original values otherwise

### Requirement 4: 云端数据同步

**User Story:** As a user, I want my edited data to sync to the cloud, so that my dietary records are backed up and accessible across devices.

#### Acceptance Criteria

1. WHEN a user saves edited food data with network connectivity THEN THE Meal_Photo_Edit_System SHALL initiate a sync request to the cloud server within 2 seconds
2. WHEN the sync request succeeds THEN THE Meal_Photo_Edit_System SHALL mark the local record as synced and display a success indicator
3. WHEN the sync request fails due to network issues THEN THE Meal_Photo_Edit_System SHALL queue the change for retry and display an "unsynced" indicator
4. WHEN network connectivity is restored THEN THE Meal_Photo_Edit_System SHALL automatically retry pending sync operations in chronological order
5. WHEN a conflict exists between local and cloud data THEN THE Meal_Photo_Edit_System SHALL use the most recently modified version based on timestamp

### Requirement 5: 用户界面反馈

**User Story:** As a user, I want clear visual feedback when editing and syncing data, so that I know the status of my changes.

#### Acceptance Criteria

1. WHEN a user is editing data THEN THE Meal_Photo_Edit_System SHALL highlight the editable fields with a distinct border color
2. WHEN data is being saved THEN THE Meal_Photo_Edit_System SHALL display a loading indicator and disable the save button
3. WHEN data is successfully saved and synced THEN THE Meal_Photo_Edit_System SHALL display a brief success toast message
4. WHEN data is saved locally but pending cloud sync THEN THE Meal_Photo_Edit_System SHALL display a cloud-with-arrow icon indicating pending sync
5. WHEN validation fails THEN THE Meal_Photo_Edit_System SHALL display an error message below the invalid field without clearing user input

