package com.iliass.iliass.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.iliass.iliass.model.*
import com.iliass.iliass.repository.StudentDataExport
import com.iliass.iliass.repository.StudentDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages export and import of student-related data
 */
object StudentDataExportManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val readableDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    /**
     * Export all data to JSON format
     */
    fun exportToJson(context: Context): ExportResult {
        return try {
            val database = StudentDatabase.getInstance(context)
            val exportData = database.getAllDataForExport()
            val json = gson.toJson(exportData)

            val fileName = "student_data_${dateFormat.format(Date())}.json"
            val file = getExportFile(context, fileName)
            file.writeText(json)

            ExportResult.Success(file.absolutePath, "JSON")
        } catch (e: Exception) {
            ExportResult.Error("Failed to export JSON: ${e.message}")
        }
    }

    /**
     * Export students and payments to CSV format
     */
    fun exportStudentsToCsv(context: Context): ExportResult {
        return try {
            val database = StudentDatabase.getInstance(context)
            val students = database.getAllStudents()
            val payments = database.getAllPayments()

            val csv = buildString {
                // Header
                appendLine("ID,Name,Monthly Amount,Phone,Email,Location,Timezone Offset,Enrollment Date,Is Active,Notes,Total Paid,Current Debt")

                // Data rows
                for (student in students) {
                    val studentPayments = payments.filter { it.studentId == student.id }
                    val totalPaid = student.getTotalPaid(studentPayments)
                    val debt = student.getCurrentDebt(studentPayments)

                    appendLine(
                        "${escapeCsv(student.id)}," +
                        "${escapeCsv(student.name)}," +
                        "${student.monthlyAmount}," +
                        "${escapeCsv(student.phone)}," +
                        "${escapeCsv(student.email)}," +
                        "${escapeCsv(student.location)}," +
                        "${student.timezoneOffsetHours}," +
                        "${readableDateFormat.format(Date(student.enrollmentDate))}," +
                        "${student.isActive}," +
                        "${escapeCsv(student.notes)}," +
                        "$totalPaid," +
                        "$debt"
                    )
                }
            }

            val fileName = "students_${dateFormat.format(Date())}.csv"
            val file = getExportFile(context, fileName)
            file.writeText(csv)

            ExportResult.Success(file.absolutePath, "CSV")
        } catch (e: Exception) {
            ExportResult.Error("Failed to export students CSV: ${e.message}")
        }
    }

    /**
     * Export payments to CSV format
     */
    fun exportPaymentsToCsv(context: Context): ExportResult {
        return try {
            val database = StudentDatabase.getInstance(context)
            val students = database.getAllStudents()
            val payments = database.getAllPayments()

            val csv = buildString {
                // Header
                appendLine("Payment ID,Student Name,Amount,Payment Date,Month For,Notes")

                // Data rows
                for (payment in payments.sortedByDescending { it.paymentDate }) {
                    val student = students.find { it.id == payment.studentId }
                    appendLine(
                        "${escapeCsv(payment.id)}," +
                        "${escapeCsv(student?.name ?: "Unknown")}," +
                        "${payment.amount}," +
                        "${readableDateFormat.format(Date(payment.paymentDate))}," +
                        "${escapeCsv(payment.monthFor)}," +
                        "${escapeCsv(payment.notes)}"
                    )
                }
            }

            val fileName = "payments_${dateFormat.format(Date())}.csv"
            val file = getExportFile(context, fileName)
            file.writeText(csv)

            ExportResult.Success(file.absolutePath, "CSV")
        } catch (e: Exception) {
            ExportResult.Error("Failed to export payments CSV: ${e.message}")
        }
    }

    /**
     * Export classes and lessons to CSV format
     */
    fun exportClassesToCsv(context: Context): ExportResult {
        return try {
            val database = StudentDatabase.getInstance(context)
            val classes = database.getAllClasses()
            val students = database.getAllStudents()
            val lessons = database.getAllLessons()

            val csv = buildString {
                // Header
                appendLine("Class ID,Class Name,Type,Start Time,Days,Student Count,Created Date,Is Active,Notes,Total Lessons,Completed Lessons")

                // Data rows
                for (studentClass in classes) {
                    val classLessons = lessons.filter { it.classId == studentClass.id }
                    val completedLessons = classLessons.count { it.isCompleted }
                    val studentNames = studentClass.studentIds.mapNotNull { id ->
                        students.find { it.id == id }?.name
                    }.joinToString("; ")

                    appendLine(
                        "${escapeCsv(studentClass.id)}," +
                        "${escapeCsv(studentClass.name)}," +
                        "${escapeCsv(studentClass.type.displayName)}," +
                        "${escapeCsv(studentClass.startTime)}," +
                        "${escapeCsv(studentClass.days.joinToString("; "))}," +
                        "${studentClass.studentIds.size}," +
                        "${readableDateFormat.format(Date(studentClass.createdDate))}," +
                        "${studentClass.isActive}," +
                        "${escapeCsv(studentClass.notes)}," +
                        "${classLessons.size}," +
                        "$completedLessons"
                    )
                }
            }

            val fileName = "classes_${dateFormat.format(Date())}.csv"
            val file = getExportFile(context, fileName)
            file.writeText(csv)

            ExportResult.Success(file.absolutePath, "CSV")
        } catch (e: Exception) {
            ExportResult.Error("Failed to export classes CSV: ${e.message}")
        }
    }

    /**
     * Export lessons to CSV format
     */
    fun exportLessonsToCsv(context: Context): ExportResult {
        return try {
            val database = StudentDatabase.getInstance(context)
            val classes = database.getAllClasses()
            val lessons = database.getAllLessons()

            val csv = buildString {
                // Header
                appendLine("Lesson ID,Class Name,Title,Type,Date,Description,Is Completed,Has PDF,Notes")

                // Data rows
                for (lesson in lessons.sortedByDescending { it.date }) {
                    val className = classes.find { it.id == lesson.classId }?.name ?: "Unknown"
                    appendLine(
                        "${escapeCsv(lesson.id)}," +
                        "${escapeCsv(className)}," +
                        "${escapeCsv(lesson.title)}," +
                        "${escapeCsv(lesson.type.displayName)}," +
                        "${readableDateFormat.format(Date(lesson.date))}," +
                        "${escapeCsv(lesson.description)}," +
                        "${lesson.isCompleted}," +
                        "${!lesson.pdfFilePath.isNullOrEmpty()}," +
                        "${escapeCsv(lesson.notes)}"
                    )
                }
            }

            val fileName = "lessons_${dateFormat.format(Date())}.csv"
            val file = getExportFile(context, fileName)
            file.writeText(csv)

            ExportResult.Success(file.absolutePath, "CSV")
        } catch (e: Exception) {
            ExportResult.Error("Failed to export lessons CSV: ${e.message}")
        }
    }

    /**
     * Export comprehensive class report with all details
     */
    fun exportClassReportCsv(context: Context, classId: String): ExportResult {
        return try {
            val database = StudentDatabase.getInstance(context)
            val studentClass = database.getClassById(classId)
                ?: return ExportResult.Error("Class not found")

            val students = database.getAllStudents()
            val payments = database.getAllPayments()
            val lessons = database.getLessonsByClass(classId)

            val csv = buildString {
                // Class Info
                appendLine("CLASS REPORT")
                appendLine("Class Name,${escapeCsv(studentClass.name)}")
                appendLine("Type,${escapeCsv(studentClass.type.displayName)}")
                appendLine("Schedule,${escapeCsv(studentClass.startTime)} on ${studentClass.days.joinToString(", ")}")
                appendLine("Status,${if (studentClass.isActive) "Active" else "Inactive"}")
                appendLine("Created,${readableDateFormat.format(Date(studentClass.createdDate))}")
                appendLine()

                // Students Section
                appendLine("STUDENTS (${studentClass.studentIds.size})")
                appendLine("Name,Monthly Amount,Phone,Email,Total Paid,Current Debt")
                for (studentId in studentClass.studentIds) {
                    val student = students.find { it.id == studentId } ?: continue
                    val studentPayments = payments.filter { it.studentId == student.id }
                    appendLine(
                        "${escapeCsv(student.name)}," +
                        "${student.monthlyAmount}," +
                        "${escapeCsv(student.phone)}," +
                        "${escapeCsv(student.email)}," +
                        "${student.getTotalPaid(studentPayments)}," +
                        "${student.getCurrentDebt(studentPayments)}"
                    )
                }
                appendLine()

                // Lessons Section
                appendLine("LESSONS (${lessons.size} total, ${lessons.count { it.isCompleted }} completed)")
                appendLine("Title,Type,Date,Completed,Description")
                for (lesson in lessons) {
                    appendLine(
                        "${escapeCsv(lesson.title)}," +
                        "${escapeCsv(lesson.type.displayName)}," +
                        "${readableDateFormat.format(Date(lesson.date))}," +
                        "${if (lesson.isCompleted) "Yes" else "No"}," +
                        "${escapeCsv(lesson.description)}"
                    )
                }
            }

            val safeName = studentClass.name.replace(Regex("[^a-zA-Z0-9]"), "_")
            val fileName = "class_report_${safeName}_${dateFormat.format(Date())}.csv"
            val file = getExportFile(context, fileName)
            file.writeText(csv)

            ExportResult.Success(file.absolutePath, "CSV")
        } catch (e: Exception) {
            ExportResult.Error("Failed to export class report: ${e.message}")
        }
    }

    /**
     * Import data from JSON file
     */
    fun importFromJson(context: Context, uri: Uri, mergeMode: Boolean): ImportResult {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader()?.use { it.readText() }
                ?: return ImportResult.Error("Failed to read file")

            val importData = gson.fromJson(json, StudentDataExport::class.java)
                ?: return ImportResult.Error("Invalid data format")

            val database = StudentDatabase.getInstance(context)
            database.importData(importData, mergeMode)

            ImportResult.Success(
                studentsCount = importData.students.size,
                paymentsCount = importData.payments.size,
                classesCount = importData.classes.size,
                lessonsCount = importData.lessons.size
            )
        } catch (e: Exception) {
            ImportResult.Error("Failed to import: ${e.message}")
        }
    }

    /**
     * Get the export directory
     */
    fun getExportDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "StudentExports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getExportFile(context: Context, fileName: String): File {
        return File(getExportDirectory(context), fileName)
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /**
     * Get export summary text
     */
    fun getDataSummary(context: Context): String {
        val database = StudentDatabase.getInstance(context)
        val students = database.getAllStudents()
        val payments = database.getAllPayments()
        val classes = database.getAllClasses()
        val lessons = database.getAllLessons()

        return buildString {
            appendLine("Data Summary:")
            appendLine("- Students: ${students.size} (${students.count { it.isActive }} active)")
            appendLine("- Payments: ${payments.size}")
            appendLine("- Classes: ${classes.size} (${classes.count { it.isActive }} active)")
            appendLine("- Lessons: ${lessons.size} (${lessons.count { it.isCompleted }} completed)")
        }
    }

    sealed class ExportResult {
        data class Success(val filePath: String, val format: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    sealed class ImportResult {
        data class Success(
            val studentsCount: Int,
            val paymentsCount: Int,
            val classesCount: Int,
            val lessonsCount: Int
        ) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
