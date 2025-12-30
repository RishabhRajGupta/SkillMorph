
package com.example.skillmorph.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single goal in the database.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val startDate: Long, // Store dates as timestamps
    val endDate: Long,
    val totalTasks: Int,
    val completedTasks: Int,
    val isImportant: Boolean,
    val progressPercentage: Int
)
