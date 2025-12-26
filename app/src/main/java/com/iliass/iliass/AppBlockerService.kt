package com.iliass.iliass

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppBlockerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 500L
    private var lastBlockedPackage = ""
    private var lastBlockTime = 0L
    private val TAG = "AppBlockerService"
    private val CHANNEL_ID = "app_blocker_channel"
    private val NOTIFICATION_ID = 1001
    private var wakeLock: PowerManager.WakeLock? = null

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkAndBlockApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Restart the monitoring if it was stopped
        handler.removeCallbacks(monitorRunnable)
        handler.post(monitorRunnable)

        // Request to ignore battery optimizations
        requestIgnoreBatteryOptimizations()

        // Return START_STICKY to restart service if killed
        return START_STICKY
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)
        Log.d(TAG, "Task removed - restarting service")

        // Restart the service when task is removed
        val restartServiceIntent = Intent(applicationContext, this::class.java)
        restartServiceIntent.setPackage(packageName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent)
        } else {
            startService(restartServiceIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
        releaseWakeLock()
        Log.d(TAG, "Service destroyed - scheduling restart")

        // Schedule service restart
        val restartServiceIntent = Intent(applicationContext, this::class.java)
        restartServiceIntent.setPackage(packageName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent)
        } else {
            startService(restartServiceIntent)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AppBlocker::ServiceWakeLock"
            ).apply {
                acquire(10 * 60 * 60 * 1000L) // 10 hours
            }
            Log.d(TAG, "WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                val packageName = packageName

                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    Log.d(TAG, "App is not ignoring battery optimizations")
                    // Note: Actual request should be done in an Activity, not Service
                    // This is just for logging purposes
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization status", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocker Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors and blocks selected apps"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Create intent to open the app when notification is tapped
        val notificationIntent = Intent(this, AppBlockerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Blocker Active")
            .setContentText("Protecting your focus")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun checkAndBlockApp() {
        try {
            val currentPackage = getCurrentForegroundApp() ?: return

            // Skip if it's this app or system UI
            if (currentPackage == packageName ||
                currentPackage.startsWith("com.android.systemui") ||
                currentPackage == "com.android.launcher") {
                return
            }

            // Get blocked apps
            val blockedApps = getBlockedApps()

            // Find if current app is blocked
            val blockedApp = blockedApps.find {
                it.packageName == currentPackage &&
                        it.isActive &&
                        !it.isExpired()
            }

            if (blockedApp != null) {
                // Prevent rapid re-blocking
                val currentTime = System.currentTimeMillis()
                if (currentPackage == lastBlockedPackage &&
                    currentTime - lastBlockTime < 2000) {
                    return
                }

                lastBlockedPackage = currentPackage
                lastBlockTime = currentTime

                Log.d(TAG, "Blocking: ${blockedApp.appName}")
                showBlockScreen(blockedApp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in check", e)
        }
    }

    private fun getCurrentForegroundApp(): String? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()

            val events = usageStatsManager.queryEvents(currentTime - 2000, currentTime)

            var foregroundApp: String? = null
            var latestTime = 0L

            while (events.hasNextEvent()) {
                val event = UsageEvents.Event()
                events.getNextEvent(event)

                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if (event.timeStamp > latestTime) {
                        foregroundApp = event.packageName
                        latestTime = event.timeStamp
                    }
                }
            }

            foregroundApp
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            null
        }
    }

    private fun getBlockedApps(): List<BlockedApp> {
        return try {
            val prefs = getSharedPreferences("app_blocker_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("blocked_apps_list", null) ?: return emptyList()
            val type = object : TypeToken<List<BlockedApp>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading blocked apps", e)
            emptyList()
        }
    }

    private fun showBlockScreen(blockedApp: BlockedApp) {
        try {
            val intent = Intent(this, BlockedAppScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                putExtra("app_name", blockedApp.appName)
                putExtra("remaining_time", blockedApp.getRemainingTime())
                putExtra("package_name", blockedApp.packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing block screen", e)
        }
    }
}