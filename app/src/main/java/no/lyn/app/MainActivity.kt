package no.lyn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.lyn.app.ui.theme.*
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LynTheme {
                LynApp()
            }
        }
    }
}

@Composable
fun LynApp() {
    var flashTime by remember { mutableStateOf<Long?>(null) }
    var elapsedSeconds by remember { mutableStateOf<Double?>(null) }
    val haptic = LocalHapticFeedback.current

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(StormBlack, Color(0xFF0D1628), StormDeep),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header
            AppHeader()

            // Timer / Tap section
            TimerSection(
                flashTime = flashTime,
                elapsedSeconds = elapsedSeconds,
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val now = System.currentTimeMillis()
                    if (flashTime == null) {
                        flashTime = now
                        elapsedSeconds = null
                    } else {
                        elapsedSeconds = (now - flashTime!!) / 1000.0
                        flashTime = null
                    }
                },
                onReset = {
                    flashTime = null
                    elapsedSeconds = null
                },
            )

            // Result section
            AnimatedVisibility(
                visible = elapsedSeconds != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
            ) {
                elapsedSeconds?.let { secs ->
                    val distanceKm = secondsToKm(secs)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ResultCard(seconds = secs, distanceKm = distanceKm)
                        SafetyCard(safetyInfo = getSafetyInfo(distanceKm))
                    }
                }
            }

            // Facts section
            FactsSection()

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AppHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ElectricBolt,
                contentDescription = null,
                tint = LightningYellow,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Lyn",
                style = MaterialTheme.typography.headlineLarge,
                color = LightningYellow,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text = "Lightning Distance Tracker",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
fun TimerSection(
    flashTime: Long?,
    elapsedSeconds: Double?,
    onTap: () -> Unit,
    onReset: () -> Unit,
) {
    val isWaiting = flashTime != null

    // Pulse animation while waiting
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isWaiting) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isWaiting) 1f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    val buttonColor = when {
        isWaiting -> LightningGold
        elapsedSeconds != null -> ElectricBlue
        else -> LightningYellow
    }

    val instructionText = when {
        isWaiting -> "Tap when you hear the THUNDER"
        elapsedSeconds != null -> "Tap again to measure a new strike"
        else -> "Tap when you see the LIGHTNING"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = instructionText,
            style = MaterialTheme.typography.titleMedium,
            color = if (isWaiting) LightningGold else TextPrimary,
            textAlign = TextAlign.Center,
        )

        // Big tap button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            // Glow ring
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = glowAlpha * 0.15f))
            )
            // Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        ambientColor = buttonColor,
                        spotColor = buttonColor,
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(buttonColor, buttonColor.copy(alpha = 0.7f)),
                        )
                    )
                    .clickable { onTap() }
            ) {
                Icon(
                    imageVector = Icons.Filled.ElectricBolt,
                    contentDescription = "Tap",
                    tint = StormBlack,
                    modifier = Modifier.size(64.dp),
                )
            }
        }

        // Reset button
        AnimatedVisibility(visible = elapsedSeconds != null || isWaiting) {
            OutlinedButton(
                onClick = onReset,
                border = BorderStroke(1.dp, StormBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reset",
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Reset")
            }
        }
    }
}

@Composable
fun ResultCard(seconds: Double, distanceKm: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard),
        border = BorderStroke(1.dp, StormBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ResultMetric(
                value = String.format(Locale.US, "%.1f", seconds),
                unit = "seconds",
                label = "Time",
                color = ElectricBlue,
            )
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = StormBorder,
            )
            ResultMetric(
                value = String.format(Locale.US, "%.1f", distanceKm),
                unit = "km",
                label = "Distance",
                color = LightningYellow,
            )
            Divider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = StormBorder,
            )
            ResultMetric(
                value = String.format(Locale.US, "%.1f", distanceKm * 0.621371),
                unit = "miles",
                label = "Distance",
                color = ElectricPurple,
            )
        }
    }
}

@Composable
fun ResultMetric(value: String, unit: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
fun SafetyCard(safetyInfo: SafetyInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard),
        border = BorderStroke(1.5.dp, safetyInfo.color.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(safetyInfo.color)
                )
                Text(
                    text = "${safetyInfo.emoji} ${safetyInfo.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = safetyInfo.color,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = safetyInfo.advice,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 22.sp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FactsSection() {
    val pagerState = rememberPagerState(pageCount = { LIGHTNING_FACTS.size })

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Lightning Facts",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(end = 40.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            FactCard(fact = LIGHTNING_FACTS[page])
        }

        // Page indicator dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(LIGHTNING_FACTS.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) LightningYellow
                            else StormBorder
                        )
                )
            }
        }
    }
}

@Composable
fun FactCard(fact: LightningFact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard),
        border = BorderStroke(1.dp, StormBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = fact.icon, fontSize = 28.sp)
                Text(
                    text = fact.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = LightningYellow,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = fact.body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 21.sp,
            )
        }
    }
}
