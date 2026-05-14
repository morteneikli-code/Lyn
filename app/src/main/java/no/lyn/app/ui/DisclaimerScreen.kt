package no.lyn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.lyn.app.R
import no.lyn.app.ui.theme.*

/**
 * One-time disclaimer shown on first launch.
 *
 * Renders five short paragraphs (what the app is, what it isn't, where to get
 * official warnings, emergency number, personal responsibility) and a single
 * accept button. The caller persists acceptance and removes the screen.
 *
 * Paragraphs come from individual string resources so translators can adjust
 * tone per language without escape-sequence drama.
 */
@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = LightningGold,
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )

                DisclaimerCard {
                    DisclaimerParagraph(R.string.disclaimer_para_what)
                    DisclaimerParagraph(R.string.disclaimer_para_not, emphasis = true)
                    DisclaimerParagraph(R.string.disclaimer_para_official)
                    DisclaimerParagraph(R.string.disclaimer_para_emergency)
                    DisclaimerParagraph(R.string.disclaimer_para_responsibility)
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightningYellow,
                    contentColor = StormBlack,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    stringResource(R.string.disclaimer_accept),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DisclaimerCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StormCard),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun DisclaimerParagraph(textRes: Int, emphasis: Boolean = false) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = if (emphasis) LightningGold else TextPrimary,
        fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
        lineHeight = 22.sp,
    )
}
