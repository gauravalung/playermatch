package com.playermatch.app.ui.screens.player

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.playermatch.app.data.model.User
import com.playermatch.app.data.repository.AuthRepository
import com.playermatch.app.data.repository.UserRepository
import com.playermatch.app.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── ViewModel ─────────────────────────────────────────────────────────────────

class PlayerDetailViewModel : ViewModel() {

    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()

    val currentUserId: String? = authRepo.currentUserId

    private val _player = MutableStateFlow<User?>(null)
    val player: StateFlow<User?> = _player.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load(playerId: String) {
        viewModelScope.launch {
            userRepo.getUserFlow(playerId).collect { user ->
                _player.value = user
                _isLoading.value = false
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val joinedFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    playerId: String,
    navController: NavController,
    viewModel: PlayerDetailViewModel = viewModel(key = playerId)
) {
    LaunchedEffect(playerId) { viewModel.load(playerId) }

    val player by viewModel.player.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOwnProfile = playerId == viewModel.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(player?.name ?: "Player Profile") },
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

            player == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { Text("Player not found", style = MaterialTheme.typography.titleMedium) }

            else -> {
                val p = player!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Name
                    Text(p.name, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)

                    // Sport chip
                    AssistChip(
                        onClick = {},
                        label = { Text(p.sport) },
                        leadingIcon = {
                            Icon(Icons.Filled.SportsSoccer, null, Modifier.size(16.dp))
                        }
                    )

                    HorizontalDivider()

                    // Info rows
                    InfoRow(
                        icon = { Icon(Icons.Filled.EmojiEvents, null, Modifier.size(20.dp)) },
                        label = "Favourite sport",
                        value = p.sport
                    )

                    if (p.createdAt > 0) {
                        InfoRow(
                            icon = {
                                Icon(Icons.Filled.EmojiEvents, null, Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            label = "Member since",
                            value = joinedFormat.format(Date(p.createdAt))
                        )
                    }

                    // Message button — hidden for own profile
                    if (!isOwnProfile) {
                        val currentUid = viewModel.currentUserId ?: ""
                        val chatId = "${minOf(currentUid, p.uid)}_${maxOf(currentUid, p.uid)}"
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                navController.navigate(
                                    Screen.Chat.createRoute(chatId, p.uid, p.name)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Chat, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Message")
                        }
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
