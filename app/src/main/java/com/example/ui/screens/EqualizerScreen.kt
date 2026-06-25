package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEqEnabled by viewModel.eqEnabled.collectAsState()
    val bands by viewModel.eqBands.collectAsState()
    val bassBoost by viewModel.bassBoost.collectAsState()
    val virtualizer by viewModel.virtualizer.collectAsState()
    val currentPreset by viewModel.currentPreset.collectAsState()

    val presets = listOf("Normal", "Pop", "Rock", "Classical", "Jazz", "Heavy Metal", "Dance")
    val bandFrequencies = listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sound Equalizer", fontWeight = FontWeight.Bold) },
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
                .padding(bottom = 120.dp, start = 16.dp, end = 16.dp)
        ) {
            // Master Toggle Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEqEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Master Equalizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEqEnabled) "Processing sound effects" else "Hardware bypass active",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isEqEnabled,
                        onCheckedChange = { viewModel.setEqEnabled(it) },
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets Horizontal Scroller
            Text("Presets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = currentPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isEqEnabled) {
                                viewModel.applyEqualizerPreset(preset)
                            }
                        },
                        label = { Text(preset) },
                        enabled = isEqEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 10-Band Sliders
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "10-Band Frequencies Adjustment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isEqEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Text("Enable Master Equalizer above to adjust", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        // Adaptive horizontal flow of compact vertical sliders!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            for (index in 0 until 10) {
                                val gainVal = bands[index]
                                val freqName = bandFrequencies[index]

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(56.dp)
                                ) {
                                    Text(
                                        text = "${if (gainVal > 0) "+" else ""}$gainVal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Box(
                                        modifier = Modifier
                                            .height(240.dp)
                                            .width(56.dp)
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Vertical slider representation in Compose
                                        Slider(
                                            value = gainVal.toFloat(),
                                            onValueChange = { newVal ->
                                                val updatedBands = bands.copyOf()
                                                updatedBands[index] = newVal.roundToInt()
                                                viewModel.updateEqBands(updatedBands)
                                            },
                                            valueRange = -15f..15f,
                                            modifier = Modifier
                                                .size(240.dp, 56.dp)
                                                .graphicsLayer {
                                                    rotationZ = -90f // Rotate to vertical!
                                                }
                                        )
                                    }

                                    Text(freqName, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subwoofer Effects (Bass Boost & Virtualizer)
            Text("Subwoofer Effects", fontWeight = FontWeight.Bold, fontSize = 14.sp)

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Bass Boost Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bass Boost", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${(bassBoost / 10f).roundToInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { viewModel.setBassBoost(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = isEqEnabled,
                            modifier = Modifier.testTag("bass_boost_seeker")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Virtualizer Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Virtualizer (3D Circle Sound)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${(virtualizer / 10f).roundToInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Slider(
                            value = virtualizer.toFloat(),
                            onValueChange = { viewModel.setVirtualizer(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = isEqEnabled,
                            modifier = Modifier.testTag("virtualizer_seeker")
                        )
                    }
                }
            }
        }
    }
}
