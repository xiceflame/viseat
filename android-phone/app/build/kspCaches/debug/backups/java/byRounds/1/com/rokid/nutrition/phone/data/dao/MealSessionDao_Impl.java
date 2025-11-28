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
import com.rokid.nutrition.phone.data.entity.MealSessionEntity;
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
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MealSessionDao_Impl implements MealSessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MealSessionEntity> __insertionAdapterOfMealSessionEntity;

  private final SharedSQLiteStatement __preparedStmtOfEndSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSession;

  public MealSessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMealSessionEntity = new EntityInsertionAdapter<MealSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `meal_sessions` (`sessionId`,`userId`,`mealType`,`status`,`startTime`,`endTime`,`autoCaptureInterval`,`totalServedKcal`,`totalConsumedKcal`,`consumptionRatio`,`durationMinutes`,`report`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MealSessionEntity entity) {
        statement.bindString(1, entity.getSessionId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getMealType());
        statement.bindString(4, entity.getStatus());
        statement.bindLong(5, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getEndTime());
        }
        statement.bindLong(7, entity.getAutoCaptureInterval());
        if (entity.getTotalServedKcal() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getTotalServedKcal());
        }
        if (entity.getTotalConsumedKcal() == null) {
          statement.bindNull(9);
        } else {
          statement.bindDouble(9, entity.getTotalConsumedKcal());
        }
        if (entity.getConsumptionRatio() == null) {
          statement.bindNull(10);
        } else {
          statement.bindDouble(10, entity.getConsumptionRatio());
        }
        if (entity.getDurationMinutes() == null) {
          statement.bindNull(11);
        } else {
          statement.bindDouble(11, entity.getDurationMinutes());
        }
        if (entity.getReport() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getReport());
        }
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindLong(14, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfEndSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE meal_sessions SET \n"
                + "            status = ?, \n"
                + "            endTime = ?, \n"
                + "            totalServedKcal = ?, \n"
                + "            totalConsumedKcal = ?, \n"
                + "            consumptionRatio = ?, \n"
                + "            durationMinutes = ?, \n"
                + "            report = ?,\n"
                + "            updatedAt = ? \n"
                + "        WHERE sessionId = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM meal_sessions WHERE sessionId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object saveSession(final MealSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMealSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object endSession(final String sessionId, final String status, final long endTime,
      final double totalServed, final double totalConsumed, final double ratio,
      final double duration, final String report, final long updatedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEndSession.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, endTime);
        _argIndex = 3;
        _stmt.bindDouble(_argIndex, totalServed);
        _argIndex = 4;
        _stmt.bindDouble(_argIndex, totalConsumed);
        _argIndex = 5;
        _stmt.bindDouble(_argIndex, ratio);
        _argIndex = 6;
        _stmt.bindDouble(_argIndex, duration);
        _argIndex = 7;
        if (report == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, report);
        }
        _argIndex = 8;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 9;
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
          __preparedStmtOfEndSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final String sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSession.acquire();
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
          __preparedStmtOfDeleteSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MealSessionEntity>> getRecentSessions(final int limit) {
    final String _sql = "SELECT * FROM meal_sessions ORDER BY startTime DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meal_sessions"}, new Callable<List<MealSessionEntity>>() {
      @Override
      @NonNull
      public List<MealSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfAutoCaptureInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCaptureInterval");
          final int _cursorIndexOfTotalServedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalServedKcal");
          final int _cursorIndexOfTotalConsumedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalConsumedKcal");
          final int _cursorIndexOfConsumptionRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "consumptionRatio");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfReport = CursorUtil.getColumnIndexOrThrow(_cursor, "report");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MealSessionEntity> _result = new ArrayList<MealSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MealSessionEntity _item;
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpMealType;
            _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final int _tmpAutoCaptureInterval;
            _tmpAutoCaptureInterval = _cursor.getInt(_cursorIndexOfAutoCaptureInterval);
            final Double _tmpTotalServedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalServedKcal)) {
              _tmpTotalServedKcal = null;
            } else {
              _tmpTotalServedKcal = _cursor.getDouble(_cursorIndexOfTotalServedKcal);
            }
            final Double _tmpTotalConsumedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalConsumedKcal)) {
              _tmpTotalConsumedKcal = null;
            } else {
              _tmpTotalConsumedKcal = _cursor.getDouble(_cursorIndexOfTotalConsumedKcal);
            }
            final Double _tmpConsumptionRatio;
            if (_cursor.isNull(_cursorIndexOfConsumptionRatio)) {
              _tmpConsumptionRatio = null;
            } else {
              _tmpConsumptionRatio = _cursor.getDouble(_cursorIndexOfConsumptionRatio);
            }
            final Double _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getDouble(_cursorIndexOfDurationMinutes);
            }
            final String _tmpReport;
            if (_cursor.isNull(_cursorIndexOfReport)) {
              _tmpReport = null;
            } else {
              _tmpReport = _cursor.getString(_cursorIndexOfReport);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MealSessionEntity(_tmpSessionId,_tmpUserId,_tmpMealType,_tmpStatus,_tmpStartTime,_tmpEndTime,_tmpAutoCaptureInterval,_tmpTotalServedKcal,_tmpTotalConsumedKcal,_tmpConsumptionRatio,_tmpDurationMinutes,_tmpReport,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSession(final String sessionId,
      final Continuation<? super MealSessionEntity> $completion) {
    final String _sql = "SELECT * FROM meal_sessions WHERE sessionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MealSessionEntity>() {
      @Override
      @Nullable
      public MealSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfAutoCaptureInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCaptureInterval");
          final int _cursorIndexOfTotalServedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalServedKcal");
          final int _cursorIndexOfTotalConsumedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalConsumedKcal");
          final int _cursorIndexOfConsumptionRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "consumptionRatio");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfReport = CursorUtil.getColumnIndexOrThrow(_cursor, "report");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final MealSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpMealType;
            _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final int _tmpAutoCaptureInterval;
            _tmpAutoCaptureInterval = _cursor.getInt(_cursorIndexOfAutoCaptureInterval);
            final Double _tmpTotalServedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalServedKcal)) {
              _tmpTotalServedKcal = null;
            } else {
              _tmpTotalServedKcal = _cursor.getDouble(_cursorIndexOfTotalServedKcal);
            }
            final Double _tmpTotalConsumedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalConsumedKcal)) {
              _tmpTotalConsumedKcal = null;
            } else {
              _tmpTotalConsumedKcal = _cursor.getDouble(_cursorIndexOfTotalConsumedKcal);
            }
            final Double _tmpConsumptionRatio;
            if (_cursor.isNull(_cursorIndexOfConsumptionRatio)) {
              _tmpConsumptionRatio = null;
            } else {
              _tmpConsumptionRatio = _cursor.getDouble(_cursorIndexOfConsumptionRatio);
            }
            final Double _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getDouble(_cursorIndexOfDurationMinutes);
            }
            final String _tmpReport;
            if (_cursor.isNull(_cursorIndexOfReport)) {
              _tmpReport = null;
            } else {
              _tmpReport = _cursor.getString(_cursorIndexOfReport);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new MealSessionEntity(_tmpSessionId,_tmpUserId,_tmpMealType,_tmpStatus,_tmpStartTime,_tmpEndTime,_tmpAutoCaptureInterval,_tmpTotalServedKcal,_tmpTotalConsumedKcal,_tmpConsumptionRatio,_tmpDurationMinutes,_tmpReport,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getActiveSession(final Continuation<? super MealSessionEntity> $completion) {
    final String _sql = "SELECT * FROM meal_sessions WHERE status = 'active' LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MealSessionEntity>() {
      @Override
      @Nullable
      public MealSessionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfAutoCaptureInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCaptureInterval");
          final int _cursorIndexOfTotalServedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalServedKcal");
          final int _cursorIndexOfTotalConsumedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalConsumedKcal");
          final int _cursorIndexOfConsumptionRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "consumptionRatio");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfReport = CursorUtil.getColumnIndexOrThrow(_cursor, "report");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final MealSessionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpMealType;
            _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final int _tmpAutoCaptureInterval;
            _tmpAutoCaptureInterval = _cursor.getInt(_cursorIndexOfAutoCaptureInterval);
            final Double _tmpTotalServedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalServedKcal)) {
              _tmpTotalServedKcal = null;
            } else {
              _tmpTotalServedKcal = _cursor.getDouble(_cursorIndexOfTotalServedKcal);
            }
            final Double _tmpTotalConsumedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalConsumedKcal)) {
              _tmpTotalConsumedKcal = null;
            } else {
              _tmpTotalConsumedKcal = _cursor.getDouble(_cursorIndexOfTotalConsumedKcal);
            }
            final Double _tmpConsumptionRatio;
            if (_cursor.isNull(_cursorIndexOfConsumptionRatio)) {
              _tmpConsumptionRatio = null;
            } else {
              _tmpConsumptionRatio = _cursor.getDouble(_cursorIndexOfConsumptionRatio);
            }
            final Double _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getDouble(_cursorIndexOfDurationMinutes);
            }
            final String _tmpReport;
            if (_cursor.isNull(_cursorIndexOfReport)) {
              _tmpReport = null;
            } else {
              _tmpReport = _cursor.getString(_cursorIndexOfReport);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new MealSessionEntity(_tmpSessionId,_tmpUserId,_tmpMealType,_tmpStatus,_tmpStartTime,_tmpEndTime,_tmpAutoCaptureInterval,_tmpTotalServedKcal,_tmpTotalConsumedKcal,_tmpConsumptionRatio,_tmpDurationMinutes,_tmpReport,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getDailyCalories(final long startOfDay, final long endOfDay,
      final Continuation<? super Double> $completion) {
    final String _sql = "\n"
            + "        SELECT COALESCE(SUM(totalConsumedKcal), 0.0) \n"
            + "        FROM meal_sessions \n"
            + "        WHERE startTime >= ? AND startTime < ? AND status = 'completed'\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endOfDay);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @NonNull
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final double _tmp;
            _tmp = _cursor.getDouble(0);
            _result = _tmp;
          } else {
            _result = 0.0;
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
  public Object getSessionsInRange(final long startTime, final long endTime,
      final Continuation<? super List<MealSessionEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM meal_sessions \n"
            + "        WHERE startTime >= ? AND startTime < ? AND status = 'completed' \n"
            + "        ORDER BY startTime ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MealSessionEntity>>() {
      @Override
      @NonNull
      public List<MealSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfMealType = CursorUtil.getColumnIndexOrThrow(_cursor, "mealType");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfAutoCaptureInterval = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCaptureInterval");
          final int _cursorIndexOfTotalServedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalServedKcal");
          final int _cursorIndexOfTotalConsumedKcal = CursorUtil.getColumnIndexOrThrow(_cursor, "totalConsumedKcal");
          final int _cursorIndexOfConsumptionRatio = CursorUtil.getColumnIndexOrThrow(_cursor, "consumptionRatio");
          final int _cursorIndexOfDurationMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMinutes");
          final int _cursorIndexOfReport = CursorUtil.getColumnIndexOrThrow(_cursor, "report");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<MealSessionEntity> _result = new ArrayList<MealSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MealSessionEntity _item;
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpMealType;
            _tmpMealType = _cursor.getString(_cursorIndexOfMealType);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final int _tmpAutoCaptureInterval;
            _tmpAutoCaptureInterval = _cursor.getInt(_cursorIndexOfAutoCaptureInterval);
            final Double _tmpTotalServedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalServedKcal)) {
              _tmpTotalServedKcal = null;
            } else {
              _tmpTotalServedKcal = _cursor.getDouble(_cursorIndexOfTotalServedKcal);
            }
            final Double _tmpTotalConsumedKcal;
            if (_cursor.isNull(_cursorIndexOfTotalConsumedKcal)) {
              _tmpTotalConsumedKcal = null;
            } else {
              _tmpTotalConsumedKcal = _cursor.getDouble(_cursorIndexOfTotalConsumedKcal);
            }
            final Double _tmpConsumptionRatio;
            if (_cursor.isNull(_cursorIndexOfConsumptionRatio)) {
              _tmpConsumptionRatio = null;
            } else {
              _tmpConsumptionRatio = _cursor.getDouble(_cursorIndexOfConsumptionRatio);
            }
            final Double _tmpDurationMinutes;
            if (_cursor.isNull(_cursorIndexOfDurationMinutes)) {
              _tmpDurationMinutes = null;
            } else {
              _tmpDurationMinutes = _cursor.getDouble(_cursorIndexOfDurationMinutes);
            }
            final String _tmpReport;
            if (_cursor.isNull(_cursorIndexOfReport)) {
              _tmpReport = null;
            } else {
              _tmpReport = _cursor.getString(_cursorIndexOfReport);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new MealSessionEntity(_tmpSessionId,_tmpUserId,_tmpMealType,_tmpStatus,_tmpStartTime,_tmpEndTime,_tmpAutoCaptureInterval,_tmpTotalServedKcal,_tmpTotalConsumedKcal,_tmpConsumptionRatio,_tmpDurationMinutes,_tmpReport,_tmpCreatedAt,_tmpUpdatedAt);
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
