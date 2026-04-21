package com.playermatch.app.ui.screens.team

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.playermatch.app.data.Sports
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
private fun fmtTime(h: Int, m: Int) = "%02d:%02d %s".format(
    if (h == 0 || h == 12) 12 else h % 12, m, if (h < 12) "AM" else "PM"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    onTeamCreated: () -> Unit,
    viewModel: CreateTeamViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form state
    var selectedSport by remember { mutableStateOf("") }
    var sportExpanded by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalSlots by remember { mutableIntStateOf(6) }

    // Date / time pickers
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(initialHour = 18, initialMinute = 0)
    var selectedHour by remember { mutableIntStateOf(18) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when (val s = state) {
            is CreateTeamState.Success -> {
                viewModel.resetState()
                onTeamCreated()
            }
            is CreateTeamState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetState()
            }
            else -> Unit
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Game") },
                navigationIcon = {
                    IconButton(onClick = onTeamCreated) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sport
            ExposedDropdownMenuBox(
                expanded = sportExpanded,
                onExpandedChange = { sportExpanded = !sportExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedSport,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport *") },
                    leadingIcon = { Icon(Icons.Filled.SportsSoccer, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = sportExpanded,
                    onDismissRequest = { sportExpanded = false }
                ) {
                    Sports.LIST.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport) },
                            onClick = { selectedSport = sport; sportExpanded = false }
                        )
                    }
                }
            }

            // Location name
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Location / Turf name *") },
                leadingIcon = { Icon(Icons.Filled.LocationOn, null) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date picker trigger
            OutlinedTextField(
                value = dateDisplay.format(Date(selectedDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date *") },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, null) },
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Change") }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Time picker trigger
            OutlinedTextField(
                value = fmtTime(selectedHour, selectedMinute),
                onValueChange = {},
                readOnly = true,
                label = { Text("Time *") },
                leadingIcon = { Icon(Icons.Filled.AccessTime, null) },
                trailingIcon = {
                    TextButton(onClick = { showTimePicker = true }) { Text("Change") }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Slots stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Slots *",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalIconButton(onClick = { if (totalSlots > 2) totalSlots-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "$totalSlots",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(16.dp))
                FilledTonalIconButton(onClick = { if (totalSlots < 22) totalSlots++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase")
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(Modifier.height(4.dp))

            // Submit
            Button(
                onClick = {
                    viewModel.createTeam(
                        sport = selectedSport,
                        locationName = locationName.trim(),
                        dateMillis = selectedDateMillis,
                        hour = selectedHour,
                        minute = selectedMinute,
                        totalSlots = totalSlots,
                        description = description.trim()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = selectedSport.isNotBlank() && locationName.isNotBlank() &&
                          state !is CreateTeamState.Loading
            ) {
                if (state is CreateTeamState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Game")
                }
            }
        }
    }
}
