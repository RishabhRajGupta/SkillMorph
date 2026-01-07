package com.example.skillmorph.utils

import android.content.Context
import android.net.Uri
import com.example.skillmorph.data.local.entities.KnowledgeChunkEntity
import com.example.skillmorph.domain.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileProcessor(
    private val context: Context,
    private val repository: ChatRepository
) {
    suspend fun processFile(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            val fileName = uri.lastPathSegment ?: "Unknown_File"

            // 1. EXTRACTION ZONE
            val extractedText = when {
                mimeType == "application/pdf" -> {
                    // --- BRIDGE FOR API DEVELOPER ---
                    // TODO: API Guy needs to add PDFBox or ML Kit here.
                    // For now, we return a placeholder so the architect can test the flow.
                    "PDF CONTENT PLACEHOLDER: The API Developer will implement PDF text extraction here for file: $fileName"
                }
                else -> {
                    // Standard Text reading
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                }
            }

            if (extractedText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("File is empty or unreadable"))
            }

            // 2. THE GRINDER (Chunking Logic)
            val words = extractedText.trim().split(Regex("\\s+"))
            val chunkSize =  500
            var chunksCreated = 0

            for (i in words.indices step chunkSize) {
                val end = minOf(i + chunkSize, words.size)
                val chunkText = words.subList(i, end).joinToString(" ")

                val entity = KnowledgeChunkEntity(
                    content = chunkText,
                    source = fileName,
                    tags = "User Upload",
                    lastUpdated = System.currentTimeMillis()
                )
                repository.saveKnowledge(entity)
                chunksCreated++
            }

            Result.success(chunksCreated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}