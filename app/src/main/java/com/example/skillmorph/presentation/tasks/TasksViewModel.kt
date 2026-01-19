
package com.example.skillmorph.presentation.tasks

import android.util.Log
import androidx.compose.material3.DatePickerDefaults.dateFormatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.data.remote.SkillMorphApi
import com.example.skillmorph.data.remote.TaskDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// Keep your existing helper class
data class CalendarDate(
    val dayOfWeek: String,
    val dayOfMonth: String,
    val date: Date
)

data class TasksUiState(
    val selectedDate: Date = Date(),
    val calendarDates: List<CalendarDate> = emptyList(),
    val tasks: List<TaskDto> = emptyList() // Changed from TaskEntity to TaskDto
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val api: SkillMorphApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState = _uiState.asStateFlow()

    init {
        selectDate(Date()) // Select today on init
    }

    fun selectDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        generateCalendarDates(date)
        fetchTasksForDate(date)
    }

    private fun generateCalendarDates(selectedDate: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        val dates = List(7) {
            val d = calendar.time
            val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(d)
            val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(d)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            CalendarDate(dayOfWeek, dayOfMonth, d)
        }
        _uiState.value = _uiState.value.copy(calendarDates = dates)
    }

    // 1. Fetch based on the SELECTED date
    private fun fetchTasksForDate(date: Date) {
        viewModelScope.launch {
            try {
                // Use SimpleDateFormat to format the date for the API
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateString = formatter.format(date)

                // Pass the date to the API
                val tasks = api.getTasks(dateString)
                _uiState.value = _uiState.value.copy(tasks = tasks)
            } catch (e: Exception) {
                Log.e("TasksVM", "Error fetching tasks: ${e.message}")
                _uiState.value = _uiState.value.copy(tasks = emptyList())
            }
        }
    }

    // Add the "Side Quest" logic you wanted
    fun addSideQuest(title: String) {
        viewModelScope.launch {
            try {
                // Use the date currently selected in the calendar
                val selectedDate = _uiState.value.selectedDate
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateString = dateFormatter.format(selectedDate)

                val body = mapOf(
                    "title" to title,
                    "date" to dateString
                )
                api.createSideQuest(body)

                // Refresh list
                fetchTasksForDate(selectedDate)
            } catch (e: Exception) {
                Log.e("TasksVM", "Error adding task: ${e.message}")
            }
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun onTaskChecked(task: TaskDto, isChecked: Boolean) {
        if (!isChecked) return // specific logic for unchecking if needed, for now let's focus on completing

        // Optimistic Update: Update UI immediately so it feels fast
        val updatedList = _uiState.value.tasks.map { item ->
            if (item.id == task.id) {
                // Mark as completed locally so it stays on screen
                item.copy(isCompleted = true)
            } else {
                item
            }
        }
        _uiState.value = _uiState.value.copy(tasks = updatedList)

        viewModelScope.launch {
            try {
                if (task.type == "GOAL") {
                    // It's a Goal Step: Call the complex Goal API
                    // We use task.id as dayNumber because backend sent it as toString(day_number)
                    val dayNum = task.id?.toIntOrNull() ?: 1
                    val goalId = task.goalId ?: return@launch

                    api.completeGoalTask(goalId, dayNum)

                    fetchTasksForDate(Date())
                } else {
                    // It's a Side Quest: Call the simple Task API
                    val taskId = task.id ?: return@launch
                    api.completeSideQuest(taskId)
                }
            } catch (e: Exception) {
                Log.e("TasksVM", "Failed to complete task: ${e.message}")
                fetchTasksForDate(_uiState.value.selectedDate) // Revert UI on failure
            }
        }
    }
}
