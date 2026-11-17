package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterVisualStyle
import com.example.game.data.GameMode
import com.example.game.engine.ArcadeRankCalculator
import com.example.game.engine.ArcadeRankSummary
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.FinishType
import com.example.ui.theme.ChakraPetchFamily
import com.example.ui.theme.CrimsonHp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RussoOneFamily
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Cinématiques de fin plein écran façon Street Fighter 6 :
 * illustration 16:9 avec zoom Ken Burns, FX anime propres au combattant,
 * badge Finish (PERFECT / ULTI FINISH / K.O.) et bilan arcade compact avec rang S/A/B/C.
 */
@Composable
fun VictoryCinematicOverlay(
    world: ArenaWorldState,
    localSide: Int,
    onRematchLocal: () -> Unit,
    onOnlineRematchToSelect: () -> Unit = {},
    onTournamentContinue: () -> Unit = {},
    onExitBattle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winnerRuntime = if (world.winnerSide == 1) world.p1 else world.p2
    val winner = winnerRuntime.def
    val didWin = world.winnerSide == localSide
    val localRuntime = if (localSide == 1) world.p1 else world.p2
    val myRounds = if (localSide == 1) world.p1RoundsWon else world.p2RoundsWon
    val oppRounds = if (localSide == 1) world.p2RoundsWon else world.p1RoundsWon
    val totalDamage = localRuntime.matchDamageTotal.coerceAtLeast(localRuntime.dealtDamageTotal)
    val remainingHpRatio = (localRuntime.hp.toFloat() / localRuntime.def.stats.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)

    val rankSummary = remember(
        didWin,
        myRounds,
        oppRounds,
        remainingHpRatio,
        localRuntime.maxComboCount,
        totalDamage,
        world.finishType
    ) {
        ArcadeRankCalculator.evaluate(
            won = didWin,
            roundsWon = myRounds,
            roundsLost = oppRounds,
            remainingHpRatio = remainingHpRatio,
            maxCombo = localRuntime.maxComboCount,
            totalDamage = totalDamage,
            finishType = world.finishType
        )
    }

    var replayKey by remember { mutableIntStateOf(0) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(winner.id, replayKey) {
        elapsedMs = 0L
        while (isActive) {
            delay(16L)
            elapsedMs += 16L
        }
    }

    val flashAlpha = ((1f - (elapsedMs / 320f)) * 0.78f).coerceIn(0f, 0.78f)
    val quoteReveal = ((elapsedMs - 220f) / 360f).coerceIn(0f, 1f)
    val rankReveal = ((elapsedMs - 400f) / 360f).coerceIn(0f, 1f)

    val slantedShape = remember {
        GenericShape { size, _ ->
            val skew = (size.height * 0.24f).coerceAtMost(size.width * 0.12f)
            moveTo(skew, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - skew, size.height)
            lineTo(0f, size.height)
            close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("victory_cinematic_overlay")
    ) {
        // 1. 100% Animated 60 FPS Choreographed Victory Scene (no static image)
        VictoryChoreographyScene(
            winner = winner,
            elapsedMs = elapsedMs,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Subtle Edge Scrim so top/bottom HUD stays legible while keeping center action crisp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x9904060D),
                            Color.Transparent,
                            Color(0x1804060D),
                            Color(0xBB04060D)
                        )
                    )
                )
        )

        // 3. Fighter-specific Anime FX Overlay + Radial Speedlines
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawVictorySpeedLines(winner = winner, elapsedMs = elapsedMs)
            when (winner.visualStyle) {
                FighterVisualStyle.JARDIN -> drawJardinVictoryFx(elapsedMs)
                FighterVisualStyle.CHINOIS_BAGUETTES -> drawChenVictoryFx(elapsedMs)
                FighterVisualStyle.GLITCHERMAN -> drawGlitchermanVictoryFx(elapsedMs)
                FighterVisualStyle.DARON_BRESILIEN -> drawDaronVictoryFx(elapsedMs)
            }
        }

        // 4. Opening Cinema Impact Flash (0..320ms)
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha))
            )
        }

        // 5. Cinema Letterbox Bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.90f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.90f))
        )

        // 6. Structured HUD Layer (Top Banner, Middle Quote + Arcade Rank, Bottom Action Bar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR: Victory/Defeat Banner + Finish Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xEC080B14),
                    shape = slantedShape,
                    border = BorderStroke(2.2.dp, if (didWin) UltiGold else CrimsonHp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        winner.primaryColor.copy(alpha = 0.24f),
                                        Color(0xFF0A1020)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (didWin) "VICTOIRE LÉGENDAIRE !" else "DÉFAITE HÉROÏQUE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Black
                            ),
                            color = if (didWin) UltiGold else CrimsonHp
                        )
                        Text(
                            text = "${winner.name.uppercase()} • ${world.p1RoundsWon} - ${world.p2RoundsWon}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                // Finish Type Callout Badge
                val finishColor = when (world.finishType) {
                    FinishType.PERFECT -> UltiGold
                    FinishType.ULTI_FINISH -> ElectricCyan
                    FinishType.KO -> FlameOrange
                    FinishType.TIME_UP -> Color(0xFFB0BEC5)
                }
                Surface(
                    color = Color(0xEC080B14),
                    shape = slantedShape,
                    border = BorderStroke(1.8.dp, finishColor)
                ) {
                    Text(
                        text = world.finishType.label,
                        fontFamily = RussoOneFamily,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Black
                        ),
                        color = finishColor,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }

            // MIDDLE ROW: Quote Card (Left) + Arcade Rank Card (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Fighter Tagline & Victory Quote
                Surface(
                    color = Color(0xDF070B14),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, winner.primaryColor.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .graphicsLayer {
                            alpha = quoteReveal
                            translationX = (1f - quoteReveal) * -42f
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = winner.victoryTagline,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = winner.primaryColor
                        )
                        Text(
                            text = winner.victoryQuote,
                            fontFamily = RussoOneFamily,
                            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                            color = Color.White
                        )
                        Text(
                            text = "ARÈNE : ${world.stage.name.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Compact Arcade Scoreboard with Animated Rank S / A / B / C
                ArcadeRankScoreCard(
                    rank = rankSummary,
                    totalDamage = totalDamage,
                    maxCombo = localRuntime.maxComboCount,
                    myRounds = myRounds,
                    oppRounds = oppRounds,
                    accentColor = winner.primaryColor,
                    revealFraction = rankReveal
                )
            }

            // BOTTOM BAR: Replay Cinematic + Mode Actions
            Surface(
                color = Color(0xEE080B14),
                border = BorderStroke(1.6.dp, UltiGold.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { replayKey++ },
                        border = BorderStroke(1.5.dp, ElectricCyan),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("replay_cinematic_button")
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REJOUER CINÉMATIQUE", color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }

                    when (world.mode) {
                        GameMode.VS_BOT -> {
                            Button(
                                onClick = onRematchLocal,
                                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("rematch_button")
                            ) {
                                Icon(Icons.Default.SportsMma, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("REVANCHE", fontWeight = FontWeight.Bold)
                            }
                        }

                        GameMode.ONLINE -> {
                            Button(
                                onClick = onOnlineRematchToSelect,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldHp,
                                    contentColor = ObsidianBg
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("online_rematch_button")
                            ) {
                                Icon(Icons.Default.SportsMma, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("REVANCHE (CHOIX PERSO)", fontWeight = FontWeight.Black)
                            }
                        }

                        GameMode.TOURNAMENT -> {
                            Button(
                                onClick = onTournamentContinue,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = UltiGold,
                                    contentColor = ObsidianBg
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("tournament_continue_button")
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CONTINUER LE TOURNOI", fontWeight = FontWeight.Black)
                            }
                        }

                        GameMode.TRAINING -> Unit
                    }

                    if (world.mode != GameMode.TOURNAMENT) {
                        OutlinedButton(
                            onClick = onExitBattle,
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("exit_match_button")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (world.mode == GameMode.ONLINE) "QUITTER LE SALON" else "MENU PRINCIPAL",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcadeRankScoreCard(
    rank: ArcadeRankSummary,
    totalDamage: Int,
    maxCombo: Int,
    myRounds: Int,
    oppRounds: Int,
    accentColor: Color,
    revealFraction: Float
) {
    val popScale = 0.86f + revealFraction * 0.14f
    Surface(
        color = Color(0xEB080B14),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, rank.tier.badgeColor.copy(alpha = 0.90f)),
        modifier = Modifier
            .graphicsLayer {
                alpha = revealFraction
                scaleX = popScale
                scaleY = popScale
                translationX = (1f - revealFraction) * 36f
            }
            .testTag("arcade_rank_card")
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF0A0E1B),
                            rank.tier.badgeColor.copy(alpha = 0.14f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rank Letter Badge (S / A / B / C)
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                rank.tier.badgeColor.copy(alpha = 0.36f),
                                Color(0xFF0A0D18)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(2.2.dp, rank.tier.badgeColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.tier.letter,
                    fontFamily = RussoOneFamily,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black
                    ),
                    color = rank.tier.badgeColor
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = rank.tier.subtitle,
                        fontFamily = ChakraPetchFamily,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = rank.tier.badgeColor
                    )
                    Text(
                        text = "${rank.score} PTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ArcadeStatCell(label = "DÉGÂTS", value = "$totalDamage", valueColor = ElectricCyan)
                    ArcadeStatCell(label = "COMBO MAX", value = "$maxCombo HITS", valueColor = accentColor)
                    ArcadeStatCell(label = "ROUNDS", value = "$myRounds - $oppRounds", valueColor = UltiGold)
                }
            }
        }
    }
}

@Composable
private fun ArcadeStatCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = RussoOneFamily,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

private fun DrawScope.drawVictorySpeedLines(winner: FighterDefinition, elapsedMs: Long) {
    val w = size.width
    val h = size.height
    val center = Offset(w * 0.50f, h * 0.44f)
    val maxR = hypot(w, h)

    for (r in 0..22) {
        val angle = (r / 22f) * (2f * PI.toFloat()) + elapsedMs * 0.00055f
        val inner = h * (0.28f + (r % 3) * 0.06f)
        drawLine(
            color = if (r % 3 == 0) Color.White.copy(alpha = 0.14f) else winner.primaryColor.copy(alpha = 0.17f),
            start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
            end = Offset(center.x + cos(angle) * maxR, center.y + sin(angle) * maxR),
            strokeWidth = if (r % 2 == 0) 4.5f else 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawJardinVictoryFx(elapsedMs: Long) {
    val w = size.width
    val h = size.height
    val erupt = (elapsedMs / 900f).coerceIn(0f, 1f)

    // Rising glowing emerald/golden forest spores
    for (i in 0..26) {
        val t = ((elapsedMs * 0.00018f) + i * 0.07f) % 1f
        val px = ((i * 0.039f + sin(elapsedMs * 0.0014f + i) * 0.03f) * w)
        val py = (1f - t) * h
        val alpha = (1f - t) * 0.88f
        drawCircle(
            color = (if (i % 3 == 0) UltiGold else EmeraldHp).copy(alpha = alpha),
            radius = 3f + (i % 3) * 2f,
            center = Offset(px, py)
        )
    }

    // Subtle glowing ground root fissures along bottom-left / bottom-right corners
    for (v in 0..5) {
        val xBase = if (v < 3) w * (0.04f + v * 0.06f) else w * (0.80f + (v - 3) * 0.06f)
        val sway = sin(elapsedMs * 0.0035f + v) * 18f
        val topY = h * (1f - erupt * (0.24f + (v % 2) * 0.10f))
        val path = Path().apply {
            moveTo(xBase, h)
            quadraticTo(xBase + sway, (h + topY) * 0.5f, xBase + 8f, topY)
        }
        drawPath(path, color = EmeraldHp.copy(alpha = 0.45f), style = Stroke(width = 6f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawChenVictoryFx(elapsedMs: Long) {
    val w = size.width
    val h = size.height

    // Blazing golden slash ribbon across backdrop
    drawLine(
        brush = Brush.linearGradient(
            listOf(Color.Transparent, UltiGold.copy(alpha = 0.55f), FlameOrange.copy(alpha = 0.45f), Color.Transparent)
        ),
        start = Offset(0f, h * 0.75f),
        end = Offset(w, h * 0.20f),
        strokeWidth = 9f,
        cap = StrokeCap.Round
    )

    // Drifting sakura petals
    for (p in 0..24) {
        val t = ((elapsedMs * 0.00022f) + p * 0.082f) % 1f
        val px = ((p * 0.048f + sin(elapsedMs * 0.0019f + p) * 0.045f) % 1f) * w
        val py = t * h
        drawOval(
            color = Color(0xFFFFB2C8).copy(alpha = 0.85f),
            topLeft = Offset(px, py),
            size = Size(11f, 6.5f)
        )
    }
}

private fun DrawScope.drawGlitchermanVictoryFx(elapsedMs: Long) {
    val w = size.width
    val h = size.height
    val glitchStep = (elapsedMs / 95L).toInt()

    // Cyber matrix vertical data streams
    for (col in 0..22) {
        val cx = (col / 22f) * w + 12f
        val headY = (((elapsedMs * 0.32f) + col * 105f) % (h + 160f)) - 80f
        drawLine(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, ElectricCyan.copy(alpha = 0.65f), Color.White.copy(alpha = 0.8f)),
                startY = headY - 120f,
                endY = headY
            ),
            start = Offset(cx, headY - 120f),
            end = Offset(cx, headY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    // Periodic chromatic glitch scan bars
    for (tear in 0..5) {
        val ty = ((tear * 0.17f + (glitchStep % 7) * 0.08f) % 1f) * h
        val tw = w * (0.20f + (tear % 3) * 0.16f)
        val tx = ((tear * 0.29f + glitchStep * 0.11f) % 0.76f) * w
        drawRect(
            color = (if (tear % 2 == 0) Color(0xFFFF2A85) else ElectricCyan).copy(alpha = 0.30f),
            topLeft = Offset(tx, ty),
            size = Size(tw, 5f)
        )
    }
}

private fun DrawScope.drawDaronVictoryFx(elapsedMs: Long) {
    val w = size.width
    val h = size.height

    // Rising golden sunset embers & tropical confetti
    for (i in 0..28) {
        val t = ((elapsedMs * 0.00019f) + i * 0.065f) % 1f
        val px = ((i * 0.036f + cos(elapsedMs * 0.0015f + i) * 0.03f) * w)
        val py = (1f - t) * h
        val alpha = (1f - t) * 0.86f
        val color = when (i % 3) {
            0 -> UltiGold
            1 -> EmeraldHp
            else -> FlameOrange
        }
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = 2.8f + (i % 3) * 1.7f,
            center = Offset(px, py)
        )
    }
}
