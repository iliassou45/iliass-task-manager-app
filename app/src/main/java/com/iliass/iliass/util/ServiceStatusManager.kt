package com.iliass.iliass.util


import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.iliass.iliass.AppBlockerService
import com.iliass.iliass.BlockedApp

object ServiceStatusManager {

    private const val TAG = "ServiceStatusManager"

    /**
     * Check if AppBlockerService is currently running
     */
    fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AppBlockerService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    /**
     * Start the AppBlockerService
     */
    fun startService(context: Context): Boolean {
        return try {
            val intent = Intent(context, AppBlockerService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d(TAG, "Service start requested")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
            false
        }
    }

    /**
     * Stop the AppBlockerService
     */
    fun stopService(context: Context): Boolean {
        return try {
            val intent = Intent(context, AppBlockerService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Service stop requested")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop service", e)
            false
        }
    }

    /**
     * Restart the AppBlockerService
     */
    fun restartService(context: Context): Boolean {
        stopService(context)
        Thread.sleep(500) // Brief delay
        return startService(context)
    }

    /**
     * Get comprehensive service status information
     */
    fun getServiceStatus(context: Context): ServiceStatus {
        val isRunning = isServiceRunning(context)
        val isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        val hasBlockedApps = getBlockedAppsCount(context) > 0

        return ServiceStatus(
            isRunning = isRunning,
            isBatteryOptimized = isBatteryOptimized,
            hasBlockedApps = hasBlockedApps,
            manufacturer = Build.MANUFACTURER
        )
    }

    /**
     * Get count of blocked apps
     */
    private fun getBlockedAppsCount(context: Context): Int {
        return try {
            val prefs = context.getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("blocked_apps_list", null) ?: return 0
            val type = object : com.google.gson.reflect.TypeToken<List<BlockedApp>>() {}.type
            val apps: List<BlockedApp> = com.google.gson.Gson().fromJson(json, type)
            apps.count { it.isActive && !it.isExpired() }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get formatted status message
     */
    fun getStatusMessage(context: Context): String {
        val status = getServiceStatus(context)

        return buildString {
            appendLine("Service Status:")
            appendLine("━━━━━━━━━━━━━━━━")
            appendLine()

            appendLine("🔒 Service: ${if (status.isRunning) "✓ Running" else "✗ Stopped"}")
            appendLine("🔋 Battery: ${if (status.isBatteryOptimized) "⚠ Optimized" else "✓ Unrestricted"}")
            appendLine("📱 Blocked Apps: ${if (status.hasBlockedApps) "✓ Active" else "None"}")
            appendLine("🏭 Device: ${status.manufacturer}")

            appendLine()

            if (!status.isRunning) {
                appendLine("⚠ Warning: Service is not running!")
                appendLine("Apps will not be blocked.")
            } else if (status.isBatteryOptimized) {
                appendLine("⚠ Warning: Battery optimization is enabled!")
                appendLine("Service may be killed by the system.")
            } else if (!status.hasBlockedApps) {
                appendLine("ℹ Info: No apps are currently blocked.")
            } else {
                appendLine("✓ Everything is working correctly!")
            }
        }
    }

    data class ServiceStatus(
        val isRunning: Boolean,
        val isBatteryOptimized: Boolean,
        val hasBlockedApps: Boolean,
        val manufacturer: String
    )
}