package com.example.depthlab.ar

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import com.example.depthlab.data.model.ArSessionState
import com.example.depthlab.data.model.DepthFrame
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraIntrinsics
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Real ARCore Session and Real-World Depth API Provider.
 * Connects directly to Google Play Services for AR to stream live camera frames,
 * hardware/ToF depth maps, and surface intrinsics.
 */
class RealArCoreManager(private val context: Context) {

    companion object {
        private const val TAG = "RealArCoreManager"
    }

    var session: Session? = null
        private set

    private val _arSessionState = MutableStateFlow(ArSessionState())
    val arSessionState: StateFlow<ArSessionState> = _arSessionState.asStateFlow()

    private var installRequested = false
    var useRawDepth: Boolean = false
        set(value) {
            field = value
            reconfigureSessionDepth()
        }

    /**
     * Checks if ARCore is supported and available on this device.
     */
    fun checkArCoreAvailability(): Boolean {
        val availability = ArCoreApk.getInstance().checkAvailability(context)
        val isSupported = availability.isSupported
        _arSessionState.value = _arSessionState.value.copy(
            isArCoreSupported = isSupported
        )
        return isSupported
    }

    /**
     * Initializes and starts the ARCore session with Real-Time Depth enabled.
     */
    fun startSession(activity: Activity): Boolean {
        if (session != null) return true

        try {
            // Check Google Play Services for AR installation / update
            val installStatus = ArCoreApk.getInstance().requestInstall(activity, !installRequested)
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                installRequested = true
                return false
            }

            val newSession = Session(activity)
            val config = Config(newSession)

            // Configure Depth Mode
            val depthModeSupported = newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            val rawDepthSupported = newSession.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)

            if (depthModeSupported || rawDepthSupported) {
                config.depthMode = if (useRawDepth && rawDepthSupported) {
                    Config.DepthMode.RAW_DEPTH_ONLY
                } else {
                    Config.DepthMode.AUTOMATIC
                }
                Log.d(TAG, "ARCore Depth Mode enabled: ${config.depthMode}")
            } else {
                config.depthMode = Config.DepthMode.DISABLED
                Log.w(TAG, "Device does not support ARCore Depth Mode")
            }

            // Optimize focus and update mode
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

            newSession.configure(config)
            newSession.resume()

            session = newSession
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = true,
                isDepthSupported = (depthModeSupported || rawDepthSupported),
                trackingState = "TRACKING_ACTIVE",
                errorMessage = null
            )
            return true

        } catch (e: UnavailableArcoreNotInstalledException) {
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = false,
                errorMessage = "Google Play Services for AR is not installed"
            )
            Log.e(TAG, "ARCore not installed", e)
        } catch (e: UnavailableApkTooOldException) {
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = false,
                errorMessage = "ARCore APK needs update"
            )
            Log.e(TAG, "ARCore APK too old", e)
        } catch (e: UnavailableSdkTooOldException) {
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = false,
                errorMessage = "App SDK is too old for ARCore"
            )
            Log.e(TAG, "SDK too old", e)
        } catch (e: UnavailableDeviceNotCompatibleException) {
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = false,
                errorMessage = "Device not compatible with ARCore"
            )
            Log.e(TAG, "Device not compatible", e)
        } catch (e: CameraNotAvailableException) {
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = false,
                errorMessage = "Camera device busy or not available"
            )
            Log.e(TAG, "Camera not available", e)
        } catch (e: Exception) {
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = false,
                errorMessage = e.localizedMessage ?: "Failed to start ARCore session"
            )
            Log.e(TAG, "Failed to start ARCore session", e)
        }
        return false
    }

    private fun reconfigureSessionDepth() {
        val currentSession = session ?: return
        try {
            val config = currentSession.config
            if (useRawDepth && currentSession.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)) {
                config.depthMode = Config.DepthMode.RAW_DEPTH_ONLY
            } else if (currentSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
            currentSession.configure(config)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update depth mode config", e)
        }
    }

    /**
     * Polls the live ARCore session on each frame and extracts the 16-bit depth buffer
     * and hardware camera intrinsics.
     */
    fun processCurrentFrame(): DepthFrame? {
        val currentSession = session ?: return null

        try {
            val frame: Frame = currentSession.update()
            val camera = frame.camera

            val tracking = camera.trackingState
            val trackingText = when (tracking) {
                TrackingState.TRACKING -> "TRACKING"
                TrackingState.PAUSED -> "TRACKING_PAUSED"
                TrackingState.STOPPED -> "STOPPED"
                else -> "UNKNOWN"
            }
            _arSessionState.value = _arSessionState.value.copy(
                trackingState = trackingText
            )

            // Acquire 16-bit millimeter depth image directly from hardware
            val depthImage: Image? = try {
                if (useRawDepth) {
                    try {
                        frame.acquireRawDepthImage16Bits()
                    } catch (e: NoSuchMethodError) {
                        frame.acquireDepthImage16Bits()
                    }
                } else {
                    frame.acquireDepthImage16Bits()
                }
            } catch (e: NotYetAvailableException) {
                // Depth is still computing in structure-from-motion loop
                null
            } catch (e: Exception) {
                Log.w(TAG, "Depth image acquisition issue: ${e.message}")
                null
            }

            // If depth buffer is available, convert into unified DepthFrame
            if (depthImage != null) {
                val depthFrame = extractDepthFrameFromImage(depthImage, camera.imageIntrinsics)
                depthImage.close()
                return depthFrame
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error updating AR frame: ${e.message}")
        }
        return null
    }

    /**
     * Converts an Android android.media.Image (16-bit unsigned depth, millimeter units)
     * into our standard DepthFrame (meters, floats).
     */
    private fun extractDepthFrameFromImage(image: Image, intrinsics: CameraIntrinsics): DepthFrame {
        val width = image.width
        val height = image.height

        val plane = image.planes[0]
        val buffer: ByteBuffer = plane.buffer.order(ByteOrder.nativeOrder())
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride

        val depths = FloatArray(width * height)
        val confidences = FloatArray(width * height)

        for (y in 0 until height) {
            val rowOffset = y * rowStride
            for (x in 0 until width) {
                val index = rowOffset + (x * pixelStride)
                val depthMillimeters = buffer.getShort(index).toInt() and 0xFFFF
                val depthMeters = depthMillimeters / 1000f

                val outIdx = y * width + x
                depths[outIdx] = depthMeters

                // Confidence heuristic: 0 depth is invalid/unmeasured; valid range 0.1m - 8m
                confidences[outIdx] = if (depthMeters > 0.05f && depthMeters < 8.5f) 0.95f else 0.0f
            }
        }

        val focalLength = intrinsics.focalLength
        val principalPoint = intrinsics.principalPoint

        // Scale intrinsics according to depth map resolution
        val imgDims = intrinsics.imageDimensions
        val scaleX = width.toFloat() / imgDims[0].toFloat()
        val scaleY = height.toFloat() / imgDims[1].toFloat()

        return DepthFrame(
            width = width,
            height = height,
            depths = depths,
            confidences = confidences,
            focalLengthX = focalLength[0] * scaleX,
            focalLengthY = focalLength[1] * scaleY,
            principalX = principalPoint[0] * scaleX,
            principalY = principalPoint[1] * scaleY,
            timestampMs = System.currentTimeMillis()
        )
    }

    fun pause() {
        try {
            session?.pause()
            _arSessionState.value = _arSessionState.value.copy(
                isSessionActive = false,
                trackingState = "PAUSED"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing ARCore session", e)
        }
    }

    fun resume(activity: Activity) {
        if (session != null) {
            try {
                session?.resume()
                _arSessionState.value = _arSessionState.value.copy(
                    isSessionActive = true,
                    trackingState = "TRACKING_ACTIVE"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming ARCore session", e)
            }
        } else {
            startSession(activity)
        }
    }

    fun destroy() {
        try {
            session?.close()
            session = null
            _arSessionState.value = ArSessionState()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ARCore session", e)
        }
    }
}
