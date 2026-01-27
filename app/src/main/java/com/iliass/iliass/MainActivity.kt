package com.iliass.iliass

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.tasksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val tasks = listOf(
            Task(
                id = 1,
                title = "🎹 Background Video Recorder",
                description = "Record video discreetly in the background",
                icon = "🎹"
            ),
            Task(
                id = 2,
                title = "📝 Notes",
                description = "Record everything",
                icon = "📝"
            ),
            Task(
                id = 3,
                title = "📱 Phone Inventory (IMEI)",
                description = "Track phones you buy from shops",
                icon = "📱"
            ),
            Task(
                id = 4,
                title = "🚫 App Blocker",
                description = "Block distracting apps and stay focused",
                icon = "🚫"
            ),
            Task(
                id = 5,
                title = "💰 Debt Manager",
                description = "Track money you owe and money owed to you",
                icon = "💰"
            ),
            Task(
                id = 6,
                title = "🇬🇧🇫🇷 Language Flashcards",
                description = "Learn English and French with flashcards",
                icon = "📚"
            ),
            Task(
                id = 7,
                title = "🔋 Battery Saver",
                description = "Optimize battery usage (Coming Soon)",
                icon = "🔋",
                isEnabled = false
            ),
            Task(
                id = 8,
                title = "📋 Task Manager",
                description = "Organize tasks, track productivity, and stay on top of your goals",
                icon = "📋",
                isEnabled = true
            ),
            Task(
                id = 9,
                title = "🎓 Student Payments",
                description = "Track student monthly payments and manage debts",
                icon = "🎓",
                isEnabled = true
            ),
            Task(
                id = 10,
                title = "👥 Student Groups/Classes",
                description = "Manage student classes, lessons, and track class analytics",
                icon = "👥",
                isEnabled = true
            ),
            Task(
                id = 11,
                title = "☁️ Cloud Backup",
                description = "Backup and restore your data to Supabase cloud",
                icon = "☁️",
                isEnabled = true
            )
        )

        taskAdapter = TaskAdapter(tasks) { task ->
            onTaskClicked(task)
        }

        recyclerView.adapter = taskAdapter
    }

    private fun onTaskClicked(task: Task) {
        when (task.id) {
            1 -> {
                // Navigate to Video Recorder activity
                val intent = Intent(this, VideoRecorderActivity::class.java)
                startActivity(intent)
            }
            2 -> {
                // Navigate to Notes activity
                val intent = Intent(this, NotesListActivity::class.java)
                startActivity(intent)
            }
            3 -> {
                // Navigate to IMEI List activity
                val intent = Intent(this, ImeiListActivity::class.java)
                startActivity(intent)
            }
            4 -> {
                // Navigate to App Blocker activity
                val intent = Intent(this, AppBlockerActivity::class.java)
                startActivity(intent)
            }
            5 -> {
                // Navigate to Debt Manager activity
                val intent = Intent(this, DebtManagerActivity::class.java)
                startActivity(intent)
            }
            6 -> {
                // Navigate to Flashcards activity
                val intent = Intent(this, FlashCardListActivity::class.java)
                startActivity(intent)
            }
            8 -> {
                // Navigate to Task Manager activity
                val intent = Intent(this, TaskManagerActivity::class.java)
                startActivity(intent)
            }
            9 -> {
                // Navigate to Student Payment activity
                val intent = Intent(this, StudentPaymentActivity::class.java)
                startActivity(intent)
            }
            10 -> {
                // Navigate to Classes activity
                val intent = Intent(this, ClassesActivity::class.java)
                startActivity(intent)
            }
            11 -> {
                // Navigate to Cloud Backup activity
                val intent = Intent(this, CloudBackupActivity::class.java)
                startActivity(intent)
            }
            else -> {
                // Show coming soon message for other tasks
                android.widget.Toast.makeText(
                    this,
                    "${task.title} - Coming Soon!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val icon: String,
    val isEnabled: Boolean = true
)