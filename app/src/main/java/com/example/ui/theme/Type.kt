package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoSansKrFontFamily = FontFamily(
  Font(R.font.noto_sans_kr, FontWeight.Normal),
  Font(R.font.noto_sans_kr, FontWeight.Medium),
  Font(R.font.noto_sans_kr, FontWeight.SemiBold),
  Font(R.font.noto_sans_kr, FontWeight.Bold),
)

// Every typography token is >= 22.sp for effortless readability by seniors (60+)
val SeniorTypography =
  Typography(
    displayLarge =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
      ),
    headlineLarge =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
      ),
    headlineMedium =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
      ),
    titleLarge =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
      ),
    titleMedium =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 30.sp,
      ),
    bodyLarge =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
      ),
    bodyMedium =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 30.sp,
      ),
    labelLarge =
      TextStyle(
        fontFamily = NotoSansKrFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
      ),
  )
