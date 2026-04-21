package com.playermatch.app.ui.screens.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.playermatch.app.data.model.Team
import com.playermatch.app.data.model.User
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.LocationRepository
import com.playermatch.app.data.repository.TeamRepository
import com.playermatch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val teamRepo = TeamRepository()
    private val locationRepo = LocationRepository(application)

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    private val _nearbyUsers = MutableStateFlow<List<User>>(emptyList())
    val nearbyUsers: StateFlow<List<User>> = _nearbyUsers.asStateFlow()

    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    fun hasLocationPermission() = locationRepo.hasLocationPermission()

    fun startObserving() {
        val uid = authRepo.currentUserId ?: return

        // Observe user's own saved location
        viewModelScope.launch {
            userRepo.getUserFlow(uid).collect { user ->
                if (user != null && (user.latitude != 0.0 || user.longitude != 0.0)) {
                    _currentLocation.value = LatLng(user.latitude, user.longitude)
                }
            }
        }

        // Observe all other users who have a location set
        viewModelScope.launch {
            userRepo.getAllUsersFlow().collect { users ->
                _nearbyUsers.value = users.filter {
                    it.uid != uid && (it.latitude != 0.0 || it.longitude != 0.0)
                }
            }
        }

        // Observe teams that have coordinates (added when team host sets location)
        viewModelScope.launch {
            teamRepo.getAllTeamsFlow().collect { teams ->
                _teams.value = teams.filter { it.latitude != 0.0 || it.longitude != 0.0 }
            }
        }
    }

    // Called after permission granted — fetches GPS fix and persists to Firestore
    fun fetchAndSaveLocation() {
        val uid = authRepo.currentUserId ?: return
        _locationError.value = null
        viewModelScope.launch {
            locationRepo.getCurrentLocation()
                .onSuccess { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    _currentLocation.value = latLng
                    userRepo.updateLocation(uid, location.latitude, location.longitude)
                }
                .onFailure {
                    _locationError.value = "Could not fetch location: ${it.message}"
                }
        }
    }

    fun clearError() { _locationError.value = null }
}
