package com.rokid.nutrition.phone.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.rokid.nutrition.phone.data.entity.SnapshotFoodEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SnapshotFoodDao_Impl implements SnapshotFoodDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SnapshotFoodEntity> __insertionAdapterOfSnapshotFoodEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteFoodsForSnapshot;

  private final SharedSQLiteStatement __preparedStmtOfUpdateFoodNutrition;

  private final SharedSQLiteStatement __preparedStmtOfDeleteFood;

  public SnapshotFoodDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSnapshotFoodEntity = new EntityInsertionAdapter<SnapshotFoodEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `snapshot_foods` (`id`,`snapshotId`,`name`,`chineseName`,`originalWeightG`,`originalCaloriesKcal`,`originalProteinG`,`originalCarbsG`,`originalFatG`,`weightG`,`caloriesKcal`,`proteinG`,`carbsG`,`fatG`,`confidence`,`cookingMethod`,`isEdited`,`editedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SnapshotFoodEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSnapshotId());
        statement.bindString(3, entity.getName());
        if (entity.getChineseName() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getChineseName());
        }
        if (entity.getOriginalWeightG() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getOriginalWeightG());
        }
        if (entity.getOriginalCaloriesKcal() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getOriginalCaloriesKcal());
        }
        if (entity.getOriginalProteinG() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getOriginalProteinG());
        }
        if (entity.getOriginalCarbsG() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getOriginalCarbsG());
        }
        if (entity.getOriginalFatG() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getOriginalFatG());
        }
        statement.bindDouble(10, entity.getWeightG());
        statement.bindDouble(11, entity.getCaloriesKcal());
        if (entity.getProteinG() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getProteinG());
        }
        if (entity.getCarbsG() == null) {
          statement.bindNull(13);
        } else {
          statement.bindDouble(13, entity.getCarbsG());
        }
        if (entity.getFatG() == null) {
          statement.bindNull(14);
        } else {
          statement.bindDouble(14, entity.getFatG());
        }
        statement.bindDouble(15, entity.getConfidence());
        if (entity.getCookingMethod() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getCookingMethod());
        }
        final int _tmp = entity.isEdited() ? 1 : 0;
        statement.bindLong(17, _tmp);
        if (entity.getEditedAt() == null) {
          statement.bindNull(18);
        } else {
          statement.bindLong(18, entity.getEditedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteFoodsForSnapshot = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM snapshot_foods WHERE snapshotId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateFoodNutrition = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE snapshot_foods SET \n"
                + "            weightG = ?,\n"
                + "            caloriesKcal = ?,\n"
                + "            proteinG = ?,\n"
                + "            carbsG = ?,\n"
                + "            fatG = ?,\n"
                + "            isEdited = 1,\n"
                + "            editedAt = ?\n"
                + "        WHERE id = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteFood = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM snapshot_foods WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object saveFoods(final List<SnapshotFoodEntity> foods,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSnapshotFoodEntity.insert(foods);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveFood(final SnapshotFoodEntity food,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSnapshotFoodEntity.insert(food);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteFoodsForSnapshot(final String snapshotId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteFoodsForSnapshot.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, snapshotId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteFoodsForSnapshot.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateFoodNutrition(final String foodId, final double weightG,
      final double caloriesKcal, final Double proteinG, final Double carbsG, final Double fatG,
      final long editedAt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateFoodNutrition.acquire();
        int _argIndex = 1;
        _stmt.bindDouble(_argIndex, weightG);
        _argIndex = 2;
        _stmt.bindDouble(_argIndex, caloriesKcal);
        _argIndex = 3;
        if (proteinG == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindDouble(_argIndex, proteinG);
        }
        _argIndex = 4;
        if (carbsG == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindDouble(_argIndex, carbsG);
        }
        _argIndex = 5;
        if (fatG == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindDouble(_argIndex, fatG);
        }
        _argIndex = 6;
        _stmt.bindLong(_argIndex, editedAt);
        _argIndex = 7;
        _stmt.bindString(_argIndex, foodId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateFoodNutrition.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteFood(final String foodId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteFood.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, foodId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteFood.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getFoodsForSnapshot(final String snapshotId,
      final Continuation<? super List<SnapshotFoodEntity>> $completion) {
    final String _sql = "SELECT * FROM snapshot_foods WHERE snapshotId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, snapshotId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SnapshotFoodEntity>>() {
      @Override
      @NonNull
      public List<SnapshotFoodEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSnapshotId = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChineseName = CursorUtil.getColumnIndexOrThrow(_cursor, "chineseName");
          final int _cursorIndexOfOriginalWeightG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalWeightG");
          final int _cursorIndexOfOriginalCaloriesKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCaloriesKcal");
          final int _cursorIndexOfOriginalProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalProteinG");
          final int _cursorIndexOfOriginalCarbsG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCarbsG");
          final int _cursorIndexOfOriginalFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalFatG");
          final int _cursorIndexOfWeightG = CursorUtil.getColumnIndexOrThrow(_cursor, "weightG");
          final int _cursorIndexOfCaloriesKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "caloriesKcal");
          final int _cursorIndexOfProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "proteinG");
          final int _cursorIndexOfCarbsG = CursorUtil.getColumnIndexOrThrow(_cursor, "carbsG");
          final int _cursorIndexOfFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "fatG");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfCookingMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingMethod");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final List<SnapshotFoodEntity> _result = new ArrayList<SnapshotFoodEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SnapshotFoodEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSnapshotId;
            _tmpSnapshotId = _cursor.getString(_cursorIndexOfSnapshotId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpChineseName;
            if (_cursor.isNull(_cursorIndexOfChineseName)) {
              _tmpChineseName = null;
            } else {
              _tmpChineseName = _cursor.getString(_cursorIndexOfChineseName);
            }
            final Double _tmpOriginalWeightG;
            if (_cursor.isNull(_cursorIndexOfOriginalWeightG)) {
              _tmpOriginalWeightG = null;
            } else {
              _tmpOriginalWeightG = _cursor.getDouble(_cursorIndexOfOriginalWeightG);
            }
            final Double _tmpOriginalCaloriesKcal;
            if (_cursor.isNull(_cursorIndexOfOriginalCaloriesKcal)) {
              _tmpOriginalCaloriesKcal = null;
            } else {
              _tmpOriginalCaloriesKcal = _cursor.getDouble(_cursorIndexOfOriginalCaloriesKcal);
            }
            final Double _tmpOriginalProteinG;
            if (_cursor.isNull(_cursorIndexOfOriginalProteinG)) {
              _tmpOriginalProteinG = null;
            } else {
              _tmpOriginalProteinG = _cursor.getDouble(_cursorIndexOfOriginalProteinG);
            }
            final Double _tmpOriginalCarbsG;
            if (_cursor.isNull(_cursorIndexOfOriginalCarbsG)) {
              _tmpOriginalCarbsG = null;
            } else {
              _tmpOriginalCarbsG = _cursor.getDouble(_cursorIndexOfOriginalCarbsG);
            }
            final Double _tmpOriginalFatG;
            if (_cursor.isNull(_cursorIndexOfOriginalFatG)) {
              _tmpOriginalFatG = null;
            } else {
              _tmpOriginalFatG = _cursor.getDouble(_cursorIndexOfOriginalFatG);
            }
            final double _tmpWeightG;
            _tmpWeightG = _cursor.getDouble(_cursorIndexOfWeightG);
            final double _tmpCaloriesKcal;
            _tmpCaloriesKcal = _cursor.getDouble(_cursorIndexOfCaloriesKcal);
            final Double _tmpProteinG;
            if (_cursor.isNull(_cursorIndexOfProteinG)) {
              _tmpProteinG = null;
            } else {
              _tmpProteinG = _cursor.getDouble(_cursorIndexOfProteinG);
            }
            final Double _tmpCarbsG;
            if (_cursor.isNull(_cursorIndexOfCarbsG)) {
              _tmpCarbsG = null;
            } else {
              _tmpCarbsG = _cursor.getDouble(_cursorIndexOfCarbsG);
            }
            final Double _tmpFatG;
            if (_cursor.isNull(_cursorIndexOfFatG)) {
              _tmpFatG = null;
            } else {
              _tmpFatG = _cursor.getDouble(_cursorIndexOfFatG);
            }
            final double _tmpConfidence;
            _tmpConfidence = _cursor.getDouble(_cursorIndexOfConfidence);
            final String _tmpCookingMethod;
            if (_cursor.isNull(_cursorIndexOfCookingMethod)) {
              _tmpCookingMethod = null;
            } else {
              _tmpCookingMethod = _cursor.getString(_cursorIndexOfCookingMethod);
            }
            final boolean _tmpIsEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp != 0;
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            _item = new SnapshotFoodEntity(_tmpId,_tmpSnapshotId,_tmpName,_tmpChineseName,_tmpOriginalWeightG,_tmpOriginalCaloriesKcal,_tmpOriginalProteinG,_tmpOriginalCarbsG,_tmpOriginalFatG,_tmpWeightG,_tmpCaloriesKcal,_tmpProteinG,_tmpCarbsG,_tmpFatG,_tmpConfidence,_tmpCookingMethod,_tmpIsEdited,_tmpEditedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getFoodById(final String foodId,
      final Continuation<? super SnapshotFoodEntity> $completion) {
    final String _sql = "SELECT * FROM snapshot_foods WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, foodId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SnapshotFoodEntity>() {
      @Override
      @Nullable
      public SnapshotFoodEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSnapshotId = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshotId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChineseName = CursorUtil.getColumnIndexOrThrow(_cursor, "chineseName");
          final int _cursorIndexOfOriginalWeightG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalWeightG");
          final int _cursorIndexOfOriginalCaloriesKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCaloriesKcal");
          final int _cursorIndexOfOriginalProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalProteinG");
          final int _cursorIndexOfOriginalCarbsG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalCarbsG");
          final int _cursorIndexOfOriginalFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "originalFatG");
          final int _cursorIndexOfWeightG = CursorUtil.getColumnIndexOrThrow(_cursor, "weightG");
          final int _cursorIndexOfCaloriesKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "caloriesKcal");
          final int _cursorIndexOfProteinG = CursorUtil.getColumnIndexOrThrow(_cursor, "proteinG");
          final int _cursorIndexOfCarbsG = CursorUtil.getColumnIndexOrThrow(_cursor, "carbsG");
          final int _cursorIndexOfFatG = CursorUtil.getColumnIndexOrThrow(_cursor, "fatG");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfCookingMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingMethod");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfEditedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "editedAt");
          final SnapshotFoodEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSnapshotId;
            _tmpSnapshotId = _cursor.getString(_cursorIndexOfSnapshotId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpChineseName;
            if (_cursor.isNull(_cursorIndexOfChineseName)) {
              _tmpChineseName = null;
            } else {
              _tmpChineseName = _cursor.getString(_cursorIndexOfChineseName);
            }
            final Double _tmpOriginalWeightG;
            if (_cursor.isNull(_cursorIndexOfOriginalWeightG)) {
              _tmpOriginalWeightG = null;
            } else {
              _tmpOriginalWeightG = _cursor.getDouble(_cursorIndexOfOriginalWeightG);
            }
            final Double _tmpOriginalCaloriesKcal;
            if (_cursor.isNull(_cursorIndexOfOriginalCaloriesKcal)) {
              _tmpOriginalCaloriesKcal = null;
            } else {
              _tmpOriginalCaloriesKcal = _cursor.getDouble(_cursorIndexOfOriginalCaloriesKcal);
            }
            final Double _tmpOriginalProteinG;
            if (_cursor.isNull(_cursorIndexOfOriginalProteinG)) {
              _tmpOriginalProteinG = null;
            } else {
              _tmpOriginalProteinG = _cursor.getDouble(_cursorIndexOfOriginalProteinG);
            }
            final Double _tmpOriginalCarbsG;
            if (_cursor.isNull(_cursorIndexOfOriginalCarbsG)) {
              _tmpOriginalCarbsG = null;
            } else {
              _tmpOriginalCarbsG = _cursor.getDouble(_cursorIndexOfOriginalCarbsG);
            }
            final Double _tmpOriginalFatG;
            if (_cursor.isNull(_cursorIndexOfOriginalFatG)) {
              _tmpOriginalFatG = null;
            } else {
              _tmpOriginalFatG = _cursor.getDouble(_cursorIndexOfOriginalFatG);
            }
            final double _tmpWeightG;
            _tmpWeightG = _cursor.getDouble(_cursorIndexOfWeightG);
            final double _tmpCaloriesKcal;
            _tmpCaloriesKcal = _cursor.getDouble(_cursorIndexOfCaloriesKcal);
            final Double _tmpProteinG;
            if (_cursor.isNull(_cursorIndexOfProteinG)) {
              _tmpProteinG = null;
            } else {
              _tmpProteinG = _cursor.getDouble(_cursorIndexOfProteinG);
            }
            final Double _tmpCarbsG;
            if (_cursor.isNull(_cursorIndexOfCarbsG)) {
              _tmpCarbsG = null;
            } else {
              _tmpCarbsG = _cursor.getDouble(_cursorIndexOfCarbsG);
            }
            final Double _tmpFatG;
            if (_cursor.isNull(_cursorIndexOfFatG)) {
              _tmpFatG = null;
            } else {
              _tmpFatG = _cursor.getDouble(_cursorIndexOfFatG);
            }
            final double _tmpConfidence;
            _tmpConfidence = _cursor.getDouble(_cursorIndexOfConfidence);
            final String _tmpCookingMethod;
            if (_cursor.isNull(_cursorIndexOfCookingMethod)) {
              _tmpCookingMethod = null;
            } else {
              _tmpCookingMethod = _cursor.getString(_cursorIndexOfCookingMethod);
            }
            final boolean _tmpIsEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp != 0;
            final Long _tmpEditedAt;
            if (_cursor.isNull(_cursorIndexOfEditedAt)) {
              _tmpEditedAt = null;
            } else {
              _tmpEditedAt = _cursor.getLong(_cursorIndexOfEditedAt);
            }
            _result = new SnapshotFoodEntity(_tmpId,_tmpSnapshotId,_tmpName,_tmpChineseName,_tmpOriginalWeightG,_tmpOriginalCaloriesKcal,_tmpOriginalProteinG,_tmpOriginalCarbsG,_tmpOriginalFatG,_tmpWeightG,_tmpCaloriesKcal,_tmpProteinG,_tmpCarbsG,_tmpFatG,_tmpConfidence,_tmpCookingMethod,_tmpIsEdited,_tmpEditedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
