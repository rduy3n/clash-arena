package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterVisualStyle
import com.example.game.data.GameMode
import com.example.game.engine.ArenaWorldState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.UltiGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cinématique d'introduction rapide (2.5s) avant le Round 1 :
 * - 0.00s -> 1.15s : Entrée personnalisée J1 (VFX + capacités + stats + réplique)
 * - 1.15s -> 2.30s : Entrée personnalisée J2 (VFX + capacités + stats + réplique)
 * - 2.30s -> 2.50s : Clash face-à-face VS explosif
 */
@Composable
fun IntroCinematicOverlay(
    world: ArenaWorldState,
    timeMs: Long,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = world.introDurationMs.coerceAtLeast(2500L)
    val elapsedMs = (totalMs - world.phaseTimerMs).coerceIn(0L, totalMs)
    val p1Focus = elapsedMs < 1150L
    val p2Focus = elapsedMs in 1150L until 2300L
    val vsClash = elapsedMs >= 2300L

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD8050710))
            .testTag("intro_cinematic_overlay")
    ) {
        // Theatrical Cinema Bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .align(Alignment.TopCenter)
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .align(Alignment.BottomCenter)
                .background(Color.Black)
        )

        when {
            p1Focus -> {
                val localProgress = (elapsedMs / 1150f).coerceIn(0f, 1f)
                FighterEntranceSpotlight(
                    fighter = world.p1.def,
                    side = 1,
                    sideLabel = "COMBATTANT 1 • J1",
                    progress = localProgress,
                    timeMs = timeMs
                )
            }
            p2Focus -> {
                val localProgress = ((elapsedMs - 1150L) / 1150f).coerceIn(0f, 1f)
                FighterEntranceSpotlight(
                    fighter = world.p2.def,
                    side = 2,
                    sideLabel = "ADVERSAIRE • J2",
                    progress = localProgress,
                    timeMs = timeMs
                )
            }
            else -> {
                val clashProgress = ((elapsedMs - 2300L) / 200f).coerceIn(0f, 1f)
                VsClashFlashPanel(
                    p1 = world.p1.def,
                    p2 = world.p2.def,
                    progress = clashProgress
                )
            }
        }

        // Skip Button (disabled in Online to keep round start synchronized)
        if (world.mode != GameMode.ONLINE && !vsClash) {
            OutlinedButton(
                onClick = onSkip,
                border = BorderStroke(1.2.dp, UltiGold.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 34.dp, end = 16.dp)
                    .height(48.dp)
                    .testTag("skip_intro_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Passer l'intro",
                    tint = UltiGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PASSER",
                    style = MaterialTheme.typography.labelLarge,
                    color = UltiGold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FighterEntranceSpotlight(
    fighter: FighterDefinition,
    side: Int,
    sideLabel: String,
    progress: Float,
    timeMs: Long
) {
    val slideIn = (progress / 0.28f).coerceIn(0f, 1f)
    val slideOffsetX = if (side == 1) -(1f - slideIn) * 320f else (1f - slideIn) * 320f
    val bannerShape = remember(side) {
        GenericShape { size, _ ->
            val skew = size.height * 0.14f
            if (side == 1) {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - skew, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(skew, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fighter-Specific Signature Canvas Entrance VFX
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(if (side == 1) w * 0.25f else w * 0.75f, h * 0.50f)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        fighter.primaryColor.copy(alpha = 0.28f),
                        fighter.accentColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.65f
                )
            )

            when (fighter.visualStyle) {
                FighterVisualStyle.GLITCHERMAN -> {
                    // Expanding 360° EMP Shockwaves & Satellite Lock-On Reticle
                    for (ring in 1..3) {
                        val r = h * (0.18f + ring * 0.11f) * (0.65f + 0.35f * slideIn)
                        drawCircle(
                            color = (if (ring % 2 == 0) fighter.accentColor else fighter.primaryColor).copy(alpha = 0.58f),
                            radius = r,
                            center = center,
                            style = Stroke(width = 3.8f)
                        )
                    }
                    // Vertical orbital targeting laser guide
                    drawLine(
                        color = fighter.primaryColor.copy(alpha = 0.45f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, h),
                        strokeWidth = 6f
                    )
                    for (bar in 0..7) {
                        val by = ((bar * 0.12f + progress * 0.4f) % 1f) * h
                        drawRect(
                            color = fighter.primaryColor.copy(alpha = 0.35f),
                            topLeft = Offset(if (side == 1) 0f else w * 0.35f, by),
                            size = Size(w * 0.65f, 4f)
                        )
                    }
                }
                FighterVisualStyle.JARDIN -> {
                    // Massive Erupting Roots & Emerald Ground Shockwaves
                    for (v in 0..5) {
                        val startX = center.x + (v - 2.5f) * w * 0.08f
                        val vinePath = Path().apply {
                            moveTo(startX, h)
                            quadraticTo(
                                startX + sin(progress * 4f + v) * 45f,
                                h * 0.55f,
                                startX + (v - 2.5f) * 24f,
                                h * (1f - slideIn * 0.82f)
                            )
                        }
                        drawPath(
                            path = vinePath,
                            color = Color(0xFF00E676).copy(alpha = 0.60f),
                            style = Stroke(width = 10f, cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(
                        color = Color(0xFFFFC400).copy(alpha = 0.42f * (1f - progress * 0.5f)),
                        radius = h * (0.15f + progress * 0.6f),
                        center = center,
                        style = Stroke(width = 8f)
                    )
                }
                FighterVisualStyle.CHINOIS_BAGUETTES -> {
                    // Blazing Golden Speed Slashes & Floating Jade Bamboo Lines
                    for (i in 0..14) {
                        val yPos = (i / 14f) * h
                        val offset = ((progress * 1.4f + i * 0.1f) % 1f) * w
                        drawLine(
                            color = (if (i % 2 == 0) Color(0xFFFFD600) else Color(0xFFFF3D00)).copy(alpha = 0.48f),
                            start = Offset(offset - w * 0.25f, yPos),
                            end = Offset(offset, yPos - 18f),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                FighterVisualStyle.DARON_BRESILIEN -> {
                    // Heavy Tank Golden Shield Aura & Carnival Energy Waves
                    for (wave in 1..4) {
                        val r = h * (0.20f + wave * 0.12f) * (0.5f + 0.5f * slideIn)
                        drawCircle(
                            color = (if (wave % 2 == 0) Color(0xFF00C853) else Color(0xFFFFD600)).copy(alpha = 0.35f),
                            radius = r,
                            center = center,
                            style = Stroke(width = 7f)
                        )
                    }
                }
            }
        }

        // Immersive Full-Screen Elemental Emergence (No Banner Card)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    translationX = slideOffsetX
                    alpha = slideIn
                    scaleX = 0.92f + slideIn * 0.08f
                    scaleY = 0.92f + slideIn * 0.08f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(3.5.dp, Brush.horizontalGradient(listOf(fighter.primaryColor, UltiGold, fighter.accentColor)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = fighter.portraitRes),
                    contentDescription = fighter.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.72f))
                            )
                        )
                )
                Surface(
                    color = ObsidianBg.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.2.dp, UltiGold),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "ULTI: ${fighter.ultimateMove.name.uppercase()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = UltiGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = fighter.primaryColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = "$sideLabel • ${fighter.archetype.uppercase()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = fighter.primaryColor
                    )
                }
                Text(
                    text = fighter.name.uppercase(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White
                )
                Text(
                    text = fighter.introAbilityTag,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = UltiGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fighter.introQuote,
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun IntroPortraitBox(
    fighter: FighterDefinition,
    scale: Float
) {
    Box(
        modifier = Modifier
            .size(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(3.dp, fighter.primaryColor, RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(id = fighter.portraitRes),
            contentDescription = fighter.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.65f)))
                )
        )
        Surface(
            color = ObsidianBg.copy(alpha = 0.85f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, UltiGold),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = "ULTI: ${fighter.ultimateMove.name.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = UltiGold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun VsClashFlashPanel(
    p1: FighterDefinition,
    p2: FighterDefinition,
    progress: Float
) {
    val zoom = 0.88f + progress * 0.24f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        p1.primaryColor.copy(alpha = 0.35f),
                        ObsidianBg.copy(alpha = 0.88f),
                        p2.primaryColor.copy(alpha = 0.35f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FighterAvatar(fighter = p1, size = 128.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black
                    ),
                    color = UltiGold
                )
                Text(
                    text = "${p1.name.uppercase()} × ${p2.name.uppercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
            }
            FighterAvatar(fighter = p2, size = 128.dp)
        }
    }
}
