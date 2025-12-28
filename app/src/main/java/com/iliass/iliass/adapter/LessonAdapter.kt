package com.iliass.iliass.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iliass.iliass.R
import com.iliass.iliass.model.Lesson
import com.iliass.iliass.util.PDFManager
import java.text.SimpleDateFormat
import java.util.*

class LessonAdapter(
    private var lessons: List<Lesson>,
    private val onLessonClick: (Lesson) -> Unit,
    private val onLessonLongClick: (Lesson) -> Boolean = { false }
) : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class LessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lessonTitleText: TextView = view.findViewById(R.id.lessonTitleText)
        val lessonTypeText: TextView = view.findViewById(R.id.lessonTypeText)
        val lessonDateText: TextView = view.findViewById(R.id.lessonDateText)
        val lessonDescriptionText: TextView = view.findViewById(R.id.lessonDescriptionText)
        val completedBadge: TextView = view.findViewById(R.id.completedBadge)
        val pdfIcon: ImageView = view.findViewById(R.id.pdfIcon)
        val cardLayout: View = view.findViewById(R.id.lessonCardLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lesson, parent, false)
        return LessonViewHolder(view)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        val lesson = lessons[position]

        holder.lessonTitleText.text = lesson.title
        holder.lessonTypeText.text = lesson.type.displayName
        holder.lessonDateText.text = dateFormat.format(Date(lesson.date))

        // Show description if available
        if (lesson.description.isNotEmpty()) {
            holder.lessonDescriptionText.text = lesson.description
            holder.lessonDescriptionText.visibility = View.VISIBLE
        } else {
            holder.lessonDescriptionText.visibility = View.GONE
        }

        // Show completed badge
        if (lesson.isCompleted) {
            holder.completedBadge.visibility = View.VISIBLE
            holder.cardLayout.alpha = 0.8f
        } else {
            holder.completedBadge.visibility = View.GONE
            holder.cardLayout.alpha = 1.0f
        }

        // Show PDF icon if PDF is attached
        if (PDFManager.pdfExists(lesson.pdfFilePath)) {
            holder.pdfIcon.visibility = View.VISIBLE
        } else {
            holder.pdfIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onLessonClick(lesson)
        }

        holder.itemView.setOnLongClickListener {
            onLessonLongClick(lesson)
        }
    }

    override fun getItemCount(): Int = lessons.size

    fun updateLessons(newLessons: List<Lesson>) {
        lessons = newLessons
        notifyDataSetChanged()
    }
}
