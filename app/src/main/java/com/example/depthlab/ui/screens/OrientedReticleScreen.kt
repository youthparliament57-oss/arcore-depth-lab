package com.example.depthlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.data.model.PaintSplat
import com.example.depthlab.data.model.Vector3D
import com.example.depthlab.engine.ColorMaps
import com.example.depthlab.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrientedReticleScreen(
    currentFrame: DepthFrame,
    reticlePos: Pair<Float, Float>,
    distanceM: Float,
    normal: Vector3D,
    angleDeg: Float,
    paintSplats: List<PaintSplat>,
    onSetReticlePos: (Float, Float) -> Unit,
    onShootPaint: () -> Unit,
    onClearSplats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Main AR Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val u = offset.x / size.width.toFloat()
                        val v = offset.y / size.height.toFloat()
                        onSetReticlePos(u, v)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (change.position.x) / size.width.toFloat()
                        val newY = (change.position.y) / size.height.toFloat()
                        onSetReticlePos(newX, newY)
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw depth contour lines background (simulating AR environment)
            for (step in 1..8) {
                val depthLevel = step * 0.7f
                val path = Path()
                var started = false
                for (x in 0..canvasW.toInt() step 40) {
                    val nx = x.toFloat() / canvasW
                    val ny = 0.3f + (step * 0.08f) + sin(nx * 6f + step) * 0.04f
                    val y = ny * canvasH
                    if (!started) {
                        path.moveTo(x.toFloat(), y)
                        started = true
                    } else {
                        path.lineTo(x.toFloat(), y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0x1800E5FF),
                    style = Stroke(width = 1.5f)
                )
            }

            // 2. Render Paint Splats on surfaces
            paintSplats.forEach { splat ->
                // Map world position roughly to screen
                val u = 0.5f + (splat.worldPos.x / (splat.worldPos.z.coerceAtLeast(0.1f) * 0.8f))
                val v = 0.5f - (splat.worldPos.y / (splat.worldPos.z.coerceAtLeast(0.1f) * 0.8f))
                val center = Offset(u * canvasW, v * canvasH)

                // Splat main blob
                drawCircle(
                    color = splat.color.copy(alpha = 0.85f),
                    radius = splat.radius * canvasW * 0.4f,
                    center = center
                )
                // Splat outline
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = splat.radius * canvasW * 0.4f,
                    center = center,
                    style = Stroke(width = 2f)
                )
                // Splat droplets
                splat.droplets.forEach { droplet ->
                    val dropCenter = Offset(
                        center.x + droplet.x * canvasW * 2f,
                        center.y + droplet.y * canvasH * 2f
                    )
                    drawCircle(
                        color = splat.color.copy(alpha = 0.7f),
                        radius = 4f,
                        center = dropCenter
                    )
                }
            }

            // 3. Render Oriented 3D Reticle
            val reticleX = reticlePos.first * canvasW
            val reticleY = reticlePos.second * canvasH
            val reticleCenter = Offset(reticleX, reticleY)

            // Outer ring
            drawCircle(
                color = NeonCyan.copy(alpha = 0.4f),
                radius = 44f,
                center = reticleCenter,
                style = Stroke(width = 2.5f)
            )

            // Inner crosshair / ring
            drawCircle(
                color = NeonCyan,
                radius = 18f,
                center = reticleCenter,
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = reticleCenter
            )

            // 4. Normal Vector Indicator (3D Arrow / Cone pointing away along normal)
            // Normal projected on 2D screen
            val normalLength = 70f
            val normalEndX = reticleX + normal.x * normalLength
            val normalEndY = reticleY - normal.y * normalLength // negative because screen Y is downward

            // Shadow of normal vector
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = reticleCenter + Offset(2f, 2f),
                end = Offset(normalEndX + 2f, normalEndY + 2f),
                strokeWidth = 4f
            )
            // Main normal vector line
            drawLine(
                color = GlowAmber,
                start = reticleCenter,
                end = Offset(normalEndX, normalEndY),
                strokeWidth = 3.5f
            )
            // Arrow head
            drawCircle(
                color = GlowAmber,
                radius = 6f,
                center = Offset(normalEndX, normalEndY)
            )
        }

        // Top HUD metrics overlay card
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
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DISTANCE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.2f m", distanceM),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Divider(
                    color = BorderCyanGlow,
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "INCLINATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.1f°", angleDeg),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowAmber
                    )
                }

                Divider(
                    color = BorderCyanGlow,
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NORMAL [X,Y,Z]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.2f, %.2f, %.2f", normal.x, normal.y, normal.z),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }

        // Bottom Controls (Action buttons for Paint Splat and Clear)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onShootPaint,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("shoot_paint_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Colorize,
                    contentDescription = "Shoot Paint",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Paint Splat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            if (paintSplats.isNotEmpty()) {
                IconButton(
                    onClick = onClearSplats,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderCyanGlow, RoundedCornerShape(12.dp))
                        .testTag("clear_paint_splats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Splats",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}
