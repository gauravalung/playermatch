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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.playermatch.app.data.model.JoinRequest
import com.playermatch.app.ui.navigation.Screen
import com.playermatch.app.data.model.Team
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.JoinRequestRepository
import com.playermatch.app.data.repository.TeamRepository
import com.playermatch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Sealed action state ────────────────────────────────────────────────────────

sealed class RequestActionState {
    object Idle : RequestActionState()
    object Loading : RequestActionState()
    data class Success(val message: String) : RequestActionState()
    data class Error(val message: String) : RequestActionState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class TeamDetailViewModel : ViewModel() {

    private val teamRepo = TeamRepository()
    private val joinRepo = JoinRequestRepository()
    private val userRepo = UserRepository()
    val authRepo = AuthRepository()

    val currentUserId: String? = authRepo.currentUserId

    private val _team = MutableStateFlow<Team?>(null)
    val team: StateFlow<Team?> = _team.asStateFlow()

    // All requests for this team — host reads these
    private val _allRequests = MutableStateFlow<List<JoinRequest>>(emptyList())
    val allRequests: StateFlow<List<JoinRequest>> = _allRequests.asStateFlow()

    // The current (non-host) user's own request — real-time status updates
    private val _myRequest = MutableStateFlow<JoinRequest?>(null)
    val myRequest: StateFlow<JoinRequest?> = _myRequest.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<RequestActionState>(RequestActionState.Idle)
    val actionState: StateFlow<RequestActionState> = _actionState.asStateFlow()

    fun load(teamId: String) {
        val uid = currentUserId ?: return

        // Real-time team listener so slot count stays live
        viewModelScope.launch {
            teamRepo.getTeamFlow(teamId).collect { team ->
                _team.value = team
                _isLoading.value = false
            }
        }

        // All requests for the team (host needs these)
        viewModelScope.launch {
            joinRepo.getRequestsForTeam(teamId).collect { _allRequests.value = it }
        }

        // Current user's own request for status display
        viewModelScope.launch {
            joinRepo.getUserRequestForTeam(teamId, uid).collect { _myRequest.value = it }
        }
    }

    // ── Player actions ─────────────────────────────────────────────────────────

    fun sendJoinRequest(teamId: String) {
        val uid = currentUserId ?: return
        val hostId = _team.value?.hostId ?: ""
        val sportName = _team.value?.sport ?: ""
        viewModelScope.launch {
            _actionState.value = RequestActionState.Loading
            userRepo.getUser(uid)
                .onSuccess { user ->
                    joinRepo.sendRequest(teamId, uid, user?.name ?: "Player", hostId, sportName)
                        .onSuccess { _actionState.value = RequestActionState.Success("Request sent!") }
                        .onFailure { _actionState.value = RequestActionState.Error(it.message ?: "Failed to send request") }
                }
                .onFailure { _actionState.value = RequestActionState.Error(it.message ?: "Could not load profile") }
        }
    }

    fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            _actionState.value = RequestActionState.Loading
            joinRepo.cancelRequest(requestId)
                .onSuccess { _actionState.value = RequestActionState.Success("Request cancelled") }
                .onFailure { _actionState.value = RequestActionState.Error(it.message ?: "Failed to cancel") }
        }
    }

    // Re-activates a rejected request without creating a new document
    fun resendRequest(requestId: String) {
        viewModelScope.launch {
            _actionState.value = RequestActionState.Loading
            joinRepo.resendRequest(requestId)
                .onSuccess { _actionState.value = RequestActionState.Success("Request resent!") }
                .onFailure { _actionState.value = RequestActionState.Error(it.message ?: "Failed to resend") }
        }
    }

    // ── Host actions ───────────────────────────────────────────────────────────

    fun acceptRequest(request: JoinRequest) {
        val hostName = _team.value?.hostName ?: "Host"
        viewModelScope.launch {
            _actionState.value = RequestActionState.Loading
            joinRepo.acceptRequest(request, hostName)
                .onSuccess { _actionState.value = RequestActionState.Success("${request.senderName} accepted!") }
                .onFailure { _actionState.value = RequestActionState.Error(it.message ?: "Failed to accept") }
        }
    }

    fun rejectRequest(request: JoinRequest) {
        val hostName = _team.value?.hostName ?: "Host"
        viewModelScope.launch {
            _actionState.value = RequestActionState.Loading
            joinRepo.rejectRequest(request, hostName)
                .onSuccess { _actionState.value = RequestActionState.Success("${request.senderName} rejected") }
                .onFailure { _actionState.value = RequestActionState.Error(it.message ?: "Failed to reject") }
        }
    }

    fun resetActionState() { _actionState.value = RequestActionState.Idle }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val dateTimeFormat = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamId: String,
    navController: NavController,
    viewModel: TeamDetailViewModel = viewModel(key = teamId)  // one VM per team
) {
    LaunchedEffect(teamId) { viewModel.load(teamId) }

    val team by viewModel.team.collectAsState()
    val allRequests by viewModel.allRequests.collectAsState()
    val myRequest by viewModel.myRequest.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val isHost = team?.hostId == viewModel.currentUserId
    val snackbarHostState = remember { SnackbarHostState() }
    val isActionBusy = actionState is RequestActionState.Loading

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is RequestActionState.Success -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            is RequestActionState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(team?.let { "${it.sport} Game" } ?: "Game Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            team == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("Game not found", style = MaterialTheme.typography.titleMedium) }

            else -> {
                val t = team!!
                val slotsLeft = t.totalSlots - t.filledSlots
                val isFull = slotsLeft <= 0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InfoCard(team = t)
                    SlotsCard(team = t, isFull = isFull, slotsLeft = slotsLeft)

                    if (isHost) {
                        val uid = viewModel.currentUserId
                        HostRequestsSection(
                            allRequests = allRequests,
                            isBusy = isActionBusy,
                            currentUserId = uid,
                            onAccept = { viewModel.acceptRequest(it) },
                            onReject = { viewModel.rejectRequest(it) },
                            onNavigateToChat = { chatId, otherUid, otherName ->
                                navController.navigate(
                                    Screen.Chat.createRoute(chatId, otherUid, otherName)
                                )
                            }
                        )
                    } else {
                        PlayerRequestSection(
                            myRequest = myRequest,
                            teamId = t.id,
                            isFull = isFull,
                            isBusy = isActionBusy,
                            hostId = t.hostId,
                            hostName = t.hostName,
                            currentUserId = viewModel.currentUserId,
                            onSend = { viewModel.sendJoinRequest(t.id) },
                            onCancel = { myRequest?.id?.let { viewModel.cancelRequest(it) } },
                            onResend = { myRequest?.id?.let { viewModel.resendRequest(it) } },
                            onMessageHost = {
                                val uid = viewModel.currentUserId ?: return@PlayerRequestSection
                                val chatId = "${minOf(uid, t.hostId)}_${maxOf(uid, t.hostId)}"
                                navController.navigate(
                                    Screen.Chat.createRoute(chatId, t.hostId, t.hostName)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Info card ─────────────────────────────────────────────────────────────────

@Composable
private fun InfoCard(team: Team) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(
                icon = { Icon(Icons.Filled.SportsSoccer, null, Modifier.size(18.dp)) },
                label = "Sport", value = team.sport
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DetailRow(
                icon = { Icon(Icons.Filled.Person, null, Modifier.size(18.dp)) },
                label = "Host", value = team.hostName
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            DetailRow(
                icon = { Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp)) },
                label = "Location", value = team.locationName.ifBlank { "Not specified" }
            )
            if (team.scheduledTime > 0) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DetailRow(
                    icon = { Icon(Icons.Filled.AccessTime, null, Modifier.size(18.dp)) },
                    label = "When",
                    value = dateTimeFormat.format(Date(team.scheduledTime))
                )
            }
            if (team.description.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("About", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(team.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ── Slots card ────────────────────────────────────────────────────────────────

@Composable
private fun SlotsCard(team: Team, isFull: Boolean, slotsLeft: Int) {
    val slotColor = if (isFull) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Groups, null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Players", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${team.filledSlots} / ${team.totalSlots}",
                    style = MaterialTheme.typography.titleMedium,
                    color = slotColor
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { team.filledSlots.toFloat() / team.totalSlots.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(),
                color = slotColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isFull) "Game is full"
                else "$slotsLeft slot${if (slotsLeft == 1) "" else "s"} remaining",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Player: join request section ──────────────────────────────────────────────

@Composable
private fun PlayerRequestSection(
    myRequest: JoinRequest?,
    teamId: String,
    isFull: Boolean,
    isBusy: Boolean,
    hostId: String,
    hostName: String,
    currentUserId: String?,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onResend: () -> Unit,
    onMessageHost: () -> Unit
) {
    when (myRequest?.status) {
        null -> {
            // No request sent yet
            Button(
                onClick = onSend,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isFull && !isBusy
            ) {
                if (isBusy) {
                    CircularProgressIndicator(Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.PersonAdd, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFull) "Game Full" else "Send Join Request")
                }
            }
        }

        JoinRequest.STATUS_PENDING -> {
            // Waiting for host
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.HourglassTop, null,
                        Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Request Sent",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Waiting for host approval…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            TextButton(
                onClick = onCancel,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text("Cancel Request", color = MaterialTheme.colorScheme.error)
            }
        }

        JoinRequest.STATUS_ACCEPTED -> {
            // Accepted — player is in
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CheckCircle, null,
                        Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("You're In! 🎉",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Your request was accepted. See you on the field!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            Button(
                onClick = onMessageHost,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Chat, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Message Host")
            }
        }

        JoinRequest.STATUS_REJECTED -> {
            // Rejected — offer resend option
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Request Rejected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                    Text("The host declined your request.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            OutlinedButton(
                onClick = onResend,
                enabled = !isFull && !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Send Request Again")
            }
        }

        else -> Unit
    }
}

// ── Host: request management section ─────────────────────────────────────────

@Composable
private fun HostRequestsSection(
    allRequests: List<JoinRequest>,
    isBusy: Boolean,
    currentUserId: String?,
    onAccept: (JoinRequest) -> Unit,
    onReject: (JoinRequest) -> Unit,
    onNavigateToChat: (chatId: String, otherUserId: String, otherUserName: String) -> Unit
) {
    val pending = allRequests.filter { it.status == JoinRequest.STATUS_PENDING }
    val accepted = allRequests.filter { it.status == JoinRequest.STATUS_ACCEPTED }

    // Pending requests
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Pending Requests (${pending.size})",
                style = MaterialTheme.typography.titleMedium
            )

            if (pending.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "No pending requests yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pending.forEachIndexed { index, request ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    else Spacer(Modifier.height(12.dp))
                    RequestRow(
                        request = request,
                        isBusy = isBusy,
                        onAccept = { onAccept(request) },
                        onReject = { onReject(request) }
                    )
                }
            }
        }
    }

    // Accepted members
    if (accepted.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Accepted Players (${accepted.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                accepted.forEachIndexed { index, request ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle, null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            request.senderName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (currentUserId != null) {
                            IconButton(
                                onClick = {
                                    val chatId = "${minOf(currentUserId, request.senderId)}_${maxOf(currentUserId, request.senderId)}"
                                    onNavigateToChat(chatId, request.senderId, request.senderName)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Chat,
                                    contentDescription = "Message ${request.senderName}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(
    request: JoinRequest,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Person, null,
            Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            request.senderName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        // Accept
        Button(
            onClick = onAccept,
            enabled = !isBusy,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isBusy) CircularProgressIndicator(Modifier.size(14.dp),
                color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            else Text("Accept", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(8.dp))
        // Reject
        OutlinedButton(
            onClick = onReject,
            enabled = !isBusy,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Reject", style = MaterialTheme.typography.labelLarge)
        }
    }
}

// ── Shared composable ─────────────────────────────────────────────────────────

@Composable
private fun DetailRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        icon()
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
