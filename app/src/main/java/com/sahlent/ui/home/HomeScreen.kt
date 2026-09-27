package com.sahlent.ui.home

// main home screen - shows prayer times, tasbih counter etc
// TODO: maybe split this file later, its getting kinda big

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahlent.data.model.Prayer
import com.sahlent.data.model.PrayerName
import com.sahlent.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.onLocationPermissionGranted()
        }
    }
    
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    // Log.d("HomeScreen", "uiState updated: ${uiState.nextPrayer?.name}")
    
    // Target reached dialog with celebration
    // maybe add confetti animation here someday?
    if (uiState.showTargetReached) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissTargetReached() },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { 
                Text(
                    "سبحان الله", 
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Secondary
                ) 
            },
            text = { 
                Text(
                    "Target of ${uiState.tasbihTarget} reached!\nMay Allah accept your dhikr.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissTargetReached() }) {
                    Text("Alhamdulillah", color = Primary)
                }
            }
        )
    }
    
    // Main scrollable content
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderSection(
                locationName = uiState.locationName,
                onRefresh = { viewModel.loadPrayerTimes() }
            )
        }
        
        // Next Prayer Card
        item {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (uiState.error != null) {
                ErrorCard(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadPrayerTimes() }
                )
            } else {
                uiState.nextPrayer?.let { nextPrayer ->
                    PremiumPrayerCard(
                        prayer = nextPrayer,
                        countdown = uiState.countdownText
                    )
                }
            }
        }
        
        // Tasbih Counter
        item {
            PremiumTasbihCounter(
                count = uiState.tasbihCount,
                target = uiState.tasbihTarget,
                totalCount = uiState.tasbihTotalCount,
                onTap = { viewModel.incrementTasbih() },
                onReset = { viewModel.resetTasbih() },
                onTargetChange = { viewModel.setTasbihTarget(it) }
            )
        }
        
        // Auto-Silent Toggle
        item {
            PremiumAutoSilentCard(
                enabled = uiState.autoSilentEnabled,
                hasDndPermission = uiState.hasDndPermission,
                onToggle = { viewModel.toggleAutoSilent() },
                onRequestPermission = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }
        
        // Prayer Times Section Title
        item {
            Text(
                text = "Today's Prayer Times",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Prayer Times List with staggered animation
        // this animation delay thing took forever to get right
        uiState.prayers?.asList()?.let { prayers ->
            itemsIndexed(
                items = prayers,
                key = { _, prayer -> prayer.name.name }
            ) { index, prayer ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 50L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(300)) + slideInVertically(
                        initialOffsetY = { 20 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    )
                ) {
                    PrayerTimeItem(prayer = prayer)
                }
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HeaderSection(
    locationName: String,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PrimaryLight, Primary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Current Location",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = Primary
            )
        }
    }
}

@Composable
fun PremiumPrayerCard(
    prayer: Prayer,
    countdown: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    val prayerColor = prayer.name.toColor()
    val gradientColors = listOf(
        prayerColor,
        prayerColor.copy(alpha = 0.8f),
        PrimaryDark
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = prayerColor.copy(alpha = glowAlpha),
                spotColor = prayerColor.copy(alpha = glowAlpha)
            )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = gradientColors,
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Next Prayer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Text(
                        text = prayer.name.toDisplayName(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = countdown,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prayer.time,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumTasbihCounter(
    count: Int,
    target: Int,
    totalCount: Long,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onTargetChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    
    /* 
     * Pulsing glow effect - makes the tasbih button feel more "alive"
     * tried different durations, 1500ms feels best
     */
    val infiniteTransition = rememberInfiniteTransition(label = "tasbihGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Reset Counter") },
            text = { Text("Reset tasbih counter to 0?") },
            confirmButton = {
                TextButton(onClick = { 
                    onReset()
                    showResetDialog = false
                }) {
                    Text("Reset", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showTargetDialog) {
        val targets = listOf(33, 99, 100, 500, 1000)
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Set Target") },
            text = {
                Column {
                    targets.forEach { t ->
                        TextButton(
                            onClick = { 
                                onTargetChange(t)
                                showTargetDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "$t",
                                color = if (t == target) Primary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (t == target) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tasbih Counter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total: $totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Main counter circle with glow
            Box(contentAlignment = Alignment.Center) {
                // Glow layer
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(pulseScale)
                        .blur(20.dp)
                        .clip(CircleShape)
                        .background(TasbihGlow)
                )
                
                // Main circle
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scale)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(TasbihCircle, TasbihCirclePressed)
                            )
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // vibration stuff is so fragmented across android versions smh
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                vibratorManager.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            }
                            // 25ms feels snappy, 50ms was too long
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(25)
                            }
                            onTap()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 56.sp
                        )
                        Text(
                            text = "of $target",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress bar
            val progress = (count.toFloat() / target.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TasbihCircle, Primary)
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Reset", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Button(
                    onClick = { showTargetDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Target: $target")
                }
            }
        }
    }
}

@Composable
fun PremiumAutoSilentCard(
    enabled: Boolean,
    hasDndPermission: Boolean,
    onToggle: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (enabled) Primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(300),
        label = "bgColor"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (enabled) Primary else MaterialTheme.colorScheme.surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Auto Silent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (!hasDndPermission) "Permission required" 
                              else if (enabled) "Active during prayers" 
                              else "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!hasDndPermission) {
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant")
                }
            } else {
                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun PrayerTimeItem(prayer: Prayer) {
    val color = prayer.name.toColor()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = prayer.name.toDisplayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = prayer.time,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Error.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry")
            }
        }
    }
}

// Extension functions
fun PrayerName.toDisplayName(): String = when (this) {
    PrayerName.FAJR -> "Fajr"
    PrayerName.SUNRISE -> "Sunrise"
    PrayerName.DHUHR -> "Dhuhr"
    PrayerName.ASR -> "Asr"
    PrayerName.MAGHRIB -> "Maghrib"
    PrayerName.ISHA -> "Isha"
}

fun PrayerName.toColor(): Color = when (this) {
    PrayerName.FAJR -> PrayerFajr
    PrayerName.SUNRISE -> PrayerSunrise
    PrayerName.DHUHR -> PrayerDhuhr
    PrayerName.ASR -> PrayerAsr
    PrayerName.MAGHRIB -> PrayerMaghrib
    PrayerName.ISHA -> PrayerIsha
}
