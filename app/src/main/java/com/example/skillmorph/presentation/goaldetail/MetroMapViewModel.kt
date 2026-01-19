
package com.example.skillmorph.presentation.goaldetail

import com.example.skillmorph.presentation.goaldetail.models.DayPlan
import com.example.skillmorph.presentation.goaldetail.models.TimelineStatus
import kotlin.collections.map
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.data.remote.SkillMorphApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetroMapViewModel @Inject constructor(
    private val api: SkillMorphApi,
    savedStateHandle: SavedStateHandle // Auto-grabs arguments from Navigation
) : ViewModel() {

    private val _dayPlans = MutableStateFlow<List<DayPlan>>(emptyList())
    val dayPlans = _dayPlans.asStateFlow()

    // 1. Get goalId from the navigation route "metro_map_screen/{goalId}"
    private val goalId: String? = savedStateHandle["goalId"]

    init {
        fetchRoadmap()
    }

    private fun fetchRoadmap() {
        if (goalId == null) return

        viewModelScope.launch {
            try {
                // 2. Fetch from Backend
                val response = api.getRoadmap(goalId)

                // 3. Map Backend Data -> UI Data
                val uiDays = response.days.map { dto ->
                    // Determine Status
                    val status = when {
                        dto.isCompleted -> TimelineStatus.COMPLETED
                        dto.isLocked -> TimelineStatus.LOCKED
                        else -> TimelineStatus.CURRENT
                    }

                    // Map to your existing UI Model
                    DayPlan(
                        dayNumber = dto.dayNumber,
                        dayLabel = "Day ${dto.dayNumber}",
                        topic = dto.topic,
                        isBufferDay = false,
                        status = status,
                        // Backend doesn't support subtasks yet, so we verify logic
                        subTasks = if (status == TimelineStatus.CURRENT)
                            listOf("Read Documentation", "Practice Code", "Review")
                        else emptyList(),
                        dateIso = ""
                    )
                }
                _dayPlans.value = uiDays

            } catch (e: Exception) {
                Log.e("MetroMapVM", "Error: ${e.message}")
            }
        }
    }

    fun completeDay(dayNumber: Int) {
        if (goalId == null) return

        // 1. Optimistic Update (Update UI instantly before server replies)
        val currentList = _dayPlans.value.toMutableList()
        val updatedList = currentList.map { day ->
            when (day.dayNumber) {
                dayNumber -> day.copy(status = TimelineStatus.COMPLETED)
                dayNumber + 1 -> day.copy(status = TimelineStatus.CURRENT) // Unlock next
                else -> day
            }
        }
        _dayPlans.value = updatedList

        // 2. Network Call
        viewModelScope.launch {
            try {
                val response = api.completeGoalTask(goalId, dayNumber)
                Log.d("MetroMapVM", "Day completed! New Goal Progress: ${response.new_progress}%")
                // You could perform a global event here to refresh the Home Screen if needed
            } catch (e: Exception) {
                Log.e("MetroMapVM", "Failed to save progress: ${e.message}")
                // Ideally, revert the optimistic update here if it failed
            }
        }
    }
}
