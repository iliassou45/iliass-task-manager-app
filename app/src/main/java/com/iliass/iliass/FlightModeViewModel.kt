package com.iliass.iliass

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FlightModeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FlightModeRepository(application)

    private val _flightModeStatus = MutableLiveData<String>()
    val flightModeStatus: LiveData<String> = _flightModeStatus

    private val _isAutoModeEnabled = MutableLiveData<Boolean>()
    val isAutoModeEnabled: LiveData<Boolean> = _isAutoModeEnabled

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _countdownSeconds = MutableLiveData<Int>()
    val countdownSeconds: LiveData<Int> = _countdownSeconds

    private var countDownTimer: CountDownTimer? = null

    companion object {
        private const val TAG = "FlightModeViewModel"
        private const val COUNTDOWN_TIME_MS = 10000L // 10 seconds
    }

    init {
        updateStatus()
        _isAutoModeEnabled.value = repository.isAutoModeEnabled()
        _countdownSeconds.value = 0
    }

    fun toggleAutoMode(enabled: Boolean, context: Context) {
        if (enabled) {
            if (!Settings.System.canWrite(context)) {
                _errorMessage.value = "Permission required. Opening settings..."
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                _isAutoModeEnabled.value = false
                return
            }
        }

        repository.setAutoModeEnabled(enabled)
        _isAutoModeEnabled.value = enabled

        if (enabled) {
            _flightModeStatus.value = "Auto mode enabled"
        } else {
            cancelCountdown()
            repository.cancelSchedule(context)
            _flightModeStatus.value = "Auto mode disabled"
        }
    }

    fun scheduleFlightMode(context: Context) {
        if (!Settings.System.canWrite(context)) {
            _errorMessage.value = "Write settings permission required"
            return
        }

        if (_isAutoModeEnabled.value != true) {
            _errorMessage.value = "Please enable auto mode first"
            return
        }

        // Start countdown
        startCountdown(context)
        _flightModeStatus.value = "Flight mode scheduled..."
    }

    private fun startCountdown(context: Context) {
        cancelCountdown() // Cancel any existing countdown

        countDownTimer = object : CountDownTimer(COUNTDOWN_TIME_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt() + 1
                _countdownSeconds.value = secondsLeft
                Log.d(TAG, "Countdown: $secondsLeft seconds")
            }

            override fun onFinish() {
                _countdownSeconds.value = 0
                Log.d(TAG, "Countdown finished, opening flight mode settings")

                // Open flight mode settings for user to toggle manually
                repository.openFlightModeSettings(context)
                _flightModeStatus.value = "Please toggle flight mode manually"
                _errorMessage.value = "Opening flight mode settings..."
            }
        }.start()
    }

    fun cancelCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
        _countdownSeconds.value = 0
    }

    private fun updateStatus() {
        val isEnabled = repository.isFlightModeOn()
        _flightModeStatus.value = if (isEnabled) {
            "Flight Mode: ON ✈️"
        } else {
            "Flight Mode: OFF"
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelCountdown()
    }
}