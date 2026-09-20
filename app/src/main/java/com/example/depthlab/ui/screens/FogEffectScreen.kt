package com.example.depthlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.ui.theme.*

val FOG_COLORS = listOf(
    Pair("Neon Cyan", Color(0xFF00E5FF)),
    Pair("Mystic Violet", Color(0xFF9D4EDD)),
    Pair("Sci-Fi Green", Color(0xFF05FFA1)),
    Pair("Amber Mist", Color(0xFFFFB800)),
    Pair("White Smoke", Color(0xFFE2E8F0))
)

@Composable
fun FogEffectScreen(
    currentFrame: DepthFrame,
    fogDistance: Float,
    fogThickness: Float,
    fogColorIndex: Int,
    onFogParamsChanged: (Float, Float, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedColor = FOG_COLORS[fogColorIndex.coerceIn(0, FOG_COLORS.lastIndex)].second

    Box(modifier = modifier.fillMaxSize()) {
        // Fog Simulation Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            val stepX = (canvasW / currentFrame.width).coerceAtLeast(1f)
            val stepY = (canvasH / currentFrame.height).coerceAtLeast(1f)

            val stride = 2
            val blockW = stepX * stride
            val blockH = stepY * stride

            for (y in 0 until currentFrame.height step stride) {
                for (x in 0 until currentFrame.width step stride) {
                    val depth = currentFrame.getDepth(x, y)

                    // Base camera scene color (grayscale room)
                    val ny = y.toFloat() / currentFrame.height
                    val baseLuma = (0.2f + 0.6f * (1f - ny))
                    val baseColor = Color(baseLuma, baseLuma * 0.96f, baseLuma * 0.92f)

                    // Exponential depth fog factor
                    val fogMin = fogDistance * 0.5f
                    val fogMax = fogDistance * 2.0f
                    val fogFactor = (((depth - fogMin) / (fogMax - fogMin)).coerceIn(0f, 1f) * fogThickness).coerceIn(0f, 0.95f)

                    // Blend base color with fog color
                    val finalColor = Color(
                        red = baseColor.red * (1f - fogFactor) + selectedColor.red * fogFactor,
                        green = baseColor.green * (1f - fogFactor) + selectedColor.green * fogFactor,
                        blue = baseColor.blue * (1f - fogFactor) + selectedColor.blue * fogFactor,
                        alpha = 1f
                    )

                    drawRect(
                        color = finalColor,
                        topLeft = Offset(x * stepX, y * stepY),
                        size = Size(blockW, blockH)
                    )
                }
            }
        }

        // Top HUD metrics
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MidnightSurface.copy(alpha = 0.9f))
                .border(1.dp, BorderCyanGlow, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FOG DISTANCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.1f m", fogDistance),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Divider(
                    color = BorderCyanGlow,
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FOG DENSITY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.0f%%", fogThickness * 100f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowAmber
                    )
                }
            }
        }

        // Bottom Controls (Color picker & sliders)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
                .padding(horizontal = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sliders card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightSurface.copy(alpha = 0.95f))
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Fog Distance (${String.format("%.1f", fogDistance)}m)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Slider(
                    value = fogDistance,
                    onValueChange = { onFogParamsChanged(it, fogThickness, fogColorIndex) },
                    valueRange = 0.5f..5.0f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                    modifier = Modifier.testTag("fog_distance_slider")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Fog Density (${(fogThickness * 100).toInt()}%)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Slider(
                    value = fogThickness,
                    onValueChange = { onFogParamsChanged(fogDistance, it, fogColorIndex) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = GlowAmber, activeTrackColor = GlowAmber),
                    modifier = Modifier.testTag("fog_density_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FOG_COLORS.forEachIndexed { idx, pair ->
                        val isSelected = idx == fogColorIndex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(pair.second)
                                .border(
                                    2.dp,
                                    if (isSelected) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { onFogParamsChanged(fogDistance, fogThickness, idx) }
                                .testTag("fog_color_$idx")
                        )
                    }
                }
            }
        }
    }
}
