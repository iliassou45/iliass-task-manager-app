package com.iliass.iliass.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
            val exportData = database.getAllDataForExport()

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
                    lessons = exportData.lessons.size
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
            database.importData(importData, mergeMode)

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
                    lessons = importData.lessons.size
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
                        lessons = importData.lessons.size
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

        return buildString {
            appendLine("Current Data:")
            appendLine("• Students: ${students.size}")
            appendLine("• Payments: ${payments.size}")
            appendLine("• Classes: ${classes.size}")
            appendLine("• Lessons: ${lessons.size}")
        }
    }

    data class ItemCounts(
        val students: Int,
        val payments: Int,
        val classes: Int,
        val lessons: Int
    ) {
        fun toDisplayString(): String {
            return "Students: $students, Payments: $payments, Classes: $classes, Lessons: $lessons"
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
}
