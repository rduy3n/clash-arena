package com.example.ui.components

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
import com.example.game.data.FighterVisualStyle
import com.example.game.engine.CombatAction
import com.example.game.engine.FighterRuntime
import com.example.ui.theme.GuardBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val InkOutline = Color(0xFF05070D)

internal fun DrawScope.drawFighterReflection(
    f: FighterRuntime,
    arenaW: Float,
    arenaH: Float,
    groundY: Float
) {
    val cx = f.x * arenaW
    val bodyW = arenaH * 0.14f * f.def.stats.widthScale
    val alpha = (0.38f - f.y * 0.40f).coerceIn(0.06f, 0.38f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                f.def.primaryColor.copy(alpha = alpha),
                f.def.accentColor.copy(alpha = alpha * 0.45f),
                Color.Transparent
            ),
            center = Offset(cx, groundY + arenaH * 0.032f),
            radius = bodyW * 1.45f
        ),
        topLeft = Offset(cx - bodyW * 1.35f, groundY + 1f),
        size = Size(bodyW * 2.7f, arenaH * 0.085f)
    )
}

internal fun DrawScope.drawVineSnare(
    f: FighterRuntime,
    arenaW: Float,
    arenaH: Float,
    groundY: Float,
    timeMs: Long
) {
    val cx = f.x * arenaW
    val footY = groundY - f.y * (arenaH * 0.64f)
    val bodyH = arenaH * 0.30f * f.def.stats.heightScale
    val bodyW = arenaH * 0.135f * f.def.stats.widthScale

    val rootPath = Path().apply {
        moveTo(cx - bodyW * 0.90f, groundY + 3f)
        quadraticTo(cx - bodyW * 0.45f, footY - bodyH * 0.18f, cx, footY - bodyH * 0.08f)
        quadraticTo(cx + bodyW * 0.45f, footY - bodyH * 0.18f, cx + bodyW * 0.90f, groundY + 3f)
    }
    drawPath(rootPath, color = Color(0xFF072613), style = Stroke(width = 11f, cap = StrokeCap.Round))
    drawPath(rootPath, color = Color(0xFF00E676), style = Stroke(width = 5f, cap = StrokeCap.Round))

    for (ring in 0..3) {
        val ry = footY - bodyH * (0.16f + ring * 0.19f)
        val sway = sin(timeMs * 0.022f + ring * 1.4f) * 4f
        val rectLeft = cx - bodyW * 0.78f + sway
        val rectW = bodyW * 1.56f
        val rectH = arenaH * 0.040f
        drawOval(
            color = Color(0xFF062511),
            topLeft = Offset(rectLeft, ry),
            size = Size(rectW, rectH),
            style = Stroke(width = 9f)
        )
        drawOval(
            color = Color(0xFF00E676),
            topLeft = Offset(rectLeft, ry),
            size = Size(rectW, rectH),
            style = Stroke(width = 4.2f)
        )
        drawCircle(
            color = Color(0xFFFFD600),
            radius = 4f,
            center = Offset(rectLeft + rectW * (if (ring % 2 == 0) 0.14f else 0.86f), ry + rectH * 0.5f)
        )
    }
}

internal fun DrawScope.drawDetailedFighter(
    f: FighterRuntime,
    arenaW: Float,
    arenaH: Float,
    groundY: Float,
    timeMs: Long,
    hitStopActive: Boolean = false
) {
    val hitJitter = if ((hitStopActive || f.action == CombatAction.HIT_STUN) && (timeMs / 25L) % 2L == 0L) {
        f.facing * -3.5f
    } else 0f
    val cx = f.x * arenaW + hitJitter
    val jumpOffset = f.y * (arenaH * 0.64f)
    val footY = groundY - jumpOffset

    val hScale = f.def.stats.heightScale
    val wScale = f.def.stats.widthScale
    val unit = arenaH * 0.32f * hScale
    val shoulderHalfW = unit * 0.225f * wScale
    val waistHalfW = unit * 0.135f * wScale
    val hipHalfW = unit * 0.155f * wScale

    // Angled directional ground shadow + tight contact AO
    val shadowScale = (1f - f.y * 1.35f).coerceIn(0.32f, 1f)
    drawOval(
        color = Color.Black.copy(alpha = 0.68f * shadowScale),
        topLeft = Offset(cx - shoulderHalfW * 1.75f * shadowScale, groundY - arenaH * 0.014f),
        size = Size(shoulderHalfW * 3.5f * shadowScale, arenaH * 0.034f)
    )

    if (f.action == CombatAction.KO) {
        drawKoPose(f, cx, groundY, unit, wScale, timeMs)
        return
    }

    val dir = f.facing.toFloat()
    val isHit = f.action == CombatAction.HIT_STUN
    val flashWhite = isHit && (timeMs / 48L) % 2L == 0L
    val isSweeping = (f.action == CombatAction.BASE_ABILITY || f.action == CombatAction.ULTI) &&
        f.def.visualStyle == FighterVisualStyle.DARON_BRESILIEN

    // Normalized action progression [0..1]
    val actionFrac = if (f.actionDurationMs > 0L) {
        (1f - f.actionTimerMs.toFloat() / f.actionDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // 3-phase Guilty Gear strike curve:
    // windup (< 0): -0.45 -> 0
    // active snap (0..1): 0 -> 1.12
    // recovery: 1.12 -> 0
    val strikeCurve = when {
        actionFrac < 0.16f -> -0.45f * sin((actionFrac / 0.16f) * (PI.toFloat() * 0.5f))
        actionFrac < 0.42f -> {
            val t = (actionFrac - 0.16f) / 0.26f
            -0.45f + 1.55f * sin(t * (PI.toFloat() * 0.5f))
        }
        else -> {
            val t = (actionFrac - 0.42f) / 0.58f
            1.10f * (1f - t)
        }
    }

    val idleBob = if (f.action == CombatAction.IDLE) sin(timeMs / 145.0).toFloat() * (unit * 0.024f) else 0f
    val guardBreathe = if (f.action == CombatAction.BLOCK) sin(timeMs / 115.0).toFloat() * (unit * 0.012f) else 0f
    val crouchDrop = when {
        isSweeping -> unit * 0.34f
        f.action == CombatAction.BLOCK -> unit * 0.085f
        f.chargingMegaPunch -> unit * 0.115f
        f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI) ->
            (strikeCurve.coerceAtLeast(0f) * unit * 0.045f)
        else -> 0f
    }

    val leanX = when {
        isSweeping -> -dir * unit * 0.18f
        f.action == CombatAction.CLASSIC -> dir * unit * (0.04f + 0.12f * strikeCurve)
        f.action == CombatAction.BASE_ABILITY -> dir * unit * (0.05f + 0.15f * strikeCurve)
        f.action == CombatAction.ULTI && !f.chargingMegaPunch -> dir * unit * (0.06f + 0.17f * strikeCurve)
        f.chargingMegaPunch -> -dir * unit * 0.15f
        f.action == CombatAction.WALK -> dir * unit * 0.085f
        isHit -> -dir * unit * 0.16f
        else -> dir * unit * 0.035f
    }

    val hipCenter = Offset(cx, footY - unit * 0.43f + idleBob + guardBreathe + crouchDrop)
    val waistCenter = Offset(cx + leanX * 0.45f, hipCenter.y - unit * 0.11f)
    val shoulderCenter = Offset(cx + leanX, hipCenter.y - unit * 0.31f)
    val neckBase = Offset(shoulderCenter.x + dir * unit * 0.02f, shoulderCenter.y - unit * 0.03f)
    val headCenter = Offset(shoulderCenter.x + dir * unit * 0.035f, shoulderCenter.y - unit * 0.165f)

    val palette = fighterPalette(f.def.visualStyle, flashWhite)

    // Guilty Gear Aura Flame / Cyber Halo when Ulti Ready or Active
    if (f.ultiMeter >= 100 || f.action == CombatAction.ULTI || f.chargingMegaPunch) {
        val pulse = 0.30f + 0.22f * sin(timeMs / 80.0).toFloat()
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    f.def.primaryColor.copy(alpha = pulse),
                    f.def.accentColor.copy(alpha = pulse * 0.50f),
                    Color.Transparent
                ),
                center = Offset(cx + leanX * 0.4f, hipCenter.y - unit * 0.10f),
                radius = unit * 0.72f
            ),
            topLeft = Offset(cx - unit * 0.56f, headCenter.y - unit * 0.22f),
            size = Size(unit * 1.12f, unit * 1.24f)
        )
    }

    // Speed dash afterimages for Chen & Ulti strikes
    if ((f.def.visualStyle == FighterVisualStyle.CHINOIS_BAGUETTES &&
            (f.action == CombatAction.BASE_ABILITY || f.action == CombatAction.ULTI)) ||
        (f.action == CombatAction.ULTI && !f.chargingMegaPunch)
    ) {
        for (ghost in 1..2) {
            val gx = cx - dir * unit * (0.20f * ghost)
            val ga = 0.28f - ghost * 0.10f
            drawOval(
                color = f.def.primaryColor.copy(alpha = ga),
                topLeft = Offset(gx - shoulderHalfW * 0.85f, headCenter.y),
                size = Size(shoulderHalfW * 1.7f, footY - headCenter.y)
            )
        }
    }

    // Flowing scarves / headbands / jacket tails BEHIND body
    drawBackAccessories(f, shoulderCenter, headCenter, unit, dir, timeMs, flashWhite)

    // --- BACK & FRONT LEGS (sculpted thigh + calf musculature) ---
    val walkPhase = sin(timeMs / 70.0).toFloat()
    val thighR = unit * 0.095f * (0.95f + 0.22f * (wScale - 1f))
    val kneeR = thighR * 0.70f
    val ankleR = thighR * 0.50f

    if (isSweeping) {
        val supportKnee = Offset(hipCenter.x - dir * unit * 0.19f, footY - unit * 0.14f)
        val supportAnkle = Offset(hipCenter.x - dir * unit * 0.28f, footY - unit * 0.03f)
        drawAnatomicalLimb(hipCenter, supportKnee, thighR, kneeR, palette.pantsLight, palette.pantsDark, dir, null, bulgeBias = 1.15f)
        drawAnatomicalLimb(supportKnee, supportAnkle, kneeR, ankleR, palette.pantsLight, palette.pantsDark, dir, null, bulgeBias = 1.12f)
        drawDetailedBoot(supportAnkle, -dir, unit, palette.shoeColor, palette.shoeTrim)

        val sweepReach = if (f.action == CombatAction.ULTI) 0.86f else 0.74f
        val sweepExt = unit * (0.34f + (sweepReach - 0.34f) * strikeCurve.coerceIn(0.35f, 1.1f))
        val sweepKnee = Offset(hipCenter.x + dir * sweepExt * 0.50f, footY - unit * 0.09f)
        val sweepAnkle = Offset(hipCenter.x + dir * sweepExt, footY - unit * 0.045f)
        drawAnatomicalLimb(hipCenter, sweepKnee, thighR, kneeR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.45f), bulgeBias = 1.15f)
        drawAnatomicalLimb(sweepKnee, sweepAnkle, kneeR, ankleR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.65f), bulgeBias = 1.14f)
        drawDetailedBoot(sweepAnkle, dir, unit * 1.08f, f.def.primaryColor, Color.White)
    } else {
        val backHip = Offset(hipCenter.x - dir * hipHalfW * 0.52f, hipCenter.y)
        val frontHip = Offset(hipCenter.x + dir * hipHalfW * 0.52f, hipCenter.y)

        val (backKnee, backAnkle, frontKnee, frontAnkle) = when (f.action) {
            CombatAction.WALK -> {
                val stride = walkPhase * unit * 0.20f
                val liftBack = (-walkPhase).coerceAtLeast(0f) * unit * 0.07f
                val liftFront = walkPhase.coerceAtLeast(0f) * unit * 0.07f
                QuadOffsets(
                    Offset(backHip.x - stride * 0.5f + dir * unit * 0.05f, footY - unit * 0.22f - liftBack),
                    Offset(backHip.x - stride - dir * unit * 0.05f, footY - unit * 0.03f - liftBack),
                    Offset(frontHip.x + stride * 0.55f + dir * unit * 0.07f, footY - unit * 0.22f - liftFront),
                    Offset(frontHip.x + stride + dir * unit * 0.06f, footY - unit * 0.03f - liftFront)
                )
            }
            CombatAction.JUMP -> {
                QuadOffsets(
                    Offset(backHip.x - dir * unit * 0.09f, hipCenter.y + unit * 0.19f),
                    Offset(backHip.x - dir * unit * 0.16f, hipCenter.y + unit * 0.35f),
                    Offset(frontHip.x + dir * unit * 0.19f, hipCenter.y + unit * 0.10f),
                    Offset(frontHip.x + dir * unit * 0.10f, hipCenter.y + unit * 0.27f)
                )
            }
            else -> {
                val stanceSpread = unit * 0.185f
                val lunge = if (f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)) {
                    unit * 0.075f * strikeCurve.coerceAtLeast(0f)
                } else 0f
                QuadOffsets(
                    Offset(backHip.x - dir * stanceSpread * 0.55f, footY - unit * 0.21f),
                    Offset(backHip.x - dir * stanceSpread, footY - unit * 0.03f),
                    Offset(frontHip.x + dir * (stanceSpread * 0.85f + lunge), footY - unit * 0.21f),
                    Offset(frontHip.x + dir * (stanceSpread * 0.95f + lunge), footY - unit * 0.03f)
                )
            }
        }

        // Back leg (deep shadow tone for 2.5D separation)
        drawAnatomicalLimb(backHip, backKnee, thighR, kneeR, palette.pantsDark, palette.pantsDeepShadow, dir, null, bulgeBias = 1.14f)
        drawAnatomicalLimb(backKnee, backAnkle, kneeR, ankleR, palette.pantsDark, palette.pantsDeepShadow, dir, null, bulgeBias = 1.12f)
        drawDetailedBoot(backAnkle, dir, unit, palette.shoeColor, palette.shoeTrim)

        // Front leg with rim highlight
        drawAnatomicalLimb(frontHip, frontKnee, thighR, kneeR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.32f), bulgeBias = 1.15f)
        drawAnatomicalLimb(frontKnee, frontAnkle, kneeR, ankleR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.35f), bulgeBias = 1.14f)
        drawDetailedBoot(frontAnkle, dir, unit, palette.shoeColor, palette.shoeTrim)
    }

    // --- BACK ARM ---
    val bicepR = unit * 0.074f * (0.95f + 0.20f * (wScale - 1f))
    val forearmR = bicepR * 0.82f
    val wristR = bicepR * 0.64f
    val backShoulder = Offset(shoulderCenter.x - dir * shoulderHalfW * 0.72f, shoulderCenter.y + unit * 0.025f)
    val frontShoulder = Offset(shoulderCenter.x + dir * shoulderHalfW * 0.72f, shoulderCenter.y + unit * 0.025f)

    if (isSweeping) {
        val plantHand = Offset(cx - dir * unit * 0.29f, footY - unit * 0.04f)
        val supportElbow = Offset((backShoulder.x + plantHand.x) * 0.5f - dir * unit * 0.05f, (backShoulder.y + plantHand.y) * 0.5f)
        drawAnatomicalLimb(backShoulder, supportElbow, bicepR, forearmR, palette.sleeveDark, palette.pantsDeepShadow, dir, null, bulgeBias = 1.12f)
        drawAnatomicalLimb(supportElbow, plantHand, forearmR, wristR, palette.armDark, palette.skinShade, dir, null, bulgeBias = 1.08f)
    } else {
        val backElbow = Offset(backShoulder.x - dir * unit * 0.07f, backShoulder.y + unit * 0.14f)
        val backHand = when (f.action) {
            CombatAction.BLOCK -> Offset(frontShoulder.x + dir * unit * 0.12f, headCenter.y + unit * 0.02f)
            else -> Offset(backShoulder.x + dir * unit * 0.13f, shoulderCenter.y + unit * 0.06f + idleBob * 0.6f)
        }
        drawAnatomicalLimb(backShoulder, backElbow, bicepR, forearmR, palette.sleeveDark, palette.pantsDeepShadow, dir, null, bulgeBias = 1.12f)
        drawAnatomicalLimb(backElbow, backHand, forearmR, wristR, palette.armDark, palette.skinShade, dir, null, bulgeBias = 1.10f)
        drawDetailedFist(backHand, wristR * 1.48f, dir, palette.gloveColor, palette.gloveShade, f.def.primaryColor)
    }

    // --- SCULPTED CEL-SHADED TORSO & OUTFIT ---
    drawSculptedTorso(
        f = f,
        shoulderCenter = shoulderCenter,
        waistCenter = waistCenter,
        hipCenter = hipCenter,
        shoulderHalfW = shoulderHalfW,
        waistHalfW = waistHalfW,
        hipHalfW = hipHalfW,
        unit = unit,
        dir = dir,
        palette = palette,
        flashWhite = flashWhite
    )

    // --- ANATOMICAL NECK + ANIME HEAD, JAWLINE, EYES, HAIR ---
    drawAnatomicalLimb(neckBase, headCenter, unit * 0.052f, unit * 0.044f, palette.skinShade, palette.skinDeepShade, dir, null, bulgeBias = 1.0f)
    drawAnimeHead(
        f = f,
        headCenter = headCenter,
        unit = unit,
        dir = dir,
        palette = palette,
        timeMs = timeMs,
        flashWhite = flashWhite
    )

    // --- FRONT ATTACKING / GUARD ARM & WEAPON ---
    val (frontElbow, frontHand) = when {
        isSweeping -> {
            val e = Offset(frontShoulder.x - dir * unit * 0.12f, frontShoulder.y + unit * 0.06f)
            val h = Offset(e.x - dir * unit * 0.12f, e.y - unit * 0.06f)
            e to h
        }
        f.chargingMegaPunch -> {
            val e = Offset(frontShoulder.x - dir * unit * 0.22f, frontShoulder.y + unit * 0.06f)
            val h = Offset(frontShoulder.x - dir * unit * 0.34f, frontShoulder.y - unit * 0.04f)
            e to h
        }
        f.action == CombatAction.CLASSIC -> {
            val ext = (0.22f + 0.27f * strikeCurve).coerceAtLeast(0.05f)
            val e = Offset(frontShoulder.x + dir * unit * (ext * 0.46f), frontShoulder.y + unit * 0.01f)
            val h = Offset(frontShoulder.x + dir * unit * ext, frontShoulder.y - unit * 0.025f)
            e to h
        }
        f.action == CombatAction.BASE_ABILITY -> {
            val ext = (0.24f + 0.31f * strikeCurve).coerceAtLeast(0.05f)
            val e = Offset(frontShoulder.x + dir * unit * (ext * 0.48f), frontShoulder.y - unit * 0.025f)
            val h = Offset(frontShoulder.x + dir * unit * ext, frontShoulder.y - unit * 0.08f)
            e to h
        }
        f.action == CombatAction.ULTI -> {
            val ext = (0.26f + 0.34f * strikeCurve).coerceAtLeast(0.06f)
            val e = Offset(frontShoulder.x + dir * unit * (ext * 0.48f), frontShoulder.y - unit * 0.05f)
            val h = Offset(frontShoulder.x + dir * unit * ext, frontShoulder.y - unit * 0.125f)
            e to h
        }
        f.action == CombatAction.BLOCK -> {
            val e = Offset(frontShoulder.x + dir * unit * 0.14f, frontShoulder.y + unit * 0.07f)
            val h = Offset(frontShoulder.x + dir * unit * 0.18f, headCenter.y + unit * 0.04f)
            e to h
        }
        else -> {
            val e = Offset(frontShoulder.x + dir * unit * 0.065f, frontShoulder.y + unit * 0.14f + idleBob * 0.5f)
            val h = Offset(frontShoulder.x + dir * unit * 0.24f, shoulderCenter.y + unit * 0.03f + idleBob * 0.8f)
            e to h
        }
    }

    drawAnatomicalLimb(frontShoulder, frontElbow, bicepR, forearmR, palette.sleeveLight, palette.sleeveDark, dir, f.def.primaryColor.copy(alpha = 0.36f), bulgeBias = 1.15f)
    drawAnatomicalLimb(frontElbow, frontHand, forearmR, wristR, palette.armLight, palette.armDark, dir, f.def.accentColor.copy(alpha = 0.34f), bulgeBias = 1.14f)
    drawDetailedFist(frontHand, wristR * 1.56f, dir, palette.gloveColor, palette.gloveShade, f.def.accentColor)

    // Jardin Colossal Punch swirling power vortex
    if (f.chargingMegaPunch) {
        val pulse = 1f + 0.18f * sin(timeMs * 0.035f)
        val auraR = unit * 0.44f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFD600), Color(0xFF00E676), Color.Transparent),
                center = frontHand,
                radius = auraR
            ),
            radius = auraR,
            center = frontHand
        )
        val rot = (timeMs * 0.70f) % 360f
        drawArc(
            color = Color(0xFFFFD600),
            startAngle = rot,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = Offset(frontHand.x - auraR * 0.78f, frontHand.y - auraR * 0.78f),
            size = Size(auraR * 1.56f, auraR * 1.56f),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
        for (orb in 0..5) {
            val ang = (timeMs * 0.013f) + orb * (2f * PI.toFloat() / 6f)
            val ox = frontHand.x + cos(ang) * auraR * 0.94f
            val oy = frontHand.y + sin(ang) * auraR * 0.80f
            drawCircle(
                color = if (orb % 2 == 0) Color(0xFF00E676) else Color(0xFFFFC400),
                radius = 5.6f,
                center = Offset(ox, oy)
            )
            drawCircle(
                color = InkOutline,
                radius = 5.6f,
                center = Offset(ox, oy),
                style = Stroke(width = 1.8f)
            )
        }
    }

    // Maître Chen's twin golden & jade chopsticks with engraved tip bands
    if (f.def.visualStyle == FighterVisualStyle.CHINOIS_BAGUETTES) {
        val attacking = f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)
        val stickLen = if (attacking) unit * (0.36f + 0.16f * strikeCurve.coerceAtLeast(0f)) else unit * 0.30f
        val tip1 = Offset(frontHand.x + dir * stickLen, frontHand.y - unit * 0.065f)
        val tip2 = Offset(frontHand.x + dir * stickLen * 0.94f, frontHand.y + unit * 0.055f)
        drawLine(InkOutline, frontHand, tip1, strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD600), frontHand, tip1, strokeWidth = 4.2f, cap = StrokeCap.Round)
        drawLine(InkOutline, frontHand, tip2, strokeWidth = 7.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFF00E676), frontHand, tip2, strokeWidth = 3.8f, cap = StrokeCap.Round)
    }

    // Hexagonal Guilty Gear Faultless-Defense Style Barrier Shield
    if (f.isBlocking) {
        val shieldX = cx + dir * unit * 0.27f
        val shieldTop = headCenter.y - unit * 0.08f
        val shieldH = unit * 0.72f
        val shieldPath = Path().apply {
            moveTo(shieldX, shieldTop)
            lineTo(shieldX + dir * unit * 0.17f, shieldTop + shieldH * 0.20f)
            lineTo(shieldX + dir * unit * 0.17f, shieldTop + shieldH * 0.80f)
            lineTo(shieldX, shieldTop + shieldH)
        }
        drawPath(shieldPath, color = GuardBlue.copy(alpha = 0.28f))
        drawPath(shieldPath, color = GuardBlue, style = Stroke(width = 6.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(shieldPath, color = Color.White.copy(alpha = 0.75f), style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawKoPose(
    f: FighterRuntime,
    cx: Float,
    groundY: Float,
    unit: Float,
    wScale: Float,
    timeMs: Long
) {
    val palette = fighterPalette(f.def.visualStyle, false)
    val bodyLen = unit * 0.76f
    val thickness = unit * 0.16f * wScale
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(palette.torsoLight, palette.torsoDark)),
        topLeft = Offset(cx - bodyLen * 0.5f, groundY - thickness),
        size = Size(bodyLen, thickness),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = InkOutline,
        topLeft = Offset(cx - bodyLen * 0.5f, groundY - thickness),
        size = Size(bodyLen, thickness),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(width = 3.4f)
    )
    val headX = cx - f.facing * bodyLen * 0.45f
    val headY = groundY - thickness * 0.75f
    drawCircle(color = palette.skinLight, radius = unit * 0.11f, center = Offset(headX, headY))
    drawCircle(color = InkOutline, radius = unit * 0.11f, center = Offset(headX, headY), style = Stroke(width = 3f))

    for (s in 0..2) {
        val ang = timeMs * 0.006f + s * (2f * PI.toFloat() / 3f)
        val sx = headX + cos(ang) * unit * 0.14f
        val sy = headY - unit * 0.18f + sin(ang) * unit * 0.04f
        drawCircle(color = Color(0xFFFFD600), radius = 4.5f, center = Offset(sx, sy))
    }
}

private fun DrawScope.drawBackAccessories(
    f: FighterRuntime,
    shoulderCenter: Offset,
    headCenter: Offset,
    unit: Float,
    dir: Float,
    timeMs: Long,
    flashWhite: Boolean
) {
    if (flashWhite) return
    val wave = sin(timeMs * 0.013f) * unit * 0.05f
    val wave2 = cos(timeMs * 0.016f) * unit * 0.035f
    when (f.def.visualStyle) {
        FighterVisualStyle.GLITCHERMAN -> {
            // Dual trailing cyber-scarf ribbons
            val scarf = Path().apply {
                moveTo(shoulderCenter.x - dir * unit * 0.05f, shoulderCenter.y - unit * 0.02f)
                quadraticTo(
                    shoulderCenter.x - dir * unit * 0.28f,
                    shoulderCenter.y + unit * 0.03f + wave,
                    shoulderCenter.x - dir * unit * 0.48f,
                    shoulderCenter.y + unit * 0.12f - wave
                )
                lineTo(shoulderCenter.x - dir * unit * 0.40f, shoulderCenter.y + unit * 0.20f - wave2)
                quadraticTo(
                    shoulderCenter.x - dir * unit * 0.20f,
                    shoulderCenter.y + unit * 0.10f + wave,
                    shoulderCenter.x - dir * unit * 0.02f,
                    shoulderCenter.y + unit * 0.04f
                )
                close()
            }
            drawPath(scarf, brush = Brush.horizontalGradient(listOf(Color(0xFFFF2A85), Color(0xFF00E5FF))))
            drawPath(scarf, color = InkOutline, style = Stroke(width = 2.8f, join = StrokeJoin.Round))
        }
        FighterVisualStyle.CHINOIS_BAGUETTES -> {
            // Twin fluttering crimson martial headband tails
            for (tail in 0..1) {
                val offsetY = tail * unit * 0.045f
                val band = Path().apply {
                    moveTo(headCenter.x - dir * unit * 0.06f, headCenter.y - unit * 0.04f + offsetY * 0.4f)
                    quadraticTo(
                        headCenter.x - dir * unit * 0.26f,
                        headCenter.y - unit * 0.02f + wave + offsetY,
                        headCenter.x - dir * unit * (0.42f - tail * 0.06f),
                        headCenter.y + unit * 0.08f - wave + offsetY
                    )
                    lineTo(headCenter.x - dir * unit * (0.36f - tail * 0.06f), headCenter.y + unit * 0.13f - wave + offsetY)
                    quadraticTo(
                        headCenter.x - dir * unit * 0.18f,
                        headCenter.y + unit * 0.04f + wave + offsetY * 0.5f,
                        headCenter.x - dir * unit * 0.05f,
                        headCenter.y + offsetY * 0.4f
                    )
                    close()
                }
                drawPath(band, color = if (tail == 0) Color(0xFFE50000) else Color(0xFF9E0000))
                drawPath(band, color = InkOutline, style = Stroke(width = 2.4f))
            }
        }
        FighterVisualStyle.JARDIN -> {
            // Jagged mossy leaf-cloak mantle
            val cape = Path().apply {
                moveTo(shoulderCenter.x - dir * unit * 0.12f, shoulderCenter.y)
                lineTo(shoulderCenter.x - dir * unit * 0.34f, shoulderCenter.y + unit * 0.30f + wave * 0.5f)
                lineTo(shoulderCenter.x - dir * unit * 0.20f, shoulderCenter.y + unit * 0.27f)
                lineTo(shoulderCenter.x - dir * unit * 0.14f, shoulderCenter.y + unit * 0.37f)
                lineTo(shoulderCenter.x - dir * unit * 0.04f, shoulderCenter.y + unit * 0.30f)
                close()
            }
            drawPath(cape, brush = Brush.verticalGradient(listOf(Color(0xFF1E5631), Color(0xFF0C2615))))
            drawPath(cape, color = InkOutline, style = Stroke(width = 3f, join = StrokeJoin.Round))
        }
        FighterVisualStyle.DARON_BRESILIEN -> {
            // Open flowing summer jacket tails
            val flap = Path().apply {
                moveTo(shoulderCenter.x - dir * unit * 0.14f, shoulderCenter.y + unit * 0.06f)
                lineTo(shoulderCenter.x - dir * unit * 0.28f, shoulderCenter.y + unit * 0.36f + wave * 0.5f)
                lineTo(shoulderCenter.x - dir * unit * 0.08f, shoulderCenter.y + unit * 0.36f)
                close()
            }
            drawPath(flap, color = Color(0xFFD9AE00))
            drawPath(flap, color = InkOutline, style = Stroke(width = 2.8f, join = StrokeJoin.Round))
        }
    }
}

private fun DrawScope.drawSculptedTorso(
    f: FighterRuntime,
    shoulderCenter: Offset,
    waistCenter: Offset,
    hipCenter: Offset,
    shoulderHalfW: Float,
    waistHalfW: Float,
    hipHalfW: Float,
    unit: Float,
    dir: Float,
    palette: FighterPalette,
    flashWhite: Boolean
) {
    val isTank = f.def.visualStyle == FighterVisualStyle.DARON_BRESILIEN
    val wMid = if (isTank) shoulderHalfW * 0.96f else waistHalfW

    // Sculpted upper-to-lower torso silhouette with curved ribcage/waist
    val torsoPath = Path().apply {
        moveTo(shoulderCenter.x - shoulderHalfW, shoulderCenter.y)
        lineTo(shoulderCenter.x + shoulderHalfW, shoulderCenter.y)
        quadraticTo(
            waistCenter.x + wMid * 1.05f,
            waistCenter.y,
            hipCenter.x + hipHalfW,
            hipCenter.y + unit * 0.03f
        )
        lineTo(hipCenter.x - hipHalfW, hipCenter.y + unit * 0.03f)
        quadraticTo(
            waistCenter.x - wMid * 1.05f,
            waistCenter.y,
            shoulderCenter.x - shoulderHalfW,
            shoulderCenter.y
        )
        close()
    }

    // Base fill
    drawPath(torsoPath, color = palette.torsoLight)

    if (!flashWhite) {
        // Hard-edge cel-shadow cut on back side of torso (Guilty Gear style)
        val shadowSidePath = Path().apply {
            if (dir > 0) {
                moveTo(shoulderCenter.x - shoulderHalfW, shoulderCenter.y)
                lineTo(shoulderCenter.x - shoulderHalfW * 0.05f, shoulderCenter.y)
                lineTo(waistCenter.x - wMid * 0.15f, waistCenter.y)
                lineTo(hipCenter.x - hipHalfW * 0.10f, hipCenter.y + unit * 0.03f)
                lineTo(hipCenter.x - hipHalfW, hipCenter.y + unit * 0.03f)
                quadraticTo(waistCenter.x - wMid * 1.05f, waistCenter.y, shoulderCenter.x - shoulderHalfW, shoulderCenter.y)
            } else {
                moveTo(shoulderCenter.x + shoulderHalfW, shoulderCenter.y)
                lineTo(shoulderCenter.x + shoulderHalfW * 0.05f, shoulderCenter.y)
                lineTo(waistCenter.x + wMid * 0.15f, waistCenter.y)
                lineTo(hipCenter.x + hipHalfW * 0.10f, hipCenter.y + unit * 0.03f)
                lineTo(hipCenter.x + hipHalfW, hipCenter.y + unit * 0.03f)
                quadraticTo(waistCenter.x + wMid * 1.05f, waistCenter.y, shoulderCenter.x + shoulderHalfW, shoulderCenter.y)
            }
            close()
        }
        drawPath(shadowSidePath, color = palette.torsoDark)

        // Sharp front rim highlight
        val rimX1 = shoulderCenter.x + dir * (shoulderHalfW * 0.86f)
        val rimX2 = hipCenter.x + dir * (hipHalfW * 0.86f)
        drawLine(
            color = f.def.primaryColor.copy(alpha = 0.55f),
            start = Offset(rimX1, shoulderCenter.y + unit * 0.02f),
            end = Offset(rimX2, hipCenter.y),
            strokeWidth = 3.2f,
            cap = StrokeCap.Round
        )
    }

    drawPath(torsoPath, color = InkOutline, style = Stroke(width = 3.8f, join = StrokeJoin.Round))

    if (flashWhite) return

    when (f.def.visualStyle) {
        FighterVisualStyle.GLITCHERMAN -> {
            // High cyber jacket collar + glowing neon lapels + chest emblem
            val collar = Path().apply {
                moveTo(shoulderCenter.x - shoulderHalfW * 0.42f, shoulderCenter.y)
                lineTo(shoulderCenter.x - shoulderHalfW * 0.52f, shoulderCenter.y - unit * 0.075f)
                lineTo(shoulderCenter.x + shoulderHalfW * 0.48f, shoulderCenter.y - unit * 0.075f)
                lineTo(shoulderCenter.x + shoulderHalfW * 0.40f, shoulderCenter.y)
                close()
            }
            drawPath(collar, color = Color(0xFF18223C))
            drawPath(collar, color = InkOutline, style = Stroke(width = 2.6f))
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(shoulderCenter.x - shoulderHalfW * 0.52f, shoulderCenter.y + unit * 0.05f),
                end = Offset(hipCenter.x + hipHalfW * 0.50f, hipCenter.y - unit * 0.04f),
                strokeWidth = 4.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFF2A85),
                start = Offset(shoulderCenter.x + shoulderHalfW * 0.46f, shoulderCenter.y + unit * 0.08f),
                end = Offset(hipCenter.x - hipHalfW * 0.40f, hipCenter.y - unit * 0.05f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
            // Holo core badge
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = unit * 0.024f,
                center = Offset(shoulderCenter.x + dir * unit * 0.03f, shoulderCenter.y + unit * 0.14f)
            )
            // Tech waist belt
            drawRoundRect(
                color = Color(0xFF0E121E),
                topLeft = Offset(hipCenter.x - hipHalfW * 1.04f, hipCenter.y - unit * 0.03f),
                size = Size(hipHalfW * 2.08f, unit * 0.05f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRect(
                color = Color(0xFFFF2A85),
                topLeft = Offset(hipCenter.x + dir * hipHalfW * 0.1f - unit * 0.03f, hipCenter.y - unit * 0.026f),
                size = Size(unit * 0.06f, unit * 0.042f)
            )
        }
        FighterVisualStyle.JARDIN -> {
            // Jagged living-bark shoulder armor pauldrons + chest pec lines + glowing nature core
            for (sideSign in listOf(-1f, 1f)) {
                val px = shoulderCenter.x + sideSign * shoulderHalfW * 0.92f
                val py = shoulderCenter.y + unit * 0.01f
                val pauldron = Path().apply {
                    moveTo(px - sideSign * unit * 0.05f, py - unit * 0.06f)
                    lineTo(px + sideSign * unit * 0.11f, py - unit * 0.09f)
                    lineTo(px + sideSign * unit * 0.09f, py + unit * 0.07f)
                    lineTo(px - sideSign * unit * 0.05f, py + unit * 0.05f)
                    close()
                }
                drawPath(pauldron, color = Color(0xFF285732))
                drawPath(pauldron, color = InkOutline, style = Stroke(width = 3f, join = StrokeJoin.Round))
            }
            // Pectoral bark plates
            drawLine(
                color = InkOutline,
                start = Offset(shoulderCenter.x - shoulderHalfW * 0.45f, shoulderCenter.y + unit * 0.12f),
                end = Offset(shoulderCenter.x + shoulderHalfW * 0.45f, shoulderCenter.y + unit * 0.12f),
                strokeWidth = 2.4f
            )
            val coreCenter = Offset((shoulderCenter.x + hipCenter.x) * 0.5f + dir * unit * 0.03f, shoulderCenter.y + unit * 0.15f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, Color(0xFFFFC400), Color(0xFF00E676))),
                radius = unit * 0.068f,
                center = coreCenter
            )
            drawCircle(color = InkOutline, radius = unit * 0.068f, center = coreCenter, style = Stroke(width = 2.4f))
        }
        FighterVisualStyle.CHINOIS_BAGUETTES -> {
            // Traditional crossed martial gi lapel with gold embroidery + tied sash belt
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(shoulderCenter.x - dir * shoulderHalfW * 0.2f, shoulderCenter.y + unit * 0.02f),
                end = Offset(hipCenter.x + dir * hipHalfW * 0.4f, hipCenter.y),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(shoulderCenter.x + dir * shoulderHalfW * 0.35f, shoulderCenter.y + unit * 0.02f),
                end = Offset(waistCenter.x, waistCenter.y + unit * 0.03f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
            // Tied black-gold martial waist sash
            drawRoundRect(
                color = Color(0xFF11131C),
                topLeft = Offset(hipCenter.x - hipHalfW * 1.06f, hipCenter.y - unit * 0.035f),
                size = Size(hipHalfW * 2.12f, unit * 0.056f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawCircle(
                color = Color(0xFFFFD600),
                radius = unit * 0.028f,
                center = Offset(hipCenter.x + dir * hipHalfW * 0.25f, hipCenter.y - unit * 0.008f)
            )
        }
        FighterVisualStyle.DARON_BRESILIEN -> {
            // Open summer jacket showing bare chest + gold chain medallion + bold palm-leaf patterns
            val chestV = Path().apply {
                moveTo(shoulderCenter.x - shoulderHalfW * 0.22f, shoulderCenter.y)
                lineTo(shoulderCenter.x + dir * unit * 0.02f, hipCenter.y - unit * 0.02f)
                lineTo(shoulderCenter.x + shoulderHalfW * 0.44f, shoulderCenter.y)
                close()
            }
            drawPath(chestV, color = palette.skinLight)
            drawPath(chestV, color = InkOutline, style = Stroke(width = 2.6f, join = StrokeJoin.Round))
            // Gold chain & medallion
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(shoulderCenter.x - shoulderHalfW * 0.14f, shoulderCenter.y + unit * 0.01f),
                end = Offset(shoulderCenter.x + dir * unit * 0.02f, shoulderCenter.y + unit * 0.13f),
                strokeWidth = 2.2f
            )
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(shoulderCenter.x + shoulderHalfW * 0.34f, shoulderCenter.y + unit * 0.01f),
                end = Offset(shoulderCenter.x + dir * unit * 0.02f, shoulderCenter.y + unit * 0.13f),
                strokeWidth = 2.2f
            )
            drawCircle(
                color = Color(0xFFFFD600),
                radius = unit * 0.036f,
                center = Offset(shoulderCenter.x + dir * unit * 0.02f, shoulderCenter.y + unit * 0.14f)
            )
            drawCircle(color = InkOutline, radius = unit * 0.036f, center = Offset(shoulderCenter.x + dir * unit * 0.02f, shoulderCenter.y + unit * 0.14f), style = Stroke(width = 1.6f))
            // Tropical palm leaf accents on summer jacket
            for (sx in listOf(-0.68f, 0.72f)) {
                drawLine(
                    color = Color(0xFF00C853),
                    start = Offset(shoulderCenter.x + shoulderHalfW * sx, shoulderCenter.y + unit * 0.08f),
                    end = Offset(hipCenter.x + hipHalfW * (sx * 0.72f), hipCenter.y - unit * 0.05f),
                    strokeWidth = 5.5f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawAnimeHead(
    f: FighterRuntime,
    headCenter: Offset,
    unit: Float,
    dir: Float,
    palette: FighterPalette,
    timeMs: Long,
    flashWhite: Boolean
) {
    val headR = unit * (if (f.def.visualStyle == FighterVisualStyle.JARDIN) 0.120f else 0.106f)

    // Sculpted anime head with sharp chin & jawline facing `dir`
    val headPath = Path().apply {
        moveTo(headCenter.x - dir * headR * 0.85f, headCenter.y - headR * 0.65f)
        quadraticTo(headCenter.x, headCenter.y - headR * 1.15f, headCenter.x + dir * headR * 0.90f, headCenter.y - headR * 0.55f)
        lineTo(headCenter.x + dir * headR * 0.94f, headCenter.y + headR * 0.25f)
        // Sharp anime chin point
        lineTo(headCenter.x + dir * headR * 0.42f, headCenter.y + headR * 1.05f)
        // Jawline back toward ear
        lineTo(headCenter.x - dir * headR * 0.65f, headCenter.y + headR * 0.62f)
        quadraticTo(headCenter.x - dir * headR * 1.00f, headCenter.y, headCenter.x - dir * headR * 0.85f, headCenter.y - headR * 0.65f)
        close()
    }

    drawPath(headPath, color = palette.skinLight)
    if (!flashWhite) {
        // Hard cel-shadow along back cheek/jaw
        val cheekShadow = Path().apply {
            moveTo(headCenter.x - dir * headR * 0.85f, headCenter.y - headR * 0.65f)
            lineTo(headCenter.x - dir * headR * 0.18f, headCenter.y - headR * 0.45f)
            lineTo(headCenter.x - dir * headR * 0.25f, headCenter.y + headR * 0.78f)
            lineTo(headCenter.x - dir * headR * 0.65f, headCenter.y + headR * 0.62f)
            quadraticTo(headCenter.x - dir * headR * 1.00f, headCenter.y, headCenter.x - dir * headR * 0.85f, headCenter.y - headR * 0.65f)
            close()
        }
        drawPath(cheekShadow, color = palette.skinShade)
    }
    drawPath(headPath, color = InkOutline, style = Stroke(width = 3.2f, join = StrokeJoin.Round))

    if (flashWhite) return

    when (f.def.visualStyle) {
        FighterVisualStyle.GLITCHERMAN -> {
            // Swept anime cyber-spikes
            val hair = Path().apply {
                moveTo(headCenter.x - dir * headR * 0.95f, headCenter.y + headR * 0.1f)
                lineTo(headCenter.x - dir * headR * 1.35f, headCenter.y - headR * 0.60f)
                lineTo(headCenter.x - dir * headR * 0.75f, headCenter.y - headR * 0.68f)
                lineTo(headCenter.x - dir * headR * 1.05f, headCenter.y - headR * 1.25f)
                lineTo(headCenter.x - dir * headR * 0.15f, headCenter.y - headR * 0.85f)
                lineTo(headCenter.x + dir * headR * 0.35f, headCenter.y - headR * 1.42f)
                lineTo(headCenter.x + dir * headR * 0.78f, headCenter.y - headR * 0.80f)
                lineTo(headCenter.x + dir * headR * 1.15f, headCenter.y - headR * 0.45f)
                lineTo(headCenter.x + dir * headR * 0.62f, headCenter.y - headR * 0.30f)
                close()
            }
            drawPath(hair, color = Color(0xFF141B30))
            drawPath(hair, color = InkOutline, style = Stroke(width = 2.6f, join = StrokeJoin.Round))

            // Sharp angular Guilty Gear cyber-visor
            val visorPath = Path().apply {
                moveTo(headCenter.x - dir * headR * 0.10f, headCenter.y - headR * 0.34f)
                lineTo(headCenter.x + dir * headR * 1.04f, headCenter.y - headR * 0.42f)
                lineTo(headCenter.x + dir * headR * 0.96f, headCenter.y + headR * 0.08f)
                lineTo(headCenter.x + dir * headR * 0.02f, headCenter.y + headR * 0.02f)
                close()
            }
            drawPath(visorPath, brush = Brush.horizontalGradient(listOf(Color(0xFFFF2A85), Color(0xFF00E5FF))))
            drawPath(visorPath, color = InkOutline, style = Stroke(width = 2.2f))
            // Glint bar
            val scanOffset = 0.25f + 0.40f * (0.5f + 0.5f * sin(timeMs * 0.008f))
            drawLine(
                color = Color.White.copy(alpha = 0.88f),
                start = Offset(headCenter.x + dir * headR * scanOffset, headCenter.y - headR * 0.24f),
                end = Offset(headCenter.x + dir * headR * (scanOffset + 0.30f), headCenter.y - headR * 0.05f),
                strokeWidth = 2.6f,
                cap = StrokeCap.Round
            )
            // Determined smirk line
            drawLine(
                color = InkOutline,
                start = Offset(headCenter.x + dir * headR * 0.25f, headCenter.y + headR * 0.52f),
                end = Offset(headCenter.x + dir * headR * 0.64f, headCenter.y + headR * 0.45f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }

        FighterVisualStyle.JARDIN -> {
            // Jagged emerald leaf-horn crown
            val crown = Path().apply {
                moveTo(headCenter.x - dir * headR * 0.9f, headCenter.y - headR * 0.4f)
                lineTo(headCenter.x - dir * headR * 1.3f, headCenter.y - headR * 1.38f)
                lineTo(headCenter.x - dir * headR * 0.25f, headCenter.y - headR * 0.85f)
                lineTo(headCenter.x + dir * headR * 0.20f, headCenter.y - headR * 1.48f)
                lineTo(headCenter.x + dir * headR * 0.98f, headCenter.y - headR * 0.50f)
                close()
            }
            drawPath(crown, brush = Brush.verticalGradient(listOf(Color(0xFF69F0AE), Color(0xFF00C853))))
            drawPath(crown, color = InkOutline, style = Stroke(width = 2.8f, join = StrokeJoin.Round))

            // Fierce glowing amber anime eye + furrowed brow
            val eyeCenter = Offset(headCenter.x + dir * headR * 0.46f, headCenter.y - headR * 0.08f)
            drawCircle(color = Color(0xFFFFD600), radius = headR * 0.22f, center = eyeCenter)
            drawCircle(color = Color.White, radius = headR * 0.09f, center = eyeCenter)
            drawLine(
                color = InkOutline,
                start = Offset(headCenter.x + dir * headR * 0.14f, headCenter.y - headR * 0.34f),
                end = Offset(headCenter.x + dir * headR * 0.84f, headCenter.y - headR * 0.16f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
        }

        FighterVisualStyle.CHINOIS_BAGUETTES -> {
            // High samurai topknot bun + sharp bangs
            drawCircle(
                color = Color(0xFF10121A),
                radius = headR * 0.45f,
                center = Offset(headCenter.x - dir * headR * 0.18f, headCenter.y - headR * 1.24f)
            )
            drawCircle(
                color = InkOutline,
                radius = headR * 0.45f,
                center = Offset(headCenter.x - dir * headR * 0.18f, headCenter.y - headR * 1.24f),
                style = Stroke(width = 2.4f)
            )
            val hair = Path().apply {
                moveTo(headCenter.x - dir * headR * 0.96f, headCenter.y + headR * 0.05f)
                lineTo(headCenter.x - dir * headR * 0.92f, headCenter.y - headR * 0.72f)
                quadraticTo(headCenter.x, headCenter.y - headR * 1.22f, headCenter.x + dir * headR * 0.95f, headCenter.y - headR * 0.52f)
                lineTo(headCenter.x + dir * headR * 0.55f, headCenter.y - headR * 0.32f)
                close()
            }
            drawPath(hair, color = Color(0xFF10121A))

            // Crimson headband strip
            drawLine(
                color = Color(0xFFE50000),
                start = Offset(headCenter.x - dir * headR * 0.90f, headCenter.y - headR * 0.44f),
                end = Offset(headCenter.x + dir * headR * 0.94f, headCenter.y - headR * 0.36f),
                strokeWidth = headR * 0.28f,
                cap = StrokeCap.Butt
            )
            // Sharp anime eye + brow
            drawLine(
                color = InkOutline,
                start = Offset(headCenter.x + dir * headR * 0.22f, headCenter.y - headR * 0.20f),
                end = Offset(headCenter.x + dir * headR * 0.78f, headCenter.y - headR * 0.08f),
                strokeWidth = 2.8f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF10131C),
                radius = headR * 0.15f,
                center = Offset(headCenter.x + dir * headR * 0.50f, headCenter.y - headR * 0.02f)
            )
            drawCircle(
                color = Color.White,
                radius = headR * 0.055f,
                center = Offset(headCenter.x + dir * headR * 0.53f, headCenter.y - headR * 0.04f)
            )
        }

        FighterVisualStyle.DARON_BRESILIEN -> {
            // Slicked dark hair sweep
            val hair = Path().apply {
                moveTo(headCenter.x - dir * headR * 0.94f, headCenter.y - headR * 0.10f)
                quadraticTo(headCenter.x - dir * headR * 0.4f, headCenter.y - headR * 1.24f, headCenter.x + dir * headR * 0.92f, headCenter.y - headR * 0.52f)
                lineTo(headCenter.x + dir * headR * 0.42f, headCenter.y - headR * 0.38f)
                close()
            }
            drawPath(hair, color = Color(0xFF1E1714))

            // Sharp angular aviator sunglasses with specular gleam
            val glassPath = Path().apply {
                moveTo(headCenter.x - dir * headR * 0.05f, headCenter.y - headR * 0.32f)
                lineTo(headCenter.x + dir * headR * 0.96f, headCenter.y - headR * 0.34f)
                lineTo(headCenter.x + dir * headR * 0.86f, headCenter.y + headR * 0.12f)
                lineTo(headCenter.x + dir * headR * 0.06f, headCenter.y + headR * 0.08f)
                close()
            }
            drawPath(glassPath, color = Color(0xFF0C1018))
            drawPath(glassPath, color = Color(0xFFFFD600), style = Stroke(width = 2.0f))
            drawLine(
                color = Color.White.copy(alpha = 0.78f),
                start = Offset(headCenter.x + dir * headR * 0.25f, headCenter.y - headR * 0.24f),
                end = Offset(headCenter.x + dir * headR * 0.50f, headCenter.y + headR * 0.02f),
                strokeWidth = 2.5f
            )
            // Charismatic handlebar mustache
            drawArc(
                color = Color(0xFF241610),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(headCenter.x + dir * headR * 0.10f - headR * 0.42f, headCenter.y + headR * 0.24f),
                size = Size(headR * 0.84f, headR * 0.36f),
                style = Stroke(width = 6.2f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawAnatomicalLimb(
    start: Offset,
    end: Offset,
    rStart: Float,
    rEnd: Float,
    fillLight: Color,
    fillDark: Color,
    dir: Float,
    rimColor: Color?,
    bulgeBias: Float = 1.12f
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux

    val midX = (start.x + end.x) * 0.5f
    val midY = (start.y + end.y) * 0.5f
    val midR = ((rStart + rEnd) * 0.5f) * bulgeBias

    val path = Path().apply {
        moveTo(start.x + px * rStart, start.y + py * rStart)
        quadraticTo(midX + px * midR, midY + py * midR, end.x + px * rEnd, end.y + py * rEnd)
        quadraticTo(end.x + ux * rEnd * 1.15f, end.y + uy * rEnd * 1.15f, end.x - px * rEnd, end.y - py * rEnd)
        quadraticTo(midX - px * midR, midY - py * midR, start.x - px * rStart, start.y - py * rStart)
        quadraticTo(start.x - ux * rStart * 1.15f, start.y - uy * rStart * 1.15f, start.x + px * rStart, start.y + py * rStart)
        close()
    }
    // Hard cel-shaded dual-tone split across muscle length
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            0.0f to fillLight,
            0.58f to fillLight,
            0.62f to fillDark,
            1.0f to fillDark,
            start = Offset(midX + px * midR, midY + py * midR),
            end = Offset(midX - px * midR, midY - py * midR)
        )
    )
    if (rimColor != null) {
        drawLine(
            color = rimColor,
            start = Offset(start.x + dir * rStart * 0.35f, start.y),
            end = Offset(end.x + dir * rEnd * 0.35f, end.y),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )
    }
    drawPath(path, color = InkOutline, style = Stroke(width = 3.2f, join = StrokeJoin.Round))
}

private fun DrawScope.drawDetailedFist(
    center: Offset,
    radius: Float,
    dir: Float,
    fill: Color,
    shade: Color,
    rimColor: Color
) {
    // Sculpted knuckle-and-thumb fist silhouette instead of plain sphere
    val w = radius * 1.24f
    val h = radius * 1.05f
    val fistPath = Path().apply {
        moveTo(center.x - dir * w * 0.65f, center.y - h * 0.55f)
        lineTo(center.x + dir * w * 0.62f, center.y - h * 0.68f)
        lineTo(center.x + dir * w * 0.90f, center.y - h * 0.10f)
        lineTo(center.x + dir * w * 0.72f, center.y + h * 0.75f)
        lineTo(center.x - dir * w * 0.55f, center.y + h * 0.68f)
        close()
    }
    drawPath(
        fistPath,
        brush = Brush.verticalGradient(
            0.0f to fill,
            0.56f to fill,
            0.60f to shade,
            1.0f to shade,
            startY = center.y - h,
            endY = center.y + h
        )
    )
    drawLine(
        color = rimColor.copy(alpha = 0.85f),
        start = Offset(center.x + dir * w * 0.62f, center.y - h * 0.60f),
        end = Offset(center.x + dir * w * 0.84f, center.y - h * 0.08f),
        strokeWidth = 2.4f,
        cap = StrokeCap.Round
    )
    // Knuckle ridge crease
    drawLine(
        color = InkOutline,
        start = Offset(center.x + dir * w * 0.25f, center.y - h * 0.35f),
        end = Offset(center.x + dir * w * 0.25f, center.y + h * 0.35f),
        strokeWidth = 2.0f
    )
    drawPath(fistPath, color = InkOutline, style = Stroke(width = 3.0f, join = StrokeJoin.Round))
}

private fun DrawScope.drawDetailedBoot(
    ankle: Offset,
    dir: Float,
    unit: Float,
    shoeColor: Color,
    trimColor: Color
) {
    val w = unit * 0.155f
    val h = unit * 0.078f
    val backX = ankle.x - dir * w * 0.26f
    val frontX = ankle.x + dir * w * 0.78f
    val topY = ankle.y - h * 0.38f
    val botY = ankle.y + h * 0.62f

    val bootPath = Path().apply {
        moveTo(backX, topY)
        lineTo(ankle.x + dir * w * 0.22f, topY)
        lineTo(frontX, ankle.y + h * 0.05f)
        lineTo(frontX, botY)
        lineTo(backX, botY)
        close()
    }
    drawPath(bootPath, color = shoeColor)
    drawLine(
        color = trimColor,
        start = Offset(backX, botY - h * 0.16f),
        end = Offset(frontX, botY - h * 0.16f),
        strokeWidth = h * 0.24f
    )
    drawPath(bootPath, color = InkOutline, style = Stroke(width = 2.8f, join = StrokeJoin.Round))
}

private data class QuadOffsets(
    val a: Offset,
    val b: Offset,
    val c: Offset,
    val d: Offset
)

private data class FighterPalette(
    val torsoLight: Color,
    val torsoDark: Color,
    val sleeveLight: Color,
    val sleeveDark: Color,
    val armLight: Color,
    val armDark: Color,
    val gloveColor: Color,
    val gloveShade: Color,
    val pantsLight: Color,
    val pantsDark: Color,
    val pantsDeepShadow: Color,
    val shoeColor: Color,
    val shoeTrim: Color,
    val skinLight: Color,
    val skinShade: Color,
    val skinDeepShade: Color
)

private fun fighterPalette(style: FighterVisualStyle, flashWhite: Boolean): FighterPalette {
    if (flashWhite) {
        val w = Color.White
        return FighterPalette(w, w, w, w, w, w, w, w, w, w, w, w, w, w, w, w)
    }
    return when (style) {
        FighterVisualStyle.GLITCHERMAN -> FighterPalette(
            torsoLight = Color(0xFF283963),
            torsoDark = Color(0xFF10172D),
            sleeveLight = Color(0xFF283963),
            sleeveDark = Color(0xFF141C35),
            armLight = Color(0xFF1C2642),
            armDark = Color(0xFF0E1424),
            gloveColor = Color(0xFF00E5FF),
            gloveShade = Color(0xFF0091B5),
            pantsLight = Color(0xFF1F2740),
            pantsDark = Color(0xFF0F1424),
            pantsDeepShadow = Color(0xFF080B14),
            shoeColor = Color(0xFFFF2A85),
            shoeTrim = Color(0xFF00E5FF),
            skinLight = Color(0xFFF4DAC6),
            skinShade = Color(0xFFCBA68C),
            skinDeepShade = Color(0xFF9F7A62)
        )
        FighterVisualStyle.JARDIN -> FighterPalette(
            torsoLight = Color(0xFF2E7D4A),
            torsoDark = Color(0xFF133B22),
            sleeveLight = Color(0xFF33683E),
            sleeveDark = Color(0xFF18381E),
            armLight = Color(0xFF42803E),
            armDark = Color(0xFF234B22),
            gloveColor = Color(0xFF00E676),
            gloveShade = Color(0xFF00A152),
            pantsLight = Color(0xFF2E5431),
            pantsDark = Color(0xFF162C19),
            pantsDeepShadow = Color(0xFF0C1A0E),
            shoeColor = Color(0xFF4E342E),
            shoeTrim = Color(0xFF00C853),
            skinLight = Color(0xFF4B8A45),
            skinShade = Color(0xFF295226),
            skinDeepShade = Color(0xFF193617)
        )
        FighterVisualStyle.CHINOIS_BAGUETTES -> FighterPalette(
            torsoLight = Color(0xFFE03838),
            torsoDark = Color(0xFF8C0000),
            sleeveLight = Color(0xFFE03838),
            sleeveDark = Color(0xFF8C0000),
            armLight = Color(0xFFF4D5B9),
            armDark = Color(0xFFC8A080),
            gloveColor = Color(0xFFFFD600),
            gloveShade = Color(0xFFC79C00),
            pantsLight = Color(0xFF282B3B),
            pantsDark = Color(0xFF141621),
            pantsDeepShadow = Color(0xFF0B0C12),
            shoeColor = Color(0xFFF5F5F5),
            shoeTrim = Color(0xFFD50000),
            skinLight = Color(0xFFF4D5B9),
            skinShade = Color(0xFFC8A080),
            skinDeepShade = Color(0xFF9E775A)
        )
        FighterVisualStyle.DARON_BRESILIEN -> FighterPalette(
            torsoLight = Color(0xFFFFE23B),
            torsoDark = Color(0xFFD1A300),
            sleeveLight = Color(0xFFFFE23B),
            sleeveDark = Color(0xFFD1A300),
            armLight = Color(0xFFE2B28D),
            armDark = Color(0xFFB5825E),
            gloveColor = Color(0xFF00C853),
            gloveShade = Color(0xFF008E3A),
            pantsLight = Color(0xFF1E88E5),
            pantsDark = Color(0xFF0D47A1),
            pantsDeepShadow = Color(0xFF072B66),
            shoeColor = Color(0xFFFFD600),
            shoeTrim = Color.White,
            skinLight = Color(0xFFE2B28D),
            skinShade = Color(0xFFB5825E),
            skinDeepShade = Color(0xFF8C6040)
        )
    }
}
