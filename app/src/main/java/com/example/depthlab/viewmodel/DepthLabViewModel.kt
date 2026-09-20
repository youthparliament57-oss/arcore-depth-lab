package com.example.depthlab.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.depthlab.data.model.*
import com.example.depthlab.engine.DepthEngine
import com.example.depthlab.engine.PhysicsEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class DepthLabViewModel : ViewModel() {

    val depthEngine = DepthEngine()
    private val physicsEngine = PhysicsEngine(depthEngine)
    var realArCoreManager: com.example.depthlab.ar.RealArCoreManager? = null

    private val _currentScene = MutableStateFlow(DepthSceneType.ORIENTED_RETICLE)
    val currentScene: StateFlow<DepthSceneType> = _currentScene.asStateFlow()

    private val _arSourceMode = MutableStateFlow(ArSourceMode.REAL_ARCORE)
    val arSourceMode: StateFlow<ArSourceMode> = _arSourceMode.asStateFlow()

    private val _currentFrame = MutableStateFlow(depthEngine.generateDepthFrame(0f))
    val currentFrame: StateFlow<DepthFrame> = _currentFrame.asStateFlow()

    private val _fps = MutableStateFlow(60)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private val _isRawDepth = MutableStateFlow(false)
    val isRawDepth: StateFlow<Boolean> = _isRawDepth.asStateFlow()

    private val _currentEnvIndex = MutableStateFlow(0)
    val currentEnvIndex: StateFlow<Int> = _currentEnvIndex.asStateFlow()

    private val _showInfoDialog = MutableStateFlow(false)
    val showInfoDialog: StateFlow<Boolean> = _showInfoDialog.asStateFlow()

    private val _arStatusMessage = MutableStateFlow<String?>(null)
    val arStatusMessage: StateFlow<String?> = _arStatusMessage.asStateFlow()

    // --- Oriented Reticle State ---
    private val _reticleScreenPos = MutableStateFlow(Pair(0.5f, 0.5f))
    val reticleScreenPos: StateFlow<Pair<Float, Float>> = _reticleScreenPos.asStateFlow()

    private val _reticleDistance = MutableStateFlow(1.45f)
    val reticleDistance: StateFlow<Float> = _reticleDistance.asStateFlow()

    private val _reticleNormal = MutableStateFlow(Vector3D(0f, 0.2f, 0.98f).normalized())
    val reticleNormal: StateFlow<Vector3D> = _reticleNormal.asStateFlow()

    private val _reticleAngle = MutableStateFlow(12.5f)
    val reticleAngle: StateFlow<Float> = _reticleAngle.asStateFlow()

    private val _paintSplats = MutableStateFlow<List<PaintSplat>>(emptyList())
    val paintSplats: StateFlow<List<PaintSplat>> = _paintSplats.asStateFlow()

    // --- Depth Map State ---
    private val _colormapType = MutableStateFlow(ColormapType.TURBO)
    val colormapType: StateFlow<ColormapType> = _colormapType.asStateFlow()

    private val _cameraViewOpacity = MutableStateFlow(0.0f) // 0 = 100% depth map, 1 = 100% camera view
    val cameraViewOpacity: StateFlow<Float> = _cameraViewOpacity.asStateFlow()

    private val _minVisualizationDist = MutableStateFlow(0.3f)
    val minVisualizationDist: StateFlow<Float> = _minVisualizationDist.asStateFlow()

    private val _maxVisualizationDist = MutableStateFlow(6.5f)
    val maxVisualizationDist: StateFlow<Float> = _maxVisualizationDist.asStateFlow()

    private val _inspectionData = MutableStateFlow<InspectionData?>(null)
    val inspectionData: StateFlow<InspectionData?> = _inspectionData.asStateFlow()

    // --- Collider State ---
    private val _projectiles = MutableStateFlow<List<Projectile>>(emptyList())
    val projectiles: StateFlow<List<Projectile>> = _projectiles.asStateFlow()

    private val _selectedProjectileShape = MutableStateFlow(ProjectileShape.SPHERE)
    val selectedProjectileShape: StateFlow<ProjectileShape> = _selectedProjectileShape.asStateFlow()

    private val _projectileThrust = MutableStateFlow(5.5f)
    val projectileThrust: StateFlow<Float> = _projectileThrust.asStateFlow()

    private val _renderWireframe = MutableStateFlow(true)
    val renderWireframe: StateFlow<Boolean> = _renderWireframe.asStateFlow()

    // --- Avatar State ---
    private val _avatarState = MutableStateFlow(
        AvatarState(position = Vector3D(0f, -0.45f, 1.8f), targetPosition = Vector3D(0f, -0.45f, 1.8f))
    )
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()

    // --- Point Cloud State ---
    private val _orbitYaw = MutableStateFlow(15f)
    val orbitYaw: StateFlow<Float> = _orbitYaw.asStateFlow()

    private val _orbitPitch = MutableStateFlow(10f)
    val orbitPitch: StateFlow<Float> = _orbitPitch.asStateFlow()

    private val _orbitZoom = MutableStateFlow(1.0f)
    val orbitZoom: StateFlow<Float> = _orbitZoom.asStateFlow()

    private val _pointSize = MutableStateFlow(4.5f)
    val pointSize: StateFlow<Float> = _pointSize.asStateFlow()

    private val _confidenceThreshold = MutableStateFlow(0.4f)
    val confidenceThreshold: StateFlow<Float> = _confidenceThreshold.asStateFlow()

    private val _subsampleStep = MutableStateFlow(2)
    val subsampleStep: StateFlow<Int> = _subsampleStep.asStateFlow()

    private val _useDepthColormap = MutableStateFlow(true)
    val useDepthColormap: StateFlow<Boolean> = _useDepthColormap.asStateFlow()

    // --- Fog Effect State ---
    private val _fogDistance = MutableStateFlow(1.5f)
    val fogDistance: StateFlow<Float> = _fogDistance.asStateFlow()

    private val _fogThickness = MutableStateFlow(0.75f)
    val fogThickness: StateFlow<Float> = _fogThickness.asStateFlow()

    private val _fogColorIndex = MutableStateFlow(0)
    val fogColorIndex: StateFlow<Int> = _fogColorIndex.asStateFlow()

    // --- Depth Occlusion State ---
    private val _occlusionEnabled = MutableStateFlow(true)
    val occlusionEnabled: StateFlow<Boolean> = _occlusionEnabled.asStateFlow()

    private val _showShadowReceiver = MutableStateFlow(true)
    val showShadowReceiver: StateFlow<Boolean> = _showShadowReceiver.asStateFlow()

    private val _virtualObjectDepth = MutableStateFlow(1.6f)
    val virtualObjectDepth: StateFlow<Float> = _virtualObjectDepth.asStateFlow()

    private var simulationTime = 0f
    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()

    init {
        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        viewModelScope.launch {
            val dt = 0.033f // ~30 fps update loop
            while (isActive) {
                simulationTime += dt
                frameCount++

                // Update FPS calculation
                val now = System.currentTimeMillis()
                if (now - lastFpsTimestamp >= 1000) {
                    _fps.value = (frameCount * 1000f / (now - lastFpsTimestamp)).toInt().coerceIn(24, 60)
                    frameCount = 0
                    lastFpsTimestamp = now
                }

                // Fetch Real AR Depth frame from hardware if available, or generate dynamic frame
                val arManager = realArCoreManager
                val realFrame = if (_arSourceMode.value == ArSourceMode.REAL_ARCORE && arManager != null) {
                    arManager.processCurrentFrame()
                } else {
                    null
                }

                val frame = if (realFrame != null) {
                    _arStatusMessage.value = "Tracking Real Hardware Depth (${realFrame.width}x${realFrame.height})"
                    realFrame
                } else {
                    if (_arSourceMode.value == ArSourceMode.REAL_ARCORE && arManager?.session != null) {
                        _arStatusMessage.value = "Calibrating ARCore depth camera..."
                    } else if (_arSourceMode.value == ArSourceMode.REAL_ARCORE) {
                        _arStatusMessage.value = "ARCore initializing / preview mode"
                    }
                    depthEngine.generateDepthFrame(simulationTime)
                }

                _currentFrame.value = frame

                // Update reticle metrics
                val (u, v) = _reticleScreenPos.value
                val depthAtReticle = frame.getDepth(
                    (u * frame.width).toInt().coerceIn(0, frame.width - 1),
                    (v * frame.height).toInt().coerceIn(0, frame.height - 1)
                )
                _reticleDistance.value = depthAtReticle
                val normal = depthEngine.computeNormalFromDepth(frame, u, v)
                _reticleNormal.value = normal
                _reticleAngle.value = normal.angleTo(Vector3D.Forward)

                // Update physics projectiles
                if (_projectiles.value.isNotEmpty()) {
                    val currentList = _projectiles.value.map { it.copy() }.toMutableList()
                    physicsEngine.updateProjectiles(currentList, frame, dt)
                    _projectiles.value = currentList
                }

                // Update avatar pathing and locomotion
                updateAvatar(dt, frame)

                delay(33)
            }
        }
    }

    private fun updateAvatar(dt: Float, frame: DepthFrame) {
        val current = _avatarState.value
        var pos = current.position
        val target = current.targetPosition

        val diff = target - pos
        val horizontalDist = Vector3D(diff.x, 0f, diff.z).length()

        var nextAction = current.currentAction
        var newTimer = current.actionTimer + dt
        var newY = pos.y
        var newVelY = current.jumpVelocityY

        when (current.currentAction) {
            AvatarAction.WALK -> {
                if (horizontalDist > 0.05f) {
                    val dir = Vector3D(diff.x, 0f, diff.z).normalized()
                    val speed = 0.85f * dt
                    pos = pos + dir * speed
                    val rot = atan2(dir.x, dir.z) * (180f / Math.PI.toFloat())

                    // Adapt height to depth terrain
                    val uv = depthEngine.worldPointToScreenUV(frame, pos)
                    if (uv != null) {
                        val groundDepth = frame.getDepth(
                            (uv.first * frame.width).toInt().coerceIn(0, frame.width - 1),
                            (uv.second * frame.height).toInt().coerceIn(0, frame.height - 1)
                        )
                        val groundWorld = depthEngine.screenUVToWorldPoint(frame, uv.first, uv.second, groundDepth)
                        newY = groundWorld.y + 0.15f
                    }

                    _avatarState.value = current.copy(
                        position = Vector3D(pos.x, newY, pos.z),
                        rotationY = rot,
                        actionTimer = newTimer
                    )
                } else {
                    _avatarState.value = current.copy(currentAction = AvatarAction.IDLE, actionTimer = 0f)
                }
            }
            AvatarAction.JUMP -> {
                newVelY -= 9.8f * dt
                newY += newVelY * dt
                if (newY <= -0.45f) {
                    newY = -0.45f
                    newVelY = 0f
                    nextAction = AvatarAction.IDLE
                }
                _avatarState.value = current.copy(
                    position = Vector3D(pos.x, newY, pos.z),
                    jumpVelocityY = newVelY,
                    currentAction = nextAction,
                    actionTimer = newTimer
                )
            }
            AvatarAction.ATTACK, AvatarAction.FAST_ATTACK, AvatarAction.FRENZY -> {
                if (newTimer > 1.2f) {
                    _avatarState.value = current.copy(currentAction = AvatarAction.IDLE, actionTimer = 0f)
                } else {
                    _avatarState.value = current.copy(actionTimer = newTimer)
                }
            }
            AvatarAction.TELEPORT -> {
                if (newTimer > 0.4f) {
                    _avatarState.value = current.copy(
                        position = target,
                        currentAction = AvatarAction.IDLE,
                        actionTimer = 0f
                    )
                } else {
                    _avatarState.value = current.copy(actionTimer = newTimer)
                }
            }
            AvatarAction.IDLE -> {
                // Gentle hover oscillation
                val hoverOffset = sin(simulationTime * 2.5f) * 0.015f
                _avatarState.value = current.copy(
                    position = Vector3D(pos.x, -0.45f + hoverOffset, pos.z)
                )
            }
        }
    }

    // --- User Actions ---

    fun selectScene(scene: DepthSceneType) {
        _currentScene.value = scene
    }

    fun toggleArSource() {
        val nextMode = if (_arSourceMode.value == ArSourceMode.REAL_ARCORE) {
            ArSourceMode.SIMULATED
        } else {
            ArSourceMode.REAL_ARCORE
        }
        _arSourceMode.value = nextMode
    }

    fun toggleRawDepth() {
        val next = !_isRawDepth.value
        _isRawDepth.value = next
        depthEngine.isRawDepth = next
        realArCoreManager?.useRawDepth = next
    }

    fun cycleEnvironment() {
        val next = (_currentEnvIndex.value + 1) % DepthEngine.ENVIRONMENTS.size
        _currentEnvIndex.value = next
        depthEngine.setEnvironment(next)
    }

    fun setReticlePosition(u: Float, v: Float) {
        _reticleScreenPos.value = Pair(u.coerceIn(0.05f, 0.95f), v.coerceIn(0.05f, 0.95f))
    }

    fun shootPaintSplat() {
        val frame = _currentFrame.value
        val (u, v) = _reticleScreenPos.value
        val dist = _reticleDistance.value
        val normal = _reticleNormal.value
        val worldPos = depthEngine.screenUVToWorldPoint(frame, u, v, dist)

        val colors = listOf(
            Color(0xFFFF2A6D), // Neon Pink
            Color(0xFF00E5FF), // Cyan
            Color(0xFFFFB800), // Glow Amber
            Color(0xFF05FFA1), // Emerald
            Color(0xFF9D4EDD)  // Violet
        )
        val chosenColor = colors.random()

        // Generate splatter droplet offsets
        val droplets = List(6) {
            Vector3D(
                (Math.random().toFloat() - 0.5f) * 0.08f,
                (Math.random().toFloat() - 0.5f) * 0.08f,
                (Math.random().toFloat() - 0.5f) * 0.02f
            )
        }

        val newSplat = PaintSplat(
            worldPos = worldPos,
            normal = normal,
            color = chosenColor,
            radius = 0.09f + (Math.random().toFloat() * 0.05f),
            droplets = droplets
        )

        _paintSplats.value = (_paintSplats.value + newSplat).takeLast(25)
    }

    fun clearPaintSplats() {
        _paintSplats.value = emptyList()
    }

    fun inspectDepthAt(screenX: Float, screenY: Float, u: Float, v: Float) {
        val frame = _currentFrame.value
        val clampedU = u.coerceIn(0f, 1f)
        val clampedV = v.coerceIn(0f, 1f)
        val px = (clampedU * frame.width).toInt().coerceIn(0, frame.width - 1)
        val py = (clampedV * frame.height).toInt().coerceIn(0, frame.height - 1)

        val depth = frame.getDepth(px, py)
        val conf = frame.getConfidence(px, py)
        val normal = depthEngine.computeNormalFromDepth(frame, clampedU, clampedV)
        val world = depthEngine.screenUVToWorldPoint(frame, clampedU, clampedV, depth)
        val angle = normal.angleTo(Vector3D.Forward)

        _inspectionData.value = InspectionData(
            screenX = screenX,
            screenY = screenY,
            depthMeters = depth,
            confidence = conf,
            normal = normal,
            worldPos = world,
            inclinationDeg = angle
        )
    }

    fun clearInspection() {
        _inspectionData.value = null
    }

    fun setColormap(type: ColormapType) {
        _colormapType.value = type
    }

    fun setCameraOpacity(opacity: Float) {
        _cameraViewOpacity.value = opacity.coerceIn(0f, 1f)
    }

    fun setVisualizationRange(min: Float, max: Float) {
        _minVisualizationDist.value = min.coerceAtLeast(0.1f)
        _maxVisualizationDist.value = max.coerceAtLeast(min + 0.5f)
    }

    fun shootProjectile() {
        val frame = _currentFrame.value
        val shape = _selectedProjectileShape.value
        val thrust = _projectileThrust.value

        // Spawn from camera view origin forward
        val spawnPos = Vector3D(
            (Math.random().toFloat() - 0.5f) * 0.15f,
            0.1f,
            0.35f
        )
        val forwardThrust = Vector3D(
            (Math.random().toFloat() - 0.5f) * 0.8f,
            0.6f,
            thrust
        )

        val colors = listOf(
            Color(0xFF00E5FF),
            Color(0xFFFF2A6D),
            Color(0xFFFFB800),
            Color(0xFF05FFA1)
        )

        val newProjectile = Projectile(
            shape = shape,
            position = spawnPos,
            velocity = forwardThrust,
            color = colors.random(),
            radius = when (shape) {
                ProjectileShape.SPHERE -> 0.08f
                ProjectileShape.CUBE -> 0.09f
                ProjectileShape.CAPSULE -> 0.07f
            }
        )

        _projectiles.value = (_projectiles.value + newProjectile).takeLast(20)
    }

    fun clearProjectiles() {
        _projectiles.value = emptyList()
    }

    fun setProjectileShape(shape: ProjectileShape) {
        _selectedProjectileShape.value = shape
    }

    fun setProjectileThrust(thrust: Float) {
        _projectileThrust.value = thrust.coerceIn(1f, 12f)
    }

    fun toggleWireframe() {
        _renderWireframe.value = !_renderWireframe.value
    }

    fun moveAvatarToTap(u: Float, v: Float) {
        val frame = _currentFrame.value
        val depth = frame.getDepth(
            (u * frame.width).toInt().coerceIn(0, frame.width - 1),
            (v * frame.height).toInt().coerceIn(0, frame.height - 1)
        )
        val targetWorld = depthEngine.screenUVToWorldPoint(frame, u, v, depth)
        _avatarState.value = _avatarState.value.copy(
            targetPosition = targetWorld,
            currentAction = AvatarAction.WALK,
            actionTimer = 0f
        )
    }

    fun triggerAvatarAction(action: AvatarAction) {
        when (action) {
            AvatarAction.JUMP -> {
                if (_avatarState.value.currentAction != AvatarAction.JUMP) {
                    _avatarState.value = _avatarState.value.copy(
                        currentAction = AvatarAction.JUMP,
                        jumpVelocityY = 2.4f,
                        actionTimer = 0f
                    )
                }
            }
            AvatarAction.TELEPORT -> {
                // Teleport to a forward position
                val current = _avatarState.value
                val randomOffset = Vector3D((Math.random().toFloat() - 0.5f) * 0.8f, 0f, (Math.random().toFloat() - 0.5f) * 0.6f)
                val newTarget = current.position + randomOffset
                _avatarState.value = current.copy(
                    targetPosition = newTarget,
                    currentAction = AvatarAction.TELEPORT,
                    actionTimer = 0f
                )
            }
            else -> {
                _avatarState.value = _avatarState.value.copy(
                    currentAction = action,
                    actionTimer = 0f
                )
            }
        }
    }

    fun updateOrbit(dYaw: Float, dPitch: Float) {
        _orbitYaw.value = (_orbitYaw.value + dYaw) % 360f
        _orbitPitch.value = (_orbitPitch.value + dPitch).coerceIn(-45f, 45f)
    }

    fun updateZoom(factor: Float) {
        _orbitZoom.value = (_orbitZoom.value * factor).coerceIn(0.5f, 3.0f)
    }

    fun resetOrbit() {
        _orbitYaw.value = 15f
        _orbitPitch.value = 10f
        _orbitZoom.value = 1.0f
    }

    fun setPointSize(size: Float) {
        _pointSize.value = size.coerceIn(1.5f, 10f)
    }

    fun setConfidenceThreshold(threshold: Float) {
        _confidenceThreshold.value = threshold.coerceIn(0f, 1f)
    }

    fun setSubsampleStep(step: Int) {
        _subsampleStep.value = step.coerceIn(1, 4)
    }

    fun togglePointCloudColormap() {
        _useDepthColormap.value = !_useDepthColormap.value
    }

    fun setFogParams(dist: Float, thickness: Float, colorIdx: Int) {
        _fogDistance.value = dist.coerceIn(0.5f, 6.0f)
        _fogThickness.value = thickness.coerceIn(0.1f, 1.0f)
        _fogColorIndex.value = colorIdx
    }

    fun toggleOcclusion() {
        _occlusionEnabled.value = !_occlusionEnabled.value
    }

    fun toggleShadowReceiver() {
        _showShadowReceiver.value = !_showShadowReceiver.value
    }

    fun setVirtualObjectDepth(depth: Float) {
        _virtualObjectDepth.value = depth.coerceIn(0.5f, 4.5f)
    }

    fun toggleInfoDialog(show: Boolean) {
        _showInfoDialog.value = show
    }
}
