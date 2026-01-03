
package com.example.skillmorph.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillmorph.data.local.entities.TaskEntity
import com.example.skillmorph.ui.theme.NeonBlue
import java.util.Calendar
import java.util.Date

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showCalendarDialog by remember { mutableStateOf(false) }

    if (showCalendarDialog) {
        CalendarDialog(
            initialDate = uiState.selectedDate,
            onDateSelected = { 
                viewModel.selectDate(it)
                showCalendarDialog = false 
            },
            onDismiss = { showCalendarDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        WeekCalendar(
            calendarDates = uiState.calendarDates,
            selectedDate = uiState.selectedDate,
            onDateSelected = { viewModel.selectDate(it) }
        )
        
        IconButton(onClick = { showCalendarDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Open calendar", tint = Color.White)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tasks found for this date.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                items(uiState.tasks) { task ->
                    TaskItem(
                        task = task,
                        onCheckedChange = { viewModel.onTaskCheckedChange(task, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDialog(
    initialDate: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.time)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(Date(it))
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A2E))
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF1A1A2E),
                titleContentColor = Color.White,
                headlineContentColor = NeonBlue,
                weekdayContentColor = Color.LightGray,
                dayContentColor = Color.White,
                disabledDayContentColor = Color.Gray,
                selectedDayContentColor = Color.Black,
                disabledSelectedDayContentColor = Color.Gray,
                selectedDayContainerColor = NeonBlue,
                todayContentColor = NeonBlue,
                todayDateBorderColor = NeonBlue,
                currentYearContentColor = Color.White,
                selectedYearContentColor = Color.Black,
                selectedYearContainerColor = NeonBlue
            )
        )
    }
}


@Composable
fun WeekCalendar(
    calendarDates: List<CalendarDate>,
    selectedDate: Date,
    onDateSelected: (Date) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        calendarDates.forEach { date ->
            Day(
                date = date,
                isSelected = isSameDay(date.date, selectedDate),
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
fun Day(date: CalendarDate, isSelected: Boolean, onDateSelected: (Date) -> Unit) {
    val backgroundColor = if (isSelected) NeonBlue else Color.Transparent
    val textColor = if (isSelected) Color.Black else Color.White

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onDateSelected(date.date) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ensure opacity is applied correctly to the text color
        Text(text = date.dayOfWeek, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = date.dayOfMonth, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TaskItem(task: TaskEntity, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onCheckedChange
        )
        Text(text = task.title, color = if (task.isCompleted) Color.Gray else Color.White)
    }
}

// --- Helper Function ---

/**
 * Checks if two Date objects represent the same day of the year.
 */
private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
