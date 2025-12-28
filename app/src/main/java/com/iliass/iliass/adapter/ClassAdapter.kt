package com.iliass.iliass.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.StudentClass
import com.iliass.iliass.repository.StudentDatabase
import java.text.DecimalFormat

class ClassAdapter(
    private var classes: List<StudentClass>,
    private val database: StudentDatabase,
    private val onClassClick: (StudentClass) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    private val decimalFormat = DecimalFormat("#,##0.00")

    class ClassViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val classNameText: TextView = view.findViewById(R.id.classNameText)
        val classTypeText: TextView = view.findViewById(R.id.classTypeText)
        val classScheduleText: TextView = view.findViewById(R.id.classScheduleText)
        val classStatusBadge: TextView = view.findViewById(R.id.classStatusBadge)
        val studentCountText: TextView = view.findViewById(R.id.studentCountText)
        val revenueText: TextView = view.findViewById(R.id.revenueText)
        val debtText: TextView = view.findViewById(R.id.debtText)
        val cardLayout: View = view.findViewById(R.id.classCardLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val studentClass = classes[position]

        holder.classNameText.text = studentClass.name
        holder.classTypeText.text = "Type: ${studentClass.type.displayName}"

        // Format schedule
        val daysStr = if (studentClass.days.isEmpty()) {
            "No days set"
        } else {
            studentClass.days.joinToString(", ") { it.take(3) }
        }
        holder.classScheduleText.text = "$daysStr at ${studentClass.startTime}"

        // Status badge
        if (studentClass.isActive) {
            holder.classStatusBadge.text = "ACTIVE"
            holder.classStatusBadge.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
        } else {
            holder.classStatusBadge.text = "INACTIVE"
            holder.classStatusBadge.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.darker_gray)
            )
        }

        // Statistics
        holder.studentCountText.text = studentClass.getCurrentStudentCount().toString()
        holder.revenueText.text = "$${decimalFormat.format(studentClass.getMonthlyRevenuePotential(database))}"

        val debt = studentClass.getTotalClassDebt(database)
        holder.debtText.text = "$${decimalFormat.format(debt)}"

        // Color debt red if > 0, green if 0
        if (debt > 0) {
            holder.debtText.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_red_dark)
            )
        } else {
            holder.debtText.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
        }

        // Change card background for inactive classes
        if (!studentClass.isActive) {
            holder.cardLayout.alpha = 0.6f
        } else {
            holder.cardLayout.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            onClassClick(studentClass)
        }
    }

    override fun getItemCount(): Int = classes.size

    fun updateClasses(newClasses: List<StudentClass>) {
        classes = newClasses
        notifyDataSetChanged()
    }
}
