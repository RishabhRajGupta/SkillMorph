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
    suspend fun processFile(uri: android.net.Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            // ARCHITECT NOTE: We explicitly check the file type
            val mimeType = contentResolver.getType(uri) ?: "text/plain"
            val fileName = uri.lastPathSegment ?: "Unknown_File"

            val extractedText = when {
                // PDF LOADING BAY
                mimeType == "application/pdf" || fileName.endsWith(".pdf") -> {
                    // This is where the API Guy will use a library like PdfBox
                    // For now, we return a "Searchable Placeholder" for testing
                    "KNOWLEDGE_BASE_PDF_MARKER: This file $fileName is a PDF. " +
                            "Content extraction will be active once the PDF library is linked."
                }

                // TEXT LOADING BAY
                else -> {
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