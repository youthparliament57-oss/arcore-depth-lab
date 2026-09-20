package com.example.depthlab.engine

import com.example.depthlab.data.model.DepthFrame
import com.example.depthlab.data.model.Projectile
import com.example.depthlab.data.model.Vector3D
import kotlin.math.abs

class PhysicsEngine(private val depthEngine: DepthEngine) {

    private val gravity = Vector3D(0f, -9.8f, 0f)
    private val restitution = 0.65f // Bounce coefficient
    private val friction = 0.88f // Surface friction

    fun updateProjectiles(
        projectiles: MutableList<Projectile>,
        frame: DepthFrame,
        dtSeconds: Float
    ) {
        val iterator = projectiles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.lifetimeS += dtSeconds

            // Remove expired projectiles after 3 minutes
            if (p.lifetimeS > 180f) {
                iterator.remove()
                continue
            }

            if (p.isSleeping || p.isAnchored) {
                continue
            }

            // Apply gravity
            p.velocity = p.velocity + gravity * dtSeconds

            // Predict next position
            val nextPos = p.position + p.velocity * dtSeconds

            // Check collision with depth surface
            val screenUV = depthEngine.worldPointToScreenUV(frame, nextPos)
            if (screenUV != null && screenUV.first in 0f..1f && screenUV.second in 0f..1f) {
                val surfaceDepth = frame.getDepth(
                    (screenUV.first * frame.width).toInt().coerceIn(0, frame.width - 1),
                    (screenUV.second * frame.height).toInt().coerceIn(0, frame.height - 1)
                )

                // If projectile penetrates surface
                if (nextPos.z >= surfaceDepth - p.radius && surfaceDepth > 0.1f) {
                    val normal = depthEngine.computeNormalFromDepth(frame, screenUV.first, screenUV.second)
                    val vDotN = p.velocity.dot(normal)

                    if (vDotN < 0f) {
                        // Reflect velocity across normal
                        val bounceVel = p.velocity - normal * (2f * vDotN)
                        p.velocity = (bounceVel * restitution)

                        // Apply friction tangent
                        val tangent = (p.velocity - normal * p.velocity.dot(normal)) * friction
                        val normalComponent = normal * p.velocity.dot(normal)
                        p.velocity = normalComponent + tangent

                        // Clamp position to surface
                        val clampedWorld = depthEngine.screenUVToWorldPoint(frame, screenUV.first, screenUV.second, surfaceDepth - p.radius)
                        p.position = clampedWorld

                        // Spin on bounce
                        p.rotation = p.rotation + Vector3D(p.velocity.z * 15f, p.velocity.x * 15f, 0f)

                        // Sleep check if velocity is negligible
                        if (p.velocity.length() < 0.18f) {
                            p.velocity = Vector3D.Zero
                            p.isSleeping = true
                            p.isAnchored = true
                            continue
                        }
                    } else {
                        p.position = nextPos
                    }
                } else {
                    p.position = nextPos
                }
            } else {
                p.position = nextPos
            }

            // Fallback ground plane at -1.2m
            if (p.position.y < -1.2f) {
                p.position = Vector3D(p.position.x, -1.2f, p.position.z)
                if (abs(p.velocity.y) > 0.25f) {
                    p.velocity = Vector3D(p.velocity.x * friction, -p.velocity.y * restitution, p.velocity.z * friction)
                } else {
                    p.velocity = Vector3D.Zero
                    p.isSleeping = true
                    p.isAnchored = true
                }
            }
        }
    }
}
