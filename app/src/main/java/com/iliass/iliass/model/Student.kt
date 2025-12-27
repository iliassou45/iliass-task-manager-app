package com.iliass.iliass.model

import java.io.Serializable
import java.util.UUID

data class Student(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val monthlyAmount: Double,
    val phone: String = "",
    val email: String = "",
    val enrollmentDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = ""
) : Serializable {

    fun getTotalPaid(payments: List<Payment>): Double {
        return payments.filter { it.studentId == id }.sumOf { it.amount }
    }

    fun getTotalPaidThisMonth(payments: List<Payment>): Double {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        return payments.filter { payment ->
            payment.studentId == id &&
            calendar.apply { timeInMillis = payment.paymentDate }.get(java.util.Calendar.MONTH) == currentMonth &&
            calendar.apply { timeInMillis = payment.paymentDate }.get(java.util.Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }

    fun getTotalPaidThisYear(payments: List<Payment>): Double {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        val currentYear = calendar.get(java.util.Calendar.YEAR)

        return payments.filter { payment ->
            payment.studentId == id &&
            calendar.apply { timeInMillis = payment.paymentDate }.get(java.util.Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }

    fun getCurrentDebt(payments: List<Payment>): Double {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now

        val enrollmentCalendar = java.util.Calendar.getInstance()
        enrollmentCalendar.timeInMillis = enrollmentDate

        // Calculate number of months since enrollment
        val monthsPassed = ((calendar.get(java.util.Calendar.YEAR) - enrollmentCalendar.get(java.util.Calendar.YEAR)) * 12) +
                          (calendar.get(java.util.Calendar.MONTH) - enrollmentCalendar.get(java.util.Calendar.MONTH)) + 1

        val totalExpected = monthlyAmount * monthsPassed
        val totalPaid = getTotalPaid(payments)

        return maxOf(0.0, totalExpected - totalPaid)
    }

    fun getNextPaymentDate(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        return calendar.timeInMillis
    }
}
