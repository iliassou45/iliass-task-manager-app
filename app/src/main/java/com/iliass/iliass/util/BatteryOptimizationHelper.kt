package com.iliass.iliass.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    /**
     * Check if the app is ignoring battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true // Not applicable for older versions
        }
    }

    /**
     * Request to ignore battery optimizations
     * Call this from an Activity
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        }
    }

    /**
     * Show dialog explaining why battery optimization exemption is needed
     */
    fun showBatteryOptimizationDialog(
        context: Context,
        onPositive: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                AlertDialog.Builder(context)
                    .setTitle("Battery Optimization")
                    .setMessage(
                        "To ensure the App Blocker works reliably, please disable battery optimization for this app.\n\n" +
                                "This will allow the service to run continuously in the background."
                    )
                    .setPositiveButton("Open Settings") { dialog, _ ->
                        dialog.dismiss()
                        onPositive()
                    }
                    .setNegativeButton("Later") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    /**
     * Open battery optimization settings page
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // Fallback to app settings
            openAppSettings(context)
        }
    }

    /**
     * Open app settings page
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    /**
     * Get manufacturer-specific battery optimization instructions
     */
    fun getManufacturerSpecificInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                "For Xiaomi/Redmi devices:\n" +
                        "1. Go to Settings > Battery & Performance\n" +
                        "2. Choose App Battery Saver\n" +
                        "3. Find this app and set to 'No restrictions'\n" +
                        "4. Also enable 'Autostart' in Security app"
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "For Huawei/Honor devices:\n" +
                        "1. Go to Settings > Battery\n" +
                        "2. App Launch > Find this app\n" +
                        "3. Disable 'Manage automatically'\n" +
                        "4. Enable all three options"
            }
            manufacturer.contains("oppo") -> {
                "For OPPO devices:\n" +
                        "1. Go to Settings > Battery > App Battery Management\n" +
                        "2. Find this app\n" +
                        "3. Set to 'No restrictions'"
            }
            manufacturer.contains("vivo") -> {
                "For Vivo devices:\n" +
                        "1. Go to Settings > Battery > Background power consumption\n" +
                        "2. Find this app\n" +
                        "3. Allow background running"
            }
            manufacturer.contains("samsung") -> {
                "For Samsung devices:\n" +
                        "1. Go to Settings > Apps > This app\n" +
                        "2. Battery > Optimize battery usage\n" +
                        "3. Select 'All' and turn off for this app\n" +
                        "4. Also check 'Put app to sleep' settings"
            }
            manufacturer.contains("oneplus") -> {
                "For OnePlus devices:\n" +
                        "1. Go to Settings > Battery > Battery Optimization\n" +
                        "2. Find this app\n" +
                        "3. Select 'Don't optimize'"
            }
            else -> {
                "For best performance:\n" +
                        "1. Disable battery optimization for this app\n" +
                        "2. Allow background activity\n" +
                        "3. Check manufacturer-specific battery settings"
            }
        }
    }
}