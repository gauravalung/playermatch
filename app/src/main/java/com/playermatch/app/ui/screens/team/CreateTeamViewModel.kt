package com.playermatch.app.ui.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playermatch.app.data.model.Team
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.TeamRepository
import com.playermatch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class CreateTeamState {
    object Idle : CreateTeamState()
    object Loading : CreateTeamState()
    object Success : CreateTeamState()
    data class Error(val message: String) : CreateTeamState()
}

class CreateTeamViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val teamRepo = TeamRepository()

    private val _state = MutableStateFlow<CreateTeamState>(CreateTeamState.Idle)
    val state: StateFlow<CreateTeamState> = _state.asStateFlow()

    fun createTeam(
        sport: String,
        locationName: String,
        dateMillis: Long,
        hour: Int,
        minute: Int,
        totalSlots: Int,
        description: String
    ) {
        val uid = authRepo.currentUserId ?: return

        if (sport.isBlank() || locationName.isBlank() || totalSlots < 2) {
            _state.value = CreateTeamState.Error("Fill in all fields; minimum 2 slots.")
            return
        }

        // Merge selected date + time into a single epoch timestamp
        val scheduledTime = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        viewModelScope.launch {
            _state.value = CreateTeamState.Loading
            userRepo.getUser(uid)
                .onSuccess { user ->
                    val team = Team(
                        hostId = uid,
                        hostName = user?.name ?: "Player",
                        sport = sport,
                        locationName = locationName,
                        scheduledTime = scheduledTime,
                        totalSlots = totalSlots,
                        filledSlots = 1,   // host occupies one slot
                        description = description
                    )
                    teamRepo.createTeam(team)
                        .onSuccess { _state.value = CreateTeamState.Success }
                        .onFailure { _state.value = CreateTeamState.Error(it.message ?: "Failed to create game") }
                }
                .onFailure { _state.value = CreateTeamState.Error(it.message ?: "Could not load profile") }
        }
    }

    fun resetState() { _state.value = CreateTeamState.Idle }
}
