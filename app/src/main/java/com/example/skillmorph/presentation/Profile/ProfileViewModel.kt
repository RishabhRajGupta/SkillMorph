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
import kotlin.random.Random

// --- Data Models ---
data class ProfileState(
    val user: UserInfo,
    val stats: UserStats,
    val skillRadar: Map<String, Float>, // Label to Value (0.0 - 1.0)
    val heatmap: List<DailyActivity>,
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
    val completionRate: Int
)

data class DailyActivity(
    val date: LocalDate,
    val intensity: Int // 0-4
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
        // Mock Heatmap: Past 100 days
        val today = LocalDate.now()
        val heatmapData = (0..105).map { offset ->
            DailyActivity(
                date = today.minusDays((105 - offset).toLong()),
                // Random intensity to look like real data
                intensity = if (Random.nextFloat() > 0.4) Random.nextInt(1, 5) else 0
            )
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
                totalActiveDays = 89,
                completionRate = 92
            ),
            // Hardcoded categories as requested, but mapped to values
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
