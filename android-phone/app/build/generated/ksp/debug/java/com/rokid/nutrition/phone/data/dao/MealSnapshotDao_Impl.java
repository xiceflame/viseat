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
import com.rokid.nutrition.phone.data.entity.MealSnapshotEntity;
import java.lang.Class;
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
public final class MealSnapshotDao_Impl implements MealSnapshotDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MealSnapshotEntity> __insertionAdapterOfMealSnapshotEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSnapshotsForSession;

  public MealSnapshotDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMealSnapshotEntity = new EntityInsertionAdapter<MealSnapshotEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `meal_snapshots` (`id`,`sessionId`,`imageUrl`,`localImagePath`,`capturedAt`,`model`,`rawJson`,`totalKcal`,`isEdited`,`lastSyncedAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MealSnapshotEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getSessionId());
        statement.bindString(3, entity.getImageUrl());
        if (entity.getLocalImagePath() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getLocalImagePath());
        }
        statement.bindLong(5, entity.getCapturedAt());
        statement.bindString(6, entity.getModel());
        if (entity.getRawJson() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getRawJson());
        }
        statement.bindDouble(8, entity.getTotalKcal());
        final int _tmp = entity.isEdited() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getLastSyncedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastSyncedAt());
        }
      }
    };
    this.__preparedStmtOfDeleteSnapshotsForSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM meal_snapshots WHERE sessionId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object saveSnapshot(final MealSnapshotEntity snapshot,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMealSnapshotEntity.insert(snapshot);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSnapshotsForSession(final String sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSnapshotsForSession.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfDeleteSnapshotsForSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getSnapshotsForSession(final String sessionId,
      final Continuation<? super List<MealSnapshotEntity>> $completion) {
    final String _sql = "SELECT * FROM meal_snapshots WHERE sessionId = ? ORDER BY capturedAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MealSnapshotEntity>>() {
      @Override
      @NonNull
      public List<MealSnapshotEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfLocalImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "localImagePath");
          final int _cursorIndexOfCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "capturedAt");
          final int _cursorIndexOfModel = CursorUtil.getColumnIndexOrThrow(_cursor, "model");
          final int _cursorIndexOfRawJson = CursorUtil.getColumnIndexOrThrow(_cursor, "rawJson");
          final int _cursorIndexOfTotalKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalKcal");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final List<MealSnapshotEntity> _result = new ArrayList<MealSnapshotEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MealSnapshotEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpImageUrl;
            _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            final String _tmpLocalImagePath;
            if (_cursor.isNull(_cursorIndexOfLocalImagePath)) {
              _tmpLocalImagePath = null;
            } else {
              _tmpLocalImagePath = _cursor.getString(_cursorIndexOfLocalImagePath);
            }
            final long _tmpCapturedAt;
            _tmpCapturedAt = _cursor.getLong(_cursorIndexOfCapturedAt);
            final String _tmpModel;
            _tmpModel = _cursor.getString(_cursorIndexOfModel);
            final String _tmpRawJson;
            if (_cursor.isNull(_cursorIndexOfRawJson)) {
              _tmpRawJson = null;
            } else {
              _tmpRawJson = _cursor.getString(_cursorIndexOfRawJson);
            }
            final double _tmpTotalKcal;
            _tmpTotalKcal = _cursor.getDouble(_cursorIndexOfTotalKcal);
            final boolean _tmpIsEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _item = new MealSnapshotEntity(_tmpId,_tmpSessionId,_tmpImageUrl,_tmpLocalImagePath,_tmpCapturedAt,_tmpModel,_tmpRawJson,_tmpTotalKcal,_tmpIsEdited,_tmpLastSyncedAt);
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
  public Object getLatestSnapshot(final String sessionId,
      final Continuation<? super MealSnapshotEntity> $completion) {
    final String _sql = "SELECT * FROM meal_snapshots WHERE sessionId = ? ORDER BY capturedAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MealSnapshotEntity>() {
      @Override
      @Nullable
      public MealSnapshotEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfLocalImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "localImagePath");
          final int _cursorIndexOfCapturedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "capturedAt");
          final int _cursorIndexOfModel = CursorUtil.getColumnIndexOrThrow(_cursor, "model");
          final int _cursorIndexOfRawJson = CursorUtil.getColumnIndexOrThrow(_cursor, "rawJson");
          final int _cursorIndexOfTotalKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalKcal");
          final int _cursorIndexOfIsEdited = CursorUtil.getColumnIndexOrThrow(_cursor, "isEdited");
          final int _cursorIndexOfLastSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastSyncedAt");
          final MealSnapshotEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpImageUrl;
            _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            final String _tmpLocalImagePath;
            if (_cursor.isNull(_cursorIndexOfLocalImagePath)) {
              _tmpLocalImagePath = null;
            } else {
              _tmpLocalImagePath = _cursor.getString(_cursorIndexOfLocalImagePath);
            }
            final long _tmpCapturedAt;
            _tmpCapturedAt = _cursor.getLong(_cursorIndexOfCapturedAt);
            final String _tmpModel;
            _tmpModel = _cursor.getString(_cursorIndexOfModel);
            final String _tmpRawJson;
            if (_cursor.isNull(_cursorIndexOfRawJson)) {
              _tmpRawJson = null;
            } else {
              _tmpRawJson = _cursor.getString(_cursorIndexOfRawJson);
            }
            final double _tmpTotalKcal;
            _tmpTotalKcal = _cursor.getDouble(_cursorIndexOfTotalKcal);
            final boolean _tmpIsEdited;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEdited);
            _tmpIsEdited = _tmp != 0;
            final Long _tmpLastSyncedAt;
            if (_cursor.isNull(_cursorIndexOfLastSyncedAt)) {
              _tmpLastSyncedAt = null;
            } else {
              _tmpLastSyncedAt = _cursor.getLong(_cursorIndexOfLastSyncedAt);
            }
            _result = new MealSnapshotEntity(_tmpId,_tmpSessionId,_tmpImageUrl,_tmpLocalImagePath,_tmpCapturedAt,_tmpModel,_tmpRawJson,_tmpTotalKcal,_tmpIsEdited,_tmpLastSyncedAt);
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

  @Override
  public Object getSnapshotIdsForSession(final String sessionId,
      final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT id FROM meal_snapshots WHERE sessionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
