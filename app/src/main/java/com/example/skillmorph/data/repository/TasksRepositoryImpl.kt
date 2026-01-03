
package com.example.skillmorph.data.repository

import com.example.skillmorph.data.local.SkillMorphDao
import com.example.skillmorph.data.local.entities.TaskEntity
import com.example.skillmorph.domain.repository.TasksRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TasksRepositoryImpl @Inject constructor(
    private val dao: SkillMorphDao
) : TasksRepository {

    override fun getTasksForDate(date: Long): Flow<List<TaskEntity>> {
        return dao.getTasksForDate(date)
    }

    override suspend fun updateTask(task: TaskEntity) {
        // Here, you could also add logic to recalculate goal progress
        // and update the GoalEntity in the same transaction.
        dao.insertTask(task) // Using insert with OnConflictStrategy.REPLACE acts as an update
    }
}
