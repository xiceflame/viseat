# Requirements Document

## Introduction

This document specifies the requirements for improving the meal monitoring experience in the phone app. The improvements include UI reorganization (removing quick actions, repositioning meal monitoring), fixing the meal session start flow to capture baseline photos, implementing swipe-to-end meal functionality, adding long-press delete for meal records with full data synchronization, and enabling food data editing in the latest recognition section.

## Glossary

- **Phone_App**: The Android companion application that connects to Rokid glasses via Bluetooth
- **Meal_Session**: A tracked eating period with baseline photo, nutrition data, and duration
- **Baseline_Photo**: The initial photo taken when starting a meal session, used as reference for calorie consumption calculation
- **Quick_Actions_Section**: The UI card containing shortcut buttons (currently "拍照识别")
- **Meal_Status_Card**: The UI card showing current meal session status and controls
- **Latest_Recognition_Section**: The UI card showing the most recent food recognition result
- **Recent_Meals_Section**: The UI list showing recent meal session history
- **Statistics_Repository**: The local data store tracking daily/weekly nutrition statistics
- **Daily_Nutrition_Tracker**: The component tracking cumulative daily nutrition intake

## Requirements

### Requirement 1

**User Story:** As a user, I want the meal monitoring controls to be prominently displayed in place of the quick actions section, so that I can easily start and manage my meal sessions.

#### Acceptance Criteria

1. WHEN the HomeScreen is displayed THEN the Phone_App SHALL NOT render the Quick_Actions_Section
2. WHEN the HomeScreen is displayed THEN the Phone_App SHALL render the Meal_Status_Card in the position previously occupied by the Quick_Actions_Section
3. WHEN the glasses are connected THEN the Phone_App SHALL display the Meal_Status_Card with "开始用餐" button
4. WHEN a meal session is active THEN the Phone_App SHALL display the Meal_Status_Card with session duration and "结束用餐" button

### Requirement 2

**User Story:** As a user, I want the app to automatically take a baseline photo when I start a meal session, so that the system can accurately calculate my calorie consumption during the meal.

#### Acceptance Criteria

1. WHEN a user clicks "开始用餐" button THEN the Phone_App SHALL trigger the glasses to capture a baseline photo
2. WHEN the baseline photo is captured THEN the Phone_App SHALL upload the photo to the backend for analysis
3. WHEN the backend returns the baseline analysis THEN the Phone_App SHALL create a meal session with the baseline nutrition data
4. WHEN the baseline photo capture fails THEN the Phone_App SHALL display an error message and NOT start the meal session
5. WHEN the glasses are not connected THEN the Phone_App SHALL disable the "开始用餐" button and display a connection prompt
6. WHILE the baseline photo is being processed THEN the Phone_App SHALL display a loading indicator with status message

### Requirement 3

**User Story:** As a user, I want to end my meal session by swiping on the glasses, so that I can conveniently finish tracking without using my phone.

#### Acceptance Criteria

1. WHEN the glasses detect a swipe-back gesture during an active meal session THEN the Phone_App SHALL receive an "end_meal" command via Bluetooth
2. WHEN the Phone_App receives an "end_meal" command THEN the Phone_App SHALL call the backend to end the session with full context data
3. WHEN the meal session ends successfully THEN the Phone_App SHALL update the UI to show "空闲" status
4. WHEN the meal session ends successfully THEN the Phone_App SHALL update the Daily_Nutrition_Tracker with consumed calories
5. IF the backend call fails THEN the Phone_App SHALL still end the local session and display the locally calculated summary

### Requirement 4

**User Story:** As a user, I want to delete meal records by long-pressing on them, so that I can remove incorrect or unwanted entries from my history.

#### Acceptance Criteria

1. WHEN a user long-presses on a meal record in the Recent_Meals_Section THEN the Phone_App SHALL display a delete confirmation dialog
2. WHEN the user confirms deletion THEN the Phone_App SHALL delete the meal session from the local database
3. WHEN the user confirms deletion THEN the Phone_App SHALL call the backend API to delete the meal session
4. WHEN a meal session is deleted THEN the Phone_App SHALL subtract the session's calories from the Statistics_Repository
5. WHEN a meal session is deleted THEN the Phone_App SHALL subtract the session's calories from the Daily_Nutrition_Tracker if the session was from today
6. WHEN the backend deletion fails THEN the Phone_App SHALL queue the deletion for retry and display a warning
7. WHEN the user cancels deletion THEN the Phone_App SHALL dismiss the dialog and maintain the current state

### Requirement 5

**User Story:** As a user, I want to edit food data in the latest recognition section, so that I can correct any inaccuracies immediately after scanning.

#### Acceptance Criteria

1. WHEN a user taps on the Latest_Recognition_Section THEN the Phone_App SHALL navigate to the food detail screen with editing capability
2. WHEN the food detail screen is displayed THEN the Phone_App SHALL show an edit button for each food item
3. WHEN a user edits food data (name, weight, calories, protein, carbs, fat) THEN the Phone_App SHALL validate the input values
4. WHEN the user saves edited food data THEN the Phone_App SHALL update the local database immediately
5. WHEN the user saves edited food data THEN the Phone_App SHALL sync the changes to the backend
6. WHEN the user saves edited food data THEN the Phone_App SHALL update the Latest_Recognition_Section to reflect the changes
7. IF the backend sync fails THEN the Phone_App SHALL queue the edit for retry and mark the item as "pending sync"

### Requirement 6

**User Story:** As a user, I want the latest recognition section to support the same editing features as the meal history, so that I have a consistent editing experience across the app.

#### Acceptance Criteria

1. WHEN displaying the Latest_Recognition_Section THEN the Phone_App SHALL show a visual indicator that the item is editable
2. WHEN the user edits food in the Latest_Recognition_Section THEN the Phone_App SHALL use the same EditFoodDialog component as the history screen
3. WHEN food data is edited THEN the Phone_App SHALL recalculate and display the updated total nutrition values
4. WHEN food data is edited in an active meal session THEN the Phone_App SHALL update the session's baseline data accordingly
