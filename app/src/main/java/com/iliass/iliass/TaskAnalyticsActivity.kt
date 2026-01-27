package com.iliass.iliass

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.iliass.iliass.model.ProductivityTip
import com.iliass.iliass.model.TaskAnalytics
import com.iliass.iliass.util.TaskManager
import java.util.*

class TaskAnalyticsActivity : AppCompatActivity() {

    private lateinit var taskManager: TaskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_analytics)

        taskManager = TaskManager.getInstance(this)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        loadAnalytics()
    }

    private fun loadAnalytics() {
        val analytics = taskManager.calculateAnalytics()
        val tips = taskManager.generateProductivityTips()

        updateProductivityScore(analytics)
        updateStats(analytics)
        updateTaskOverview(analytics)
        updatePeakPerformance(analytics)
        updateWeeklyActivity(analytics)
        updateTips(tips)
    }

    private fun updateProductivityScore(analytics: TaskAnalytics) {
        val scoreText = findViewById<TextView>(R.id.textProductivityScore)
        val labelText = findViewById<TextView>(R.id.textScoreLabel)
        val scoreCard = findViewById<MaterialCardView>(R.id.textProductivityScore)?.parent?.parent as? MaterialCardView

        val score = analytics.productivityScore.toInt()
        scoreText.text = score.toString()

        val (label, color) = when {
            score >= 80 -> "Excellent! You're a productivity machine! 🚀" to "#1B5E20"
            score >= 60 -> "Good! Keep up the momentum! 👍" to "#2E7D32"
            score >= 40 -> "Getting there! Room for improvement 💪" to "#F57F17"
            score >= 20 -> "Let's pick up the pace! 🎯" to "#E65100"
            else -> "Time to start fresh! You got this! 🌱" to "#C62828"
        }

        labelText.text = label
        try {
            scoreText.setTextColor(Color.parseColor(color))
        } catch (e: Exception) {}
    }

    private fun updateStats(analytics: TaskAnalytics) {
        // Completion Rate
        findViewById<TextView>(R.id.textCompletionRate).text = "${analytics.completionRate.toInt()}%"

        // On-Time Rate
        findViewById<TextView>(R.id.textOnTimeRate).text = "${analytics.onTimeCompletionRate.toInt()}%"

        // Current Streak
        findViewById<TextView>(R.id.textCurrentStreak).text = "${analytics.currentStreak} days"
        findViewById<TextView>(R.id.textLongestStreak).text = "Best: ${analytics.longestStreak} days"

        // Avg Tasks Per Day
        findViewById<TextView>(R.id.textAvgTasksPerDay).text = String.format("%.1f", analytics.averageTasksPerDay)
    }

    private fun updateTaskOverview(analytics: TaskAnalytics) {
        findViewById<TextView>(R.id.textTotalCreated).text = analytics.totalTasksCreated.toString()
        findViewById<TextView>(R.id.textTotalCompleted).text = analytics.totalTasksCompleted.toString()
        findViewById<TextView>(R.id.textTotalOverdue).text = analytics.totalTasksOverdue.toString()
        findViewById<TextView>(R.id.textTotalCancelled).text = analytics.totalTasksCancelled.toString()
    }

    private fun updatePeakPerformance(analytics: TaskAnalytics) {
        // Best Hour
        val bestHourText = findViewById<TextView>(R.id.textBestHour)
        bestHourText.text = if (analytics.mostProductiveHour >= 0) {
            when {
                analytics.mostProductiveHour == 0 -> "12 AM"
                analytics.mostProductiveHour < 12 -> "${analytics.mostProductiveHour} AM"
                analytics.mostProductiveHour == 12 -> "12 PM"
                else -> "${analytics.mostProductiveHour - 12} PM"
            }
        } else {
            "Not enough data"
        }

        // Best Day
        val bestDayText = findViewById<TextView>(R.id.textBestDay)
        val dayNames = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        bestDayText.text = if (analytics.mostProductiveDay in 1..7) {
            dayNames[analytics.mostProductiveDay - 1]
        } else {
            "Not enough data"
        }

        // Estimation Accuracy
        findViewById<TextView>(R.id.textEstimationAccuracy).text =
            if (analytics.estimationAccuracy > 0) "${analytics.estimationAccuracy.toInt()}%" else "N/A"
    }

    private fun updateWeeklyActivity(analytics: TaskAnalytics) {
        val weeklyLayout = findViewById<LinearLayout>(R.id.weeklyActivityLayout)
        weeklyLayout.removeAllViews()

        val dayLabels = arrayOf("S", "M", "T", "W", "T", "F", "S")
        val maxTasks = analytics.weeklyCompletion.maxOrNull() ?: 1

        for (i in 0..6) {
            val dayLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Bar
            val barHeight = if (maxTasks > 0) {
                (analytics.weeklyCompletion[i].toFloat() / maxTasks * 80).toInt().coerceAtLeast(8)
            } else 8

            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24, barHeight.dpToPx()).apply {
                    gravity = Gravity.CENTER
                }
                setBackgroundColor(
                    if (analytics.weeklyCompletion[i] > 0) Color.parseColor("#6200EE")
                    else Color.parseColor("#E0E0E0")
                )
            }

            // Count
            val countText = TextView(this).apply {
                text = analytics.weeklyCompletion[i].toString()
                textSize = 10f
                setTextColor(Color.parseColor("#757575"))
                gravity = Gravity.CENTER
            }

            // Day label
            val dayText = TextView(this).apply {
                text = dayLabels[i]
                textSize = 12f
                setTextColor(Color.parseColor("#212121"))
                gravity = Gravity.CENTER
            }

            dayLayout.addView(countText)
            dayLayout.addView(bar)
            dayLayout.addView(dayText)
            weeklyLayout.addView(dayLayout)
        }
    }

    private fun updateTips(tips: List<ProductivityTip>) {
        val tipsLayout = findViewById<LinearLayout>(R.id.tipsLayout)
        tipsLayout.removeAllViews()

        if (tips.isEmpty()) {
            val noTipsText = TextView(this).apply {
                text = "🌟 You're doing great! No specific improvements needed."
                textSize = 14f
                setTextColor(Color.parseColor("#4CAF50"))
                setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
            }
            tipsLayout.addView(noTipsText)
            return
        }

        tips.forEach { tip ->
            val tipView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
            }

            val emojiText = TextView(this).apply {
                text = tip.emoji
                textSize = 20f
                setPadding(0, 0, 12.dpToPx(), 0)
            }

            val contentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleText = TextView(this).apply {
                text = tip.title
                textSize = 14f
                setTextColor(Color.parseColor("#212121"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val descText = TextView(this).apply {
                text = tip.description
                textSize = 12f
                setTextColor(Color.parseColor("#757575"))
            }

            contentLayout.addView(titleText)
            contentLayout.addView(descText)
            tipView.addView(emojiText)
            tipView.addView(contentLayout)
            tipsLayout.addView(tipView)

            // Add divider (except for last item)
            if (tips.indexOf(tip) < tips.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    ).apply { setMargins(0, 8.dpToPx(), 0, 0) }
                    setBackgroundColor(Color.parseColor("#E0E0E0"))
                }
                tipsLayout.addView(divider)
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
