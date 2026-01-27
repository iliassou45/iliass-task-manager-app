package com.iliass.iliass

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.DailyTask
import com.iliass.iliass.model.TaskCategory
import com.iliass.iliass.model.TaskPriority
import com.iliass.iliass.util.TaskManager
import java.text.SimpleDateFormat
import java.util.*

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var taskManager: TaskManager

    private lateinit var editTitle: TextInputEditText
    private lateinit var editDescription: TextInputEditText
    private lateinit var editNotes: TextInputEditText
    private lateinit var radioGroupPriority: RadioGroup
    private lateinit var spinnerCategory: Spinner
    private lateinit var textSelectedDate: TextView
    private lateinit var textSelectedTime: TextView
    private lateinit var btnClearTime: ImageButton
    private lateinit var textDuration: TextView
    private lateinit var switchReminder: SwitchCompat
    private lateinit var reminderOptionsLayout: LinearLayout
    private lateinit var textReminderTime: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var headerTitle: TextView

    private var selectedDueDate: Long = System.currentTimeMillis()
    private var selectedDueTime: Long? = null
    private var selectedReminderTime: Long? = null
    private var estimatedMinutes: Int = 30
    private var editingTaskId: String? = null
    private var isViewMode: Boolean = false

    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_task)

        taskManager = TaskManager.getInstance(this)

        initViews()
        setupCategorySpinner()
        setupClickListeners()

        // Check if editing existing task
        editingTaskId = intent.getStringExtra("task_id")
        isViewMode = intent.getBooleanExtra("view_mode", false)

        if (editingTaskId != null) {
            loadExistingTask(editingTaskId!!)
        } else {
            // Set default date to today
            updateDateDisplay()
        }
    }

    private fun initViews() {
        editTitle = findViewById(R.id.editTitle)
        editDescription = findViewById(R.id.editDescription)
        editNotes = findViewById(R.id.editNotes)
        radioGroupPriority = findViewById(R.id.radioGroupPriority)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        textSelectedDate = findViewById(R.id.textSelectedDate)
        textSelectedTime = findViewById(R.id.textSelectedTime)
        btnClearTime = findViewById(R.id.btnClearTime)
        textDuration = findViewById(R.id.textDuration)
        switchReminder = findViewById(R.id.switchReminder)
        reminderOptionsLayout = findViewById(R.id.reminderOptionsLayout)
        textReminderTime = findViewById(R.id.textReminderTime)
        btnSave = findViewById(R.id.btnSave)
        headerTitle = findViewById(R.id.headerTitle)
    }

    private fun setupCategorySpinner() {
        val categories = TaskCategory.values().map { "${it.emoji} ${it.displayName}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        textSelectedDate.setOnClickListener {
            showDatePicker()
        }

        textSelectedTime.setOnClickListener {
            showTimePicker()
        }

        btnClearTime.setOnClickListener {
            selectedDueTime = null
            textSelectedTime.text = "No specific time"
            btnClearTime.visibility = View.GONE
        }

        // Duration controls
        findViewById<MaterialButton>(R.id.btnDecreaseDuration).setOnClickListener {
            if (estimatedMinutes > 5) {
                estimatedMinutes -= if (estimatedMinutes <= 15) 5 else if (estimatedMinutes <= 60) 15 else 30
                updateDurationDisplay()
            }
        }

        findViewById<MaterialButton>(R.id.btnIncreaseDuration).setOnClickListener {
            estimatedMinutes += if (estimatedMinutes < 15) 5 else if (estimatedMinutes < 60) 15 else 30
            updateDurationDisplay()
        }

        // Quick duration chips
        findViewById<Chip>(R.id.chip15min).setOnClickListener {
            estimatedMinutes = 15
            updateDurationDisplay()
        }
        findViewById<Chip>(R.id.chip30min).setOnClickListener {
            estimatedMinutes = 30
            updateDurationDisplay()
        }
        findViewById<Chip>(R.id.chip1hr).setOnClickListener {
            estimatedMinutes = 60
            updateDurationDisplay()
        }
        findViewById<Chip>(R.id.chip2hr).setOnClickListener {
            estimatedMinutes = 120
            updateDurationDisplay()
        }

        // Reminder toggle
        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            reminderOptionsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (isChecked && selectedReminderTime == null) {
                // Default to 30 minutes before due time
                setDefaultReminderTime()
            }
        }

        textReminderTime.setOnClickListener {
            showReminderTimePicker()
        }

        btnSave.setOnClickListener {
            saveTask()
        }
    }

    private fun loadExistingTask(taskId: String) {
        val task = taskManager.getTask(taskId) ?: return

        headerTitle.text = if (isViewMode) "📋 Task Details" else "✏️ Edit Task"
        btnSave.text = if (isViewMode) "Edit Task" else "Update Task"

        editTitle.setText(task.title)
        editDescription.setText(task.description)
        editNotes.setText(task.notes)

        // Priority
        when (task.priority) {
            TaskPriority.LOW -> findViewById<RadioButton>(R.id.radioLow).isChecked = true
            TaskPriority.MEDIUM -> findViewById<RadioButton>(R.id.radioMedium).isChecked = true
            TaskPriority.HIGH -> findViewById<RadioButton>(R.id.radioHigh).isChecked = true
            TaskPriority.URGENT -> findViewById<RadioButton>(R.id.radioUrgent).isChecked = true
        }

        // Category
        spinnerCategory.setSelection(task.category.ordinal)

        // Date and Time
        selectedDueDate = task.dueDate
        updateDateDisplay()

        selectedDueTime = task.dueTime
        if (selectedDueTime != null) {
            textSelectedTime.text = timeFormat.format(Date(selectedDueTime!!))
            btnClearTime.visibility = View.VISIBLE
        }

        // Duration
        estimatedMinutes = task.estimatedMinutes
        updateDurationDisplay()

        // Reminder
        switchReminder.isChecked = task.reminderEnabled
        if (task.reminderEnabled && task.reminderTime != null) {
            selectedReminderTime = task.reminderTime
            reminderOptionsLayout.visibility = View.VISIBLE
            textReminderTime.text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                .format(Date(task.reminderTime))
        }

        if (isViewMode) {
            // Disable editing in view mode
            btnSave.setOnClickListener {
                isViewMode = false
                headerTitle.text = "✏️ Edit Task"
                btnSave.text = "Update Task"
                enableEditing(true)
            }
            enableEditing(false)
        }
    }

    private fun enableEditing(enabled: Boolean) {
        editTitle.isEnabled = enabled
        editDescription.isEnabled = enabled
        editNotes.isEnabled = enabled
        radioGroupPriority.isEnabled = enabled
        for (i in 0 until radioGroupPriority.childCount) {
            radioGroupPriority.getChildAt(i).isEnabled = enabled
        }
        spinnerCategory.isEnabled = enabled
        textSelectedDate.isClickable = enabled
        textSelectedTime.isClickable = enabled
        switchReminder.isEnabled = enabled
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDueDate }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                selectedDueDate = calendar.timeInMillis
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDueTime != null) {
            calendar.timeInMillis = selectedDueTime!!
        }

        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                selectedDueTime = calendar.timeInMillis
                textSelectedTime.text = timeFormat.format(Date(selectedDueTime!!))
                btnClearTime.visibility = View.VISIBLE
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showReminderTimePicker() {
        val calendar = Calendar.getInstance()

        // First pick date
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)

                // Then pick time
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        selectedReminderTime = calendar.timeInMillis
                        textReminderTime.text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            .format(Date(selectedReminderTime!!))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setDefaultReminderTime() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDueTime ?: selectedDueDate
        calendar.add(Calendar.MINUTE, -30)
        selectedReminderTime = calendar.timeInMillis
        textReminderTime.text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            .format(Date(selectedReminderTime!!))
    }

    private fun updateDateDisplay() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDueDate }

        textSelectedDate.text = when {
            selectedCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            selectedCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
            selectedCal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
            selectedCal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
            else -> dateFormat.format(Date(selectedDueDate))
        }
    }

    private fun updateDurationDisplay() {
        textDuration.text = when {
            estimatedMinutes < 60 -> "$estimatedMinutes minutes"
            estimatedMinutes == 60 -> "1 hour"
            estimatedMinutes % 60 == 0 -> "${estimatedMinutes / 60} hours"
            else -> "${estimatedMinutes / 60}h ${estimatedMinutes % 60}min"
        }
    }

    private fun saveTask() {
        val title = editTitle.text?.toString()?.trim() ?: ""
        if (title.isEmpty()) {
            editTitle.error = "Title is required"
            editTitle.requestFocus()
            return
        }

        val description = editDescription.text?.toString()?.trim() ?: ""
        val notes = editNotes.text?.toString()?.trim() ?: ""

        val priority = when (radioGroupPriority.checkedRadioButtonId) {
            R.id.radioLow -> TaskPriority.LOW
            R.id.radioMedium -> TaskPriority.MEDIUM
            R.id.radioHigh -> TaskPriority.HIGH
            R.id.radioUrgent -> TaskPriority.URGENT
            else -> TaskPriority.MEDIUM
        }

        val category = TaskCategory.values()[spinnerCategory.selectedItemPosition]

        val task = if (editingTaskId != null) {
            val existingTask = taskManager.getTask(editingTaskId!!)
            existingTask?.copy(
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDate = selectedDueDate,
                dueTime = selectedDueTime,
                estimatedMinutes = estimatedMinutes,
                notes = notes,
                reminderEnabled = switchReminder.isChecked,
                reminderTime = if (switchReminder.isChecked) selectedReminderTime else null
            ) ?: createNewTask(title, description, category, priority, notes)
        } else {
            createNewTask(title, description, category, priority, notes)
        }

        taskManager.saveTask(task)

        Toast.makeText(
            this,
            if (editingTaskId != null) "Task updated!" else "Task created!",
            Toast.LENGTH_SHORT
        ).show()

        finish()
    }

    private fun createNewTask(
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        notes: String
    ): DailyTask {
        return DailyTask(
            title = title,
            description = description,
            category = category,
            priority = priority,
            dueDate = selectedDueDate,
            dueTime = selectedDueTime,
            estimatedMinutes = estimatedMinutes,
            notes = notes,
            reminderEnabled = switchReminder.isChecked,
            reminderTime = if (switchReminder.isChecked) selectedReminderTime else null
        )
    }
}
