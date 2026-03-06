
package com.skillmorph.skillmorph.data.local.entities

/**
 * Represents a single goal in the database.
 */
data class GoalEntity(
    val id: String, // CHANGED from Long to String
    val title: String,
    val description: String,
    val startDate: Long,
    val endDate: Long,
    val totalTasks: Int,
    val completedTasks: Int,
    val isImportant: Boolean,
    val progressPercentage: Int
)