package com.iliass.iliass.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iliass.iliass.model.Student
import com.iliass.iliass.model.Payment

class StudentDatabase private constructor(context: Context) {

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("student_payment_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val students = mutableListOf<Student>()
    private val payments = mutableListOf<Payment>()

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
    }

    private fun saveStudentsToStorage() {
        val json = gson.toJson(students)
        sharedPrefs.edit().putString("students", json).apply()
    }

    private fun savePaymentsToStorage() {
        val json = gson.toJson(payments)
        sharedPrefs.edit().putString("payments", json).apply()
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
}
