package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.game.data.MoveEffectType
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.CombatAction
import com.example.game.engine.SlashEffect
import com.example.ui.theme.RussoOneFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArenaCanvas(
    world: ArenaWorldState,
    timeMs: Long,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        // Stage artwork backdrop
        Image(
            painter = painterResource(id = world.stage.backgroundRes),
            contentDescription = world.stage.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Cinematic lighting & depth vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66060810),
                            Color(0x14060810),
                            Color(0xC2060810)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val groundY = h * 0.84f

            // Screen shake offset on heavy Ulti / Colossal Punch hits
            val shakeMag = if (world.screenShakeMs > 0L) (world.screenShakeMs / 380f) * 16f else 0f
            val shakeX = if (shakeMag > 0f) sin(timeMs * 0.9f) * shakeMag else 0f
            val shakeY = if (shakeMag > 0f) cos(timeMs * 1.1f) * shakeMag * 0.7f else 0f

            translate(left = shakeX, top = shakeY) {
                // Perspective glossy arcade floor
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            world.stage.floorColor.copy(alpha = 0.88f),
                            Color(0xFF04060C)
                        ),
                        startY = groundY,
                        endY = h
                    ),
                    topLeft = Offset(0f, groundY),
                    size = Size(w, h - groundY)
                )

                // Perspective grid lines + horizontal depth lines
                for (i in 0..10) {
                    val topX = w * (i / 10f)
                    val bottomX = w * 0.5f + (topX - w * 0.5f) * 1.55f
                    drawLine(
                        color = world.stage.accentGlow.copy(alpha = 0.22f),
                        start = Offset(topX, groundY),
                        end = Offset(bottomX, h),
                        strokeWidth = 2f
                    )
                }
                for (row in 1..2) {
                    val ry = groundY + (h - groundY) * (row / 3f)
                    drawLine(
                        color = world.stage.accentGlow.copy(alpha = 0.14f),
                        start = Offset(0f, ry),
                        end = Offset(w, ry),
                        strokeWidth = 1.5f
                    )
                }

                // Glowing horizon trim
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            world.stage.accentGlow.copy(alpha = 0.45f),
                            world.stage.accentGlow,
                            world.stage.accentGlow.copy(alpha = 0.45f)
                        )
                    ),
                    start = Offset(0f, groundY),
                    end = Offset(w, groundY),
                    strokeWidth = 5f
                )

                // Ambient stage particles (sakura petals or neon embers)
                val isDojo = world.stage.id == "sunset_dojo"
                for (pIdx in 0..15) {
                    val t = ((timeMs * 0.00016f) + pIdx * 0.11f) % 1f
                    val px = ((pIdx * 0.068f + sin(timeMs * 0.0012f + pIdx) * 0.03f) * w)
                    val py = if (isDojo) t * groundY else (1f - t) * groundY
                    drawCircle(
                        color = if (isDojo) Color(0xFFFFB2C8).copy(alpha = 0.60f) else world.stage.accentGlow.copy(alpha = 0.45f),
                        radius = if (isDojo) 4.2f else 3f,
                        center = Offset(px, py)
                    )
                }

                // Dramatic Ulti darkening + anime speed-lines!
                val ultiActive = world.ultiFlashMs > 0L ||
                    world.p1.action == CombatAction.ULTI ||
                    world.p2.action == CombatAction.ULTI ||
                    world.p1.chargingMegaPunch ||
                    world.p2.chargingMegaPunch
                if (ultiActive) {
                    val flashAlpha = if (world.ultiFlashMs > 0L) (world.ultiFlashMs / 360f).coerceIn(0f, 1f) * 0.30f else 0.18f
                    drawRect(color = Color(0xFF03040A).copy(alpha = 0.36f))
                    drawRect(color = world.ultiFlashColor.copy(alpha = flashAlpha))
                    val focusFighter = when {
                        world.p1.action == CombatAction.ULTI || world.p1.chargingMegaPunch -> world.p1
                        world.p2.action == CombatAction.ULTI || world.p2.chargingMegaPunch -> world.p2
                        else -> world.p1
                    }
                    drawAnimeSpeedLines(
                        center = Offset(focusFighter.x * w, groundY - focusFighter.y * h * 0.6f - h * 0.16f),
                        w = w,
                        h = h,
                        color = focusFighter.def.primaryColor,
                        timeMs = timeMs
                    )
                }

                // Fighter floor glow reflections
                drawFighterReflection(world.p1, w, h, groundY)
                drawFighterReflection(world.p2, w, h, groundY)

                // Detailed cel-shaded 2D fighters
                drawDetailedFighter(world.p1, w, h, groundY, timeMs)
                drawDetailedFighter(world.p2, w, h, groundY, timeMs)

                // Coiled vine snare overlay when caught in Jardin's Ulti
                if (world.p1.vineSnaredMs > 0L) drawVineSnare(world.p1, w, h, groundY, timeMs)
                if (world.p2.vineSnaredMs > 0L) drawVineSnare(world.p2, w, h, groundY, timeMs)

                // Signature Special & Ulti effects OVER characters so giant fists & vines pop!
                world.slashEffects.forEach { fx ->
                    drawSpecialEffect(fx = fx, w = w, h = h, groundY = groundY, timeMs = timeMs)
                }

                // Impact shockwave rings + anime starburst sparks
                world.sparks.forEach { spark ->
                    val p = (spark.ageMs.toFloat() / spark.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                    val sx = spark.x * w
                    val sy = groundY - spark.y * (h * 0.64f)
                    val maxR = if (spark.isUlti) h * 0.28f else h * 0.14f
                    val count = if (spark.isUlti) 14 else 8

                    // Expanding shockwave ring
                    drawCircle(
                        color = spark.color.copy(alpha = (1f - p) * 0.78f),
                        radius = maxR * (0.25f + p * 0.95f),
                        center = Offset(sx, sy),
                        style = Stroke(width = (1f - p) * (if (spark.isUlti) 10f else 5.5f))
                    )
                    // Bright white core flash
                    drawCircle(
                        color = Color.White.copy(alpha = (1f - p) * 0.92f),
                        radius = maxR * 0.28f * (1f - p),
                        center = Offset(sx, sy)
                    )
                    // Jagged comic rays
                    for (i in 0 until count) {
                        val angle = (i.toFloat() / count) * (2f * PI.toFloat()) + p * 0.45f
                        val dist = maxR * (0.3f + p * 0.85f)
                        drawLine(
                            color = spark.color.copy(alpha = 1f - p),
                            start = Offset(sx + cos(angle) * dist * 0.22f, sy + sin(angle) * dist * 0.22f),
                            end = Offset(sx + cos(angle) * dist, sy + sin(angle) * dist),
                            strokeWidth = if (spark.isUlti) 7.5f else 4.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Outlined floating combat popups
                world.popups.forEach { popup ->
                    val alpha = (1f - popup.ageMs.toFloat() / popup.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                    drawPopupText(
                        textMeasurer = textMeasurer,
                        text = popup.text,
                        x = popup.x * w,
                        y = (groundY - popup.y * (h * 0.64f) - h * 0.27f).coerceAtLeast(16f),
                        color = popup.color.copy(alpha = alpha),
                        isBig = popup.isCritical,
                        alpha = alpha
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAnimeSpeedLines(
    center: Offset,
    w: Float,
    h: Float,
    color: Color,
    timeMs: Long
) {
    val maxDist = hypot(w, h)
    val lineCount = 22
    val phase = (timeMs * 0.004f)
    for (i in 0 until lineCount) {
        val angle = (i.toFloat() / lineCount) * (2f * PI.toFloat()) + sin(phase + i) * 0.03f
        val innerR = h * (0.28f + (i % 3) * 0.08f)
        drawLine(
            color = color.copy(alpha = 0.26f),
            start = Offset(center.x + cos(angle) * innerR, center.y + sin(angle) * innerR),
            end = Offset(center.x + cos(angle) * maxDist, center.y + sin(angle) * maxDist),
            strokeWidth = if (i % 2 == 0) 4f else 2f
        )
    }
}

private fun DrawScope.drawSpecialEffect(
    fx: SlashEffect,
    w: Float,
    h: Float,
    groundY: Float,
    timeMs: Long
) {
    val progress = (fx.ageMs.toFloat() / fx.maxAgeMs.toFloat()).coerceIn(0f, 1f)
    val fade = (1f - progress).coerceIn(0f, 1f)
    val fxX = fx.x * w
    val fxY = groundY - fx.y * (h * 0.64f) - h * 0.15f
    val reachPx = fx.range * w

    when (fx.effectType) {
        // GLITCHERMAN: Bayonetta-style Cyber Portal + Sculpted Giant Armored Holo-Fist!
        MoveEffectType.BAYONETTA_FIST, MoveEffectType.ULTI_GLITCH_BARRAGE -> {
            val isUlti = fx.effectType == MoveEffectType.ULTI_GLITCH_BARRAGE
            val portalX = fxX + fx.facing * w * 0.055f
            val portalR = h * (if (isUlti) 0.17f else 0.135f)
            drawBayonettaPortalAndFist(
                portalCenter = Offset(portalX, fxY),
                facing = fx.facing.toFloat(),
                portalR = portalR,
                reachPx = reachPx,
                progress = progress,
                fade = fade,
                primary = fx.color,
                accent = fx.accentColor,
                isBig = isUlti,
                timeMs = timeMs
            )
            if (isUlti) {
                drawBayonettaPortalAndFist(
                    portalCenter = Offset(portalX + fx.facing * w * 0.04f, fxY - h * 0.11f),
                    facing = fx.facing.toFloat(),
                    portalR = portalR * 0.78f,
                    reachPx = reachPx * 0.92f,
                    progress = ((progress - 0.12f) * 1.15f).coerceIn(0f, 1f),
                    fade = fade,
                    primary = fx.accentColor,
                    accent = fx.color,
                    isBig = false,
                    timeMs = timeMs
                )
            }
        }

        // JARDIN: Cracked glowing earth + thick erupting thorny vines with leaves & spores!
        MoveEffectType.VINE_WHIP, MoveEffectType.ULTI_VINE_PUNCH -> {
            val isUlti = fx.effectType == MoveEffectType.ULTI_VINE_PUNCH
            val vineReach = reachPx * (0.30f + (progress * 1.9f).coerceAtMost(0.72f))

            // Glowing earth fissure line in front of Jardin
            drawLine(
                color = Color(0xFFFFC400).copy(alpha = fade * 0.85f),
                start = Offset(fxX + fx.facing * 10f, groundY),
                end = Offset(fxX + fx.facing * vineReach, groundY),
                strokeWidth = if (isUlti) 8f else 5f,
                cap = StrokeCap.Round
            )

            val spikes = if (isUlti) 7 else 5
            for (s in 1..spikes) {
                val frac = s / spikes.toFloat()
                val baseX = fxX + fx.facing * (vineReach * frac)
                val vineH = h * (if (isUlti) 0.28f else 0.18f) * sin(frac * PI).toFloat().coerceAtLeast(0.35f)
                val sway = sin(timeMs * 0.018f + s) * 10f
                val tipX = baseX + fx.facing * 18f + sway * 0.4f
                val tipY = groundY - vineH

                val vineBody = Path().apply {
                    moveTo(baseX - 13f, groundY)
                    quadraticTo(baseX + sway - 8f, groundY - vineH * 0.55f, tipX, tipY)
                    quadraticTo(baseX + sway + 14f, groundY - vineH * 0.45f, baseX + 13f, groundY)
                    close()
                }
                drawPath(
                    path = vineBody,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF69F0AE).copy(alpha = fade), Color(0xFF1B5E20).copy(alpha = fade)),
                        startY = tipY,
                        endY = groundY
                    )
                )
                drawPath(vineBody, color = Color(0xFF072610).copy(alpha = fade), style = Stroke(width = 3f, join = StrokeJoin.Round))

                // Golden bud / leaf flare at vine tip
                drawCircle(
                    color = Color(0xFFFFC400).copy(alpha = fade * 0.9f),
                    radius = if (isUlti) 6.5f else 4.5f,
                    center = Offset(tipX, tipY)
                )
            }
        }

        // DARON BRÉSILIEN: Sweeping low ground crescent & kicked-up dust clouds (Balayette!)
        MoveEffectType.SWEEP_KICK, MoveEffectType.ULTI_TROPICAL_SLAM -> {
            val sweepW = reachPx * (0.45f + progress * 0.55f)
            val arcTop = groundY - h * 0.13f
            drawArc(
                brush = Brush.horizontalGradient(listOf(fx.accentColor.copy(alpha = fade), fx.color.copy(alpha = fade))),
                startAngle = if (fx.facing > 0) -30f else 150f,
                sweepAngle = 62f,
                useCenter = false,
                topLeft = Offset(fxX - sweepW * 0.18f, arcTop),
                size = Size(sweepW, h * 0.16f),
                style = Stroke(width = 18f * fade + 5f, cap = StrokeCap.Round)
            )
            // Floor dust puffs
            for (d in 1..4) {
                val dx = fxX + fx.facing * sweepW * (d * 0.22f)
                val dr = (10f + d * 4f) * (0.5f + progress * 0.7f)
                drawCircle(
                    color = Color(0xFFE0D2B4).copy(alpha = fade * 0.45f),
                    radius = dr,
                    center = Offset(dx, groundY - dr * 0.4f)
                )
            }
        }

        // MAÎTRE CHEN: Crisp golden-jade crescent slash & rapid chopstick needles
        MoveEffectType.CHOPSTICK_FLURRY, MoveEffectType.ULTI_CHOPSTICKS -> {
            val count = if (fx.effectType == MoveEffectType.ULTI_CHOPSTICKS) 8 else 5
            for (c in 0 until count) {
                val offsetY = (c - count / 2f) * (h * 0.032f)
                val len = reachPx * (0.60f + (c % 3) * 0.18f)
                val startPt = Offset(fxX + fx.facing * 14f, fxY + offsetY)
                val endPt = Offset(fxX + fx.facing * len, fxY + offsetY * 0.65f)
                drawLine(
                    color = Color(0xFF070910).copy(alpha = fade * 0.7f),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 9f * fade + 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = fade),
                            if (c % 2 == 0) fx.accentColor.copy(alpha = fade) else Color(0xFF00E676).copy(alpha = fade)
                        )
                    ),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 5.5f * fade + 1.8f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Classic anime crescent slash
        MoveEffectType.MELEE -> {
            val arcOffset = fx.facing * reachPx * 0.56f
            drawArc(
                color = Color.White.copy(alpha = fade * 0.92f),
                startAngle = if (fx.facing > 0) -55f else 125f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(fxX + arcOffset - h * 0.11f, fxY - h * 0.11f),
                size = Size(h * 0.22f, h * 0.22f),
                style = Stroke(width = 14f * fade + 3f, cap = StrokeCap.Round)
            )
            drawArc(
                color = fx.color.copy(alpha = fade * 0.95f),
                startAngle = if (fx.facing > 0) -60f else 120f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(fxX + arcOffset - h * 0.115f, fxY - h * 0.115f),
                size = Size(h * 0.23f, h * 0.23f),
                style = Stroke(width = 8f * fade + 2f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawBayonettaPortalAndFist(
    portalCenter: Offset,
    facing: Float,
    portalR: Float,
    reachPx: Float,
    progress: Float,
    fade: Float,
    primary: Color,
    accent: Color,
    isBig: Boolean,
    timeMs: Long
) {
    // 1. Cyber-Runic Portal Rings
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = fade * 0.55f), Color.Transparent),
            center = portalCenter,
            radius = portalR * 1.25f
        ),
        topLeft = Offset(portalCenter.x - portalR * 0.55f, portalCenter.y - portalR),
        size = Size(portalR * 1.1f, portalR * 2f)
    )
    drawOval(
        color = primary.copy(alpha = fade),
        topLeft = Offset(portalCenter.x - portalR * 0.42f, portalCenter.y - portalR),
        size = Size(portalR * 0.84f, portalR * 2f),
        style = Stroke(width = 6f)
    )
    drawOval(
        color = accent.copy(alpha = fade * 0.9f),
        topLeft = Offset(portalCenter.x - portalR * 0.28f, portalCenter.y - portalR * 0.72f),
        size = Size(portalR * 0.56f, portalR * 1.44f),
        style = Stroke(width = 3.5f)
    )

    // 2. Colossal Articulated Holo-Forearm & Clenched Armored Fist emerging from portal
    val fistTravel = (progress * 1.65f).coerceIn(0f, 1f)
    val fistCenterX = portalCenter.x + facing * (reachPx * 0.84f * fistTravel)
    val fistH = portalR * (if (isBig) 1.55f else 1.28f)
    val fistW = fistH * 1.22f

    // Forearm beam connecting portal to fist cuff
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(accent.copy(alpha = fade * 0.7f), primary.copy(alpha = fade * 0.9f))
        ),
        topLeft = Offset(
            x = if (facing > 0) portalCenter.x else fistCenterX,
            y = portalCenter.y - fistH * 0.30f
        ),
        size = Size(kotlin.math.abs(fistCenterX - portalCenter.x).coerceAtLeast(8f), fistH * 0.60f)
    )

    // Fist main block
    val fistLeft = fistCenterX - fistW * 0.5f
    val fistTop = portalCenter.y - fistH * 0.45f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(primary.copy(alpha = fade * 0.95f), accent.copy(alpha = fade * 0.92f))
        ),
        topLeft = Offset(fistLeft, fistTop),
        size = Size(fistW, fistH * 0.90f),
        cornerRadius = CornerRadius(20f, 20f)
    )
    drawRoundRect(
        color = Color(0xFF070910).copy(alpha = fade),
        topLeft = Offset(fistLeft, fistTop),
        size = Size(fistW, fistH * 0.90f),
        cornerRadius = CornerRadius(20f, 20f),
        style = Stroke(width = 4.5f)
    )

    // 4 Articulated Knuckle Blocks at the impact front!
    val knuckleX = fistCenterX + facing * (fistW * 0.38f)
    for (k in 0..3) {
        val ky = fistTop + fistH * (0.14f + k * 0.20f)
        drawRoundRect(
            color = Color.White.copy(alpha = fade * 0.92f),
            topLeft = Offset(knuckleX - fistW * 0.14f, ky - fistH * 0.08f),
            size = Size(fistW * 0.28f, fistH * 0.16f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = Color(0xFF070910).copy(alpha = fade),
            topLeft = Offset(knuckleX - fistW * 0.14f, ky - fistH * 0.08f),
            size = Size(fistW * 0.28f, fistH * 0.16f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 2.5f)
        )
    }

    // Supersonic air shockwave cone ahead of fist
    val coneX = fistCenterX + facing * fistW * 0.56f
    val conePath = Path().apply {
        moveTo(coneX + facing * fistW * 0.26f, portalCenter.y)
        lineTo(coneX - facing * fistW * 0.12f, portalCenter.y - fistH * 0.58f)
        lineTo(coneX, portalCenter.y)
        lineTo(coneX - facing * fistW * 0.12f, portalCenter.y + fistH * 0.58f)
        close()
    }
    drawPath(conePath, color = Color.White.copy(alpha = fade * 0.72f))
}

private fun DrawScope.drawPopupText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    isBig: Boolean,
    alpha: Float
) {
    val shadowStyle = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Bold,
        fontSize = if (isBig) 19.sp else 14.sp,
        color = Color(0xFF05070D).copy(alpha = alpha * 0.95f)
    )
    val style = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Bold,
        fontSize = if (isBig) 19.sp else 14.sp,
        color = color
    )
    val result = textMeasurer.measure(text = text, style = style)
    val shadowResult = textMeasurer.measure(text = text, style = shadowStyle)
    val left = (x - result.size.width / 2f).coerceIn(8f, size.width - result.size.width - 8f)
    drawText(textLayoutResult = shadowResult, topLeft = Offset(left + 2.5f, y + 2.5f))
    drawText(textLayoutResult = result, topLeft = Offset(left, y))
}

private fun hypot(a: Float, b: Float): Float = kotlin.math.hypot(a.toDouble(), b.toDouble()).toFloat()
