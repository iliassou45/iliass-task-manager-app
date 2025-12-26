package com.iliass.iliass.model

data class FlashCardFolder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val cardCount: Int = 0,
    val color: String = "#2196F3"
)

// Updated FlashCard to include folder reference
data class FlashCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    val word: String,
    val meaning: String,
    val language: Language,
    val folderId: String? = null,  // References parent folder
    val createdAt: Long = System.currentTimeMillis(),
    var lastReviewed: Long = 0,
    var timesReviewed: Int = 0,
    var correctCount: Int = 0
)

enum class Language {
    ENGLISH,
    FRENCH
}

data class FlashCardStats(
    val totalCards: Int,
    val englishCards: Int,
    val frenchCards: Int,
    val totalReviews: Int,
    val accuracy: Float
)