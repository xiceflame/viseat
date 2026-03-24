# Implementation Plan

- [x] 1. Create DebugModeSection component
  - [x] 1.1 Create DebugModeSection composable with collapsible card UI
    - Implement expandable/collapsible card with icon and title
    - Add "Debug Log" and "Demo Mode" options with appropriate icons
    - Handle expand/collapse state with animation
    - _Requirements: 1.1, 1.2, 5.1, 5.2, 5.3_

  - [x] 1.2 Update ProfileScreen to include DebugModeSection
    - Add DebugModeSection at the bottom of ProfileScreen
    - Remove existing debug log item from SettingsSection
    - Add navigation callbacks for debug log and demo mode
    - _Requirements: 1.1, 1.3, 1.4_

  - [ ]* 1.3 Write unit tests for DebugModeSection
    - Test expanded and collapsed states
    - Test click handlers are invoked correctly
    - _Requirements: 1.1, 1.2, 5.1, 5.2, 5.3_

- [x] 2. Update Navigation for Demo Mode access
  - [x] 2.1 Update MainNavigation to pass demo mode callback to ProfileScreen
    - Add onNavigateToDemoMode parameter to ProfileScreen call
    - Wire up navigation to Screen.Demo.route
    - _Requirements: 1.4_

  - [x] 2.2 Verify navigation flow from ProfileScreen to DemoScreen
    - Ensure back navigation works correctly
    - _Requirements: 1.3, 1.4_

- [x] 3. Sync demo JSON files from glasses app
  - [x] 3.1 Copy latest demo JSON files to phone app assets
    - Copy meal_start_response.json, meal_middle_response.json, meal_end_response.json
    - Copy coke_response.json, chips_response.json
    - Update DemoDataRepository to use synced files
    - _Requirements: 2.1, 2.2_

  - [ ]* 3.2 Write property test for JSON parsing round trip
    - **Property 1: Demo JSON Parsing Round Trip**
    - **Validates: Requirements 2.2**
    - Test that parsing and re-serializing produces equivalent data
    - _Requirements: 2.2_

- [x] 4. Fix image rotation for all photo displays
  - [x] 4.1 Update MealPhotoCard in FoodDetailScreen
    - Apply graphicsLayer { rotationZ = -90f } to AsyncImage
    - Verify rotation displays correctly
    - _Requirements: 3.1, 3.2_

  - [x] 4.2 Update PhotoViewerDialog with rotation
    - Apply same rotation transformation to full-screen photo view
    - _Requirements: 3.3_

  - [x] 4.3 Update photo thumbnails in history lists
    - Apply rotation to any photo thumbnails in HomeScreen or StatsScreen
    - _Requirements: 3.4_

  - [ ]* 4.4 Write UI tests for image rotation
    - Verify rotation modifier is applied in all contexts
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 5. Ensure meal detail shows photo correctly
  - [x] 5.1 Verify EnhancedFoodDetailScreen displays photo at top
    - Confirm MealPhotoCard is rendered when photoUri is available
    - _Requirements: 4.1, 4.2_

  - [x] 5.2 Handle missing photo gracefully
    - Ensure no crash or error when photoUri is null
    - Hide photo section or show placeholder
    - _Requirements: 4.4_

  - [x] 5.3 Verify photo tap opens PhotoViewerDialog
    - Confirm click handler triggers showPhotoViewer state
    - _Requirements: 4.3_

- [x] 6. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Final integration and cleanup
  - [x] 7.1 Test complete flow from ProfileScreen
    - Navigate to Debug Log and back
    - Navigate to Demo Mode and back
    - Run demo mode scenarios
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 7.2 Verify image rotation in all screens
    - Check FoodDetailScreen photo display
    - Check PhotoViewerDialog display
    - Check any thumbnail displays
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2_

- [x] 8. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
