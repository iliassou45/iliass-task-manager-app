package com.iliass.iliass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.model.Note
import com.iliass.iliass.ui.adapter.NotesAdapter
import com.iliass.iliass.util.NoteManager
import com.iliass.iliass.util.CategoryManager
import java.io.File

class NotesListActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var searchButton: ImageButton
    private lateinit var newNoteButton: Button
    private lateinit var importButton: Button
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var searchBar: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var closeSearchButton: ImageButton
    private lateinit var emptyStateText: TextView
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var manageCategoriesButton: ImageButton
    private lateinit var noteManager: NoteManager
    private lateinit var categoryManager: CategoryManager
    private lateinit var adapter: NotesAdapter

    private var allNotes: List<Note> = emptyList()
    private var filteredNotes: List<Note> = emptyList()
    private var categories: MutableList<String> = mutableListOf()
    private var isSearchVisible = false
    private var selectedCategory: String = "All"

    companion object {
        private const val IMPORT_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes_list)

        noteManager = NoteManager(this)
        categoryManager = CategoryManager(this)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadCategories()
        loadNotes()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        searchButton = findViewById(R.id.searchButton)
        newNoteButton = findViewById(R.id.newNoteButton)
        importButton = findViewById(R.id.importButton)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        searchBar = findViewById(R.id.searchBar)
        searchInput = findViewById(R.id.searchInput)
        closeSearchButton = findViewById(R.id.closeSearchButton)
        emptyStateText = findViewById(R.id.emptyStateText)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        manageCategoriesButton = findViewById(R.id.manageCategoriesButton)
    }

    private fun setupRecyclerView() {
        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = NotesAdapter(emptyList()) { note ->
            openNoteForEditing(note)
        }
        notesRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            if (isSearchVisible) {
                toggleSearch()
            } else {
                finish()
            }
        }

        searchButton.setOnClickListener {
            toggleSearch()
        }

        closeSearchButton.setOnClickListener {
            toggleSearch()
        }

        newNoteButton.setOnClickListener {
            startActivity(Intent(this, NoteActivity::class.java))
        }

        importButton.setOnClickListener {
            openFilePicker()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterNotes(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        manageCategoriesButton.setOnClickListener {
            showManageCategoriesDialog()
        }
    }

    private fun loadCategories() {
        categories.clear()
        categories.add("All")
        categories.addAll(categoryManager.getCategoriesFromNotes(allNotes))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = adapter

        // Restore selected category
        val selectedIndex = categories.indexOf(selectedCategory)
        if (selectedIndex >= 0) {
            categoryFilterSpinner.setSelection(selectedIndex)
        }
    }

    private fun applyFilters() {
        // Apply category filter
        filteredNotes = if (selectedCategory == "All") {
            allNotes
        } else {
            allNotes.filter { note ->
                val noteCategory = note.category ?: ""
                noteCategory == selectedCategory || (noteCategory.isEmpty() && selectedCategory == CategoryManager.DEFAULT_CATEGORY)
            }
        }

        // Apply search filter if search is active
        if (isSearchVisible && searchInput.text.isNotEmpty()) {
            val query = searchInput.text.toString()
            filteredNotes = filteredNotes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
            }
        }

        adapter.updateNotes(filteredNotes)
        updateEmptyState()
    }

    private fun showManageCategoriesDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.select_dialog_item, null)
        val options = arrayOf("Add Category", "Delete Category")

        AlertDialog.Builder(this)
            .setTitle("Manage Categories")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddCategoryDialog()
                    1 -> showDeleteCategoryDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "Category name"

        AlertDialog.Builder(this)
            .setTitle("Add New Category")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val categoryName = input.text.toString().trim()
                if (categoryName.isNotEmpty()) {
                    if (categoryManager.addCategory(categoryName)) {
                        loadCategories()
                        Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteCategoryDialog() {
        val categoryList = categoryManager.getAllCategories()
            .filter { it != CategoryManager.DEFAULT_CATEGORY }
            .toTypedArray()

        if (categoryList.isEmpty()) {
            Toast.makeText(this, "No categories to delete", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setItems(categoryList) { _, which ->
                val categoryToDelete = categoryList[which]
                AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Delete category \"$categoryToDelete\"?\n\nNotes in this category will be moved to \"${CategoryManager.DEFAULT_CATEGORY}\".")
                    .setPositiveButton("Delete") { _, _ ->
                        // Update notes in this category to default
                        val notesToUpdate = allNotes.filter { it.category == categoryToDelete }
                        notesToUpdate.forEach { note ->
                            noteManager.saveNote(note.copy(category = CategoryManager.DEFAULT_CATEGORY))
                        }

                        // Delete the category
                        if (categoryManager.deleteCategory(categoryToDelete)) {
                            loadNotes()
                            loadCategories()
                            Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible

        if (isSearchVisible) {
            searchBar.visibility = View.VISIBLE
            searchButton.visibility = View.GONE
            searchInput.requestFocus()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        } else {
            searchBar.visibility = View.GONE
            searchButton.visibility = View.VISIBLE
            searchInput.text.clear()
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
            applyFilters()
        }
    }

    private fun filterNotes(query: String) {
        applyFilters()
    }

    private fun loadNotes() {
        allNotes = noteManager.getAllNotes()
        loadCategories()
        applyFilters()
    }

    private fun updateEmptyState() {
        if (allNotes.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            emptyStateText.text = "No notes yet.\nTap 'New Note' to create one!"
        } else {
            emptyStateText.visibility = View.GONE
        }
    }

    private fun openNoteForEditing(note: Note) {
        startActivity(Intent(this, NoteActivity::class.java).apply {
            putExtra("note", note)
        })
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/*", "*/*"))
        }
        startActivityForResult(intent, IMPORT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMPORT_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { uri ->
                importNoteFromUri(uri)
            }
        }
    }

    private fun importNoteFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val content = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                val importedNote = parseIliassFormat(content)
                if (importedNote != null) {
                    if (noteManager.saveNote(importedNote)) {
                        Toast.makeText(
                            this,
                            "Note imported successfully: ${importedNote.title}",
                            Toast.LENGTH_LONG
                        ).show()
                        loadNotes()
                    } else {
                        Toast.makeText(this, "Failed to save imported note", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Invalid note format. Make sure it's a .iliass file",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error importing file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun parseIliassFormat(content: String): Note? {
        return try {
            Log.d("NotesListActivity", "========== PARSING ILIASS FORMAT ==========")
            Log.d("NotesListActivity", "Total content length: ${content.length}")
            Log.d("NotesListActivity", "Content preview: ${content.take(200)}")

            // Find the format header
            if (!content.contains("ILIASS_NOTE_FORMAT")) {
                Log.d("NotesListActivity", "Invalid format: missing ILIASS_NOTE_FORMAT header")
                return null
            }

            var id = ""
            var title = ""
            var createdAt = System.currentTimeMillis()
            var updatedAt = System.currentTimeMillis()
            var color = 0xFF4CAF50.toInt()
            var category = ""
            var plainContent = ""
            var formattedContent = ""

            // Parse line by line
            var currentSection = ""
            val contentLines = mutableListOf<String>()
            val formattedLines = mutableListOf<String>()

            var i = 0
            val lines = content.split("\n")

            while (i < lines.size) {
                val line = lines[i]
                Log.d("NotesListActivity", "Line $i: '$line'")

                when {
                    line.startsWith("ID:") -> {
                        id = line.substring(3)
                        Log.d("NotesListActivity", "Found ID: $id")
                    }
                    line.startsWith("TITLE:") -> {
                        title = line.substring(6)
                        Log.d("NotesListActivity", "Found TITLE: $title")
                    }
                    line.startsWith("CREATED_AT:") -> {
                        createdAt = line.substring(11).toLongOrNull() ?: System.currentTimeMillis()
                        Log.d("NotesListActivity", "Found CREATED_AT: $createdAt")
                    }
                    line.startsWith("UPDATED_AT:") -> {
                        updatedAt = line.substring(11).toLongOrNull() ?: System.currentTimeMillis()
                        Log.d("NotesListActivity", "Found UPDATED_AT: $updatedAt")
                    }
                    line.startsWith("COLOR:") -> {
                        color = line.substring(6).toLongOrNull()?.toInt() ?: 0xFF4CAF50.toInt()
                        Log.d("NotesListActivity", "Found COLOR: ${String.format("%08X", color)}")
                    }
                    line.startsWith("CATEGORY:") -> {
                        category = line.substring(9)
                        Log.d("NotesListActivity", "Found CATEGORY: $category")
                    }
                    line == "CONTENT:" -> {
                        currentSection = "CONTENT"
                        Log.d("NotesListActivity", "Entering CONTENT section")
                    }
                    line == "FORMATTED_CONTENT:" -> {
                        currentSection = "FORMATTED_CONTENT"
                        Log.d("NotesListActivity", "Entering FORMATTED_CONTENT section")
                    }
                    line == "---END---" -> {
                        Log.d("NotesListActivity", "Reached END marker")
                        break
                    }
                    line == "---" -> {
                        if (currentSection == "CONTENT") {
                            plainContent = contentLines.joinToString("\n")
                            Log.d("NotesListActivity", "Finished CONTENT section, length: ${plainContent.length}")
                            Log.d("NotesListActivity", "Plain content: '$plainContent'")
                            contentLines.clear()
                            currentSection = ""
                        } else if (currentSection == "FORMATTED_CONTENT") {
                            formattedContent = formattedLines.joinToString("\n")
                            Log.d("NotesListActivity", "Finished FORMATTED_CONTENT section, length: ${formattedContent.length}")
                            Log.d("NotesListActivity", "Formatted content: '$formattedContent'")
                            formattedLines.clear()
                            currentSection = ""
                        }
                    }
                    currentSection == "CONTENT" -> {
                        contentLines.add(line)
                    }
                    currentSection == "FORMATTED_CONTENT" -> {
                        formattedLines.add(line)
                    }
                }
                i++
            }

            // Validate
            if (title.isEmpty() || plainContent.isEmpty()) {
                Log.d("NotesListActivity", "Invalid: missing title or content")
                return null
            }

            Log.d("NotesListActivity", "Creating note: id=$id, title=$title, contentLength=${plainContent.length}, formattedLength=${formattedContent.length}")

            val note = Note(
                id = id,
                title = title,
                content = plainContent,
                contentWithFormatting = formattedContent,
                createdAt = createdAt,
                updatedAt = updatedAt,
                color = color,
                category = category
            )

            Log.d("NotesListActivity", "========== PARSING COMPLETE ==========")
            note
        } catch (e: Exception) {
            Log.e("NotesListActivity", "Error parsing ILIASS format: ${e.message}", e)
            null
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
    }
}