package com.example.skillmorph.presentation.Profile

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.skillmorph.utils.glassEffect
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.math.cos
import kotlin.math.sin


// --- Brand Colors ---
private val DarkBackground = Color(0xFF0F1014)
private val CardSurface = Color(0xFF16171D) // Slightly lighter than background
private val NeonCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFF9D00FF)
private val BrandGradient = Brush.linearGradient(listOf(NeonCyan, NeonPurple))

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel() // Hilt Injection usually goes here
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Main Container matching App Background
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .glassEffect(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header Section
        ProfileHeader(state.user)

        // 2. Stats Row (Streak, Active, Complete)
        StatsRow(state.stats)

        // 3. Consistency Graph (Heatmap)
        SectionCard(title = "Consistency Graph") {
            HeatmapGraph(state.heatmap)
        }

        // 4. Skills & Badges Split Row
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radar Chart (60% width)
            Box(modifier = Modifier.weight(0.6f)) {
                SectionCard(title = "Skill Matrix", modifier = Modifier.fillMaxHeight()) {
                    SkillRadarChart(state.skillRadar)
                }
            }

            // Badges (40% width)
            Box(modifier = Modifier.weight(0.4f)) {
                SectionCard(title = "Badges", modifier = Modifier.fillMaxHeight()) {
                    BadgesList(state.badges)
                }
            }
        }

        Spacer(modifier = Modifier.height(50.dp)) // Bottom padding for nav bar
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
fun ProfileHeader(user: UserInfo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(5.dp)
    ) {
        // Avatar with Gradient Ring
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(86.dp)) {
                drawCircle(
                    brush = BrandGradient,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Text Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = user.title,
                fontSize = 14.sp,
                color = NeonCyan // Accent color for title
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Level & XP Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lvl ${user.currentLevel}",
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple,
                    fontSize = 14.sp
                )
                Text(
                    text = "${user.currentXp} / ${user.maxXp} XP",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Gradient Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2A2B35))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(user.currentXp / user.maxXp.toFloat())
                        .fillMaxHeight()
                        .background(BrandGradient)
                )
            }
        }
    }
}

@Composable
fun StatsRow(stats: UserStats) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Streak",
            value = "${stats.currentStreak} \uD83D\uDD25", // Fire emoji
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Active",
            value = "${stats.totalActiveDays} Days",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Complete",
            value = "${stats.completionRate}%",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(CardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .background(CardSurface, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun HeatmapGraph(data: List<DailyActivity>) {
    // 7 Rows (Days of week), calculate columns dynamically
    val rows = 7
    val columns = data.chunked(rows)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(100.dp) // Fixed height to fit ~7 squares
    ) {
        items(columns) { columnData ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                columnData.forEach { day ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getHeatmapColor(day.intensity))
                    )
                }
            }
        }
    }
}

fun getHeatmapColor(intensity: Int): Color {
    return when (intensity) {
        0 -> Color(0xFF2A2B35) // Empty / Gray
        1 -> Color(0xFF0D47A1) // Dark Blue
        2 -> Color(0xFF1976D2) // Medium Blue
        3 -> Color(0xFF42A5F5) // Light Blue
        4 -> Color(0xFF00E5FF) // Neon Cyan (Max intensity)
        else -> Color(0xFF2A2B35)
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun SkillRadarChart(skills: Map<String, Float>) {
    val labels = skills.keys.toList()
    val values = skills.values.toList()

    // 1. BoxWithConstraints lets us measure the available Dp space
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // FAILSAFE FIX: Use simple if/else instead of min() to avoid import errors
        val minDimension = if (maxWidth < maxHeight) maxWidth else maxHeight
        val chartRadius = minDimension / 2 * 0.65f

        // Canvas needs pixels, not Dp. We convert here.
        val density = LocalDensity.current

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radiusPx = with(density) { chartRadius.toPx() }
            val center = Offset(size.width / 2, size.height / 2)
            val angleStep = (2 * Math.PI / labels.size).toFloat()

            // --- Draw Web Background (Grid) ---
            for (i in 1..4) {
                val r = radiusPx * (i / 4f)
                val path = Path()
                for (j in labels.indices) {
                    val angle = j * angleStep - (Math.PI / 2).toFloat()
                    val x = center.x + r * cos(angle)
                    val y = center.y + r * sin(angle)
                    if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path = path,
                    color = Color.Gray.copy(alpha = 0.2f),
                    style = Stroke(1.dp.toPx())
                )
            }

            // --- Draw Data Polygon (The Blue Shape) ---
            val dataPath = Path()
            for (j in labels.indices) {
                val r = radiusPx * values[j]
                val angle = j * angleStep - (Math.PI / 2).toFloat()
                val x = center.x + r * cos(angle)
                val y = center.y + r * sin(angle)
                if (j == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            // Fill with low opacity
            drawPath(dataPath, NeonCyan.copy(alpha = 0.3f))
            // Border with full opacity
            drawPath(dataPath, NeonCyan, style = Stroke(2.dp.toPx()))
        }

        // 3. Draw Text Labels
        // We calculate positions in Dp so we can use standard Text composables
        labels.forEachIndexed { index, label ->
            val angleStep = (2 * Math.PI / labels.size).toFloat()
            val angle = index * angleStep - (Math.PI / 2).toFloat()

            // Push text further out (1.35x radius) so it doesn't overlap the web
            val labelRadius = chartRadius * 1.35f

            // Calculate offset using simple Trig
            val xOffset = labelRadius * cos(angle)
            val yOffset = labelRadius * sin(angle)

            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(x = xOffset, y = yOffset)
            )
        }
    }
}

@Composable
fun BadgesList(badges: List<Badge>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        badges.forEach { badge ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = badge.icon,
                    contentDescription = null,
                    tint = if (badge.isUnlocked) NeonCyan else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = badge.name,
                    color = if (badge.isUnlocked) Color.White else Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewProfile() {
    ProfileScreen()
}