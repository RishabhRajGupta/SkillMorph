
package com.example.skillmorph.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Represents a single task associated with a Goal.
 */
@Entity(
    tableName = "tasks",
    // NEW: We added this indices line to fix the error
    indices = [androidx.room.Index(value = ["goalId"])],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = androidx.room.ForeignKey.CASCADE
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
