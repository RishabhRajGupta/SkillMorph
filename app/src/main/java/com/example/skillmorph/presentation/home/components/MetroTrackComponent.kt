package com.example.skillmorph.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skillmorph.data.local.entities.TaskEntity
import com.example.skillmorph.utils.glassEffect

/**
 * ARCHITECT NOTE: This is the horizontal "Metro Track" for the Home Screen.
 * It provides a quick glance at the day's "Stations" (Tasks).
 */
@Composable
fun MetroTrack(
    stations: List<TaskEntity>,
    onStationClick: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .glassEffect(), // Utilizing your existing glassEffect extension
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(stations) { station ->
            MetroStationItem(
                title = station.title,
                isCompleted = station.isCompleted,
                onClick = { onStationClick(station.id.toInt(), !station.isCompleted) }
            )
        }
    }
}

@Composable
private fun MetroStationItem(
    title: String,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    // Metro Theme Colors
    val activeColor = Color(0xFF00E5FF) // Cyan
    val completedColor = Color(0xFF00FF00) // Green
    val currentColor = if (isCompleted) completedColor else activeColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clickable { onClick() }
    ) {
        // The "Station" Node
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(currentColor.copy(alpha = 0.15f))
                .border(2.dp, currentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = completedColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Station Label
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}