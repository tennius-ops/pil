package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BloodSugarRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeRecords(userId: String): Flow<List<BloodSugarRecord>> {
    return db
      .collection("users")
      .document(userId)
      .collection("bloodSugarRecords")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          BloodSugarRecord::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      }
  }

  suspend fun addRecord(
    userId: String,
    valueMgDl: Long,
    timing: BloodSugarTiming,
    note: String?,
  ): Result<Unit> {
    var docPath = "users/$userId/bloodSugarRecords"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
      require(valueMgDl in 20L..600L) { "Blood sugar value must be between 20 and 600 mg/dL." }

      val now = Date()
      val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
      val timeStr = SimpleDateFormat("a h:mm", Locale.KOREAN).format(now)
      val trimmedNote = note?.trim()?.takeIf { it.isNotEmpty() }?.take(120)

      val docRef =
        db.collection("users").document(userId).collection("bloodSugarRecords").document()
      docPath = docRef.path

      val payload =
        mapOf(
          "userId" to uid,
          "valueMgDl" to valueMgDl,
          "timing" to timing.name,
          "measuredDate" to dateStr,
          "measuredTimeText" to timeStr,
          "note" to trimmedNote,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ).filterValues { it != null } as Map<String, Any>

      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteRecord(userId: String, recordId: String): Result<Unit> {
    var docPath = "users/$userId/bloodSugarRecords/$recordId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
      require(recordId.isNotBlank()) { "Record ID must not be blank for deletion." }

      val docRef =
        db
          .collection("users")
          .document(userId)
          .collection("bloodSugarRecords")
          .document(recordId)
      docPath = docRef.path
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }
}
