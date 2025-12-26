package com.iliass.iliass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.taskCard)
        val iconText: TextView = itemView.findViewById(R.id.taskIcon)
        val titleText: TextView = itemView.findViewById(R.id.taskTitle)
        val descriptionText: TextView = itemView.findViewById(R.id.taskDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.iconText.text = task.icon
        holder.titleText.text = task.title
        holder.descriptionText.text = task.description

        // Apply disabled styling if task is not enabled
        if (!task.isEnabled) {
            holder.cardView.alpha = 0.5f
            holder.titleText.setTextColor(0xFF9E9E9E.toInt())
            holder.descriptionText.setTextColor(0xFFBDBDBD.toInt())
        } else {
            holder.cardView.alpha = 1f
           // holder.titleText.setTextColor(0xFF212121.toInt())
            holder.descriptionText.setTextColor(0xFF757575.toInt())
        }

        holder.cardView.setOnClickListener {
            if (task.isEnabled) {
                onTaskClick(task)
            }
        }
    }

    override fun getItemCount() = tasks.size
}