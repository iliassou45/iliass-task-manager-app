package com.iliass.iliass.model

import com.iliass.iliass.repository.StudentDatabase
import java.util.*

/**
 * Represents a class/group of students
 */
data class StudentClass(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ClassType,
    val startTime: String, // Format: "HH:mm" (e.g., "14:30")
    val days: List<String>, // e.g., ["Monday", "Wednesday", "Friday"]
    val studentIds: MutableList<String> = mutableListOf(), // List of student IDs in this class
    val createdDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = ""
) {
    /**
     * Get the number of students currently in the class
     */
    fun getCurrentStudentCount(): Int = studentIds.size

    /**
     * Get the number of students who left (were removed from the class)
     * This is tracked in the database history
     */
    fun getStudentsLeftCount(database: StudentDatabase): Int {
        return database.getStudentsLeftFromClass(id)
    }

    /**
     * Calculate monthly revenue potential based on students' monthly payments
     */
    fun getMonthlyRevenuePotential(database: StudentDatabase): Double {
        val students = database.getAllStudents().filter { it.id in studentIds }
        return students.sumOf { it.monthlyAmount }
    }

    /**
     * Calculate total debt for this class
     */
    fun getTotalClassDebt(database: StudentDatabase): Double {
        val students = database.getAllStudents().filter { it.id in studentIds }
        val allPayments = database.getAllPayments()
        return students.sumOf { student ->
            student.getCurrentDebt(allPayments.filter { it.studentId == student.id })
        }
    }

    /**
     * Get all students in this class
     */
    fun getStudents(database: StudentDatabase): List<Student> {
        return database.getAllStudents().filter { it.id in studentIds }
    }
}

/**
 * Types of classes
 */
enum class ClassType(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    EXPERT("Expert"),
    CONVERSATION("Conversation"),
    EXAM_PREP("Exam Preparation"),
    BUSINESS("Business"),
    KIDS("Kids"),
    PRIVATE("Private Lessons");

    companion object {
        fun fromDisplayName(name: String): ClassType? {
            return values().find { it.displayName == name }
        }
    }
}
