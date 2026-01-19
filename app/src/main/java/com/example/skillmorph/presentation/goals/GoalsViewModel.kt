
package com.example.skillmorph.presentation.goals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.data.local.entities.GoalEntity
import com.example.skillmorph.data.remote.SkillMorphApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val api: SkillMorphApi
) : ViewModel() {

    private val _goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    val goals = _goals.asStateFlow()

    init {
        fetchGoals()
    }

    private fun fetchGoals() {
        viewModelScope.launch {
            try {
                // 1. Get Real List from Backend
                val backendGoals = api.getGoals()

                // 2. Map to your UI Entity
                // (Since backend doesn't send progress yet, we mock it for the UI visuals)
                val uiGoals = backendGoals.map { dto ->
                    GoalEntity(
                        id = dto.id,
                        title = dto.title,
                        description = dto.category, // Show category as description
                        startDate = System.currentTimeMillis(),
                        endDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
                        totalTasks = 30,
                        completedTasks = (Math.random() * 15).toInt(), // Random mock progress
                        isImportant = false,
                        progressPercentage = (Math.random() * 100).toInt()
                    )
                }
                _goals.value = uiGoals

            } catch (e: Exception) {
                // Fallback to empty or error state
                Log.e("GoalsVM", "Error fetching goals: ${e.message}")
            }
        }
    }
}
