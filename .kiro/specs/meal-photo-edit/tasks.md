# Implementation Plan

## 1. Database Schema Updates

- [x] 1.1 Update MealSnapshotEntity with edit tracking fields
  - Add `isEdited: Boolean` and `lastSyncedAt: Long?` fields
  - Create Room migration for existing data
  - _Requirements: 3.1, 3.2_

- [x] 1.2 Update SnapshotFoodEntity with dual value storage
  - Add original value fields: `originalWeightG`, `originalCaloriesKcal`, `originalProteinG`, `originalCarbsG`, `originalFatG`
  - Add edit tracking: `isEdited: Boolean`, `editedAt: Long?`
  - Create Room migration
  - _Requirements: 3.3_

- [x] 1.3 Create SyncQueueEntity for offline sync
  - Define entity with `id`, `operationType`, `targetId`, `payload`, `createdAt`, `retryCount`, `lastError`, `status`
  - Create SyncQueueDao with insert, query, update, delete operations
  - _Requirements: 4.3, 4.4_

- [ ]* 1.4 Write property test for dual value storage
  - **Property 8: Dual Value Storage**
  - **Validates: Requirements 3.3**

## 2. Core Utility Classes

- [x] 2.1 Implement NutritionCalculator
  - Create `recalculateProportionally(original: NutritionValues, originalWeight: Double, newWeight: Double): NutritionValues`
  - Handle edge cases (zero weight, very small ratios)
  - _Requirements: 2.2_

- [ ]* 2.2 Write property test for proportional recalculation
  - **Property 3: Proportional Nutrition Recalculation**
  - **Validates: Requirements 2.2**

- [x] 2.3 Implement FoodItemValidator
  - Create `validate(updates: FoodItemUpdates): ValidationResult`
  - Check non-negative values for weight, calories, protein, carbs, fat
  - Return field-specific error messages
  - _Requirements: 2.4_

- [ ]* 2.4 Write property test for non-negative validation
  - **Property 5: Non-Negative Validation**
  - **Validates: Requirements 2.4**

## 3. Repository Layer

- [x] 3.1 Implement MealEditRepository
  - Create `getMealSnapshot(snapshotId: String): MealSnapshotWithFoods?`
  - Create `updateFoodItem(foodId: String, updates: FoodItemUpdates): Result<Unit>`
  - Implement dual value storage logic (preserve original, update current)
  - _Requirements: 2.2, 2.3, 3.1, 3.3_

- [ ]* 3.2 Write property test for persistence round-trip
  - **Property 7: Persistence Round-Trip**
  - **Validates: Requirements 3.2**

- [x] 3.3 Implement PhotoStorageRepository
  - Create `getPhotoUri(snapshotId: String): String?` with local/cloud fallback
  - Create `savePhotoToGallery(imageUri: String): Result<Uri>`
  - Create `downloadPhotoFromCloud(imageUrl: String): Result<String>`
  - _Requirements: 1.1, 1.4, 1.6_

- [ ]* 3.4 Write property test for photo fallback behavior
  - **Property 2: Photo Fallback Behavior**
  - **Validates: Requirements 1.4**

## 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## 5. Sync Layer

- [x] 5.1 Implement SyncQueue operations
  - Create `enqueue(operation: SyncOperation)`
  - Create `dequeueNext(): SyncOperation?` (chronological order)
  - Create `markCompleted(id: String)` and `markFailed(id: String, error: String)`
  - _Requirements: 4.3, 4.4_

- [ ]* 5.2 Write property test for chronological sync order
  - **Property 12: Chronological Sync Order**
  - **Validates: Requirements 4.4**

- [x] 5.3 Implement SyncManager
  - Create `enqueueSyncOperation(operation: SyncOperation)`
  - Create `processPendingOperations()` with network check
  - Create `observeSyncStatus(): Flow<SyncStatus>`
  - Implement retry logic with exponential backoff
  - _Requirements: 4.1, 4.2, 4.3_

- [ ]* 5.4 Write property test for sync success state update
  - **Property 10: Sync Success State Update**
  - **Validates: Requirements 4.2**

- [ ]* 5.5 Write property test for sync failure queue entry
  - **Property 11: Sync Failure Queue Entry**
  - **Validates: Requirements 4.3**

- [x] 5.6 Implement ConflictResolver
  - Create `resolve(local: FoodItemData, remote: FoodItemData): FoodItemData`
  - Use `editedAt` timestamp for conflict resolution
  - _Requirements: 4.5_

- [ ]* 5.7 Write property test for conflict resolution
  - **Property 13: Conflict Resolution by Timestamp**
  - **Validates: Requirements 4.5**

## 6. Backend API Updates

- [x] 6.1 Add update food endpoint to ApiService
  - Add `updateFood(request: UpdateFoodRequest): UpdateFoodResponse`
  - Define request/response models
  - _Requirements: 4.1_

- [x] 6.2 Implement backend endpoint for food updates
  - Create `PUT /api/v1/meal/food/{food_id}` endpoint
  - Accept weight, calories, protein, carbs, fat updates
  - Store `edited_at` timestamp
  - _Requirements: 4.1, 4.2_

## 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## 8. ViewModel Layer

- [x] 8.1 Implement FoodDetailViewModel
  - Create state management with `FoodDetailUiState`
  - Implement `loadMealSnapshot(snapshotId: String)`
  - Implement `editFoodItem(foodId: String, updates: FoodItemUpdates)`
  - Implement `saveChanges()` with validation and sync
  - Implement `cancelEdit()` to restore original values
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 8.2 Write property test for edit cancellation
  - **Property 6: Edit Cancellation Round-Trip**
  - **Validates: Requirements 2.5**

- [ ]* 8.3 Write property test for manual override preservation
  - **Property 4: Manual Override Preservation**
  - **Validates: Requirements 2.3**

- [ ]* 8.4 Write property test for display value selection
  - **Property 9: Display Value Selection**
  - **Validates: Requirements 3.4**

## 9. UI Components

- [x] 9.1 Implement PhotoViewerDialog
  - Create full-screen photo viewer with pinch-to-zoom
  - Add download button with gallery save functionality
  - Handle loading and error states
  - _Requirements: 1.5, 1.6_

- [x] 9.2 Implement EditFoodDialog
  - Create editable form for weight, calories, protein, carbs, fat
  - Add toggle for proportional recalculation mode
  - Display validation errors inline
  - Add save/cancel buttons with loading state
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 5.1, 5.2, 5.5_

- [x] 9.3 Update FoodDetailScreen
  - Add photo display at top with tap-to-fullscreen
  - Make food items tappable to open edit dialog
  - Add sync status indicator
  - Show edited indicator for modified items
  - _Requirements: 1.2, 2.1, 5.3, 5.4_

- [x] 9.4 Update HistoryScreen
  - Add thumbnail photo for each meal record
  - Handle missing photos with placeholder
  - _Requirements: 1.3, 1.4_

## 10. Photo Storage Integration

- [ ] 10.1 Update photo capture flow to save local path
  - Modify existing capture logic to store `localImagePath` in MealSnapshotEntity
  - Ensure path is saved alongside cloud URL
  - _Requirements: 1.1_

- [ ]* 10.2 Write property test for photo path association
  - **Property 1: Photo Path Association**
  - **Validates: Requirements 1.1**

## 11. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

