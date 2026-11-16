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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterVisualStyle
import com.example.game.data.GameMode
import com.example.game.engine.ArenaWorldState
import com.example.ui.theme.ChakraPetchFamily
import com.example.ui.theme.CrimsonHp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RussoOneFamily
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cinématique animée personnalisée pour le vainqueur du combat (mode horizontal).
 * - Jardin : explosion émeraude/or derrière lui, puis sa main saisit et fissure la caméra !
 * - Maître Chen : devant un temple samouraï illuminé en train de manger des nouilles fumantes.
 * - Glitcherman : piratage complet de l'écran avec pluie de code, fenêtres système et glitch RGB.
 * - Daron Brésilien : posé tranquillement sur un transat au coucher du soleil avec boisson tropicale.
 */
@Composable
fun VictoryCinematicOverlay(
    world: ArenaWorldState,
    localSide: Int,
    onRematchLocal: () -> Unit,
    onExitBattle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winner = if (world.winnerSide == 1) world.p1.def else world.p2.def
    val didWin = world.winnerSide == localSide

    var replayKey by remember { mutableIntStateOf(0) }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(winner.id, replayKey) {
        elapsedMs = 0L
        while (isActive) {
            delay(16L)
            elapsedMs += 16L
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("victory_cinematic_overlay")
    ) {
        // Character-specific animated full-screen cinematic theater
        when (winner.visualStyle) {
            FighterVisualStyle.JARDIN -> JardinVictoryCinematic(winner = winner, elapsedMs = elapsedMs)
            FighterVisualStyle.CHINOIS_BAGUETTES -> ChenTempleNoodlesCinematic(winner = winner, elapsedMs = elapsedMs)
            FighterVisualStyle.GLITCHERMAN -> GlitchermanHackCinematic(winner = winner, elapsedMs = elapsedMs)
            FighterVisualStyle.DARON_BRESILIEN -> DaronTransatCinematic(winner = winner, elapsedMs = elapsedMs)
        }

        // Top Result Title Pill
        Surface(
            color = ObsidianBg.copy(alpha = 0.82f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.8.dp, if (didWin) UltiGold else CrimsonHp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (didWin) "VICTOIRE !" else "DÉFAITE",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (didWin) UltiGold else CrimsonHp
                )
                Text(
                    text = "${winner.name} (${world.p1RoundsWon} - ${world.p2RoundsWon})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        // Sleek Bottom Action Bar (preserves rematch_button & exit_match_button tags)
        Surface(
            color = ObsidianBg.copy(alpha = 0.90f),
            border = BorderStroke(1.2.dp, ObsidianOutline),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { replayKey++ },
                    border = BorderStroke(1.dp, ElectricCyan)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REJOUER", color = ElectricCyan)
                }

                if (world.mode == GameMode.VS_BOT) {
                    Button(
                        onClick = onRematchLocal,
                        colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                        modifier = Modifier.testTag("rematch_button")
                    ) {
                        Icon(Icons.Default.SportsMma, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REVANCHE")
                    }
                }

                OutlinedButton(
                    onClick = onExitBattle,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("exit_match_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MENU", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun JardinVictoryCinematic(winner: FighterDefinition, elapsedMs: Long) {
    // Timeline:
    // 0..750ms: colossal explosion builds behind Jardin
    // 750..1150ms: fist/hand lunges toward the camera lens
    // 1150ms+: impact shake + camera screen cracks into radial spiderweb glass!
    val explosionT = (elapsedMs / 950f).coerceIn(0f, 1f)
    val lungeT = ((elapsedMs - 700f) / 420f).coerceIn(0f, 1f)
    val impactAge = (elapsedMs - 1120L).coerceAtLeast(0L)
    val isCracked = elapsedMs >= 1120L
    val shakeAmp = if (isCracked && impactAge < 520L) (1f - impactAge / 520f) * 20f else 0f
    val shakeX = if (shakeAmp > 0f) sin(elapsedMs * 0.14f) * shakeAmp else 0f
    val shakeY = if (shakeAmp > 0f) cos(elapsedMs * 0.18f) * shakeAmp else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = shakeX
                translationY = shakeY
            }
    ) {
        // 1. Background explosion canvas behind Jardin
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val expCenter = Offset(w * 0.50f, h * 0.54f)

            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0A140D), Color(0xFF1A2F1C), Color(0xFF06080B))
                )
            )

            // Massive emerald & amber fireball plumes
            val blastRadius = h * (0.28f + explosionT * 0.62f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFFFD600), Color(0xFFFF6D00), Color(0xFF00E676), Color.Transparent),
                    center = expCenter,
                    radius = blastRadius
                ),
                radius = blastRadius,
                center = expCenter
            )
            // Secondary billowing explosion clouds
            for (c in 0..7) {
                val ang = c * (2f * PI.toFloat() / 8f) + elapsedMs * 0.0004f
                val dist = blastRadius * 0.45f
                val cx = expCenter.x + cos(ang) * dist
                val cy = expCenter.y + sin(ang) * dist * 0.72f
                drawCircle(
                    color = (if (c % 2 == 0) Color(0xFFFF9100) else Color(0xFF00C853)).copy(alpha = 0.52f),
                    radius = blastRadius * 0.30f,
                    center = Offset(cx, cy)
                )
            }
            // Shockwave ring
            drawCircle(
                color = Color(0xFFFFD600).copy(alpha = (1f - explosionT * 0.7f) * 0.85f),
                radius = blastRadius * 1.08f,
                center = expCenter,
                style = Stroke(width = 10f)
            )
        }

        // 2. Jardin Hero Portrait Standing in Front of Explosion
        Image(
            painter = painterResource(id = winner.portraitRes),
            contentDescription = winner.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.Center)
                .size(230.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(3.dp, UltiGold, RoundedCornerShape(24.dp))
                .graphicsLayer {
                    val s = 0.92f + explosionT * 0.10f
                    scaleX = s
                    scaleY = s
                }
        )

        // 3. Giant Mossy Hand Grabbing / Lunging & Radial Cracked Glass Lens
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.52f, h * 0.48f)

            if (lungeT > 0f) {
                // Hand / giant fist reaching from bottom-right toward camera center
                val scale = 0.35f + lungeT * 0.95f
                val handPos = Offset(
                    x = center.x + (1f - lungeT) * w * 0.25f,
                    y = center.y + (1f - lungeT) * h * 0.35f
                )
                val palmR = h * 0.13f * scale
                // Forearm
                drawLine(
                    color = Color(0xFF1F4E29),
                    start = Offset(w * 0.88f, h * 1.05f),
                    end = handPos,
                    strokeWidth = palmR * 1.45f,
                    cap = StrokeCap.Round
                )
                // Giant gripping palm & fingers
                drawCircle(color = Color(0xFF2E6B3A), radius = palmR, center = handPos)
                drawCircle(color = Color(0xFF070910), radius = palmR, center = handPos, style = Stroke(width = 5f))
                for (f in -2..2) {
                    val ang = -PI.toFloat() * 0.5f + f * 0.34f
                    val fingerTip = Offset(handPos.x + cos(ang) * palmR * 1.45f, handPos.y + sin(ang) * palmR * 1.45f)
                    drawLine(
                        color = Color(0xFF070910),
                        start = handPos,
                        end = fingerTip,
                        strokeWidth = palmR * 0.44f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF00C853),
                        start = handPos,
                        end = fingerTip,
                        strokeWidth = palmR * 0.34f,
                        cap = StrokeCap.Round
                    )
                }
            }

            if (isCracked) {
                // Full-screen spiderweb cracked camera lens!
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = h * 0.08f,
                    center = center
                )
                val rays = 14
                for (i in 0 until rays) {
                    val baseAng = (i / rays.toFloat()) * (2f * PI.toFloat()) + (i % 3) * 0.09f
                    val crackPath = Path().apply {
                        moveTo(center.x, center.y)
                        var curDist = 0f
                        for (seg in 1..5) {
                            curDist += w * 0.12f
                            val jitter = if (seg % 2 == 0) 0.08f else -0.07f
                            val px = center.x + cos(baseAng + jitter) * curDist
                            val py = center.y + sin(baseAng + jitter) * curDist
                            lineTo(px, py)
                        }
                    }
                    drawPath(crackPath, color = Color(0xFF05070B).copy(alpha = 0.85f), style = Stroke(width = 5.5f, join = StrokeJoin.Round))
                    drawPath(crackPath, color = Color.White.copy(alpha = 0.92f), style = Stroke(width = 2.4f, join = StrokeJoin.Round))
                }
                // Concentric spiderweb glass rings with specular shards
                for (ring in 1..3) {
                    val r = h * (0.15f * ring)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.60f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChenTempleNoodlesCinematic(winner: FighterDefinition, elapsedMs: Long) {
    val settleT = (elapsedMs / 900f).coerceIn(0f, 1f)
    val liftCycle = 0.5f + 0.5f * sin(elapsedMs * 0.004f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Samurai Temple Artwork Background
        Image(
            painter = painterResource(id = R.drawable.img_arena_dojo_gg),
            contentDescription = "Temple Samouraï",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xBA090710), Color(0x55090710), Color(0xCA090710))
                    )
                )
        )

        // Temple lanterns, sakura petals, steaming noodle bowl & chopstick noodle lift!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Glowing Japanese temple lanterns hanging from eaves
            for (lxFrac in listOf(0.14f, 0.86f)) {
                val lx = w * lxFrac
                val sway = sin(elapsedMs * 0.002f + lxFrac * 10f) * 8f
                drawLine(Color(0xFF1A0F0A), Offset(lx, 0f), Offset(lx + sway, h * 0.14f), strokeWidth = 4f)
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFD600), Color(0xFFFF3D00))),
                    topLeft = Offset(lx + sway - 24f, h * 0.14f),
                    size = Size(48f, 64f)
                )
                drawOval(
                    color = Color(0xFF210703),
                    topLeft = Offset(lx + sway - 24f, h * 0.14f),
                    size = Size(48f, 64f),
                    style = Stroke(width = 3f)
                )
            }

            // Drifting pink sakura petals
            for (p in 0..18) {
                val t = ((elapsedMs * 0.00018f) + p * 0.09f) % 1f
                val px = ((p * 0.06f + sin(elapsedMs * 0.0015f + p) * 0.04f) % 1f) * w
                val py = t * h
                drawOval(
                    color = Color(0xFFFFB2C8).copy(alpha = 0.78f),
                    topLeft = Offset(px, py),
                    size = Size(10f, 6f)
                )
            }

            // Artisan Ramen / Noodle Bowl on the right foreground table
            val bowlCenter = Offset(w * 0.64f, h * 0.60f)
            val bowlW = h * 0.44f
            val bowlH = h * 0.24f

            // Wooden temple table plank
            drawRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF5D4037), Color(0xFF2D1B14))),
                topLeft = Offset(w * 0.44f, bowlCenter.y + bowlH * 0.34f),
                size = Size(w * 0.44f, h * 0.42f)
            )

            // Ceramic Bowl
            val bowlPath = Path().apply {
                moveTo(bowlCenter.x - bowlW * 0.5f, bowlCenter.y - bowlH * 0.2f)
                lineTo(bowlCenter.x + bowlW * 0.5f, bowlCenter.y - bowlH * 0.2f)
                quadraticTo(bowlCenter.x + bowlW * 0.42f, bowlCenter.y + bowlH * 0.55f, bowlCenter.x, bowlCenter.y + bowlH * 0.58f)
                quadraticTo(bowlCenter.x - bowlW * 0.42f, bowlCenter.y + bowlH * 0.55f, bowlCenter.x - bowlW * 0.5f, bowlCenter.y - bowlH * 0.2f)
                close()
            }
            drawPath(
                bowlPath,
                brush = Brush.verticalGradient(listOf(Color(0xFFD50000), Color(0xFF7F0000)))
            )
            drawPath(bowlPath, color = Color(0xFF0A080C), style = Stroke(width = 4f))
            // Gold rim & broth
            drawOval(
                color = Color(0xFFFFAB40),
                topLeft = Offset(bowlCenter.x - bowlW * 0.47f, bowlCenter.y - bowlH * 0.32f),
                size = Size(bowlW * 0.94f, bowlH * 0.32f)
            )
            drawOval(
                color = Color(0xFFFFD600),
                topLeft = Offset(bowlCenter.x - bowlW * 0.50f, bowlCenter.y - bowlH * 0.34f),
                size = Size(bowlW, bowlH * 0.35f),
                style = Stroke(width = 4f)
            )

            // Lifted golden noodles & twin chopsticks!
            val liftY = bowlCenter.y - bowlH * (0.65f + liftCycle * 0.42f)
            for (n in -2..2) {
                val nx = bowlCenter.x + n * 10f
                val noodle = Path().apply {
                    moveTo(nx, bowlCenter.y - bowlH * 0.18f)
                    quadraticTo(nx + sin(elapsedMs * 0.008f + n) * 6f, (bowlCenter.y + liftY) * 0.5f, nx + n * 2f, liftY)
                }
                drawPath(noodle, color = Color(0xFFFFF176), style = Stroke(width = 5f, cap = StrokeCap.Round))
            }
            // Twin jade & gold chopsticks pinching the noodles
            drawLine(
                color = Color(0xFF00E676),
                start = Offset(bowlCenter.x - bowlW * 0.22f, liftY - 6f),
                end = Offset(bowlCenter.x + bowlW * 0.62f, liftY - 58f),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(bowlCenter.x - bowlW * 0.20f, liftY + 8f),
                end = Offset(bowlCenter.x + bowlW * 0.64f, liftY - 38f),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )

            // Rising animated steam wisps from hot noodles
            for (s in 0..3) {
                val st = ((elapsedMs * 0.0005f) + s * 0.25f) % 1f
                val sx = bowlCenter.x - 30f + s * 22f + sin(st * 6f + s) * 10f
                val sy = bowlCenter.y - bowlH * 0.4f - st * h * 0.26f
                drawCircle(
                    color = Color.White.copy(alpha = (1f - st) * 0.35f),
                    radius = 12f + st * 14f,
                    center = Offset(sx, sy)
                )
            }
        }

        // Maître Chen Portrait + Cozy Temple Quote
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 34.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = winner.portraitRes),
                contentDescription = winner.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(168.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(3.dp, UltiGold, RoundedCornerShape(20.dp))
                    .graphicsLayer { alpha = settleT }
            )
            Surface(
                color = ObsidianBg.copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, UltiGold)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("PAIX DU TEMPLE", style = MaterialTheme.typography.labelSmall, color = UltiGold)
                    Text(
                        text = "« Un bol de nouilles\naprès la victoire ! »",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun GlitchermanHackCinematic(winner: FighterDefinition, elapsedMs: Long) {
    val glitchStep = (elapsedMs / 90L).toInt()
    val rgbJitterX = if (glitchStep % 4 == 0) 8f else -4f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        // Matrix code rain & RGB glitch scan bars
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Cyber terminal grid
            val step = 48f
            var x = 0f
            while (x < w) {
                drawLine(Color(0xFF00E5FF).copy(alpha = 0.09f), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < h) {
                drawLine(Color(0xFF00E5FF).copy(alpha = 0.09f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                y += step
            }

            // Falling Matrix/Hex streaks
            for (col in 0..22) {
                val cx = (col / 22f) * w + 10f
                val headY = (((elapsedMs * 0.28f) + col * 97f) % (h + 180f)) - 90f
                drawLine(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = 0.65f), Color.White),
                        startY = headY - 120f,
                        endY = headY
                    ),
                    start = Offset(cx, headY - 120f),
                    end = Offset(cx, headY),
                    strokeWidth = 3f
                )
            }

            // Sudden RGB horizontal glitch tears
            for (tear in 0..5) {
                val ty = ((tear * 0.17f + (glitchStep % 7) * 0.09f) % 1f) * h
                val tw = w * (0.25f + (tear % 3) * 0.22f)
                val tx = ((tear * 0.31f + glitchStep * 0.13f) % 0.7f) * w
                drawRect(
                    color = (if (tear % 2 == 0) Color(0xFFFF2A85) else Color(0xFF00E5FF)).copy(alpha = 0.38f),
                    topLeft = Offset(tx, ty),
                    size = Size(tw, 9f)
                )
            }
        }

        // Main Hacked Terminal Window + Pop-up Windows
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Hacked Portrait Frame with RGB offset
            Box(
                modifier = Modifier
                    .size(182.dp)
                    .graphicsLayer { translationX = rgbJitterX * 0.5f }
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.5.dp, ElectricCyan, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = winner.portraitRes),
                    contentDescription = winner.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC020612))
                        .align(Alignment.BottomCenter)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "ADMIN://GLITCHERMAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }

            // Right: Hacked System Console
            Surface(
                color = Color(0xE6070D1D),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, ElectricCyan),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ROOT_TERMINAL // SYSTÈME PIRATÉ",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFF2A85)
                        )
                        Text(
                            text = "[100% PWNED]",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldHp
                        )
                    }
                    Text(
                        text = "> OVERRIDE ARENA_FIREWALL... OK\n> INJECT BAYONETTA_PROTOCOL.BIN... OK\n> CONTRÔLE TOTAL DE L'ÉCRAN ACQUIS",
                        fontFamily = ChakraPetchFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                    Surface(
                        color = Color(0xFFFF2A85).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF2A85))
                    ) {
                        Text(
                            text = "SYSTÈME PIRATÉ PAR GLITCHERMAN",
                            fontFamily = RussoOneFamily,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DaronTransatCinematic(winner: FighterDefinition, elapsedMs: Long) {
    val sway = sin(elapsedMs * 0.0022f) * 8f

    Box(modifier = Modifier.fillMaxSize()) {
        // Golden Tropical Rio Sunset + Ocean + Swaying Palm Trees + Daron Lounging on Transat with Cocktail!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sunset Sky
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF281242), Color(0xFFFF5252), Color(0xFFFFB74D), Color(0xFFFFE082)),
                    endY = h * 0.64f
                ),
                size = Size(w, h * 0.64f)
            )
            // Glowing Setting Sun
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, Color(0xFFFFD600), Color.Transparent)),
                radius = h * 0.24f,
                center = Offset(w * 0.72f, h * 0.44f)
            )
            // Ocean water with shimmering sun reflections
            drawRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF00838F), Color(0xFF00ACC1))),
                topLeft = Offset(0f, h * 0.56f),
                size = Size(w, h * 0.16f)
            )
            for (wv in 0..5) {
                val wy = h * (0.58f + wv * 0.022f)
                val wShift = sin(elapsedMs * 0.003f + wv) * 14f
                val wWidth = w * (0.14f - wv * 0.015f)
                drawLine(
                    color = Color(0xFFFFE082).copy(alpha = 0.55f - wv * 0.06f),
                    start = Offset(w * 0.72f - wWidth * 0.5f + wShift, wy),
                    end = Offset(w * 0.72f + wWidth * 0.5f + wShift, wy),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            }
            // Golden beach sand
            drawRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFDD835), Color(0xFFE6B442))),
                topLeft = Offset(0f, h * 0.70f),
                size = Size(w, h * 0.30f)
            )

            // Swaying Palm Tree on Right
            drawPalmTree(base = Offset(w * 0.90f, h * 0.76f), height = h * 0.56f, sway = sway)
            drawPalmTree(base = Offset(w * 0.10f, h * 0.78f), height = h * 0.46f, sway = -sway * 0.7f)

            // Beach Lounger (Transat) centered right!
            val loungerX = w * 0.58f
            val loungerY = h * 0.68f
            // Wooden legs
            drawLine(Color(0xFF5D4037), Offset(loungerX - 95f, loungerY), Offset(loungerX - 110f, loungerY + 46f), strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(Color(0xFF5D4037), Offset(loungerX + 100f, loungerY + 12f), Offset(loungerX + 110f, loungerY + 46f), strokeWidth = 9f, cap = StrokeCap.Round)
            // Reclined backrest + seat frame
            val backrestTop = Offset(loungerX - 135f, loungerY - 78f)
            val seatJoint = Offset(loungerX - 25f, loungerY + 6f)
            val footEnd = Offset(loungerX + 145f, loungerY + 10f)
            drawLine(Color(0xFF00C853), backrestTop, seatJoint, strokeWidth = 22f, cap = StrokeCap.Round)
            drawLine(Color(0xFFFFD600), seatJoint, footEnd, strokeWidth = 20f, cap = StrokeCap.Round)
            drawLine(Color(0xFF070910), backrestTop, seatJoint, strokeWidth = 3f)

            // Daron relaxed torso in yellow summer shirt + sunglasses lounging back
            val headPos = Offset(loungerX - 88f, loungerY - 76f)
            drawRoundRect(
                color = Color(0xFFFFE033),
                topLeft = Offset(loungerX - 82f, loungerY - 58f),
                size = Size(92f, 52f),
                cornerRadius = CornerRadius(18f, 18f)
            )
            // Blue shorts & legs stretched out comfortably
            drawLine(Color(0xFF1E88E5), Offset(loungerX + 4f, loungerY - 22f), Offset(loungerX + 122f, loungerY - 6f), strokeWidth = 28f, cap = StrokeCap.Round)
            // Head + sunglasses + big relaxed smile/mustache
            drawCircle(color = Color(0xFFE0B08A), radius = 32f, center = headPos)
            drawCircle(color = Color(0xFF070910), radius = 32f, center = headPos, style = Stroke(width = 3f))
            drawRoundRect(color = Color(0xFF111318), topLeft = Offset(headPos.x - 4f, headPos.y - 12f), size = Size(30f, 14f), cornerRadius = CornerRadius(4f, 4f))

            // Tropical Cocktail in his raised hand (with straw & little umbrella!)
            val glassCenter = Offset(loungerX + 34f, loungerY - 94f)
            drawLine(Color(0xFFE0B08A), Offset(loungerX - 10f, loungerY - 40f), glassCenter, strokeWidth = 14f, cap = StrokeCap.Round)
            val cupPath = Path().apply {
                moveTo(glassCenter.x - 22f, glassCenter.y - 26f)
                lineTo(glassCenter.x + 22f, glassCenter.y - 26f)
                lineTo(glassCenter.x + 14f, glassCenter.y + 18f)
                lineTo(glassCenter.x - 14f, glassCenter.y + 18f)
                close()
            }
            drawPath(
                cupPath,
                brush = Brush.verticalGradient(listOf(Color(0xFFFFD600), Color(0xFFFF3D00)))
            )
            drawPath(cupPath, color = Color.White, style = Stroke(width = 3f))
            // Straw
            drawLine(Color(0xFF00E5FF), Offset(glassCenter.x + 6f, glassCenter.y + 6f), Offset(glassCenter.x + 22f, glassCenter.y - 46f), strokeWidth = 4f, cap = StrokeCap.Round)
            // Cocktail Umbrella
            drawLine(Color(0xFF8D6E63), Offset(glassCenter.x - 10f, glassCenter.y - 10f), Offset(glassCenter.x - 24f, glassCenter.y - 44f), strokeWidth = 3f)
            drawArc(
                color = Color(0xFFFF2A85),
                startAngle = 190f,
                sweepAngle = 150f,
                useCenter = true,
                topLeft = Offset(glassCenter.x - 44f, glassCenter.y - 60f),
                size = Size(40f, 28f)
            )
        }

        // Left: Daron Portrait + Chill Badge
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = winner.portraitRes),
                contentDescription = winner.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(164.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(3.dp, UltiGold, RoundedCornerShape(20.dp))
            )
            Surface(
                color = ObsidianBg.copy(alpha = 0.84f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, UltiGold)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("AMBIANCE RIO", style = MaterialTheme.typography.labelSmall, color = UltiGold)
                    Text(
                        text = "Posé sur le transat,\ncocktail à la main !",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawPalmTree(base: Offset, height: Float, sway: Float) {
    val top = Offset(base.x + sway * 1.4f - 28f, base.y - height)
    val trunk = Path().apply {
        moveTo(base.x, base.y)
        quadraticTo(base.x - 14f, base.y - height * 0.5f, top.x, top.y)
    }
    drawPath(trunk, color = Color(0xFF4E342E), style = Stroke(width = 16f, cap = StrokeCap.Round))
    for (leaf in 0..4) {
        val ang = -PI.toFloat() * 0.85f + leaf * 0.42f + sway * 0.01f
        val tip = Offset(top.x + cos(ang) * height * 0.42f, top.y + sin(ang) * height * 0.26f + 22f)
        drawLine(Color(0xFF00C853), top, tip, strokeWidth = 14f, cap = StrokeCap.Round)
    }
}
