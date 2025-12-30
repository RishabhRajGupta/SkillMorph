
package com.example.skillmorph.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Represents a single task associated with a Goal.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE // If a goal is deleted, its tasks are also deleted.
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val scheduledDate: Long, // Timestamp for the date
    val isCompleted: Boolean = false,
    val isBufferTask: Boolean = false // A special task for buffer days
)
