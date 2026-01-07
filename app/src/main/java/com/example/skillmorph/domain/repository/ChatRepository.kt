package com.example.skillmorph.domain.repository

import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.data.local.entities.KnowledgeChunkEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Save a message (Voice or Text)
    suspend fun saveMessage(message: ChatEntity)

    // In the ChatRepository interface
    suspend fun saveKnowledge(chunk: KnowledgeChunkEntity)

    // Get all history
    fun getAllMessages(): Flow<List<ChatEntity>>

    fun searchKnowledge(query: String): kotlinx.coroutines.flow.Flow<List<com.example.skillmorph.data.local.entities.KnowledgeChunkEntity>>
}