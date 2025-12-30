
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

    // --- Chat Operations ---
    @Insert
    suspend fun insertChatMessage(chatMessage: ChatEntity)

    @Query("SELECT * FROM chats ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatEntity>>
}
