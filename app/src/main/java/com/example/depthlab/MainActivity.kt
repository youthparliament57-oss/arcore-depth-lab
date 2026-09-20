package com.example.depthlab

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.depthlab.ar.RealArCoreManager
import com.example.depthlab.data.model.ArSourceMode
import com.example.depthlab.data.model.DepthSceneType
import com.example.depthlab.ui.components.ArCameraSurfaceView
import com.example.depthlab.ui.components.DepthLabBottomBar
import com.example.depthlab.ui.components.DepthLabTopBar
import com.example.depthlab.ui.components.InfoDialog
import com.example.depthlab.ui.screens.*
import com.example.depthlab.ui.theme.DeepNavy
import com.example.depthlab.ui.theme.DepthLabTheme
import com.example.depthlab.ui.theme.SciFiGreen
import com.example.depthlab.viewmodel.DepthLabViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DepthLabTheme {
                DepthLabApp()
            }
        }
    }
}

@Composable
fun DepthLabApp(
    viewModel: DepthLabViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val activity = context as? Activity
    val arManager = remember {
        RealArCoreManager(context).also {
            viewModel.realArCoreManager = it
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && activity != null) {
            arManager.checkArCoreAvailability()
            arManager.startSession(activity)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            arManager.destroy()
        }
    }

    val arSourceMode by viewModel.arSourceMode.collectAsState()
    val arStatusMessage by viewModel.arStatusMessage.collectAsState()
    val currentScene by viewModel.currentScene.collectAsState()
    val currentFrame by viewModel.currentFrame.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val isRawDepth by viewModel.isRawDepth.collectAsState()
    val envIndex by viewModel.currentEnvIndex.collectAsState()
    val showInfoDialog by viewModel.showInfoDialog.collectAsState()

    // Reticle state
    val reticlePos by viewModel.reticleScreenPos.collectAsState()
    val reticleDistance by viewModel.reticleDistance.collectAsState()
    val reticleNormal by viewModel.reticleNormal.collectAsState()
    val reticleAngle by viewModel.reticleAngle.collectAsState()
    val paintSplats by viewModel.paintSplats.collectAsState()

    // Depth Map state
    val colormapType by viewModel.colormapType.collectAsState()
    val cameraOpacity by viewModel.cameraViewOpacity.collectAsState()
    val minDistance by viewModel.minVisualizationDist.collectAsState()
    val maxDistance by viewModel.maxVisualizationDist.collectAsState()
    val inspectionData by viewModel.inspectionData.collectAsState()

    // Collider state
    val projectiles by viewModel.projectiles.collectAsState()
    val selectedShape by viewModel.selectedProjectileShape.collectAsState()
    val thrust by viewModel.projectileThrust.collectAsState()
    val renderWireframe by viewModel.renderWireframe.collectAsState()

    // Avatar state
    val avatarState by viewModel.avatarState.collectAsState()

    // Point cloud state
    val orbitYaw by viewModel.orbitYaw.collectAsState()
    val orbitPitch by viewModel.orbitPitch.collectAsState()
    val orbitZoom by viewModel.orbitZoom.collectAsState()
    val pointSize by viewModel.pointSize.collectAsState()
    val confidenceThreshold by viewModel.confidenceThreshold.collectAsState()
    val subsampleStep by viewModel.subsampleStep.collectAsState()
    val useDepthColormap by viewModel.useDepthColormap.collectAsState()

    // Fog state
    val fogDistance by viewModel.fogDistance.collectAsState()
    val fogThickness by viewModel.fogThickness.collectAsState()
    val fogColorIndex by viewModel.fogColorIndex.collectAsState()

    // Occlusion state
    val occlusionEnabled by viewModel.occlusionEnabled.collectAsState()
    val showShadowReceiver by viewModel.showShadowReceiver.collectAsState()
    val virtualObjectDepth by viewModel.virtualObjectDepth.collectAsState()

    Scaffold(
        containerColor = DeepNavy,
        topBar = {
            DepthLabTopBar(
                currentScene = currentScene,
                fps = fps,
                isRawDepth = isRawDepth,
                envIndex = envIndex,
                arSourceMode = arSourceMode,
                isArActive = (arSourceMode == ArSourceMode.REAL_ARCORE),
                onToggleArSource = { viewModel.toggleArSource() },
                onToggleRawDepth = { viewModel.toggleRawDepth() },
                onCycleEnvironment = { viewModel.cycleEnvironment() },
                onOpenInfo = { viewModel.toggleInfoDialog(true) }
            )
        },
        bottomBar = {
            DepthLabBottomBar(
                currentScene = currentScene,
                onSceneSelected = { viewModel.selectScene(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavy)
                .padding(innerPadding)
        ) {
            // Live Real-World AR Camera Surface Feed
            if (arSourceMode == ArSourceMode.REAL_ARCORE && arManager.session != null) {
                AndroidView(
                    factory = { ctx ->
                        ArCameraSurfaceView(ctx).apply {
                            arManager.session?.let { attachSession(it) }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Crossfade(
                targetState = currentScene,
                label = "SceneTransition"
            ) { scene ->
                when (scene) {
                    DepthSceneType.ORIENTED_RETICLE -> {
                        OrientedReticleScreen(
                            currentFrame = currentFrame,
                            reticlePos = reticlePos,
                            distanceM = reticleDistance,
                            normal = reticleNormal,
                            angleDeg = reticleAngle,
                            paintSplats = paintSplats,
                            onSetReticlePos = { u, v -> viewModel.setReticlePosition(u, v) },
                            onShootPaint = { viewModel.shootPaintSplat() },
                            onClearSplats = { viewModel.clearPaintSplats() }
                        )
                    }
                    DepthSceneType.DEPTH_MAP -> {
                        DepthMapScreen(
                            currentFrame = currentFrame,
                            colormapType = colormapType,
                            cameraOpacity = cameraOpacity,
                            minDistance = minDistance,
                            maxDistance = maxDistance,
                            inspectionData = inspectionData,
                            onColormapChanged = { viewModel.setColormap(it) },
                            onCameraOpacityChanged = { viewModel.setCameraOpacity(it) },
                            onRangeChanged = { min, max -> viewModel.setVisualizationRange(min, max) },
                            onInspectAt = { sx, sy, u, v -> viewModel.inspectDepthAt(sx, sy, u, v) },
                            onClearInspection = { viewModel.clearInspection() }
                        )
                    }
                    DepthSceneType.COLLIDER -> {
                        ColliderScreen(
                            currentFrame = currentFrame,
                            projectiles = projectiles,
                            selectedShape = selectedShape,
                            thrust = thrust,
                            renderWireframe = renderWireframe,
                            onShootProjectile = { viewModel.shootProjectile() },
                            onClearProjectiles = { viewModel.clearProjectiles() },
                            onSelectShape = { viewModel.setProjectileShape(it) },
                            onSetThrust = { viewModel.setProjectileThrust(it) },
                            onToggleWireframe = { viewModel.toggleWireframe() }
                        )
                    }
                    DepthSceneType.AVATAR_LOCOMOTION -> {
                        AvatarLocomotionScreen(
                            currentFrame = currentFrame,
                            avatarState = avatarState,
                            onMoveAvatarToTap = { u, v -> viewModel.moveAvatarToTap(u, v) },
                            onTriggerAction = { viewModel.triggerAvatarAction(it) }
                        )
                    }
                    DepthSceneType.POINT_CLOUD -> {
                        PointCloudScreen(
                            currentFrame = currentFrame,
                            yaw = orbitYaw,
                            pitch = orbitPitch,
                            zoom = orbitZoom,
                            pointSize = pointSize,
                            confidenceThreshold = confidenceThreshold,
                            subsampleStep = subsampleStep,
                            useDepthColormap = useDepthColormap,
                            onOrbitChanged = { dyaw, dpitch -> viewModel.updateOrbit(dyaw, dpitch) },
                            onZoomChanged = { factor -> viewModel.updateZoom(factor) },
                            onResetOrbit = { viewModel.resetOrbit() },
                            onSetConfidence = { viewModel.setConfidenceThreshold(it) },
                            onToggleColormap = { viewModel.togglePointCloudColormap() }
                        )
                    }
                    DepthSceneType.FOG_EFFECT -> {
                        FogEffectScreen(
                            currentFrame = currentFrame,
                            fogDistance = fogDistance,
                            fogThickness = fogThickness,
                            fogColorIndex = fogColorIndex,
                            onFogParamsChanged = { dist, thick, idx ->
                                viewModel.setFogParams(dist, thick, idx)
                            }
                        )
                    }
                    DepthSceneType.DEPTH_OCCLUSION -> {
                        DepthOcclusionScreen(
                            currentFrame = currentFrame,
                            occlusionEnabled = occlusionEnabled,
                            showShadowReceiver = showShadowReceiver,
                            virtualObjectDepth = virtualObjectDepth,
                            onToggleOcclusion = { viewModel.toggleOcclusion() },
                            onToggleShadowReceiver = { viewModel.toggleShadowReceiver() },
                            onSetObjectDepth = { viewModel.setVirtualObjectDepth(it) }
                        )
                    }
                }
            }

            if (showInfoDialog) {
                InfoDialog(
                    onDismiss = { viewModel.toggleInfoDialog(false) }
                )
            }

            // AR status HUD banner
            arStatusMessage?.let { status ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC090D16))
                        .border(
                            1.dp,
                            SciFiGreen.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SciFiGreen
                    )
                }
            }
        }
    }
}
