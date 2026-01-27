package com.iliass.iliass.model

import java.util.UUID

data class DailyTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.GENERAL,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long, // Timestamp for the date of the task
    val startTime: Long? = null, // Start time of the task interval (hour and minute)
    val endTime: Long? = null, // End time of the task interval (hour and minute)
    val hasTimeInterval: Boolean = false, // Whether this task has a scheduled time block
    val estimatedMinutes: Int = 30, // Estimated time to complete (used when no interval)
    val actualMinutes: Int? = null, // Actual time taken (filled on completion)
    val status: TaskStatus = TaskStatus.PENDING,
    val completedAt: Long? = null, // When task was marked complete
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val notes: String = "",
    val reminderEnabled: Boolean = false,
    val reminderTime: Long? = null // Time to send reminder
) {
    // Helper function to check if this task overlaps with another
    fun overlapsWithTime(otherStart: Long, otherEnd: Long, date: Long): Boolean {
        if (!hasTimeInterval || startTime == null || endTime == null) return false
        if (dueDate != date) return false

        val thisStart = startTime
        val thisEnd = endTime

        // Check for overlap: two intervals overlap if one starts before the other ends
        return thisStart < otherEnd && otherStart < thisEnd
    }

    // Get duration in minutes if time interval is set
    fun getIntervalDurationMinutes(): Int {
        return if (hasTimeInterval && startTime != null && endTime != null) {
            ((endTime - startTime) / (1000 * 60)).toInt()
        } else {
            estimatedMinutes
        }
    }
}

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

data class TimeConflict(
    val conflictingTask: DailyTask,
    val message: String
)

enum class TaskStatus(val displayName: String, val emoji: String) {
    PENDING("Pending", "⏳"),
    IN_PROGRESS("In Progress", "🔄"),
    COMPLETED("Completed", "✅"),
    CANCELLED("Cancelled", "❌"),
    OVERDUE("Overdue", "⚠️")
}

enum class TaskPriority(val displayName: String, val emoji: String, val colorHex: String) {
    LOW("Low", "🟢", "#4CAF50"),
    MEDIUM("Medium", "🟡", "#FFC107"),
    HIGH("High", "🟠", "#FF9800"),
    URGENT("Urgent", "🔴", "#F44336")
}

enum class TaskCategory(val displayName: String, val emoji: String, val colorHex: String) {
    GENERAL("General", "📋", "#607D8B"),
    WORK("Work", "💼", "#2196F3"),
    PERSONAL("Personal", "🏠", "#9C27B0"),
    HEALTH("Health & Fitness", "💪", "#4CAF50"),
    LEARNING("Learning", "📚", "#FF9800"),
    SHOPPING("Shopping", "🛒", "#E91E63"),
    FINANCE("Finance", "💰", "#795548"),
    SOCIAL("Social", "👥", "#00BCD4"),
    ERRANDS("Errands", "🏃", "#FFEB3B"),
    CREATIVE("Creative", "🎨", "#673AB7")
}

// Analytics data models
data class TaskAnalytics(
    val totalTasksCreated: Int = 0,
    val totalTasksCompleted: Int = 0,
    val totalTasksCancelled: Int = 0,
    val totalTasksOverdue: Int = 0,
    val completionRate: Float = 0f, // Percentage
    val onTimeCompletionRate: Float = 0f, // Completed before due date
    val averageCompletionTimeMinutes: Float = 0f,
    val currentStreak: Int = 0, // Days with at least one completed task
    val longestStreak: Int = 0,
    val productivityScore: Float = 0f, // 0-100 score
    val mostProductiveHour: Int = -1, // 0-23
    val mostProductiveDay: Int = -1, // 1-7 (Sunday=1)
    val categoryBreakdown: Map<TaskCategory, Int> = emptyMap(),
    val priorityBreakdown: Map<TaskPriority, Int> = emptyMap(),
    val weeklyCompletion: List<Int> = List(7) { 0 }, // Tasks completed per day of week
    val averageTasksPerDay: Float = 0f,
    val estimationAccuracy: Float = 0f, // How accurate time estimates are
    val lastUpdated: Long = System.currentTimeMillis()
)

data class DailyStats(
    val date: Long,
    val tasksCreated: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksOverdue: Int = 0,
    val totalMinutesWorked: Int = 0,
    val completionRate: Float = 0f
)

data class ProductivityTip(
    val title: String,
    val description: String,
    val emoji: String,
    val actionable: Boolean = true
)
