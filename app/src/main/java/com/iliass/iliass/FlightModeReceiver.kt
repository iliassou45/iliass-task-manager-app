package com.iliass.iliass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

class FlightModeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "FlightModeReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            Log.d(TAG, "Attempting to toggle flight mode")
            toggleFlightMode(it)
        }
    }

    private fun toggleFlightMode(context: Context) {
        try {
            // Check if we have permission
            if (!Settings.System.canWrite(context)) {
                Log.e(TAG, "No permission to write settings")
                return
            }

            // Get current state
            val currentState = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            )

            // Toggle the state
            val newState = if (currentState == 0) 1 else 0

            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                newState
            )

            // Broadcast the change
            val broadcastIntent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            broadcastIntent.putExtra("state", newState == 1)
            context.sendBroadcast(broadcastIntent)

            Log.d(TAG, "Flight mode toggled to: ${if (newState == 1) "ON" else "OFF"}")

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot modify system settings", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling flight mode", e)
        }
    }
}