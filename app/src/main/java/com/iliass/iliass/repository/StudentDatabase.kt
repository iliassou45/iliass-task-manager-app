package com.iliass.iliass.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.Student
import com.iliass.iliass.model.Payment
import com.iliass.iliass.model.StudentClass
import com.iliass.iliass.model.Lesson
import com.iliass.iliass.model.ClassStudentHistory
import com.iliass.iliass.model.HistoryAction
import com.iliass.iliass.model.Note
import com.iliass.iliass.model.Debt
import com.iliass.iliass.model.DailyTask
import com.iliass.iliass.util.NoteManager
import com.iliass.iliass.util.TaskManager

class StudentDatabase private constructor(context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("student_payment_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val students = mutableListOf<Student>()
    private val payments = mutableListOf<Payment>()
    private val classes = mutableListOf<StudentClass>()
    private val lessons = mutableListOf<Lesson>()
    private val classHistory = mutableListOf<ClassStudentHistory>()

    init {
        loadDataFromStorage()
    }

    companion object {
        @Volatile
        private var instance: StudentDatabase? = null

        fun getInstance(context: Context): StudentDatabase {
            return instance ?: synchronized(this) {
                instance ?: StudentDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun loadDataFromStorage() {
        // Load students
        val studentsJson = sharedPrefs.getString("students", null)
        if (studentsJson != null) {
            val type = object : TypeToken<List<Student>>() {}.type
            val loadedStudents: List<Student> = gson.fromJson(studentsJson, type)
            students.clear()
            students.addAll(loadedStudents)
        }

        // Load payments
        val paymentsJson = sharedPrefs.getString("payments", null)
        if (paymentsJson != null) {
            val type = object : TypeToken<List<Payment>>() {}.type
            val loadedPayments: List<Payment> = gson.fromJson(paymentsJson, type)
            payments.clear()
            payments.addAll(loadedPayments)
        }

        // Load classes
        val classesJson = sharedPrefs.getString("classes", null)
        if (classesJson != null) {
            val type = object : TypeToken<List<StudentClass>>() {}.type
            val loadedClasses: List<StudentClass> = gson.fromJson(classesJson, type)
            classes.clear()
            classes.addAll(loadedClasses)
        }

        // Load lessons
        val lessonsJson = sharedPrefs.getString("lessons", null)
        if (lessonsJson != null) {
            val type = object : TypeToken<List<Lesson>>() {}.type
            val loadedLessons: List<Lesson> = gson.fromJson(lessonsJson, type)
            lessons.clear()
            lessons.addAll(loadedLessons)
        }

        // Load class history
        val historyJson = sharedPrefs.getString("class_history", null)
        if (historyJson != null) {
            val type = object : TypeToken<List<ClassStudentHistory>>() {}.type
            val loadedHistory: List<ClassStudentHistory> = gson.fromJson(historyJson, type)
            classHistory.clear()
            classHistory.addAll(loadedHistory)
        }
    }

    private fun saveStudentsToStorage() {
        val json = gson.toJson(students)
        sharedPrefs.edit().putString("students", json).apply()
    }

    private fun savePaymentsToStorage() {
        val json = gson.toJson(payments)
        sharedPrefs.edit().putString("payments", json).apply()
    }

    private fun saveClassesToStorage() {
        val json = gson.toJson(classes)
        sharedPrefs.edit().putString("classes", json).apply()
    }

    private fun saveLessonsToStorage() {
        val json = gson.toJson(lessons)
        sharedPrefs.edit().putString("lessons", json).apply()
    }

    private fun saveClassHistoryToStorage() {
        val json = gson.toJson(classHistory)
        sharedPrefs.edit().putString("class_history", json).apply()
    }

    // Student operations
    fun addStudent(student: Student) {
        students.add(student)
        saveStudentsToStorage()
    }

    fun updateStudent(updatedStudent: Student) {
        val index = students.indexOfFirst { it.id == updatedStudent.id }
        if (index != -1) {
            students[index] = updatedStudent
            saveStudentsToStorage()
        }
    }

    fun deleteStudent(studentId: String) {
        students.removeIf { it.id == studentId }
        // Also delete associated payments
        payments.removeIf { it.studentId == studentId }
        saveStudentsToStorage()
        savePaymentsToStorage()
    }

    fun getStudentById(id: String): Student? {
        return students.find { it.id == id }
    }

    fun getAllStudents(): List<Student> {
        return students.toList()
    }

    fun getActiveStudents(): List<Student> {
        return students.filter { it.isActive }.sortedBy { it.name }
    }

    fun getInactiveStudents(): List<Student> {
        return students.filter { !it.isActive }.sortedBy { it.name }
    }

    // Payment operations
    fun addPayment(payment: Payment) {
        payments.add(payment)
        savePaymentsToStorage()
    }

    fun updatePayment(updatedPayment: Payment) {
        val index = payments.indexOfFirst { it.id == updatedPayment.id }
        if (index != -1) {
            payments[index] = updatedPayment
            savePaymentsToStorage()
        }
    }

    fun deletePayment(paymentId: String) {
        payments.removeIf { it.id == paymentId }
        savePaymentsToStorage()
    }

    fun getPaymentById(id: String): Payment? {
        return payments.find { it.id == id }
    }

    fun getAllPayments(): List<Payment> {
        return payments.toList()
    }

    fun getPaymentsByStudent(studentId: String): List<Payment> {
        return payments.filter { it.studentId == studentId }
            .sortedByDescending { it.paymentDate }
    }

    // Statistics
    fun getTotalPaymentsThisMonth(): Double {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        return payments.filter { payment ->
            calendar.apply { timeInMillis = payment.paymentDate }.get(java.util.Calendar.MONTH) == currentMonth &&
            calendar.apply { timeInMillis = payment.paymentDate }.get(java.util.Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }

    fun getTotalPaymentsThisYear(): Double {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        return payments.filter { payment ->
            calendar.apply { timeInMillis = payment.paymentDate }.get(java.util.Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }

    fun getActiveStudentCount(): Int {
        return students.count { it.isActive }
    }

    fun getInactiveStudentCount(): Int {
        return students.count { !it.isActive }
    }

    // Class operations
    fun addClass(studentClass: StudentClass) {
        classes.add(studentClass)
        saveClassesToStorage()
    }

    fun updateClass(updatedClass: StudentClass) {
        val index = classes.indexOfFirst { it.id == updatedClass.id }
        if (index != -1) {
            classes[index] = updatedClass
            saveClassesToStorage()
        }
    }

    fun deleteClass(classId: String) {
        classes.removeIf { it.id == classId }
        // Also delete associated lessons and history
        lessons.removeIf { it.classId == classId }
        classHistory.removeIf { it.classId == classId }
        saveClassesToStorage()
        saveLessonsToStorage()
        saveClassHistoryToStorage()
    }

    fun getClassById(id: String): StudentClass? {
        return classes.find { it.id == id }
    }

    fun getAllClasses(): List<StudentClass> {
        return classes.toList()
    }

    fun getActiveClasses(): List<StudentClass> {
        return classes.filter { it.isActive }.sortedBy { it.name }
    }

    fun getInactiveClasses(): List<StudentClass> {
        return classes.filter { !it.isActive }.sortedBy { it.name }
    }

    fun getClassesByStudent(studentId: String): List<StudentClass> {
        return classes.filter { studentId in it.studentIds }
    }

    fun addStudentToClass(classId: String, studentId: String) {
        val classIndex = classes.indexOfFirst { it.id == classId }
        if (classIndex != -1) {
            val studentClass = classes[classIndex]
            if (studentId !in studentClass.studentIds) {
                studentClass.studentIds.add(studentId)
                classes[classIndex] = studentClass
                saveClassesToStorage()

                // Record in history
                val historyEntry = ClassStudentHistory(
                    classId = classId,
                    studentId = studentId,
                    action = HistoryAction.JOINED
                )
                classHistory.add(historyEntry)
                saveClassHistoryToStorage()
            }
        }
    }

    fun removeStudentFromClass(classId: String, studentId: String) {
        val classIndex = classes.indexOfFirst { it.id == classId }
        if (classIndex != -1) {
            val studentClass = classes[classIndex]
            if (studentId in studentClass.studentIds) {
                studentClass.studentIds.remove(studentId)
                classes[classIndex] = studentClass
                saveClassesToStorage()

                // Record in history
                val historyEntry = ClassStudentHistory(
                    classId = classId,
                    studentId = studentId,
                    action = HistoryAction.LEFT
                )
                classHistory.add(historyEntry)
                saveClassHistoryToStorage()
            }
        }
    }

    fun getStudentsLeftFromClass(classId: String): Int {
        return classHistory.count { it.classId == classId && it.action == HistoryAction.LEFT }
    }

    // Lesson operations
    fun addLesson(lesson: Lesson) {
        lessons.add(lesson)
        saveLessonsToStorage()
    }

    fun updateLesson(updatedLesson: Lesson) {
        val index = lessons.indexOfFirst { it.id == updatedLesson.id }
        if (index != -1) {
            lessons[index] = updatedLesson
            saveLessonsToStorage()
        }
    }

    fun deleteLesson(lessonId: String) {
        lessons.removeIf { it.id == lessonId }
        saveLessonsToStorage()
    }

    fun getLessonById(id: String): Lesson? {
        return lessons.find { it.id == id }
    }

    fun getAllLessons(): List<Lesson> {
        return lessons.toList()
    }

    fun getLessonsByClass(classId: String): List<Lesson> {
        return lessons.filter { it.classId == classId }
            .sortedByDescending { it.date }
    }

    fun getCompletedLessonsCount(classId: String): Int {
        return lessons.count { it.classId == classId && it.isCompleted }
    }

    fun getPendingLessonsCount(classId: String): Int {
        return lessons.count { it.classId == classId && !it.isCompleted }
    }

    /**
     * Check if a student is already enrolled in any class
     * Returns the class if found, null otherwise
     */
    fun getStudentCurrentClass(studentId: String): StudentClass? {
        return classes.find { studentId in it.studentIds }
    }

    /**
     * Check if a student is enrolled in any class other than the specified one
     * Returns the other class if found, null otherwise
     */
    fun getStudentOtherClass(studentId: String, excludeClassId: String): StudentClass? {
        return classes.find { it.id != excludeClassId && studentId in it.studentIds }
    }

    /**
     * Check if a student can be added to a class
     * Returns true if the student is not in any other class
     */
    fun canStudentBeAddedToClass(studentId: String, classId: String): Boolean {
        val currentClass = getStudentCurrentClass(studentId)
        return currentClass == null || currentClass.id == classId
    }

    // Export all data for backup
    fun getAllDataForExport(context: Context): StudentDataExport {
        // Get notes from NoteManager
        val noteManager = NoteManager(context)
        val notes = noteManager.getAllNotes()

        // Get debts from DebtDatabase
        val debtDatabase = DebtDatabase.getInstance(context)
        val debts = debtDatabase.getAllDebts()

        // Get tasks from TaskManager
        val taskManager = TaskManager.getInstance(context)
        val tasks = taskManager.getAllTasks()

        return StudentDataExport(
            students = students.toList(),
            payments = payments.toList(),
            classes = classes.toList(),
            lessons = lessons.toList(),
            classHistory = classHistory.toList(),
            notes = notes,
            debts = debts,
            tasks = tasks,
            exportDate = System.currentTimeMillis()
        )
    }

    // Import data from backup
    fun importData(data: StudentDataExport, mergeMode: Boolean = false, context: Context) {
        if (mergeMode) {
            // Merge mode: add new items, skip duplicates
            data.students.forEach { student ->
                if (students.none { it.id == student.id }) {
                    students.add(student)
                }
            }
            data.payments.forEach { payment ->
                if (payments.none { it.id == payment.id }) {
                    payments.add(payment)
                }
            }
            data.classes.forEach { studentClass ->
                if (classes.none { it.id == studentClass.id }) {
                    classes.add(studentClass)
                }
            }
            data.lessons.forEach { lesson ->
                if (lessons.none { it.id == lesson.id }) {
                    lessons.add(lesson)
                }
            }
            data.classHistory.forEach { history ->
                if (classHistory.none { it.id == history.id }) {
                    classHistory.add(history)
                }
            }
        } else {
            // Replace mode: clear and replace all data
            students.clear()
            students.addAll(data.students)
            payments.clear()
            payments.addAll(data.payments)
            classes.clear()
            classes.addAll(data.classes)
            lessons.clear()
            lessons.addAll(data.lessons)
            classHistory.clear()
            classHistory.addAll(data.classHistory)
        }

        // Save all data
        saveStudentsToStorage()
        savePaymentsToStorage()
        saveClassesToStorage()
        saveLessonsToStorage()
        saveClassHistoryToStorage()

        // Import notes
        val noteManager = NoteManager(context)
        val existingNotes = noteManager.getAllNotes().toMutableList()
        var notesAdded = 0
        data.notes.forEach { note ->
            if (mergeMode) {
                // Only add if not exists
                if (existingNotes.none { it.id == note.id }) {
                    if (noteManager.saveNote(note)) {
                        existingNotes.add(note) // Keep track of added notes
                        notesAdded++
                    }
                }
            } else {
                if (noteManager.saveNote(note)) {
                    notesAdded++
                }
            }
        }
        android.util.Log.d("StudentDatabase", "Notes import: ${data.notes.size} in backup, $notesAdded added")

        // Import debts
        val debtDatabase = DebtDatabase.getInstance(context)
        val existingDebts = debtDatabase.getAllDebts()
        data.debts.forEach { debt ->
            if (mergeMode) {
                // Only add if not exists
                if (existingDebts.none { it.id == debt.id }) {
                    debtDatabase.addDebt(debt)
                }
            } else {
                // In replace mode, delete existing and add new
                debtDatabase.deleteDebt(debt.id)
                debtDatabase.addDebt(debt)
            }
        }

        // Import tasks
        val taskManager = TaskManager.getInstance(context)
        val existingTasks = taskManager.getAllTasks()
        var tasksAdded = 0
        data.tasks.forEach { task ->
            if (mergeMode) {
                // Only add if not exists
                if (existingTasks.none { it.id == task.id }) {
                    taskManager.saveTask(task)
                    tasksAdded++
                }
            } else {
                // In replace mode, save (will overwrite if exists)
                taskManager.saveTask(task)
                tasksAdded++
            }
        }
        android.util.Log.d("StudentDatabase", "Tasks import: ${data.tasks.size} in backup, $tasksAdded added")
    }

    // Clear all data
    fun clearAllData() {
        students.clear()
        payments.clear()
        classes.clear()
        lessons.clear()
        classHistory.clear()

        saveStudentsToStorage()
        savePaymentsToStorage()
        saveClassesToStorage()
        saveLessonsToStorage()
        saveClassHistoryToStorage()
    }
}

/**
 * Data class for exporting all student-related data
 */
data class StudentDataExport(
    val students: List<Student>,
    val payments: List<Payment>,
    val classes: List<StudentClass>,
    val lessons: List<Lesson>,
    val classHistory: List<ClassStudentHistory>,
    val notes: List<Note> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val tasks: List<DailyTask> = emptyList(),
    val exportDate: Long,
    val version: Int = 3
)
