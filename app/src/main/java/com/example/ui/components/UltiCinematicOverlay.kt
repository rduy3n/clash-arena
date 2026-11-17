package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterRoster
import com.example.game.data.FighterVisualStyle
import com.example.game.engine.UltiCinematicState
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.UltiGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Cinématique d'Ultime de 2 secondes avec ralenti dramatique (cut-in anime horizontal).
 */
@Composable
fun UltiCinematicOverlay(
    cinematic: UltiCinematicState,
    timeMs: Long,
    modifier: Modifier = Modifier
) {
    val fighter = FighterRoster.getFighter(cinematic.fighterId)
    val total = cinematic.totalMs.coerceAtLeast(1L).toFloat()
    val elapsedFrac = (1f - (cinematic.remainingMs / total)).coerceIn(0f, 1f)
    // Entrance snap in first 15%, hold dramatic zoom across slow-mo, punchy exit in last 10%
    val slideIn = (elapsedFrac / 0.16f).coerceIn(0f, 1f)
    val slideSign = if (cinematic.casterSide == 1) -1f else 1f
    val zoomScale = 1.04f + elapsedFrac * 0.12f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xA804060D))
            .testTag("ulti_cinematic_overlay")
    ) {
        // Radial anime speed lines & character-themed VFX behind the cut-in strip
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(
                x = if (cinematic.casterSide == 1) w * 0.28f else w * 0.72f,
                y = h * 0.5f
            )
            val maxR = hypot(w, h)
            for (i in 0 until 28) {
                val angle = (i / 28f) * (2f * PI.toFloat()) + (timeMs * 0.005f)
                val inner = h * (0.20f + (i % 3) * 0.08f)
                drawLine(
                    color = if (i % 2 == 0) cinematic.primaryColor.copy(alpha = 0.42f) else cinematic.accentColor.copy(alpha = 0.28f),
                    start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                    end = Offset(center.x + cos(angle) * maxR, center.y + sin(angle) * maxR),
                    strokeWidth = if (i % 3 == 0) 6f else 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // Character-specific dramatic cut-in accents
            when (cinematic.visualStyle) {
                FighterVisualStyle.GLITCHERMAN -> {
                    // Orbital satellite vertical targeting beam & cyber scan bars
                    drawRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.24f),
                        topLeft = Offset(center.x - w * 0.045f, 0f),
                        size = Size(w * 0.09f, h)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.72f),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, h),
                        strokeWidth = 4f
                    )
                    for (g in 0..8) {
                        val gy = ((g * 0.11f + elapsedFrac * 0.6f) % 1f) * h
                        val gx = ((g * 0.23f + sin(timeMs * 0.02f + g) * 0.2f).coerceIn(0f, 0.85f)) * w
                        drawRect(
                            color = (if (g % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF2A85)).copy(alpha = 0.35f),
                            topLeft = Offset(gx, gy),
                            size = Size(w * 0.18f, 6f)
                        )
                    }
                }
                FighterVisualStyle.JARDIN -> {
                    for (r in 0..5) {
                        val pulseR = h * (0.22f + r * 0.14f) * (0.7f + 0.3f * sin(elapsedFrac * PI.toFloat()))
                        drawCircle(
                            color = Color(0xFF00E676).copy(alpha = 0.22f),
                            radius = pulseR,
                            center = center
                        )
                    }
                }
                FighterVisualStyle.CHINOIS_BAGUETTES -> {
                    // Crossed blazing golden chopstick slash beams
                    drawLine(
                        color = Color(0xFFFFD600).copy(alpha = 0.55f),
                        start = Offset(0f, h * 0.18f),
                        end = Offset(w, h * 0.82f),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color(0xFFFF3D00).copy(alpha = 0.45f),
                        start = Offset(0f, h * 0.78f),
                        end = Offset(w, h * 0.22f),
                        strokeWidth = 8f
                    )
                }
                FighterVisualStyle.DARON_BRESILIEN -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD600).copy(alpha = 0.40f), Color(0xFF00C853).copy(alpha = 0.18f), Color.Transparent),
                            center = center,
                            radius = h * 0.65f
                        ),
                        radius = h * 0.65f,
                        center = center
                    )
                }
            }
        }

        // Top Cinema Letterbox Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((36f * slideIn).dp)
                .align(Alignment.TopCenter)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.BottomCenter)
                    .background(cinematic.primaryColor)
            )
        }

        // Bottom Cinema Letterbox Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((36f * slideIn).dp)
                .align(Alignment.BottomCenter)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(cinematic.accentColor)
            )
        }

        // Central Wide Anime Cut-In Strip
        Surface(
            color = ObsidianBg.copy(alpha = 0.90f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(2.5.dp, Brush.horizontalGradient(listOf(cinematic.primaryColor, UltiGold, cinematic.accentColor))),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.86f)
                .height(178.dp)
                .graphicsLayer {
                    translationX = slideSign * (1f - slideIn) * 420f
                    alpha = slideIn
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cinematic.casterSide == 1) {
                    PortraitPanel(
                        portraitRes = fighter.portraitRes,
                        name = fighter.name,
                        primary = cinematic.primaryColor,
                        zoomScale = zoomScale
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    cinematic.primaryColor.copy(alpha = 0.22f),
                                    ObsidianBg,
                                    cinematic.accentColor.copy(alpha = 0.22f)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = if (cinematic.casterSide == 1) Alignment.Start else Alignment.End
                ) {
                    Surface(
                        color = UltiGold,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ULTIME • J${cinematic.casterSide}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = fighter.name.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = cinematic.primaryColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = cinematic.moveName.uppercase(),
                        style = MaterialTheme.typography.displayMedium.copy(fontStyle = FontStyle.Italic),
                        color = Color.White,
                        maxLines = 2
                    )
                }

                if (cinematic.casterSide == 2) {
                    PortraitPanel(
                        portraitRes = fighter.portraitRes,
                        name = fighter.name,
                        primary = cinematic.primaryColor,
                        zoomScale = zoomScale
                    )
                }
            }
        }
    }
}

@Composable
private fun PortraitPanel(
    portraitRes: Int,
    name: String,
    primary: Color,
    zoomScale: Float
) {
    Box(
        modifier = Modifier
            .width(176.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, primary, RoundedCornerShape(14.dp))
    ) {
        Image(
            painter = painterResource(id = portraitRes),
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, primary.copy(alpha = 0.25f))
                    )
                )
        )
    }
}
