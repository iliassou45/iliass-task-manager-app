package com.iliass.iliass.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.iliass.iliass.R
import com.iliass.iliass.TaskManagerActivity

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "task_reminder_channel"
        private const val CHANNEL_NAME = "Task Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val taskTitle = intent.getStringExtra("task_title") ?: "Task Reminder"
        val taskDescription = intent.getStringExtra("task_description") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your daily tasks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open task manager
        val openIntent = Intent(context, TaskManagerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("highlight_task_id", taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark as done action
        val doneIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = "ACTION_COMPLETE_TASK"
            putExtra("task_id", taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action (reschedule for 15 minutes later)
        val snoozeIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE_TASK"
            putExtra("task_id", taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📋 $taskTitle")
            .setContentText(if (taskDescription.isNotEmpty()) taskDescription else "Time to work on this task!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(if (taskDescription.isNotEmpty()) taskDescription else "Time to work on this task!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "Done ✓", donePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}
