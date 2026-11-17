package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BloodSugarRecord
import com.example.data.BloodSugarRepository
import com.example.data.BloodSugarTiming
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BloodSugarListUiState {
  data object Loading : BloodSugarListUiState

  data class Success(val records: List<BloodSugarRecord>) : BloodSugarListUiState

  data class Error(val message: String) : BloodSugarListUiState
}

data class BloodSugarInputUiState(
  val valueInput: String = "",
  val selectedTiming: BloodSugarTiming = BloodSugarTiming.FASTING,
  val noteInput: String = "",
  val isSaving: Boolean = false,
  val feedbackMessage: String? = null,
  val isErrorFeedback: Boolean = false,
)

class BloodSugarViewModel(
  private val repository: BloodSugarRepository,
  private val currentUserId: String,
) : ViewModel() {

  val recordsState: StateFlow<BloodSugarListUiState> =
    repository
      .observeRecords(currentUserId)
      .map<List<BloodSugarRecord>, BloodSugarListUiState> { BloodSugarListUiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing blood sugar records", rootError)
        emit(
          BloodSugarListUiState.Error(
            rootError.localizedMessage ?: "혈당 기록을 불러오는 중 오류가 발생했습니다."
          )
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = BloodSugarListUiState.Loading,
      )

  private val _inputState = MutableStateFlow(BloodSugarInputUiState())
  val inputState: StateFlow<BloodSugarInputUiState> = _inputState.asStateFlow()

  fun onValueChange(raw: String) {
    val digitsOnly = raw.filter { it.isDigit() }.take(3)
    _inputState.update {
      it.copy(valueInput = digitsOnly, feedbackMessage = null, isErrorFeedback = false)
    }
  }

  fun onAdjustValue(delta: Int) {
    val current = _inputState.value.valueInput.toIntOrNull() ?: DEFAULT_BASE_VALUE
    val updated = (current + delta).coerceIn(20, 600)
    _inputState.update {
      it.copy(valueInput = updated.toString(), feedbackMessage = null, isErrorFeedback = false)
    }
  }

  fun onTimingSelected(timing: BloodSugarTiming) {
    _inputState.update {
      it.copy(selectedTiming = timing, feedbackMessage = null, isErrorFeedback = false)
    }
  }

  fun onNoteChange(text: String) {
    _inputState.update {
      it.copy(noteInput = text.take(120), feedbackMessage = null, isErrorFeedback = false)
    }
  }

  fun saveRecord(
    invalidRangeMessage: String,
    savedSuccessMessage: String,
  ) {
    val current = _inputState.value
    val numericValue = current.valueInput.toLongOrNull()
    if (numericValue == null || numericValue !in 20L..600L) {
      _inputState.update {
        it.copy(feedbackMessage = invalidRangeMessage, isErrorFeedback = true)
      }
      return
    }

    viewModelScope.launch {
      _inputState.update { it.copy(isSaving = true, feedbackMessage = null) }
      val result =
        repository.addRecord(
          userId = currentUserId,
          valueMgDl = numericValue,
          timing = current.selectedTiming,
          note = current.noteInput,
        )
      result.fold(
        onSuccess = {
          _inputState.update {
            it.copy(
              valueInput = "",
              noteInput = "",
              isSaving = false,
              feedbackMessage = savedSuccessMessage,
              isErrorFeedback = false,
            )
          }
        },
        onFailure = { error ->
          _inputState.update {
            it.copy(
              isSaving = false,
              feedbackMessage = error.localizedMessage ?: "혈당 기록 저장에 실패했습니다.",
              isErrorFeedback = true,
            )
          }
        },
      )
    }
  }

  fun deleteRecord(recordId: String) {
    viewModelScope.launch {
      val result = repository.deleteRecord(userId = currentUserId, recordId = recordId)
      result.onFailure { error ->
        _inputState.update {
          it.copy(
            feedbackMessage = error.localizedMessage ?: "기록 삭제에 실패했습니다.",
            isErrorFeedback = true,
          )
        }
      }
    }
  }

  private companion object {
    const val TAG = "BloodSugarVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
    const val DEFAULT_BASE_VALUE = 110
  }
}
