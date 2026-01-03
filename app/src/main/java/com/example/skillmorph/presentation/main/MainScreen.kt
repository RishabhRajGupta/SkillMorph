
package com.example.skillmorph.presentation.main

import android.R.attr.end
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.skillmorph.Agent
import com.example.skillmorph.presentation.goals.GoalsScreen
import com.example.skillmorph.presentation.navigation.Screen
import com.example.skillmorph.ui.theme.NeonBlue
import com.example.skillmorph.ui.theme.NeonCyan
import com.example.skillmorph.ui.theme.TransparentWhite
import com.example.skillmorph.utils.glassEffect

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var isVoiceMode by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                streakCount = 10,
                isStreakActive = true,
                hasNotification = true,
                onMenuClick = { /* Handle menu click */ },
                onNotificationClick = { /* Handle notification click */ }
            )
        },
        bottomBar = {
            // Our custom glassy Bottom Navigation Bar
            BottomNavBar(navController = navController)
        },
        // Set container color to transparent to see the theme's gradient background
        containerColor = Color.Transparent
    ) { innerPadding ->
        // NavHost for the main screens (Home, Goals, etc.)
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Goals.route) { GoalsScreen() }
            composable(Screen.Tasks.route) { TasksScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    streakCount: Int,
    isStreakActive: Boolean, // True = Orange Fire, False = Gray/White
    hasNotification: Boolean,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    // Standard TopAppBar has elevation/shadow by default.
    // We use a simple Row to get full control over transparency.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding() // Respect the notch area
            .padding(horizontal = 16.dp, vertical = 12.dp), // Add breathing room
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 1. LEFT: Side Sheet Trigger (Hamburger)
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 2. APP NAME: "SkillMorph"
        Text(
            text = "SkillMorph",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f) // Pushes right-side items to the end
        )

        // 3. STREAK COUNTER
        StreakBadge(count = streakCount, isActive = isStreakActive)

        Spacer(modifier = Modifier.width(16.dp))

        // 4. NOTIFICATION BELL (With Badge)
        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFF00E5FF), // Cyan tint like your screenshot
                    modifier = Modifier.size(28.dp)
                )
            }
            // The Red Dot Badge
            if (hasNotification) {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp) // Adjust position slightly
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }
    }
}

@Composable
fun StreakBadge(count: Int, isActive: Boolean) {
    // Dynamic Colors based on active state
    val backgroundColor = if (isActive) {
        // Active: Low opacity Orange background
        Color(0xFFFF9800).copy(alpha = 0.2f)
    } else {
        // Inactive: Transparent White background
        Color.White.copy(alpha = 0.1f)
    }

    val iconColor = if (isActive) Color(0xFFFF9800) else Color.LightGray.copy(alpha = 0.7f)
    val textColor = if (isActive) Color(0xFFFF9800) else Color.LightGray.copy(alpha = 0.7f)

    // Border for active state
    val borderStroke = if (isActive) {
        BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
    } else null

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50)) // Pill shape
            .background(backgroundColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(50)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp) // Internal padding
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = "Streak",
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}


@Composable
fun InputModeChip(
    isVoiceMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- GLASSY COLORS ---
    val neonCyan = Color(0xFF00E5FF)

    // 1. The Glass Gradient (Top is slightly lighter to simulate reflection)
    val glassGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.15f), // Top: lighter
            Color.White.copy(alpha = 0.05f)  // Bottom: darker/transparent
        )
    )

    // 2. The Border Gradient (Shiny top rim, fading bottom)
    val borderGradient = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.3f),
            Color.Transparent
        )
    )

    // Dimensions
    val chipHeight = 54.dp
    val indicatorWidth = 100.dp
    val totalWidth = 200.dp

    // Animation State
    val indicatorOffset by animateDpAsState(
        targetValue = if (isVoiceMode) 0.dp else indicatorWidth,
        animationSpec = tween(300),
        label = "offset"
    )

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(chipHeight)
            .clip(CircleShape)
            // Apply the Glass Gradient Background
            .background(glassGradient)
            // Add the Shiny Border
            .border(1.dp, borderGradient, CircleShape)
            .clickable { onToggle() }
    ) {
        // --- ACTIVE INDICATOR (Cyan Pill) ---
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(indicatorWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(CircleShape)
                .background(neonCyan.copy(alpha = 0.8f)) // Slightly see-through cyan
            // Add a subtle glow/blur to the indicator if desired
        )

        // --- CONTENT LAYER ---
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipOption(
                text = "Voice",
                icon = Icons.Rounded.Mic,
                isSelected = isVoiceMode,
                modifier = Modifier.width(indicatorWidth)
            )

            ChipOption(
                text = "Type",
                icon = Icons.Rounded.Keyboard,
                isSelected = !isVoiceMode,
                modifier = Modifier.width(indicatorWidth)
            )
        }
    }
}

@Composable
private fun ChipOption(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier
) {
    // Text Color Animation: Black on Cyan, White on Glass
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
        animationSpec = tween(300),
        label = "color"
    )

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        Screen.Home,
        Screen.Goals,
        Screen.Tasks,
        Screen.Profile
    )

    NavigationBar(
        modifier = Modifier.glassEffect(),
        containerColor = TransparentWhite // Use a semi-transparent color for the glass effect
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                label = { Text(screen.title) },
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to avoid building up a large back stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when re-selecting the same item
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonBlue,
                    selectedTextColor = NeonBlue,
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.LightGray,
                    indicatorColor = Color.Transparent // Hide the selection indicator
                )
            )
        }
    }
}

// --- Dummy Screens (to be replaced later) ---

@Composable
fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // ... Background & Content ...

        Agent()
    }
}

@Composable
fun GoalsScreen() {
    GoalsScreen(onGoalClick = {
        goalId ->
    })
}

@Composable
fun TasksScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Tasks Screen", color = Color.White)
    }
}

@Composable
fun ProfileScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Profile Screen", color = Color.White)
    }
}
