package com.iliass.iliass

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.Lesson
import com.iliass.iliass.model.LessonType
import com.iliass.iliass.repository.StudentDatabase
import com.iliass.iliass.util.PDFManager

class AddEditLessonActivity : AppCompatActivity() {

    private lateinit var database: StudentDatabase
    private lateinit var lessonTitleInput: TextInputEditText
    private lateinit var lessonTypeSpinner: Spinner
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var completedSwitch: SwitchMaterial
    private lateinit var attachPDFButton: Button
    private lateinit var removePDFButton: Button
    private lateinit var pdfFileNameText: TextView
    private lateinit var saveButton: Button

    private var editingLesson: Lesson? = null
    private var classId: String = ""
    private var selectedPdfUri: Uri? = null
    private var existingPdfPath: String? = null

    companion object {
        private const val PDF_PICKER_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_lesson)

        database = StudentDatabase.getInstance(this)

        classId = intent.getStringExtra("CLASS_ID") ?: ""
        if (classId.isEmpty()) {
            Toast.makeText(this, "Error: Class not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initializeViews()
        setupLessonTypeSpinner()
        loadLessonIfEditing()
        setupButtons()
        updateTitle()
    }

    private fun initializeViews() {
        lessonTitleInput = findViewById(R.id.lessonTitleInput)
        lessonTypeSpinner = findViewById(R.id.lessonTypeSpinner)
        descriptionInput = findViewById(R.id.descriptionInput)
        notesInput = findViewById(R.id.notesInput)
        completedSwitch = findViewById(R.id.completedSwitch)
        attachPDFButton = findViewById(R.id.attachPDFButton)
        removePDFButton = findViewById(R.id.removePDFButton)
        pdfFileNameText = findViewById(R.id.pdfFileNameText)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun setupLessonTypeSpinner() {
        val lessonTypes = LessonType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, lessonTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        lessonTypeSpinner.adapter = adapter
    }

    private fun loadLessonIfEditing() {
        val lessonId = intent.getStringExtra("LESSON_ID")
        if (lessonId != null) {
            editingLesson = database.getLessonById(lessonId)
            editingLesson?.let { lesson ->
                lessonTitleInput.setText(lesson.title)

                val typeIndex = LessonType.values().indexOf(lesson.type)
                if (typeIndex >= 0) {
                    lessonTypeSpinner.setSelection(typeIndex)
                }

                descriptionInput.setText(lesson.description)
                notesInput.setText(lesson.notes)
                completedSwitch.isChecked = lesson.isCompleted

                if (lesson.pdfFilePath != null && PDFManager.pdfExists(lesson.pdfFilePath)) {
                    existingPdfPath = lesson.pdfFilePath
                    pdfFileNameText.text = "PDF attached (${PDFManager.getFileSize(lesson.pdfFilePath)})"
                    removePDFButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupButtons() {
        attachPDFButton.setOnClickListener {
            openPDFPicker()
        }

        removePDFButton.setOnClickListener {
            selectedPdfUri = null
            existingPdfPath = null
            pdfFileNameText.text = "No file attached"
            removePDFButton.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            saveLesson()
        }
    }

    private fun openPDFPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/pdf"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PDF_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PDF_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedPdfUri = uri
                val fileName = getFileName(uri)
                pdfFileNameText.text = fileName ?: "PDF selected"
                removePDFButton.visibility = View.VISIBLE
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun saveLesson() {
        val title = lessonTitleInput.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a lesson title", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedType = LessonType.values()[lessonTypeSpinner.selectedItemPosition]
        val description = descriptionInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()
        val isCompleted = completedSwitch.isChecked

        // Handle PDF file
        var pdfPath: String? = existingPdfPath

        if (editingLesson != null) {
            // Update existing lesson
            val lessonId = editingLesson!!.id

            // Save new PDF if selected
            if (selectedPdfUri != null) {
                // Delete old PDF if exists
                if (existingPdfPath != null) {
                    PDFManager.deletePDFFile(existingPdfPath!!)
                }
                pdfPath = PDFManager.savePDFFile(this, selectedPdfUri!!, lessonId)
            }

            val updatedLesson = editingLesson!!.copy(
                title = title,
                type = selectedType,
                description = description,
                notes = notes,
                isCompleted = isCompleted,
                pdfFilePath = pdfPath
            )
            database.updateLesson(updatedLesson)
            Toast.makeText(this, "Lesson updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            // Create new lesson
            val newLesson = Lesson(
                classId = classId,
                title = title,
                type = selectedType,
                description = description,
                notes = notes,
                isCompleted = isCompleted
            )

            // Save PDF if selected
            if (selectedPdfUri != null) {
                pdfPath = PDFManager.savePDFFile(this, selectedPdfUri!!, newLesson.id)
            }

            val lessonWithPdf = newLesson.copy(pdfFilePath = pdfPath)
            database.addLesson(lessonWithPdf)
            Toast.makeText(this, "Lesson created successfully", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun updateTitle() {
        supportActionBar?.title = if (editingLesson != null) "Edit Lesson" else "New Lesson"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
