
package com.example.skillmorph.presentation.goaldetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillmorph.presentation.goaldetail.models.DayPlan
import com.example.skillmorph.presentation.goaldetail.models.TimelineStatus
import com.example.skillmorph.utils.glassEffect

// --- Style Guide Colors ---
val DarkBackground = Color(0xFF0F1014)
val Blue = Color(0xFF2D8CFF)
val Green = Color(0xFF00C853)

// --- The New Top-Level Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroMapScreen(
    goalId: String?,
    onNavigateBack: () -> Unit,
    viewModel: MetroMapViewModel = hiltViewModel() 
) {
    val dayPlans by viewModel.dayPlans.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Metro Map", 
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                     // A spacer to balance the title when there's a navigation icon
                    Spacer(modifier = Modifier.width(48.dp)) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        MetroMapTimeline(
            days = dayPlans,
            // Pass the Toggle Event
            onToggleSubtask = { dayNum, index -> viewModel.toggleSubtask(dayNum, index) },
            // Pass the Complete Event (With Context)
            onDayComplete = { dayNum -> viewModel.completeDay(dayNum, context) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// --- Main Composable ---
@Composable
fun MetroMapTimeline(
    days: List<DayPlan>,
    onDayComplete: (Int) -> Unit,
    onToggleSubtask: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
    ) {
        itemsIndexed(days) { index, day ->
            TimelineItem(day = day, onDayComplete = onDayComplete, onToggleSubtask = onToggleSubtask, isLastItem = index == days.size - 1)
        }
    }
}

@Composable
private fun TimelineItem(
    day: DayPlan,
    onDayComplete: (Int) -> Unit,
    onToggleSubtask: (Int, Int) -> Unit,
    isLastItem: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        MetroLine(state = day.status, isLastItem = isLastItem)

        Spacer(modifier = Modifier.width(16.dp))

        Box(modifier = Modifier.padding(bottom = 24.dp)) {
            NodeCard(node = day, onDayComplete = onDayComplete, onToggleSubtask = onToggleSubtask)
        }
    }
}

// --- 2. The UI Components (Line and Nodes) ---

@Composable
private fun MetroLine(state: TimelineStatus, isLastItem: Boolean) {
    val connectorColor = when (state) {
        TimelineStatus.COMPLETED -> Green
        else -> Color.Gray.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier.width(32.dp)
            .fillMaxHeight(), // Removed fillMaxHeight
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { // Changed to fillMaxSize of the Box
            val nodeRadius = 12.dp.toPx()
            val lineStroke = 6.dp.toPx()

            // Draw the line connecting the nodes
            if (!isLastItem) {
                drawLine(
                    color = connectorColor,
                    start = androidx.compose.ui.geometry.Offset(center.x, nodeRadius * 2),
                    end = androidx.compose.ui.geometry.Offset(center.x, size.height),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round
                )
            }
        }

        // The Node itself
        when (state) {
            TimelineStatus.COMPLETED -> NodeIcon(icon = Icons.Default.Check, color = Green)
            TimelineStatus.CURRENT -> CurrentNodeIcon()
            TimelineStatus.LOCKED -> NodeIcon(icon = Icons.Default.Lock, color = Color.Gray.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun NodeIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Box(modifier = Modifier.background(color, CircleShape).padding(4.dp)) {
        Icon(icon, null, tint = DarkBackground, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CurrentNodeIcon() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Blue)
            .background(Blue, CircleShape)
            .border(2.dp, Color.White, CircleShape)
    )
}

// --- 3. The Cards (Content) ---

@Composable
private fun NodeCard(node: DayPlan, onDayComplete: (Int) -> Unit, onToggleSubtask: (Int, Int) -> Unit) {
    Column(modifier = Modifier.padding(top = 2.dp)) { // Align card with node
        when (node.status) {
            TimelineStatus.COMPLETED -> CompletedTaskCard(node)
            TimelineStatus.CURRENT -> CurrentTaskCard(node, onDayComplete = onDayComplete, onToggleSubtask=onToggleSubtask)
            TimelineStatus.LOCKED -> LockedTaskCard(node)
        }
    }
}

@Composable
private fun CompletedTaskCard(node: DayPlan) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMetroEffect(color = Color.Gray.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Row {
            Text("${node.dayLabel}: ${node.topic}", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text("Score: 100%", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CurrentTaskCard(
    node: DayPlan,
    onToggleSubtask: (Int, Int) -> Unit, // <--- New Callback
    onDayComplete: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMetroEffect(color = Blue.copy(alpha = 0.15f), borderColor = Blue)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${node.dayLabel}: ${node.topic}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            // 🔴 RENDER FROM PERSISTED STATE
            if (node.subTasks.isNotEmpty()) {
                node.subTasks.forEachIndexed { index, task ->
                    val isChecked = node.subTaskStates.getOrElse(index) { false } // Safety read

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { onToggleSubtask(node.dayNumber, index) } // Click Row to toggle
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleSubtask(node.dayNumber, index) }, // Click Box to toggle
                            colors = CheckboxDefaults.colors(
                                checkedColor = Blue,
                                uncheckedColor = Color.Gray,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task,
                            color = if (isChecked) Color.Gray else Color.LightGray, // Dim text if done
                            textDecoration = if (isChecked) TextDecoration.LineThrough else null, // Strike through
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onDayComplete(node.dayNumber) },
                modifier = Modifier.fillMaxWidth(),
                // 🔴 Visual Feedback: Disable button color if not done (optional)
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (node.subTaskStates.all { it }) Blue else Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.LockOpen, null, tint = DarkBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("COMPLETE STATION", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LockedTaskCard(node: DayPlan) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassMetroEffect(color = Color.Gray.copy(alpha = 0.1f))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text("Locked • Complete previous day", color = Color.Gray, fontSize = 12.sp)
    }
}

// --- Placeholder for the .glassEffect() modifier ---
fun Modifier.glassMetroEffect(
    color: Color = Color.White.copy(alpha = 0.1f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
): Modifier = this
    .clip(shape)
    .background(color)
    .border(1.dp, borderColor, shape)

// --- Preview ---

@Preview(showBackground = true)
@Composable
private fun MetroMapScreenPreview() {
    MetroMapScreen(goalId = "1", onNavigateBack = {})
}
