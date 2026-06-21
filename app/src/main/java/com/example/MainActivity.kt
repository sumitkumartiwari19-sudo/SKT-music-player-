package com.example

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MusicViewModel

// Core navigation state enum
enum class ActiveScreen {
    MAIN, EQUALIZER, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val viewModel: MusicViewModel = viewModel(
                factory = MusicViewModel.provideFactory(context)
            )

            // Dynamic User accent selections
            val userAccent by viewModel.accentColor.collectAsState()
            val userThemeMode by viewModel.themeMode.collectAsState()

            val isDarkSystem = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (userThemeMode) {
                "light" -> false
                "dark" -> true
                else -> isDarkSystem
            }

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                accentColorName = userAccent,
                dynamicColor = userAccent == "Default"
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigationShell(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationShell(viewModel: MusicViewModel) {
    val context = LocalContext.current

    // Active bottom tab: 0: Home, 1: Search, 2: Library
    var activeBottomTab by remember { mutableStateOf(0) }
    var activeRootScreen by remember { mutableStateOf(ActiveScreen.MAIN) }

    // Full Player Modal overlay state
    var showFullPlayerModal by remember { mutableStateOf(false) }
    var showSleepTimerShortcutDialog by remember { mutableStateOf(false) }

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playProgress by viewModel.playbackPosition.collectAsState()
    val songDuration by viewModel.duration.collectAsState()

    // Android Permission handling
    val musicPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanLocalFiles()
            Toast.makeText(context, "Storage scanner successfully configured!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Running in Offline demo mode.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        // Trigger storage scan permissions
        launcher.launch(musicPermission)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = activeRootScreen,
            transitionSpec = {
                SlideInVerticallyWithFade() togetherWith SlideOutVerticallyWithFade()
            },
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                ActiveScreen.MAIN -> {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            "SKT Music",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            letterSpacing = (-0.5).sp
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { activeRootScreen = ActiveScreen.SETTINGS },
                                        modifier = Modifier.testTag("settings_top_btn")
                                    ) {
                                        Icon(Icons.Outlined.Settings, contentDescription = "Settings Options")
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = activeBottomTab == 0,
                                    onClick = { activeBottomTab = 0 },
                                    label = { Text("Home") },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    modifier = Modifier.testTag("tab_home")
                                )
                                NavigationBarItem(
                                    selected = activeBottomTab == 1,
                                    onClick = { activeBottomTab = 1 },
                                    label = { Text("Search") },
                                    icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                    modifier = Modifier.testTag("tab_search")
                                )
                                NavigationBarItem(
                                    selected = activeBottomTab == 2,
                                    onClick = { activeBottomTab = 2 },
                                    label = { Text("Library") },
                                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                                    modifier = Modifier.testTag("tab_library")
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeBottomTab) {
                                0 -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToLibrary = { activeBottomTab = 2 },
                                    onOpenSleepTimer = { showSleepTimerShortcutDialog = true }
                                )
                                1 -> SearchScreen(viewModel = viewModel)
                                2 -> LibraryScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                ActiveScreen.EQUALIZER -> {
                    EqualizerScreen(
                        viewModel = viewModel,
                        onBackClick = { activeRootScreen = ActiveScreen.MAIN }
                    )
                }

                ActiveScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackClick = { activeRootScreen = ActiveScreen.MAIN }
                    )
                }
            }
        }

        // persistent mini player component hovering above the bottom navigation bar
        if (currentSong != null && activeRootScreen == ActiveScreen.MAIN) {
            val song = currentSong!!
            val safeProgress = if (songDuration > 0) playProgress.toFloat() / songDuration.toFloat() else 0f

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 16.dp, end = 16.dp) // fit cleanly above bottom bar
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                    .clickable { showFullPlayerModal = true }
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (song.albumArtUri != null && song.albumArtUri.startsWith("http")) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }

                    IconButton(onClick = { viewModel.next() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }
                }

                // Progress slide line at bottom of glass miniplayer
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(safeProgress)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Full player animated transition slides
        AnimatedVisibility(
            visible = showFullPlayerModal,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                onCloseClick = { showFullPlayerModal = false },
                onNavigateToEqualizer = {
                    showFullPlayerModal = false
                    activeRootScreen = ActiveScreen.EQUALIZER
                },
                onOpenSleepTimer = {
                    showFullPlayerModal = false
                    showSleepTimerShortcutDialog = true
                }
            )
        }

        // Sleep Timer setup Dialog Shortcut
        if (showSleepTimerShortcutDialog) {
            var inputMinutes by remember { mutableStateOf("15") }
            val sleepValRemaining by viewModel.sleepTimerRemaining.collectAsState()

            AlertDialog(
                onDismissRequest = { showSleepTimerShortcutDialog = false },
                title = { Text("Sleep Timer Setup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pause playback automatically after selected period. Enter minutes duration below:", fontSize = 12.sp)
                        OutlinedTextField(
                            value = inputMinutes,
                            onValueChange = { inputMinutes = it },
                            label = { Text("Duration (Minutes)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (sleepValRemaining > 0) {
                            Text(
                                "Remaining time active: ${sleepValRemaining / 60000} mins left.",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val mins = inputMinutes.toIntOrNull() ?: 15
                        viewModel.startSleepTimer(mins)
                        showSleepTimerShortcutDialog = false
                    }) {
                        Text("Start Timer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.stopSleepTimer()
                        showSleepTimerShortcutDialog = false
                    }) {
                        Text("Disable Timer")
                    }
                }
            )
        }
    }
}

// Fluid Navigation transitions help
@OptIn(ExperimentalAnimationApi::class)
private fun SlideInVerticallyWithFade() = slideInVertically(
    initialOffsetY = { 300 }
) + fadeIn()

@OptIn(ExperimentalAnimationApi::class)
private fun SlideOutVerticallyWithFade() = slideOutVertically(
    targetOffsetY = { -300 }
) + fadeOut()
