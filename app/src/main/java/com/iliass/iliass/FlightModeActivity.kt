package com.iliass.iliass

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

class FlightModeActivity : AppCompatActivity() {

    private lateinit var viewModel: FlightModeViewModel
    private lateinit var backButton: ImageButton
    private lateinit var statusText: TextView
    private lateinit var enableSwitch: Switch
    private lateinit var scheduleButton: Button
    private lateinit var countdownText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flight_mode)

        viewModel = ViewModelProvider(this)[FlightModeViewModel::class.java]

        initViews()
        observeViewModel()
        setupListeners()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        statusText = findViewById(R.id.statusText)
        enableSwitch = findViewById(R.id.enableSwitch)
        scheduleButton = findViewById(R.id.scheduleButton)
        countdownText = findViewById(R.id.countdownText)
    }

    private fun observeViewModel() {
        viewModel.flightModeStatus.observe(this) { status ->
            statusText.text = status
        }

        viewModel.isAutoModeEnabled.observe(this) { enabled ->
            enableSwitch.isChecked = enabled
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.countdownSeconds.observe(this) { seconds ->
            if (seconds > 0) {
                countdownText.text = "Activating in: ${seconds}s"
                countdownText.visibility = TextView.VISIBLE
                scheduleButton.isEnabled = false
            } else {
                countdownText.visibility = TextView.GONE
                scheduleButton.isEnabled = true
            }
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleAutoMode(isChecked, this)
        }

        scheduleButton.setOnClickListener {
            viewModel.scheduleFlightMode(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cancelCountdown()
    }
}