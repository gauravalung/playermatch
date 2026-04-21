package com.playermatch.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playermatch.app.data.model.User
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.LocationRepository
import com.playermatch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PlayerWithDistance(
    val user: User,
    val distanceKm: Double?   // null when current user has no saved location
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val locationRepo = LocationRepository(application)

    private val _nearbyPlayers = MutableStateFlow<List<PlayerWithDistance>>(emptyList())
    val nearbyPlayers: StateFlow<List<PlayerWithDistance>> = _nearbyPlayers.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedSport = MutableStateFlow("All")
    val selectedSport: StateFlow<String> = _selectedSport.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val uid = authRepo.currentUserId ?: return
        viewModelScope.launch {
            combine(
                userRepo.getUserFlow(uid),
                userRepo.getAllUsersFlow(),
                _selectedSport
            ) { currentUser, allUsers, sport ->
                Triple(currentUser, allUsers, sport)
            }.collect { (currentUser, allUsers, sport) ->
                _currentUser.value = currentUser
                val hasLocation = currentUser != null &&
                        (currentUser.latitude != 0.0 || currentUser.longitude != 0.0)

                val filtered = allUsers
                    .filter { it.uid != uid }
                    .filter { sport == "All" || it.sport == sport }
                    .map { user ->
                        val dist = if (hasLocation) {
                            locationRepo.distanceInKm(
                                currentUser!!.latitude, currentUser.longitude,
                                user.latitude, user.longitude
                            )
                        } else null
                        PlayerWithDistance(user, dist)
                    }
                    .sortedBy { it.distanceKm ?: Double.MAX_VALUE }

                _nearbyPlayers.value = filtered
                _isLoading.value = false
            }
        }
    }

    fun selectSport(sport: String) {
        _selectedSport.value = sport
    }
}
