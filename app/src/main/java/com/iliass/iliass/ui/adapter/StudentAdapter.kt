package com.iliass.iliass.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.Payment
import com.iliass.iliass.model.Student
import java.text.NumberFormat
import java.util.*

class StudentAdapter(
    private var students: List<Student>,
    private var allPayments: List<Payment>,
    private val onStudentClick: (Student) -> Unit,
    private val onStudentLongClick: ((Student) -> Unit)? = null
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.studentCard)
        val nameText: TextView = itemView.findViewById(R.id.studentNameText)
        val monthlyAmountText: TextView = itemView.findViewById(R.id.monthlyAmountText)
        val locationTimeText: TextView = itemView.findViewById(R.id.locationTimeText)
        val debtText: TextView = itemView.findViewById(R.id.debtText)
        val statusText: TextView = itemView.findViewById(R.id.studentStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        val studentPayments = allPayments.filter { it.studentId == student.id }
        val currentDebt = student.getCurrentDebt(studentPayments)
        val monthlyAmount = student.monthlyAmount

        holder.nameText.text = student.name

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        holder.monthlyAmountText.text = "Monthly: ${currencyFormat.format(monthlyAmount)}"

        // Display location and time
        if (student.location.isNotEmpty() || student.timezoneOffsetHours != 0.0) {
            val locationTime = buildString {
                if (student.location.isNotEmpty()) {
                    append("📍 ${student.location}")
                }
                val studentLocalTime = student.getStudentLocalTime()
                if (studentLocalTime.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append("${studentLocalTime}")
                }
            }
            holder.locationTimeText.text = locationTime
            holder.locationTimeText.visibility = View.VISIBLE
        } else {
            holder.locationTimeText.visibility = View.GONE
        }

        if (currentDebt > 0) {
            holder.debtText.text = "Debt: ${currencyFormat.format(currentDebt)}"
            holder.debtText.visibility = View.VISIBLE
            holder.debtText.setTextColor(0xFFE53935.toInt())
        } else {
            holder.debtText.text = "Paid up"
            holder.debtText.setTextColor(0xFF43A047.toInt())
        }

        if (student.isActive) {
            holder.statusText.text = "Active"
            holder.statusText.setTextColor(0xFF43A047.toInt())
            holder.cardView.setCardBackgroundColor(0xFFFFFFFF.toInt())
        } else {
            holder.statusText.text = "Inactive"
            holder.statusText.setTextColor(0xFF757575.toInt())
            holder.cardView.setCardBackgroundColor(0xFFF5F5F5.toInt())
        }

        holder.cardView.setOnClickListener {
            onStudentClick(student)
        }

        holder.cardView.setOnLongClickListener {
            onStudentLongClick?.invoke(student)
            true
        }
    }

    override fun getItemCount() = students.size

    fun updateData(newStudents: List<Student>, newPayments: List<Payment>) {
        students = newStudents
        allPayments = newPayments
        notifyDataSetChanged()
    }
}
