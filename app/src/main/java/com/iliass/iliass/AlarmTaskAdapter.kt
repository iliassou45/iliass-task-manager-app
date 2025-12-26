package com.iliass.iliass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.iliass.iliass.model.AlarmTask
import com.iliass.iliass.model.RepeatInterval
import java.text.SimpleDateFormat
import java.util.*

class AlarmTaskAdapter(
    private var tasks: List<AlarmTask>,
    private val onToggle: (String) -> Unit,
    private val onEdit: (AlarmTask) -> Unit,
    private val onDelete: (AlarmTask) -> Unit
) : RecyclerView.Adapter<AlarmTaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskTitle: TextView = view.findViewById(R.id.taskTitle)
        val taskDescription: TextView = view.findViewById(R.id.taskDescription)
        val alarmTime: TextView = view.findViewById(R.id.alarmTime)
        val repeatInfo: TextView = view.findViewById(R.id.repeatInfo)
        val switchEnabled: SwitchCompat = view.findViewById(R.id.switchEnabled)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.taskTitle.text = task.title
        holder.taskDescription.text = task.description
        holder.taskDescription.visibility = if (task.description.isNotEmpty()) View.VISIBLE else View.GONE

        // Format alarm time
        holder.alarmTime.text = formatAlarmTime(task.alarmTime)

        // Show repeat info if repeating
        if (task.isRepeating && task.repeatInterval != RepeatInterval.NONE) {
            holder.repeatInfo.visibility = View.VISIBLE
            holder.repeatInfo.text = "🔁 ${task.repeatInterval.displayName}"
        } else {
            holder.repeatInfo.visibility = View.GONE
        }

        // Set switch state without triggering listener
        holder.switchEnabled.setOnCheckedChangeListener(null)
        holder.switchEnabled.isChecked = task.isEnabled
        holder.switchEnabled.setOnCheckedChangeListener { _, _ ->
            onToggle(task.id)
        }

        // Edit button
        holder.btnEdit.setOnClickListener {
            onEdit(task)
        }

        // Delete button
        holder.btnDelete.setOnClickListener {
            onDelete(task)
        }

        // Dim the card if disabled
        holder.itemView.alpha = if (task.isEnabled) 1.0f else 0.5f
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<AlarmTask>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    private fun formatAlarmTime(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val now = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val dateStr = when {
            isSameDay(calendar, now) -> "Today"
            isTomorrow(calendar, now) -> "Tomorrow"
            else -> dateFormat.format(calendar.time)
        }

        return "$dateStr at ${timeFormat.format(calendar.time)}"
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isTomorrow(calendar: Calendar, now: Calendar): Boolean {
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return isSameDay(calendar, tomorrow)
    }
}
