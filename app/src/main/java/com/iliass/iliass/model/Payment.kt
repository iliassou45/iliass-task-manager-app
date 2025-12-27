package com.iliass.iliass.model

import java.io.Serializable
import java.util.UUID

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val studentId: String,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val monthFor: String = "" // e.g., "January 2025"
) : Serializable
