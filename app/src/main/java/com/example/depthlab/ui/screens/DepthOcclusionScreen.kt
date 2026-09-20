package com.example.depthlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.ui.theme.*

@Composable
fun DepthOcclusionScreen(
    currentFrame: DepthFrame,
    occlusionEnabled: Boolean,
    showShadowReceiver: Boolean,
    virtualObjectDepth: Float,
    onToggleOcclusion: () -> Unit,
    onToggleShadowReceiver: () -> Unit,
    onSetObjectDepth: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var occludedPercentage by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        // Main Occlusion Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Background Camera Room with Depth Blocks
            val stepX = (canvasW / currentFrame.width).coerceAtLeast(1f)
            val stepY = (canvasH / currentFrame.height).coerceAtLeast(1f)
            val stride = 3

            for (y in 0 until currentFrame.height step stride) {
                for (x in 0 until currentFrame.width step stride) {
                    val d = currentFrame.getDepth(x, y)
                    val ny = y.toFloat() / currentFrame.height
                    val luma = (0.25f + 0.6f * (1f - ny))
                    val roomColor = Color(luma, luma * 0.95f, luma * 0.9f)

                    drawRect(
                        color = roomColor,
                        topLeft = Offset(x * stepX, y * stepY),
                        size = Size(stepX * stride, stepY * stride)
                    )
                }
            }

            // 2. Virtual Object Center in screen coordinates
            val objScreenX = canvasW * 0.5f
            val objScreenY = canvasH * 0.55f
            val objRadius = ((0.3f / virtualObjectDepth) * canvasW * 0.6f).coerceIn(25f, 160f)

            // 3. Contact Shadow Receiver
            if (showShadowReceiver) {
                val shadowY = objScreenY + objRadius * 0.9f
                drawOval(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(objScreenX - objRadius * 1.1f, shadowY),
                    size = Size(objRadius * 2.2f, objRadius * 0.5f)
                )
            }

            // 4. Calculate Occlusion against Real-World Depth Buffer
            val objLeft = (objScreenX - objRadius).coerceIn(0f, canvasW)
            val objRight = (objScreenX + objRadius).coerceIn(0f, canvasW)
            val objTop = (objScreenY - objRadius).coerceIn(0f, canvasH)
            val objBottom = (objScreenY + objRadius).coerceIn(0f, canvasH)

            var totalSamples = 0
            var occludedSamples = 0

            // If occlusion is enabled, we render in depth-aware slices
            if (occlusionEnabled) {
                // Draw virtual object base
                drawRoundRect(
                    color = NeonCyan.copy(alpha = 0.95f),
                    topLeft = Offset(objScreenX - objRadius, objScreenY - objRadius),
                    size = Size(objRadius * 2f, objRadius * 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                // Virtual object glowing frame
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(objScreenX - objRadius, objScreenY - objRadius),
                    size = Size(objRadius * 2f, objRadius * 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                    style = Stroke(width = 3f)
                )

                // Now overdraw the foreground occluding physical pixels!
                val sampleStep = 4
                for (sy in objTop.toInt()..objBottom.toInt() step sampleStep) {
                    val py = (sy / canvasH * currentFrame.height).toInt().coerceIn(0, currentFrame.height - 1)
                    for (sx in objLeft.toInt()..objRight.toInt() step sampleStep) {
                        val px = (sx / canvasW * currentFrame.width).toInt().coerceIn(0, currentFrame.width - 1)
                        val physicalDepth = currentFrame.getDepth(px, py)

                        totalSamples++

                        // If real physical object is CLOSER to camera than virtual object:
                        if (physicalDepth < virtualObjectDepth - 0.05f) {
                            occludedSamples++
                            // Overdraw with physical scene pixel!
                            val ny = py.toFloat() / currentFrame.height
                            val luma = (0.25f + 0.6f * (1f - ny))
                            val physicalColor = Color(luma, luma * 0.95f, luma * 0.9f)

                            drawRect(
                                color = physicalColor,
                                topLeft = Offset(sx.toFloat(), sy.toFloat()),
                                size = Size(sampleStep.toFloat(), sampleStep.toFloat())
                            )
                        }
                    }
                }
            } else {
                // NO OCCLUSION: Renders unnaturally on top of everything
                drawRoundRect(
                    color = NeonCyan.copy(alpha = 0.95f),
                    topLeft = Offset(objScreenX - objRadius, objScreenY - objRadius),
                    size = Size(objRadius * 2f, objRadius * 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(objScreenX - objRadius, objScreenY - objRadius),
                    size = Size(objRadius * 2f, objRadius * 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                    style = Stroke(width = 3f)
                )
            }

            if (totalSamples > 0) {
                occludedPercentage = (occludedSamples * 100 / totalSamples).coerceIn(0, 100)
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
                        text = "VIRTUAL DEPTH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.2f m", virtualObjectDepth),
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
                        text = "OCCLUDED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = if (occlusionEnabled) "$occludedPercentage%" else "OFF",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (occlusionEnabled && occludedPercentage > 0) GlowAmber else SciFiGreen
                    )
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
                .padding(horizontal = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightSurface.copy(alpha = 0.95f))
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Virtual Object Distance: ${String.format("%.2f", virtualObjectDepth)}m",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Slider(
                    value = virtualObjectDepth,
                    onValueChange = onSetObjectDepth,
                    valueRange = 0.6f..3.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                    modifier = Modifier.testTag("virtual_object_depth_slider")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleOcclusion,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (occlusionEnabled) SciFiGreen else SurfaceCardHigh
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_occlusion_button")
                    ) {
                        Icon(
                            imageVector = if (occlusionEnabled) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = "Toggle Occlusion",
                            tint = if (occlusionEnabled) DeepNavy else TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (occlusionEnabled) "Occlusion ON" else "Occlusion OFF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (occlusionEnabled) DeepNavy else TextPrimary
                        )
                    }

                    Button(
                        onClick = onToggleShadowReceiver,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showShadowReceiver) NeonCyan else SurfaceCardHigh
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_shadow_receiver_button")
                    ) {
                        Text(
                            text = if (showShadowReceiver) "Shadow ON" else "Shadow OFF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showShadowReceiver) DeepNavy else TextPrimary
                        )
                    }
                }
            }
        }
    }
}
