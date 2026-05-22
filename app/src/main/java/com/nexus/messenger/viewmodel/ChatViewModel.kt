package com.nexus.messenger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.messenger.data.AuthRepository
import com.nexus.messenger.data.ChatRepository
import com.nexus.messenger.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatViewModel(private val chatId: String) : ViewModel() {
    private val authRepository = AuthRepository()
    private val chatRepository = ChatRepository()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentUserId = authRepository.getCurrentUserId()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        if (chatId.isEmpty()) return

        viewModelScope.launch {
            chatRepository.getMessages(chatId)
                .catch { e ->
                    _error.value = e.message ?: "Failed to load messages"
                }
                .collect { messageList ->
                    _messages.value = messageList
                }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val senderId = currentUserId ?: return

        viewModelScope.launch {
            chatRepository.sendMessage(chatId, senderId, text)
                .onFailure {
                    _error.value = it.message ?: "Failed to send message"
                }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
