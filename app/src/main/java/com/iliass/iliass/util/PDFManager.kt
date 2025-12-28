package com.iliass.iliass.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Utility class for managing PDF files for lessons
 */
object PDFManager {
    private const val TAG = "PDFManager"
    private const val PDF_DIRECTORY = "lesson_pdfs"

    /**
     * Get the directory where PDFs are stored
     */
    fun getPDFDirectory(context: Context): File {
        val dir = File(context.filesDir, PDF_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Save a PDF file from a URI
     * @param context Application context
     * @param uri URI of the PDF file to save
     * @param lessonId ID of the lesson this PDF belongs to
     * @return Path to the saved PDF file, or null if failed
     */
    fun savePDFFile(context: Context, uri: Uri, lessonId: String): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $uri")
                return null
            }

            val pdfDir = getPDFDirectory(context)
            val fileName = "lesson_${lessonId}.pdf"
            val pdfFile = File(pdfDir, fileName)

            FileOutputStream(pdfFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            Log.d(TAG, "PDF saved successfully: ${pdfFile.absolutePath}")
            return pdfFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PDF file", e)
            return null
        }
    }

    /**
     * Delete a PDF file
     * @param filePath Path to the PDF file
     * @return true if deleted successfully, false otherwise
     */
    fun deletePDFFile(filePath: String): Boolean {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "PDF deleted successfully: $filePath")
                } else {
                    Log.e(TAG, "Failed to delete PDF: $filePath")
                }
                return deleted
            } else {
                Log.w(TAG, "PDF file does not exist: $filePath")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting PDF file", e)
            return false
        }
    }

    /**
     * Check if a PDF file exists
     */
    fun pdfExists(filePath: String?): Boolean {
        if (filePath == null) return false
        return File(filePath).exists()
    }

    /**
     * Get the file size in a human-readable format
     */
    fun getFileSize(filePath: String?): String {
        if (filePath == null) return "Unknown"
        try {
            val file = File(filePath)
            if (!file.exists()) return "File not found"

            val sizeInBytes = file.length()
            return when {
                sizeInBytes < 1024 -> "$sizeInBytes B"
                sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
                else -> String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size", e)
            return "Unknown"
        }
    }

    /**
     * Get file URI for sharing or viewing
     */
    fun getFileUri(filePath: String): Uri {
        return Uri.fromFile(File(filePath))
    }
}
