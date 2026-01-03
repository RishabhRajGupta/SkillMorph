
package com.example.skillmorph.presentation.goaldetail

import androidx.lifecycle.ViewModel
import com.example.skillmorph.presentation.goaldetail.models.DayPlan
import com.example.skillmorph.presentation.goaldetail.models.TimelineStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MetroMapViewModel @Inject constructor() : ViewModel() {

    private val _dayPlans = MutableStateFlow<List<DayPlan>>(emptyList())
    val dayPlans = _dayPlans.asStateFlow()

    init {
        // For now, load the same sample data used in the preview.
        // Later, this will fetch data from the repository based on a goalId.
        _dayPlans.value = listOf(
            DayPlan(1, "Day 1", "Python Basics", false, TimelineStatus.COMPLETED, emptyList(), ""),
            DayPlan(2, "Day 2", "Loops & Logic", false, TimelineStatus.CURRENT, listOf("Watch Video (5 min)", "Read PDF (Pg 12)", "Read Summary"), ""),
            DayPlan(3, "Day 3", "Advanced Functions", false, TimelineStatus.LOCKED, emptyList(), ""),
            DayPlan(4, "Day 4", "Buffer Day", true, TimelineStatus.LOCKED, emptyList(), "")
        )
    }

    fun completeDay(dayId: Int) {
        // TODO: Implement logic to handle day completion
    }
}
