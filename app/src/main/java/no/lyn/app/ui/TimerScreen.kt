package no.lyn.app.ui

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
import kotlinx.coroutines.launch
import no.lyn.app.*
import no.lyn.app.data.AppDatabase
import no.lyn.app.data.Measurement
import no.lyn.app.ui.theme.*
import java.util.Locale

@Composable
fun TimerScreen(
    database: AppDatabase,
    // State hoisted to LynApp so it survives tab switching
    flashTime: Long?,
    elapsedSeconds: Double?,
    onTap: () -> Unit,
    onReset: () -> Unit,
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(StormBlack, Color(0xFF0D1628), StormDeep),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
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
            AppHeader()

            TimerSection(
                flashTime = flashTime,
                elapsedSeconds = elapsedSeconds,
                onTap = onTap,
                onReset = onReset,
            )

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
            Icon(Icons.Filled.ElectricBolt, null, tint = LightningYellow, modifier = Modifier.size(32.dp))
            Text(
                text = "Lyn",
                style = MaterialTheme.typography.headlineLarge,
                color = LightningYellow,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text("Lightning Distance Tracker", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isWaiting) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isWaiting) 1f else 0.4f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    val buttonColor = when {
        isWaiting         -> LightningGold
        elapsedSeconds != null -> ElectricBlue
        else              -> LightningYellow
    }

    val instructionText = when {
        isWaiting              -> "Tap when you hear the THUNDER"
        elapsedSeconds != null -> "Tap again to measure a new strike"
        else                   -> "Tap when you see the LIGHTNING"
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

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(buttonColor.copy(alpha = glowAlpha * 0.15f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .shadow(20.dp, CircleShape, ambientColor = buttonColor, spotColor = buttonColor)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(buttonColor, buttonColor.copy(alpha = 0.7f))))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTap()
                    },
            ) {
                Icon(Icons.Filled.ElectricBolt, "Tap", tint = StormBlack, modifier = Modifier.size(64.dp))
            }
        }

        AnimatedVisibility(visible = elapsedSeconds != null || isWaiting) {
            OutlinedButton(
                onClick = onReset,
                border = BorderStroke(1.dp, StormBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            ) {
                Icon(Icons.Filled.Refresh, "Reset", modifier = Modifier.size(16.dp))
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
            ResultMetric("%.1f".format(seconds), "seconds", "Time", ElectricBlue)
            VerticalDivider(modifier = Modifier.height(60.dp), color = StormBorder)
            ResultMetric("%.1f".format(distanceKm), "km", "Distance", LightningYellow)
            VerticalDivider(modifier = Modifier.height(60.dp), color = StormBorder)
            ResultMetric("%.1f".format(distanceKm * 0.621371), "miles", "Distance", ElectricPurple)
        }
    }
}

@Composable
fun ResultMetric(value: String, unit: String, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
        Text(unit, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
fun SafetyCard(safetyInfo: no.lyn.app.SafetyInfo) {
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
                ColorDot(color = safetyInfo.color, size = 12.dp)
                Text(
                    "${safetyInfo.emoji} ${safetyInfo.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = safetyInfo.color,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(safetyInfo.advice, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, lineHeight = 22.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FactsSection() {
    val pagerState = rememberPagerState(pageCount = { LIGHTNING_FACTS.size })

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Lightning Facts", style = MaterialTheme.typography.titleMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(end = 40.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page -> FactCard(fact = LIGHTNING_FACTS[page]) }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(LIGHTNING_FACTS.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) LightningYellow else StormBorder),
                )
            }
        }
    }
}

@Composable
fun FactCard(fact: no.lyn.app.LightningFact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard),
        border = BorderStroke(1.dp, StormBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(fact.icon, fontSize = 28.sp)
                Text(fact.title, style = MaterialTheme.typography.titleMedium, color = LightningYellow, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(fact.body, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, lineHeight = 21.sp)
        }
    }
}
