package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dailyCheckDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "daily_health_check")

class DailyCheckRepository(
  private val context: Context,
  private val currentDateProvider: () -> Date = { Date() },
) {
  private object Keys {
    val DATE_KEY = stringPreferencesKey("recorded_date_key")
    val MORNING_TAKEN = booleanPreferencesKey("morning_taken")
    val LUNCH_TAKEN = booleanPreferencesKey("lunch_taken")
    val DINNER_TAKEN = booleanPreferencesKey("dinner_taken")
    val WATER_CUPS = intPreferencesKey("water_cups")
  }

  fun currentDate(): Date = currentDateProvider()

  fun currentDateKey(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(currentDateProvider())
  }

  fun millisUntilNextMidnight(now: Date = currentDateProvider()): Long {
    val cal =
      java.util.Calendar.getInstance().apply {
        time = now
        add(java.util.Calendar.DAY_OF_YEAR, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 50)
      }
    return (cal.timeInMillis - now.time).coerceAtLeast(100L)
  }

  fun observeTodayRecord(): Flow<DailyCheckRecord> {
    return context.dailyCheckDataStore.data.map { prefs ->
      val today = currentDateKey()
      val storedDate = prefs[Keys.DATE_KEY]
      if (storedDate != today) {
        DailyCheckRecord(dateKey = today)
      } else {
        DailyCheckRecord(
          dateKey = today,
          morningTaken = prefs[Keys.MORNING_TAKEN] ?: false,
          lunchTaken = prefs[Keys.LUNCH_TAKEN] ?: false,
          dinnerTaken = prefs[Keys.DINNER_TAKEN] ?: false,
          waterCups = prefs[Keys.WATER_CUPS] ?: 0,
        )
      }
    }
  }

  suspend fun toggleMedication(time: MedicationTime) {
    val today = currentDateKey()
    context.dailyCheckDataStore.edit { prefs ->
      resetIfNewDay(prefs, today)
      val key =
        when (time) {
          MedicationTime.MORNING -> Keys.MORNING_TAKEN
          MedicationTime.LUNCH -> Keys.LUNCH_TAKEN
          MedicationTime.DINNER -> Keys.DINNER_TAKEN
        }
      val current = prefs[key] ?: false
      prefs[key] = !current
    }
  }

  suspend fun addWaterCup() {
    val today = currentDateKey()
    context.dailyCheckDataStore.edit { prefs ->
      resetIfNewDay(prefs, today)
      val current = prefs[Keys.WATER_CUPS] ?: 0
      prefs[Keys.WATER_CUPS] = current + 1
    }
  }

  suspend fun removeWaterCup() {
    val today = currentDateKey()
    context.dailyCheckDataStore.edit { prefs ->
      resetIfNewDay(prefs, today)
      val current = prefs[Keys.WATER_CUPS] ?: 0
      if (current > 0) {
        prefs[Keys.WATER_CUPS] = current - 1
      }
    }
  }

  suspend fun ensureCurrentDaySynced() {
    val today = currentDateKey()
    context.dailyCheckDataStore.edit { prefs -> resetIfNewDay(prefs, today) }
  }

  private fun resetIfNewDay(
    prefs: androidx.datastore.preferences.core.MutablePreferences,
    today: String,
  ) {
    val storedDate = prefs[Keys.DATE_KEY]
    if (storedDate != today) {
      prefs[Keys.DATE_KEY] = today
      prefs[Keys.MORNING_TAKEN] = false
      prefs[Keys.LUNCH_TAKEN] = false
      prefs[Keys.DINNER_TAKEN] = false
      prefs[Keys.WATER_CUPS] = 0
    }
  }
}
