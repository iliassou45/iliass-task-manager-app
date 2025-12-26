package com.iliass.iliass.model

import java.io.Serializable
import java.util.*

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val contentWithFormatting: String = "", // Store HTML formatted content
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val color: Int = generateRandomColor(), // Add color field
    val category: String = "" // Category/Folder for organizing notes
) : Serializable {
    companion object {
        private fun generateRandomColor(): Int {
            val colors = listOf(
                0xFFEF5350.toInt(), // Red
                0xFFEC407A.toInt(), // Pink
                0xFFAB47BC.toInt(), // Purple
                0xFF7E57C2.toInt(), // Deep Purple
                0xFF5C6BC0.toInt(), // Indigo
                0xFF42A5F5.toInt(), // Blue
                0xFF26C6DA.toInt(), // Cyan
                0xFF26A69A.toInt(), // Teal
                0xFF66BB6A.toInt(), // Green
                0xFF9CCC65.toInt(), // Light Green
                0xFFFFEE58.toInt(), // Yellow
                0xFFFFCA28.toInt(), // Amber
                0xFFFF7043.toInt(), // Deep Orange
                0xFF8D6E63.toInt()  // Brown
            )
            return colors.random()
        }
    }
}