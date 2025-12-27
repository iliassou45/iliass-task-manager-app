package com.iliass.iliass

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.Student
import com.iliass.iliass.repository.StudentDatabase

class AddEditStudentActivity : AppCompatActivity() {

    private lateinit var headerTitle: TextView
    private lateinit var studentNameInput: TextInputEditText
    private lateinit var monthlyAmountInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var locationInput: TextInputEditText
    private lateinit var timezoneOffsetInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var activeSwitch: SwitchCompat
    private lateinit var saveButton: Button

    private val studentDatabase by lazy { StudentDatabase.getInstance(this) }
    private var editingStudent: Student? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_student)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        loadStudentIfEditing()
        setupSaveButton()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initViews() {
        headerTitle = findViewById(R.id.headerTitle)
        studentNameInput = findViewById(R.id.studentNameInput)
        monthlyAmountInput = findViewById(R.id.monthlyAmountInput)
        phoneInput = findViewById(R.id.phoneInput)
        emailInput = findViewById(R.id.emailInput)
        locationInput = findViewById(R.id.locationInput)
        timezoneOffsetInput = findViewById(R.id.timezoneOffsetInput)
        notesInput = findViewById(R.id.notesInput)
        activeSwitch = findViewById(R.id.activeSwitch)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadStudentIfEditing() {
        val studentId = intent.getStringExtra("STUDENT_ID")
        if (studentId != null) {
            editingStudent = studentDatabase.getStudentById(studentId)
            editingStudent?.let { student ->
                supportActionBar?.title = "Edit Student"
                headerTitle.text = "Edit Student"
                saveButton.text = "Update Student"

                studentNameInput.setText(student.name)
                monthlyAmountInput.setText(student.monthlyAmount.toString())
                phoneInput.setText(student.phone)
                emailInput.setText(student.email)
                locationInput.setText(student.location)
                if (student.timezoneOffsetHours != 0.0) {
                    timezoneOffsetInput.setText(student.timezoneOffsetHours.toString())
                }
                notesInput.setText(student.notes)
                activeSwitch.isChecked = student.isActive
            }
        } else {
            supportActionBar?.title = "Add Student"
            headerTitle.text = "Add Student"
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (validateInputs()) {
                saveStudent()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = studentNameInput.text.toString().trim()
        val monthlyAmountStr = monthlyAmountInput.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter student name", Toast.LENGTH_SHORT).show()
            return false
        }

        if (monthlyAmountStr.isEmpty()) {
            Toast.makeText(this, "Please enter monthly amount", Toast.LENGTH_SHORT).show()
            return false
        }

        val monthlyAmount = monthlyAmountStr.toDoubleOrNull()
        if (monthlyAmount == null || monthlyAmount <= 0) {
            Toast.makeText(this, "Please enter a valid monthly amount", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun saveStudent() {
        val name = studentNameInput.text.toString().trim()
        val monthlyAmount = monthlyAmountInput.text.toString().toDouble()
        val phone = phoneInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val timezoneOffsetStr = timezoneOffsetInput.text.toString().trim()
        val timezoneOffset = timezoneOffsetStr.toDoubleOrNull() ?: 0.0
        val notes = notesInput.text.toString().trim()
        val isActive = activeSwitch.isChecked

        val student = if (editingStudent != null) {
            editingStudent!!.copy(
                name = name,
                monthlyAmount = monthlyAmount,
                phone = phone,
                email = email,
                location = location,
                timezoneOffsetHours = timezoneOffset,
                notes = notes,
                isActive = isActive
            )
        } else {
            Student(
                name = name,
                monthlyAmount = monthlyAmount,
                phone = phone,
                email = email,
                location = location,
                timezoneOffsetHours = timezoneOffset,
                notes = notes,
                isActive = isActive
            )
        }

        if (editingStudent != null) {
            studentDatabase.updateStudent(student)
            Toast.makeText(this, "Student updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            studentDatabase.addStudent(student)
            Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
