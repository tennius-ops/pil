package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyCheckRecord
import com.example.data.DailyCheckRepository
import com.example.data.MedicationTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DailyCheckUiState(
  val formattedDate: String = "",
  val dayOfWeek: String = "",
  val record: DailyCheckRecord = DailyCheckRecord(),
  val isLoaded: Boolean = false,
)

class DailyCheckViewModel(
  application: Application,
  private val repository: DailyCheckRepository,
) : AndroidViewModel(application) {

  constructor(application: Application) : this(application, DailyCheckRepository(application))

  private val _uiState = MutableStateFlow(createInitialDateState(repository.currentDate()))
  val uiState: StateFlow<DailyCheckUiState> = _uiState.asStateFlow()

  private var midnightTimerJob: Job? = null

  init {
    viewModelScope.launch {
      repository.ensureCurrentDaySynced()
      repository.observeTodayRecord().collect { record ->
        val (dateText, dayText) = formatKoreanDate(repository.currentDate())
        _uiState.update {
          it.copy(
            formattedDate = dateText,
            dayOfWeek = dayText,
            record = record,
            isLoaded = true,
          )
        }
      }
    }
    restartMidnightTimer()
  }

  fun onToggleMedication(time: MedicationTime) {
    viewModelScope.launch { repository.toggleMedication(time) }
  }

  fun onAddWaterCup() {
    viewModelScope.launch { repository.addWaterCup() }
  }

  fun onRemoveWaterCup() {
    viewModelScope.launch { repository.removeWaterCup() }
  }

  fun onResumeCheckDate() {
    viewModelScope.launch {
      repository.ensureCurrentDaySynced()
      val (dateText, dayText) = formatKoreanDate(repository.currentDate())
      _uiState.update { it.copy(formattedDate = dateText, dayOfWeek = dayText) }
    }
    restartMidnightTimer()
  }

  private fun restartMidnightTimer() {
    midnightTimerJob?.cancel()
    midnightTimerJob =
      viewModelScope.launch {
        while (isActive) {
          val waitMillis = repository.millisUntilNextMidnight()
          delay(waitMillis)
          repository.ensureCurrentDaySynced()
          val (dateText, dayText) = formatKoreanDate(repository.currentDate())
          _uiState.update { it.copy(formattedDate = dateText, dayOfWeek = dayText) }
        }
      }
  }

  companion object {
    fun formatKoreanDate(date: Date): Pair<String, String> {
      val dateFormatter = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)
      val dayFormatter = SimpleDateFormat("EEEE", Locale.KOREAN)
      return dateFormatter.format(date) to dayFormatter.format(date)
    }

    private fun createInitialDateState(date: Date): DailyCheckUiState {
      val (dateText, dayText) = formatKoreanDate(date)
      return DailyCheckUiState(formattedDate = dateText, dayOfWeek = dayText)
    }
  }
}
