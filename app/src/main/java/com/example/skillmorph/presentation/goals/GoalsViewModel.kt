
package com.example.skillmorph.presentation.goals

import androidx.lifecycle.ViewModel
import com.example.skillmorph.data.local.entities.GoalEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor() : ViewModel() {

    private val _goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    val goals = _goals.asStateFlow()

    init {
        // Load dummy data for preview purposes.
        // We will replace this with a real repository call later.
        _goals.value = listOf(
            GoalEntity(
                id = 1,
                title = "Python Masterclass",
                description = "",
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
                totalTasks = 20,
                completedTasks = 9,
                isImportant = true,
                progressPercentage = 45
            ),
            GoalEntity(
                id = 2,
                title = "Calculus & Derivatives",
                description = "",
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(60),
                totalTasks = 50,
                completedTasks = 10,
                isImportant = false,
                progressPercentage = 20
            )
        )
    }
}
