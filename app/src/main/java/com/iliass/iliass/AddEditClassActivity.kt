package com.iliass.iliass

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.ClassType
import com.iliass.iliass.model.StudentClass
import com.iliass.iliass.repository.StudentDatabase
import java.util.*

class AddEditClassActivity : AppCompatActivity() {

    private lateinit var database: StudentDatabase
    private lateinit var classNameInput: TextInputEditText
    private lateinit var classTypeSpinner: Spinner
    private lateinit var startTimeInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var activeSwitch: SwitchMaterial
    private lateinit var saveButton: Button

    private lateinit var checkMonday: CheckBox
    private lateinit var checkTuesday: CheckBox
    private lateinit var checkWednesday: CheckBox
    private lateinit var checkThursday: CheckBox
    private lateinit var checkFriday: CheckBox
    private lateinit var checkSaturday: CheckBox
    private lateinit var checkSunday: CheckBox

    private var editingClass: StudentClass? = null
    private var selectedHour = 9
    private var selectedMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_class)

        database = StudentDatabase.getInstance(this)

        initializeViews()
        setupClassTypeSpinner()
        setupTimeInput()
        loadClassIfEditing()
        setupSaveButton()

        updateTitle()
    }

    private fun initializeViews() {
        classNameInput = findViewById(R.id.classNameInput)
        classTypeSpinner = findViewById(R.id.classTypeSpinner)
        startTimeInput = findViewById(R.id.startTimeInput)
        notesInput = findViewById(R.id.notesInput)
        activeSwitch = findViewById(R.id.activeSwitch)
        saveButton = findViewById(R.id.saveButton)

        checkMonday = findViewById(R.id.checkMonday)
        checkTuesday = findViewById(R.id.checkTuesday)
        checkWednesday = findViewById(R.id.checkWednesday)
        checkThursday = findViewById(R.id.checkThursday)
        checkFriday = findViewById(R.id.checkFriday)
        checkSaturday = findViewById(R.id.checkSaturday)
        checkSunday = findViewById(R.id.checkSunday)
    }

    private fun setupClassTypeSpinner() {
        val classTypes = ClassType.values().map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, classTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        classTypeSpinner.adapter = adapter
    }

    private fun setupTimeInput() {
        startTimeInput.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                startTimeInput.setText(String.format("%02d:%02d", hourOfDay, minute))
            },
            selectedHour,
            selectedMinute,
            true
        )
        timePickerDialog.show()
    }

    private fun loadClassIfEditing() {
        val classId = intent.getStringExtra("CLASS_ID")
        if (classId != null) {
            editingClass = database.getClassById(classId)
            editingClass?.let { studentClass ->
                classNameInput.setText(studentClass.name)

                val typeIndex = ClassType.values().indexOf(studentClass.type)
                if (typeIndex >= 0) {
                    classTypeSpinner.setSelection(typeIndex)
                }

                startTimeInput.setText(studentClass.startTime)
                val timeParts = studentClass.startTime.split(":")
                if (timeParts.size == 2) {
                    selectedHour = timeParts[0].toIntOrNull() ?: 9
                    selectedMinute = timeParts[1].toIntOrNull() ?: 0
                }

                notesInput.setText(studentClass.notes)
                activeSwitch.isChecked = studentClass.isActive

                // Set days
                checkMonday.isChecked = "Monday" in studentClass.days
                checkTuesday.isChecked = "Tuesday" in studentClass.days
                checkWednesday.isChecked = "Wednesday" in studentClass.days
                checkThursday.isChecked = "Thursday" in studentClass.days
                checkFriday.isChecked = "Friday" in studentClass.days
                checkSaturday.isChecked = "Saturday" in studentClass.days
                checkSunday.isChecked = "Sunday" in studentClass.days
            }
        }
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            saveClass()
        }
    }

    private fun saveClass() {
        val name = classNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a class name", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedType = ClassType.values()[classTypeSpinner.selectedItemPosition]
        val startTime = startTimeInput.text.toString().trim()
        if (startTime.isEmpty()) {
            Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show()
            return
        }

        val days = mutableListOf<String>()
        if (checkMonday.isChecked) days.add("Monday")
        if (checkTuesday.isChecked) days.add("Tuesday")
        if (checkWednesday.isChecked) days.add("Wednesday")
        if (checkThursday.isChecked) days.add("Thursday")
        if (checkFriday.isChecked) days.add("Friday")
        if (checkSaturday.isChecked) days.add("Saturday")
        if (checkSunday.isChecked) days.add("Sunday")

        if (days.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        val notes = notesInput.text.toString().trim()
        val isActive = activeSwitch.isChecked

        if (editingClass != null) {
            // Update existing class
            val updatedClass = editingClass!!.copy(
                name = name,
                type = selectedType,
                startTime = startTime,
                days = days,
                notes = notes,
                isActive = isActive
            )
            database.updateClass(updatedClass)
            Toast.makeText(this, "Class updated successfully", Toast.LENGTH_SHORT).show()
        } else {
            // Create new class
            val newClass = StudentClass(
                name = name,
                type = selectedType,
                startTime = startTime,
                days = days,
                notes = notes,
                isActive = isActive
            )
            database.addClass(newClass)
            Toast.makeText(this, "Class created successfully", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun updateTitle() {
        supportActionBar?.title = if (editingClass != null) "Edit Class" else "New Class"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
