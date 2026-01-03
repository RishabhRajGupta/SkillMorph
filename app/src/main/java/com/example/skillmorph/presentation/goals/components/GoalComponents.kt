
package com.example.skillmorph.presentation.goals.components

import android.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skillmorph.data.local.entities.GoalEntity
import com.example.skillmorph.ui.theme.NeonBlue
import com.example.skillmorph.ui.theme.NeonCyan
import com.example.skillmorph.utils.glassEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalCard(
    goal: GoalEntity,
    onGoalClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect()
            .clickable { onGoalClick(goal.id) }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircularProgressWithText(percentage = goal.progressPercentage)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = goal.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    DateColumn(label = "Start date", date = goal.startDate)
                    Spacer(modifier = Modifier.width(24.dp))
                    DateColumn(label = "End date", date = goal.endDate)
                }
            }
        }
    }
}

@Composable
fun CircularProgressWithText(
    percentage: Int,
    radius: Dp = 40.dp,
    strokeWidth: Dp = 8.dp
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val currentPercentage = animateFloatAsState(
        targetValue = if (animationPlayed) percentage / 100f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 300),
        label = "progressAnimation"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Box(
        modifier = Modifier.size(radius * 2f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = NeonCyan,
                startAngle = -90f,
                sweepAngle = 360 * currentPercentage.value,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(text = "${(currentPercentage.value * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
@Composable
fun DateColumn(label: String, date: Long) {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    val formattedDate = sdf.format(Date(date))
    Column {
        Text(text = label, color = Color.LightGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        NeonGlowText(text = formattedDate, color = NeonBlue)
    }
}
@Composable
fun NeonGlowText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.drawBehind {
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL
                    this.color = color.copy(alpha = 0.5f).toArgb()
                    maskFilter = android.graphics.BlurMaskFilter(15f, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
                drawText(text, 0f, size.height, paint)
            }
        }
    )
}