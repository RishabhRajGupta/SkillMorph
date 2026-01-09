package com.example.skillmorph.domain.repository

import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.data.local.entities.GoalEntity
import com.example.skillmorph.data.local.entities.KnowledgeChunkEntity
import com.example.skillmorph.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    // Save a message (Voice or Text)
    suspend fun saveMessage(message: ChatEntity)

    // In the ChatRepository interface
    suspend fun saveKnowledge(chunk: KnowledgeChunkEntity)

    // Get all history
    fun getAllMessages(): Flow<List<ChatEntity>>

    fun searchKnowledge(query: String): kotlinx.coroutines.flow.Flow<List<com.example.skillmorph.data.local.entities.KnowledgeChunkEntity>>

    // Add these to ChatRepository.kt interface
    fun getAllTasks(): Flow<List<com.example.skillmorph.data.local.entities.TaskEntity>>
    suspend fun saveTask(task: com.example.skillmorph.data.local.entities.TaskEntity)
    suspend fun toggleTask(taskId: Int, isCompleted: Boolean)

    //suspend fun saveGoal(goal: com.example.skillmorph.data.local.entities.GoalEntity)

    fun getAllGoals(): Flow<List<GoalEntity>>
    suspend fun saveGoal(goal: GoalEntity)

}