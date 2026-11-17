package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.BloodSugarRecord
import com.example.data.BloodSugarTiming
import com.example.ui.BloodSugarInputUiState
import com.example.ui.BloodSugarListUiState
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.CreamSurfaceVariant
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.IvoryBackgroundBottom
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedGreenDark
import com.example.ui.theme.MedGreenLightSurface
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WaterBlue
import com.example.ui.theme.WaterBlueDark
import com.example.ui.theme.WaterBlueSurface

@Composable
fun BloodSugarScreen(
  inputState: BloodSugarInputUiState,
  listState: BloodSugarListUiState,
  onValueChange: (String) -> Unit,
  onAdjustValue: (Int) -> Unit,
  onTimingSelected: (BloodSugarTiming) -> Unit,
  onNoteChange: (String) -> Unit,
  onSaveClick: () -> Unit,
  onDeleteRecord: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val backgroundBrush = Brush.verticalGradient(listOf(IvoryBackground, IvoryBackgroundBottom))

  Box(
    modifier = modifier.fillMaxSize().background(backgroundBrush),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
      // 1. Input Card
      BloodSugarInputCard(
        inputState = inputState,
        onValueChange = onValueChange,
        onAdjustValue = { delta ->
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onAdjustValue(delta)
        },
        onTimingSelected = { timing ->
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onTimingSelected(timing)
        },
        onNoteChange = onNoteChange,
        onSaveClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onSaveClick()
        },
      )

      // 2. Saved History Section
      Text(
        text = stringResource(R.string.blood_sugar_history_title),
        style = MaterialTheme.typography.titleLarge,
        color = InkPrimary,
        modifier = Modifier.padding(horizontal = 4.dp),
      )

      when (listState) {
        BloodSugarListUiState.Loading -> {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = CreamSurface,
            border = BorderStroke(2.dp, WarmBorder),
          ) {
            Column(
              modifier = Modifier.padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              CircularProgressIndicator(color = MedGreen, modifier = Modifier.size(40.dp))
              Text(
                text = stringResource(R.string.blood_sugar_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = InkSecondary,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        is BloodSugarListUiState.Error -> {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFDECEC),
            border = BorderStroke(2.dp, Color(0xFFD32F2F)),
          ) {
            Text(
              text = listState.message,
              style = MaterialTheme.typography.bodyLarge,
              color = Color(0xFFB71C1C),
              modifier = Modifier.padding(20.dp),
            )
          }
        }

        is BloodSugarListUiState.Success -> {
          if (listState.records.isEmpty()) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("blood_sugar_empty_card"),
              shape = RoundedCornerShape(24.dp),
              color = CreamSurface,
              border = BorderStroke(2.dp, WarmBorder),
            ) {
              Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Bloodtype,
                  contentDescription = stringResource(R.string.cd_blood_sugar_icon),
                  tint = MedGreen,
                  modifier = Modifier.size(44.dp),
                )
                Text(
                  text = stringResource(R.string.blood_sugar_empty),
                  style = MaterialTheme.typography.bodyLarge,
                  color = InkSecondary,
                  textAlign = TextAlign.Center,
                )
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
              listState.records.forEach { record ->
                BloodSugarHistoryCard(
                  record = record,
                  onDeleteClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteRecord(record.id)
                  },
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun BloodSugarInputCard(
  inputState: BloodSugarInputUiState,
  onValueChange: (String) -> Unit,
  onAdjustValue: (Int) -> Unit,
  onTimingSelected: (BloodSugarTiming) -> Unit,
  onNoteChange: (String) -> Unit,
  onSaveClick: () -> Unit,
) {
  val timingRows = BloodSugarTiming.entries.chunked(2)

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("blood_sugar_input_card"),
    shape = RoundedCornerShape(26.dp),
    color = CreamSurface,
    border = BorderStroke(2.dp, WarmBorder),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Bloodtype,
          contentDescription = stringResource(R.string.cd_blood_sugar_icon),
          tint = Color(0xFFC62828),
          modifier = Modifier.size(34.dp),
        )
        Text(
          text = stringResource(R.string.blood_sugar_input_title),
          style = MaterialTheme.typography.headlineMedium,
          color = InkPrimary,
        )
      }

      // Timing selector 2x2 grid
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.blood_sugar_timing_label),
          style = MaterialTheme.typography.titleMedium,
          color = InkSecondary,
        )

        timingRows.forEach { rowTimings ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            rowTimings.forEach { timing ->
              val isSelected = inputState.selectedTiming == timing
              val bgColor by
                animateColorAsState(
                  targetValue = if (isSelected) MedGreen else CreamSurfaceVariant,
                  animationSpec = tween(200),
                  label = "timingBg_${timing.name}",
                )
              val textColor = if (isSelected) Color.White else InkPrimary
              val borderColor = if (isSelected) MedGreenDark else WarmBorder

              Surface(
                modifier =
                  Modifier.weight(1f)
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(role = Role.RadioButton) { onTimingSelected(timing) }
                    .testTag("timing_${timing.name}"),
                shape = RoundedCornerShape(18.dp),
                color = bgColor,
                border = BorderStroke(2.dp, borderColor),
              ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = stringResource(timing.labelRes),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = textColor,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          }
        }
      }

      // Numeric value input + -10 / +10 quick steppers
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = stringResource(R.string.blood_sugar_value_label),
          style = MaterialTheme.typography.titleMedium,
          color = InkSecondary,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { onAdjustValue(-10) },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, WarmBorder),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier.heightIn(min = 64.dp).testTag("blood_sugar_minus_10"),
          ) {
            Text(
              text = stringResource(R.string.blood_sugar_minus_10),
              style = MaterialTheme.typography.titleLarge,
              color = InkPrimary,
            )
          }

          OutlinedTextField(
            value = inputState.valueInput,
            onValueChange = onValueChange,
            placeholder = {
              Text(
                text = stringResource(R.string.blood_sugar_value_hint),
                style = MaterialTheme.typography.titleLarge,
                color = InkSecondary.copy(alpha = 0.6f),
              )
            },
            suffix = {
              Text(
                text = stringResource(R.string.blood_sugar_unit),
                style = MaterialTheme.typography.bodyLarge,
                color = InkSecondary,
              )
            },
            singleLine = true,
            textStyle =
              MaterialTheme.typography.headlineMedium.copy(
                color = InkPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
              ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(18.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MedGreen,
                unfocusedBorderColor = WarmBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
              ),
            modifier = Modifier.weight(1f).testTag("blood_sugar_value_input"),
          )

          OutlinedButton(
            onClick = { onAdjustValue(10) },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, WarmBorder),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier.heightIn(min = 64.dp).testTag("blood_sugar_plus_10"),
          ) {
            Text(
              text = stringResource(R.string.blood_sugar_plus_10),
              style = MaterialTheme.typography.titleLarge,
              color = InkPrimary,
            )
          }
        }
      }

      // Optional memo
      OutlinedTextField(
        value = inputState.noteInput,
        onValueChange = onNoteChange,
        label = {
          Text(
            text = stringResource(R.string.blood_sugar_note_label),
            style = MaterialTheme.typography.bodyLarge,
          )
        },
        placeholder = {
          Text(
            text = stringResource(R.string.blood_sugar_note_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = InkSecondary.copy(alpha = 0.6f),
          )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkPrimary),
        shape = RoundedCornerShape(18.dp),
        colors =
          OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MedGreen,
            unfocusedBorderColor = WarmBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
          ),
        modifier = Modifier.fillMaxWidth().testTag("blood_sugar_note_input"),
      )

      if (inputState.feedbackMessage != null) {
        val bgColor = if (inputState.isErrorFeedback) Color(0xFFFDECEC) else MedGreenLightSurface
        val borderColor = if (inputState.isErrorFeedback) Color(0xFFD32F2F) else MedGreen
        val textColor = if (inputState.isErrorFeedback) Color(0xFFB71C1C) else MedGreenDark

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = bgColor,
          border = BorderStroke(1.5.dp, borderColor),
          modifier = Modifier.fillMaxWidth().testTag("blood_sugar_feedback"),
        ) {
          Text(
            text = inputState.feedbackMessage,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      Button(
        onClick = onSaveClick,
        enabled = !inputState.isSaving,
        modifier =
          Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag("save_blood_sugar_button"),
        shape = RoundedCornerShape(22.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MedGreen,
            contentColor = Color.White,
          ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
      ) {
        if (inputState.isSaving) {
          CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = Color.White,
            strokeWidth = 3.dp,
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.blood_sugar_saving),
            style = MaterialTheme.typography.labelLarge,
          )
        } else {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.blood_sugar_save_button),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun BloodSugarHistoryCard(
  record: BloodSugarRecord,
  onDeleteClick: () -> Unit,
) {
  val timing = record.resolvedTiming()
  val (statusLabelRes, statusBg, statusTextColor) = resolveStatusBadge(record.valueMgDl, timing)

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("blood_sugar_record_${record.id}"),
    shape = RoundedCornerShape(22.dp),
    color = CreamSurface,
    border = BorderStroke(2.dp, WarmBorder),
    shadowElevation = 1.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "${record.valueMgDl} ${stringResource(R.string.blood_sugar_unit)}",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
            color = InkPrimary,
          )
          Surface(shape = RoundedCornerShape(14.dp), color = statusBg) {
            Text(
              text = stringResource(statusLabelRes),
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = statusTextColor,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(shape = RoundedCornerShape(12.dp), color = WaterBlueSurface) {
            Text(
              text = stringResource(timing.labelRes),
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = WaterBlueDark,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "${record.measuredDate} · ${record.measuredTimeText}",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSecondary,
          )
        }

        if (!record.note.isNullOrBlank()) {
          Text(
            text = record.note,
            style = MaterialTheme.typography.bodyLarge,
            color = InkPrimary,
          )
        }
      }

      IconButton(
        onClick = onDeleteClick,
        modifier = Modifier.size(56.dp).testTag("delete_blood_sugar_${record.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = stringResource(R.string.cd_delete_blood_sugar),
          tint = InkSecondary,
          modifier = Modifier.size(32.dp),
        )
      }
    }
  }
}

private fun resolveStatusBadge(
  valueMgDl: Long,
  timing: BloodSugarTiming,
): Triple<Int, Color, Color> {
  if (valueMgDl < 70L) {
    return Triple(R.string.blood_sugar_status_low, WaterBlueSurface, WaterBlue)
  }
  val highThreshold =
    when (timing) {
      BloodSugarTiming.FASTING,
      BloodSugarTiming.BEFORE_MEAL -> 126L
      BloodSugarTiming.AFTER_MEAL -> 180L
      BloodSugarTiming.BEDTIME -> 150L
    }
  return if (valueMgDl >= highThreshold) {
    Triple(R.string.blood_sugar_status_high, Color(0xFFFDECEC), Color(0xFFB71C1C))
  } else {
    Triple(R.string.blood_sugar_status_normal, MedGreenLightSurface, MedGreenDark)
  }
}
