package com.playermatch.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playermatch.app.data.model.User
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    object Success : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _updateState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val updateState: StateFlow<ProfileUiState> = _updateState.asStateFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            userRepo.getUserFlow(uid).collect { _user.value = it }
        }
    }

    fun updateProfile(name: String, sport: String) {
        val current = _user.value ?: return
        if (name.isBlank() || sport.isBlank()) {
            _updateState.value = ProfileUiState.Error("Name and sport cannot be empty")
            return
        }
        viewModelScope.launch {
            _updateState.value = ProfileUiState.Loading
            val updated = current.copy(name = name.trim(), sport = sport)
            userRepo.updateUser(updated)
                .onSuccess { _updateState.value = ProfileUiState.Success }
                .onFailure { _updateState.value = ProfileUiState.Error(it.message ?: "Update failed") }
        }
    }

    fun resetUpdateState() {
        _updateState.value = ProfileUiState.Idle
    }

    fun logout() = authRepo.logout()
}
