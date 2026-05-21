package com.nexus.messenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.messenger.data.AuthRepository
import com.nexus.messenger.data.UserProfile
import com.nexus.messenger.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val authResult = authRepository.signUp(email, password)
            authResult.onSuccess { userId ->
                val userProfile = UserProfile(uid = userId, email = email, displayName = displayName)
                val saveResult = userRepository.saveUserProfile(userProfile)
                saveResult.onSuccess {
                    _authState.value = AuthState.Success(userId)
                }.onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Failed to save user profile")
                }
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Sign up failed")
            }
        }
    }

    fun logIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.logIn(email, password)
            result.onSuccess { userId ->
                _authState.value = AuthState.Success(userId)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Log in failed")
            }
        }
    }
}
