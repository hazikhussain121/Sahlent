package com.sahlent.ui.qibla

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahlent.ui.theme.*
import kotlin.math.abs

@Composable
fun QiblaScreen(
    viewModel: QiblaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Determine if user is facing Qibla (within 5 degrees)
    val isFacingQibla by remember(uiState.needleRotation) {
        derivedStateOf {
            abs(uiState.needleRotation % 360) < 5 || abs(uiState.needleRotation % 360) > 355
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Qibla Compass",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Point the arrow towards the Kaaba",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (uiState.isLoading) {
            CircularProgressIndicator(color = Primary)
        } else if (!uiState.isCompassAvailable) {
            CompassUnavailable()
        } else if (uiState.error != null) {
            ErrorMessage(uiState.error!!)
        } else {
            // Premium Compass
            PremiumQiblaCompass(
                needleRotation = uiState.needleRotation,
                qiblaDirection = uiState.qiblaDirection,
                isFacingQibla = isFacingQibla
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Facing Qibla celebration
        AnimatedVisibility(
            visible = isFacingQibla && !uiState.isLoading && uiState.error == null,
            enter = fadeIn() + scaleIn()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Success.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = "✓ You're facing the Qibla!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Success,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Direction info
        if (!uiState.isLoading && uiState.error == null) {
            DirectionInfo(
                qiblaDirection = uiState.qiblaDirection,
                currentBearing = uiState.compassBearing
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calibration hint
        CalibrationHint()
    }
}

@Composable
fun PremiumQiblaCompass(
    needleRotation: Float,
    qiblaDirection: Float,
    isFacingQibla: Boolean
) {
    // Smoother spring animation for compass
    val animatedRotation by animateFloatAsState(
        targetValue = needleRotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )
    
    // Glow effect when facing Qibla
    val infiniteTransition = rememberInfiniteTransition(label = "qiblaGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isFacingQibla) 0.4f else 0.1f,
        targetValue = if (isFacingQibla) 0.8f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    val glowColor = if (isFacingQibla) Success else Primary
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(320.dp)
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(300.dp)
                .blur(30.dp)
                .clip(CircleShape)
                .background(glowColor.copy(alpha = glowAlpha * 0.5f))
        )
        
        // Outer ring
        Box(
            modifier = Modifier
                .size(290.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        )
        
        // Compass markings ring
        Canvas(
            modifier = Modifier
                .size(270.dp)
                .rotate(animatedRotation)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2
            
            drawPremiumCompassMarkings(center, radius)
            drawPremiumQiblaNeedle(center, radius)
        }
        
        // Inner circle with Kaaba
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Primary, PrimaryDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🕋",
                fontSize = 28.sp
            )
        }
        
        // Fixed "You" indicator at top
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Secondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▼",
                    fontSize = 16.sp,
                    color = PrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun DrawScope.drawPremiumCompassMarkings(center: Offset, radius: Float) {
    val directions = listOf("N", "E", "S", "W")
    
    // Draw degree marks with premium styling
    for (i in 0 until 360 step 5) {
        val angle = Math.toRadians(i.toDouble() - 90)
        val isCardinal = i % 90 == 0
        val isMajor = i % 30 == 0
        
        val innerRadius = when {
            isCardinal -> radius * 0.72f
            isMajor -> radius * 0.82f
            else -> radius * 0.88f
        }
        
        val startX = center.x + (innerRadius * kotlin.math.cos(angle)).toFloat()
        val startY = center.y + (innerRadius * kotlin.math.sin(angle)).toFloat()
        val endX = center.x + (radius * 0.94f * kotlin.math.cos(angle)).toFloat()
        val endY = center.y + (radius * 0.94f * kotlin.math.sin(angle)).toFloat()
        
        val lineColor = when {
            isCardinal -> Primary
            isMajor -> Primary.copy(alpha = 0.6f)
            else -> Color.Gray.copy(alpha = 0.3f)
        }
        
        drawLine(
            color = lineColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isCardinal) 4f else if (isMajor) 2.5f else 1.5f,
            cap = StrokeCap.Round
        )
    }
    
    // Draw outer decorative ring
    drawCircle(
        color = Primary.copy(alpha = 0.3f),
        radius = radius * 0.96f,
        center = center,
        style = Stroke(width = 2f)
    )
}

fun DrawScope.drawPremiumQiblaNeedle(center: Offset, radius: Float) {
    val needleLength = radius * 0.55f
    val needleWidth = 28f
    
    // Main needle body (pointing up to Qibla)
    val needlePath = Path().apply {
        moveTo(center.x, center.y - needleLength)
        lineTo(center.x - needleWidth / 2, center.y)
        lineTo(center.x - needleWidth / 4, center.y + needleLength * 0.25f)
        lineTo(center.x + needleWidth / 4, center.y + needleLength * 0.25f)
        lineTo(center.x + needleWidth / 2, center.y)
        close()
    }
    
    drawPath(
        path = needlePath,
        brush = Brush.verticalGradient(
            colors = listOf(Primary, PrimaryDark)
        )
    )
    
    // Arrow head
    val arrowPath = Path().apply {
        moveTo(center.x, center.y - needleLength - 18f)
        lineTo(center.x - 14f, center.y - needleLength + 8f)
        lineTo(center.x + 14f, center.y - needleLength + 8f)
        close()
    }
    
    drawPath(
        path = arrowPath,
        brush = Brush.verticalGradient(
            colors = listOf(Secondary, SecondaryDark)
        )
    )
    
    // Center dot
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Secondary, SecondaryDark)
        ),
        radius = 10f,
        center = center
    )
}

@Composable
fun DirectionInfo(
    qiblaDirection: Float,
    currentBearing: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Qibla Direction",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${qiblaDirection.toInt()}°",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
            
            Text(
                text = getDirectionName(qiblaDirection),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CalibrationHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Secondary.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = "💡 Move your phone in a figure-8 pattern to calibrate",
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryDark,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        )
    }
}

@Composable
fun CompassUnavailable() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Error.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Error,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Compass Not Available",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Error
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Your device doesn't have a compass sensor",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Error.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(20.dp)
        )
    }
}

private fun getDirectionName(degrees: Float): String {
    return when {
        degrees < 22.5 || degrees >= 337.5 -> "North"
        degrees < 67.5 -> "Northeast"
        degrees < 112.5 -> "East"
        degrees < 157.5 -> "Southeast"
        degrees < 202.5 -> "South"
        degrees < 247.5 -> "Southwest"
        degrees < 292.5 -> "West"
        else -> "Northwest"
    }
}
