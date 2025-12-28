package com.iliass.iliass.model

import java.util.*

/**
 * Represents a lesson taught in a class
 */
data class Lesson(
    val id: String = UUID.randomUUID().toString(),
    val classId: String, // Reference to StudentClass
    val title: String,
    val type: LessonType,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val pdfFilePath: String? = null, // Path to attached PDF file
    val isCompleted: Boolean = false,
    val notes: String = ""
)

/**
 * Types of lessons
 */
enum class LessonType(val displayName: String) {
    LESSON("Lesson"),
    EXERCISE("Exercise"),
    CORRECTION("Correction"),
    DIALOG("Dialog"),
    VOCABULARY("Vocabulary"),
    GRAMMAR("Grammar"),
    READING("Reading"),
    LISTENING("Listening"),
    SPEAKING("Speaking"),
    WRITING("Writing"),
    TEST("Test"),
    REVIEW("Review");

    companion object {
        fun fromDisplayName(name: String): LessonType? {
            return values().find { it.displayName == name }
        }
    }
}
