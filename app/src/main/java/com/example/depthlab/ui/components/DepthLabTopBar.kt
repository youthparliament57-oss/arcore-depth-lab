package com.example.depthlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depthlab.data.model.DepthSceneType
import com.example.depthlab.engine.DepthEngine
import com.example.depthlab.ui.theme.*

@Composable
fun DepthLabTopBar(
    currentScene: DepthSceneType,
    fps: Int,
    isRawDepth: Boolean,
    envIndex: Int,
    onToggleRawDepth: () -> Unit,
    onCycleEnvironment: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentEnv = DepthEngine.ENVIRONMENTS[envIndex]

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and active scene
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DEPTH LAB",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceCardHigh)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$fps FPS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SciFiGreen
                    )
                }
            }
            Text(
                text = currentScene.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // Action controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Environment switcher chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(8.dp))
                    .clickable { onCycleEnvironment() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("cycle_environment_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch Environment",
                        tint = ElectricBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentEnv.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            // Raw Depth vs Smooth Depth Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isRawDepth) AccentPink.copy(alpha = 0.2f) else SurfaceCard)
                    .border(
                        1.dp,
                        if (isRawDepth) AccentPink else BorderCyanGlow,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleRawDepth() }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("toggle_raw_depth_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRawDepth) Icons.Default.Sensors else Icons.Default.SensorsOff,
                        contentDescription = "Toggle Raw Depth",
                        tint = if (isRawDepth) AccentPink else NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRawDepth) "Raw Depth" else "Smooth Depth",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRawDepth) AccentPink else NeonCyan
                    )
                }
            }

            // Info Dialog Button
            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceCard)
                    .testTag("info_help_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Help & Information",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
