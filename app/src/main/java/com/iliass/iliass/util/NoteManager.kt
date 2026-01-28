package com.iliass.iliass.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.Note
import java.io.File

class NoteManager(private val context: Context) {

    private val gson = Gson()
    private val notesDirectory: File
    private val notesFile: File
    // Fallback to app-private storage if external is not available
    private val internalNotesFile: File

    companion object {
        private const val TAG = "NoteManager"
        private const val NOTES_DIR = "Notes"
        private const val NOTES_FILE = "notes.json"
    }

    init {
        // Try external storage first
        notesDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            NOTES_DIR
        )
        try {
            if (!notesDirectory.exists()) {
                notesDirectory.mkdirs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not create external notes directory", e)
        }

        notesFile = File(notesDirectory, NOTES_FILE)

        // Internal storage fallback (app-private, no permissions needed)
        val internalDir = File(context.filesDir, NOTES_DIR)
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        internalNotesFile = File(internalDir, NOTES_FILE)
    }

    private fun getActiveNotesFile(): File {
        // Prefer external if it exists and is readable, otherwise use internal
        return if (notesFile.exists() && notesFile.canRead()) {
            notesFile
        } else if (internalNotesFile.exists()) {
            internalNotesFile
        } else {
            // Default to internal for new saves (more reliable)
            internalNotesFile
        }
    }

    private fun getWritableNotesFile(): File {
        // Try external first, fallback to internal
        return try {
            if (notesDirectory.exists() || notesDirectory.mkdirs()) {
                if (notesFile.exists() || notesFile.createNewFile() || notesFile.canWrite()) {
                    notesFile
                } else {
                    internalNotesFile
                }
            } else {
                internalNotesFile
            }
        } catch (e: Exception) {
            Log.w(TAG, "External storage not available, using internal", e)
            internalNotesFile
        }
    }

    fun saveNote(note: Note): Boolean {
        return try {
            Log.d(TAG, "========== SAVING NOTE TO DATABASE ==========")
            Log.d(TAG, "Note title: ${note.title}")
            Log.d(TAG, "Note id: ${note.id}")
            Log.d(TAG, "Note content length: ${note.content.length}")
            Log.d(TAG, "Note contentWithFormatting length: ${note.contentWithFormatting.length}")
            Log.d(TAG, "Note color: ${String.format("%08X", note.color)}")

            val allNotes = getAllNotes().toMutableList()

            // Remove existing note if updating
            allNotes.removeAll { it.id == note.id }

            // Add the new/updated note
            allNotes.add(note)

            // Save to file - use writable file
            val json = gson.toJson(allNotes)
            val targetFile = getWritableNotesFile()
            Log.d(TAG, "Saving to: ${targetFile.absolutePath}")
            Log.d(TAG, "JSON being written (first 500 chars): ${json.take(500)}")

            targetFile.writeText(json)

            // Also sync to internal storage for reliability
            if (targetFile != internalNotesFile) {
                try {
                    internalNotesFile.writeText(json)
                    Log.d(TAG, "Also synced to internal storage")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not sync to internal storage", e)
                }
            }

            Log.d(TAG, "Note saved: ${note.title}")
            Log.d(TAG, "========== SAVE COMPLETE ==========")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note to primary location, trying internal...", e)
            // Fallback: try saving to internal storage only
            return try {
                val allNotes = getAllNotesFromFile(internalNotesFile).toMutableList()
                allNotes.removeAll { it.id == note.id }
                allNotes.add(note)
                val json = gson.toJson(allNotes)
                internalNotesFile.writeText(json)
                Log.d(TAG, "Note saved to internal storage: ${note.title}")
                true
            } catch (e2: Exception) {
                Log.e(TAG, "Error saving note to internal storage", e2)
                false
            }
        }
    }

    private fun getAllNotesFromFile(file: File): List<Note> {
        return try {
            if (!file.exists() || !file.canRead()) {
                return emptyList()
            }
            val json = file.readText()
            if (json.isEmpty()) {
                return emptyList()
            }
            val type = object : TypeToken<List<Note>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notes from ${file.absolutePath}", e)
            emptyList()
        }
    }

    fun getAllNotes(): List<Note> {
        return try {
            // Load from both locations and merge (prefer by most recent updatedAt)
            val externalNotes = getAllNotesFromFile(notesFile)
            val internalNotes = getAllNotesFromFile(internalNotesFile)

            Log.d(TAG, "External notes: ${externalNotes.size}, Internal notes: ${internalNotes.size}")

            // Merge: combine both, preferring the most recently updated version
            val mergedMap = mutableMapOf<String, Note>()

            externalNotes.forEach { note ->
                mergedMap[note.id] = note
            }

            internalNotes.forEach { note ->
                val existing = mergedMap[note.id]
                if (existing == null || note.updatedAt > existing.updatedAt) {
                    mergedMap[note.id] = note
                }
            }

            val notes = mergedMap.values.toList()
            Log.d(TAG, "Loaded ${notes.size} total notes (merged)")

            notes
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notes", e)
            emptyList()
        }
    }

    fun deleteNote(noteId: String): Boolean {
        return try {
            val allNotes = getAllNotes().toMutableList()
            allNotes.removeAll { it.id == noteId }

            val json = gson.toJson(allNotes)
            notesFile.writeText(json)

            Log.d(TAG, "Note deleted: $noteId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note", e)
            false
        }
    }

    fun exportNotesToFile(): Boolean {
        return try {
            val allNotes = getAllNotes()
            val timestamp = System.currentTimeMillis()
            val exportFile = File(notesDirectory, "notes_backup_$timestamp.json")

            val json = gson.toJson(allNotes)
            exportFile.writeText(json)

            Log.d(TAG, "Notes exported to ${exportFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting notes", e)
            false
        }
    }

    fun importNotes(): Boolean {
        return try {
            // Look for backup files in the notes directory
            val backupFiles = notesDirectory.listFiles { file ->
                file.name.startsWith("notes_backup_") && file.name.endsWith(".json")
            }

            if (backupFiles.isNullOrEmpty()) {
                Log.w(TAG, "No backup files found")
                return false
            }

            // Import from the most recent backup
            val backupFile = backupFiles.maxByOrNull { it.lastModified() } ?: return false

            val json = backupFile.readText()
            if (json.isEmpty()) {
                return false
            }

            val type = object : TypeToken<List<Note>>() {}.type
            val importedNotes: List<Note> = gson.fromJson(json, type)

            // Merge with existing notes
            val existingNotes = getAllNotes().toMutableList()
            for (importedNote in importedNotes) {
                existingNotes.removeAll { it.id == importedNote.id }
                existingNotes.add(importedNote)
            }

            val mergedJson = gson.toJson(existingNotes)
            notesFile.writeText(mergedJson)

            Log.d(TAG, "Notes imported: ${importedNotes.size} notes")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error importing notes", e)
            false
        }
    }

    fun getNotesDirectoryPath(): String {
        return notesDirectory.absolutePath
    }
}