package com.example.skillmorph.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String?,
    val startDate: Long,
    val endDate: Long,
    val totalTasks: Int,
    val completedTasks: Int,
    val isImportant: Boolean,
    val progressPercentage: Int,
    // Ensure these two are exactly as written:
    val currentStreak: Int = 0,
    val lastCompletionTimestamp: Long = 0L
)
