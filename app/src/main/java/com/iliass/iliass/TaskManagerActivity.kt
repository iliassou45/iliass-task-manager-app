package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.iliass.iliass.adapter.TaskItemAdapter
import com.iliass.iliass.model.DailyTask
import com.iliass.iliass.model.TaskStatus
import com.iliass.iliass.util.TaskManager

class TaskManagerActivity : AppCompatActivity() {

    private lateinit var taskManager: TaskManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskItemAdapter
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var tipCard: MaterialCardView
    private lateinit var textTipEmoji: TextView
    private lateinit var textTipTitle: TextView
    private lateinit var textTipDescription: TextView
    private lateinit var textCompletedCount: TextView
    private lateinit var textPendingCount: TextView
    private lateinit var textStreakCount: TextView

    private var currentFilter = FilterType.TODAY
    private var tipIndex = 0
    private var tips = listOf<com.iliass.iliass.model.ProductivityTip>()

    private enum class FilterType {
        TODAY, UPCOMING, OVERDUE, COMPLETED, ALL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_manager)

        taskManager = TaskManager.getInstance(this)

        initViews()
        setupRecyclerView()
        setupTabLayout()
        setupClickListeners()

        // Check for highlighted task from notification
        intent.getStringExtra("highlight_task_id")?.let { taskId ->
            val task = taskManager.getTask(taskId)
            task?.let { showTaskDetail(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        taskManager.updateOverdueTasks()
        loadTasks()
        updateQuickStats()
        loadTips()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        tabLayout = findViewById(R.id.tabLayout)
        tipCard = findViewById(R.id.tipCard)
        textTipEmoji = findViewById(R.id.textTipEmoji)
        textTipTitle = findViewById(R.id.textTipTitle)
        textTipDescription = findViewById(R.id.textTipDescription)
        textCompletedCount = findViewById(R.id.textCompletedCount)
        textPendingCount = findViewById(R.id.textPendingCount)
        textStreakCount = findViewById(R.id.textStreakCount)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskItemAdapter(
            onTaskClick = { task -> showTaskDetail(task) },
            onTaskComplete = { task -> completeTask(task) },
            onTaskDelete = { task -> confirmDeleteTask(task) },
            onTaskEdit = { task -> editTask(task) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TaskManagerActivity)
            adapter = taskAdapter
        }
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> FilterType.TODAY
                    1 -> FilterType.UPCOMING
                    2 -> FilterType.OVERDUE
                    3 -> FilterType.COMPLETED
                    4 -> FilterType.ALL
                    else -> FilterType.TODAY
                }
                loadTasks()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnAnalytics).setOnClickListener {
            startActivity(Intent(this, TaskAnalyticsActivity::class.java))
        }

        findViewById<FloatingActionButton>(R.id.fabAddTask).setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }

        findViewById<ImageButton>(R.id.btnDismissTip).setOnClickListener {
            showNextTip()
        }

        tipCard.setOnClickListener {
            showNextTip()
        }
    }

    private fun loadTasks() {
        val tasks = when (currentFilter) {
            FilterType.TODAY -> taskManager.getTodayTasks()
            FilterType.UPCOMING -> taskManager.getUpcomingTasks(7)
            FilterType.OVERDUE -> taskManager.getOverdueTasks()
            FilterType.COMPLETED -> taskManager.getTasksByStatus(TaskStatus.COMPLETED)
                .sortedByDescending { it.completedAt }
            FilterType.ALL -> taskManager.getAllTasks()
                .sortedWith(compareBy({ it.status == TaskStatus.COMPLETED },
                                      { it.status == TaskStatus.CANCELLED },
                                      { -it.priority.ordinal },
                                      { it.dueDate }))
        }

        taskAdapter.submitList(tasks)
        updateEmptyState(tasks.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        val emptyEmoji = findViewById<TextView>(R.id.emptyEmoji)
        val emptyTitle = findViewById<TextView>(R.id.emptyTitle)
        val emptyDescription = findViewById<TextView>(R.id.emptyDescription)

        if (isEmpty) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE

            when (currentFilter) {
                FilterType.TODAY -> {
                    emptyEmoji.text = "🎉"
                    emptyTitle.text = "No tasks for today"
                    emptyDescription.text = "Enjoy your free day or add some tasks!"
                }
                FilterType.UPCOMING -> {
                    emptyEmoji.text = "📅"
                    emptyTitle.text = "No upcoming tasks"
                    emptyDescription.text = "Plan ahead by adding future tasks"
                }
                FilterType.OVERDUE -> {
                    emptyEmoji.text = "✨"
                    emptyTitle.text = "No overdue tasks!"
                    emptyDescription.text = "Great job staying on top of things!"
                }
                FilterType.COMPLETED -> {
                    emptyEmoji.text = "📝"
                    emptyTitle.text = "No completed tasks yet"
                    emptyDescription.text = "Complete some tasks to see them here"
                }
                FilterType.ALL -> {
                    emptyEmoji.text = "📋"
                    emptyTitle.text = "No tasks yet"
                    emptyDescription.text = "Tap the + button to add your first task"
                }
            }
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateQuickStats() {
        val (completed, pending, _) = taskManager.getQuickStats()
        val streak = taskManager.getCurrentStreak()

        textCompletedCount.text = completed.toString()
        textPendingCount.text = pending.toString()
        textStreakCount.text = "$streak 🔥"
    }

    private fun loadTips() {
        tips = taskManager.generateProductivityTips()
        if (tips.isNotEmpty()) {
            tipCard.visibility = View.VISIBLE
            showTip(tips[tipIndex % tips.size])
        } else {
            // Show default encouraging tip
            tipCard.visibility = View.VISIBLE
            textTipEmoji.text = "💪"
            textTipTitle.text = "You're doing great!"
            textTipDescription.text = "Keep up the momentum and crush your goals today!"
        }
    }

    private fun showTip(tip: com.iliass.iliass.model.ProductivityTip) {
        textTipEmoji.text = tip.emoji
        textTipTitle.text = tip.title
        textTipDescription.text = tip.description
    }

    private fun showNextTip() {
        if (tips.isNotEmpty()) {
            tipIndex = (tipIndex + 1) % tips.size
            showTip(tips[tipIndex])
        }
    }

    private fun showTaskDetail(task: DailyTask) {
        val intent = Intent(this, AddEditTaskActivity::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("view_mode", true)
        }
        startActivity(intent)
    }

    private fun editTask(task: DailyTask) {
        val intent = Intent(this, AddEditTaskActivity::class.java).apply {
            putExtra("task_id", task.id)
        }
        startActivity(intent)
    }

    private fun completeTask(task: DailyTask) {
        // Show dialog to optionally enter actual time spent
        val dialogView = layoutInflater.inflate(R.layout.dialog_complete_task, null)
        val editMinutes = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editActualMinutes)
        editMinutes?.setText(task.estimatedMinutes.toString())

        AlertDialog.Builder(this)
            .setTitle("✅ Complete Task")
            .setMessage("Mark \"${task.title}\" as completed?")
            .setView(dialogView)
            .setPositiveButton("Complete") { _, _ ->
                val actualMinutes = editMinutes?.text?.toString()?.toIntOrNull()
                taskManager.completeTask(task.id, actualMinutes)
                Toast.makeText(this, "🎉 Task completed!", Toast.LENGTH_SHORT).show()
                loadTasks()
                updateQuickStats()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteTask(task: DailyTask) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                taskManager.deleteTask(task.id)
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
                loadTasks()
                updateQuickStats()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
