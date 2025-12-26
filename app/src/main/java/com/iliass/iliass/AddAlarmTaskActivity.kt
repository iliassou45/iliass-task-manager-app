package com.iliass.iliass

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.iliass.iliass.model.AlarmTask
import com.iliass.iliass.model.RepeatInterval
import com.iliass.iliass.util.AlarmTaskManager
import java.text.SimpleDateFormat
import java.util.*

class AddAlarmTaskActivity : AppCompatActivity() {

    private lateinit var editTitle: TextInputEditText
    private lateinit var editDescription: TextInputEditText
    private lateinit var textSelectedDate: TextView
    private lateinit var textSelectedTime: TextView
    private lateinit var switchRepeat: SwitchCompat
    private lateinit var repeatOptionsLayout: LinearLayout
    private lateinit var radioGroupRepeat: RadioGroup
    private lateinit var radioDaily: RadioButton
    private lateinit var radioWeekly: RadioButton
    private lateinit var radioMonthly: RadioButton
    private lateinit var btnSave: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var headerTitle: TextView

    private lateinit var taskManager: AlarmTaskManager
    private var editingTaskId: String? = null
    private val calendar = Calendar.getInstance()
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_alarm_task)

        initViews()
        setupListeners()
        loadExistingTask()
    }

    private fun initViews() {
        editTitle = findViewById(R.id.editTitle)
        editDescription = findViewById(R.id.editDescription)
        textSelectedDate = findViewById(R.id.textSelectedDate)
        textSelectedTime = findViewById(R.id.textSelectedTime)
        switchRepeat = findViewById(R.id.switchRepeat)
        repeatOptionsLayout = findViewById(R.id.repeatOptionsLayout)
        radioGroupRepeat = findViewById(R.id.radioGroupRepeat)
        radioDaily = findViewById(R.id.radioDaily)
        radioWeekly = findViewById(R.id.radioWeekly)
        radioMonthly = findViewById(R.id.radioMonthly)
        btnSave = findViewById(R.id.btnSave)
        backButton = findViewById(R.id.backButton)
        headerTitle = findViewById(R.id.headerTitle)

        taskManager = AlarmTaskManager.getInstance(this)

        // Set default time to next hour
        selectedTime.add(Calendar.HOUR_OF_DAY, 1)
        selectedTime.set(Calendar.MINUTE, 0)
        selectedTime.set(Calendar.SECOND, 0)

        selectedDate.timeInMillis = selectedTime.timeInMillis
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        textSelectedDate.setOnClickListener {
            showDatePicker()
        }

        textSelectedTime.setOnClickListener {
            showTimePicker()
        }

        switchRepeat.setOnCheckedChangeListener { _, isChecked ->
            repeatOptionsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            saveTask()
        }
    }

    private fun loadExistingTask() {
        editingTaskId = intent.getStringExtra("task_id")

        editingTaskId?.let { id ->
            headerTitle.text = "⏰ Edit Task Alarm"
            val task = taskManager.getTask(id) ?: return

            editTitle.setText(task.title)
            editDescription.setText(task.description)

            selectedDate.timeInMillis = task.alarmTime
            selectedTime.timeInMillis = task.alarmTime

            updateDateDisplay()
            updateTimeDisplay()

            switchRepeat.isChecked = task.isRepeating
            when (task.repeatInterval) {
                RepeatInterval.DAILY -> radioDaily.isChecked = true
                RepeatInterval.WEEKLY -> radioWeekly.isChecked = true
                RepeatInterval.MONTHLY -> radioMonthly.isChecked = true
                else -> radioDaily.isChecked = true
            }
        } ?: run {
            updateDateDisplay()
            updateTimeDisplay()
        }
    }

    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate.set(Calendar.YEAR, selectedYear)
            selectedDate.set(Calendar.MONTH, selectedMonth)
            selectedDate.set(Calendar.DAY_OF_MONTH, selectedDay)
            updateDateDisplay()
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val hour = selectedTime.get(Calendar.HOUR_OF_DAY)
        val minute = selectedTime.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
            selectedTime.set(Calendar.MINUTE, selectedMinute)
            selectedTime.set(Calendar.SECOND, 0)
            updateTimeDisplay()
        }, hour, minute, false).show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        textSelectedDate.text = dateFormat.format(selectedDate.time)
    }

    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        textSelectedTime.text = timeFormat.format(selectedTime.time)
    }

    private fun saveTask() {
        val title = editTitle.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show()
            editTitle.requestFocus()
            return
        }

        val description = editDescription.text.toString().trim()

        // Combine date and time
        val alarmCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedDate.get(Calendar.YEAR))
            set(Calendar.MONTH, selectedDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, selectedDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val alarmTime = alarmCalendar.timeInMillis

        // Check if alarm time is in the past
        if (alarmTime < System.currentTimeMillis() && !switchRepeat.isChecked) {
            Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show()
            return
        }

        val isRepeating = switchRepeat.isChecked
        val repeatInterval = if (isRepeating) {
            when (radioGroupRepeat.checkedRadioButtonId) {
                R.id.radioDaily -> RepeatInterval.DAILY
                R.id.radioWeekly -> RepeatInterval.WEEKLY
                R.id.radioMonthly -> RepeatInterval.MONTHLY
                else -> RepeatInterval.NONE
            }
        } else {
            RepeatInterval.NONE
        }

        val task = AlarmTask(
            id = editingTaskId ?: UUID.randomUUID().toString(),
            title = title,
            description = description,
            alarmTime = alarmTime,
            isEnabled = true,
            isRepeating = isRepeating,
            repeatInterval = repeatInterval,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        taskManager.saveTask(task)

        val message = if (editingTaskId != null) "Task updated" else "Task alarm created"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        finish()
    }
}
