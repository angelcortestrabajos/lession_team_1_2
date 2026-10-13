package com.example.lession_equipo1.repository

import com.example.lession_equipo1.model.HistoryEntry
import com.example.lession_equipo1.model.TrainingGoal
import com.example.lession_equipo1.model.UserProfile
import com.example.lession_equipo1.model.WorkoutHistory
import com.example.lession_equipo1.network.SupabaseClient
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val supabase = SupabaseClient.client

    suspend fun saveWorkoutHistory(workout: WorkoutHistory) {
        try {
            if (workout.id.isEmpty()) {
                db.collection("users").document(workout.userId)
                    .collection("workout_history")
                    .add(workout)
                    .await()
            } else {
                db.collection("users").document(workout.userId)
                    .collection("workout_history")
                    .document(workout.id)
                    .set(workout)
                    .await()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteWorkout(userId: String, workoutId: String) {
        try {
            db.collection("users").document(userId)
                .collection("workout_history")
                .document(workoutId)
                .delete()
                .await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getWorkoutHistory(userId: String): Flow<List<WorkoutHistory>> = flow {
        try {
            val snapshot = db.collection("users").document(userId)
                .collection("workout_history")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val history = snapshot.documents.map { doc ->
                doc.toObject(WorkoutHistory::class.java)?.apply { id = doc.id } ?: WorkoutHistory()
            }
            emit(history)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun saveGoal(goal: TrainingGoal) {
        try {
            if (goal.id.isEmpty()) {
                db.collection("users").document(goal.userId)
                    .collection("training_goals")
                    .add(goal)
                    .await()
            } else {
                db.collection("users").document(goal.userId)
                    .collection("training_goals")
                    .document(goal.id)
                    .set(goal)
                    .await()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteGoal(userId: String, goalId: String) {
        try {
            db.collection("users").document(userId)
                .collection("training_goals")
                .document(goalId)
                .delete()
                .await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getGoals(userId: String): Flow<List<TrainingGoal>> = flow {
        try {
            val snapshot = db.collection("users").document(userId)
                .collection("training_goals")
                .get()
                .await()
            
            val goals = snapshot.documents.map { doc ->
                doc.toObject(TrainingGoal::class.java)?.apply { id = doc.id } ?: TrainingGoal()
            }
            emit(goals)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun createOrUpdateProfile(uid: String, profile: UserProfile) {
        val userRef = db.collection("users").document(uid)
        val existingProfile = userRef.get().await()
        val createdAt = existingProfile.getTimestamp("created_at") ?: Timestamp.now()
        val profileToSave = profile.copy(
            createdAt = createdAt,
            lastLogin = Timestamp.now()
        )
        userRef.set(profileToSave, SetOptions.merge()).await()

        try {
            supabase.from("profiles").upsert(profileToSave)
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun saveMeasurement(uid: String, entry: HistoryEntry) {
        db.collection("users").document(uid)
            .collection("biometric_history")
            .add(entry)
            .await()

        try {
            val data = mapOf(
                "user_id" to uid,
                "heart_rate" to entry.heartRate,
                "fatigue" to entry.fatigue,
                "risk_level" to entry.riskLevel
            )
            supabase.from("biometric_history").insert(data)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun getHistory(uid: String): Flow<List<HistoryEntry>> = callbackFlow {
        val registration = db.collection("users").document(uid)
            .collection("biometric_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val history = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(HistoryEntry::class.java)?.apply {
                        id = doc.id
                    }
                } ?: emptyList()

                trySend(history)
            }

        awaitClose { registration.remove() }
    }

    suspend fun deleteMeasurement(uid: String, recordId: String) {
        db.collection("users").document(uid)
            .collection("biometric_history")
            .document(recordId)
            .delete()
            .await()
    }

    suspend fun deleteAllMeasurements(uid: String) {
        val snapshot = db.collection("users").document(uid)
            .collection("biometric_history")
            .get()
            .await()

        val batch = db.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    fun getHistoryByRange(uid: String, start: Long, end: Long): Flow<List<HistoryEntry>> = flow {
        try {
            val snapshot = db.collection("users").document(uid)
                .collection("biometric_history")
                .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(start)))
                .whereLessThanOrEqualTo("timestamp", Timestamp(Date(end)))
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val history = snapshot.documents.map { doc ->
                doc.toObject(HistoryEntry::class.java)?.apply { id = doc.id } ?: HistoryEntry()
            }
            emit(history)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
