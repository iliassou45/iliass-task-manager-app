package com.iliass.iliass.util

import com.iliass.iliass.AppBlockerService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.iliass.iliass.BlockedApp

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            Log.d(TAG, "Boot completed or app updated - checking if service should start")

            // Check if there are any blocked apps
            val prefs = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("blocked_apps_list", null)

            if (!json.isNullOrEmpty()) {
                try {
                    // Parse blocked apps
                    val type = object : com.google.gson.reflect.TypeToken<List<BlockedApp>>() {}.type
                    val blockedApps: List<BlockedApp> = com.google.gson.Gson().fromJson(json, type)

                    // Only start service if there are active, non-expired blocked apps
                    val hasActiveBlocks = blockedApps.any { it.isActive && !it.isExpired() }

                    if (hasActiveBlocks) {
                        Log.d(TAG, "Starting AppBlockerService - found active blocks")

                        val serviceIntent = Intent(context, AppBlockerService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } else {
                        Log.d(TAG, "No active blocks found - service not started")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking blocked apps", e)
                }
            } else {
                Log.d(TAG, "No blocked apps configured")
            }

            // Reschedule all alarm tasks
            try {
                Log.d(TAG, "Rescheduling alarm tasks")
                val alarmTaskManager = AlarmTaskManager.getInstance(context)
                alarmTaskManager.rescheduleAllAlarms()
                Log.d(TAG, "Alarm tasks rescheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarm tasks", e)
            }
        }
    }
}