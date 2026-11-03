package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun ScreenFlashOverlay(
    flashEvents: SharedFlow<Unit>,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(flashEvents) {
        flashEvents.collect {
            // Quick intense flash then fade out
            alphaAnim.snapTo(0.85f)
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400)
            )
        }
    }

    if (alphaAnim.value > 0.01f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .border(
                    width = 12.dp,
                    color = Color(0xFFFDE047).copy(alpha = alphaAnim.value)
                )
        )
    }
}
