package com.playermatch.app.ui.screens.team

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.playermatch.app.data.model.Team
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── ViewModel ──────────────────────────────────────────────────────────────────

class TeamDetailViewModel : ViewModel() {
    private val teamRepo = TeamRepository()
    val authRepo = AuthRepository()

    private val _team = MutableStateFlow<Team?>(null)
    val team: StateFlow<Team?> = _team.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadTeam(teamId: String) {
        viewModelScope.launch {
            teamRepo.getTeam(teamId)
                .onSuccess { _team.value = it }
                .onFailure { }
            _isLoading.value = false
        }
    }
}

// ── Screen ─────────────────────────────────────────────────────────────────────

private val dateTimeFormat = SimpleDateFormat("EEEE, dd MMM yyyy • hh:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamId: String,
    navController: NavController,
    viewModel: TeamDetailViewModel = viewModel()
) {
    LaunchedEffect(teamId) { viewModel.loadTeam(teamId) }

    val team by viewModel.team.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isHost = team?.hostId == viewModel.authRepo.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(team?.sport?.let { "$it Game" } ?: "Game Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            team == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Game not found", style = MaterialTheme.typography.titleMedium)
            }

            else -> {
                val t = team!!
                val slotsLeft = t.totalSlots - t.filledSlots
                val isFull = slotsLeft <= 0
                val fillFraction = t.filledSlots.toFloat() / t.totalSlots.coerceAtLeast(1)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow(
                                icon = { Icon(Icons.Filled.SportsSoccer, null, modifier = Modifier.size(18.dp)) },
                                label = "Sport",
                                value = t.sport
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            InfoRow(
                                icon = { Icon(Icons.Filled.Person, null, modifier = Modifier.size(18.dp)) },
                                label = "Host",
                                value = t.hostName
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            InfoRow(
                                icon = { Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp)) },
                                label = "Location",
                                value = t.locationName.ifBlank { "Not specified" }
                            )
                            if (t.scheduledTime > 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                InfoRow(
                                    icon = { Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(18.dp)) },
                                    label = "When",
                                    value = dateTimeFormat.format(Date(t.scheduledTime))
                                )
                            }
                            if (t.description.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "About",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = t.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Slots card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Groups,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Players", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = "${t.filledSlots} / ${t.totalSlots}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isFull) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { fillFraction },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isFull) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isFull) "Game is full" else "$slotsLeft slot${if (slotsLeft == 1) "" else "s"} available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action button — join request implemented in STEP 6
                    if (!isHost) {
                        Button(
                            onClick = { /* Join request — STEP 6 */ },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !isFull
                        ) {
                            Text(if (isFull) "Game Full" else "Send Join Request")
                        }
                    } else {
                        Text(
                            text = "You are hosting this game. Join requests appear in Step 6.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        icon()
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
