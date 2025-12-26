package com.iliass.iliass

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import android.os.Build

class FlightModeRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("FlightModePrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTO_MODE = "auto_mode_enabled"
        private const val TAG = "FlightModeRepository"
    }

    fun isFlightModeOn(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking flight mode", e)
            false
        }
    }

    fun toggleFlightMode(context: Context): Boolean {
        return try {
            // Check if we have permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(context)) {
                    Log.e(TAG, "No permission to write settings")
                    return false
                }
            }

            // Get current state
            val currentState = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            )

            // Toggle the state
            val newState = if (currentState == 0) 1 else 0

            // Try to set the new state
            val success = try {
                Settings.Global.putInt(
                    context.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    newState
                )
                true
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Cannot modify system settings", e)
                false
            }

            if (success) {
                // Broadcast the change
                val broadcastIntent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                broadcastIntent.putExtra("state", newState == 1)
                context.sendBroadcast(broadcastIntent)

                Log.d(TAG, "Flight mode toggled to: ${if (newState == 1) "ON" else "OFF"}")
                return true
            }

            return false

        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flight mode: ${e.message}", e)
            return false
        }
    }

    fun setAutoModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_MODE, enabled).apply()
    }

    fun isAutoModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_MODE, false)
    }

    fun openFlightModeSettings(context: Context) {
        try {
            // Try to open wireless settings (where flight mode toggle is)
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened wireless settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings", e)
            // Fallback to general settings
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening fallback settings", e2)
            }
        }
    }

    fun cancelSchedule(context: Context) {
        Log.d(TAG, "Schedule cancelled")
    }
}