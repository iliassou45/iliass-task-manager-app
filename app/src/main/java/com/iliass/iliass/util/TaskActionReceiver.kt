package com.iliass.iliass.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.iliass.iliass.model.TaskStatus

class TaskActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val taskManager = TaskManager.getInstance(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            "ACTION_COMPLETE_TASK" -> {
                taskManager.completeTask(taskId)
                notificationManager.cancel(taskId.hashCode())
                Toast.makeText(context, "✅ Task completed!", Toast.LENGTH_SHORT).show()
            }
            "ACTION_SNOOZE_TASK" -> {
                val task = taskManager.getTask(taskId)
                if (task != null) {
                    // Snooze for 15 minutes
                    val newReminderTime = System.currentTimeMillis() + (15 * 60 * 1000)
                    val updatedTask = task.copy(
                        reminderTime = newReminderTime,
                        reminderEnabled = true
                    )
                    taskManager.saveTask(updatedTask)
                    notificationManager.cancel(taskId.hashCode())
                    Toast.makeText(context, "⏰ Snoozed for 15 minutes", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
