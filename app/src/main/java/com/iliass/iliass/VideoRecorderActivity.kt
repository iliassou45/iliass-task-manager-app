package com.iliass.iliass

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider

class VideoRecorderActivity : AppCompatActivity() {

    private lateinit var viewModel: VideoRecorderViewModel
    private lateinit var backButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button
    private lateinit var timerText: TextView
    private lateinit var recordingIndicator: TextView
    private lateinit var portraitButton: Button
    private lateinit var landscapeButton: Button

    companion object {
        private const val REQUEST_PERMISSIONS = 100

        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_recorder)

        viewModel = ViewModelProvider(this)[VideoRecorderViewModel::class.java]

        initViews()
        observeViewModel()
        setupListeners()
        checkPermissions()

        // Debug: Log supported camera sizes
        CameraDebugHelper.logSupportedVideoSizes(this)
    }

    override fun onStart() {
        super.onStart()
        viewModel.bindService(this)
    }

    override fun onStop() {
        super.onStop()
        // Don't unbind if recording - let service continue
        if (viewModel.isRecording.value != true) {
            viewModel.unbindService(this)
        }
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        statusText = findViewById(R.id.statusText)
        recordButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        timerText = findViewById(R.id.timerText)
        recordingIndicator = findViewById(R.id.recordingIndicator)
        portraitButton = findViewById(R.id.portraitButton)
        landscapeButton = findViewById(R.id.landscapeButton)
    }

    private fun observeViewModel() {
        viewModel.isRecording.observe(this) { recording ->
            updateUI(recording)
        }

        viewModel.recordingTime.observe(this) { time ->
            timerText.text = formatTime(time)
        }

        viewModel.statusMessage.observe(this) { message ->
            statusText.text = message
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.selectedOrientation.observe(this) { orientation ->
            updateOrientationButtons(orientation)
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            if (viewModel.isRecording.value == true) {
                Toast.makeText(
                    this,
                    "Recording in progress. Stop recording before closing.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                finish()
            }
        }

        recordButton.setOnClickListener {
            if (allPermissionsGranted()) {
                viewModel.startRecording(this)
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            viewModel.stopRecording(this)
        }

        portraitButton.setOnClickListener {
            viewModel.setOrientation(VideoRecorderService.VideoOrientation.PORTRAIT)
        }

        landscapeButton.setOnClickListener {
            viewModel.setOrientation(VideoRecorderService.VideoOrientation.LANDSCAPE)
        }
    }

    private fun updateUI(isRecording: Boolean) {
        if (isRecording) {
            recordButton.isEnabled = false
            stopButton.isEnabled = true
            timerText.visibility = TextView.VISIBLE
            recordingIndicator.visibility = TextView.VISIBLE
            portraitButton.isEnabled = false
            landscapeButton.isEnabled = false
        } else {
            recordButton.isEnabled = true
            stopButton.isEnabled = false
            timerText.visibility = TextView.GONE
            recordingIndicator.visibility = TextView.GONE
            portraitButton.isEnabled = true
            landscapeButton.isEnabled = true
        }
    }

    private fun updateOrientationButtons(orientation: VideoRecorderService.VideoOrientation) {
        when (orientation) {
            VideoRecorderService.VideoOrientation.PORTRAIT -> {
                portraitButton.alpha = 1.0f
                landscapeButton.alpha = 0.5f
            }
            VideoRecorderService.VideoOrientation.LANDSCAPE -> {
                landscapeButton.alpha = 1.0f
                portraitButton.alpha = 0.5f
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun checkPermissions() {
        if (allPermissionsGranted()) {
            statusText.text = "✅ Ready to record"
        } else {
            statusText.text = "⚠️ Permissions required"
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (allPermissionsGranted()) {
                statusText.text = "✅ Ready to record"
                Toast.makeText(
                    this,
                    "Permissions granted! Tap record to start.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted. Cannot record video.",
                    Toast.LENGTH_LONG
                ).show()
                statusText.text = "❌ Permissions denied"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unbind service when activity is destroyed
        viewModel.unbindService(this)
    }

    override fun onBackPressed() {
        if (viewModel.isRecording.value == true) {
            Toast.makeText(
                this,
                "Recording continues in background. Use notification to stop.",
                Toast.LENGTH_LONG
            ).show()
            moveTaskToBack(true)
        } else {
            super.onBackPressed()
        }
    }
}