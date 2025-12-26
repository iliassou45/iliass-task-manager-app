package com.iliass.iliass

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.util.Log
import android.util.Size

object CameraDebugHelper {
    private const val TAG = "CameraDebugHelper"

    fun logSupportedVideoSizes(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facing) {
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    else -> "Unknown"
                }

                Log.d(TAG, "===== Camera $cameraId ($facingStr) =====")

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val videoSizes = map?.getOutputSizes(MediaRecorder::class.java)

                if (videoSizes != null && videoSizes.isNotEmpty()) {
                    Log.d(TAG, "Supported video recording sizes:")
                    videoSizes.forEach { size ->
                        val aspectRatio = size.width.toFloat() / size.height.toFloat()
                        val orientation = if (size.width > size.height) "Landscape" else "Portrait"
                        Log.d(TAG, "  ${size.width}x${size.height} - $orientation - Aspect: ${String.format("%.2f", aspectRatio)}")
                    }

                    // Check if specific sizes are supported
                    val has1920x1080 = videoSizes.any { it.width == 1920 && it.height == 1080 }
                    val has1080x1920 = videoSizes.any { it.width == 1080 && it.height == 1920 }

                    Log.d(TAG, "1920x1080 (Landscape) supported: $has1920x1080")
                    Log.d(TAG, "1080x1920 (Portrait) supported: $has1080x1920")

                    // Suggest alternatives
                    if (!has1080x1920) {
                        val portraitSizes = videoSizes.filter { it.width < it.height }
                        if (portraitSizes.isNotEmpty()) {
                            Log.d(TAG, "Alternative portrait sizes available:")
                            portraitSizes.take(3).forEach { size ->
                                Log.d(TAG, "  ${size.width}x${size.height}")
                            }
                        } else {
                            Log.d(TAG, "No native portrait sizes available - use landscape with orientation hint")
                        }
                    }
                } else {
                    Log.d(TAG, "No video sizes found for this camera")
                }

                Log.d(TAG, "=============================\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging camera info", e)
        }
    }

    fun getBestVideoSize(context: Context, preferPortrait: Boolean): Size? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            val cameraId = cameraManager.cameraIdList[0]
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val videoSizes = map?.getOutputSizes(MediaRecorder::class.java) ?: return null

            // Try exact match first
            if (preferPortrait) {
                videoSizes.find { it.width == 1080 && it.height == 1920 }?.let { return it }
                // Try any portrait size close to 1080p
                videoSizes.filter { it.width < it.height }.maxByOrNull { it.width * it.height }?.let { return it }
            } else {
                videoSizes.find { it.width == 1920 && it.height == 1080 }?.let { return it }
                // Try any landscape size close to 1080p
                videoSizes.filter { it.width > it.height }.maxByOrNull { it.width * it.height }?.let { return it }
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting best video size", e)
            return null
        }
    }
}