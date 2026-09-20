package com.example.depthlab.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.depthlab.data.model.AvatarAction
import com.example.depthlab.data.model.AvatarState
import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AvatarLocomotionScreen(
    currentFrame: DepthFrame,
    avatarState: AvatarState,
    onMoveAvatarToTap: (Float, Float) -> Unit,
    onTriggerAction: (AvatarAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Main World Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val u = offset.x / size.width.toFloat()
                        val v = offset.y / size.height.toFloat()
                        onMoveAvatarToTap(u, v)
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Destination Target Marker if walking
            if (avatarState.currentAction == AvatarAction.WALK) {
                val zTarget = avatarState.targetPosition.z.coerceAtLeast(0.1f)
                val targetScreenX = canvasW * 0.5f + (avatarState.targetPosition.x / (zTarget * 0.75f)) * canvasW * 0.5f
                val targetScreenY = canvasH * 0.5f - (avatarState.targetPosition.y / (zTarget * 0.75f)) * canvasH * 0.5f

                // Pulsing target ring
                val pulse = (avatarState.actionTimer * 4f) % 1f
                drawCircle(
                    color = NeonCyan.copy(alpha = 1f - pulse),
                    radius = 20f + pulse * 25f,
                    center = Offset(targetScreenX, targetScreenY),
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = NeonCyan,
                    radius = 8f,
                    center = Offset(targetScreenX, targetScreenY)
                )
            }

            // 2. Project Robochef Avatar in 3D Perspective
            val z = avatarState.position.z.coerceAtLeast(0.2f)
            val screenX = canvasW * 0.5f + (avatarState.position.x / (z * 0.75f)) * canvasW * 0.5f
            val screenY = canvasH * 0.5f - (avatarState.position.y / (z * 0.75f)) * canvasH * 0.5f

            val avatarScale = ((0.35f / z) * canvasW * 0.7f).coerceIn(24f, 130f)

            // Ground Shadow
            val shadowY = screenY + avatarScale * 0.9f
            drawOval(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(screenX - avatarScale * 0.7f, shadowY),
                size = Size(avatarScale * 1.4f, avatarScale * 0.35f)
            )

            // Jet Thruster Rings below Robochef
            val thrusterY = screenY + avatarScale * 0.7f
            val thrusterPulse = (avatarState.actionTimer * 8f) % 1f
            drawOval(
                color = NeonCyan.copy(alpha = 0.6f - thrusterPulse * 0.4f),
                topLeft = Offset(screenX - avatarScale * 0.4f, thrusterY),
                size = Size(avatarScale * 0.8f, avatarScale * 0.25f),
                style = Stroke(width = 2f)
            )

            // Render Robochef (Head, Glass Visor, Chef Hat, Torso, Floating Hands)
            val chefCenter = Offset(screenX, screenY)

            // Torso (Metallic Cyan Sphere/Oval)
            drawRoundRect(
                color = SurfaceCardHigh,
                topLeft = Offset(chefCenter.x - avatarScale * 0.35f, chefCenter.y - avatarScale * 0.2f),
                size = Size(avatarScale * 0.7f, avatarScale * 0.8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(avatarScale * 0.3f, avatarScale * 0.3f)
            )
            // Robochef Core glow
            val coreColor = when (avatarState.currentAction) {
                AvatarAction.FRENZY -> AccentPink
                AvatarAction.ATTACK, AvatarAction.FAST_ATTACK -> GlowAmber
                else -> NeonCyan
            }
            drawCircle(
                color = coreColor,
                radius = avatarScale * 0.15f,
                center = Offset(chefCenter.x, chefCenter.y + avatarScale * 0.15f)
            )

            // Head (Sphere with glass visor)
            val headCenter = Offset(chefCenter.x, chefCenter.y - avatarScale * 0.35f)
            drawCircle(
                color = MidnightSurface,
                radius = avatarScale * 0.32f,
                center = headCenter
            )
            // Glass Visor
            drawRoundRect(
                color = NeonCyan.copy(alpha = 0.85f),
                topLeft = Offset(headCenter.x - avatarScale * 0.22f, headCenter.y - avatarScale * 0.12f),
                size = Size(avatarScale * 0.44f, avatarScale * 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(avatarScale * 0.1f, avatarScale * 0.1f)
            )

            // Chef Hat (Toque Blanche) on Top!
            val hatBottom = headCenter.y - avatarScale * 0.25f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(headCenter.x - avatarScale * 0.25f, hatBottom - avatarScale * 0.35f),
                size = Size(avatarScale * 0.5f, avatarScale * 0.38f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(avatarScale * 0.15f, avatarScale * 0.15f)
            )
            // Chef hat pleats
            drawRoundRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset(headCenter.x - avatarScale * 0.28f, hatBottom - avatarScale * 0.08f),
                size = Size(avatarScale * 0.56f, avatarScale * 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Floating Floating Hands / Spatula
            val actionOffset = sin(avatarState.actionTimer * 12f) * (avatarScale * 0.2f)
            val leftHand = Offset(chefCenter.x - avatarScale * 0.55f, chefCenter.y + (if (avatarState.currentAction == AvatarAction.ATTACK) -actionOffset else 0f))
            val rightHand = Offset(chefCenter.x + avatarScale * 0.55f, chefCenter.y + (if (avatarState.currentAction == AvatarAction.FAST_ATTACK) -actionOffset else 0f))

            drawCircle(color = SurfaceCardHigh, radius = avatarScale * 0.12f, center = leftHand)
            drawCircle(color = SurfaceCardHigh, radius = avatarScale * 0.12f, center = rightHand)
            drawCircle(color = NeonCyan, radius = avatarScale * 0.06f, center = leftHand)
            drawCircle(color = NeonCyan, radius = avatarScale * 0.06f, center = rightHand)
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
                        text = "ELEVATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.2f m", avatarState.position.y),
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
                        text = "ACTION STATE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = avatarState.currentAction.name,
                        fontSize = 14.sp,
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DEPTH (Z)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format("%.2f m", avatarState.position.z),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }

        // Bottom Action Triggers
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 86.dp)
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                label = "Jump",
                icon = Icons.Default.North,
                onClick = { onTriggerAction(AvatarAction.JUMP) },
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                label = "Attack",
                icon = Icons.Default.SportsMartialArts,
                onClick = { onTriggerAction(AvatarAction.ATTACK) },
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                label = "Frenzy",
                icon = Icons.Default.Bolt,
                onClick = { onTriggerAction(AvatarAction.FRENZY) },
                modifier = Modifier.weight(1f),
                tint = AccentPink
            )
            ActionButton(
                label = "Warp",
                icon = Icons.Default.AllInclusive,
                onClick = { onTriggerAction(AvatarAction.TELEPORT) },
                modifier = Modifier.weight(1f),
                tint = GlowAmber
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NeonCyan
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard.copy(alpha = 0.95f))
            .border(1.dp, BorderCyanGlow, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp)
            .testTag("action_${label.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}
