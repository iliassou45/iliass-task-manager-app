package com.iliass.iliass.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.AlarmTask
import com.iliass.iliass.model.RepeatInterval
import java.util.Calendar

class AlarmTaskManager private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("alarm_tasks", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        @Volatile
        private var instance: AlarmTaskManager? = null

        fun getInstance(context: Context): AlarmTaskManager {
            return instance ?: synchronized(this) {
                instance ?: AlarmTaskManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getAllTasks(): List<AlarmTask> {
        val json = sharedPreferences.getString("tasks", "[]") ?: "[]"
        val type = object : TypeToken<List<AlarmTask>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getTask(id: String): AlarmTask? {
        return getAllTasks().find { it.id == id }
    }

    fun saveTask(task: AlarmTask) {
        val tasks = getAllTasks().toMutableList()
        val existingIndex = tasks.indexOfFirst { it.id == task.id }

        if (existingIndex != -1) {
            tasks[existingIndex] = task
        } else {
            tasks.add(task)
        }

        saveTasks(tasks)

        // Schedule or cancel alarm based on task state
        if (task.isEnabled) {
            scheduleAlarm(task)
        } else {
            cancelAlarm(task)
        }
    }

    fun deleteTask(id: String) {
        val tasks = getAllTasks().toMutableList()
        val task = tasks.find { it.id == id }

        task?.let {
            cancelAlarm(it)
        }

        tasks.removeAll { it.id == id }
        saveTasks(tasks)
    }

    fun toggleTaskEnabled(id: String) {
        val task = getTask(id) ?: return
        val updatedTask = task.copy(
            isEnabled = !task.isEnabled,
            updatedAt = System.currentTimeMillis()
        )
        saveTask(updatedTask)
    }

    private fun saveTasks(tasks: List<AlarmTask>) {
        val json = gson.toJson(tasks)
        sharedPreferences.edit().putString("tasks", json).apply()
    }

    fun scheduleAlarm(task: AlarmTask) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_description", task.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate alarm time
        var alarmTime = task.alarmTime

        // If the alarm time is in the past and it's repeating, calculate next occurrence
        if (alarmTime < System.currentTimeMillis() && task.isRepeating) {
            alarmTime = calculateNextAlarmTime(task)
        }

        // Schedule alarm based on Android version
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12+, check if we can schedule exact alarms
                if (alarmManager.canScheduleExactAlarms()) {
                    scheduleExactAlarm(alarmTime, task.isRepeating, task.repeatInterval, pendingIntent)
                } else {
                    // Fallback to inexact alarm
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                        pendingIntent
                    )
                }
            } else {
                scheduleExactAlarm(alarmTime, task.isRepeating, task.repeatInterval, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun scheduleExactAlarm(
        alarmTime: Long,
        isRepeating: Boolean,
        repeatInterval: RepeatInterval,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isRepeating) {
                val intervalMillis = when (repeatInterval) {
                    RepeatInterval.DAILY -> AlarmManager.INTERVAL_DAY
                    RepeatInterval.WEEKLY -> AlarmManager.INTERVAL_DAY * 7
                    RepeatInterval.MONTHLY -> AlarmManager.INTERVAL_DAY * 30
                    else -> 0L
                }

                if (intervalMillis > 0) {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        intervalMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
            }
        } else {
            if (isRepeating) {
                val intervalMillis = when (repeatInterval) {
                    RepeatInterval.DAILY -> AlarmManager.INTERVAL_DAY
                    RepeatInterval.WEEKLY -> AlarmManager.INTERVAL_DAY * 7
                    RepeatInterval.MONTHLY -> AlarmManager.INTERVAL_DAY * 30
                    else -> 0L
                }

                if (intervalMillis > 0) {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        intervalMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
            }
        }
    }

    private fun calculateNextAlarmTime(task: AlarmTask): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = task.alarmTime
        }

        val now = System.currentTimeMillis()

        while (calendar.timeInMillis < now) {
            when (task.repeatInterval) {
                RepeatInterval.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                RepeatInterval.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatInterval.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                else -> break
            }
        }

        return calendar.timeInMillis
    }

    fun cancelAlarm(task: AlarmTask) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAllAlarms() {
        getAllTasks().forEach { task ->
            if (task.isEnabled) {
                scheduleAlarm(task)
            }
        }
    }
}
