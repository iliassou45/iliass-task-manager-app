package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.iliass.iliass.model.AlarmTask
import com.iliass.iliass.util.AlarmTaskManager

class AlarmTaskListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var backButton: ImageButton
    private lateinit var adapter: AlarmTaskAdapter
    private lateinit var taskManager: AlarmTaskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_task_list)

        initViews()
        setupListeners()
        loadTasks()
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        fabAddTask = findViewById(R.id.fabAddTask)
        backButton = findViewById(R.id.backButton)
        taskManager = AlarmTaskManager.getInstance(this)

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        fabAddTask.setOnClickListener {
            val intent = Intent(this, AddAlarmTaskActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadTasks() {
        val tasks = taskManager.getAllTasks()
            .sortedWith(compareBy<AlarmTask> { !it.isEnabled }.thenBy { it.alarmTime })

        if (tasks.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            if (!::adapter.isInitialized) {
                adapter = AlarmTaskAdapter(
                    tasks,
                    onToggle = { taskId -> toggleTask(taskId) },
                    onEdit = { task -> editTask(task) },
                    onDelete = { task -> confirmDelete(task) }
                )
                recyclerView.adapter = adapter
            } else {
                adapter.updateTasks(tasks)
            }
        }
    }

    private fun toggleTask(taskId: String) {
        taskManager.toggleTaskEnabled(taskId)
        loadTasks()

        val task = taskManager.getTask(taskId)
        val message = if (task?.isEnabled == true) {
            "Alarm enabled"
        } else {
            "Alarm disabled"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun editTask(task: AlarmTask) {
        val intent = Intent(this, AddAlarmTaskActivity::class.java).apply {
            putExtra("task_id", task.id)
        }
        startActivity(intent)
    }

    private fun confirmDelete(task: AlarmTask) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task Alarm")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: AlarmTask) {
        taskManager.deleteTask(task.id)
        loadTasks()
        Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
    }
}
