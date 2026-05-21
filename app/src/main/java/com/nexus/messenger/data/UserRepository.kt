package com.nexus.messenger.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = ""
)

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            usersCollection.document(userProfile.uid).set(userProfile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val documentSnapshot = usersCollection.document(uid).get().await()
            if (documentSnapshot.exists()) {
                val profile = documentSnapshot.toObject(UserProfile::class.java)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
