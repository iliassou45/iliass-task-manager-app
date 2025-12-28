package com.iliass.iliass.model

import java.util.*

/**
 * Tracks the history of students joining and leaving classes
 * This helps calculate how many students have left over time
 */
data class ClassStudentHistory(
    val id: String = UUID.randomUUID().toString(),
    val classId: String,
    val studentId: String,
    val action: HistoryAction,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

enum class HistoryAction {
    JOINED,
    LEFT
}
