package com.nexus.messenger.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    suspend fun signUp(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("User creation failed")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logIn(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Login failed")
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
