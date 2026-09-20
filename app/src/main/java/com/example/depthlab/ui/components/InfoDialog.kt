package com.example.depthlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.depthlab.ui.theme.*

@Composable
fun InfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MidnightSurface)
                .border(1.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ARCore Depth Lab",
                            style = Typography.titleLarge,
                            color = NeonCyan
                        )
                        Text(
                            text = "Depth API Interactive Experiments",
                            style = Typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_info_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SectionCard(
                    title = "1. Localized Depth (CPU)",
                    description = "Samples depth values at specific screen UV coordinates. Calculates local gradients using weighted mean estimators to determine exact 3D surface normal vectors, driving oriented reticles and avatar terrain snapping."
                )

                Spacer(modifier = Modifier.height(10.dp))

                SectionCard(
                    title = "2. Dense Depth (GPU)",
                    description = "Processes per-pixel depth to render false-color colormaps (Google's Turbo Colormap, Jet, Viridis), atmospheric distance fog, and realistic occlusion of virtual objects behind real-world physical boundaries."
                )

                Spacer(modifier = Modifier.height(10.dp))

                SectionCard(
                    title = "3. Surface Depth (Mesh & Point Cloud)",
                    description = "Constructs 3D polygonal vertex meshes and point clouds from depth arrays, enabling real-time physics collisions for bouncing projectiles, contact shadows, and 3D environment scanning."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_info_dialog_button")
                ) {
                    Text(
                        text = "Got It",
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = TextSecondary
        )
    }
}
