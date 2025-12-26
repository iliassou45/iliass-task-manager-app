package com.iliass.iliass

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoRecorderService : Service() {

    private val binder = LocalBinder()
    private var mediaRecorder: MediaRecorder? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var videoFile: File? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var dummySurface: Surface? = null
    private var dummyTexture: SurfaceTexture? = null

    private var isRecording = false
    private var recordingStartTime = 0L
    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    private var currentOrientation: VideoOrientation = VideoOrientation.LANDSCAPE

    var onRecordingStateChanged: ((Boolean) -> Unit)? = null
    var onRecordingTimeChanged: ((Int) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onStatusChanged: ((String) -> Unit)? = null

    enum class VideoOrientation(val width: Int, val height: Int, val label: String, val rotationHint: Int) {
        PORTRAIT(1920, 1080, "Portrait", 90),
        LANDSCAPE(1920, 1080, "Landscape", 0)
    }

    inner class LocalBinder : Binder() {
        fun getService(): VideoRecorderService = this@VideoRecorderService
    }

    companion object {
        private const val TAG = "VideoRecorderService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "video_recording_channel"

        const val ACTION_START_RECORDING = "com.iliass.iliass.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.iliass.iliass.STOP_RECORDING"
        const val EXTRA_ORIENTATION = "orientation"
    }

    override fun onCreate() {
        super.onCreate()
        startBackgroundThread()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val orientation = intent.getStringExtra(EXTRA_ORIENTATION)
                currentOrientation = if (orientation == "PORTRAIT") {
                    VideoOrientation.PORTRAIT
                } else {
                    VideoOrientation.LANDSCAPE
                }
                startRecording()
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    fun setOrientation(orientation: VideoOrientation) {
        if (isRecording) {
            onError?.invoke("Cannot change orientation while recording")
            return
        }
        currentOrientation = orientation
    }

    fun startRecording() {
        if (isRecording) {
            onError?.invoke("Already recording")
            return
        }

        try {
            videoFile = createVideoFile()

            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification("Starting recording..."))

            openCamera()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            onError?.invoke("Failed to start recording: ${e.message}")
            stopSelf()
        }
    }

    private fun openCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            val cameraId = cameraManager.cameraIdList[0]
            Log.d(TAG, "Opening camera: $cameraId")

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "Camera opened successfully")
                    cameraDevice = camera
                    startRecordingSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "Camera disconnected")
                    camera.close()
                    cameraDevice = null
                    onError?.invoke("Camera disconnected")
                    stopRecording()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                    cameraDevice = null
                    onError?.invoke("Camera error: $error")
                    stopRecording()
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
            onError?.invoke("Failed to open camera: ${e.message}")
            stopSelf()
        }
    }

    private fun startRecordingSession() {
        val camera = cameraDevice ?: return

        try {
            setupMediaRecorder(videoFile!!)

            val recorderSurface = mediaRecorder?.surface ?: return

            dummyTexture = SurfaceTexture(0).apply {
                setDefaultBufferSize(1920, 1080)
            }
            dummySurface = Surface(dummyTexture)

            val surfaces = listOf(recorderSurface, dummySurface!!)

            camera.createCaptureSession(surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session

                        try {
                            val captureRequestBuilder = camera.createCaptureRequest(
                                CameraDevice.TEMPLATE_RECORD
                            )
                            captureRequestBuilder.addTarget(recorderSurface)
                            captureRequestBuilder.addTarget(dummySurface!!)

                            captureRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                            )
                            captureRequestBuilder.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON
                            )
                            captureRequestBuilder.set(
                                CaptureRequest.CONTROL_AWB_MODE,
                                CaptureRequest.CONTROL_AWB_MODE_AUTO
                            )

                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )

                            backgroundHandler?.postDelayed({
                                try {
                                    mediaRecorder?.start()
                                    isRecording = true

                                    startTimer()
                                    onRecordingStateChanged?.invoke(true)
                                    onStatusChanged?.invoke("🔴 Recording in background...")

                                    // Update notification
                                    val notification = createNotification("Recording: 00:00:00")
                                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    notificationManager.notify(NOTIFICATION_ID, notification)

                                    Log.d(TAG, "Recording started: ${videoFile?.absolutePath}")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error starting MediaRecorder", e)
                                    onError?.invoke("Failed to start recording: ${e.message}")
                                    stopRecording()
                                }
                            }, 500)

                        } catch (e: Exception) {
                            Log.e(TAG, "Error in capture request", e)
                            onError?.invoke("Failed to start recording: ${e.message}")
                            stopRecording()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure camera session")
                        onError?.invoke("Failed to configure camera")
                        stopRecording()
                    }
                },
                backgroundHandler
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error in startRecordingSession", e)
            onError?.invoke("Failed to start recording: ${e.message}")
            stopRecording()
        }
    }

    fun stopRecording() {
        if (!isRecording) {
            return
        }

        Log.d(TAG, "Stopping recording")

        try {
            stopTimer()

            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null

            mediaRecorder?.let {
                try {
                    it.stop()
                    Log.d(TAG, "MediaRecorder stopped successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping MediaRecorder", e)
                }
            }

            releaseMediaRecorder()
            closeCamera()
            releaseSurfaces()

            isRecording = false
            onRecordingStateChanged?.invoke(false)
            onStatusChanged?.invoke("✅ Video saved: ${videoFile?.name}")

            Log.d(TAG, "Recording stopped: ${videoFile?.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            onError?.invoke("Error stopping recording: ${e.message}")
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun setupMediaRecorder(file: File) {
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncodingBitRate(5000000)
                setVideoFrameRate(30)
                setVideoSize(currentOrientation.width, currentOrientation.height)
                setOrientationHint(currentOrientation.rotationHint)
                setOutputFile(file.absolutePath)
                prepare()

                Log.d(TAG, "MediaRecorder prepared with ${currentOrientation.label}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up MediaRecorder", e)
            releaseMediaRecorder()
            throw e
        }
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.let {
            try {
                it.reset()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaRecorder", e)
            }
        }
        mediaRecorder = null
    }

    private fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null
    }

    private fun releaseSurfaces() {
        dummySurface?.release()
        dummySurface = null
        dummyTexture?.release()
        dummyTexture = null
    }

    private fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "VID_${timeStamp}.mp4")
    }

    private fun startTimer() {
        recordingStartTime = System.currentTimeMillis()
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                onRecordingTimeChanged?.invoke(elapsed)

                // Update notification with time
                val hours = elapsed / 3600
                val minutes = (elapsed % 3600) / 60
                val secs = elapsed % 60
                val timeStr = String.format("%02d:%02d:%02d", hours, minutes, secs)

                val notification = createNotification("Recording: $timeStr")
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)

                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video recording status"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Recording")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createPendingIntent())
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                createStopPendingIntent()
            )
            .build()

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, VideoRecorderActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createStopPendingIntent(): PendingIntent {
        val intent = Intent(this, VideoRecorderService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        return PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun isRecording() = isRecording

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        stopBackgroundThread()
        Log.d(TAG, "Service destroyed")
    }
}