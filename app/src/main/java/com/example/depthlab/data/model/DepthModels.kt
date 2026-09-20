package com.example.depthlab.data.model

import androidx.compose.ui.graphics.Color

enum class DepthSceneType(val title: String, val subtitle: String, val iconName: String) {
    ORIENTED_RETICLE("3D Reticle", "Normal estimation & splat", "reticle"),
    DEPTH_MAP("Depth Map", "False-color Turbo colormap", "depthmap"),
    COLLIDER("Depth Collider", "Real-world physics simulation", "collider"),
    AVATAR_LOCOMOTION("Robochef", "Surface navigation & pathing", "avatar"),
    POINT_CLOUD("Point Cloud", "3D point cloud blender", "pointcloud"),
    FOG_EFFECT("AR Fog", "Atmospheric depth haze", "fog"),
    DEPTH_OCCLUSION("Occlusion", "Geometry occlusion & shadow", "occlusion")
}

enum class ColormapType(val displayName: String) {
    TURBO("Turbo (Google)"),
    JET("Jet Rainbow"),
    VIRIDIS("Viridis"),
    GRAYSCALE("Grayscale")
}

data class DepthFrame(
    val width: Int,
    val height: Int,
    val depths: FloatArray,          // depth in meters for each pixel
    val confidences: FloatArray,     // 0.0 to 1.0 confidence
    val focalLengthX: Float = 500f,
    val focalLengthY: Float = 500f,
    val principalX: Float = 250f,
    val principalY: Float = 250f,
    val timestampMs: Long = System.currentTimeMillis()
) {
    fun getDepth(x: Int, y: Int): Float {
        if (x !in 0 until width || y !in 0 until height) return 0f
        return depths[y * width + x]
    }

    fun getConfidence(x: Int, y: Int): Float {
        if (x !in 0 until width || y !in 0 until height) return 0f
        return confidences[y * width + x]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DepthFrame
        return timestampMs == other.timestampMs && width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + timestampMs.hashCode()
        return result
    }
}

data class InspectionData(
    val screenX: Float,
    val screenY: Float,
    val depthMeters: Float,
    val confidence: Float,
    val normal: Vector3D,
    val worldPos: Vector3D,
    val inclinationDeg: Float
)

enum class ProjectileShape {
    SPHERE, CUBE, CAPSULE
}

data class Projectile(
    val id: Long = System.nanoTime(),
    val shape: ProjectileShape = ProjectileShape.SPHERE,
    var position: Vector3D,
    var velocity: Vector3D,
    var rotation: Vector3D = Vector3D(),
    val color: Color = Color(0xFF00E5FF),
    val radius: Float = 0.08f,
    var isSleeping: Boolean = false,
    var isAnchored: Boolean = false,
    var lifetimeS: Float = 0f
)

enum class AvatarAction {
    IDLE, WALK, JUMP, ATTACK, FAST_ATTACK, FRENZY, TELEPORT
}

data class AvatarState(
    val position: Vector3D = Vector3D(0f, -0.4f, 1.8f),
    val targetPosition: Vector3D = Vector3D(0f, -0.4f, 1.8f),
    val rotationY: Float = 0f,
    val currentAction: AvatarAction = AvatarAction.IDLE,
    val actionTimer: Float = 0f,
    val isGrounded: Boolean = true,
    val jumpVelocityY: Float = 0f
)

data class PaintSplat(
    val id: Long = System.nanoTime(),
    val worldPos: Vector3D,
    val normal: Vector3D,
    val color: Color,
    val radius: Float = 0.12f,
    val droplets: List<Vector3D> = emptyList()
)

data class DepthEnvironment(
    val id: String,
    val name: String,
    val description: String
)
