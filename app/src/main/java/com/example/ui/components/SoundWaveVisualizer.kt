package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppTheme

@Composable
fun SoundWaveVisualizer(
    isListening: Boolean,
    soundLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 12
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_transition")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    val waveColor = AppTheme.colors.waveColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("sound_wave_visualizer"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isListening) {
            Text(
                text = "الميكروفون متوقف - اضغط للبدء",
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            for (i in 0 until barCount) {
                // Vary bar multiplier for organic wave look
                val centerOffset = kotlin.math.abs(i - (barCount / 2f)) / (barCount / 2f)
                val baseFactor = (1f - (centerOffset * 0.5f)).coerceIn(0.2f, 1f)
                val animatedHeight = if (soundLevel > 0.05f) {
                    val dynamicVal = 8f + (36f * soundLevel * baseFactor * pulse)
                    dynamicVal.coerceIn(8f, 48f).dp
                } else {
                    val idleVal = 6f + (8f * pulse * baseFactor)
                    idleVal.dp
                }

                Box(
                    modifier = Modifier
                        .paddingBar()
                        .width(6.dp)
                        .height(animatedHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(waveColor)
                )
            }
        }
    }
}

private fun Modifier.paddingBar(): Modifier = this.then(
    Modifier.background(Color.Transparent)
        .width(8.dp)
)
