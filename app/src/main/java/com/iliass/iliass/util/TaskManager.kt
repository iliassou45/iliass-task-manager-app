package com.iliass.iliass.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.*
import java.util.*

class TaskManager private constructor(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("daily_tasks", Context.MODE_PRIVATE)
    private val analyticsPreferences: SharedPreferences =
        context.getSharedPreferences("task_analytics", Context.MODE_PRIVATE)
    private val streakPreferences: SharedPreferences =
        context.getSharedPreferences("task_streaks", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        @Volatile
        private var instance: TaskManager? = null

        fun getInstance(context: Context): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager(context.applicationContext).also { instance = it }
            }
        }

        private const val TASKS_KEY = "tasks"
        private const val DAILY_STATS_KEY = "daily_stats"
        private const val STREAK_KEY = "current_streak"
        private const val LONGEST_STREAK_KEY = "longest_streak"
        private const val LAST_COMPLETION_DATE_KEY = "last_completion_date"
    }

    // ==================== CRUD Operations ====================

    fun getAllTasks(): List<DailyTask> {
        val json = sharedPreferences.getString(TASKS_KEY, "[]") ?: "[]"
        val type = object : TypeToken<List<DailyTask>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getTask(id: String): DailyTask? {
        return getAllTasks().find { it.id == id }
    }

    fun saveTask(task: DailyTask): DailyTask {
        val tasks = getAllTasks().toMutableList()
        val existingIndex = tasks.indexOfFirst { it.id == task.id }
        val updatedTask = task.copy(updatedAt = System.currentTimeMillis())

        if (existingIndex != -1) {
            tasks[existingIndex] = updatedTask
        } else {
            tasks.add(updatedTask)
        }

        saveTasks(tasks)

        // Schedule reminder if enabled
        if (updatedTask.reminderEnabled && updatedTask.reminderTime != null) {
            scheduleReminder(updatedTask)
        }

        return updatedTask
    }

    fun deleteTask(id: String) {
        val tasks = getAllTasks().toMutableList()
        val task = tasks.find { it.id == id }
        task?.let { cancelReminder(it) }
        tasks.removeAll { it.id == id }
        saveTasks(tasks)
    }

    private fun saveTasks(tasks: List<DailyTask>) {
        val json = gson.toJson(tasks)
        sharedPreferences.edit().putString(TASKS_KEY, json).apply()
    }

    // ==================== Task Status Operations ====================

    fun completeTask(id: String, actualMinutes: Int? = null): DailyTask? {
        val task = getTask(id) ?: return null
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            actualMinutes = actualMinutes,
            updatedAt = System.currentTimeMillis()
        )
        saveTask(completedTask)
        updateStreakOnCompletion()
        recordDailyStats(completedTask)
        return completedTask
    }

    fun startTask(id: String): DailyTask? {
        val task = getTask(id) ?: return null
        val startedTask = task.copy(
            status = TaskStatus.IN_PROGRESS,
            updatedAt = System.currentTimeMillis()
        )
        return saveTask(startedTask)
    }

    fun cancelTask(id: String): DailyTask? {
        val task = getTask(id) ?: return null
        val cancelledTask = task.copy(
            status = TaskStatus.CANCELLED,
            updatedAt = System.currentTimeMillis()
        )
        return saveTask(cancelledTask)
    }

    fun toggleSubtask(taskId: String, subtaskId: String): DailyTask? {
        val task = getTask(taskId) ?: return null
        val updatedSubtasks = task.subtasks.map { subtask ->
            if (subtask.id == subtaskId) {
                subtask.copy(
                    isCompleted = !subtask.isCompleted,
                    completedAt = if (!subtask.isCompleted) System.currentTimeMillis() else null
                )
            } else subtask
        }

        // Check if all subtasks completed
        val allCompleted = updatedSubtasks.all { it.isCompleted }
        val newStatus = if (allCompleted && updatedSubtasks.isNotEmpty()) TaskStatus.COMPLETED else task.status

        val updatedTask = task.copy(
            subtasks = updatedSubtasks,
            status = newStatus,
            completedAt = if (newStatus == TaskStatus.COMPLETED && task.completedAt == null) System.currentTimeMillis() else task.completedAt
        )
        return saveTask(updatedTask)
    }

    // ==================== Task Queries ====================

    fun getTodayTasks(): List<DailyTask> {
        val today = getStartOfDay(System.currentTimeMillis())
        val tomorrow = today + 24 * 60 * 60 * 1000
        return getAllTasks().filter { task ->
            task.dueDate >= today && task.dueDate < tomorrow
        }.sortedWith(compareBy(
            { it.status != TaskStatus.PENDING && it.status != TaskStatus.IN_PROGRESS },
            { -it.priority.ordinal },
            { it.startTime ?: Long.MAX_VALUE } // Sort by scheduled start time first
        ))
    }

    fun getUpcomingTasks(days: Int = 7): List<DailyTask> {
        val today = getStartOfDay(System.currentTimeMillis())
        val endDate = today + days * 24 * 60 * 60 * 1000
        return getAllTasks().filter { task ->
            task.dueDate >= today && task.dueDate < endDate &&
            task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
        }.sortedBy { it.dueDate }
    }

    fun getOverdueTasks(): List<DailyTask> {
        val now = System.currentTimeMillis()
        return getAllTasks().filter { task ->
            task.dueDate < getStartOfDay(now) &&
            task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED
        }.sortedBy { it.dueDate }
    }

    fun getTasksByCategory(category: TaskCategory): List<DailyTask> {
        return getAllTasks().filter { it.category == category }
    }

    fun getTasksByStatus(status: TaskStatus): List<DailyTask> {
        return getAllTasks().filter { it.status == status }
    }

    fun getCompletedTasksInRange(startDate: Long, endDate: Long): List<DailyTask> {
        return getAllTasks().filter { task ->
            task.completedAt != null && task.completedAt >= startDate && task.completedAt < endDate
        }
    }

    // ==================== Time Interval & Conflict Detection ====================

    /**
     * Check if a proposed time interval conflicts with existing tasks on the same date
     * @param date The date to check
     * @param startTime The proposed start time
     * @param endTime The proposed end time
     * @param excludeTaskId Optional task ID to exclude (for editing existing tasks)
     * @return List of conflicting tasks, empty if no conflicts
     */
    fun checkTimeConflicts(
        date: Long,
        startTime: Long,
        endTime: Long,
        excludeTaskId: String? = null
    ): List<TimeConflict> {
        val normalizedDate = getStartOfDay(date)
        val conflicts = mutableListOf<TimeConflict>()

        getAllTasks()
            .filter { task ->
                task.id != excludeTaskId &&
                task.hasTimeInterval &&
                task.startTime != null &&
                task.endTime != null &&
                getStartOfDay(task.dueDate) == normalizedDate &&
                task.status != TaskStatus.CANCELLED &&
                task.status != TaskStatus.COMPLETED
            }
            .forEach { task ->
                if (task.overlapsWithTime(startTime, endTime, normalizedDate)) {
                    val startCal = Calendar.getInstance().apply { timeInMillis = task.startTime!! }
                    val endCal = Calendar.getInstance().apply { timeInMillis = task.endTime!! }
                    val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

                    conflicts.add(TimeConflict(
                        conflictingTask = task,
                        message = "Conflicts with \"${task.title}\" (${timeFormat.format(startCal.time)} - ${timeFormat.format(endCal.time)})"
                    ))
                }
            }

        return conflicts
    }

    /**
     * Get all tasks with time intervals for a specific date, sorted by start time
     */
    fun getScheduledTasksForDate(date: Long): List<DailyTask> {
        val normalizedDate = getStartOfDay(date)
        return getAllTasks()
            .filter { task ->
                task.hasTimeInterval &&
                task.startTime != null &&
                getStartOfDay(task.dueDate) == normalizedDate &&
                task.status != TaskStatus.CANCELLED
            }
            .sortedBy { it.startTime }
    }

    /**
     * Get available time slots for a given date
     * @param date The date to check
     * @param durationMinutes Required duration in minutes
     * @param dayStartHour Start of working hours (default 8 AM)
     * @param dayEndHour End of working hours (default 22 = 10 PM)
     * @return List of available (startTime, endTime) pairs
     */
    fun getAvailableTimeSlots(
        date: Long,
        durationMinutes: Int,
        dayStartHour: Int = 8,
        dayEndHour: Int = 22
    ): List<Pair<Long, Long>> {
        val normalizedDate = getStartOfDay(date)
        val scheduledTasks = getScheduledTasksForDate(date)

        val slots = mutableListOf<Pair<Long, Long>>()
        val calendar = Calendar.getInstance().apply { timeInMillis = normalizedDate }

        // Set day start
        calendar.set(Calendar.HOUR_OF_DAY, dayStartHour)
        calendar.set(Calendar.MINUTE, 0)
        var currentStart = calendar.timeInMillis

        // Set day end
        calendar.set(Calendar.HOUR_OF_DAY, dayEndHour)
        val dayEnd = calendar.timeInMillis

        val requiredDuration = durationMinutes * 60 * 1000L

        for (task in scheduledTasks) {
            val taskStart = task.startTime!!
            val taskEnd = task.endTime!!

            // Check gap before this task
            if (taskStart > currentStart && taskStart - currentStart >= requiredDuration) {
                slots.add(Pair(currentStart, taskStart))
            }

            // Move current start to after this task
            if (taskEnd > currentStart) {
                currentStart = taskEnd
            }
        }

        // Check remaining time after last task
        if (dayEnd > currentStart && dayEnd - currentStart >= requiredDuration) {
            slots.add(Pair(currentStart, dayEnd))
        }

        return slots
    }

    // ==================== Analytics ====================

    fun calculateAnalytics(): TaskAnalytics {
        val allTasks = getAllTasks()
        val completedTasks = allTasks.filter { it.status == TaskStatus.COMPLETED }
        val cancelledTasks = allTasks.filter { it.status == TaskStatus.CANCELLED }
        val overdueTasks = getOverdueTasks()

        val completionRate = if (allTasks.isNotEmpty()) {
            (completedTasks.size.toFloat() / allTasks.size) * 100
        } else 0f

        // On-time completion rate
        val onTimeCompletions = completedTasks.count { task ->
            task.completedAt != null && task.completedAt <= task.dueDate + (24 * 60 * 60 * 1000)
        }
        val onTimeRate = if (completedTasks.isNotEmpty()) {
            (onTimeCompletions.toFloat() / completedTasks.size) * 100
        } else 0f

        // Average completion time
        val avgCompletionTime = completedTasks
            .mapNotNull { it.actualMinutes }
            .average()
            .toFloat()
            .takeIf { !it.isNaN() } ?: 0f

        // Productivity by hour
        val hourCounts = completedTasks
            .mapNotNull { it.completedAt }
            .map { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) }
            .groupingBy { it }
            .eachCount()
        val mostProductiveHour = hourCounts.maxByOrNull { it.value }?.key ?: -1

        // Productivity by day of week
        val dayCounts = completedTasks
            .mapNotNull { it.completedAt }
            .map { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_WEEK) }
            .groupingBy { it }
            .eachCount()
        val mostProductiveDay = dayCounts.maxByOrNull { it.value }?.key ?: -1

        // Weekly completion
        val weeklyCompletion = MutableList(7) { 0 }
        completedTasks.forEach { task ->
            task.completedAt?.let { timestamp ->
                val dayOfWeek = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_WEEK) - 1
                if (dayOfWeek in 0..6) weeklyCompletion[dayOfWeek]++
            }
        }

        // Category breakdown
        val categoryBreakdown = completedTasks.groupingBy { it.category }.eachCount()

        // Priority breakdown
        val priorityBreakdown = completedTasks.groupingBy { it.priority }.eachCount()

        // Current and longest streak
        val currentStreak = getCurrentStreak()
        val longestStreak = getLongestStreak()

        // Estimation accuracy
        val tasksWithEstimates = completedTasks.filter { it.actualMinutes != null && it.estimatedMinutes > 0 }
        val estimationAccuracy = if (tasksWithEstimates.isNotEmpty()) {
            val accuracies = tasksWithEstimates.map { task ->
                val diff = kotlin.math.abs(task.estimatedMinutes - (task.actualMinutes ?: 0))
                val maxVal = maxOf(task.estimatedMinutes, task.actualMinutes ?: 0)
                if (maxVal > 0) (1 - (diff.toFloat() / maxVal)) * 100 else 100f
            }
            accuracies.average().toFloat()
        } else 0f

        // Average tasks per day (last 30 days)
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val recentCompletions = completedTasks.count { (it.completedAt ?: 0) >= thirtyDaysAgo }
        val avgTasksPerDay = recentCompletions / 30f

        // Productivity score (weighted composite)
        val productivityScore = calculateProductivityScore(
            completionRate, onTimeRate, currentStreak, avgTasksPerDay, estimationAccuracy
        )

        return TaskAnalytics(
            totalTasksCreated = allTasks.size,
            totalTasksCompleted = completedTasks.size,
            totalTasksCancelled = cancelledTasks.size,
            totalTasksOverdue = overdueTasks.size,
            completionRate = completionRate,
            onTimeCompletionRate = onTimeRate,
            averageCompletionTimeMinutes = avgCompletionTime,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            productivityScore = productivityScore,
            mostProductiveHour = mostProductiveHour,
            mostProductiveDay = mostProductiveDay,
            categoryBreakdown = categoryBreakdown,
            priorityBreakdown = priorityBreakdown,
            weeklyCompletion = weeklyCompletion,
            averageTasksPerDay = avgTasksPerDay,
            estimationAccuracy = estimationAccuracy,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun calculateProductivityScore(
        completionRate: Float,
        onTimeRate: Float,
        streak: Int,
        avgTasksPerDay: Float,
        estimationAccuracy: Float
    ): Float {
        // Weighted score
        val completionWeight = 0.30f
        val onTimeWeight = 0.25f
        val streakWeight = 0.15f
        val volumeWeight = 0.15f
        val accuracyWeight = 0.15f

        val completionScore = completionRate // 0-100
        val onTimeScore = onTimeRate // 0-100
        val streakScore = minOf(streak * 10f, 100f) // 10 days = 100
        val volumeScore = minOf(avgTasksPerDay * 20f, 100f) // 5 tasks/day = 100
        val accuracyScore = estimationAccuracy // 0-100

        return (completionScore * completionWeight +
                onTimeScore * onTimeWeight +
                streakScore * streakWeight +
                volumeScore * volumeWeight +
                accuracyScore * accuracyWeight)
    }

    fun generateProductivityTips(): List<ProductivityTip> {
        val analytics = calculateAnalytics()
        val tips = mutableListOf<ProductivityTip>()

        // Completion rate tips
        if (analytics.completionRate < 50) {
            tips.add(ProductivityTip(
                "Break Down Large Tasks",
                "Your completion rate is ${analytics.completionRate.toInt()}%. Try breaking big tasks into smaller, manageable subtasks.",
                "📦"
            ))
        }

        // On-time tips
        if (analytics.onTimeCompletionRate < 70) {
            tips.add(ProductivityTip(
                "Add Buffer Time",
                "Only ${analytics.onTimeCompletionRate.toInt()}% of tasks are completed on time. Try adding 20% extra time to your estimates.",
                "⏰"
            ))
        }

        // Overdue tasks
        if (analytics.totalTasksOverdue > 3) {
            tips.add(ProductivityTip(
                "Clear Your Backlog",
                "You have ${analytics.totalTasksOverdue} overdue tasks. Consider rescheduling or cancelling tasks that are no longer relevant.",
                "📅"
            ))
        }

        // Streak encouragement
        if (analytics.currentStreak == 0) {
            tips.add(ProductivityTip(
                "Start a Streak!",
                "Complete at least one task today to start building momentum!",
                "🔥"
            ))
        } else if (analytics.currentStreak > 0 && analytics.currentStreak < analytics.longestStreak) {
            tips.add(ProductivityTip(
                "Beat Your Record!",
                "Current streak: ${analytics.currentStreak} days. Your best: ${analytics.longestStreak} days. Keep going!",
                "🏆"
            ))
        }

        // Time management
        if (analytics.mostProductiveHour >= 0) {
            val hourStr = when {
                analytics.mostProductiveHour == 0 -> "12 AM"
                analytics.mostProductiveHour < 12 -> "${analytics.mostProductiveHour} AM"
                analytics.mostProductiveHour == 12 -> "12 PM"
                else -> "${analytics.mostProductiveHour - 12} PM"
            }
            tips.add(ProductivityTip(
                "Peak Performance Time",
                "You're most productive around $hourStr. Schedule important tasks during this time!",
                "⚡"
            ))
        }

        // Estimation accuracy
        if (analytics.estimationAccuracy > 0 && analytics.estimationAccuracy < 60) {
            tips.add(ProductivityTip(
                "Improve Time Estimates",
                "Your time estimates are ${analytics.estimationAccuracy.toInt()}% accurate. Track actual time to improve planning.",
                "📊"
            ))
        }

        // Volume tip
        if (analytics.averageTasksPerDay < 1) {
            tips.add(ProductivityTip(
                "Set Daily Goals",
                "Try completing at least 3 tasks per day to build productivity habits.",
                "🎯"
            ))
        }

        // Positive reinforcement
        if (analytics.productivityScore >= 70) {
            tips.add(ProductivityTip(
                "You're Doing Great!",
                "Productivity score: ${analytics.productivityScore.toInt()}/100. Keep up the excellent work!",
                "🌟",
                actionable = false
            ))
        }

        return tips.take(5) // Return top 5 tips
    }

    // ==================== Streak Management ====================

    private fun updateStreakOnCompletion() {
        val today = getStartOfDay(System.currentTimeMillis())
        val lastCompletionDate = streakPreferences.getLong(LAST_COMPLETION_DATE_KEY, 0)
        var currentStreak = streakPreferences.getInt(STREAK_KEY, 0)

        when {
            lastCompletionDate == 0L -> {
                // First completion ever
                currentStreak = 1
            }
            lastCompletionDate == today -> {
                // Already completed today, no change
            }
            lastCompletionDate == today - 24 * 60 * 60 * 1000 -> {
                // Completed yesterday, increment streak
                currentStreak++
            }
            else -> {
                // Streak broken, start new
                currentStreak = 1
            }
        }

        val longestStreak = maxOf(streakPreferences.getInt(LONGEST_STREAK_KEY, 0), currentStreak)

        streakPreferences.edit()
            .putInt(STREAK_KEY, currentStreak)
            .putInt(LONGEST_STREAK_KEY, longestStreak)
            .putLong(LAST_COMPLETION_DATE_KEY, today)
            .apply()
    }

    fun getCurrentStreak(): Int {
        val lastCompletionDate = streakPreferences.getLong(LAST_COMPLETION_DATE_KEY, 0)
        val today = getStartOfDay(System.currentTimeMillis())
        val yesterday = today - 24 * 60 * 60 * 1000

        return if (lastCompletionDate == today || lastCompletionDate == yesterday) {
            streakPreferences.getInt(STREAK_KEY, 0)
        } else {
            0 // Streak broken
        }
    }

    fun getLongestStreak(): Int {
        return streakPreferences.getInt(LONGEST_STREAK_KEY, 0)
    }

    // ==================== Daily Stats ====================

    private fun recordDailyStats(task: DailyTask) {
        val today = getStartOfDay(System.currentTimeMillis())
        val statsJson = analyticsPreferences.getString(DAILY_STATS_KEY, "[]") ?: "[]"
        val type = object : TypeToken<MutableList<DailyStats>>() {}.type
        val statsList: MutableList<DailyStats> = try {
            gson.fromJson(statsJson, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }

        val existingIndex = statsList.indexOfFirst { it.date == today }
        val currentStats = if (existingIndex >= 0) statsList[existingIndex] else DailyStats(date = today)

        val updatedStats = currentStats.copy(
            tasksCompleted = currentStats.tasksCompleted + 1,
            totalMinutesWorked = currentStats.totalMinutesWorked + (task.actualMinutes ?: task.estimatedMinutes)
        )

        if (existingIndex >= 0) {
            statsList[existingIndex] = updatedStats
        } else {
            statsList.add(updatedStats)
        }

        // Keep only last 90 days
        val ninetyDaysAgo = today - 90L * 24 * 60 * 60 * 1000
        statsList.removeAll { it.date < ninetyDaysAgo }

        analyticsPreferences.edit()
            .putString(DAILY_STATS_KEY, gson.toJson(statsList))
            .apply()
    }

    fun getDailyStats(days: Int = 30): List<DailyStats> {
        val statsJson = analyticsPreferences.getString(DAILY_STATS_KEY, "[]") ?: "[]"
        val type = object : TypeToken<List<DailyStats>>() {}.type
        val statsList: List<DailyStats> = try {
            gson.fromJson(statsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val cutoff = getStartOfDay(System.currentTimeMillis()) - days * 24 * 60 * 60 * 1000
        return statsList.filter { it.date >= cutoff }.sortedBy { it.date }
    }

    // ==================== Reminders ====================

    fun scheduleReminder(task: DailyTask) {
        val reminderTime = task.reminderTime ?: return
        if (reminderTime <= System.currentTimeMillis()) return

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(task: DailyTask) {
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAllReminders() {
        getAllTasks()
            .filter { it.reminderEnabled && it.reminderTime != null && it.status == TaskStatus.PENDING }
            .forEach { scheduleReminder(it) }
    }

    // ==================== Utility Functions ====================

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun updateOverdueTasks() {
        val now = System.currentTimeMillis()
        val tasks = getAllTasks().toMutableList()
        var changed = false

        tasks.forEachIndexed { index, task ->
            if (task.status == TaskStatus.PENDING && task.dueDate < getStartOfDay(now)) {
                tasks[index] = task.copy(status = TaskStatus.OVERDUE, updatedAt = now)
                changed = true
            }
        }

        if (changed) {
            saveTasks(tasks)
        }
    }

    fun getQuickStats(): Triple<Int, Int, Int> {
        val today = getTodayTasks()
        val completed = today.count { it.status == TaskStatus.COMPLETED }
        val pending = today.count { it.status == TaskStatus.PENDING || it.status == TaskStatus.IN_PROGRESS }
        val overdue = getOverdueTasks().size
        return Triple(completed, pending, overdue)
    }
}
