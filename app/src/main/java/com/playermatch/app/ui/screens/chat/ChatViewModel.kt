package com.playermatch.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playermatch.app.data.model.Message
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.ChatRepository
import com.playermatch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SendState {
    object Idle : SendState()
    object Sending : SendState()
    data class Error(val message: String) : SendState()
}

class ChatViewModel : ViewModel() {

    private val chatRepo = ChatRepository()
    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()

    val currentUserId: String? = authRepo.currentUserId

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    // Cached so we don't hit Firestore on every send
    private var currentUserName: String = "Player"

    fun startChat(chatId: String, otherUserId: String, otherUserName: String) {
        val uid = currentUserId ?: return

        // Fetch own name once, then create the chat doc if needed
        viewModelScope.launch {
            userRepo.getUser(uid).onSuccess { user ->
                currentUserName = user?.name ?: "Player"
                chatRepo.getOrCreateChat(
                    chatId = chatId,
                    uid1 = uid, name1 = currentUserName,
                    uid2 = otherUserId, name2 = otherUserName
                )
            }
        }

        // Start listening to messages independently (returns empty list until first message)
        viewModelScope.launch {
            chatRepo.getMessagesFlow(chatId).collect { msgs ->
                _messages.value = msgs
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        val uid = currentUserId ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _sendState.value = SendState.Sending
            chatRepo.sendMessage(chatId, uid, currentUserName, text.trim())
                .onSuccess { _sendState.value = SendState.Idle }
                .onFailure { _sendState.value = SendState.Error(it.message ?: "Failed to send") }
        }
    }

    fun resetSendState() { _sendState.value = SendState.Idle }
}
