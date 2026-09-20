package com.example.depthlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.depthlab.data.model.Projectile
import com.example.depthlab.data.model.ProjectileShape
import com.example.depthlab.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ColliderScreen(
    currentFrame: DepthFrame,
    projectiles: List<Projectile>,
    selectedShape: ProjectileShape,
    thrust: Float,
    renderWireframe: Boolean,
    onShootProjectile: () -> Unit,
    onClearProjectiles: () -> Unit,
    onSelectShape: (ProjectileShape) -> Unit,
    onSetThrust: (Float) -> Unit,
    onToggleWireframe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Physics Simulation Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Wireframe Depth Mesh if enabled
            if (renderWireframe) {
                val gridStep = 8
                val scaleX = canvasW / currentFrame.width
                val scaleY = canvasH / currentFrame.height

                for (y in 0 until currentFrame.height - gridStep step gridStep) {
                    for (x in 0 until currentFrame.width - gridStep step gridStep) {
                        val d00 = currentFrame.getDepth(x, y)
                        val d10 = currentFrame.getDepth(x + gridStep, y)
                        val d01 = currentFrame.getDepth(x, y + gridStep)

                        // 2D projection
                        val p00 = Offset(x * scaleX, y * scaleY)
                        val p10 = Offset((x + gridStep) * scaleX, y * scaleY)
                        val p01 = Offset(x * scaleX, (y + gridStep) * scaleY)

                        val alpha = (1f - (d00 / 5f)).coerceIn(0.1f, 0.45f)
                        val wireColor = NeonCyan.copy(alpha = alpha)

                        drawLine(wireColor, p00, p10, strokeWidth = 1f)
                        drawLine(wireColor, p00, p01, strokeWidth = 1f)
                        drawLine(wireColor, p10, p01, strokeWidth = 0.8f)
                    }
                }
            }

            // 2. Draw Simulated Projectiles with 3D perspective projection and shadows
            projectiles.forEach { p ->
                val z = p.position.z.coerceAtLeast(0.1f)
                val projX = canvasW * 0.5f + (p.position.x / (z * 0.75f)) * canvasW * 0.5f
                val projY = canvasH * 0.5f - (p.position.y / (z * 0.75f)) * canvasH * 0.5f

                // Projected radius based on depth distance
                val projectedRadius = ((p.radius / z) * canvasW * 0.8f).coerceIn(6f, 60f)

                // Projected shadow on ground / surface
                val shadowY = projY + projectedRadius * 1.5f
                val shadowAlpha = if (p.isSleeping) 0.5f else 0.3f
                drawOval(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(projX - projectedRadius * 1.2f, shadowY),
                    size = Size(projectedRadius * 2.4f, projectedRadius * 0.6f)
                )

                // Render Projectile according to shape
                val center = Offset(projX, projY)
                when (p.shape) {
                    ProjectileShape.SPHERE -> {
                        // Sphere gradient highlights
                        drawCircle(
                            color = p.color,
                            radius = projectedRadius,
                            center = center
                        )
                        // Specular shine
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f),
                            radius = projectedRadius * 0.35f,
                            center = Offset(center.x - projectedRadius * 0.3f, center.y - projectedRadius * 0.3f)
                        )
                        // Sphere outline
                        drawCircle(
                            color = Color.White.copy(alpha = 0.5f),
                            radius = projectedRadius,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }
                    ProjectileShape.CUBE -> {
                        // Rotated box
                        val halfSize = projectedRadius
                        drawRect(
                            color = p.color,
                            topLeft = Offset(center.x - halfSize, center.y - halfSize),
                            size = Size(halfSize * 2f, halfSize * 2f)
                        )
                        drawRect(
                            color = Color.White.copy(alpha = 0.8f),
                            topLeft = Offset(center.x - halfSize, center.y - halfSize),
                            size = Size(halfSize * 2f, halfSize * 2f),
                            style = Stroke(width = 2f)
                        )
                    }
                    ProjectileShape.CAPSULE -> {
                        // Pill / Capsule shape
                        val w = projectedRadius * 1.4f
                        val h = projectedRadius * 2.2f
                        drawRoundRect(
                            color = p.color,
                            topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f),
                            size = Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.5f, w * 0.5f)
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.8f),
                            topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f),
                            size = Size(w, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.5f, w * 0.5f),
                            style = Stroke(width = 2f)
                        )
                    }
                }

                // Sleeping/Anchored indicator
                if (p.isAnchored) {
                    drawCircle(
                        color = GlowAmber,
                        radius = 3f,
                        center = center
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
                        text = "PROJECTILES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${projectiles.size}",
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
                        text = "RESTITUTION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "0.65",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlowAmber
                    )
                }

                Divider(
                    color = BorderCyanGlow,
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                )

                // Wireframe toggle
                IconButton(
                    onClick = onToggleWireframe,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (renderWireframe) SurfaceCardHigh else Color.Transparent)
                        .testTag("toggle_wireframe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Toggle Wireframe",
                        tint = if (renderWireframe) NeonCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Shape selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MidnightSurface.copy(alpha = 0.95f))
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectileShape.values().forEach { shape ->
                        val isSelected = shape == selectedShape
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SurfaceCardHigh else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectShape(shape) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("shape_${shape.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (shape) {
                                    ProjectileShape.SPHERE -> "Sphere"
                                    ProjectileShape.CUBE -> "Cube"
                                    ProjectileShape.CAPSULE -> "Capsule"
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) NeonCyan else TextSecondary
                            )
                        }
                    }
                }

                Text(
                    text = "Thrust: ${String.format("%.1f", thrust)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Shoot Projectile button & Clear button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onShootProjectile,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shoot_projectile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = "Throw Object",
                        tint = DeepNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Throw Projectile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DeepNavy
                    )
                }

                if (projectiles.isNotEmpty()) {
                    IconButton(
                        onClick = onClearProjectiles,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceCard)
                            .border(1.dp, BorderCyanGlow, RoundedCornerShape(12.dp))
                            .testTag("clear_projectiles_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
