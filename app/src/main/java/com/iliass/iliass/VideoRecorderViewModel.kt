package com.iliass.iliass

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class VideoRecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val _isRecording = MutableLiveData<Boolean>()
    val isRecording: LiveData<Boolean> = _isRecording

    private val _recordingTime = MutableLiveData<Int>()
    val recordingTime: LiveData<Int> = _recordingTime

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedOrientation = MutableLiveData<VideoRecorderService.VideoOrientation>()
    val selectedOrientation: LiveData<VideoRecorderService.VideoOrientation> = _selectedOrientation

    private var recorderService: VideoRecorderService? = null
    private var serviceBound = false

    companion object {
        private const val TAG = "VideoRecorderViewModel"
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VideoRecorderService.LocalBinder
            recorderService = binder.getService()
            serviceBound = true

            // Set up callbacks
            recorderService?.onRecordingStateChanged = { recording ->
                _isRecording.postValue(recording)
            }

            recorderService?.onRecordingTimeChanged = { time ->
                _recordingTime.postValue(time)
            }

            recorderService?.onError = { error ->
                _errorMessage.postValue(error)
            }

            recorderService?.onStatusChanged = { status ->
                _statusMessage.postValue(status)
            }

            Log.d(TAG, "Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recorderService = null
            serviceBound = false
            Log.d(TAG, "Service disconnected")
        }
    }

    init {
        _isRecording.value = false
        _recordingTime.value = 0
        _statusMessage.value = "Ready to record"
        _selectedOrientation.value = VideoRecorderService.VideoOrientation.LANDSCAPE
    }

    fun bindService(context: Context) {
        if (!serviceBound) {
            val intent = Intent(context, VideoRecorderService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService(context: Context) {
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }

    fun setOrientation(orientation: VideoRecorderService.VideoOrientation) {
        if (_isRecording.value == true) {
            _errorMessage.value = "Cannot change orientation while recording"
            return
        }
        _selectedOrientation.value = orientation
        recorderService?.setOrientation(orientation)
        _statusMessage.value = "Orientation set to ${orientation.label}"
    }

    fun startRecording(context: Context) {
        if (_isRecording.value == true) {
            _errorMessage.value = "Already recording"
            return
        }

        val intent = Intent(context, VideoRecorderService::class.java).apply {
            action = VideoRecorderService.ACTION_START_RECORDING
            putExtra(
                VideoRecorderService.EXTRA_ORIENTATION,
                _selectedOrientation.value?.name ?: "LANDSCAPE"
            )
        }

        context.startForegroundService(intent)

        // Bind to service if not already bound
        if (!serviceBound) {
            bindService(context)
        }
    }

    fun stopRecording(context: Context) {
        if (_isRecording.value != true) {
            return
        }

        val intent = Intent(context, VideoRecorderService::class.java).apply {
            action = VideoRecorderService.ACTION_STOP_RECORDING
        }
        context.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}