package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LibraryScreen
import com.example.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme() // Premium dark music theme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionWrapper {
                        MainAppContainer()
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Storage permission is recommended to read local audio files.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissionToRequest)
        // Set to true regardless so emulator / sample songs always function perfectly
        hasPermission = true
    }

    content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    val viewModel: MusicViewModel = viewModel()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Melody Player",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sleep Timer") },
                            onClick = {
                                showSleepTimerDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Audio Equalizer") },
                            onClick = {
                                showEqualizerDialog = true
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create Playlist") },
                            onClick = {
                                showCreatePlaylistDialog = true
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LibraryScreen(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Dialogs
            if (showSleepTimerDialog) {
                SleepTimerDialog(viewModel, onDismiss = { showSleepTimerDialog = false })
            }

            if (showEqualizerDialog) {
                EqualizerDialog(viewModel, onDismiss = { showEqualizerDialog = false })
            }

            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(viewModel, onDismiss = { showCreatePlaylistDialog = false })
            }
        }
    }
}

@Composable
fun SleepTimerDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val remainingMs by viewModel.sleepTimeRemaining.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (remainingMs != null) {
                    val totalSec = remainingMs!! / 1000
                    val min = totalSec / 60
                    val sec = totalSec % 60
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Active Countdown", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                text = String.format("%02d:%02d", min, sec),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text("Select duration:", fontWeight = FontWeight.SemiBold)
                
                val options = listOf(
                    5 to "5 Minutes",
                    10 to "10 Minutes",
                    15 to "15 Minutes",
                    30 to "30 Minutes",
                    60 to "60 Minutes"
                )

                options.forEach { (mins, label) ->
                    OutlinedButton(
                        onClick = {
                            viewModel.startSleepTimer(mins)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.startSleepTimerForEndOfSong()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("End of Current Song")
                }

                if (remainingMs != null) {
                    Button(
                        onClick = {
                            viewModel.stopSleepTimer()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Sleep Timer")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun EqualizerDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    val enabled by viewModel.eqEnabled.collectAsState()
    val preset by viewModel.eqPreset.collectAsState()
    val bands by viewModel.eqBands.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Audio Equalizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.toggleEqualizer(it) }
                    )
                }

                Text("Presets", fontWeight = FontWeight.SemiBold)
                
                var showPresetMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { showPresetMenu = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(preset)
                    }
                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false }
                    ) {
                        val presets = listOf("Normal", "Bass Boost", "Pop", "Rock", "Jazz", "Classical")
                        presets.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.setEqPreset(name)
                                    showPresetMenu = false
                                }
                            )
                        }
                    }
                }

                Divider()

                Text("5-Band Equalizer", fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val bandFreqs = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
                    bands.forEachIndexed { index, level ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text("${level}dB", fontSize = 10.sp, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { viewModel.setEqBand(index, it.toInt()) },
                                valueRange = -15f..15f,
                                steps = 30,
                                enabled = enabled,
                                modifier = Modifier
                                    .weight(1f)
                                    .width(16.dp)
                            )
                            Text(bandFreqs[index], fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createPlaylist(name.trim())
                        onDismiss()
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
