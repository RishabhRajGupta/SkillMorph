
package com.example.skillmorph.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skillmorph.data.local.entities.TaskEntity
import com.example.skillmorph.domain.repository.TasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// A simple data class to hold date info for the calendar
data class CalendarDate(
    val dayOfWeek: String, // e.g., "Mon"
    val dayOfMonth: String, // e.g., "23"
    val date: Date
)

data class TasksUiState(
    val selectedDate: Date = Date(),
    val calendarDates: List<CalendarDate> = emptyList(),
    val tasks: List<TaskEntity> = emptyList()
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TasksRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState = _uiState.asStateFlow()

    init {
        selectDate(Date()) // Select today's date on init
    }

    fun selectDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        generateCalendarDates(date)
        fetchTasksForDate(date)
    }

    private fun generateCalendarDates(selectedDate: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        // Set calendar to the start of the week (e.g., Sunday or Monday)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)

        val dates = List(7) { 
            val date = calendar.time
            val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
            val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(date)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            CalendarDate(dayOfWeek, dayOfMonth, date)
        }
        _uiState.value = _uiState.value.copy(calendarDates = dates)
    }

    private fun fetchTasksForDate(date: Date) {
        // Reset time to midnight to ensure we fetch all tasks for the given day
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        repository.getTasksForDate(calendar.timeInMillis)
            .onEach { tasks ->
                _uiState.value = _uiState.value.copy(tasks = tasks)
            }
            .launchIn(viewModelScope)
    }

    fun onTaskCheckedChange(task: TaskEntity, isChecked: Boolean) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = isChecked))
            // The flow from the database will automatically update the UI
        }
    }
}
