
package com.example.skillmorph.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.skillmorph.data.local.entities.ChatEntity
import com.example.skillmorph.data.local.entities.GoalEntity
import com.example.skillmorph.data.local.entities.TaskEntity
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

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE scheduledDate = :date")
    fun getTasksForDate(date: Long): Flow<List<TaskEntity>>

    // Added function to get all tasks for a specific goal ID, sorted by date
    @Query("SELECT * FROM tasks WHERE goalId = :goalId ORDER BY scheduledDate ASC")
    fun getTasksForGoal(goalId: Long): Flow<List<TaskEntity>>

    // --- Chat Operations ---
    @Insert
    suspend fun insertChatMessage(chatMessage: ChatEntity)

    @Query("SELECT * FROM chats ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatEntity>>

    // --- Knowledge Shelf Operations (FTS4) ---

    // 1. Hook for the API Guy:  call this to save chunks of PDF text.
    @Insert
    suspend fun insertKnowledgeChunk(chunk: com.example.skillmorph.data.local.entities.KnowledgeChunkEntity)

    // 2. The Magic Search Query
    // This uses the special "MATCH" command which is specific to FTS4 tables.
    // It finds knowledge instantly, ranked by relevance.
    @Query("""
        SELECT rowid, * FROM knowledge_chunks 
        WHERE knowledge_chunks MATCH :query
    """)
    fun searchKnowledge(query: String): kotlinx.coroutines.flow.Flow<List<com.example.skillmorph.data.local.entities.KnowledgeChunkEntity>>
}
