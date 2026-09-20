package com.example.depthlab.data.model

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector3D(x / scalar, y / scalar, z / scalar) else Vector3D()

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 1e-6f) this / len else Vector3D(0f, 1f, 0f)
    }

    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D = Vector3D(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun angleTo(other: Vector3D): Float {
        val dotProd = (this.normalized().dot(other.normalized())).coerceIn(-1f, 1f)
        return acos(dotProd) * (180f / Math.PI.toFloat())
    }

    companion object {
        val Zero = Vector3D(0f, 0f, 0f)
        val Up = Vector3D(0f, 1f, 0f)
        val Down = Vector3D(0f, -1f, 0f)
        val Forward = Vector3D(0f, 0f, 1f)
        val Back = Vector3D(0f, 0f, -1f)
        val Right = Vector3D(1f, 0f, 0f)
        val Left = Vector3D(-1f, 0f, 0f)

        fun lerp(start: Vector3D, end: Vector3D, t: Float): Vector3D {
            val clampedT = t.coerceIn(0f, 1f)
            return start + (end - start) * clampedT
        }
    }
}
