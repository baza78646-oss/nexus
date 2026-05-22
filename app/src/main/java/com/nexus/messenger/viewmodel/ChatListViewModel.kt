package com.nexus.messenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.messenger.data.AuthRepository
import com.nexus.messenger.data.Chat
import com.nexus.messenger.data.ChatRepository
import com.nexus.messenger.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val chatRepository = ChatRepository()
    private val userRepository = UserRepository()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _newChatId = MutableStateFlow<String?>(null)
    val newChatId: StateFlow<String?> = _newChatId.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId != null) {
            viewModelScope.launch {
                _isLoading.value = true
                chatRepository.getChats(currentUserId)
                    .catch { e ->
                        _error.value = e.message ?: "Failed to load chats"
                        _isLoading.value = false
                    }
                    .collect { chatList ->
                        _chats.value = chatList
                        _isLoading.value = false
                    }
            }
        } else {
            _error.value = "User not authenticated"
        }
    }

    fun createNewChatByEmail(email: String) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId == null) {
            _error.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val userResult = userRepository.getUserProfileByEmail(email)
            userResult.onSuccess { profile ->
                if (profile != null) {
                    if (profile.uid == currentUserId) {
                         _error.value = "Cannot create a chat with yourself"
                         _isLoading.value = false
                         return@onSuccess
                    }
                    val createChatResult = chatRepository.createChat(currentUserId, profile.uid)
                    createChatResult.onSuccess { chatId ->
                        _newChatId.value = chatId
                    }.onFailure {
                        _error.value = it.message ?: "Failed to create chat"
                    }
                } else {
                    _error.value = "User with email $email not found"
                }
            }.onFailure {
                _error.value = it.message ?: "Error finding user"
            }

            _isLoading.value = false
        }
    }

    fun clearNewChatId() {
        _newChatId.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
