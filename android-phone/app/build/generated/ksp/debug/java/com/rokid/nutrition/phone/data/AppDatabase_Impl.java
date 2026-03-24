package com.rokid.nutrition.phone.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.rokid.nutrition.phone.data.dao.MealSessionDao;
import com.rokid.nutrition.phone.data.dao.MealSessionDao_Impl;
import com.rokid.nutrition.phone.data.dao.MealSnapshotDao;
import com.rokid.nutrition.phone.data.dao.MealSnapshotDao_Impl;
import com.rokid.nutrition.phone.data.dao.SnapshotFoodDao;
import com.rokid.nutrition.phone.data.dao.SnapshotFoodDao_Impl;
import com.rokid.nutrition.phone.data.dao.SyncQueueDao;
import com.rokid.nutrition.phone.data.dao.SyncQueueDao_Impl;
import com.rokid.nutrition.phone.data.dao.UserProfileDao;
import com.rokid.nutrition.phone.data.dao.UserProfileDao_Impl;
import com.rokid.nutrition.phone.data.dao.WeightEntryDao;
import com.rokid.nutrition.phone.data.dao.WeightEntryDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile UserProfileDao _userProfileDao;

  private volatile MealSessionDao _mealSessionDao;

  private volatile MealSnapshotDao _mealSnapshotDao;

  private volatile SnapshotFoodDao _snapshotFoodDao;

  private volatile SyncQueueDao _syncQueueDao;

  private volatile WeightEntryDao _weightEntryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profiles` (`id` TEXT NOT NULL, `nickname` TEXT, `age` INTEGER NOT NULL, `gender` TEXT NOT NULL, `birthDate` TEXT, `height` REAL NOT NULL, `weight` REAL NOT NULL, `bmi` REAL NOT NULL, `activityLevel` TEXT NOT NULL, `healthGoal` TEXT NOT NULL, `targetWeight` REAL, `targetDate` TEXT, `dietType` TEXT NOT NULL, `allergens` TEXT NOT NULL, `healthConditions` TEXT NOT NULL, `dietaryPreferences` TEXT NOT NULL, `isOnboardingCompleted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `meal_sessions` (`sessionId` TEXT NOT NULL, `userId` TEXT NOT NULL, `mealType` TEXT NOT NULL, `status` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `autoCaptureInterval` INTEGER NOT NULL, `totalServedKcal` REAL, `totalConsumedKcal` REAL, `consumptionRatio` REAL, `durationMinutes` REAL, `report` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`sessionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `meal_snapshots` (`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, `localImagePath` TEXT, `capturedAt` INTEGER NOT NULL, `model` TEXT NOT NULL, `rawJson` TEXT, `totalKcal` REAL NOT NULL, `isEdited` INTEGER NOT NULL, `lastSyncedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`sessionId`) REFERENCES `meal_sessions`(`sessionId`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_snapshots_sessionId` ON `meal_snapshots` (`sessionId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `snapshot_foods` (`id` TEXT NOT NULL, `snapshotId` TEXT NOT NULL, `name` TEXT NOT NULL, `chineseName` TEXT, `category` TEXT, `originalWeightG` REAL, `originalCaloriesKcal` REAL, `originalProteinG` REAL, `originalCarbsG` REAL, `originalFatG` REAL, `weightG` REAL NOT NULL, `caloriesKcal` REAL NOT NULL, `proteinG` REAL, `carbsG` REAL, `fatG` REAL, `confidence` REAL NOT NULL, `cookingMethod` TEXT, `isEdited` INTEGER NOT NULL, `editedAt` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`snapshotId`) REFERENCES `meal_snapshots`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_snapshot_foods_snapshotId` ON `snapshot_foods` (`snapshotId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_queue` (`id` TEXT NOT NULL, `operationType` TEXT NOT NULL, `targetId` TEXT NOT NULL, `payload` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL, `lastError` TEXT, `status` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_entries` (`id` TEXT NOT NULL, `weight` REAL NOT NULL, `note` TEXT, `recordedAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '94a80a18b94cfed64f3607b3b5be37ec')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `user_profiles`");
        db.execSQL("DROP TABLE IF EXISTS `meal_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `meal_snapshots`");
        db.execSQL("DROP TABLE IF EXISTS `snapshot_foods`");
        db.execSQL("DROP TABLE IF EXISTS `sync_queue`");
        db.execSQL("DROP TABLE IF EXISTS `weight_entries`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsUserProfiles = new HashMap<String, TableInfo.Column>(19);
        _columnsUserProfiles.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("nickname", new TableInfo.Column("nickname", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("age", new TableInfo.Column("age", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("gender", new TableInfo.Column("gender", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("birthDate", new TableInfo.Column("birthDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("height", new TableInfo.Column("height", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("weight", new TableInfo.Column("weight", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("bmi", new TableInfo.Column("bmi", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("activityLevel", new TableInfo.Column("activityLevel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("healthGoal", new TableInfo.Column("healthGoal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("targetWeight", new TableInfo.Column("targetWeight", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("targetDate", new TableInfo.Column("targetDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("dietType", new TableInfo.Column("dietType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("allergens", new TableInfo.Column("allergens", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("healthConditions", new TableInfo.Column("healthConditions", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("dietaryPreferences", new TableInfo.Column("dietaryPreferences", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("isOnboardingCompleted", new TableInfo.Column("isOnboardingCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfiles.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserProfiles = new TableInfo("user_profiles", _columnsUserProfiles, _foreignKeysUserProfiles, _indicesUserProfiles);
        final TableInfo _existingUserProfiles = TableInfo.read(db, "user_profiles");
        if (!_infoUserProfiles.equals(_existingUserProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "user_profiles(com.rokid.nutrition.phone.data.entity.UserProfileEntity).\n"
                  + " Expected:\n" + _infoUserProfiles + "\n"
                  + " Found:\n" + _existingUserProfiles);
        }
        final HashMap<String, TableInfo.Column> _columnsMealSessions = new HashMap<String, TableInfo.Column>(14);
        _columnsMealSessions.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("mealType", new TableInfo.Column("mealType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("endTime", new TableInfo.Column("endTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("autoCaptureInterval", new TableInfo.Column("autoCaptureInterval", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("totalServedKcal", new TableInfo.Column("totalServedKcal", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("totalConsumedKcal", new TableInfo.Column("totalConsumedKcal", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("consumptionRatio", new TableInfo.Column("consumptionRatio", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("durationMinutes", new TableInfo.Column("durationMinutes", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("report", new TableInfo.Column("report", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSessions.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMealSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMealSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMealSessions = new TableInfo("meal_sessions", _columnsMealSessions, _foreignKeysMealSessions, _indicesMealSessions);
        final TableInfo _existingMealSessions = TableInfo.read(db, "meal_sessions");
        if (!_infoMealSessions.equals(_existingMealSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "meal_sessions(com.rokid.nutrition.phone.data.entity.MealSessionEntity).\n"
                  + " Expected:\n" + _infoMealSessions + "\n"
                  + " Found:\n" + _existingMealSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsMealSnapshots = new HashMap<String, TableInfo.Column>(10);
        _columnsMealSnapshots.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("localImagePath", new TableInfo.Column("localImagePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("capturedAt", new TableInfo.Column("capturedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("model", new TableInfo.Column("model", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("rawJson", new TableInfo.Column("rawJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("totalKcal", new TableInfo.Column("totalKcal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("isEdited", new TableInfo.Column("isEdited", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMealSnapshots.put("lastSyncedAt", new TableInfo.Column("lastSyncedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMealSnapshots = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysMealSnapshots.add(new TableInfo.ForeignKey("meal_sessions", "CASCADE", "NO ACTION", Arrays.asList("sessionId"), Arrays.asList("sessionId")));
        final HashSet<TableInfo.Index> _indicesMealSnapshots = new HashSet<TableInfo.Index>(1);
        _indicesMealSnapshots.add(new TableInfo.Index("index_meal_snapshots_sessionId", false, Arrays.asList("sessionId"), Arrays.asList("ASC")));
        final TableInfo _infoMealSnapshots = new TableInfo("meal_snapshots", _columnsMealSnapshots, _foreignKeysMealSnapshots, _indicesMealSnapshots);
        final TableInfo _existingMealSnapshots = TableInfo.read(db, "meal_snapshots");
        if (!_infoMealSnapshots.equals(_existingMealSnapshots)) {
          return new RoomOpenHelper.ValidationResult(false, "meal_snapshots(com.rokid.nutrition.phone.data.entity.MealSnapshotEntity).\n"
                  + " Expected:\n" + _infoMealSnapshots + "\n"
                  + " Found:\n" + _existingMealSnapshots);
        }
        final HashMap<String, TableInfo.Column> _columnsSnapshotFoods = new HashMap<String, TableInfo.Column>(19);
        _columnsSnapshotFoods.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("snapshotId", new TableInfo.Column("snapshotId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("chineseName", new TableInfo.Column("chineseName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("category", new TableInfo.Column("category", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("originalWeightG", new TableInfo.Column("originalWeightG", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("originalCaloriesKcal", new TableInfo.Column("originalCaloriesKcal", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("originalProteinG", new TableInfo.Column("originalProteinG", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("originalCarbsG", new TableInfo.Column("originalCarbsG", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("originalFatG", new TableInfo.Column("originalFatG", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("weightG", new TableInfo.Column("weightG", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("caloriesKcal", new TableInfo.Column("caloriesKcal", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("proteinG", new TableInfo.Column("proteinG", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("carbsG", new TableInfo.Column("carbsG", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("fatG", new TableInfo.Column("fatG", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("confidence", new TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("cookingMethod", new TableInfo.Column("cookingMethod", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("isEdited", new TableInfo.Column("isEdited", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSnapshotFoods.put("editedAt", new TableInfo.Column("editedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSnapshotFoods = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysSnapshotFoods.add(new TableInfo.ForeignKey("meal_snapshots", "CASCADE", "NO ACTION", Arrays.asList("snapshotId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSnapshotFoods = new HashSet<TableInfo.Index>(1);
        _indicesSnapshotFoods.add(new TableInfo.Index("index_snapshot_foods_snapshotId", false, Arrays.asList("snapshotId"), Arrays.asList("ASC")));
        final TableInfo _infoSnapshotFoods = new TableInfo("snapshot_foods", _columnsSnapshotFoods, _foreignKeysSnapshotFoods, _indicesSnapshotFoods);
        final TableInfo _existingSnapshotFoods = TableInfo.read(db, "snapshot_foods");
        if (!_infoSnapshotFoods.equals(_existingSnapshotFoods)) {
          return new RoomOpenHelper.ValidationResult(false, "snapshot_foods(com.rokid.nutrition.phone.data.entity.SnapshotFoodEntity).\n"
                  + " Expected:\n" + _infoSnapshotFoods + "\n"
                  + " Found:\n" + _existingSnapshotFoods);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncQueue = new HashMap<String, TableInfo.Column>(8);
        _columnsSyncQueue.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("operationType", new TableInfo.Column("operationType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("targetId", new TableInfo.Column("targetId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("payload", new TableInfo.Column("payload", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("retryCount", new TableInfo.Column("retryCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("lastError", new TableInfo.Column("lastError", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncQueue.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncQueue = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncQueue = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncQueue = new TableInfo("sync_queue", _columnsSyncQueue, _foreignKeysSyncQueue, _indicesSyncQueue);
        final TableInfo _existingSyncQueue = TableInfo.read(db, "sync_queue");
        if (!_infoSyncQueue.equals(_existingSyncQueue)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_queue(com.rokid.nutrition.phone.data.entity.SyncQueueEntity).\n"
                  + " Expected:\n" + _infoSyncQueue + "\n"
                  + " Found:\n" + _existingSyncQueue);
        }
        final HashMap<String, TableInfo.Column> _columnsWeightEntries = new HashMap<String, TableInfo.Column>(5);
        _columnsWeightEntries.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightEntries.put("weight", new TableInfo.Column("weight", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightEntries.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightEntries.put("recordedAt", new TableInfo.Column("recordedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeightEntries.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWeightEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWeightEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWeightEntries = new TableInfo("weight_entries", _columnsWeightEntries, _foreignKeysWeightEntries, _indicesWeightEntries);
        final TableInfo _existingWeightEntries = TableInfo.read(db, "weight_entries");
        if (!_infoWeightEntries.equals(_existingWeightEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "weight_entries(com.rokid.nutrition.phone.data.entity.WeightEntryEntity).\n"
                  + " Expected:\n" + _infoWeightEntries + "\n"
                  + " Found:\n" + _existingWeightEntries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "94a80a18b94cfed64f3607b3b5be37ec", "a3e7680233fe778c831c8403bfc790a3");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "user_profiles","meal_sessions","meal_snapshots","snapshot_foods","sync_queue","weight_entries");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `user_profiles`");
      _db.execSQL("DELETE FROM `meal_sessions`");
      _db.execSQL("DELETE FROM `meal_snapshots`");
      _db.execSQL("DELETE FROM `snapshot_foods`");
      _db.execSQL("DELETE FROM `sync_queue`");
      _db.execSQL("DELETE FROM `weight_entries`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(UserProfileDao.class, UserProfileDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MealSessionDao.class, MealSessionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MealSnapshotDao.class, MealSnapshotDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SnapshotFoodDao.class, SnapshotFoodDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncQueueDao.class, SyncQueueDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WeightEntryDao.class, WeightEntryDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public UserProfileDao userProfileDao() {
    if (_userProfileDao != null) {
      return _userProfileDao;
    } else {
      synchronized(this) {
        if(_userProfileDao == null) {
          _userProfileDao = new UserProfileDao_Impl(this);
        }
        return _userProfileDao;
      }
    }
  }

  @Override
  public MealSessionDao mealSessionDao() {
    if (_mealSessionDao != null) {
      return _mealSessionDao;
    } else {
      synchronized(this) {
        if(_mealSessionDao == null) {
          _mealSessionDao = new MealSessionDao_Impl(this);
        }
        return _mealSessionDao;
      }
    }
  }

  @Override
  public MealSnapshotDao mealSnapshotDao() {
    if (_mealSnapshotDao != null) {
      return _mealSnapshotDao;
    } else {
      synchronized(this) {
        if(_mealSnapshotDao == null) {
          _mealSnapshotDao = new MealSnapshotDao_Impl(this);
        }
        return _mealSnapshotDao;
      }
    }
  }

  @Override
  public SnapshotFoodDao snapshotFoodDao() {
    if (_snapshotFoodDao != null) {
      return _snapshotFoodDao;
    } else {
      synchronized(this) {
        if(_snapshotFoodDao == null) {
          _snapshotFoodDao = new SnapshotFoodDao_Impl(this);
        }
        return _snapshotFoodDao;
      }
    }
  }

  @Override
  public SyncQueueDao syncQueueDao() {
    if (_syncQueueDao != null) {
      return _syncQueueDao;
    } else {
      synchronized(this) {
        if(_syncQueueDao == null) {
          _syncQueueDao = new SyncQueueDao_Impl(this);
        }
        return _syncQueueDao;
      }
    }
  }

  @Override
  public WeightEntryDao weightEntryDao() {
    if (_weightEntryDao != null) {
      return _weightEntryDao;
    } else {
      synchronized(this) {
        if(_weightEntryDao == null) {
          _weightEntryDao = new WeightEntryDao_Impl(this);
        }
        return _weightEntryDao;
      }
    }
  }
}
