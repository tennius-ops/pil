package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyCheckRecord
import com.example.data.DailyCheckRepository
import com.example.data.MedicationTime
import com.example.ui.DailyCheckUiState
import com.example.ui.screens.DailyCheckScreen
import com.example.ui.theme.SeniorHealthTheme
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `app name matches Korean senior check label`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("오늘 약·물", appName)
  }

  @Test
  fun `clicking medication and water buttons updates senior UI`() {
    composeTestRule.setContent {
      SeniorHealthTheme {
        var state by remember {
          mutableStateOf(
            DailyCheckUiState(
              formattedDate = "2026년 11월 16일",
              dayOfWeek = "월요일",
              record = DailyCheckRecord(dateKey = "2026-11-16"),
              isLoaded = true,
            )
          )
        }
        DailyCheckScreen(
          uiState = state,
          onToggleMedication = { time ->
            val r = state.record
            val updated =
              when (time) {
                MedicationTime.MORNING -> r.copy(morningTaken = !r.morningTaken)
                MedicationTime.LUNCH -> r.copy(lunchTaken = !r.lunchTaken)
                MedicationTime.DINNER -> r.copy(dinnerTaken = !r.dinnerTaken)
              }
            state = state.copy(record = updated)
          },
          onAddWaterCup = {
            val r = state.record
            state = state.copy(record = r.copy(waterCups = r.waterCups + 1))
          },
          onRemoveWaterCup = {
            val r = state.record
            if (r.waterCups > 0) {
              state = state.copy(record = r.copy(waterCups = r.waterCups - 1))
            }
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("today_date_text").assertTextEquals("2026년 11월 16일")
    composeTestRule.onNodeWithText("아침 약").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("점심 약").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("저녁 약").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("water_cups_visual").assertContentDescriptionEquals("오늘 마신 물 0잔")
    composeTestRule.onNodeWithTag("water_cup_0").assertContentDescriptionEquals("빈 물컵 1")

    // Tap morning medication and verify '복용 완료' appears
    composeTestRule.onNodeWithTag("med_button_morning").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("복용 완료").assertIsDisplayed()

    // Tap add water twice and verify cup drawings update to 2 filled cups
    composeTestRule.onNodeWithTag("add_water_button").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("add_water_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("water_cups_visual").assertContentDescriptionEquals("오늘 마신 물 2잔")
    composeTestRule.onNodeWithTag("water_cup_0").assertContentDescriptionEquals("마신 물컵 1")
    composeTestRule.onNodeWithTag("water_cup_1").assertContentDescriptionEquals("마신 물컵 2")
    composeTestRule.onNodeWithTag("water_cup_2").assertContentDescriptionEquals("빈 물컵 3")
  }

  @Test
  fun `repository persists today record across reads and resets on next day`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var fakeTimeMillis = 1_794_892_800_000L // fixed date
    val repo = DailyCheckRepository(context) { Date(fakeTimeMillis) }

    repo.ensureCurrentDaySynced()
    repo.toggleMedication(MedicationTime.MORNING)
    repo.toggleMedication(MedicationTime.DINNER)
    repo.addWaterCup()
    repo.addWaterCup()

    val saved = repo.observeTodayRecord().first()
    assertTrue(saved.morningTaken)
    assertFalse(saved.lunchTaken)
    assertTrue(saved.dinnerTaken)
    assertEquals(2, saved.waterCups)

    // Advance by 24 hours -> new date should start fresh
    fakeTimeMillis += 86_400_000L
    val nextDay = repo.observeTodayRecord().first()
    assertFalse(nextDay.morningTaken)
    assertFalse(nextDay.dinnerTaken)
    assertEquals(0, nextDay.waterCups)
  }

  @Test
  fun `MainActivity creates DailyCheckViewModel without crashing`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    val vm =
      androidx.lifecycle.ViewModelProvider(activity)[com.example.ui.DailyCheckViewModel::class.java]
    assertTrue(vm.uiState.value.formattedDate.isNotEmpty())
  }

  @Test
  fun `millisUntilNextMidnight computes exact delay until 00_00 next day`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cal =
      java.util.Calendar.getInstance().apply {
        set(2026, java.util.Calendar.NOVEMBER, 16, 23, 59, 30)
        set(java.util.Calendar.MILLISECOND, 0)
      }
    val repo = DailyCheckRepository(context) { cal.time }
    val millis = repo.millisUntilNextMidnight()
    assertEquals(30_050L, millis)
  }

  @Test
  fun `ensureCurrentDaySynced clears yesterday records after midnight`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cal =
      java.util.Calendar.getInstance().apply {
        set(2026, java.util.Calendar.NOVEMBER, 16, 23, 55, 0)
        set(java.util.Calendar.MILLISECOND, 0)
      }
    var now = cal.time
    val repo = DailyCheckRepository(context) { now }

    repo.ensureCurrentDaySynced()
    repo.toggleMedication(MedicationTime.MORNING)
    repo.toggleMedication(MedicationTime.LUNCH)
    repo.toggleMedication(MedicationTime.DINNER)
    repo.addWaterCup()
    repo.addWaterCup()
    repo.addWaterCup()

    val beforeMidnight = repo.observeTodayRecord().first()
    assertEquals(3, beforeMidnight.completedMedicationCount)
    assertEquals(3, beforeMidnight.waterCups)

    // Cross midnight to 2026-11-17 00:00:05
    cal.add(java.util.Calendar.MINUTE, 5)
    cal.set(java.util.Calendar.SECOND, 5)
    now = cal.time

    repo.ensureCurrentDaySynced()
    val afterMidnight = repo.observeTodayRecord().first()
    assertEquals("2026-11-17", afterMidnight.dateKey)
    assertFalse(afterMidnight.morningTaken)
    assertFalse(afterMidnight.lunchTaken)
    assertFalse(afterMidnight.dinnerTaken)
    assertEquals(0, afterMidnight.waterCups)
  }

  @Test
  fun `BloodSugarScreen displays input controls, timing selection, and saved history`() {
    val sampleRecord =
      com.example.data.BloodSugarRecord(
        id = "rec_1",
        userId = "user_1",
        valueMgDl = 112L,
        timing = com.example.data.BloodSugarTiming.FASTING.name,
        measuredDate = "2026-11-16",
        measuredTimeText = "오전 8:15",
        note = "공복 측정",
      )

    composeTestRule.setContent {
      SeniorHealthTheme {
        var inputState by remember { mutableStateOf(com.example.ui.BloodSugarInputUiState()) }
        var records by remember { mutableStateOf(listOf(sampleRecord)) }

        com.example.ui.screens.BloodSugarScreen(
          inputState = inputState,
          listState = com.example.ui.BloodSugarListUiState.Success(records),
          onValueChange = { raw -> inputState = inputState.copy(valueInput = raw) },
          onAdjustValue = { delta ->
            val current = inputState.valueInput.toIntOrNull() ?: 110
            inputState = inputState.copy(valueInput = (current + delta).toString())
          },
          onTimingSelected = { timing -> inputState = inputState.copy(selectedTiming = timing) },
          onNoteChange = { note -> inputState = inputState.copy(noteInput = note) },
          onSaveClick = {
            val numeric = inputState.valueInput.toLongOrNull() ?: 0L
            records =
              listOf(
                com.example.data.BloodSugarRecord(
                  id = "rec_2",
                  userId = "user_1",
                  valueMgDl = numeric,
                  timing = inputState.selectedTiming.name,
                  measuredDate = "2026-11-16",
                  measuredTimeText = "오후 1:20",
                  note = inputState.noteInput.ifBlank { null },
                )
              ) + records
            inputState =
              inputState.copy(valueInput = "", noteInput = "", feedbackMessage = "혈당 기록이 저장되었습니다.")
          },
          onDeleteRecord = { id -> records = records.filterNot { it.id == id } },
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("112 mg/dL").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("timing_AFTER_MEAL").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("blood_sugar_plus_10").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("save_blood_sugar_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("혈당 기록이 저장되었습니다.").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("120 mg/dL").performScrollTo().assertIsDisplayed()
  }
}
