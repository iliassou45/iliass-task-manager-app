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

    companion object {
        private const val TAG = "NoteManager"
        private const val NOTES_DIR = "Notes"
        private const val NOTES_FILE = "notes.json"
    }

    init {
        // Create notes directory in external storage
        notesDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            NOTES_DIR
        )
        if (!notesDirectory.exists()) {
            notesDirectory.mkdirs()
        }

        notesFile = File(notesDirectory, NOTES_FILE)
    }

    fun saveNote(note: Note): Boolean {
        return try {
            Log.d(TAG, "========== SAVING NOTE TO DATABASE ==========")
            Log.d(TAG, "Note title: ${note.title}")
            Log.d(TAG, "Note id: ${note.id}")
            Log.d(TAG, "Note content length: ${note.content.length}")
            Log.d(TAG, "Note contentWithFormatting length: ${note.contentWithFormatting.length}")
            Log.d(TAG, "Note contentWithFormatting: '${note.contentWithFormatting}'")
            Log.d(TAG, "Note color: ${String.format("%08X", note.color)}")

            val allNotes = getAllNotes().toMutableList()

            // Remove existing note if updating
            allNotes.removeAll { it.id == note.id }

            // Add the new/updated note
            allNotes.add(note)

            // Save to file
            val json = gson.toJson(allNotes)
            Log.d(TAG, "JSON being written (first 500 chars): ${json.take(500)}")

            notesFile.writeText(json)

            Log.d(TAG, "Note saved: ${note.title}")
            Log.d(TAG, "========== SAVE COMPLETE ==========")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note", e)
            false
        }
    }

    fun getAllNotes(): List<Note> {
        return try {
            if (!notesFile.exists()) {
                return emptyList()
            }

            val json = notesFile.readText()
            if (json.isEmpty()) {
                return emptyList()
            }

            Log.d(TAG, "Loading notes from JSON (first 500 chars): ${json.take(500)}")

            val type = object : TypeToken<List<Note>>() {}.type
            val notes: List<Note> = gson.fromJson(json, type)

            Log.d(TAG, "Loaded ${notes.size} notes from database")
            for ((idx, note) in notes.withIndex()) {
                Log.d(TAG, "Note $idx: title='${note.title}', contentWithFormatting length=${note.contentWithFormatting.length}, contentWithFormatting='${note.contentWithFormatting}'")
            }

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