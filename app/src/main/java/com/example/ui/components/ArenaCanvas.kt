package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.game.data.MoveEffectType
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.CombatAction
import com.example.game.engine.MatchPhase
import com.example.game.engine.Projectile
import com.example.game.engine.ProjectileStyle
import com.example.game.engine.SlashEffect
import com.example.ui.theme.RussoOneFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArenaCanvas(
    world: ArenaWorldState,
    timeMs: Long,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Dynamic Guilty Gear-style camera zoom & impact coordinate tracking
    val separation = abs(world.p1.x - world.p2.x)
    val isHeavyMoment = world.ultiCinematic != null ||
        world.hitStopMs > 0L ||
        world.impactFlashMs > 0L ||
        world.p1.chargingMegaPunch ||
        world.p2.chargingMegaPunch
    val rawZoom = when {
        world.phase == MatchPhase.KO_SLOWMO -> if (world.koFreezeRemainingMs > 0L) 1.28f else 1.24f
        world.hitStopMs > 0L || world.impactFlashMs > 0L -> 1.19f
        isHeavyMoment -> 1.14f
        separation < 0.24f -> 1.12f
        separation < 0.42f -> 1.07f
        else -> 1.01f
    }
    val camZoom by animateFloatAsState(
        targetValue = rawZoom,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "arenaCamZoom"
    )
    val rawPivotX = when {
        world.phase == MatchPhase.KO_SLOWMO -> world.impactX.coerceIn(0.18f, 0.82f)
        world.impactFlashMs > 0L -> world.impactX
        else -> ((world.p1.x + world.p2.x) * 0.5f).coerceIn(0.18f, 0.82f)
    }
    val camPivotX by animateFloatAsState(
        targetValue = rawPivotX,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "arenaCamPivotX"
    )

    val stageColorFilter: ColorFilter? = when {
        world.phase == MatchPhase.KO_SLOWMO && world.koFreezeRemainingMs > (world.koFreezeTotalMs * 0.58f) -> {
            // Negative / Invert high-impact anime frame!
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1.15f, 0f, 0f, 0f, 255f,
                        0f, -1.15f, 0f, 0f, 255f,
                        0f, 0f, -1.15f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        world.phase == MatchPhase.KO_SLOWMO && world.koFreezeRemainingMs > 0L -> {
            // Stark high-contrast noir monochrome freeze
            val bw = ColorMatrix().apply { setToSaturation(0.06f) }
            ColorFilter.colorMatrix(bw)
        }
        world.phase == MatchPhase.KO_SLOWMO -> {
            // Dramatic cinematic desaturated slow-mo contrast
            val slow = ColorMatrix().apply { setToSaturation(0.32f) }
            ColorFilter.colorMatrix(slow)
        }
        else -> null
    }

    Box(modifier = modifier.clipToBounds()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = camZoom
                    scaleY = camZoom
                    transformOrigin = TransformOrigin(camPivotX, 0.68f)
                }
        ) {
            // HD 16:9 Anime Stage Backdrop
            Image(
                painter = painterResource(id = world.stage.backgroundRes),
                contentDescription = world.stage.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = stageColorFilter
            )

            // Volumetric anime rim lighting & atmospheric vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x6604060D),
                                Color(0x1405070E),
                                Color(0x3805070E),
                                Color(0xC204060C)
                            )
                        )
                    )
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val groundY = h * 0.84f

                // Screen shake offset on heavy impacts, Ulti & hit-stop
                val shakeBase = when {
                    world.screenShakeMs > 0L -> (world.screenShakeMs / 380f) * 18f
                    world.hitStopMs > 0L -> 7f
                    else -> 0f
                }
                val shakeX = if (shakeBase > 0f) sin(timeMs * 0.95f) * shakeBase else 0f
                val shakeY = if (shakeBase > 0f) cos(timeMs * 1.15f) * shakeBase * 0.68f else 0f

                translate(left = shakeX, top = shakeY) {
                    // Overhead dramatic spotlights behind both fighters
                    listOf(world.p1, world.p2).forEach { fighter ->
                        val fx = fighter.x * w
                        val conePath = Path().apply {
                            moveTo(fx - w * 0.045f, 0f)
                            lineTo(fx + w * 0.045f, 0f)
                            lineTo(fx + w * 0.17f, groundY)
                            lineTo(fx - w * 0.17f, groundY)
                            close()
                        }
                        drawPath(
                            path = conePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    fighter.def.primaryColor.copy(alpha = 0.11f),
                                    world.stage.accentGlow.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = groundY
                            )
                        )
                    }

                    // Horizon atmospheric bloom
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, world.stage.accentGlow.copy(alpha = 0.22f)),
                            startY = groundY - h * 0.24f,
                            endY = groundY
                        ),
                        topLeft = Offset(0f, groundY - h * 0.24f),
                        size = Size(w, h * 0.24f)
                    )

                    // Glossy anime perspective combat floor
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                world.stage.floorColor.copy(alpha = 0.93f),
                                Color(0xFF04060C)
                            ),
                            startY = groundY,
                            endY = h
                        ),
                        topLeft = Offset(0f, groundY),
                        size = Size(w, h - groundY)
                    )

                    // Perspective grid lines + horizontal depth lines
                    for (i in 0..12) {
                        val topX = w * (i / 12f)
                        val bottomX = w * 0.5f + (topX - w * 0.5f) * 1.70f
                        drawLine(
                            color = world.stage.accentGlow.copy(alpha = 0.26f),
                            start = Offset(topX, groundY),
                            end = Offset(bottomX, h),
                            strokeWidth = 2f
                        )
                    }
                    for (row in 1..3) {
                        val frac = row / 4f
                        val ry = groundY + (h - groundY) * (frac * frac)
                        drawLine(
                            color = world.stage.accentGlow.copy(alpha = 0.18f),
                            start = Offset(0f, ry),
                            end = Offset(w, ry),
                            strokeWidth = 1.6f
                        )
                    }

                    // Crisp double horizon rim line
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                world.stage.accentGlow.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.90f),
                                world.stage.accentGlow.copy(alpha = 0.45f)
                            )
                        ),
                        start = Offset(0f, groundY),
                        end = Offset(w, groundY),
                        strokeWidth = 3.6f
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(
                                world.stage.accentGlow.copy(alpha = 0.28f),
                                world.stage.accentGlow,
                                world.stage.accentGlow.copy(alpha = 0.28f)
                            )
                        ),
                        start = Offset(0f, groundY + 4f),
                        end = Offset(w, groundY + 4f),
                        strokeWidth = 5.2f
                    )

                    // Ambient stage particles (sakura petals or neon embers)
                    val isDojo = world.stage.id == "sunset_dojo"
                    for (pIdx in 0..17) {
                        val t = ((timeMs * 0.00016f) + pIdx * 0.11f) % 1f
                        val px = ((pIdx * 0.062f + sin(timeMs * 0.0012f + pIdx) * 0.032f) * w)
                        val py = if (isDojo) t * groundY else (1f - t) * groundY
                        val alpha = if (pIdx % 3 == 0) 0.78f else 0.46f
                        drawCircle(
                            color = if (isDojo) Color(0xFFFFB2C8).copy(alpha = alpha) else world.stage.accentGlow.copy(alpha = alpha),
                            radius = if (isDojo) 4.0f + (pIdx % 3) else 2.8f + (pIdx % 2) * 1.4f,
                            center = Offset(px, py)
                        )
                    }

                    // Dramatic Ulti or KO slow-mo darkening + anime speed-lines
                    val ultiActive = world.ultiFlashMs > 0L ||
                        world.phase == MatchPhase.KO_SLOWMO ||
                        world.p1.action == CombatAction.ULTI ||
                        world.p2.action == CombatAction.ULTI ||
                        world.p1.chargingMegaPunch ||
                        world.p2.chargingMegaPunch
                    if (ultiActive) {
                        val flashAlpha = if (world.ultiFlashMs > 0L) (world.ultiFlashMs / 360f).coerceIn(0f, 1f) * 0.34f else 0.22f
                        drawRect(color = Color(0xFF03040A).copy(alpha = if (world.phase == MatchPhase.KO_SLOWMO) 0.54f else 0.46f))
                        drawRect(color = world.ultiFlashColor.copy(alpha = flashAlpha))
                        val focusFighter = when {
                            world.p1.action == CombatAction.ULTI || world.p1.chargingMegaPunch -> world.p1
                            world.p2.action == CombatAction.ULTI || world.p2.chargingMegaPunch -> world.p2
                            world.phase == MatchPhase.KO_SLOWMO && world.winnerSide == 2 -> world.p1
                            world.phase == MatchPhase.KO_SLOWMO -> world.p2
                            else -> world.p1
                        }
                        drawAnimeSpeedLines(
                            center = Offset(focusFighter.x * w, groundY - focusFighter.y * h * 0.6f - h * 0.18f),
                            w = w,
                            h = h,
                            color = if (world.phase == MatchPhase.KO_SLOWMO) Color(0xFFFFD600) else focusFighter.def.primaryColor,
                            timeMs = timeMs
                        )
                    }

                    // Guilty Gear High-Contrast Anime Impact Frame behind/around impact
                    if (world.impactFlashMs > 0L) {
                        val impactFrac = (world.impactFlashMs / 160f).coerceIn(0f, 1f)
                        val impactCenter = Offset(world.impactX * w, groundY - h * 0.22f)
                        drawRect(
                            color = if (world.impactFlashMs > 85L) {
                                Color.White.copy(alpha = 0.26f * impactFrac)
                            } else {
                                Color(0xFF04060D).copy(alpha = 0.36f * impactFrac)
                            }
                        )
                        drawAnimeImpactStarburst(
                            center = impactCenter,
                            w = w,
                            h = h,
                            impactFrac = impactFrac,
                            accent = world.ultiFlashColor
                        )
                    }

                    // Fighter floor glow reflections
                    drawFighterReflection(world.p1, w, h, groundY)
                    drawFighterReflection(world.p2, w, h, groundY)

                    // Ultra-detailed cel-shaded Guilty Gear anime fighters
                    val hitStopActive = world.hitStopMs > 0L
                    drawDetailedFighter(world.p1, w, h, groundY, timeMs, hitStopActive = hitStopActive)
                    drawDetailedFighter(world.p2, w, h, groundY, timeMs, hitStopActive = hitStopActive)

                    // Coiled vine snare overlay when caught in Jardin's Ulti
                    if (world.p1.vineSnaredMs > 0L) drawVineSnare(world.p1, w, h, groundY, timeMs)
                    if (world.p2.vineSnaredMs > 0L) drawVineSnare(world.p2, w, h, groundY, timeMs)

                    // Signature Special & Ulti textured VFX OVER characters
                    world.slashEffects.forEach { fx ->
                        drawSpecialEffect(fx = fx, w = w, h = h, groundY = groundY, timeMs = timeMs)
                    }

                    // Active Combo Projectiles (Cyber Shards, Spinning Chopsticks, Coconut Bombs)
                    world.projectiles.forEach { proj ->
                        drawProjectile(proj = proj, w = w, h = h, groundY = groundY, timeMs = timeMs)
                    }

                    // Impact shockwave rings + crisp starburst sparks
                    world.sparks.forEach { spark ->
                        val p = (spark.ageMs.toFloat() / spark.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                        val sx = spark.x * w
                        val sy = groundY - spark.y * (h * 0.64f)
                        val maxR = if (spark.isUlti) h * 0.32f else h * 0.165f
                        val count = if (spark.isUlti) 16 else 10

                        // Outer dark ink shock ring + bright core ring
                        drawCircle(
                            color = Color(0xFF06080E).copy(alpha = (1f - p) * 0.72f),
                            radius = maxR * (0.24f + p * 0.98f),
                            center = Offset(sx, sy),
                            style = Stroke(width = (1f - p) * (if (spark.isUlti) 15f else 8.5f))
                        )
                        drawCircle(
                            color = spark.color.copy(alpha = (1f - p) * 0.90f),
                            radius = maxR * (0.24f + p * 0.98f),
                            center = Offset(sx, sy),
                            style = Stroke(width = (1f - p) * (if (spark.isUlti) 9.5f else 5.2f))
                        )
                        // Inner diamond starburst core
                        drawCircle(
                            color = Color.White.copy(alpha = (1f - p) * 0.96f),
                            radius = maxR * 0.32f * (1f - p),
                            center = Offset(sx, sy)
                        )
                        for (i in 0 until count) {
                            val angle = (i.toFloat() / count) * (2f * PI.toFloat()) + p * 0.45f
                            val dist = maxR * (0.28f + p * 0.95f)
                            drawLine(
                                color = if (i % 2 == 0) Color.White.copy(alpha = 1f - p) else spark.color.copy(alpha = 1f - p),
                                start = Offset(sx + cos(angle) * dist * 0.20f, sy + sin(angle) * dist * 0.20f),
                                end = Offset(sx + cos(angle) * dist, sy + sin(angle) * dist),
                                strokeWidth = if (spark.isUlti) 8f else 4.8f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Fatal Blow Freeze-Frame & Slow-Mo Particles (shards & embers)
                    if (world.fatalParticles.isNotEmpty()) {
                        world.fatalParticles.forEach { fp ->
                            val frac = (fp.ageMs.toFloat() / fp.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                            val alpha = (1f - frac * 0.85f).coerceIn(0f, 1f)
                            val px = fp.x * w
                            val py = groundY - fp.y * (h * 0.64f)
                            if (fp.isShard) {
                                rotate(degrees = fp.rotationDeg, pivot = Offset(px, py)) {
                                    val s = fp.size * (1f - frac * 0.25f)
                                    val shardPath = Path().apply {
                                        moveTo(px, py - s * 1.15f)
                                        lineTo(px + s * 0.72f, py)
                                        lineTo(px, py + s * 0.85f)
                                        lineTo(px - s * 0.65f, py + s * 0.12f)
                                        close()
                                    }
                                    drawPath(shardPath, color = fp.color.copy(alpha = alpha))
                                    drawPath(
                                        shardPath,
                                        color = Color(0xFF05070D).copy(alpha = alpha),
                                        style = Stroke(width = 2.2f)
                                    )
                                }
                            } else {
                                drawCircle(
                                    color = fp.color.copy(alpha = alpha * 0.42f),
                                    radius = fp.size * 1.9f,
                                    center = Offset(px, py)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = alpha * 0.92f),
                                    radius = fp.size * 0.72f,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }

                    // KO Freeze-Frame High-Contrast Vignette + Chromatic Split Screen Filter
                    if (world.phase == MatchPhase.KO_SLOWMO) {
                        val inFreeze = world.koFreezeRemainingMs > 0L
                        val center = Offset(world.impactX.coerceIn(0.15f, 0.85f) * w, groundY - h * 0.20f)
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF04060D).copy(alpha = if (inFreeze) 0.72f else 0.56f)
                                ),
                                center = center,
                                radius = w * (if (inFreeze) 0.58f else 0.72f)
                            )
                        )
                        if (inFreeze) {
                            // Chromatic RGB scan-tear bars on impact freeze-frame
                            for (bar in 0..7) {
                                val by = ((bar * 0.13f + 0.05f) % 1f) * h
                                drawRect(
                                    color = (if (bar % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF2A85)).copy(alpha = 0.26f),
                                    topLeft = Offset(0f, by),
                                    size = Size(w, 4.5f)
                                )
                            }
                        }
                    }

                    // Dynamic anime comic callout banner ("SLASH !", "COLOSSAL !", "ULTIME !")
                    if (world.impactFlashMs > 0L && !world.impactCallout.isNullOrBlank()) {
                        val impactFrac = (world.impactFlashMs / 160f).coerceIn(0f, 1f)
                        drawImpactCalloutBanner(
                            textMeasurer = textMeasurer,
                            callout = world.impactCallout,
                            center = Offset(world.impactX * w, groundY - h * 0.44f),
                            alpha = impactFrac.coerceIn(0.35f, 1f),
                            accent = world.ultiFlashColor
                        )
                    }

                    // Outlined floating damage numbers above fighters
                    world.popups.forEach { popup ->
                        val progress = (popup.ageMs.toFloat() / popup.maxAgeMs.toFloat()).coerceIn(0f, 1f)
                        val alpha = if (progress > 0.70f) ((1f - progress) / 0.30f).coerceIn(0f, 1f) else 1f
                        val popCurve = if (progress < 0.18f) {
                            val t = progress / 0.18f
                            0.72f + 0.68f * sin(t * (PI.toFloat() * 0.65f))
                        } else {
                            1.0f
                        }
                        drawPopupText(
                            textMeasurer = textMeasurer,
                            text = popup.text,
                            subLabel = popup.subLabel,
                            x = popup.x * w,
                            y = (groundY - popup.y * (h * 0.64f) - h * 0.24f).coerceIn(22f, groundY - 24f),
                            color = popup.color.copy(alpha = alpha),
                            scaleMultiplier = popup.scaleMultiplier * popCurve,
                            isCritical = popup.isCritical,
                            alpha = alpha
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawAnimeImpactStarburst(
    center: Offset,
    w: Float,
    h: Float,
    impactFrac: Float,
    accent: Color
) {
    val maxDist = hypot(w, h) * 0.65f
    val rays = 20
    for (i in 0 until rays) {
        val ang = (i.toFloat() / rays) * (2f * PI.toFloat())
        val inner = h * (0.06f + (i % 3) * 0.03f)
        val outer = inner + maxDist * (0.45f + (i % 2) * 0.35f) * (1.15f - impactFrac * 0.35f)
        val perp = ang + PI.toFloat() * 0.5f
        val halfThick = if (i % 2 == 0) 6.5f * impactFrac else 3.2f * impactFrac
        val rayPath = Path().apply {
            moveTo(center.x + cos(ang) * inner + cos(perp) * halfThick, center.y + sin(ang) * inner + sin(perp) * halfThick)
            lineTo(center.x + cos(ang) * outer, center.y + sin(ang) * outer)
            lineTo(center.x + cos(ang) * inner - cos(perp) * halfThick, center.y + sin(ang) * inner - sin(perp) * halfThick)
            close()
        }
        drawPath(
            path = rayPath,
            color = when {
                i % 3 == 0 -> Color(0xFF04060D).copy(alpha = 0.78f * impactFrac)
                i % 2 == 0 -> Color.White.copy(alpha = 0.85f * impactFrac)
                else -> accent.copy(alpha = 0.72f * impactFrac)
            }
        )
    }
}

private fun DrawScope.drawImpactCalloutBanner(
    textMeasurer: TextMeasurer,
    callout: String,
    center: Offset,
    alpha: Float,
    accent: Color
) {
    val safeX = center.x.coerceIn(size.width * 0.18f, size.width * 0.82f)
    val safeY = center.y.coerceIn(size.height * 0.22f, size.height * 0.56f)
    val style = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 23.sp,
        color = Color.White.copy(alpha = alpha)
    )
    val inkStyle = style.copy(color = Color(0xFF04060D).copy(alpha = alpha))
    val textRes = textMeasurer.measure(callout, style)
    val inkRes = textMeasurer.measure(callout, inkStyle)

    val boxW = textRes.size.width + 38f
    val boxH = textRes.size.height + 14f
    val left = safeX - boxW * 0.5f
    val top = safeY - boxH * 0.5f
    val skew = 14f

    rotate(degrees = -7f, pivot = Offset(safeX, safeY)) {
        val banner = Path().apply {
            moveTo(left + skew, top)
            lineTo(left + boxW + skew, top)
            lineTo(left + boxW - skew, top + boxH)
            lineTo(left - skew, top + boxH)
            close()
        }
        drawPath(banner, color = accent.copy(alpha = 0.92f * alpha))
        drawPath(banner, color = Color(0xFF04060D).copy(alpha = alpha), style = Stroke(width = 4f, join = StrokeJoin.Miter))

        val tx = safeX - textRes.size.width * 0.5f
        val ty = safeY - textRes.size.height * 0.5f
        drawText(inkRes, topLeft = Offset(tx + 2.5f, ty + 2.5f))
        drawText(inkRes, topLeft = Offset(tx - 1.5f, ty - 1.5f))
        drawText(textRes, topLeft = Offset(tx, ty))
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
    val lineCount = 26
    val phase = (timeMs * 0.0045f)
    for (i in 0 until lineCount) {
        val angle = (i.toFloat() / lineCount) * (2f * PI.toFloat()) + sin(phase + i) * 0.03f
        val innerR = h * (0.26f + (i % 3) * 0.07f)
        drawLine(
            color = if (i % 3 == 0) Color.White.copy(alpha = 0.24f) else color.copy(alpha = 0.30f),
            start = Offset(center.x + cos(angle) * innerR, center.y + sin(angle) * innerR),
            end = Offset(center.x + cos(angle) * maxDist, center.y + sin(angle) * maxDist),
            strokeWidth = if (i % 2 == 0) 4.4f else 2.2f
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
        // GLITCHERMAN BASE: 360° Onde EMP repoussante avec anneaux concentriques & éclairs
        MoveEffectType.EMP_PULSE -> {
            drawEmpPulseEffect(
                center = Offset(fxX, fxY),
                reachPx = reachPx,
                progress = progress,
                fade = fade,
                primary = fx.color,
                accent = fx.accentColor,
                timeMs = timeMs
            )
        }

        // GLITCHERMAN ULTI: Verrouillage satellite + Rayon Laser Orbital céleste !
        MoveEffectType.ULTI_ORBITAL_STRIKE -> {
            drawOrbitalStrikeEffect(
                targetX = fx.targetX * w,
                h = h,
                groundY = groundY,
                reachPx = reachPx,
                progress = progress,
                fade = fade,
                primary = fx.color,
                accent = fx.accentColor,
                timeMs = timeMs
            )
        }

        // JARDIN: Shattered Glowing Earth Fissure + Rock Debris + Bark-Textured Thorny Vines & Serrated Leaves
        MoveEffectType.VINE_WHIP, MoveEffectType.ULTI_VINE_PUNCH -> {
            val isUlti = fx.effectType == MoveEffectType.ULTI_VINE_PUNCH
            val vineReach = reachPx * (0.28f + (progress * 1.95f).coerceAtMost(0.76f))

            // Multi-layered glowing fissure crack on arena floor
            val fissurePath = Path().apply {
                moveTo(fxX + fx.facing * 8f, groundY)
                val segs = 7
                for (i in 1..segs) {
                    val px = fxX + fx.facing * (vineReach * (i / segs.toFloat()))
                    val py = groundY + (if (i % 2 == 0) 4.5f else -5.5f)
                    lineTo(px, py)
                }
            }
            drawPath(
                fissurePath,
                color = Color(0xFF06160B).copy(alpha = fade * 0.92f),
                style = Stroke(width = if (isUlti) 15f else 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawPath(
                fissurePath,
                color = Color(0xFFFFC400).copy(alpha = fade * 0.95f),
                style = Stroke(width = if (isUlti) 6.5f else 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Flying shattered rock chunks from ground eruption
            for (r in 0..5) {
                val rf = (r + 1) / 6f
                val rockX = fxX + fx.facing * (vineReach * rf * 0.92f) + sin(r * 2.1f) * 14f
                val rockArc = sin(progress * PI.toFloat()).coerceAtLeast(0.1f) * h * (0.065f + (r % 3) * 0.036f)
                val rockY = groundY - rockArc
                val rockSize = (if (isUlti) 10f else 6.8f) + (r % 3) * 2.5f
                drawRoundRect(
                    color = Color(0xFF4E342E).copy(alpha = fade),
                    topLeft = Offset(rockX - rockSize * 0.5f, rockY - rockSize * 0.5f),
                    size = Size(rockSize, rockSize * 0.85f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = Color(0xFF070910).copy(alpha = fade),
                    topLeft = Offset(rockX - rockSize * 0.5f, rockY - rockSize * 0.5f),
                    size = Size(rockSize, rockSize * 0.85f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 2.2f)
                )
            }

            // Thick textured thorny vines with inner bark striations, serrated leaves, thorns & golden spores
            val spikes = if (isUlti) 8 else 5
            for (s in 1..spikes) {
                val frac = s / spikes.toFloat()
                val baseX = fxX + fx.facing * (vineReach * frac)
                val vineH = h * (if (isUlti) 0.32f else 0.21f) * sin(frac * PI).toFloat().coerceAtLeast(0.35f)
                val sway = sin(timeMs * 0.018f + s * 1.1f) * 12f
                val tipX = baseX + fx.facing * 20f + sway * 0.45f
                val tipY = groundY - vineH

                val vineBody = Path().apply {
                    moveTo(baseX - 16f, groundY)
                    quadraticTo(baseX + sway - 10f, groundY - vineH * 0.56f, tipX, tipY)
                    quadraticTo(baseX + sway + 17f, groundY - vineH * 0.44f, baseX + 16f, groundY)
                    close()
                }
                drawPath(
                    path = vineBody,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF69F0AE).copy(alpha = fade),
                            Color(0xFF00C853).copy(alpha = fade),
                            Color(0xFF143D1B).copy(alpha = fade)
                        ),
                        startY = tipY,
                        endY = groundY
                    )
                )
                drawPath(vineBody, color = Color(0xFF061E0D).copy(alpha = fade), style = Stroke(width = 3.4f, join = StrokeJoin.Round))

                // Inner bark highlight ridge
                val ridgePath = Path().apply {
                    moveTo(baseX - 3f, groundY - 4f)
                    quadraticTo(baseX + sway * 0.6f, groundY - vineH * 0.54f, tipX, tipY + 6f)
                }
                drawPath(
                    ridgePath,
                    color = Color(0xFFB9F6CA).copy(alpha = fade * 0.60f),
                    style = Stroke(width = 2.6f, cap = StrokeCap.Round)
                )

                // Jagged thorns + serrated emerald leaf blades along stem
                for (tIdx in 1..2) {
                    val tf = tIdx * 0.36f
                    val tx = baseX + (tipX - baseX) * tf + sway * 0.25f
                    val ty = groundY - vineH * tf
                    val sideSign = if ((s + tIdx) % 2 == 0) 1f else -1f
                    val thorn = Path().apply {
                        moveTo(tx, ty - 4f)
                        lineTo(tx + sideSign * 15f, ty - 12f)
                        lineTo(tx + sideSign * 2f, ty + 5f)
                        close()
                    }
                    drawPath(thorn, color = Color(0xFFFFC400).copy(alpha = fade))
                    drawPath(thorn, color = Color(0xFF061E0D).copy(alpha = fade), style = Stroke(width = 1.8f))

                    val leaf = Path().apply {
                        moveTo(tx, ty + 6f)
                        quadraticTo(tx - sideSign * 14f, ty - 6f, tx - sideSign * 22f, ty + 4f)
                        quadraticTo(tx - sideSign * 12f, ty + 14f, tx, ty + 6f)
                        close()
                    }
                    drawPath(leaf, color = Color(0xFF00E676).copy(alpha = fade * 0.90f))
                    drawPath(leaf, color = Color(0xFF061E0D).copy(alpha = fade), style = Stroke(width = 1.6f))
                }

                // Golden glowing spore bud
                drawCircle(
                    color = Color(0xFFFFD600).copy(alpha = fade * 0.95f),
                    radius = if (isUlti) 7.5f else 5f,
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = fade * 0.75f),
                    radius = if (isUlti) 3.2f else 2.2f,
                    center = Offset(tipX, tipY)
                )
            }
        }

        // DARON BRÉSILIEN: Seismic Floor Ripples + Tropical Cyclone Sweep Ribbon + Cel-Shaded Dust Puffs
        MoveEffectType.SWEEP_KICK, MoveEffectType.ULTI_TROPICAL_SLAM -> {
            val isUlti = fx.effectType == MoveEffectType.ULTI_TROPICAL_SLAM
            val sweepW = reachPx * (0.44f + progress * 0.62f)
            val arcTop = groundY - h * (if (isUlti) 0.175f else 0.14f)
            val arcH = h * (if (isUlti) 0.21f else 0.165f)
            val arcLeft = if (fx.facing > 0) fxX - sweepW * 0.12f else fxX - sweepW * 0.88f

            // Concentric seismic floor shockwaves
            for (ring in 1..2) {
                val rw = sweepW * (0.55f + ring * 0.30f)
                val rh = h * (0.040f + ring * 0.018f)
                drawOval(
                    color = fx.color.copy(alpha = fade * (0.72f - ring * 0.22f)),
                    topLeft = Offset(fxX + fx.facing * sweepW * 0.35f - rw * 0.5f, groundY - rh * 0.45f),
                    size = Size(rw, rh),
                    style = Stroke(width = 3.6f * fade + 1.5f)
                )
            }

            // Outer ink & emerald cyclone band
            drawArc(
                color = Color(0xFF070910).copy(alpha = fade * 0.82f),
                startAngle = if (fx.facing > 0) -35f else 145f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = Offset(arcLeft, arcTop),
                size = Size(sweepW, arcH),
                style = Stroke(width = (if (isUlti) 32f else 23f) * fade + 6f, cap = StrokeCap.Round)
            )
            // Tropical gold-to-emerald sweep ribbon
            drawArc(
                brush = Brush.horizontalGradient(
                    listOf(fx.accentColor.copy(alpha = fade), fx.color.copy(alpha = fade), Color.White.copy(alpha = fade * 0.92f))
                ),
                startAngle = if (fx.facing > 0) -32f else 148f,
                sweepAngle = 66f,
                useCenter = false,
                topLeft = Offset(arcLeft, arcTop),
                size = Size(sweepW, arcH),
                style = Stroke(width = (if (isUlti) 23f else 16.5f) * fade + 4f, cap = StrokeCap.Round)
            )
            // Razor white core streak
            drawArc(
                color = Color.White.copy(alpha = fade * 0.94f),
                startAngle = if (fx.facing > 0) -24f else 156f,
                sweepAngle = 48f,
                useCenter = false,
                topLeft = Offset(arcLeft + sweepW * 0.04f, arcTop + 4f),
                size = Size(sweepW * 0.92f, arcH * 0.88f),
                style = Stroke(width = 5.2f * fade + 1.5f, cap = StrokeCap.Round)
            )

            // Cel-shaded anime dust puffs & flying pebbles
            for (d in 1..5) {
                val dx = fxX + fx.facing * sweepW * (d * 0.18f)
                val dr = (11f + d * 4.4f) * (0.45f + progress * 0.78f)
                drawCircle(
                    color = Color(0xFF070910).copy(alpha = fade * 0.55f),
                    radius = dr + 2f,
                    center = Offset(dx, groundY - dr * 0.42f)
                )
                drawCircle(
                    color = Color(0xFFD7CCC8).copy(alpha = fade * 0.78f),
                    radius = dr,
                    center = Offset(dx, groundY - dr * 0.42f)
                )
                drawCircle(
                    color = Color(0xFFFFF8E1).copy(alpha = fade * 0.55f),
                    radius = dr * 0.62f,
                    center = Offset(dx - fx.facing * dr * 0.2f, groundY - dr * 0.62f)
                )
                val pebbleY = groundY - dr * (1.25f + (d % 2) * 0.72f) * sin(progress * PI.toFloat()).coerceAtLeast(0.2f)
                drawCircle(
                    color = fx.color.copy(alpha = fade * 0.92f),
                    radius = 3.8f,
                    center = Offset(dx + fx.facing * 8f, pebbleY)
                )
            }
        }

        // MAÎTRE CHEN: Sumi-e Ink Brush Crescents, Ink Splatter & Engraved Jade/Gold Chopstick Needles
        MoveEffectType.CHOPSTICK_FLURRY, MoveEffectType.ULTI_CHOPSTICKS -> {
            val isUlti = fx.effectType == MoveEffectType.ULTI_CHOPSTICKS
            val count = if (isUlti) 9 else 5

            // Sweeping calligraphic sumi-e brush arc behind the needles
            val arcSpan = reachPx * 0.88f
            val arcTop = fxY - h * 0.145f
            val arcLeft = if (fx.facing > 0) fxX else fxX - arcSpan
            drawArc(
                color = Color(0xFF05070D).copy(alpha = fade * 0.85f),
                startAngle = if (fx.facing > 0) -46f else 134f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(arcLeft, arcTop),
                size = Size(arcSpan, h * 0.29f),
                style = Stroke(width = (if (isUlti) 24f else 16f) * fade + 4f, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF0A0C12).copy(alpha = fade * 0.75f), fx.color.copy(alpha = fade * 0.90f), Color(0xFFFFD600).copy(alpha = fade))
                ),
                startAngle = if (fx.facing > 0) -44f else 136f,
                sweepAngle = 88f,
                useCenter = false,
                topLeft = Offset(arcLeft, arcTop),
                size = Size(arcSpan, h * 0.29f),
                style = Stroke(width = (if (isUlti) 16f else 11f) * fade + 2.5f, cap = StrokeCap.Round)
            )

            // Sumi-e ink splatter droplets
            for (drop in 0..6) {
                val dx = fxX + fx.facing * reachPx * (0.28f + drop * 0.10f)
                val dy = fxY + sin(drop * 2.3f) * h * 0.13f
                drawCircle(
                    color = Color(0xFF05070D).copy(alpha = fade * 0.82f),
                    radius = (3f + (drop % 3) * 2f) * fade + 1f,
                    center = Offset(dx, dy)
                )
            }

            // Engraved tapered chopstick needles + glowing tips
            for (c in 0 until count) {
                val offsetY = (c - count / 2f) * (h * 0.030f)
                val len = reachPx * (0.60f + (c % 3) * 0.19f)
                val thrustProgress = (progress * 1.4f).coerceIn(0.25f, 1f)
                val startPt = Offset(fxX + fx.facing * (12f + len * 0.12f * progress), fxY + offsetY)
                val endPt = Offset(fxX + fx.facing * len * thrustProgress, fxY + offsetY * 0.56f)

                drawLine(
                    color = Color(0xFF070910).copy(alpha = fade * 0.84f),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 10.5f * fade + 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = fade),
                            if (c % 2 == 0) fx.accentColor.copy(alpha = fade) else Color(0xFF00E676).copy(alpha = fade),
                            fx.color.copy(alpha = fade)
                        )
                    ),
                    start = startPt,
                    end = endPt,
                    strokeWidth = 5.8f * fade + 1.8f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color.White.copy(alpha = fade * 0.96f),
                    radius = (if (isUlti) 5.8f else 4.2f) * fade + 1.5f,
                    center = endPt
                )
            }
        }

        // Classic Guilty Gear Multi-Band Motion-Smear Crescent & Directional Sparks
        MoveEffectType.MELEE -> {
            val arcOffset = fx.facing * reachPx * 0.54f
            val r = h * 0.125f
            val arcRectTopLeft = Offset(fxX + arcOffset - r, fxY - r)
            val arcRectSize = Size(r * 2f, r * 2f)
            val startAng = if (fx.facing > 0) -62f else 118f

            drawArc(
                color = Color(0xFF070910).copy(alpha = fade * 0.88f),
                startAngle = startAng,
                sweepAngle = 124f,
                useCenter = false,
                topLeft = arcRectTopLeft,
                size = arcRectSize,
                style = Stroke(width = 19f * fade + 4f, cap = StrokeCap.Round)
            )
            drawArc(
                color = fx.color.copy(alpha = fade * 0.96f),
                startAngle = startAng + 4f,
                sweepAngle = 116f,
                useCenter = false,
                topLeft = arcRectTopLeft,
                size = arcRectSize,
                style = Stroke(width = 12.5f * fade + 2.5f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White.copy(alpha = fade * 0.96f),
                startAngle = startAng + 12f,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = Offset(arcRectTopLeft.x + fx.facing * 3f, arcRectTopLeft.y + 3f),
                size = Size(arcRectSize.width * 0.94f, arcRectSize.height * 0.94f),
                style = Stroke(width = 4.8f * fade + 1.2f, cap = StrokeCap.Round)
            )
            for (fIdx in -1..1) {
                val fy = fxY + fIdx * r * 0.45f
                val fxStart = fxX + arcOffset + fx.facing * r * 0.65f
                drawLine(
                    color = fx.accentColor.copy(alpha = fade * 0.92f),
                    start = Offset(fxStart, fy),
                    end = Offset(fxStart + fx.facing * r * (0.45f + progress * 0.35f), fy + fIdx * 6f),
                    strokeWidth = 3.4f,
                    cap = StrokeCap.Round
                )
            }
        }

        // GLITCHERMAN COMBO 1 (← → + CAP): Blink Cyber warp trail + chromatic X-slash
        MoveEffectType.GLITCH_BLINK_STRIKE -> {
            val originX = fx.targetX * w
            val trailMidY = fxY
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(fx.accentColor.copy(alpha = fade * 0.25f), fx.color.copy(alpha = fade * 0.95f))
                ),
                start = Offset(originX, trailMidY),
                end = Offset(fxX, trailMidY),
                strokeWidth = (18f * fade + 3f),
                cap = StrokeCap.Round
            )
            for (bar in -2..2) {
                val by = trailMidY + bar * 12f
                drawLine(
                    color = (if (bar % 2 == 0) Color.White else fx.accentColor).copy(alpha = fade * 0.85f),
                    start = Offset(originX + (fxX - originX) * 0.2f, by),
                    end = Offset(fxX, by),
                    strokeWidth = 3.2f
                )
            }
            val slashSpan = h * 0.16f
            drawLine(
                color = Color(0xFF04060D).copy(alpha = fade * 0.9f),
                start = Offset(fxX - slashSpan, fxY - slashSpan),
                end = Offset(fxX + slashSpan, fxY + slashSpan),
                strokeWidth = 14f * fade + 4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = fx.color.copy(alpha = fade),
                start = Offset(fxX - slashSpan, fxY - slashSpan),
                end = Offset(fxX + slashSpan, fxY + slashSpan),
                strokeWidth = 7f * fade + 2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = fx.accentColor.copy(alpha = fade),
                start = Offset(fxX - slashSpan, fxY + slashSpan * 0.7f),
                end = Offset(fxX + slashSpan, fxY - slashSpan * 0.7f),
                strokeWidth = 7f * fade + 2f,
                cap = StrokeCap.Round
            )
        }

        // GLITCHERMAN COMBO 2 (→ ← + C): Cyber muzzle glyph pulse
        MoveEffectType.GLITCH_SHARD_BARRAGE -> {
            val muzzleX = fxX + fx.facing * 26f
            val r = h * (0.07f + progress * 0.09f)
            drawOval(
                color = fx.color.copy(alpha = fade * 0.9f),
                topLeft = Offset(muzzleX - r * 0.45f, fxY - r),
                size = Size(r * 0.9f, r * 2f),
                style = Stroke(width = 5f * fade + 1.5f)
            )
            drawCircle(
                color = fx.accentColor.copy(alpha = fade * 0.65f),
                radius = r * 0.55f,
                center = Offset(muzzleX, fxY)
            )
        }

        // JARDIN COMBO 1 (← → + CAP): Seismic emerald ground-slam pillars + rock burst
        MoveEffectType.EARTH_QUAKE_SLAM -> {
            val slamR = reachPx * (0.38f + progress * 0.65f)
            for (ring in 0..2) {
                val frac = ((progress * 1.3f) - ring * 0.14f).coerceIn(0.1f, 1f)
                val rx = reachPx * frac
                val ry = h * (0.045f + ring * 0.016f)
                drawOval(
                    color = (if (ring % 2 == 0) fx.color else fx.accentColor).copy(alpha = fade * 0.92f),
                    topLeft = Offset(fxX - rx, groundY - ry * 0.5f),
                    size = Size(rx * 2f, ry),
                    style = Stroke(width = 6.5f * fade + 2f)
                )
            }
            for (pillar in -2..2) {
                if (pillar == 0) continue
                val px = fxX + pillar * slamR * 0.34f
                val ph = h * (0.19f - abs(pillar) * 0.03f) * sin(progress * PI.toFloat()).coerceAtLeast(0.25f)
                val spire = Path().apply {
                    moveTo(px - 15f, groundY)
                    lineTo(px, groundY - ph)
                    lineTo(px + 15f, groundY)
                    close()
                }
                drawPath(spire, color = Color(0xFF00C853).copy(alpha = fade * 0.92f))
                drawPath(spire, color = Color(0xFF061E0D).copy(alpha = fade), style = Stroke(width = 3f))
            }
        }

        // JARDIN COMBO 2 (→ ← + C): Rising Golden Bark Uppercut Crescent
        MoveEffectType.THORN_UPPERCUT -> {
            val r = h * 0.18f
            val centerOffset = fx.facing * reachPx * 0.38f
            val rectTopLeft = Offset(fxX + centerOffset - r, fxY - r * 1.15f)
            val startAngle = if (fx.facing > 0) -95f else 175f
            drawArc(
                color = Color(0xFF061E0D).copy(alpha = fade * 0.9f),
                startAngle = startAngle,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = rectTopLeft,
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 22f * fade + 5f, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFD600).copy(alpha = fade), fx.color.copy(alpha = fade))),
                startAngle = startAngle,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = rectTopLeft,
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 13f * fade + 3f, cap = StrokeCap.Round)
            )
        }

        // MAÎTRE CHEN COMBO 1 (← → + CAP): Blazing Dragon Dash Kick Cone & Flame Streaks
        MoveEffectType.DRAGON_DASH_KICK -> {
            val coneLen = reachPx * 0.85f
            val conePath = Path().apply {
                moveTo(fxX + fx.facing * coneLen * 0.55f, fxY)
                lineTo(fxX - fx.facing * coneLen * 0.45f, fxY - h * 0.11f)
                lineTo(fxX - fx.facing * coneLen * 0.30f, fxY)
                lineTo(fxX - fx.facing * coneLen * 0.45f, fxY + h * 0.11f)
                close()
            }
            drawPath(
                path = conePath,
                brush = Brush.horizontalGradient(listOf(fx.accentColor.copy(alpha = fade * 0.88f), fx.color.copy(alpha = fade * 0.95f)))
            )
            for (s in -2..2) {
                val sy = fxY + s * h * 0.038f
                drawLine(
                    color = Color.White.copy(alpha = fade * 0.85f),
                    start = Offset(fxX - fx.facing * coneLen * 0.42f, sy),
                    end = Offset(fxX + fx.facing * coneLen * 0.38f, sy),
                    strokeWidth = 4f * fade + 1.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        // MAÎTRE CHEN COMBO 2 (→ ← + C): Sumi-e release fan
        MoveEffectType.CHOPSTICK_VOLLEY -> {
            val fanX = fxX + fx.facing * 24f
            val r = h * 0.10f
            drawArc(
                color = fx.accentColor.copy(alpha = fade * 0.9f),
                startAngle = if (fx.facing > 0) -48f else 132f,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = Offset(fanX - r, fxY - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 7f * fade + 2f, cap = StrokeCap.Round)
            )
        }

        // DARON BRÉSILIEN COMBO 1 (← → + CAP): Samba Meteor Shockwave Dome
        MoveEffectType.SAMBA_METEOR_DROP -> {
            val domeR = reachPx * (0.35f + progress * 0.72f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = fade * 0.75f), fx.color.copy(alpha = fade * 0.6f), Color.Transparent),
                    center = Offset(fxX, groundY - h * 0.06f),
                    radius = domeR
                ),
                radius = domeR,
                center = Offset(fxX, groundY - h * 0.06f)
            )
            drawCircle(
                color = fx.accentColor.copy(alpha = fade * 0.95f),
                radius = domeR * 0.88f,
                center = Offset(fxX, groundY - h * 0.06f),
                style = Stroke(width = 9f * fade + 2.5f)
            )
        }

        // DARON BRÉSILIEN COMBO 2 (→ ← + C): Coconut Toss Launch Burst
        MoveEffectType.COCONUT_BOMB_TOSS -> {
            val burstX = fxX + fx.facing * 26f
            val r = h * (0.06f + progress * 0.08f)
            drawCircle(
                color = fx.color.copy(alpha = fade * 0.85f),
                radius = r,
                center = Offset(burstX, fxY),
                style = Stroke(width = 5f * fade + 1.5f)
            )
        }
    }
}

private fun DrawScope.drawProjectile(
    proj: Projectile,
    w: Float,
    h: Float,
    groundY: Float,
    timeMs: Long
) {
    val px = proj.x * w
    val py = groundY - proj.y * (h * 0.64f) - h * 0.10f
    val dir = if (proj.vx >= 0f) 1f else -1f

    when (proj.style) {
        ProjectileStyle.CYBER_SHARD -> {
            val len = h * 0.085f
            val thick = h * 0.022f
            // Neon tail streak
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, proj.accentColor.copy(alpha = 0.85f), proj.color)
                ),
                start = Offset(px - dir * len * 1.45f, py),
                end = Offset(px + dir * len * 0.4f, py),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
            val shard = Path().apply {
                moveTo(px + dir * len * 0.72f, py)
                lineTo(px - dir * len * 0.2f, py - thick)
                lineTo(px - dir * len * 0.55f, py)
                lineTo(px - dir * len * 0.2f, py + thick)
                close()
            }
            drawPath(shard, color = proj.color)
            drawPath(shard, color = Color(0xFF04060D), style = Stroke(width = 2.6f))
            drawCircle(color = Color.White, radius = thick * 0.42f, center = Offset(px + dir * 4f, py))
        }

        ProjectileStyle.GOLDEN_CHOPSTICK -> {
            val len = h * 0.075f
            val spinDeg = (proj.ageMs * 0.95f) * dir
            rotate(degrees = spinDeg, pivot = Offset(px, py)) {
                drawLine(
                    color = Color(0xFF04060D),
                    start = Offset(px - len * 0.6f, py),
                    end = Offset(px + len * 0.6f, py),
                    strokeWidth = 8.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.horizontalGradient(listOf(proj.accentColor, proj.color, Color.White)),
                    start = Offset(px - len * 0.6f, py),
                    end = Offset(px + len * 0.6f, py),
                    strokeWidth = 4.8f,
                    cap = StrokeCap.Round
                )
            }
        }

        ProjectileStyle.COCONUT_BOMB -> {
            val r = h * 0.040f
            val spinDeg = (proj.ageMs * 0.65f) * dir
            // Flame trail
            drawCircle(
                color = Color(0xFFFF6D00).copy(alpha = 0.75f),
                radius = r * 1.25f,
                center = Offset(px - dir * r * 0.95f, py)
            )
            drawCircle(
                color = proj.color.copy(alpha = 0.85f),
                radius = r * 0.8f,
                center = Offset(px - dir * r * 1.45f, py)
            )
            rotate(degrees = spinDeg, pivot = Offset(px, py)) {
                drawCircle(color = Color(0xFF4E342E), radius = r, center = Offset(px, py))
                drawCircle(color = Color(0xFF04060D), radius = r, center = Offset(px, py), style = Stroke(width = 3.2f))
                drawArc(
                    color = proj.accentColor,
                    startAngle = -50f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(px - r * 0.72f, py - r * 0.72f),
                    size = Size(r * 1.44f, r * 1.44f),
                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                )
                // Glowing fuse spark
                val pulse = 0.8f + 0.35f * sin(timeMs * 0.03f)
                drawCircle(color = Color(0xFFFFD600), radius = r * 0.36f * pulse, center = Offset(px, py - r * 0.92f))
                drawCircle(color = Color.White, radius = r * 0.18f, center = Offset(px, py - r * 0.92f))
            }
        }
    }
}

private fun DrawScope.drawEmpPulseEffect(
    center: Offset,
    reachPx: Float,
    progress: Float,
    fade: Float,
    primary: Color,
    accent: Color,
    timeMs: Long
) {
    val maxR = reachPx * (0.35f + progress * 0.72f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = fade * 0.65f), primary.copy(alpha = fade * 0.42f), Color.Transparent),
            center = center,
            radius = maxR
        ),
        radius = maxR,
        center = center
    )

    for (ring in 0..2) {
        val rFrac = ((progress * 1.35f) - ring * 0.16f).coerceIn(0.05f, 1.0f)
        val r = reachPx * rFrac
        drawCircle(
            color = Color(0xFF05070D).copy(alpha = fade * 0.75f),
            radius = r,
            center = center,
            style = Stroke(width = (13f - ring * 3f) * fade + 2f)
        )
        drawCircle(
            color = (if (ring % 2 == 0) primary else accent).copy(alpha = fade * 0.94f),
            radius = r,
            center = center,
            style = Stroke(width = (7.5f - ring * 2f) * fade + 1.5f)
        )
    }

    for (bolt in 0 until 10) {
        val ang = (bolt / 10f) * (2f * PI.toFloat()) + timeMs * 0.008f
        val perp = ang + PI.toFloat() * 0.5f
        val boltPath = Path().apply {
            moveTo(center.x + cos(ang) * maxR * 0.15f, center.y + sin(ang) * maxR * 0.15f)
            for (seg in 1..4) {
                val frac = 0.15f + seg * 0.21f
                val jitter = (if ((seg + bolt) % 2 == 0) 1f else -1f) * 14f * fade
                lineTo(
                    center.x + cos(ang) * maxR * frac + cos(perp) * jitter,
                    center.y + sin(ang) * maxR * frac + sin(perp) * jitter
                )
            }
        }
        drawPath(
            boltPath,
            color = if (bolt % 2 == 0) Color.White.copy(alpha = fade * 0.95f) else accent.copy(alpha = fade * 0.9f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawOrbitalStrikeEffect(
    targetX: Float,
    h: Float,
    groundY: Float,
    reachPx: Float,
    progress: Float,
    fade: Float,
    primary: Color,
    accent: Color,
    timeMs: Long
) {
    val retW = reachPx * 0.64f
    val retH = h * 0.068f
    drawOval(
        color = accent.copy(alpha = fade * 0.92f),
        topLeft = Offset(targetX - retW * 0.5f, groundY - retH * 0.5f),
        size = Size(retW, retH),
        style = Stroke(width = 4.2f)
    )
    drawOval(
        color = primary.copy(alpha = fade * 0.85f),
        topLeft = Offset(targetX - retW * 0.30f, groundY - retH * 0.30f),
        size = Size(retW * 0.60f, retH * 0.60f),
        style = Stroke(width = 2.5f)
    )

    val beamDescend = (progress / 0.24f).coerceIn(0.18f, 1f)
    val beamBottom = groundY * beamDescend
    val widthPulse = 1f + 0.14f * sin(timeMs * 0.035f)
    val outerW = (reachPx * 0.36f * widthPulse) * (0.45f + fade * 0.55f)
    val midW = outerW * 0.62f
    val coreW = outerW * 0.26f

    drawRect(
        color = Color(0xFF04060D).copy(alpha = fade * 0.88f),
        topLeft = Offset(targetX - outerW * 0.5f - 4f, 0f),
        size = Size(outerW + 8f, beamBottom)
    )
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(accent.copy(alpha = fade * 0.88f), primary.copy(alpha = fade * 0.95f), accent.copy(alpha = fade * 0.88f)),
            startX = targetX - outerW * 0.5f,
            endX = targetX + outerW * 0.5f
        ),
        topLeft = Offset(targetX - outerW * 0.5f, 0f),
        size = Size(outerW, beamBottom)
    )
    drawRect(
        color = primary.copy(alpha = fade),
        topLeft = Offset(targetX - midW * 0.5f, 0f),
        size = Size(midW, beamBottom)
    )
    drawRect(
        color = Color.White.copy(alpha = fade * 0.98f),
        topLeft = Offset(targetX - coreW * 0.5f, 0f),
        size = Size(coreW, beamBottom)
    )

    for (ring in 0..3) {
        val ry = (((progress * 2.2f + ring * 0.25f) % 1f) * beamBottom)
        drawOval(
            color = Color.White.copy(alpha = fade * 0.9f),
            topLeft = Offset(targetX - outerW * 0.62f, ry - 6f),
            size = Size(outerW * 1.24f, 12f),
            style = Stroke(width = 3.2f)
        )
    }

    if (beamDescend >= 0.95f) {
        val craterR = reachPx * (0.28f + progress * 0.36f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = fade), primary.copy(alpha = fade * 0.78f), Color.Transparent),
                center = Offset(targetX, groundY),
                radius = craterR
            ),
            radius = craterR,
            center = Offset(targetX, groundY)
        )
        for (s in -3..3) {
            val sx = targetX + s * (outerW * 0.26f)
            val sy = groundY - h * (0.08f + (abs(s) % 3) * 0.05f) * progress
            drawLine(
                color = if (s % 2 == 0) accent.copy(alpha = fade) else Color.White.copy(alpha = fade),
                start = Offset(sx, groundY),
                end = Offset(sx + s * 10f, sy),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawPopupText(
    textMeasurer: TextMeasurer,
    text: String,
    subLabel: String?,
    x: Float,
    y: Float,
    color: Color,
    scaleMultiplier: Float,
    isCritical: Boolean,
    alpha: Float
) {
    val baseSp = (17f * scaleMultiplier).coerceIn(13f, 34f).sp
    val outlineColor = Color(0xFF04060C).copy(alpha = alpha * 0.96f)
    val outlineStyle = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = baseSp,
        color = outlineColor
    )
    val mainStyle = TextStyle(
        fontFamily = RussoOneFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = baseSp,
        color = color
    )

    val result = textMeasurer.measure(text = text, style = mainStyle)
    val outlineResult = textMeasurer.measure(text = text, style = outlineStyle)
    val left = (x - result.size.width / 2f).coerceIn(10f, (size.width - result.size.width - 10f).coerceAtLeast(10f))

    if (isCritical) {
        val center = Offset(left + result.size.width / 2f, y + result.size.height / 2f)
        val glowR = (result.size.width * 0.78f).coerceAtLeast(36f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha * 0.42f), Color.Transparent),
                center = center,
                radius = glowR
            ),
            radius = glowR,
            center = center
        )
    }

    if (!subLabel.isNullOrBlank()) {
        val labelSizeSp = (9.5f * scaleMultiplier.coerceAtMost(1.35f)).coerceIn(8.5f, 13f).sp
        val labelOutlineStyle = TextStyle(
            fontFamily = RussoOneFamily,
            fontWeight = FontWeight.Bold,
            fontSize = labelSizeSp,
            color = outlineColor
        )
        val labelStyle = TextStyle(
            fontFamily = RussoOneFamily,
            fontWeight = FontWeight.Bold,
            fontSize = labelSizeSp,
            color = Color.White.copy(alpha = alpha * 0.95f)
        )
        val labelRes = textMeasurer.measure(text = subLabel, style = labelStyle)
        val labelOutRes = textMeasurer.measure(text = subLabel, style = labelOutlineStyle)
        val labelLeft = (x - labelRes.size.width / 2f).coerceIn(8f, (size.width - labelRes.size.width - 8f).coerceAtLeast(8f))
        val labelTop = (y - labelRes.size.height + 2f).coerceAtLeast(6f)
        drawText(textLayoutResult = labelOutRes, topLeft = Offset(labelLeft + 1.8f, labelTop + 1.8f))
        drawText(textLayoutResult = labelOutRes, topLeft = Offset(labelLeft - 1.2f, labelTop - 1.2f))
        drawText(textLayoutResult = labelRes, topLeft = Offset(labelLeft, labelTop))
    }

    val strokeShift = if (isCritical) 2.6f else 2.0f
    drawText(textLayoutResult = outlineResult, topLeft = Offset(left - strokeShift, y))
    drawText(textLayoutResult = outlineResult, topLeft = Offset(left + strokeShift, y))
    drawText(textLayoutResult = outlineResult, topLeft = Offset(left, y - strokeShift))
    drawText(textLayoutResult = outlineResult, topLeft = Offset(left, y + strokeShift))
    drawText(textLayoutResult = outlineResult, topLeft = Offset(left + strokeShift, y + strokeShift))
    drawText(textLayoutResult = result, topLeft = Offset(left, y))
}

private fun hypot(a: Float, b: Float): Float = kotlin.math.hypot(a.toDouble(), b.toDouble()).toFloat()
