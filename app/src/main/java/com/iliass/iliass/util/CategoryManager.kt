package com.iliass.iliass.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class CategoryManager(private val context: Context) {

    private val gson = Gson()
    private val categoriesFile: File

    companion object {
        private const val TAG = "CategoryManager"
        private const val CATEGORIES_FILE = "categories.json"
        const val DEFAULT_CATEGORY = "Uncategorized"
    }

    init {
        // Use the same directory as NoteManager for consistency
        val notesDirectory = File(
            android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOCUMENTS
            ),
            "Notes"
        )
        if (!notesDirectory.exists()) {
            notesDirectory.mkdirs()
        }

        categoriesFile = File(notesDirectory, CATEGORIES_FILE)
    }

    fun getAllCategories(): List<String> {
        return try {
            if (!categoriesFile.exists()) {
                // Return default categories if file doesn't exist
                return mutableListOf(DEFAULT_CATEGORY)
            }

            val json = categoriesFile.readText()
            val type = object : TypeToken<List<String>>() {}.type
            val categories: List<String> = gson.fromJson(json, type) ?: emptyList()

            // Always include default category
            val allCategories = categories.toMutableList()
            if (!allCategories.contains(DEFAULT_CATEGORY)) {
                allCategories.add(0, DEFAULT_CATEGORY)
            }

            allCategories
        } catch (e: Exception) {
            Log.e(TAG, "Error reading categories", e)
            mutableListOf(DEFAULT_CATEGORY)
        }
    }

    fun addCategory(categoryName: String): Boolean {
        return try {
            if (categoryName.isBlank()) {
                return false
            }

            val categories = getAllCategories().toMutableList()

            // Check if category already exists (case-insensitive)
            if (categories.any { it.equals(categoryName, ignoreCase = true) }) {
                Log.w(TAG, "Category already exists: $categoryName")
                return false
            }

            categories.add(categoryName)
            saveCategories(categories)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding category", e)
            false
        }
    }

    fun deleteCategory(categoryName: String): Boolean {
        return try {
            // Don't allow deleting the default category
            if (categoryName == DEFAULT_CATEGORY) {
                return false
            }

            val categories = getAllCategories().toMutableList()
            val removed = categories.remove(categoryName)

            if (removed) {
                saveCategories(categories)
            }

            removed
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting category", e)
            false
        }
    }

    fun renameCategory(oldName: String, newName: String): Boolean {
        return try {
            if (newName.isBlank() || oldName == DEFAULT_CATEGORY) {
                return false
            }

            val categories = getAllCategories().toMutableList()
            val index = categories.indexOf(oldName)

            if (index != -1) {
                categories[index] = newName
                saveCategories(categories)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error renaming category", e)
            false
        }
    }

    private fun saveCategories(categories: List<String>) {
        try {
            // Remove default category from saved list (it's always added on load)
            val categoriesToSave = categories.filter { it != DEFAULT_CATEGORY }
            val json = gson.toJson(categoriesToSave)
            categoriesFile.writeText(json)
            Log.d(TAG, "Categories saved successfully: ${categoriesToSave.joinToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving categories", e)
            throw e
        }
    }

    fun getCategoriesFromNotes(notes: List<com.iliass.iliass.model.Note>): List<String> {
        // Extract unique categories from notes
        val noteCategories = notes.mapNotNull {
            if (it.category.isNotBlank()) it.category else null
        }.distinct()

        // Merge with saved categories
        val allCategories = (getAllCategories() + noteCategories).distinct().sorted()
        return allCategories
    }
}
