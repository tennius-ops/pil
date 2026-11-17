package com.example.data

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class BloodSugarTiming(@StringRes val labelRes: Int) {
  FASTING(R.string.timing_fasting),
  BEFORE_MEAL(R.string.timing_before_meal),
  AFTER_MEAL(R.string.timing_after_meal),
  BEDTIME(R.string.timing_bedtime);

  companion object {
    fun fromRaw(raw: String?): BloodSugarTiming =
      enumValues<BloodSugarTiming>().firstOrNull { it.name == raw } ?: FASTING
  }
}

@Keep
@IgnoreExtraProperties
data class BloodSugarRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val valueMgDl: Long = 0L,
  val timing: String = BloodSugarTiming.FASTING.name,
  val measuredDate: String = "",
  val measuredTimeText: String = "",
  val note: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedTiming(): BloodSugarTiming = BloodSugarTiming.fromRaw(timing)
}
