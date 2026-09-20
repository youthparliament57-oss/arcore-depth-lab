package com.example.depthlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.engine.ColorMaps
import com.example.depthlab.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PointCloudScreen(
    currentFrame: DepthFrame,
    yaw: Float,
    pitch: Float,
    zoom: Float,
    pointSize: Float,
    confidenceThreshold: Float,
    subsampleStep: Int,
    useDepthColormap: Boolean,
    onOrbitChanged: (Float, Float) -> Unit,
    onZoomChanged: (Float) -> Unit,
    onResetOrbit: () -> Unit,
    onSetConfidence: (Float) -> Unit,
    onToggleColormap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeVertexCount by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        // Point Cloud 3D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dYaw = dragAmount.x * 0.4f
                        val dPitch = -dragAmount.y * 0.4f
                        onOrbitChanged(dYaw, dPitch)
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW * 0.5f
            val centerY = canvasH * 0.5f

            val radYaw = (yaw * Math.PI / 180.0).toFloat()
            val radPitch = (pitch * Math.PI / 180.0).toFloat()

            val cosY = cos(radYaw)
            val sinY = sin(radYaw)
            val cosP = cos(radPitch)
            val sinP = sin(radPitch)

            var count = 0

            val step = subsampleStep.coerceIn(1, 4)

            for (y in 0 until currentFrame.height step step) {
                for (x in 0 until currentFrame.width step step) {
                    val conf = currentFrame.getConfidence(x, y)
                    if (conf < confidenceThreshold) continue

                    val depth = currentFrame.getDepth(x, y)
                    if (depth <= 0.1f || depth > 8.0f) continue

                    count++

                    // Reconstruct 3D camera-space point
                    val u = x.toFloat() / currentFrame.width
                    val v = y.toFloat() / currentFrame.height
                    val px = (x - currentFrame.principalX) * depth / currentFrame.focalLengthX
                    val py = -(y - currentFrame.principalY) * depth / currentFrame.focalLengthY
                    val pz = depth - 2.0f // center around 2m

                    // Apply orbital rotation (Yaw around Y, Pitch around X)
                    // 1. Yaw (Y-axis)
                    val x1 = px * cosY + pz * sinY
                    val z1 = -px * sinY + pz * cosY

                    // 2. Pitch (X-axis)
                    val y2 = py * cosP - z1 * sinP
                    val z2 = py * sinP + z1 * cosP + 2.0f // re-offset

                    if (z2 <= 0.2f) continue

                    // Perspective projection to canvas
                    val projScale = (canvasW * 0.8f * zoom) / z2
                    val sx = centerX + x1 * projScale
                    val sy = centerY - y2 * projScale

                    if (sx in 0f..canvasW && sy in 0f..canvasH) {
                        val pointColor = if (useDepthColormap) {
                            val normD = ((depth - 0.5f) / 4.5f).coerceIn(0f, 1f)
                            ColorMaps.turbo(normD)
                        } else {
                            // Camera RGB approximation
                            val luma = (0.25f + 0.65f * (1f - v)).coerceIn(0.2f, 1f)
                            Color(luma, luma * 0.95f, luma * 0.85f)
                        }

                        drawCircle(
                            color = pointColor,
                            radius = (pointSize * (1.5f / z2)).coerceIn(1.5f, 12f),
                            center = Offset(sx, sy)
                        )
                    }
                }
            }

            activeVertexCount = count
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
                        text = "ACTIVE POINTS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "$activeVertexCount",
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
                        text = "ORBIT YAW / PITCH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${yaw.toInt()}° / ${pitch.toInt()}°",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GlowAmber
                    )
                }

                Divider(
                    color = BorderCyanGlow,
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                )

                IconButton(
                    onClick = onResetOrbit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCardHigh)
                        .testTag("reset_orbit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Orbit",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Controls (Confidence Filter & Colormap Toggle)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
                .padding(horizontal = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightSurface.copy(alpha = 0.95f))
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Confidence Threshold: ${(confidenceThreshold * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Slider(
                        value = confidenceThreshold,
                        onValueChange = onSetConfidence,
                        valueRange = 0f..0.9f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan
                        ),
                        modifier = Modifier.testTag("confidence_slider")
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = onToggleColormap,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCardHigh)
                        .testTag("toggle_pointcloud_colormap_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Toggle Colormap",
                        tint = if (useDepthColormap) NeonCyan else GlowAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
