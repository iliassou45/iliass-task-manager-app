package com.iliass.iliass.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.Student
import java.text.DecimalFormat

class StudentCheckboxAdapter(
    private val students: List<Student>,
    private val selectedStudentIds: MutableSet<String>
) : RecyclerView.Adapter<StudentCheckboxAdapter.StudentCheckboxViewHolder>() {

    private val decimalFormat = DecimalFormat("#,##0.00")

    class StudentCheckboxViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val studentCheckBox: CheckBox = view.findViewById(R.id.studentCheckBox)
        val studentNameText: TextView = view.findViewById(R.id.studentNameText)
        val studentInfoText: TextView = view.findViewById(R.id.studentInfoText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentCheckboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_checkbox, parent, false)
        return StudentCheckboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentCheckboxViewHolder, position: Int) {
        val student = students[position]

        holder.studentNameText.text = student.name
        holder.studentInfoText.text = "Monthly: $${decimalFormat.format(student.monthlyAmount)}"

        holder.studentCheckBox.isChecked = student.id in selectedStudentIds

        holder.studentCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedStudentIds.add(student.id)
            } else {
                selectedStudentIds.remove(student.id)
            }
        }

        holder.itemView.setOnClickListener {
            holder.studentCheckBox.isChecked = !holder.studentCheckBox.isChecked
        }
    }

    override fun getItemCount(): Int = students.size
}
