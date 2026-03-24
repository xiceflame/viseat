package com.rokid.nutrition.phone.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.rokid.nutrition.phone.data.entity.UserProfileEntity;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserProfileDao_Impl implements UserProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserProfileEntity> __insertionAdapterOfUserProfileEntity;

  public UserProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserProfileEntity = new EntityInsertionAdapter<UserProfileEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_profiles` (`id`,`nickname`,`age`,`gender`,`birthDate`,`height`,`weight`,`bmi`,`activityLevel`,`healthGoal`,`targetWeight`,`targetDate`,`dietType`,`allergens`,`healthConditions`,`dietaryPreferences`,`isOnboardingCompleted`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProfileEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getNickname() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getNickname());
        }
        statement.bindLong(3, entity.getAge());
        statement.bindString(4, entity.getGender());
        if (entity.getBirthDate() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getBirthDate());
        }
        statement.bindDouble(6, entity.getHeight());
        statement.bindDouble(7, entity.getWeight());
        statement.bindDouble(8, entity.getBmi());
        statement.bindString(9, entity.getActivityLevel());
        statement.bindString(10, entity.getHealthGoal());
        if (entity.getTargetWeight() == null) {
          statement.bindNull(11);
        } else {
          statement.bindDouble(11, entity.getTargetWeight());
        }
        if (entity.getTargetDate() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getTargetDate());
        }
        statement.bindString(13, entity.getDietType());
        statement.bindString(14, entity.getAllergens());
        statement.bindString(15, entity.getHealthConditions());
        statement.bindString(16, entity.getDietaryPreferences());
        final int _tmp = entity.isOnboardingCompleted() ? 1 : 0;
        statement.bindLong(17, _tmp);
        statement.bindLong(18, entity.getCreatedAt());
        statement.bindLong(19, entity.getUpdatedAt());
      }
    };
  }

  @Override
  public Object saveProfile(final UserProfileEntity profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserProfileEntity.insert(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getProfile(final String id,
      final Continuation<? super UserProfileEntity> $completion) {
    final String _sql = "SELECT * FROM user_profiles WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfGender = CursorUtil.getColumnIndexOrThrow(_cursor, "gender");
          final int _cursorIndexOfBirthDate = CursorUtil.getColumnIndexOrThrow(_cursor, "birthDate");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfBmi = CursorUtil.getColumnIndexOrThrow(_cursor, "bmi");
          final int _cursorIndexOfActivityLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLevel");
          final int _cursorIndexOfHealthGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "healthGoal");
          final int _cursorIndexOfTargetWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeight");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfDietType = CursorUtil.getColumnIndexOrThrow(_cursor, "dietType");
          final int _cursorIndexOfAllergens = CursorUtil.getColumnIndexOrThrow(_cursor, "allergens");
          final int _cursorIndexOfHealthConditions = CursorUtil.getColumnIndexOrThrow(_cursor, "healthConditions");
          final int _cursorIndexOfDietaryPreferences = CursorUtil.getColumnIndexOrThrow(_cursor, "dietaryPreferences");
          final int _cursorIndexOfIsOnboardingCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnboardingCompleted");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNickname;
            if (_cursor.isNull(_cursorIndexOfNickname)) {
              _tmpNickname = null;
            } else {
              _tmpNickname = _cursor.getString(_cursorIndexOfNickname);
            }
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpGender;
            _tmpGender = _cursor.getString(_cursorIndexOfGender);
            final String _tmpBirthDate;
            if (_cursor.isNull(_cursorIndexOfBirthDate)) {
              _tmpBirthDate = null;
            } else {
              _tmpBirthDate = _cursor.getString(_cursorIndexOfBirthDate);
            }
            final float _tmpHeight;
            _tmpHeight = _cursor.getFloat(_cursorIndexOfHeight);
            final float _tmpWeight;
            _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            final float _tmpBmi;
            _tmpBmi = _cursor.getFloat(_cursorIndexOfBmi);
            final String _tmpActivityLevel;
            _tmpActivityLevel = _cursor.getString(_cursorIndexOfActivityLevel);
            final String _tmpHealthGoal;
            _tmpHealthGoal = _cursor.getString(_cursorIndexOfHealthGoal);
            final Float _tmpTargetWeight;
            if (_cursor.isNull(_cursorIndexOfTargetWeight)) {
              _tmpTargetWeight = null;
            } else {
              _tmpTargetWeight = _cursor.getFloat(_cursorIndexOfTargetWeight);
            }
            final String _tmpTargetDate;
            if (_cursor.isNull(_cursorIndexOfTargetDate)) {
              _tmpTargetDate = null;
            } else {
              _tmpTargetDate = _cursor.getString(_cursorIndexOfTargetDate);
            }
            final String _tmpDietType;
            _tmpDietType = _cursor.getString(_cursorIndexOfDietType);
            final String _tmpAllergens;
            _tmpAllergens = _cursor.getString(_cursorIndexOfAllergens);
            final String _tmpHealthConditions;
            _tmpHealthConditions = _cursor.getString(_cursorIndexOfHealthConditions);
            final String _tmpDietaryPreferences;
            _tmpDietaryPreferences = _cursor.getString(_cursorIndexOfDietaryPreferences);
            final boolean _tmpIsOnboardingCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOnboardingCompleted);
            _tmpIsOnboardingCompleted = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserProfileEntity(_tmpId,_tmpNickname,_tmpAge,_tmpGender,_tmpBirthDate,_tmpHeight,_tmpWeight,_tmpBmi,_tmpActivityLevel,_tmpHealthGoal,_tmpTargetWeight,_tmpTargetDate,_tmpDietType,_tmpAllergens,_tmpHealthConditions,_tmpDietaryPreferences,_tmpIsOnboardingCompleted,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<UserProfileEntity> getProfileFlow(final String id) {
    final String _sql = "SELECT * FROM user_profiles WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_profiles"}, new Callable<UserProfileEntity>() {
      @Override
      @Nullable
      public UserProfileEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfAge = CursorUtil.getColumnIndexOrThrow(_cursor, "age");
          final int _cursorIndexOfGender = CursorUtil.getColumnIndexOrThrow(_cursor, "gender");
          final int _cursorIndexOfBirthDate = CursorUtil.getColumnIndexOrThrow(_cursor, "birthDate");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfBmi = CursorUtil.getColumnIndexOrThrow(_cursor, "bmi");
          final int _cursorIndexOfActivityLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "activityLevel");
          final int _cursorIndexOfHealthGoal = CursorUtil.getColumnIndexOrThrow(_cursor, "healthGoal");
          final int _cursorIndexOfTargetWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "targetWeight");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfDietType = CursorUtil.getColumnIndexOrThrow(_cursor, "dietType");
          final int _cursorIndexOfAllergens = CursorUtil.getColumnIndexOrThrow(_cursor, "allergens");
          final int _cursorIndexOfHealthConditions = CursorUtil.getColumnIndexOrThrow(_cursor, "healthConditions");
          final int _cursorIndexOfDietaryPreferences = CursorUtil.getColumnIndexOrThrow(_cursor, "dietaryPreferences");
          final int _cursorIndexOfIsOnboardingCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isOnboardingCompleted");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final UserProfileEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNickname;
            if (_cursor.isNull(_cursorIndexOfNickname)) {
              _tmpNickname = null;
            } else {
              _tmpNickname = _cursor.getString(_cursorIndexOfNickname);
            }
            final int _tmpAge;
            _tmpAge = _cursor.getInt(_cursorIndexOfAge);
            final String _tmpGender;
            _tmpGender = _cursor.getString(_cursorIndexOfGender);
            final String _tmpBirthDate;
            if (_cursor.isNull(_cursorIndexOfBirthDate)) {
              _tmpBirthDate = null;
            } else {
              _tmpBirthDate = _cursor.getString(_cursorIndexOfBirthDate);
            }
            final float _tmpHeight;
            _tmpHeight = _cursor.getFloat(_cursorIndexOfHeight);
            final float _tmpWeight;
            _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            final float _tmpBmi;
            _tmpBmi = _cursor.getFloat(_cursorIndexOfBmi);
            final String _tmpActivityLevel;
            _tmpActivityLevel = _cursor.getString(_cursorIndexOfActivityLevel);
            final String _tmpHealthGoal;
            _tmpHealthGoal = _cursor.getString(_cursorIndexOfHealthGoal);
            final Float _tmpTargetWeight;
            if (_cursor.isNull(_cursorIndexOfTargetWeight)) {
              _tmpTargetWeight = null;
            } else {
              _tmpTargetWeight = _cursor.getFloat(_cursorIndexOfTargetWeight);
            }
            final String _tmpTargetDate;
            if (_cursor.isNull(_cursorIndexOfTargetDate)) {
              _tmpTargetDate = null;
            } else {
              _tmpTargetDate = _cursor.getString(_cursorIndexOfTargetDate);
            }
            final String _tmpDietType;
            _tmpDietType = _cursor.getString(_cursorIndexOfDietType);
            final String _tmpAllergens;
            _tmpAllergens = _cursor.getString(_cursorIndexOfAllergens);
            final String _tmpHealthConditions;
            _tmpHealthConditions = _cursor.getString(_cursorIndexOfHealthConditions);
            final String _tmpDietaryPreferences;
            _tmpDietaryPreferences = _cursor.getString(_cursorIndexOfDietaryPreferences);
            final boolean _tmpIsOnboardingCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsOnboardingCompleted);
            _tmpIsOnboardingCompleted = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new UserProfileEntity(_tmpId,_tmpNickname,_tmpAge,_tmpGender,_tmpBirthDate,_tmpHeight,_tmpWeight,_tmpBmi,_tmpActivityLevel,_tmpHealthGoal,_tmpTargetWeight,_tmpTargetDate,_tmpDietType,_tmpAllergens,_tmpHealthConditions,_tmpDietaryPreferences,_tmpIsOnboardingCompleted,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
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
  public Object hasProfile(final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT EXISTS(SELECT 1 FROM user_profiles WHERE id = 'default')";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp != 0;
          } else {
            _result = false;
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
