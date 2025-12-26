// File: FlashCardManager.kt (Fixed)
package com.iliass.iliass.util

import android.content.Context
import com.iliass.iliass.model.FlashCard
import com.iliass.iliass.model.FlashCardFolder
import com.iliass.iliass.model.FlashCardStats
import com.iliass.iliass.model.Language

class FlashCardManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("flashcards", Context.MODE_PRIVATE)

    // Folders
    fun createFolder(name: String, description: String = "", color: String = "#2196F3"): FlashCardFolder {
        val folder = FlashCardFolder(name = name, description = description, color = color)
        val folders = getFolders().toMutableList()
        folders.add(folder)
        saveFolders(folders)
        return folder
    }

    fun getFolders(): List<FlashCardFolder> {
        val json = prefs.getString("folders", "[]") ?: "[]"
        return parseJsonFolders(json)
    }

    fun updateFolder(folder: FlashCardFolder) {
        val folders = getFolders().toMutableList()
        val index = folders.indexOfFirst { it.id == folder.id }
        if (index >= 0) {
            folders[index] = folder
            saveFolders(folders)
        }
    }

    fun deleteFolder(folderId: String) {
        val folders = getFolders().toMutableList()
        folders.removeAll { it.id == folderId }
        saveFolders(folders)

        val cards = getFlashCards().toMutableList()
        cards.forEach { card ->
            if (card.folderId == folderId) {
                val updated = card.copy(folderId = null)
                updateFlashCard(updated)
            }
        }
    }

    fun getFolderCardCount(folderId: String): Int {
        return getFlashCards().count { it.folderId == folderId }
    }

    // Cards
    fun addFlashCard(card: FlashCard) {
        val cards = getFlashCards().toMutableList()
        cards.add(card)
        saveCards(cards)
    }

    fun getFlashCards(): List<FlashCard> {
        val json = prefs.getString("cards", "[]") ?: "[]"
        return parseJsonCards(json)
    }

    fun updateFlashCard(card: FlashCard) {
        val cards = getFlashCards().toMutableList()
        val index = cards.indexOfFirst { it.id == card.id }
        if (index >= 0) {
            cards[index] = card
            saveCards(cards)
        }
    }

    fun deleteFlashCard(cardId: String) {
        val cards = getFlashCards().toMutableList()
        cards.removeAll { it.id == cardId }
        saveCards(cards)
    }

    fun getCardsByFolder(folderId: String?): List<FlashCard> {
        return getFlashCards().filter { it.folderId == folderId }
    }

    fun getCardsByLanguage(language: Language): List<FlashCard> {
        return getFlashCards().filter { it.language == language }
    }

    fun searchCards(query: String): List<FlashCard> {
        val lower = query.lowercase()
        return getFlashCards().filter {
            it.word.lowercase().contains(lower) ||
                    it.meaning.lowercase().contains(lower)
        }
    }

    fun moveCardToFolder(cardId: String, folderId: String?) {
        val card = getFlashCards().find { it.id == cardId }
        card?.let {
            val updated = it.copy(folderId = folderId)
            updateFlashCard(updated)
        }
    }

    fun getStats(): FlashCardStats {
        val cards = getFlashCards()
        val englishCards = cards.count { it.language == Language.ENGLISH }
        val frenchCards = cards.count { it.language == Language.FRENCH }
        val totalReviews = cards.sumOf { it.timesReviewed }
        val totalCorrect = cards.sumOf { it.correctCount }
        val accuracy = if (totalReviews > 0) (totalCorrect.toFloat() / totalReviews) * 100 else 0f

        return FlashCardStats(
            totalCards = cards.size,
            englishCards = englishCards,
            frenchCards = frenchCards,
            totalReviews = totalReviews,
            accuracy = accuracy
        )
    }

    private fun saveCards(cards: List<FlashCard>) {
        val json = cardsToJson(cards)
        prefs.edit().putString("cards", json).apply()
    }

    private fun saveFolders(folders: List<FlashCardFolder>) {
        val json = foldersToJson(folders)
        prefs.edit().putString("folders", json).apply()
    }

    private fun cardsToJson(cards: List<FlashCard>): String {
        return cards.joinToString(",", "[", "]") { card ->
            """{"id":"${card.id}","word":"${escapeJson(card.word)}","meaning":"${escapeJson(card.meaning)}","language":"${card.language}","folderId":"${card.folderId}","createdAt":${card.createdAt},"lastReviewed":${card.lastReviewed},"timesReviewed":${card.timesReviewed},"correctCount":${card.correctCount}}"""
        }
    }

    private fun parseJsonCards(json: String): List<FlashCard> {
        if (json == "[]") return emptyList()
        return try {
            val cards = mutableListOf<FlashCard>()
            var depth = 0
            var currentObject = StringBuilder()

            for (i in json.indices) {
                val char = json[i]

                when (char) {
                    '{' -> {
                        depth++
                        if (depth == 1) {
                            currentObject.clear()
                        } else {
                            currentObject.append(char)
                        }
                    }
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            // Parse this object
                            val parts = parseJsonParts(currentObject.toString())
                            cards.add(FlashCard(
                                id = parts["id"] ?: "",
                                word = unescapeJson(parts["word"] ?: ""),
                                meaning = unescapeJson(parts["meaning"] ?: ""),
                                language = Language.valueOf(parts["language"] ?: "ENGLISH"),
                                folderId = parts["folderId"]?.takeIf { it.isNotEmpty() && it != "null" },
                                createdAt = parts["createdAt"]?.toLongOrNull() ?: 0,
                                lastReviewed = parts["lastReviewed"]?.toLongOrNull() ?: 0,
                                timesReviewed = parts["timesReviewed"]?.toIntOrNull() ?: 0,
                                correctCount = parts["correctCount"]?.toIntOrNull() ?: 0
                            ))
                        } else {
                            currentObject.append(char)
                        }
                    }
                    else -> {
                        if (depth > 0) {
                            currentObject.append(char)
                        }
                    }
                }
            }

            cards
        } catch (e: Exception) {
            android.util.Log.e("FlashCardManager", "Error parsing cards JSON", e)
            emptyList()
        }
    }

    private fun foldersToJson(folders: List<FlashCardFolder>): String {
        return folders.joinToString(",", "[", "]") { folder ->
            """{"id":"${folder.id}","name":"${escapeJson(folder.name)}","description":"${escapeJson(folder.description)}","createdAt":${folder.createdAt},"color":"${folder.color}"}"""
        }
    }

    private fun parseJsonFolders(json: String): List<FlashCardFolder> {
        if (json == "[]") return emptyList()
        return try {
            val folders = mutableListOf<FlashCardFolder>()
            var depth = 0
            var currentObject = StringBuilder()

            for (i in json.indices) {
                val char = json[i]

                when (char) {
                    '{' -> {
                        depth++
                        if (depth == 1) {
                            currentObject.clear()
                        } else {
                            currentObject.append(char)
                        }
                    }
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            // Parse this object
                            val parts = parseJsonParts(currentObject.toString())
                            folders.add(FlashCardFolder(
                                id = parts["id"] ?: "",
                                name = unescapeJson(parts["name"] ?: ""),
                                description = unescapeJson(parts["description"] ?: ""),
                                createdAt = parts["createdAt"]?.toLongOrNull() ?: 0,
                                color = parts["color"] ?: "#2196F3"
                            ))
                        } else {
                            currentObject.append(char)
                        }
                    }
                    else -> {
                        if (depth > 0) {
                            currentObject.append(char)
                        }
                    }
                }
            }

            folders
        } catch (e: Exception) {
            android.util.Log.e("FlashCardManager", "Error parsing folders JSON", e)
            emptyList()
        }
    }

    private fun parseJsonParts(json: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var index = 0

        while (index < json.length) {
            // Find the start of a key
            val keyStart = json.indexOf('"', index)
            if (keyStart == -1) break

            val keyEnd = json.indexOf('"', keyStart + 1)
            if (keyEnd == -1) break

            val key = json.substring(keyStart + 1, keyEnd)

            // Find the start of the value
            val colonIndex = json.indexOf(':', keyEnd)
            if (colonIndex == -1) break

            // Skip whitespace after colon
            var valueStart = colonIndex + 1
            while (valueStart < json.length && json[valueStart].isWhitespace()) {
                valueStart++
            }
            if (valueStart >= json.length) break

            // Extract the value
            var value = ""

            if (json[valueStart] == '"') {
                // String value - find the closing quote, accounting for escaped quotes
                var searchIndex = valueStart + 1
                var foundEnd = false
                while (searchIndex < json.length) {
                    if (json[searchIndex] == '"' && json.getOrNull(searchIndex - 1) != '\\') {
                        value = json.substring(valueStart + 1, searchIndex)
                        index = searchIndex + 1
                        foundEnd = true
                        break
                    }
                    searchIndex++
                }
                if (!foundEnd) {
                    // Malformed JSON, skip to next
                    index = json.length
                }
            } else {
                // Number value - find the next comma or closing brace
                val valueEnd = json.indexOfAny(charArrayOf(',', '}'), valueStart)
                if (valueEnd == -1) {
                    value = json.substring(valueStart).trim()
                    index = json.length
                } else {
                    value = json.substring(valueStart, valueEnd).trim()
                    index = valueEnd
                }
            }

            // Only add to map if we found a value
            if (value.isNotEmpty() || key.isNotEmpty()) {
                map[key] = value
            }
        }

        return map
    }

    private fun escapeJson(str: String): String {
        val builder = StringBuilder()
        for (char in str) {
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                else -> {
                    // Handle all other control characters and special unicode
                    if (char < ' ') {
                        builder.append(String.format("\\u%04x", char.code))
                    } else {
                        builder.append(char)
                    }
                }
            }
        }
        return builder.toString()
    }

    private fun unescapeJson(str: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < str.length) {
            if (str[i] == '\\' && i + 1 < str.length) {
                when (str[i + 1]) {
                    'n' -> {
                        builder.append('\n')
                        i += 2
                    }
                    'r' -> {
                        builder.append('\r')
                        i += 2
                    }
                    't' -> {
                        builder.append('\t')
                        i += 2
                    }
                    'b' -> {
                        builder.append('\b')
                        i += 2
                    }
                    'f' -> {
                        builder.append('\u000C')
                        i += 2
                    }
                    '"' -> {
                        builder.append('"')
                        i += 2
                    }
                    '\\' -> {
                        builder.append('\\')
                        i += 2
                    }
                    'u' -> {
                        // Handle unicode escape sequences like \u000a
                        if (i + 5 < str.length) {
                            try {
                                val unicode = str.substring(i + 2, i + 6).toInt(16)
                                builder.append(unicode.toChar())
                                i += 6
                            } catch (e: Exception) {
                                builder.append(str[i])
                                i++
                            }
                        } else {
                            builder.append(str[i])
                            i++
                        }
                    }
                    else -> {
                        builder.append(str[i])
                        i++
                    }
                }
            } else {
                builder.append(str[i])
                i++
            }
        }
        return builder.toString()
    }
}