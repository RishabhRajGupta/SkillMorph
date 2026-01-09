
package com.example.skillmorph.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.data.local.entities.GoalEntity
import com.example.skillmorph.data.local.entities.TaskEntity
import com.example.skillmorph.data.local.entities.KnowledgeChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillMorphDao {

    // --- Goal Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals ORDER BY startDate DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    // --- Task Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date")
    fun getTasksForDate(date: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE goalId = :goalId ORDER BY scheduledDate ASC")
    fun getTasksForGoal(goalId: Long): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean)

    // --- Chat Operations ---
    @Insert
    suspend fun insertChatMessage(chatMessage: ChatEntity)

    @Query("SELECT * FROM chats ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatEntity>>

    // --- Knowledge Shelf Operations ---
    @Insert
    suspend fun insertKnowledgeChunk(chunk: KnowledgeChunkEntity)

    @Query("SELECT rowid, * FROM knowledge_chunks WHERE knowledge_chunks MATCH :query")
    fun searchKnowledge(query: String): Flow<List<KnowledgeChunkEntity>>
}