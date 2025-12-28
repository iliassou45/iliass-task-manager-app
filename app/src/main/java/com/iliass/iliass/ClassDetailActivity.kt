package com.iliass.iliass

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.iliass.iliass.adapter.LessonAdapter
import com.iliass.iliass.adapter.StudentCheckboxAdapter
import com.iliass.iliass.model.Lesson
import com.iliass.iliass.model.StudentClass
import com.iliass.iliass.repository.StudentDatabase
import com.iliass.iliass.util.PDFManager
import java.text.DecimalFormat

class ClassDetailActivity : AppCompatActivity() {

    private lateinit var database: StudentDatabase
    private lateinit var studentClass: StudentClass
    private lateinit var lessonAdapter: LessonAdapter

    private lateinit var classNameText: TextView
    private lateinit var classTypeText: TextView
    private lateinit var classTimeText: TextView
    private lateinit var classDaysText: TextView
    private lateinit var classNotesText: TextView

    private lateinit var studentCountText: TextView
    private lateinit var studentsLeftText: TextView
    private lateinit var monthlyRevenueText: TextView
    private lateinit var classDebtText: TextView
    private lateinit var lessonsCountText: TextView

    private lateinit var manageStudentsButton: Button
    private lateinit var editClassButton: Button
    private lateinit var fabAddLesson: FloatingActionButton
    private lateinit var lessonsRecyclerView: RecyclerView
    private lateinit var emptyLessonsView: TextView

    private val decimalFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_detail)

        database = StudentDatabase.getInstance(this)

        val classId = intent.getStringExtra("CLASS_ID")
        if (classId == null) {
            Toast.makeText(this, "Error: Class not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val foundClass = database.getClassById(classId)
        if (foundClass == null) {
            Toast.makeText(this, "Error: Class not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        studentClass = foundClass

        initializeViews()
        setupRecyclerView()
        setupButtons()
        updateUI()
    }

    private fun initializeViews() {
        classNameText = findViewById(R.id.classNameText)
        classTypeText = findViewById(R.id.classTypeText)
        classTimeText = findViewById(R.id.classTimeText)
        classDaysText = findViewById(R.id.classDaysText)
        classNotesText = findViewById(R.id.classNotesText)

        studentCountText = findViewById(R.id.studentCountText)
        studentsLeftText = findViewById(R.id.studentsLeftText)
        monthlyRevenueText = findViewById(R.id.monthlyRevenueText)
        classDebtText = findViewById(R.id.classDebtText)
        lessonsCountText = findViewById(R.id.lessonsCountText)

        manageStudentsButton = findViewById(R.id.manageStudentsButton)
        editClassButton = findViewById(R.id.editClassButton)
        fabAddLesson = findViewById(R.id.fabAddLesson)
        lessonsRecyclerView = findViewById(R.id.lessonsRecyclerView)
        emptyLessonsView = findViewById(R.id.emptyLessonsView)
    }

    private fun setupRecyclerView() {
        lessonAdapter = LessonAdapter(
            emptyList(),
            onLessonClick = { lesson -> openLessonDetail(lesson) },
            onLessonLongClick = { lesson -> showDeleteLessonDialog(lesson); true }
        )
        lessonsRecyclerView.adapter = lessonAdapter
        lessonsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupButtons() {
        manageStudentsButton.setOnClickListener {
            showManageStudentsDialog()
        }

        editClassButton.setOnClickListener {
            val intent = Intent(this, AddEditClassActivity::class.java)
            intent.putExtra("CLASS_ID", studentClass.id)
            startActivity(intent)
        }

        fabAddLesson.setOnClickListener {
            val intent = Intent(this, AddEditLessonActivity::class.java)
            intent.putExtra("CLASS_ID", studentClass.id)
            startActivity(intent)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun updateUI() {
        // Reload class data
        database.getClassById(studentClass.id)?.let {
            studentClass = it
        }

        supportActionBar?.title = studentClass.name

        // Class info
        classNameText.text = studentClass.name
        classTypeText.text = "Type: ${studentClass.type.displayName}"
        classTimeText.text = "Time: ${studentClass.startTime}"
        classDaysText.text = "Days: ${studentClass.days.joinToString(", ")}"

        if (studentClass.notes.isNotEmpty()) {
            classNotesText.text = studentClass.notes
            classNotesText.visibility = View.VISIBLE
        } else {
            classNotesText.visibility = View.GONE
        }

        // Statistics
        studentCountText.text = studentClass.getCurrentStudentCount().toString()
        studentsLeftText.text = studentClass.getStudentsLeftCount(database).toString()
        monthlyRevenueText.text = "$${decimalFormat.format(studentClass.getMonthlyRevenuePotential(database))}"
        classDebtText.text = "$${decimalFormat.format(studentClass.getTotalClassDebt(database))}"

        // Lessons
        val lessons = database.getLessonsByClass(studentClass.id)
        lessonAdapter.updateLessons(lessons)
        lessonsCountText.text = "${lessons.size} lessons (${database.getCompletedLessonsCount(studentClass.id)} completed)"

        if (lessons.isEmpty()) {
            emptyLessonsView.visibility = View.VISIBLE
        } else {
            emptyLessonsView.visibility = View.GONE
        }
    }

    private fun showManageStudentsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manage_students, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.studentsRecyclerView)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        val allStudents = database.getActiveStudents()
        val selectedStudentIds = studentClass.studentIds.toMutableSet()

        val adapter = StudentCheckboxAdapter(allStudents, selectedStudentIds)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            // Find students to add and remove
            val currentStudentIds = studentClass.studentIds.toSet()
            val studentsToAdd = selectedStudentIds - currentStudentIds
            val studentsToRemove = currentStudentIds - selectedStudentIds

            // Add new students
            studentsToAdd.forEach { studentId ->
                database.addStudentToClass(studentClass.id, studentId)
            }

            // Remove students
            studentsToRemove.forEach { studentId ->
                database.removeStudentFromClass(studentClass.id, studentId)
            }

            Toast.makeText(this, "Students updated successfully", Toast.LENGTH_SHORT).show()
            updateUI()
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun openLessonDetail(lesson: Lesson) {
        // If lesson has a PDF, show options dialog
        if (PDFManager.pdfExists(lesson.pdfFilePath)) {
            val options = arrayOf("View PDF", "Edit Lesson")
            AlertDialog.Builder(this)
                .setTitle(lesson.title)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> openPDFViewer(lesson)
                        1 -> openLessonEditor(lesson)
                    }
                }
                .show()
        } else {
            openLessonEditor(lesson)
        }
    }

    private fun openPDFViewer(lesson: Lesson) {
        val intent = Intent(this, PDFViewerActivity::class.java)
        intent.putExtra("PDF_PATH", lesson.pdfFilePath)
        intent.putExtra("LESSON_TITLE", lesson.title)
        startActivity(intent)
    }

    private fun openLessonEditor(lesson: Lesson) {
        val intent = Intent(this, AddEditLessonActivity::class.java)
        intent.putExtra("CLASS_ID", studentClass.id)
        intent.putExtra("LESSON_ID", lesson.id)
        startActivity(intent)
    }

    private fun showDeleteLessonDialog(lesson: Lesson) {
        AlertDialog.Builder(this)
            .setTitle("Delete Lesson")
            .setMessage("Are you sure you want to delete \"${lesson.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                database.deleteLesson(lesson.id)
                Toast.makeText(this, "Lesson deleted", Toast.LENGTH_SHORT).show()
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
