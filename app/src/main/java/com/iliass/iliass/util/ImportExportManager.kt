package com.iliass.iliass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.Phone
import com.iliass.iliass.util.PhoneStorageManager
import java.io.ByteArrayOutputStream
import java.io.File

data class PhoneExportData(
    val phone: Phone,
    val imageBase64: String?
)

class ImportExportManager(private val context: Context) {

    private val gson = Gson()
    private val storageManager = PhoneStorageManager(context)

    /**
     * Export all phones to JSON with embedded images
     */
    fun exportToJson(): String {
        val phones = storageManager.getAllPhones()
        val exportData = phones.map { phone ->
            val imageBase64 = phone.imagePath?.let { path ->
                encodeImageToBase64(path)
            }
            PhoneExportData(phone, imageBase64)
        }
        return gson.toJson(exportData)
    }

    /**
     * Import phones from JSON
     */
    fun importFromJson(jsonContent: String): ImportResult {
        return try {
            val type = object : TypeToken<List<PhoneExportData>>() {}.type
            val exportData: List<PhoneExportData> = gson.fromJson(jsonContent, type)

            var successCount = 0
            var failedCount = 0
            var skippedCount = 0

            exportData.forEach { data ->
                try {
                    // Check if IMEI already exists
                    if (storageManager.isImeiExists(data.phone.imei)) {
                        skippedCount++
                        return@forEach // Skip this phone, continue with next
                    }

                    // Generate a NEW unique ID for the imported phone
                    val newPhoneId = System.currentTimeMillis().toString() + "_" + (0..9999).random()

                    // Decode and save image if exists
                    var imagePath: String? = null
                    data.imageBase64?.let { base64 ->
                        val bitmap = decodeBase64ToBitmap(base64)
                        bitmap?.let {
                            // Use the NEW phone ID for the image
                            imagePath = storageManager.saveImage(it, newPhoneId)
                        }
                    }

                    // Create phone with NEW ID and new image path
                    // CRITICAL FIX: Preserve the original dateRegistered instead of using current time
                    val phone = data.phone.copy(
                        id = newPhoneId,
                        imagePath = imagePath,
                        dateRegistered = data.phone.dateRegistered  // Keep original registration date
                    )

                    if (storageManager.savePhone(phone)) {
                        successCount++
                    } else {
                        failedCount++
                    }

                    // Small delay to ensure unique timestamps
                    Thread.sleep(1)

                } catch (e: Exception) {
                    e.printStackTrace()
                    failedCount++
                }
            }

            ImportResult(
                success = true,
                totalCount = exportData.size,
                successCount = successCount,
                failedCount = failedCount,
                skippedCount = skippedCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(
                success = false,
                errorMessage = "Failed to parse JSON: ${e.message}"
            )
        }
    }

    /**
     * Export to CSV format
     */
    fun exportToCsv(): String {
        val phones = storageManager.getAllPhones()
        val csv = StringBuilder()

        // Header
        csv.append("Phone Name,IMEI,Shop Name,Shop Address,Date Registered,Notes,Has Image\n")

        // Data rows
        phones.forEach { phone ->
            csv.append("\"${escapeCsv(phone.name)}\",")
            csv.append("\"${escapeCsv(phone.imei)}\",")
            csv.append("\"${escapeCsv(phone.shopName)}\",")
            csv.append("\"${escapeCsv(phone.shopAddress)}\",")
            csv.append("\"${phone.dateRegistered}\",")
            csv.append("\"${escapeCsv(phone.notes ?: "")}\",")
            csv.append("\"${if (phone.imagePath != null) "Yes" else "No"}\"\n")
        }

        return csv.toString()
    }

    private fun encodeImageToBase64(imagePath: String): String? {
        return try {
            val imageFile = File(imagePath)
            if (!imageFile.exists()) return null

            val bitmap = BitmapFactory.decodeFile(imagePath)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            val imageBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}

data class ImportResult(
    val success: Boolean,
    val totalCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0,
    val errorMessage: String = ""
) {
    fun getMessage(): String {
        return if (success) {
            buildString {
                append("Import completed!\n")
                append("Total: $totalCount\n")
                append("Success: $successCount\n")
                if (skippedCount > 0) {
                    append("Skipped (duplicate IMEI): $skippedCount\n")
                }
                if (failedCount > 0) {
                    append("Failed: $failedCount")
                }
            }
        } else {
            errorMessage
        }
    }
}