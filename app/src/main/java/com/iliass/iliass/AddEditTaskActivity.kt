package com.iliass.iliass

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
    private lateinit var switchTimeInterval: SwitchCompat
    private lateinit var timeIntervalLayout: LinearLayout
    private lateinit var textStartTime: TextView
    private lateinit var textEndTime: TextView
    private lateinit var textConflictWarning: TextView
    private lateinit var durationLayout: LinearLayout
    private lateinit var textDuration: TextView
    private lateinit var switchReminder: SwitchCompat
    private lateinit var reminderOptionsLayout: LinearLayout
    private lateinit var textReminderTime: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var headerTitle: TextView

    private var selectedDueDate: Long = System.currentTimeMillis()
    private var selectedStartTime: Long? = null
    private var selectedEndTime: Long? = null
    private var hasTimeInterval: Boolean = false
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
            updateDurationDisplay()
        }
    }

    private fun initViews() {
        editTitle = findViewById(R.id.editTitle)
        editDescription = findViewById(R.id.editDescription)
        editNotes = findViewById(R.id.editNotes)
        radioGroupPriority = findViewById(R.id.radioGroupPriority)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        textSelectedDate = findViewById(R.id.textSelectedDate)
        switchTimeInterval = findViewById(R.id.switchTimeInterval)
        timeIntervalLayout = findViewById(R.id.timeIntervalLayout)
        textStartTime = findViewById(R.id.textStartTime)
        textEndTime = findViewById(R.id.textEndTime)
        textConflictWarning = findViewById(R.id.textConflictWarning)
        durationLayout = findViewById(R.id.durationLayout)
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
            if (!isViewMode) showDatePicker()
        }

        // Time interval toggle
        switchTimeInterval.setOnCheckedChangeListener { _, isChecked ->
            hasTimeInterval = isChecked
            timeIntervalLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            durationLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
            textConflictWarning.visibility = View.GONE

            if (isChecked && selectedStartTime == null) {
                // Set default times: current hour to current hour + 1
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                selectedStartTime = calendar.timeInMillis

                calendar.add(Calendar.HOUR_OF_DAY, 1)
                selectedEndTime = calendar.timeInMillis

                updateTimeIntervalDisplay()
            }
        }

        textStartTime.setOnClickListener {
            if (!isViewMode) showStartTimePicker()
        }

        textEndTime.setOnClickListener {
            if (!isViewMode) showEndTimePicker()
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
                setDefaultReminderTime()
            }
        }

        textReminderTime.setOnClickListener {
            if (!isViewMode) showReminderTimePicker()
        }

        btnSave.setOnClickListener {
            if (isViewMode) {
                // Switch to edit mode
                isViewMode = false
                headerTitle.text = "✏️ Edit Task"
                btnSave.text = "Update Task"
                enableEditing(true)
            } else {
                // Save the task
                saveTask()
            }
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

        // Date
        selectedDueDate = task.dueDate
        updateDateDisplay()

        // Time Interval
        hasTimeInterval = task.hasTimeInterval
        switchTimeInterval.isChecked = hasTimeInterval
        if (hasTimeInterval) {
            selectedStartTime = task.startTime
            selectedEndTime = task.endTime
            timeIntervalLayout.visibility = View.VISIBLE
            durationLayout.visibility = View.GONE
            updateTimeIntervalDisplay()
        } else {
            timeIntervalLayout.visibility = View.GONE
            durationLayout.visibility = View.VISIBLE
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
            enableEditing(false)
        }
    }

    private fun enableEditing(enabled: Boolean) {
        editTitle.isEnabled = enabled
        editDescription.isEnabled = enabled
        editNotes.isEnabled = enabled

        for (i in 0 until radioGroupPriority.childCount) {
            radioGroupPriority.getChildAt(i).isEnabled = enabled
        }

        spinnerCategory.isEnabled = enabled
        switchTimeInterval.isEnabled = enabled
        switchReminder.isEnabled = enabled

        // Duration buttons
        findViewById<MaterialButton>(R.id.btnDecreaseDuration).isEnabled = enabled
        findViewById<MaterialButton>(R.id.btnIncreaseDuration).isEnabled = enabled
        findViewById<Chip>(R.id.chip15min).isEnabled = enabled
        findViewById<Chip>(R.id.chip30min).isEnabled = enabled
        findViewById<Chip>(R.id.chip1hr).isEnabled = enabled
        findViewById<Chip>(R.id.chip2hr).isEnabled = enabled
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
                checkTimeConflicts()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showStartTimePicker() {
        val calendar = Calendar.getInstance()
        if (selectedStartTime != null) {
            calendar.timeInMillis = selectedStartTime!!
        }

        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                selectedStartTime = calendar.timeInMillis

                // Auto-set end time if not set or if it's before start time
                if (selectedEndTime == null || selectedEndTime!! <= selectedStartTime!!) {
                    calendar.add(Calendar.HOUR_OF_DAY, 1)
                    selectedEndTime = calendar.timeInMillis
                }

                updateTimeIntervalDisplay()
                checkTimeConflicts()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showEndTimePicker() {
        val calendar = Calendar.getInstance()
        if (selectedEndTime != null) {
            calendar.timeInMillis = selectedEndTime!!
        }

        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                val newEndTime = calendar.timeInMillis

                // Validate end time is after start time
                if (selectedStartTime != null && newEndTime <= selectedStartTime!!) {
                    Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                selectedEndTime = newEndTime
                updateTimeIntervalDisplay()
                checkTimeConflicts()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateTimeIntervalDisplay() {
        if (selectedStartTime != null) {
            textStartTime.text = timeFormat.format(Date(selectedStartTime!!))
        } else {
            textStartTime.text = "Select start time"
        }

        if (selectedEndTime != null) {
            textEndTime.text = timeFormat.format(Date(selectedEndTime!!))
        } else {
            textEndTime.text = "Select end time"
        }

        // Calculate and show duration
        if (selectedStartTime != null && selectedEndTime != null) {
            val durationMinutes = ((selectedEndTime!! - selectedStartTime!!) / (1000 * 60)).toInt()
            estimatedMinutes = durationMinutes
        }
    }

    private fun checkTimeConflicts() {
        if (!hasTimeInterval || selectedStartTime == null || selectedEndTime == null) {
            textConflictWarning.visibility = View.GONE
            return
        }

        val conflicts = taskManager.checkTimeConflicts(
            selectedDueDate,
            selectedStartTime!!,
            selectedEndTime!!,
            editingTaskId
        )

        if (conflicts.isNotEmpty()) {
            val conflictMessages = conflicts.joinToString("\n") { "⚠️ ${it.message}" }
            textConflictWarning.text = conflictMessages
            textConflictWarning.visibility = View.VISIBLE
        } else {
            textConflictWarning.visibility = View.GONE
        }
    }

    private fun showReminderTimePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)

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
        calendar.timeInMillis = selectedStartTime ?: selectedDueDate
        calendar.add(Calendar.MINUTE, -15)
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

        // Validate time interval
        if (hasTimeInterval) {
            if (selectedStartTime == null || selectedEndTime == null) {
                Toast.makeText(this, "Please select both start and end times", Toast.LENGTH_SHORT).show()
                return
            }

            // Check for conflicts
            val conflicts = taskManager.checkTimeConflicts(
                selectedDueDate,
                selectedStartTime!!,
                selectedEndTime!!,
                editingTaskId
            )

            if (conflicts.isNotEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Time Conflict")
                    .setMessage("This time slot conflicts with existing tasks:\n\n${conflicts.joinToString("\n") { it.message }}\n\nDo you want to save anyway?")
                    .setPositiveButton("Save Anyway") { _, _ -> performSave() }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }
        }

        performSave()
    }

    private fun performSave() {
        val title = editTitle.text?.toString()?.trim() ?: ""
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
                startTime = if (hasTimeInterval) selectedStartTime else null,
                endTime = if (hasTimeInterval) selectedEndTime else null,
                hasTimeInterval = hasTimeInterval,
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
            startTime = if (hasTimeInterval) selectedStartTime else null,
            endTime = if (hasTimeInterval) selectedEndTime else null,
            hasTimeInterval = hasTimeInterval,
            estimatedMinutes = estimatedMinutes,
            notes = notes,
            reminderEnabled = switchReminder.isChecked,
            reminderTime = if (switchReminder.isChecked) selectedReminderTime else null
        )
    }
}
