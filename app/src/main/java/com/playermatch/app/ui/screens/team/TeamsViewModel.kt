package com.playermatch.app.ui.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playermatch.app.data.model.Team
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TeamsViewModel : ViewModel() {

    private val teamRepo = TeamRepository()
    private val authRepo = AuthRepository()

    val currentUserId: String? = authRepo.currentUserId

    private val _allTeams = MutableStateFlow<List<Team>>(emptyList())
    private val _selectedSport = MutableStateFlow("All")
    val selectedSport: StateFlow<String> = _selectedSport.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Derived list — auto-updates when either _allTeams or _selectedSport changes
    val teams: StateFlow<List<Team>> = combine(_allTeams, _selectedSport) { teams, sport ->
        if (sport == "All") teams else teams.filter { it.sport == sport }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            teamRepo.getAllTeamsFlow().collect {
                _allTeams.value = it
                _isLoading.value = false
            }
        }
    }

    fun selectSport(sport: String) { _selectedSport.value = sport }
}
