package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = Color(0xFFE91E8C) // pink/purple accent color
    val textLight = Color.White
    val textMuted = Color.White.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1025))
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Spacer for status bar safety bounds
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // TOP SECTION - App Branding:
            Spacer(modifier = Modifier.height(16.dp))

            // SKT Music Player icon (large, centered or left aligned)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                accentColor,
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
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "SKT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "SKT Music Player" text below icon
            Text(
                text = "SKT Music Player",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = textLight,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Thin divider line
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.15f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // MIDDLE SECTION - Developer Info:
            // Developer name with person icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Developer",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Developer",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sumit Kumar Tiwari",
                        fontSize = 15.sp,
                        color = textLight,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Website link (clickable → opens in browser)
            // Show globe/link icon + website URL
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val browserIntent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://sumitkumartiwari.netlify.app/")
                            )
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open website", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Website",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Website",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "sumitkumartiwari.netlify.app",
                        fontSize = 13.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Instagram link (clickable → opens Instagram app)
            // Show Instagram icon + @username
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val instagramUsername = "sumitkumartiwari19"
                        val instagramUrl = "https://instagram.com/$instagramUsername"
                        try {
                            val uri = android.net.Uri.parse("instagram://user?username=$instagramUsername")
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                            intent.setPackage("com.instagram.android")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(instagramUrl)
                                )
                            )
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Instagram",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Instagram",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "@sumitkumartiwari19",
                        fontSize = 13.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Facebook link (clickable)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val browserIntent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.facebook.com/share/1KrQFthNcg/")
                            )
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open Facebook", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Facebook",
                        tint = Color(0xFF1877F2),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Facebook",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Sumit Kumar Tiwari",
                        fontSize = 13.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // X (Twitter) link (clickable)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val browserIntent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://x.com/Sumit_Tiwari_19")
                            )
                            context.startActivity(browserIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open X", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "X",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "X (Twitter)",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "@Sumit_Tiwari_19",
                        fontSize = 13.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // BOTTOM SECTION:
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Version 1.0.0",
                fontSize = 12.sp,
                color = textMuted,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Made with ♥ by Sumit Kumar Tiwari",
                fontSize = 11.sp,
                color = textMuted,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}
