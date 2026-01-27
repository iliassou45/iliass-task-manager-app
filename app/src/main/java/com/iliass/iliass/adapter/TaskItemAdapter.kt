package com.iliass.iliass.adapter

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.iliass.iliass.R
import com.iliass.iliass.model.DailyTask
import com.iliass.iliass.model.TaskPriority
import com.iliass.iliass.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.*

class TaskItemAdapter(
    private val onTaskClick: (DailyTask) -> Unit,
    private val onTaskComplete: (DailyTask) -> Unit,
    private val onTaskDelete: (DailyTask) -> Unit,
    private val onTaskEdit: (DailyTask) -> Unit
) : ListAdapter<DailyTask, TaskItemAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.taskCard)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkboxComplete)
        private val titleText: TextView = itemView.findViewById(R.id.textTitle)
        private val descriptionText: TextView = itemView.findViewById(R.id.textDescription)
        private val timeText: TextView = itemView.findViewById(R.id.textTime)
        private val categoryChip: Chip = itemView.findViewById(R.id.chipCategory)
        private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
        private val statusText: TextView = itemView.findViewById(R.id.textStatus)
        private val editButton: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val subtasksLayout: LinearLayout = itemView.findViewById(R.id.subtasksLayout)
        private val subtasksText: TextView = itemView.findViewById(R.id.textSubtasks)
        private val estimatedTimeText: TextView = itemView.findViewById(R.id.textEstimatedTime)

        fun bind(task: DailyTask) {
            // Title
            titleText.text = task.title
            if (task.status == TaskStatus.COMPLETED) {
                titleText.paintFlags = titleText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                titleText.alpha = 0.6f
            } else {
                titleText.paintFlags = titleText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                titleText.alpha = 1f
            }

            // Description
            if (task.description.isNotEmpty()) {
                descriptionText.visibility = View.VISIBLE
                descriptionText.text = task.description
            } else {
                descriptionText.visibility = View.GONE
            }

            // Time
            timeText.text = formatDateTime(task.dueDate, task.dueTime)

            // Category
            categoryChip.text = "${task.category.emoji} ${task.category.displayName}"
            try {
                categoryChip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    Color.parseColor(task.category.colorHex)
                )
            } catch (e: Exception) {
                // Use default color
            }

            // Priority indicator
            try {
                priorityIndicator.setBackgroundColor(Color.parseColor(task.priority.colorHex))
            } catch (e: Exception) {
                priorityIndicator.setBackgroundColor(Color.GRAY)
            }

            // Status
            statusText.text = "${task.status.emoji} ${task.status.displayName}"
            when (task.status) {
                TaskStatus.COMPLETED -> {
                    statusText.setTextColor(Color.parseColor("#4CAF50"))
                    cardView.alpha = 0.8f
                }
                TaskStatus.OVERDUE -> {
                    statusText.setTextColor(Color.parseColor("#F44336"))
                    cardView.alpha = 1f
                }
                TaskStatus.IN_PROGRESS -> {
                    statusText.setTextColor(Color.parseColor("#2196F3"))
                    cardView.alpha = 1f
                }
                TaskStatus.CANCELLED -> {
                    statusText.setTextColor(Color.parseColor("#9E9E9E"))
                    cardView.alpha = 0.6f
                }
                else -> {
                    statusText.setTextColor(Color.parseColor("#757575"))
                    cardView.alpha = 1f
                }
            }

            // Checkbox
            checkBox.isChecked = task.status == TaskStatus.COMPLETED
            checkBox.isEnabled = task.status != TaskStatus.CANCELLED

            // Subtasks
            if (task.subtasks.isNotEmpty()) {
                subtasksLayout.visibility = View.VISIBLE
                val completed = task.subtasks.count { it.isCompleted }
                subtasksText.text = "📝 ${completed}/${task.subtasks.size} subtasks"
            } else {
                subtasksLayout.visibility = View.GONE
            }

            // Estimated time
            if (task.estimatedMinutes > 0) {
                estimatedTimeText.visibility = View.VISIBLE
                estimatedTimeText.text = "⏱️ ${formatDuration(task.estimatedMinutes)}"
            } else {
                estimatedTimeText.visibility = View.GONE
            }

            // Click listeners
            cardView.setOnClickListener { onTaskClick(task) }

            checkBox.setOnClickListener {
                if (task.status != TaskStatus.COMPLETED) {
                    onTaskComplete(task)
                }
            }

            editButton.setOnClickListener { onTaskEdit(task) }
            deleteButton.setOnClickListener { onTaskDelete(task) }

            // Priority glow for urgent/high
            if (task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH) {
                cardView.strokeWidth = 2
                try {
                    cardView.strokeColor = Color.parseColor(task.priority.colorHex)
                } catch (e: Exception) {}
            } else {
                cardView.strokeWidth = 0
            }
        }

        private fun formatDateTime(dueDate: Long, dueTime: Long?): String {
            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

            calendar.timeInMillis = dueDate

            val dateStr = when {
                calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
                calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"
                else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(dueDate))
            }

            return if (dueTime != null) {
                val timeCalendar = Calendar.getInstance().apply { timeInMillis = dueTime }
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dueTime))
                "📅 $dateStr at $timeStr"
            } else {
                "📅 $dateStr"
            }
        }

        private fun formatDuration(minutes: Int): String {
            return when {
                minutes < 60 -> "${minutes}min"
                minutes % 60 == 0 -> "${minutes / 60}h"
                else -> "${minutes / 60}h ${minutes % 60}min"
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<DailyTask>() {
        override fun areItemsTheSame(oldItem: DailyTask, newItem: DailyTask): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DailyTask, newItem: DailyTask): Boolean {
            return oldItem == newItem
        }
    }
}
