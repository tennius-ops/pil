package com.example.data

enum class MedicationTime {
  MORNING,
  LUNCH,
  DINNER,
}

data class DailyCheckRecord(
  val dateKey: String = "",
  val morningTaken: Boolean = false,
  val lunchTaken: Boolean = false,
  val dinnerTaken: Boolean = false,
  val waterCups: Int = 0,
) {
  val completedMedicationCount: Int
    get() = listOf(morningTaken, lunchTaken, dinnerTaken).count { it }
}
