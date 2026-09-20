package com.example.depthlab.engine

import com.example.depthlab.data.model.DepthEnvironment
import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.data.model.Vector3D
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DepthEngine {

    companion object {
        const val DEFAULT_WIDTH = 120
        const val DEFAULT_HEIGHT = 160
        const val OUTLIER_DEPTH_RATIO = 0.2f
        const val WINDOW_RADIUS_PIXELS = 2
        const val INVALID_DEPTH = -1f

        val ENVIRONMENTS = listOf(
            DepthEnvironment("living_room", "Living Room", "Uneven floor, coffee table, couch & wall obstacles"),
            DepthEnvironment("workspace", "Studio Desk", "Desktop, dual monitors, coffee mug & step elevation"),
            DepthEnvironment("corridor", "Urban Corridor", "Long hallway, pillars, ramps & recessed doorway")
        )
    }

    var isRawDepth: Boolean = false
    var currentEnvironmentIndex: Int = 0
        private set

    fun setEnvironment(index: Int) {
        currentEnvironmentIndex = index.coerceIn(0, ENVIRONMENTS.lastIndex)
    }

    /**
     * Generates a realistic dynamic depth frame for the chosen environment.
     * Incorporates sensor noise (for Raw Depth mode) or bilateral smoothing (for Smooth Depth mode).
     */
    fun generateDepthFrame(timeSeconds: Float): DepthFrame {
        val width = DEFAULT_WIDTH
        val height = DEFAULT_HEIGHT
        val depths = FloatArray(width * height)
        val confidences = FloatArray(width * height)

        val env = ENVIRONMENTS[currentEnvironmentIndex]

        for (y in 0 until height) {
            val ny = y.toFloat() / height.toFloat() // 0.0 (top) to 1.0 (bottom)
            for (x in 0 until width) {
                val nx = x.toFloat() / width.toFloat() // 0.0 (left) to 1.0 (right)

                val (d, c) = when (env.id) {
                    "living_room" -> computeLivingRoomDepth(nx, ny, timeSeconds)
                    "workspace" -> computeWorkspaceDepth(nx, ny, timeSeconds)
                    else -> computeCorridorDepth(nx, ny, timeSeconds)
                }

                var finalDepth = d
                var finalConf = c

                if (isRawDepth) {
                    // Raw depth exhibits high-frequency sensor noise and edge dropouts
                    val noise = (sin(x * 12.3f + y * 31.7f) * 0.04f)
                    finalDepth = (finalDepth + noise).coerceAtLeast(0.2f)
                    if ((x % 11 == 0 && y % 7 == 0) || finalDepth > 6.0f) {
                        finalConf *= 0.35f
                    }
                }

                val idx = y * width + x
                depths[idx] = finalDepth
                confidences[idx] = finalConf
            }
        }

        return DepthFrame(
            width = width,
            height = height,
            depths = depths,
            confidences = confidences,
            focalLengthX = width * 1.15f,
            focalLengthY = height * 1.15f,
            principalX = width * 0.5f,
            principalY = height * 0.5f,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun computeLivingRoomDepth(nx: Float, ny: Float, t: Float): Pair<Float, Float> {
        // Floor plane starting around middle down to bottom
        var depth = 1.2f + (1.0f - ny) * 3.5f

        // Back wall for top portion
        if (ny < 0.35f) {
            depth = 3.8f + (0.35f - ny) * 0.8f
        }

        // Coffee table in the center foreground (ny between 0.55 and 0.85, nx between 0.35 and 0.65)
        if (ny in 0.55f..0.85f && nx in 0.32f..0.68f) {
            depth = 1.35f - (ny - 0.55f) * 0.3f
        }

        // Armchair / Sofa on left side (nx < 0.3)
        if (nx < 0.28f && ny in 0.30f..0.75f) {
            depth = 2.1f - (0.28f - nx) * 0.8f
        }

        // Floor rug contour
        if (ny in 0.45f..0.95f && nx in 0.2f..0.8f) {
            depth += sin(nx * 8f + t * 0.2f) * 0.02f
        }

        return Pair(depth.coerceIn(0.3f, 8.0f), 0.95f)
    }

    private fun computeWorkspaceDepth(nx: Float, ny: Float, t: Float): Pair<Float, Float> {
        var depth = 0.8f + (1.0f - ny) * 2.8f

        // Studio back wall
        if (ny < 0.30f) {
            depth = 2.8f
        }

        // Dual Monitors in upper center
        if (ny in 0.25f..0.60f && nx in 0.25f..0.75f) {
            val angle = if (nx < 0.5f) (0.5f - nx) * 0.4f else (nx - 0.5f) * 0.4f
            depth = 1.15f + angle
        }

        // Keyboard & Desk surface in lower foreground
        if (ny > 0.65f) {
            depth = 0.55f + (1f - ny) * 0.9f
        }

        // Mug on right side
        if (ny in 0.70f..0.82f && nx in 0.75f..0.88f) {
            depth = 0.62f
        }

        return Pair(depth.coerceIn(0.25f, 7.0f), 0.92f)
    }

    private fun computeCorridorDepth(nx: Float, ny: Float, t: Float): Pair<Float, Float> {
        // Perspective corridor: walls on left and right, ceiling on top, floor on bottom
        val centerX = 0.5f
        val dx = abs(nx - centerX)

        var depth = 1.0f / (0.15f + dx * 1.5f + (1.0f - ny) * 0.5f)

        // Recessed doorway at the far end
        if (dx < 0.12f && ny in 0.35f..0.65f) {
            depth = 5.6f + sin(t * 0.5f) * 0.1f
        }

        // Left pillar obstacle
        if (nx in 0.18f..0.28f && ny in 0.25f..0.85f) {
            depth = 2.4f
        }

        return Pair(depth.coerceIn(0.4f, 9.0f), 0.96f)
    }

    /**
     * Exact port of ComputeNormalMapFromDepthWeightedMeanGradient from OrientedReticle.cs
     */
    fun computeNormalFromDepth(
        frame: DepthFrame,
        normX: Float,
        normY: Float
    ): Vector3D {
        val centerX = (normX * frame.width).toInt().coerceIn(0, frame.width - 1)
        val centerY = (normY * frame.height).toInt().coerceIn(0, frame.height - 1)

        val centerDepth = frame.getDepth(centerX, centerY)
        if (centerDepth <= 0f) return Vector3D.Up

        val outlierDist = OUTLIER_DEPTH_RATIO * centerDepth
        var neighborCorrX = 0f
        var neighborCorrY = 0f
        var sumConfX = 0f
        var sumConfY = 0f

        val r = WINDOW_RADIUS_PIXELS
        for (dy in -r..r) {
            for (dx in -r..r) {
                if (dx == 0 && dy == 0) continue

                val curX = (centerX + dx).coerceIn(0, frame.width - 1)
                val curY = (centerY + dy).coerceIn(0, frame.height - 1)
                val neighborDepth = frame.getDepth(curX, curY)
                if (neighborDepth <= 0f) continue

                val depthDiff = neighborDepth - centerDepth
                if (abs(depthDiff) > outlierDist) continue

                val weight = 1f / (1f + (dx * dx + dy * dy).toFloat())

                if (dx != 0) {
                    neighborCorrX += (depthDiff / dx.toFloat()) * weight
                    sumConfX += weight
                }
                if (dy != 0) {
                    neighborCorrY += (depthDiff / dy.toFloat()) * weight
                    sumConfY += weight
                }
            }
        }

        val gradX = if (sumConfX > 0f) neighborCorrX / sumConfX else 0f
        val gradY = if (sumConfY > 0f) neighborCorrY / sumConfY else 0f

        // Convert depth image gradient to 3D surface normal in camera coordinate space
        val normal = Vector3D(-gradX * 25f, -gradY * 25f, 1.0f).normalized()
        return normal
    }

    /**
     * Converts normalized screen UV (0..1) + depth into 3D view-space point (in meters)
     */
    fun screenUVToWorldPoint(
        frame: DepthFrame,
        u: Float,
        v: Float,
        depthMeters: Float
    ): Vector3D {
        val px = u * frame.width
        val py = v * frame.height
        val x = (px - frame.principalX) * depthMeters / frame.focalLengthX
        val y = -(py - frame.principalY) * depthMeters / frame.focalLengthY
        val z = depthMeters
        return Vector3D(x, y, z)
    }

    /**
     * Converts 3D point in camera space back to screen UV (0..1)
     */
    fun worldPointToScreenUV(
        frame: DepthFrame,
        point: Vector3D
    ): Pair<Float, Float>? {
        if (point.z <= 0.05f) return null
        val px = (point.x * frame.focalLengthX / point.z) + frame.principalX
        val py = (-point.y * frame.focalLengthY / point.z) + frame.principalY
        val u = px / frame.width
        val v = py / frame.height
        return Pair(u, v)
    }
}
