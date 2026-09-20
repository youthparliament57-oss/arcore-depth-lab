package com.example.depthlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depthlab.data.model.ColormapType
import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.data.model.InspectionData
import com.example.depthlab.engine.ColorMaps
import com.example.depthlab.ui.theme.*

@Composable
fun DepthMapScreen(
    currentFrame: DepthFrame,
    colormapType: ColormapType,
    cameraOpacity: Float,
    minDistance: Float,
    maxDistance: Float,
    inspectionData: InspectionData?,
    onColormapChanged: (ColormapType) -> Unit,
    onCameraOpacityChanged: (Float) -> Unit,
    onRangeChanged: (Float, Float) -> Unit,
    onInspectAt: (Float, Float, Float, Float) -> Unit,
    onClearInspection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Depth Map Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val u = offset.x / size.width.toFloat()
                        val v = offset.y / size.height.toFloat()
                        onInspectAt(offset.x, offset.y, u, v)
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // Render depth grid blocks
            val stepX = (canvasW / currentFrame.width).coerceAtLeast(1f)
            val stepY = (canvasH / currentFrame.height).coerceAtLeast(1f)

            // Step through frame
            val stride = 2
            val blockW = stepX * stride
            val blockH = stepY * stride

            val distRange = (maxDistance - minDistance).coerceAtLeast(0.1f)

            for (y in 0 until currentFrame.height step stride) {
                for (x in 0 until currentFrame.width step stride) {
                    val d = currentFrame.getDepth(x, y)
                    val normDepth = ((d - minDistance) / distRange).coerceIn(0f, 1f)

                    // False color
                    val depthColor = ColorMaps.mapDepthToColor(normDepth, colormapType)

                    // Simulated camera luminance (grayscale scene background)
                    val camLuma = (0.2f + 0.6f * (1f - (y.toFloat() / currentFrame.height)))
                    val camColor = Color(camLuma, camLuma * 0.95f, camLuma * 0.9f)

                    // Blend with camera opacity
                    val blendedColor = Color(
                        red = depthColor.red * (1f - cameraOpacity) + camColor.red * cameraOpacity,
                        green = depthColor.green * (1f - cameraOpacity) + camColor.green * cameraOpacity,
                        blue = depthColor.blue * (1f - cameraOpacity) + camColor.blue * cameraOpacity,
                        alpha = 1f
                    )

                    drawRect(
                        color = blendedColor,
                        topLeft = Offset(x * stepX, y * stepY),
                        size = Size(blockW, blockH)
                    )
                }
            }

            // Draw Inspection Probe if selected
            inspectionData?.let { data ->
                val probeCenter = Offset(data.screenX, data.screenY)

                // Outer animated ring
                drawCircle(
                    color = Color.White,
                    radius = 28f,
                    center = probeCenter,
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = NeonCyan,
                    radius = 16f,
                    center = probeCenter,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = probeCenter
                )

                // Crosshair ticks
                drawLine(
                    color = Color.White,
                    start = Offset(probeCenter.x - 36f, probeCenter.y),
                    end = Offset(probeCenter.x - 18f, probeCenter.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(probeCenter.x + 18f, probeCenter.y),
                    end = Offset(probeCenter.x + 36f, probeCenter.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(probeCenter.x, probeCenter.y - 36f),
                    end = Offset(probeCenter.x, probeCenter.y - 18f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White,
                    start = Offset(probeCenter.x, probeCenter.y + 18f),
                    end = Offset(probeCenter.x, probeCenter.y + 36f),
                    strokeWidth = 2f
                )
            }
        }

        // Inspection Data Card
        inspectionData?.let { data ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightSurface.copy(alpha = 0.95f))
                    .border(1.dp, NeonCyan, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = "PROBE DEPTH",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = String.format("%.3f m", data.depthMeters),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = String.format("%d mm", (data.depthMeters * 1000).toInt()),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Divider(
                        color = BorderCyanGlow,
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                    )

                    Column {
                        Text(
                            text = "CONFIDENCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = String.format("%.0f%%", data.confidence * 100f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SciFiGreen
                        )
                        Text(
                            text = String.format("Tilt: %.1f°", data.inclinationDeg),
                            fontSize = 11.sp,
                            color = GlowAmber
                        )
                    }

                    IconButton(
                        onClick = onClearInspection,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dismiss_inspection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close probe",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Colormap Bar & Controls (Bottom HUD)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Expandable settings drawer
            if (showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MidnightSurface.copy(alpha = 0.95f))
                        .border(1.dp, BorderCyanGlow, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Camera / Depth Blend",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Slider(
                        value = cameraOpacity,
                        onValueChange = onCameraOpacityChanged,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("camera_blend_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Distance Range: ${String.format("%.1f", minDistance)}m - ${String.format("%.1f", maxDistance)}m",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Slider(
                        value = maxDistance,
                        onValueChange = { newMax -> onRangeChanged(minDistance, newMax) },
                        valueRange = 2f..10f,
                        colors = SliderDefaults.colors(
                            thumbColor = GlowAmber,
                            activeTrackColor = GlowAmber
                        ),
                        modifier = Modifier.testTag("max_distance_slider")
                    )
                }
            }

            // Colormap pills and settings toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightSurface.copy(alpha = 0.95f))
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColormapType.values().forEach { cmap ->
                        val isSelected = cmap == colormapType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SurfaceCardHigh else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onColormapChanged(cmap) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("colormap_${cmap.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (cmap) {
                                    ColormapType.TURBO -> "Turbo"
                                    ColormapType.JET -> "Jet"
                                    ColormapType.VIRIDIS -> "Viridis"
                                    ColormapType.GRAYSCALE -> "Gray"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (showSettings) SurfaceCardHigh else Color.Transparent)
                        .testTag("depth_settings_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Depth settings",
                        tint = if (showSettings) NeonCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
