package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val themeMode by viewModel.themeMode.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val lyricsEnabled by viewModel.lyricsEnabled.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputText by remember { mutableStateOf("") }

    val colorsOptions = listOf("Default", "Blue", "Green", "Red", "Purple", "Orange")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Theme Mode Preference
            Text(
                "Theme options",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ListItem(
                        headlineContent = { Text("App Theme Mode", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Switch light, dark or follow system obsidian") },
                        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingContent = {
                            var expandedThemeMenu by remember { mutableStateOf(false) }
                            Box {
                                Button(
                                    onClick = { expandedThemeMenu = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(themeMode.uppercase())
                                }
                                DropdownMenu(
                                    expanded = expandedThemeMenu,
                                    onDismissRequest = { expandedThemeMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Follow System") },
                                        onClick = {
                                            viewModel.setThemeMode("system")
                                            expandedThemeMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Light Mode") },
                                        onClick = {
                                            viewModel.setThemeMode("light")
                                            expandedThemeMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Dark Mode") },
                                        onClick = {
                                            viewModel.setThemeMode("dark")
                                            expandedThemeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Accent Color Customization
            Text(
                "Personalization",
                style = MaterialTheme.typography.titleSmall,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Material You Accent Colors", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Change primary buttons and progress bar highlighting. Selecting 'Default' enables organic Material You dynamic theme on Android 12+.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Vertical Flow color choice list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorsOptions.forEach { colorName ->
                            val isSelected = accentColor == colorName
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAccentColor(colorName) },
                                label = { Text(colorName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Player mechanics
            Text(
                "Media Mechanics",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ListItem(
                        headlineContent = { Text("Display Lyrics", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Enable synchronized embedded lyrics overlay") },
                        leadingContent = { Icon(Icons.Default.TextSnippet, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = lyricsEnabled,
                                onCheckedChange = { viewModel.setLyricsEnabled(it) }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Backup / Restore Options
            Text(
                "Backup & Restore presets",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ListItem(
                        headlineContent = { Text("Backup Sound Presets", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Serialize custom EQ and Favorite lists") },
                        leadingContent = { Icon(Icons.Outlined.Save, contentDescription = null) },
                        modifier = Modifier.clickable { showBackupDialog = true }
                    )
                    Divider()
                    ListItem(
                        headlineContent = { Text("Restore Sound Presets", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Paste serial strings to sync values") },
                        leadingContent = { Icon(Icons.Outlined.Restore, contentDescription = null) },
                        modifier = Modifier.clickable { showRestoreDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Help & Battery optimizations
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Battery Optimization Advice", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "If background audio pauses after locking your device, check that Android Battery Optimization is disabled for SKT Music Player under Settings > Apps.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
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
