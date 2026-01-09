package com.example.skillmorph.data.repository

import com.example.skillmorph.data.local.SkillMorphDao
import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.data.local.entities.GoalEntity
import com.example.skillmorph.data.local.entities.KnowledgeChunkEntity
import com.example.skillmorph.data.local.entities.TaskEntity
import com.example.skillmorph.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val dao: SkillMorphDao
) : ChatRepository {

    override suspend fun saveMessage(message: ChatEntity) {
        dao.insertChatMessage(message)
    }

    override fun searchKnowledge(query: String) = dao.searchKnowledge(query)

    override fun getAllMessages(): Flow<List<ChatEntity>> {
        return dao.getAllChatMessages()
    }

    override suspend fun saveKnowledge(chunk: com.example.skillmorph.data.local.entities.KnowledgeChunkEntity) {
        dao.insertKnowledgeChunk(chunk)
    }

    // Inside ChatRepositoryImpl.kt
    override fun getAllTasks() = dao.getTasksForDate(System.currentTimeMillis()) // Starting with today's tasks
    override suspend fun saveTask(task: com.example.skillmorph.data.local.entities.TaskEntity) = dao.insertTask(task)
    override suspend fun toggleTask(taskId: Int, isCompleted: Boolean) = dao.updateTaskStatus(taskId, isCompleted)

    // 4. Strategic Goals & Streaks
    override fun getAllGoals(): Flow<List<GoalEntity>> {
        return dao.getAllGoals()
    }

    override suspend fun saveGoal(goal: GoalEntity) {
        dao.insertGoal(goal)
    }

}