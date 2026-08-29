package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
        delay(2500)
        onTimeout()
    }

    val transition = updateTransition(targetState = isVisible, label = "splashTransition")
    
    val icebergScale by transition.animateFloat(
        transitionSpec = { tween(2000, easing = FastOutSlowInEasing) },
        label = "icebergScale"
    ) { visible ->
        if (visible) 1f else 0.5f 
    }
    
    val glowOpacity by transition.animateFloat(
        transitionSpec = { tween(1500, easing = LinearEasing) },
        label = "glowOpacity"
    ) { visible ->
        if (visible) 0.8f else 0f
    }
    
    val yOffset by transition.animateFloat(
        transitionSpec = { tween(2000, easing = FastOutSlowInEasing) },
        label = "yOffset"
    ) { visible ->
        if (visible) 0f else 200f 
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF020617), Color(0xFF0F172A)))), 
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2 + yOffset
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = glowOpacity),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = 400f * icebergScale
                ),
                radius = 400f * icebergScale,
                center = Offset(centerX, centerY)
            )
            
            val icebergPath = Path().apply {
                moveTo(centerX, centerY - 180f * icebergScale)
                lineTo(centerX + 60f * icebergScale, centerY - 80f * icebergScale)
                lineTo(centerX + 120f * icebergScale, centerY - 20f * icebergScale)
                lineTo(centerX + 160f * icebergScale, centerY + 80f * icebergScale)
                lineTo(centerX - 140f * icebergScale, centerY + 80f * icebergScale)
                lineTo(centerX - 90f * icebergScale, centerY - 40f * icebergScale)
                lineTo(centerX - 40f * icebergScale, centerY - 90f * icebergScale)
                close()
            }
            
            val leftFacet = Path().apply {
                moveTo(centerX, centerY - 180f * icebergScale) 
                lineTo(centerX - 40f * icebergScale, centerY - 90f * icebergScale)
                lineTo(centerX - 90f * icebergScale, centerY - 40f * icebergScale)
                lineTo(centerX - 140f * icebergScale, centerY + 80f * icebergScale)
                lineTo(centerX, centerY + 80f * icebergScale)
                close()
            }
            
            val rightFacet = Path().apply {
                moveTo(centerX, centerY - 180f * icebergScale) 
                lineTo(centerX + 60f * icebergScale, centerY - 80f * icebergScale)
                lineTo(centerX + 120f * icebergScale, centerY - 20f * icebergScale)
                lineTo(centerX + 160f * icebergScale, centerY + 80f * icebergScale)
                lineTo(centerX, centerY + 80f * icebergScale)
                close()
            }
            
            val ridgePath = Path().apply {
                moveTo(centerX, centerY - 180f * icebergScale)
                lineTo(centerX - 20f * icebergScale, centerY - 50f * icebergScale)
                lineTo(centerX + 10f * icebergScale, centerY + 30f * icebergScale)
                lineTo(centerX, centerY + 80f * icebergScale)
            }
            
            drawPath(
                path = leftFacet,
                color = Color(0xFFB3E5FC),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            drawPath(
                path = rightFacet,
                color = Color(0xFF81D4FA),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            
            drawPath(
                path = icebergPath,
                color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
            
            drawPath(
                path = ridgePath,
                color = Color(0xFF00B8D4).copy(alpha = 0.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
            
            drawRect(
                color = Color(0xFF001529).copy(alpha = 0.95f),
                topLeft = Offset(0f, centerY + 80f * icebergScale),
                size = Size(width, height - (centerY + 80f * icebergScale))
            )
            
            val reflectionPath = Path().apply {
                moveTo(centerX - 140f * icebergScale, centerY + 80f * icebergScale)
                lineTo(centerX + 160f * icebergScale, centerY + 80f * icebergScale)
                lineTo(centerX + 100f * icebergScale, centerY + 180f * icebergScale)
                lineTo(centerX, centerY + 240f * icebergScale)
                lineTo(centerX - 80f * icebergScale, centerY + 180f * icebergScale)
                close()
            }
            drawPath(
                path = reflectionPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startY = centerY + 80f * icebergScale,
                    endY = centerY + 250f * icebergScale
                ),
                style = androidx.compose.ui.graphics.drawscope.Fill
            )
            
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                start = Offset(0f, centerY + 80f * icebergScale),
                end = Offset(width, centerY + 80f * icebergScale),
                strokeWidth = 2f
            )
        }
        
        Text(
            text = "NEON MESSENGER",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 8.sp),
            color = Color(0xFF00E5FF).copy(alpha = glowOpacity),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        )
    }
}
