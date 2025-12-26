package com.iliass.iliass.model

data class AlarmTask(
    val id: String,
    val title: String,
    val description: String,
    val alarmTime: Long,
    val isEnabled: Boolean = true,
    val isRepeating: Boolean = false,
    val repeatInterval: RepeatInterval = RepeatInterval.NONE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class RepeatInterval(val displayName: String) {
    NONE("Once"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}
