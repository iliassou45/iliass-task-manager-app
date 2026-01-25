package com.iliass.iliass.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.Note
import com.iliass.iliass.model.Student
import com.iliass.iliass.model.StudentClass
import com.iliass.iliass.repository.DebtDatabase
import com.iliass.iliass.repository.StudentDataExport
import com.iliass.iliass.repository.StudentDatabase
import io.github.jan.supabase.storage.upload
import io.github.jan.supabase.storage.downloadAuthenticated
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager for backing up and restoring data to/from Supabase cloud storage
 */
object SupabaseBackupManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    private val readableDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    private const val BACKUP_FILE_NAME = "backup_data.json"

    /**
     * Upload backup to Supabase storage
     */
    suspend fun uploadBackup(context: Context): BackupResult {
        return try {
            // Check if user is authenticated
            val userId = SupabaseManager.getCurrentUserId()
                ?: return BackupResult.Error("Please sign in to backup your data")

            // Get all data from database
            val database = StudentDatabase.getInstance(context)
            val exportData = database.getAllDataForExport(context)

            // Convert to JSON
            val json = gson.toJson(exportData)
            val jsonBytes = json.toByteArray(Charsets.UTF_8)

            // Upload to Supabase storage
            val filePath = "$userId/$BACKUP_FILE_NAME"
            val bucket = SupabaseManager.getStorageBucket()

            // Try to upload (will overwrite if exists)
            bucket.upload(filePath, jsonBytes, upsert = true)

            // Save last sync timestamp
            val timestamp = System.currentTimeMillis()
            SupabaseManager.saveLastSyncTimestamp(context, timestamp)

            BackupResult.Success(
                message = "Backup uploaded successfully",
                timestamp = timestamp,
                itemCounts = ItemCounts(
                    students = exportData.students.size,
                    payments = exportData.payments.size,
                    classes = exportData.classes.size,
                    lessons = exportData.lessons.size,
                    notes = exportData.notes.size,
                    debts = exportData.debts.size
                )
            )
        } catch (e: Exception) {
            BackupResult.Error("Backup failed: ${e.message}")
        }
    }

    /**
     * Download and restore backup from Supabase storage
     */
    suspend fun downloadBackup(context: Context, mergeMode: Boolean = false): RestoreResult {
        return try {
            // Check if user is authenticated
            val userId = SupabaseManager.getCurrentUserId()
                ?: return RestoreResult.Error("Please sign in to restore your data")

            // Download from Supabase storage
            val filePath = "$userId/$BACKUP_FILE_NAME"
            val bucket = SupabaseManager.getStorageBucket()

            val jsonBytes = bucket.downloadAuthenticated(filePath)
            val json = String(jsonBytes, Charsets.UTF_8)

            // Parse JSON
            val importData = gson.fromJson(json, StudentDataExport::class.java)
                ?: return RestoreResult.Error("Invalid backup data format")

            // Import data to database
            val database = StudentDatabase.getInstance(context)
            database.importData(importData, mergeMode, context)

            // Update last sync timestamp
            val timestamp = System.currentTimeMillis()
            SupabaseManager.saveLastSyncTimestamp(context, timestamp)

            RestoreResult.Success(
                message = if (mergeMode) "Data merged successfully" else "Data restored successfully",
                timestamp = timestamp,
                itemCounts = ItemCounts(
                    students = importData.students.size,
                    payments = importData.payments.size,
                    classes = importData.classes.size,
                    lessons = importData.lessons.size,
                    notes = importData.notes.size,
                    debts = importData.debts.size
                )
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Object not found") == true -> "No backup found in cloud"
                e.message?.contains("404") == true -> "No backup found in cloud"
                else -> "Restore failed: ${e.message}"
            }
            RestoreResult.Error(errorMessage)
        }
    }

    /**
     * Check if a backup exists in the cloud
     */
    suspend fun checkBackupExists(context: Context): BackupInfo? {
        return try {
            val userId = SupabaseManager.getCurrentUserId() ?: return null
            val filePath = "$userId/$BACKUP_FILE_NAME"
            val bucket = SupabaseManager.getStorageBucket()

            // Try to download and parse to verify it exists and is valid
            val jsonBytes = bucket.downloadAuthenticated(filePath)
            val json = String(jsonBytes, Charsets.UTF_8)
            val importData = gson.fromJson(json, StudentDataExport::class.java)

            if (importData != null) {
                BackupInfo(
                    exists = true,
                    backupDate = importData.exportDate,
                    itemCounts = ItemCounts(
                        students = importData.students.size,
                        payments = importData.payments.size,
                        classes = importData.classes.size,
                        lessons = importData.lessons.size,
                        notes = importData.notes.size,
                        debts = importData.debts.size
                    )
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compare local data with cloud data
     */
    suspend fun compareWithCloud(context: Context): ComparisonResult {
        return try {
            val userId = SupabaseManager.getCurrentUserId()
                ?: return ComparisonResult.Error("Please sign in to compare data")

            // Get local data
            val database = StudentDatabase.getInstance(context)
            val localData = database.getAllDataForExport(context)

            // Download cloud data
            val filePath = "$userId/$BACKUP_FILE_NAME"
            val bucket = SupabaseManager.getStorageBucket()

            val jsonBytes = bucket.downloadAuthenticated(filePath)
            val json = String(jsonBytes, Charsets.UTF_8)
            val cloudData = gson.fromJson(json, StudentDataExport::class.java)
                ?: return ComparisonResult.Error("Invalid backup data format")

            // Compare students
            val localStudentIds = localData.students.map { it.id }.toSet()
            val cloudStudentIds = cloudData.students.map { it.id }.toSet()
            val newStudentsInCloud = cloudData.students.filter { it.id !in localStudentIds }
            val missingStudentsInCloud = localData.students.filter { it.id !in cloudStudentIds }

            // Compare classes
            val localClassIds = localData.classes.map { it.id }.toSet()
            val cloudClassIds = cloudData.classes.map { it.id }.toSet()
            val newClassesInCloud = cloudData.classes.filter { it.id !in localClassIds }
            val missingClassesInCloud = localData.classes.filter { it.id !in cloudClassIds }

            // Compare notes
            val localNoteIds = localData.notes.map { it.id }.toSet()
            val cloudNoteIds = cloudData.notes.map { it.id }.toSet()
            val newNotesInCloud = cloudData.notes.filter { it.id !in localNoteIds }
            val missingNotesInCloud = localData.notes.filter { it.id !in cloudNoteIds }

            // Compare debts
            val localDebtIds = localData.debts.map { it.id }.toSet()
            val cloudDebtIds = cloudData.debts.map { it.id }.toSet()
            val newDebtsInCloud = cloudData.debts.filter { it.id !in localDebtIds }
            val missingDebtsInCloud = localData.debts.filter { it.id !in cloudDebtIds }

            // Compare payments
            val localPaymentIds = localData.payments.map { it.id }.toSet()
            val cloudPaymentIds = cloudData.payments.map { it.id }.toSet()
            val newPaymentsInCloud = cloudData.payments.size - localData.payments.filter { it.id in cloudPaymentIds }.size
            val missingPaymentsInCloud = localData.payments.size - cloudData.payments.filter { it.id in localPaymentIds }.size

            // Compare lessons
            val localLessonIds = localData.lessons.map { it.id }.toSet()
            val cloudLessonIds = cloudData.lessons.map { it.id }.toSet()
            val newLessonsInCloud = cloudData.lessons.size - localData.lessons.filter { it.id in cloudLessonIds }.size
            val missingLessonsInCloud = localData.lessons.size - cloudData.lessons.filter { it.id in localLessonIds }.size

            ComparisonResult.Success(
                cloudBackupDate = cloudData.exportDate,
                localCounts = ItemCounts(
                    students = localData.students.size,
                    payments = localData.payments.size,
                    classes = localData.classes.size,
                    lessons = localData.lessons.size,
                    notes = localData.notes.size,
                    debts = localData.debts.size
                ),
                cloudCounts = ItemCounts(
                    students = cloudData.students.size,
                    payments = cloudData.payments.size,
                    classes = cloudData.classes.size,
                    lessons = cloudData.lessons.size,
                    notes = cloudData.notes.size,
                    debts = cloudData.debts.size
                ),
                differences = DataDifferences(
                    newStudentsInCloud = newStudentsInCloud,
                    missingStudentsInCloud = missingStudentsInCloud,
                    newClassesInCloud = newClassesInCloud,
                    missingClassesInCloud = missingClassesInCloud,
                    newNotesInCloud = newNotesInCloud,
                    missingNotesInCloud = missingNotesInCloud,
                    newDebtsInCloud = newDebtsInCloud,
                    missingDebtsInCloud = missingDebtsInCloud,
                    newPaymentsCount = newPaymentsInCloud,
                    missingPaymentsCount = missingPaymentsInCloud,
                    newLessonsCount = newLessonsInCloud,
                    missingLessonsCount = missingLessonsInCloud
                )
            )
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Object not found") == true -> "No backup found in cloud"
                e.message?.contains("404") == true -> "No backup found in cloud"
                else -> "Comparison failed: ${e.message}"
            }
            ComparisonResult.Error(errorMessage)
        }
    }

    /**
     * Get formatted last sync time
     */
    fun getFormattedLastSyncTime(context: Context): String {
        val timestamp = SupabaseManager.getLastSyncTimestamp(context)
        return if (timestamp > 0) {
            readableDateFormat.format(Date(timestamp))
        } else {
            "Never"
        }
    }

    /**
     * Get current data summary
     */
    fun getDataSummary(context: Context): String {
        val database = StudentDatabase.getInstance(context)
        val students = database.getAllStudents()
        val payments = database.getAllPayments()
        val classes = database.getAllClasses()
        val lessons = database.getAllLessons()

        // Get notes
        val noteManager = NoteManager(context)
        val notes = noteManager.getAllNotes()

        // Get debts
        val debtDatabase = DebtDatabase.getInstance(context)
        val debts = debtDatabase.getAllDebts()

        return buildString {
            appendLine("Current Data:")
            appendLine("• Students: ${students.size}")
            appendLine("• Payments: ${payments.size}")
            appendLine("• Classes: ${classes.size}")
            appendLine("• Lessons: ${lessons.size}")
            appendLine("• Notes: ${notes.size}")
            appendLine("• Debts: ${debts.size}")
        }
    }

    data class ItemCounts(
        val students: Int,
        val payments: Int,
        val classes: Int,
        val lessons: Int,
        val notes: Int = 0,
        val debts: Int = 0
    ) {
        fun toDisplayString(): String {
            return "Students: $students, Payments: $payments, Classes: $classes, Lessons: $lessons, Notes: $notes, Debts: $debts"
        }
    }

    data class BackupInfo(
        val exists: Boolean,
        val backupDate: Long,
        val itemCounts: ItemCounts
    )

    sealed class BackupResult {
        data class Success(
            val message: String,
            val timestamp: Long,
            val itemCounts: ItemCounts
        ) : BackupResult()
        data class Error(val message: String) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(
            val message: String,
            val timestamp: Long,
            val itemCounts: ItemCounts
        ) : RestoreResult()
        data class Error(val message: String) : RestoreResult()
    }

    sealed class ComparisonResult {
        data class Success(
            val cloudBackupDate: Long,
            val localCounts: ItemCounts,
            val cloudCounts: ItemCounts,
            val differences: DataDifferences
        ) : ComparisonResult()
        data class Error(val message: String) : ComparisonResult()
    }

    data class DataDifferences(
        val newStudentsInCloud: List<Student>,
        val missingStudentsInCloud: List<Student>,
        val newClassesInCloud: List<StudentClass>,
        val missingClassesInCloud: List<StudentClass>,
        val newNotesInCloud: List<Note>,
        val missingNotesInCloud: List<Note>,
        val newDebtsInCloud: List<Debt>,
        val missingDebtsInCloud: List<Debt>,
        val newPaymentsCount: Int,
        val missingPaymentsCount: Int,
        val newLessonsCount: Int,
        val missingLessonsCount: Int
    ) {
        fun hasDifferences(): Boolean {
            return newStudentsInCloud.isNotEmpty() ||
                    missingStudentsInCloud.isNotEmpty() ||
                    newClassesInCloud.isNotEmpty() ||
                    missingClassesInCloud.isNotEmpty() ||
                    newNotesInCloud.isNotEmpty() ||
                    missingNotesInCloud.isNotEmpty() ||
                    newDebtsInCloud.isNotEmpty() ||
                    missingDebtsInCloud.isNotEmpty() ||
                    newPaymentsCount > 0 ||
                    missingPaymentsCount > 0 ||
                    newLessonsCount > 0 ||
                    missingLessonsCount > 0
        }

        fun toSummaryString(): String {
            val sb = StringBuilder()

            if (newStudentsInCloud.isNotEmpty()) {
                sb.appendLine("NEW in Cloud (will be added):")
                newStudentsInCloud.forEach { sb.appendLine("  + Student: ${it.name}") }
            }
            if (newClassesInCloud.isNotEmpty()) {
                newClassesInCloud.forEach { sb.appendLine("  + Class: ${it.name}") }
            }
            if (newNotesInCloud.isNotEmpty()) {
                newNotesInCloud.forEach { sb.appendLine("  + Note: ${it.title}") }
            }
            if (newDebtsInCloud.isNotEmpty()) {
                newDebtsInCloud.forEach { sb.appendLine("  + Debt: ${it.personName}") }
            }
            if (newPaymentsCount > 0) {
                sb.appendLine("  + $newPaymentsCount new payment(s)")
            }
            if (newLessonsCount > 0) {
                sb.appendLine("  + $newLessonsCount new lesson(s)")
            }

            if (missingStudentsInCloud.isNotEmpty() || missingClassesInCloud.isNotEmpty() ||
                missingNotesInCloud.isNotEmpty() || missingDebtsInCloud.isNotEmpty() ||
                missingPaymentsCount > 0 || missingLessonsCount > 0) {
                sb.appendLine()
                sb.appendLine("MISSING in Cloud (only in local):")
                missingStudentsInCloud.forEach { sb.appendLine("  - Student: ${it.name}") }
                missingClassesInCloud.forEach { sb.appendLine("  - Class: ${it.name}") }
                missingNotesInCloud.forEach { sb.appendLine("  - Note: ${it.title}") }
                missingDebtsInCloud.forEach { sb.appendLine("  - Debt: ${it.personName}") }
                if (missingPaymentsCount > 0) {
                    sb.appendLine("  - $missingPaymentsCount payment(s)")
                }
                if (missingLessonsCount > 0) {
                    sb.appendLine("  - $missingLessonsCount lesson(s)")
                }
            }

            return if (sb.isEmpty()) "No differences found - data is in sync!" else sb.toString()
        }
    }
}
