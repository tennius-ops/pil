package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MedicationTime
import com.example.ui.DailyCheckUiState
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.CreamSurfaceVariant
import com.example.ui.theme.DinnerIconColor
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.IvoryBackgroundBottom
import com.example.ui.theme.LunchIconColor
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedGreenDark
import com.example.ui.theme.MedGreenLightSurface
import com.example.ui.theme.MorningIconColor
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WaterBlue
import com.example.ui.theme.WaterBlueBorder
import com.example.ui.theme.WaterBlueDark
import com.example.ui.theme.WaterBlueSurface
import com.example.ui.theme.WaterCupOutline
import com.example.ui.theme.WaterFillDeep
import com.example.ui.theme.WaterFillSurface

private const val DAILY_WATER_GOAL = 8

@Composable
fun DailyCheckScreen(
  uiState: DailyCheckUiState,
  onToggleMedication: (MedicationTime) -> Unit,
  onAddWaterCup: () -> Unit,
  onRemoveWaterCup: () -> Unit,
  modifier: Modifier = Modifier,
  applySafeDrawingInsets: Boolean = true,
) {
  val haptic = LocalHapticFeedback.current
  val backgroundBrush = Brush.verticalGradient(listOf(IvoryBackground, IvoryBackgroundBottom))
  val boxModifier =
    if (applySafeDrawingInsets) {
      modifier.fillMaxSize().background(backgroundBrush).windowInsetsPadding(WindowInsets.safeDrawing)
    } else {
      modifier.fillMaxSize().background(backgroundBrush)
    }

  Box(
    modifier = boxModifier,
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 600.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
      // 1. Top Date Header
      TodayDateCard(
        formattedDate = uiState.formattedDate,
        dayOfWeek = uiState.dayOfWeek,
        completedMeds = uiState.record.completedMedicationCount,
      )

      // 2. Medication Section ('아침 약', '점심 약', '저녁 약')
      Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
          text = stringResource(R.string.med_section_title),
          style = MaterialTheme.typography.titleLarge,
          color = InkPrimary,
          modifier = Modifier.padding(horizontal = 4.dp),
        )

        MedicationCheckButton(
          label = stringResource(R.string.med_morning),
          isTaken = uiState.record.morningTaken,
          icon = Icons.Filled.WbTwilight,
          iconTint = MorningIconColor,
          iconContentDescription = stringResource(R.string.cd_morning_icon),
          testTag = "med_button_morning",
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleMedication(MedicationTime.MORNING)
          },
        )

        MedicationCheckButton(
          label = stringResource(R.string.med_lunch),
          isTaken = uiState.record.lunchTaken,
          icon = Icons.Filled.WbSunny,
          iconTint = LunchIconColor,
          iconContentDescription = stringResource(R.string.cd_lunch_icon),
          testTag = "med_button_lunch",
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleMedication(MedicationTime.LUNCH)
          },
        )

        MedicationCheckButton(
          label = stringResource(R.string.med_dinner),
          isTaken = uiState.record.dinnerTaken,
          icon = Icons.Filled.NightsStay,
          iconTint = DinnerIconColor,
          iconContentDescription = stringResource(R.string.cd_dinner_icon),
          testTag = "med_button_dinner",
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggleMedication(MedicationTime.DINNER)
          },
        )
      }

      // 3. Water Intake Section
      WaterIntakeSection(
        waterCups = uiState.record.waterCups,
        onAddWaterCup = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          onAddWaterCup()
        },
        onRemoveWaterCup = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onRemoveWaterCup()
        },
      )

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun TodayDateCard(
  formattedDate: String,
  dayOfWeek: String,
  completedMeds: Int,
) {
  val allDone = completedMeds == 3
  val badgeBg by
    animateColorAsState(
      targetValue = if (allDone) MedGreenLightSurface else CreamSurfaceVariant,
      animationSpec = tween(250),
      label = "badgeBg",
    )
  val badgeTextColor = if (allDone) MedGreenDark else InkSecondary

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("today_date_card"),
    shape = RoundedCornerShape(24.dp),
    color = CreamSurface,
    border = BorderStroke(2.dp, WarmBorder),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.CalendarToday,
          contentDescription = stringResource(R.string.cd_calendar_icon),
          tint = MedGreen,
          modifier = Modifier.size(28.dp),
        )
        Text(
          text = stringResource(R.string.today_date_label),
          style = MaterialTheme.typography.bodyLarge,
          color = InkSecondary,
        )
      }

      Text(
        text = formattedDate,
        style = MaterialTheme.typography.headlineLarge,
        color = InkPrimary,
        modifier = Modifier.testTag("today_date_text"),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = dayOfWeek,
          style = MaterialTheme.typography.titleLarge,
          color = MedGreenDark,
        )

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = badgeBg,
        ) {
          Text(
            text =
              if (allDone) {
                stringResource(R.string.med_all_done)
              } else {
                stringResource(R.string.med_progress_format, completedMeds)
              },
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = badgeTextColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MedicationCheckButton(
  label: String,
  isTaken: Boolean,
  icon: ImageVector,
  iconTint: Color,
  iconContentDescription: String,
  testTag: String,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.97f else 1f,
      animationSpec = spring(stiffness = Spring.StiffnessMedium),
      label = "medButtonScale",
    )

  val containerColor by
    animateColorAsState(
      targetValue = if (isTaken) MedGreen else CreamSurface,
      animationSpec = tween(220),
      label = "medButtonBg",
    )

  val borderColor by
    animateColorAsState(
      targetValue = if (isTaken) MedGreenDark else WarmBorder,
      animationSpec = tween(220),
      label = "medButtonBorder",
    )

  val titleColor by
    animateColorAsState(
      targetValue = if (isTaken) Color.White else InkPrimary,
      animationSpec = tween(220),
      label = "medButtonTitle",
    )

  val statusDoneText = stringResource(R.string.med_status_done)
  val statusPendingText = stringResource(R.string.med_status_pending)
  val accessibilityState = if (isTaken) "$label, $statusDoneText" else "$label, $statusPendingText"

  Surface(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 98.dp)
        .scale(scale)
        .clip(RoundedCornerShape(24.dp))
        .clickable(
          interactionSource = interactionSource,
          indication = androidx.compose.material3.ripple(),
          role = Role.Checkbox,
          onClick = onClick,
        )
        .semantics { contentDescription = accessibilityState }
        .testTag(testTag),
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
    border = BorderStroke(2.5.dp, borderColor),
    shadowElevation = if (isTaken) 4.dp else 2.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Box(
          modifier =
            Modifier.size(54.dp)
              .clip(CircleShape)
              .background(if (isTaken) Color.White.copy(alpha = 0.2f) else CreamSurfaceVariant),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = iconContentDescription,
            tint = if (isTaken) Color.White else iconTint,
            modifier = Modifier.size(34.dp),
          )
        }

        Text(
          text = label,
          style = MaterialTheme.typography.headlineMedium,
          color = titleColor,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = if (isTaken) statusDoneText else statusPendingText,
          style = MaterialTheme.typography.titleLarge,
          color = if (isTaken) Color.White else InkSecondary,
        )
        Icon(
          imageVector =
            if (isTaken) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
          contentDescription =
            if (isTaken) {
              stringResource(R.string.cd_checked_icon)
            } else {
              stringResource(R.string.cd_unchecked_icon)
            },
          tint = if (isTaken) Color.White else WarmBorder,
          modifier = Modifier.size(38.dp),
        )
      }
    }
  }
}

@Composable
private fun WaterIntakeSection(
  waterCups: Int,
  onAddWaterCup: () -> Unit,
  onRemoveWaterCup: () -> Unit,
) {
  val totalCupSlots = maxOf(DAILY_WATER_GOAL, ((waterCups + 3) / 4) * 4)
  val cupRows = (0 until totalCupSlots).chunked(4)
  val summaryCd = stringResource(R.string.cd_water_cups_summary, waterCups)

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("water_section_card"),
    shape = RoundedCornerShape(26.dp),
    color = WaterBlueSurface,
    border = BorderStroke(2.dp, WaterBlueBorder),
    shadowElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier.padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.water_section_title),
          style = MaterialTheme.typography.titleLarge,
          color = InkPrimary,
        )

        if (waterCups > 0) {
          val undoCd = stringResource(R.string.cd_water_undo)
          OutlinedButton(
            onClick = onRemoveWaterCup,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, WaterBlueBorder),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier =
              Modifier.heightIn(min = 52.dp)
                .semantics { contentDescription = undoCd }
                .testTag("remove_water_button"),
          ) {
            Text(
              text = stringResource(R.string.water_undo_button),
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = WaterBlueDark,
            )
          }
        }
      }

      // Cup drawings replacing the numeric cup count
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .semantics { contentDescription = summaryCd }
            .testTag("water_cups_visual"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        cupRows.forEach { rowIndices ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            rowIndices.forEach { index ->
              val isFilled = index < waterCups
              val cupFill by
                animateFloatAsState(
                  targetValue = if (isFilled) 1f else 0f,
                  animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                  label = "cupFill_$index",
                )
              val cupCd =
                if (isFilled) {
                  stringResource(R.string.cd_water_cup_filled, index + 1)
                } else {
                  stringResource(R.string.cd_water_cup_empty, index + 1)
                }

              WaterCupDrawing(
                fillFraction = cupFill,
                isFilled = isFilled,
                contentDesc = cupCd,
                modifier = Modifier.size(width = 58.dp, height = 68.dp).testTag("water_cup_$index"),
              )
            }
          }
        }
      }

      Text(
        text =
          if (waterCups >= DAILY_WATER_GOAL) {
            stringResource(R.string.water_goal_reached)
          } else {
            stringResource(R.string.water_goal_hint)
          },
        style = MaterialTheme.typography.bodyLarge,
        color = if (waterCups >= DAILY_WATER_GOAL) MedGreenDark else InkSecondary,
      )

      // Large primary water button at the bottom
      Button(
        onClick = onAddWaterCup,
        modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp).testTag("add_water_button"),
        shape = RoundedCornerShape(22.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = WaterBlue,
            contentColor = Color.White,
          ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.WaterDrop,
          contentDescription = stringResource(R.string.cd_water_drop_icon),
          modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = stringResource(R.string.water_add_button),
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
private fun WaterCupDrawing(
  fillFraction: Float,
  isFilled: Boolean,
  contentDesc: String,
  modifier: Modifier = Modifier,
) {
  val outlineColor by
    animateColorAsState(
      targetValue = if (isFilled) WaterCupOutline else WaterBlueBorder,
      animationSpec = tween(220),
      label = "cupOutlineColor",
    )

  Canvas(
    modifier = modifier.semantics { contentDescription = contentDesc },
  ) {
    val w = size.width
    val h = size.height
    val strokeWidth = 3.dp.toPx()

    val topLeftX = w * 0.14f
    val topRightX = w * 0.86f
    val bottomLeftX = w * 0.24f
    val bottomRightX = w * 0.76f
    val topY = h * 0.10f
    val bottomY = h * 0.92f
    val cupHeight = bottomY - topY

    val cupPath =
      Path().apply {
        moveTo(topLeftX, topY)
        lineTo(topRightX, topY)
        lineTo(bottomRightX, bottomY)
        lineTo(bottomLeftX, bottomY)
        close()
      }

    // Cup glass background
    drawPath(path = cupPath, color = Color.White)

    // Clipped animated water level inside the cup
    if (fillFraction > 0.01f) {
      clipPath(cupPath) {
        val waterTopY = bottomY - (cupHeight * 0.84f * fillFraction)
        val waterBrush =
          Brush.verticalGradient(
            colors = listOf(WaterFillSurface, WaterFillDeep),
            startY = waterTopY,
            endY = bottomY,
          )
        drawRect(
          brush = waterBrush,
          topLeft = Offset(0f, waterTopY),
          size = Size(w, bottomY - waterTopY + 6f),
        )
        // Bright meniscus highlight at the water surface
        drawLine(
          color = Color.White.copy(alpha = 0.65f),
          start = Offset(0f, waterTopY + 2f),
          end = Offset(w, waterTopY + 2f),
          strokeWidth = 2.5.dp.toPx(),
        )
      }
    }

    // Crisp cup rim & outline
    drawPath(
      path = cupPath,
      color = outlineColor,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Cup rim pill at the top
    drawRoundRect(
      color = outlineColor,
      topLeft = Offset(topLeftX - 3.dp.toPx(), topY - 1.5.dp.toPx()),
      size = Size((topRightX - topLeftX) + 6.dp.toPx(), 3.dp.toPx()),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
    )
  }
}
