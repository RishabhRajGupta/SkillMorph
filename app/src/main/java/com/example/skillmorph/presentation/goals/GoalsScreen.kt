package com.example.skillmorph.presentation.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillmorph.presentation.goals.components.GoalCard
import com.example.skillmorph.ui.theme.gradientBrush

@Composable
fun GoalsScreen(onGoalClick: (Long) -> Unit) {
    val viewModel: GoalsViewModel = hiltViewModel()
    val goals by viewModel.goals.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(goals) { goal ->
            val gradientBrush = gradientBrush()
            GoalCard(
                goal = goal, onGoalClick = onGoalClick,
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(16.dp))
                    .border(2.dp, gradientBrush, shape = RoundedCornerShape(16.dp))
                    .background(color = Color.White.copy(alpha = 0.1f))
            )

        }
    }
}
