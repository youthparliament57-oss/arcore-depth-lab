package com.example.depthlab.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depthlab.data.model.DepthSceneType
import com.example.depthlab.ui.theme.*

@Composable
fun DepthLabBottomBar(
    currentScene: DepthSceneType,
    onSceneSelected: (DepthSceneType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightSurface.copy(alpha = 0.94f))
            .border(1.dp, BorderCyanGlow, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DepthSceneType.values().forEach { scene ->
                val isSelected = scene == currentScene
                val icon = getSceneIcon(scene)

                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) SurfaceCardHigh else Color.Transparent,
                    label = "bgColor"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else Color.Transparent,
                    label = "borderColor"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else TextSecondary,
                    label = "contentColor"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onSceneSelected(scene) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("nav_scene_${scene.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = scene.title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = scene.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

private fun getSceneIcon(scene: DepthSceneType): ImageVector {
    return when (scene) {
        DepthSceneType.ORIENTED_RETICLE -> Icons.Default.Adjust
        DepthSceneType.DEPTH_MAP -> Icons.Default.Gradient
        DepthSceneType.COLLIDER -> Icons.Default.SportsBaseball
        DepthSceneType.AVATAR_LOCOMOTION -> Icons.Default.SmartToy
        DepthSceneType.POINT_CLOUD -> Icons.Default.Grain
        DepthSceneType.FOG_EFFECT -> Icons.Default.Cloud
        DepthSceneType.DEPTH_OCCLUSION -> Icons.Default.Layers
    }
}
