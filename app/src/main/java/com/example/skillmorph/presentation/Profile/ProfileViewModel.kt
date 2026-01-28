package com.example.skillmorph.presentation.Profile

import androidx.lifecycle.ViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Bolt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

// --- Data Models ---
data class ProfileState(
    val user: UserInfo,
    val stats: UserStats,
    val skillRadar: Map<String, Float>,
    val heatmap: List<DailyActivity>, // Now holds 365 days
    val badges: List<Badge>
)

data class UserInfo(
    val name: String,
    val handle: String,
    val title: String,
    val currentLevel: Int,
    val currentXp: Int,
    val maxXp: Int
)

data class UserStats(
    val currentStreak: Int,
    val totalActiveDays: Int,
    val maxStreak: Int // UPDATED: Changed from completionRate
)

data class DailyActivity(
    val date: LocalDate,
    val intensity: Int // 0 (Gray) to 4 (Bright Green)
)

data class Badge(
    val name: String,
    val icon: ImageVector,
    val isUnlocked: Boolean
)

// --- ViewModel ---
class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(generateMockState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private fun generateMockState(): ProfileState {
        val today = LocalDate.now()
        // Generate last 365 days of data for the LeetCode graph
        // We create a list from [Today - 364] to [Today]
        val heatmapData = (0 until 365).map { offset ->
            val date = today.minusDays((364 - offset).toLong())
            // Randomize intensity to mimic real coding patterns
            val isWeekend = date.dayOfWeek.value >= 6
            val baseChance = if (isWeekend) 0.3f else 0.6f

            val intensity = if (Random.nextFloat() < baseChance) {
                Random.nextInt(1, 5) // 1 to 4
            } else {
                0
            }
            DailyActivity(date, intensity)
        }

        return ProfileState(
            user = UserInfo(
                name = "Alex Carter",
                handle = "@sys_alex",
                title = "System Architect",
                currentLevel = 12,
                currentXp = 2450,
                maxXp = 5000
            ),
            stats = UserStats(
                currentStreak = 14,
                totalActiveDays = 111,
                maxStreak = 65 // Updated Field
            ),
            skillRadar = mapOf(
                "Coding" to 0.90f,
                "Design" to 0.60f,
                "Logic" to 0.80f,
                "Focus" to 0.85f,
                "Math" to 0.50f,
                "System" to 0.75f
            ),
            heatmap = heatmapData,
            badges = listOf(
                Badge("Early Riser", Icons.Rounded.Bolt, true),
                Badge("Code Ninja", Icons.Default.Code, true),
                Badge("Streak Master", Icons.Default.LocalFireDepartment, true),
                Badge("Bug Hunter", Icons.Default.BugReport, false)
            )
        )
    }
}