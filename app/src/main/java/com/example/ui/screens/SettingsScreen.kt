package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlin.math.roundToInt
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val lyricsEnabled by viewModel.lyricsEnabled.collectAsState()
    val appFont by viewModel.appFont.collectAsState()

    val crossfadeDuration by viewModel.crossfadeDurationState.collectAsState()
    val gaplessPlayback by viewModel.gaplessPlaybackState.collectAsState()
    val replayGainEnabled by viewModel.replayGainEnabledState.collectAsState()

    val miniPlayerShape by viewModel.miniPlayerShape.collectAsState()
    val miniPlayerRadius by viewModel.miniPlayerRadius.collectAsState()
    val bottomBarShape by viewModel.bottomBarShape.collectAsState()
    val bottomBarRadius by viewModel.bottomBarRadius.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }

    val colorsOptions = listOf("Default", "Blue", "Green", "Red", "Purple", "Orange")

    // State to navigate categories: "main", "player", "looks", "customisation", "developer", "about"
    var currentSubPage by remember { mutableStateOf("main") }

    val pageTitle = when (currentSubPage) {
        "player" -> "Player Settings"
        "looks" -> "Looks & Feel"
        "customisation" -> "Customisation"
        "developer" -> "About Developer"
        "about" -> "About SKT Player"
        else -> "Settings"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = pageTitle, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSubPage != "main") {
                                currentSubPage = "main"
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (currentSubPage) {
                    "main" -> {
                        // 1. THEME MODE (Always visible at the top of settings as requested!)
                        Text(
                            text = "Theme Settings",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Choose Application Theme", fontWeight = FontWeight.Bold)
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("system" to "Follow System", "light" to "Light Mode", "dark" to "Dark Mode").forEach { (modeKey, modeName) ->
                                        val isSelected = themeMode == modeKey
                                        Button(
                                            onClick = { viewModel.setThemeMode(modeKey) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Text(modeName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. CATEGORIES SELECTOR
                        Text(
                            text = "Settings Categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                // Category 1: Player Settings
                                ListItem(
                                    headlineContent = { Text("Player Settings", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("Lyrics, Equalizer, Sleep Timer, Folders & filters") },
                                    leadingContent = { 
                                        Icon(
                                            Icons.Default.PlayCircle, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) 
                                    },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { currentSubPage = "player" }
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                                // Category 2: Looks and Feel
                                ListItem(
                                    headlineContent = { Text("Looks & Feels", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("App custom color accents, typography font styles") },
                                    leadingContent = { 
                                        Icon(
                                            Icons.Default.Palette, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) 
                                    },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { currentSubPage = "looks" }
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                                // Category 3: Customisation
                                ListItem(
                                    headlineContent = { Text("Customisation", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("Mini Player and Bottom Bar shapes, rounded corners") },
                                    leadingContent = { 
                                        Icon(
                                            Icons.Default.DashboardCustomize, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) 
                                    },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { currentSubPage = "customisation" }
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                                // Category 4: About Developer
                                ListItem(
                                    headlineContent = { Text("About Developer", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("Sumit Kumar Tiwari - Facebook, Insta, X, Website") },
                                    leadingContent = { 
                                        Icon(
                                            Icons.Default.ContactPage, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) 
                                    },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { currentSubPage = "developer" }
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                                // Category 5: Backups
                                ListItem(
                                    headlineContent = { Text("Backup & Restore Presets", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("Export favorites and custom 10-band EQ presets") },
                                    leadingContent = { 
                                        Icon(
                                            Icons.Default.Backup, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) 
                                    },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { showBackupDialog = true }
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 16.dp))

                                // Category 6: About App
                                ListItem(
                                    headlineContent = { Text("About SKT Player", fontWeight = FontWeight.Bold) },
                                    supportingContent = { Text("Read performance tips, developer insights & specs") },
                                    leadingContent = { 
                                        Icon(
                                            Icons.Default.Info, 
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) 
                                    },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { currentSubPage = "about" }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    "player" -> {
                        // PLAYER CATEGORY SUB-SCREEN
                        Text(
                            text = "Playback Customisations",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ListItem(
                                    headlineContent = { Text("Display Lyrics Overlay", fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("Show embedded synchronized lyrics inside full player") },
                                    leadingContent = { Icon(Icons.Default.TextSnippet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingContent = {
                                        Switch(
                                            checked = lyricsEnabled,
                                            onCheckedChange = { viewModel.setLyricsEnabled(it) }
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 0.dp)
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                ListItem(
                                    headlineContent = { Text("Gapless Playback", fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("Eliminate silences between consecutive music tracks") },
                                    leadingContent = { Icon(Icons.Default.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingContent = {
                                        Switch(
                                            checked = gaplessPlayback,
                                            onCheckedChange = { viewModel.setGaplessPlayback(it) }
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 0.dp)
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                ListItem(
                                    headlineContent = { Text("Replay Gain Normalization", fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("Normalize track volumes to prevent sudden loudness shifts") },
                                    leadingContent = { Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingContent = {
                                        Switch(
                                            checked = replayGainEnabled,
                                            onCheckedChange = { viewModel.setReplayGainEnabled(it) }
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 0.dp)
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text("Crossfade Duration", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                                Text("Blend tracks seamlessly", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text("${crossfadeDuration}s", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = crossfadeDuration.toFloat(),
                                        onValueChange = { viewModel.setCrossfadeDuration(it.roundToInt()) },
                                        valueRange = 0f..12f,
                                        steps = 11,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                // 10-Band EQ launching row
                                ListItem(
                                    headlineContent = { Text("10-Band Audio Equalizer", fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("Fine-tune sound frequencies, bass boost & reverb") },
                                    leadingContent = { Icon(Icons.Default.Equalizer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { onOpenEqualizer() }.padding(horizontal = 0.dp)
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                // Sleep timer launching row
                                val sleepRemaining by viewModel.sleepTimerRemaining.collectAsState()
                                val activeLabel = if (sleepRemaining > 0) "${sleepRemaining / 60000} mins left" else "Not Active"
                                ListItem(
                                    headlineContent = { Text("Configure Sleep Timer", fontWeight = FontWeight.SemiBold) },
                                    supportingContent = { Text("Auto-pause music when sleep timer ticks off ($activeLabel)") },
                                    leadingContent = { Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) },
                                    modifier = Modifier.clickable { onOpenSleepTimer() }.padding(horizontal = 0.dp)
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                // Playback Speed Controls
                                val currentSpeed by viewModel.playbackSpeed.collectAsState()
                                Text("Playback Speed Ratio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = "${String.format("%.2f", currentSpeed)}x",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(48.dp)
                                    )
                                    Slider(
                                        value = currentSpeed,
                                        onValueChange = { viewModel.setPlaybackSpeed(it) },
                                        valueRange = 0.5f..2.0f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Music Library Folders & Filters",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val skipFoldersText by viewModel.skipFolders.collectAsState()
                                val forceIncludeFoldersText by viewModel.forceIncludeFolders.collectAsState()
                                val minDurSetting by viewModel.minDuration.collectAsState()
                                val maxDurSetting by viewModel.maxDuration.collectAsState()

                                Text(
                                    "Skip Folders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Type or paste folder names/paths to exclude (comma separated):",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = skipFoldersText,
                                    onValueChange = { viewModel.setSkipFolders(it) },
                                    placeholder = { Text("e.g. temp, WhatsApp Audio, download") },
                                    modifier = Modifier.fillMaxWidth().testTag("skip_folders_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    "Force Include Folders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Folder names/paths to always include, overriding blacklist:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = forceIncludeFoldersText,
                                    onValueChange = { viewModel.setForceIncludeFolders(it) },
                                    placeholder = { Text("e.g. Music, FavoriteTracks") },
                                    modifier = Modifier.fillMaxWidth().testTag("force_folders_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    "Duration Filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Exclude songs outside the configured range below.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Min duration slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Min: ${minDurSetting}s",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(72.dp)
                                    )
                                    Slider(
                                        value = minDurSetting.toFloat(),
                                        onValueChange = { viewModel.setMinDuration(it.toInt()) },
                                        valueRange = 0f..300f,
                                        modifier = Modifier.weight(1f).testTag("duration_min_slider")
                                    )
                                }

                                // Max duration slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val displayMaxText = if (maxDurSetting >= 1800) "No Limit" else "${maxDurSetting / 60}m ${maxDurSetting % 60}s"
                                    Text(
                                        text = "Max: $displayMaxText",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(72.dp)
                                    )
                                    Slider(
                                        value = maxDurSetting.toFloat(),
                                        onValueChange = { viewModel.setMaxDuration(it.toInt()) },
                                        valueRange = 30f..1800f,
                                        modifier = Modifier.weight(1f).testTag("duration_max_slider")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    "looks" -> {
                        // LOOKS AND FEELS CATEGORY SUB-SCREEN
                        Text(
                            text = "Color & Aesthetic Theme",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Material You Color Accent Presets", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Selecting 'Default' enables organic Material You dynamic system accent coloring on Android 12+.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    colorsOptions.forEach { colorName ->
                                        val isSelected = accentColor == colorName
                                        val colorDot = when(colorName) {
                                            "Blue" -> Color(0xFF2196F3)
                                            "Green" -> Color(0xFF4CAF50)
                                            "Red" -> Color(0xFFE91E63)
                                            "Purple" -> Color(0xFF9C27B0)
                                            "Orange" -> Color(0xFFFF9800)
                                            else -> MaterialTheme.colorScheme.primary
                                        }

                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setAccentColor(colorName) },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(colorDot)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(colorName, fontSize = 12.sp)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // App Font Selection Screen Section
                        Text(
                            text = "Typography Font Style",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Change system typography across headings, titles, subtexts, and indicators instantly:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                val fontFamiliesList = listOf("Default", "Monospace", "Serif", "Cursive", "Sans-Serif")

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    fontFamiliesList.forEach { fontName ->
                                        val isSelected = appFont == fontName
                                        val previewFont = when(fontName) {
                                            "Monospace" -> FontFamily.Monospace
                                            "Serif" -> FontFamily.Serif
                                            "Cursive" -> FontFamily.Cursive
                                            "Sans-Serif" -> FontFamily.SansSerif
                                            else -> FontFamily.Default
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    else Color.Transparent
                                                )
                                                .clickable { viewModel.setAppFont(fontName) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.setAppFont(fontName) }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = fontName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = previewFont,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Quick brown fox jumps over the lazy dog",
                                                    fontSize = 11.sp,
                                                    fontFamily = previewFont,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    "customisation" -> {
                        // MINI PLAYER AND BOTTOM NAV CONFIGS
                        Text(
                            text = "Mini Player Customisation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Configure Mini Player Layout Shape",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Pill" to "Pill Shape", "Round" to "Round Shape", "Box" to "Box Shape").forEach { (shapeKey, label) ->
                                        val isSelected = miniPlayerShape == shapeKey
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setMiniPlayerShape(shapeKey) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Corner Radius Smoothness",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${miniPlayerRadius}dp",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Controls radius smooth index of the round shape",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Slider(
                                    value = miniPlayerRadius.toFloat(),
                                    onValueChange = { viewModel.setMiniPlayerRadius(it.toInt()) },
                                    valueRange = 0f..40f,
                                    enabled = miniPlayerShape == "Round",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Text(
                            text = "Bottom Navigation Bar Customisation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Configure Bottom Navigation Shape",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Pill" to "Pill Shape", "Round" to "Round Shape", "Box" to "Box Shape").forEach { (shapeKey, label) ->
                                        val isSelected = bottomBarShape == shapeKey
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.setBottomBarShape(shapeKey) },
                                            label = { Text(label, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Bar Corners Smooth Index",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${bottomBarRadius}dp",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Creates floating aesthetic when pill/round is activated",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Slider(
                                    value = bottomBarRadius.toFloat(),
                                    onValueChange = { viewModel.setBottomBarRadius(it.toInt()) },
                                    valueRange = 0f..40f,
                                    enabled = bottomBarShape == "Round",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    "developer" -> {
                        // ABOUT DEVELOPER SUB-PAGE
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFE91E8C),
                                                MaterialTheme.colorScheme.primary,
                                                Color(0xFF2196F3)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SKT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Sumit Kumar Tiwari",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Android & Web Developer Expert",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // "Follow us on" header as strictly requested
                            Text(
                                text = "Follow us on",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(bottom = 12.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                DeveloperLinkCard(
                                    platform = "Facebook",
                                    label = "Facebook Profile 1",
                                    username = "@sumitkumartiwari",
                                    url = "https://www.facebook.com/share/1Br6JeRcoT/",
                                    iconRes = R.drawable.ic_facebook,
                                    accentColor = Color(0xFF1877F2)
                                )

                                DeveloperLinkCard(
                                    platform = "Facebook",
                                    label = "Facebook Profile 2",
                                    username = "@sumitkumartiwari",
                                    url = "https://www.facebook.com/share/1CmyqrjawA/",
                                    iconRes = R.drawable.ic_facebook,
                                    accentColor = Color(0xFF1877F2)
                                )

                                DeveloperLinkCard(
                                    platform = "X (Twitter)",
                                    label = "X Account 1",
                                    username = "@sumit_tiwari19",
                                    url = "https://x.com/sumit_tiwari19",
                                    iconRes = R.drawable.ic_x,
                                    accentColor = Color.White
                                )

                                DeveloperLinkCard(
                                    platform = "X (Twitter)",
                                    label = "X Account 2",
                                    username = "@Sumit_Tiwari_19",
                                    url = "https://x.com/Sumit_Tiwari_19",
                                    iconRes = R.drawable.ic_x,
                                    accentColor = Color.White
                                )

                                DeveloperLinkCard(
                                    platform = "X (Twitter)",
                                    label = "X Account 3",
                                    username = "@sumit_tiwari95",
                                    url = "https://x.com/sumit_tiwari95",
                                    iconRes = R.drawable.ic_x,
                                    accentColor = Color.White
                                )

                                DeveloperLinkCard(
                                    platform = "Instagram",
                                    label = "Instagram Handle",
                                    username = "@sumitkumartiwari19",
                                    url = "https://instagram.com/sumitkumartiwari19",
                                    imageVector = Icons.Default.CameraAlt,
                                    accentColor = Color(0xFFE1306C)
                                )

                                DeveloperLinkCard(
                                    platform = "Website",
                                    label = "Personal Netlify Website",
                                    username = "sumitkumartiwari.netlify.app",
                                    url = "https://sumitkumartiwari.netlify.app/",
                                    imageVector = Icons.Default.Language,
                                    accentColor = Color(0xFF00C853)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Crafted with ♥ by Sumit Kumar Tiwari",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    "about" -> {
                        // ABOUT APP SUB-PAGE
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFE91E8C),
                                                MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "SKT Music Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "SKT",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "SKT Music Player",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "A high-performance modern offline music player crafted with Jetpack Compose. Enjoy a highly custom 10-band equalizer, synchronized lyrics, fine-tuned duration audio filters, responsive themes, customized backups, and precise sleep timers designed with maximum battery optimization.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Tip For Best Audio", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Disable system battery optimizations for the app if background audio shuts pauses prematurely. Enable local EQ filters to experience richer custom frequency ranges.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }

        // Backup visual output modal dialog
        if (showBackupDialog) {
            val backupText = viewModel.backupSettings()
            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                title = { Text("Backup Output String") },
                text = {
                    Column {
                        Text("Copy and preserve this offline string to easily restore your playlist favorites and EQ bands next time:", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = backupText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showBackupDialog = false
                        Toast.makeText(context, "Copied backup specs!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy & Done")
                    }
                }
            )
        }

        // Restore prompt dialog
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restore Sound Settings") },
                text = {
                    Column {
                        Text("Paste your previously exported backup string here:", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = restoreInputText,
                            onValueChange = { restoreInputText = it },
                            placeholder = { Text("FAVORITES:...|PRESET:...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.restoreSettings(restoreInputText)
                        showRestoreDialog = false
                        Toast.makeText(context, "Settings restored successfully!", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun DeveloperLinkCard(
    platform: String,
    label: String,
    username: String,
    url: String,
    iconRes: Int = 0,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accentColor: Color
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isPressed by remember { mutableStateOf(false) }

    // Click press scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "clickScale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url)
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open external browser", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLongPress = {
                        try {
                            clipboardManager.setText(AnnotatedString(url))
                            Toast.makeText(context, "$label link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            // fallback
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = "$platform icon",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "$platform icon",
                        tint = if (platform == "X (Twitter)" && accentColor == Color.White) MaterialTheme.colorScheme.onSurface else accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
