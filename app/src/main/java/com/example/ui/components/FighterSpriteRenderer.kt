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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val InkOutline = Color(0xFF070910)

internal fun DrawScope.drawFighterReflection(
    f: FighterRuntime,
    arenaW: Float,
    arenaH: Float,
    groundY: Float
) {
    val cx = f.x * arenaW
    val bodyW = arenaH * 0.11f * f.def.stats.widthScale
    val alpha = (0.28f - f.y * 0.35f).coerceIn(0.05f, 0.28f)
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(f.def.primaryColor.copy(alpha = alpha), Color.Transparent),
            center = Offset(cx, groundY + arenaH * 0.025f),
            radius = bodyW * 1.15f
        ),
        topLeft = Offset(cx - bodyW * 1.05f, groundY + 2f),
        size = Size(bodyW * 2.1f, arenaH * 0.065f)
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
    val bodyH = arenaH * 0.28f * f.def.stats.heightScale
    val bodyW = arenaH * 0.12f * f.def.stats.widthScale

    // Ground roots gripping ankles
    val rootPath = Path().apply {
        moveTo(cx - bodyW * 0.8f, groundY + 2f)
        quadraticTo(cx - bodyW * 0.45f, footY - bodyH * 0.15f, cx, footY - bodyH * 0.08f)
        quadraticTo(cx + bodyW * 0.45f, footY - bodyH * 0.15f, cx + bodyW * 0.8f, groundY + 2f)
    }
    drawPath(rootPath, color = Color(0xFF0B3B1E), style = Stroke(width = 9f, cap = StrokeCap.Round))
    drawPath(rootPath, color = Color(0xFF00E676), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

    // Coiled glowing thorny vines around torso
    for (ring in 0..3) {
        val ry = footY - bodyH * (0.16f + ring * 0.19f)
        val sway = sin(timeMs * 0.022f + ring * 1.4f) * 3.5f
        val rectLeft = cx - bodyW * 0.72f + sway
        val rectW = bodyW * 1.44f
        val rectH = arenaH * 0.036f
        drawOval(
            color = Color(0xFF0C3B1E),
            topLeft = Offset(rectLeft, ry),
            size = Size(rectW, rectH),
            style = Stroke(width = 8f)
        )
        drawOval(
            color = Color(0xFF00E676),
            topLeft = Offset(rectLeft, ry),
            size = Size(rectW, rectH),
            style = Stroke(width = 4f)
        )
        // Little thorns on vine ring
        drawCircle(
            color = Color(0xFFFFC400),
            radius = 3.5f,
            center = Offset(rectLeft + rectW * (if (ring % 2 == 0) 0.15f else 0.85f), ry + rectH * 0.5f)
        )
    }
}

internal fun DrawScope.drawDetailedFighter(
    f: FighterRuntime,
    arenaW: Float,
    arenaH: Float,
    groundY: Float,
    timeMs: Long
) {
    val cx = f.x * arenaW
    val jumpOffset = f.y * (arenaH * 0.64f)
    val footY = groundY - jumpOffset

    val hScale = f.def.stats.heightScale
    val wScale = f.def.stats.widthScale
    val unit = arenaH * 0.29f * hScale
    val shoulderHalfW = unit * 0.21f * wScale
    val hipHalfW = unit * 0.14f * wScale

    // Contact shadow on floor
    val shadowScale = (1f - f.y * 1.35f).coerceIn(0.32f, 1f)
    drawOval(
        color = Color.Black.copy(alpha = 0.60f * shadowScale),
        topLeft = Offset(cx - shoulderHalfW * 1.55f * shadowScale, groundY - arenaH * 0.014f),
        size = Size(shoulderHalfW * 3.1f * shadowScale, arenaH * 0.032f)
    )

    // KO floored pose with spinning stars
    if (f.action == CombatAction.KO) {
        drawKoPose(f, cx, groundY, unit, wScale, timeMs)
        return
    }

    val dir = f.facing.toFloat()
    val isHit = f.action == CombatAction.HIT_STUN
    val flashWhite = isHit && (timeMs / 55L) % 2L == 0L
    val isSweeping = (f.action == CombatAction.BASE_ABILITY || f.action == CombatAction.ULTI) &&
        f.def.visualStyle == FighterVisualStyle.DARON_BRESILIEN

    val idleBob = if (f.action == CombatAction.IDLE) sin(timeMs / 160.0).toFloat() * (unit * 0.025f) else 0f
    val crouchDrop = when {
        isSweeping -> unit * 0.36f
        f.action == CombatAction.BLOCK -> unit * 0.08f
        f.chargingMegaPunch -> unit * 0.10f
        else -> 0f
    }

    val leanX = when {
        isSweeping -> -dir * unit * 0.16f
        f.action == CombatAction.CLASSIC || f.action == CombatAction.BASE_ABILITY -> dir * unit * 0.12f
        f.action == CombatAction.ULTI && !f.chargingMegaPunch -> dir * unit * 0.16f
        f.chargingMegaPunch -> -dir * unit * 0.12f
        f.action == CombatAction.WALK -> dir * unit * 0.07f
        isHit -> -dir * unit * 0.14f
        else -> dir * unit * 0.03f
    }

    val hipCenter = Offset(cx, footY - unit * 0.44f + idleBob + crouchDrop)
    val shoulderCenter = Offset(cx + leanX, hipCenter.y - unit * 0.31f)
    val headCenter = Offset(shoulderCenter.x + dir * unit * 0.02f, shoulderCenter.y - unit * 0.16f)

    // Palette by character
    val palette = fighterPalette(f.def.visualStyle, flashWhite)

    // Ulti ready aura behind fighter
    if (f.ultiMeter >= 100 || f.action == CombatAction.ULTI || f.chargingMegaPunch) {
        val pulse = 0.25f + 0.18f * sin(timeMs / 95.0).toFloat()
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(f.def.primaryColor.copy(alpha = pulse), Color.Transparent),
                center = Offset(cx + leanX * 0.4f, hipCenter.y - unit * 0.10f),
                radius = unit * 0.62f
            ),
            topLeft = Offset(cx - unit * 0.48f, headCenter.y - unit * 0.18f),
            size = Size(unit * 0.96f, unit * 1.10f)
        )
    }

    // Flowing scarves / headbands / cloak tails BEHIND body
    drawBackAccessories(f, shoulderCenter, headCenter, unit, dir, timeMs, flashWhite)

    // --- BACK LEG & FRONT LEG ---
    val walkPhase = sin(timeMs / 75.0).toFloat()
    val legThicknessTop = unit * 0.092f * (0.95f + 0.22f * (wScale - 1f))
    val legThicknessBot = legThicknessTop * 0.72f

    if (isSweeping) {
        // Authentic Capoeira / Street Fighter Balayette sweep pose!
        // Back/support leg bent tucked under hips
        val supportKnee = Offset(hipCenter.x - dir * unit * 0.18f, footY - unit * 0.14f)
        val supportAnkle = Offset(hipCenter.x - dir * unit * 0.26f, footY - unit * 0.03f)
        drawTaperedLimb(hipCenter, supportKnee, legThicknessTop, legThicknessBot, palette.pantsLight, palette.pantsDark)
        drawTaperedLimb(supportKnee, supportAnkle, legThicknessBot, legThicknessBot * 0.78f, palette.pantsLight, palette.pantsDark)
        drawFighterBoot(supportAnkle, -dir, unit, palette.shoeColor)

        // Extended sweeping front leg skimming the arena floor!
        val sweepExt = if (f.action == CombatAction.ULTI) unit * 0.78f else unit * 0.66f
        val sweepKnee = Offset(hipCenter.x + dir * sweepExt * 0.52f, footY - unit * 0.08f)
        val sweepAnkle = Offset(hipCenter.x + dir * sweepExt, footY - unit * 0.045f)
        drawTaperedLimb(hipCenter, sweepKnee, legThicknessTop, legThicknessBot, palette.pantsLight, palette.pantsDark)
        drawTaperedLimb(sweepKnee, sweepAnkle, legThicknessBot, legThicknessBot * 0.75f, palette.pantsLight, palette.pantsDark)
        drawFighterBoot(sweepAnkle, dir, unit * 1.05f, f.def.primaryColor)
    } else {
        val backHip = Offset(hipCenter.x - dir * hipHalfW * 0.5f, hipCenter.y)
        val frontHip = Offset(hipCenter.x + dir * hipHalfW * 0.5f, hipCenter.y)

        val (backKnee, backAnkle, frontKnee, frontAnkle) = when (f.action) {
            CombatAction.WALK -> {
                val stride = walkPhase * unit * 0.18f
                val liftBack = (-walkPhase).coerceAtLeast(0f) * unit * 0.06f
                val liftFront = walkPhase.coerceAtLeast(0f) * unit * 0.06f
                QuadOffsets(
                    Offset(backHip.x - stride * 0.5f + dir * unit * 0.04f, footY - unit * 0.22f - liftBack),
                    Offset(backHip.x - stride - dir * unit * 0.05f, footY - unit * 0.03f - liftBack),
                    Offset(frontHip.x + stride * 0.55f + dir * unit * 0.06f, footY - unit * 0.22f - liftFront),
                    Offset(frontHip.x + stride + dir * unit * 0.06f, footY - unit * 0.03f - liftFront)
                )
            }
            CombatAction.JUMP -> {
                QuadOffsets(
                    Offset(backHip.x - dir * unit * 0.08f, hipCenter.y + unit * 0.19f),
                    Offset(backHip.x - dir * unit * 0.14f, hipCenter.y + unit * 0.36f),
                    Offset(frontHip.x + dir * unit * 0.18f, hipCenter.y + unit * 0.11f),
                    Offset(frontHip.x + dir * unit * 0.10f, hipCenter.y + unit * 0.28f)
                )
            }
            else -> {
                // Classic Street Fighter wide stance
                val stanceSpread = unit * 0.17f
                QuadOffsets(
                    Offset(backHip.x - dir * stanceSpread * 0.55f, footY - unit * 0.21f),
                    Offset(backHip.x - dir * stanceSpread, footY - unit * 0.03f),
                    Offset(frontHip.x + dir * stanceSpread * 0.85f, footY - unit * 0.21f),
                    Offset(frontHip.x + dir * stanceSpread * 0.95f, footY - unit * 0.03f)
                )
            }
        }

        // Back leg (slightly darker for depth)
        drawTaperedLimb(backHip, backKnee, legThicknessTop, legThicknessBot, palette.pantsDark, palette.pantsDark)
        drawTaperedLimb(backKnee, backAnkle, legThicknessBot, legThicknessBot * 0.78f, palette.pantsDark, palette.pantsDark)
        drawFighterBoot(backAnkle, dir, unit, palette.shoeColor)

        // Front leg
        drawTaperedLimb(frontHip, frontKnee, legThicknessTop, legThicknessBot, palette.pantsLight, palette.pantsDark)
        drawTaperedLimb(frontKnee, frontAnkle, legThicknessBot, legThicknessBot * 0.78f, palette.pantsLight, palette.pantsDark)
        drawFighterBoot(frontAnkle, dir, unit, palette.shoeColor)
    }

    // --- BACK ARM (retracted guard or support hand during sweep) ---
    val armThickness = unit * 0.072f * (0.95f + 0.20f * (wScale - 1f))
    val backShoulder = Offset(shoulderCenter.x - dir * shoulderHalfW * 0.72f, shoulderCenter.y + unit * 0.02f)
    val frontShoulder = Offset(shoulderCenter.x + dir * shoulderHalfW * 0.72f, shoulderCenter.y + unit * 0.02f)

    if (isSweeping) {
        // Support arm planted on arena floor
        val plantHand = Offset(cx - dir * unit * 0.28f, footY - unit * 0.04f)
        val supportElbow = Offset((backShoulder.x + plantHand.x) * 0.5f - dir * unit * 0.04f, (backShoulder.y + plantHand.y) * 0.5f)
        drawTaperedLimb(backShoulder, supportElbow, armThickness, armThickness * 0.85f, palette.sleeveDark, palette.sleeveDark)
        drawTaperedLimb(supportElbow, plantHand, armThickness * 0.85f, armThickness * 0.74f, palette.skinShade, palette.skinShade)
    } else {
        val backElbow = Offset(backShoulder.x - dir * unit * 0.06f, backShoulder.y + unit * 0.14f)
        val backHand = when (f.action) {
            CombatAction.BLOCK -> Offset(frontShoulder.x + dir * unit * 0.12f, headCenter.y + unit * 0.02f)
            else -> Offset(backShoulder.x + dir * unit * 0.12f, shoulderCenter.y + unit * 0.06f)
        }
        drawTaperedLimb(backShoulder, backElbow, armThickness, armThickness * 0.86f, palette.sleeveDark, palette.sleeveDark)
        drawTaperedLimb(backElbow, backHand, armThickness * 0.86f, armThickness * 0.74f, palette.armDark, palette.armDark)
        drawArmoredFist(backHand, armThickness * 1.02f, palette.gloveColor, f.def.primaryColor)
    }

    // --- CEL-SHADED TORSO & OUTFIT ---
    drawCharacterTorso(
        f = f,
        shoulderCenter = shoulderCenter,
        hipCenter = hipCenter,
        shoulderHalfW = shoulderHalfW,
        hipHalfW = hipHalfW,
        unit = unit,
        dir = dir,
        palette = palette,
        timeMs = timeMs
    )

    // --- HEAD, FACE, HAIR, VISOR / GLASSES / LEAF CROWN ---
    drawCharacterHead(
        f = f,
        headCenter = headCenter,
        unit = unit,
        dir = dir,
        palette = palette,
        timeMs = timeMs
    )

    // --- FRONT ATTACKING / GUARD ARM & WEAPON ---
    val (frontElbow, frontHand) = when {
        isSweeping -> {
            val e = Offset(frontShoulder.x - dir * unit * 0.10f, frontShoulder.y + unit * 0.06f)
            val h = Offset(e.x - dir * unit * 0.12f, e.y - unit * 0.06f)
            e to h
        }
        f.chargingMegaPunch -> {
            // Pulled back coiled power punch!
            val e = Offset(frontShoulder.x - dir * unit * 0.18f, frontShoulder.y + unit * 0.05f)
            val h = Offset(frontShoulder.x - dir * unit * 0.28f, frontShoulder.y - unit * 0.04f)
            e to h
        }
        f.action == CombatAction.CLASSIC -> {
            val e = Offset(frontShoulder.x + dir * unit * 0.18f, frontShoulder.y + unit * 0.01f)
            val h = Offset(frontShoulder.x + dir * unit * 0.42f, frontShoulder.y - unit * 0.02f)
            e to h
        }
        f.action == CombatAction.BASE_ABILITY -> {
            val e = Offset(frontShoulder.x + dir * unit * 0.22f, frontShoulder.y - unit * 0.02f)
            val h = Offset(frontShoulder.x + dir * unit * 0.48f, frontShoulder.y - unit * 0.07f)
            e to h
        }
        f.action == CombatAction.ULTI -> {
            val e = Offset(frontShoulder.x + dir * unit * 0.24f, frontShoulder.y - unit * 0.05f)
            val h = Offset(frontShoulder.x + dir * unit * 0.52f, frontShoulder.y - unit * 0.12f)
            e to h
        }
        f.action == CombatAction.BLOCK -> {
            val e = Offset(frontShoulder.x + dir * unit * 0.14f, frontShoulder.y + unit * 0.07f)
            val h = Offset(frontShoulder.x + dir * unit * 0.18f, headCenter.y + unit * 0.04f)
            e to h
        }
        else -> {
            val e = Offset(frontShoulder.x + dir * unit * 0.06f, frontShoulder.y + unit * 0.14f)
            val h = Offset(frontShoulder.x + dir * unit * 0.22f, shoulderCenter.y + unit * 0.03f)
            e to h
        }
    }

    drawTaperedLimb(frontShoulder, frontElbow, armThickness, armThickness * 0.88f, palette.sleeveLight, palette.sleeveDark)
    drawTaperedLimb(frontElbow, frontHand, armThickness * 0.88f, armThickness * 0.78f, palette.armLight, palette.armDark)
    drawArmoredFist(frontHand, armThickness * 1.15f, palette.gloveColor, f.def.accentColor)

    // Jardin Colossal Punch swirling power vortex on fist while charging!
    if (f.chargingMegaPunch) {
        val pulse = 1f + 0.18f * sin(timeMs * 0.035f)
        val auraR = unit * 0.36f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFD600), Color(0xFF00E676), Color.Transparent),
                center = frontHand,
                radius = auraR
            ),
            radius = auraR,
            center = frontHand
        )
        // Spinning energy ring
        val rot = (timeMs * 0.6f) % 360f
        drawArc(
            color = Color(0xFFFFD600),
            startAngle = rot,
            sweepAngle = 210f,
            useCenter = false,
            topLeft = Offset(frontHand.x - auraR * 0.75f, frontHand.y - auraR * 0.75f),
            size = Size(auraR * 1.5f, auraR * 1.5f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
    }

    // Maître Chen's twin golden & jade chopsticks!
    if (f.def.visualStyle == FighterVisualStyle.CHINOIS_BAGUETTES) {
        val attacking = f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)
        val stickLen = if (attacking) unit * 0.44f else unit * 0.28f
        val tip1 = Offset(frontHand.x + dir * stickLen, frontHand.y - unit * 0.06f)
        val tip2 = Offset(frontHand.x + dir * stickLen * 0.94f, frontHand.y + unit * 0.05f)
        // Dark outline + glowing stick
        drawLine(InkOutline, frontHand, tip1, strokeWidth = 7f, cap = StrokeCap.Round)
        drawLine(Color(0xFFFFD600), frontHand, tip1, strokeWidth = 3.8f, cap = StrokeCap.Round)
        drawLine(InkOutline, frontHand, tip2, strokeWidth = 6.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFF00E676), frontHand, tip2, strokeWidth = 3.5f, cap = StrokeCap.Round)
    }

    // Hexagonal/Arc Guard Shield
    if (f.isBlocking) {
        val shieldX = cx + dir * unit * 0.25f
        val shieldTop = headCenter.y - unit * 0.05f
        val shieldH = unit * 0.66f
        val shieldPath = Path().apply {
            moveTo(shieldX, shieldTop)
            lineTo(shieldX + dir * unit * 0.14f, shieldTop + shieldH * 0.22f)
            lineTo(shieldX + dir * unit * 0.14f, shieldTop + shieldH * 0.78f)
            lineTo(shieldX, shieldTop + shieldH)
        }
        drawPath(shieldPath, color = GuardBlue.copy(alpha = 0.22f))
        drawPath(shieldPath, color = GuardBlue, style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
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
    val bodyLen = unit * 0.72f
    val thickness = unit * 0.16f * wScale
    // Lying horizontally on floor
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
        style = Stroke(width = 3.2f)
    )
    // Head resting on left/right
    val headX = cx - f.facing * bodyLen * 0.45f
    val headY = groundY - thickness * 0.75f
    drawCircle(color = palette.skinLight, radius = unit * 0.11f, center = Offset(headX, headY))
    drawCircle(color = InkOutline, radius = unit * 0.11f, center = Offset(headX, headY), style = Stroke(width = 3f))

    // Spinning KO stars above head
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
    val wave = sin(timeMs * 0.012f) * unit * 0.045f
    when (f.def.visualStyle) {
        FighterVisualStyle.GLITCHERMAN -> {
            // Flowing cyber-neon scarf ribbon
            val scarf = Path().apply {
                moveTo(shoulderCenter.x - dir * unit * 0.05f, shoulderCenter.y - unit * 0.02f)
                quadraticTo(
                    shoulderCenter.x - dir * unit * 0.25f,
                    shoulderCenter.y + unit * 0.04f + wave,
                    shoulderCenter.x - dir * unit * 0.40f,
                    shoulderCenter.y + unit * 0.12f - wave
                )
                lineTo(shoulderCenter.x - dir * unit * 0.34f, shoulderCenter.y + unit * 0.18f - wave)
                quadraticTo(
                    shoulderCenter.x - dir * unit * 0.20f,
                    shoulderCenter.y + unit * 0.10f + wave,
                    shoulderCenter.x - dir * unit * 0.02f,
                    shoulderCenter.y + unit * 0.04f
                )
                close()
            }
            drawPath(scarf, brush = Brush.horizontalGradient(listOf(Color(0xFFFF2A85), Color(0xFF00E5FF))))
            drawPath(scarf, color = InkOutline, style = Stroke(width = 2.5f))
        }
        FighterVisualStyle.CHINOIS_BAGUETTES -> {
            // Fluttering martial headband tails
            val band = Path().apply {
                moveTo(headCenter.x - dir * unit * 0.06f, headCenter.y - unit * 0.04f)
                quadraticTo(
                    headCenter.x - dir * unit * 0.24f,
                    headCenter.y - unit * 0.02f + wave,
                    headCenter.x - dir * unit * 0.36f,
                    headCenter.y + unit * 0.08f - wave
                )
                lineTo(headCenter.x - dir * unit * 0.32f, headCenter.y + unit * 0.13f - wave)
                quadraticTo(
                    headCenter.x - dir * unit * 0.18f,
                    headCenter.y + unit * 0.04f + wave,
                    headCenter.x - dir * unit * 0.05f,
                    headCenter.y
                )
                close()
            }
            drawPath(band, color = Color(0xFFD50000))
            drawPath(band, color = InkOutline, style = Stroke(width = 2.2f))
        }
        FighterVisualStyle.JARDIN -> {
            // Rugged mossy cape/leaf mantle
            val cape = Path().apply {
                moveTo(shoulderCenter.x - dir * unit * 0.12f, shoulderCenter.y)
                lineTo(shoulderCenter.x - dir * unit * 0.28f, shoulderCenter.y + unit * 0.28f + wave * 0.5f)
                lineTo(shoulderCenter.x - dir * unit * 0.08f, shoulderCenter.y + unit * 0.32f)
                close()
            }
            drawPath(cape, color = Color(0xFF133A22))
            drawPath(cape, color = InkOutline, style = Stroke(width = 2.8f))
        }
        FighterVisualStyle.DARON_BRESILIEN -> {
            // Fluttering back edge of open summer jacket
            val flap = Path().apply {
                moveTo(shoulderCenter.x - dir * unit * 0.14f, shoulderCenter.y + unit * 0.06f)
                lineTo(shoulderCenter.x - dir * unit * 0.24f, shoulderCenter.y + unit * 0.34f + wave * 0.5f)
                lineTo(shoulderCenter.x - dir * unit * 0.08f, shoulderCenter.y + unit * 0.34f)
                close()
            }
            drawPath(flap, color = Color(0xFFE0B400))
            drawPath(flap, color = InkOutline, style = Stroke(width = 2.5f))
        }
    }
}

private fun DrawScope.drawCharacterTorso(
    f: FighterRuntime,
    shoulderCenter: Offset,
    hipCenter: Offset,
    shoulderHalfW: Float,
    hipHalfW: Float,
    unit: Float,
    dir: Float,
    palette: FighterPalette,
    timeMs: Long
) {
    val torsoPath = Path().apply {
        moveTo(shoulderCenter.x - shoulderHalfW, shoulderCenter.y)
        lineTo(shoulderCenter.x + shoulderHalfW, shoulderCenter.y)
        lineTo(hipCenter.x + hipHalfW, hipCenter.y + unit * 0.03f)
        lineTo(hipCenter.x - hipHalfW, hipCenter.y + unit * 0.03f)
        close()
    }
    drawPath(
        path = torsoPath,
        brush = Brush.horizontalGradient(
            colors = if (dir > 0) listOf(palette.torsoDark, palette.torsoLight) else listOf(palette.torsoLight, palette.torsoDark),
            startX = shoulderCenter.x - shoulderHalfW,
            endX = shoulderCenter.x + shoulderHalfW
        )
    )
    drawPath(torsoPath, color = InkOutline, style = Stroke(width = 3.5f, join = StrokeJoin.Round))

    when (f.def.visualStyle) {
        FighterVisualStyle.GLITCHERMAN -> {
            // High cyber-hoodie collar + neon circuit chest trim
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(shoulderCenter.x - shoulderHalfW * 0.55f, shoulderCenter.y + unit * 0.06f),
                end = Offset(hipCenter.x + hipHalfW * 0.55f, hipCenter.y - unit * 0.04f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFFF2A85),
                start = Offset(shoulderCenter.x + shoulderHalfW * 0.45f, shoulderCenter.y + unit * 0.08f),
                end = Offset(hipCenter.x - hipHalfW * 0.40f, hipCenter.y - unit * 0.06f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        FighterVisualStyle.JARDIN -> {
            // Massive living-bark shoulder armor & glowing emerald-gold chest core
            val pauldronL = Offset(shoulderCenter.x - shoulderHalfW * 0.9f, shoulderCenter.y + unit * 0.02f)
            val pauldronR = Offset(shoulderCenter.x + shoulderHalfW * 0.9f, shoulderCenter.y + unit * 0.02f)
            drawCircle(color = Color(0xFF2E5C38), radius = unit * 0.09f, center = pauldronL)
            drawCircle(color = InkOutline, radius = unit * 0.09f, center = pauldronL, style = Stroke(width = 3f))
            drawCircle(color = Color(0xFF2E5C38), radius = unit * 0.09f, center = pauldronR)
            drawCircle(color = InkOutline, radius = unit * 0.09f, center = pauldronR, style = Stroke(width = 3f))

            val coreCenter = Offset((shoulderCenter.x + hipCenter.x) * 0.5f + dir * unit * 0.03f, shoulderCenter.y + unit * 0.14f)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, Color(0xFFFFC400), Color(0xFF00E676))),
                radius = unit * 0.062f,
                center = coreCenter
            )
        }
        FighterVisualStyle.CHINOIS_BAGUETTES -> {
            // Gold silk lapel trim & fluttering waist sash
            drawLine(
                color = Color(0xFFFFD600),
                start = Offset(shoulderCenter.x, shoulderCenter.y + unit * 0.02f),
                end = Offset(hipCenter.x + dir * hipHalfW * 0.35f, hipCenter.y),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            // Gold waist belt
            drawRoundRect(
                color = Color(0xFF14161F),
                topLeft = Offset(hipCenter.x - hipHalfW * 1.04f, hipCenter.y - unit * 0.035f),
                size = Size(hipHalfW * 2.08f, unit * 0.055f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
        FighterVisualStyle.DARON_BRESILIEN -> {
            // Open summer jacket chest V + gleaming gold medallion + green tropical leaf accents
            val chestV = Path().apply {
                moveTo(shoulderCenter.x - shoulderHalfW * 0.25f, shoulderCenter.y)
                lineTo(shoulderCenter.x + dir * unit * 0.02f, hipCenter.y - unit * 0.04f)
                lineTo(shoulderCenter.x + shoulderHalfW * 0.42f, shoulderCenter.y)
                close()
            }
            drawPath(chestV, color = palette.skinLight)
            drawPath(chestV, color = InkOutline, style = Stroke(width = 2.5f))
            // Gold necklace medallion
            drawCircle(
                color = Color(0xFFFFD600),
                radius = unit * 0.034f,
                center = Offset(shoulderCenter.x + dir * unit * 0.02f, shoulderCenter.y + unit * 0.13f)
            )
            // Green tropical leaf graphics on summer jacket
            drawLine(
                color = Color(0xFF00C853),
                start = Offset(shoulderCenter.x - shoulderHalfW * 0.72f, shoulderCenter.y + unit * 0.08f),
                end = Offset(hipCenter.x - hipHalfW * 0.50f, hipCenter.y - unit * 0.05f),
                strokeWidth = 5.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF00C853),
                start = Offset(shoulderCenter.x + shoulderHalfW * 0.72f, shoulderCenter.y + unit * 0.09f),
                end = Offset(hipCenter.x + hipHalfW * 0.56f, hipCenter.y - unit * 0.05f),
                strokeWidth = 5.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawCharacterHead(
    f: FighterRuntime,
    headCenter: Offset,
    unit: Float,
    dir: Float,
    palette: FighterPalette,
    timeMs: Long
) {
    val headR = unit * (if (f.def.visualStyle == FighterVisualStyle.JARDIN) 0.118f else 0.105f)
    // Head circle with cel-shaded gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.skinLight, palette.skinShade),
            center = Offset(headCenter.x + dir * headR * 0.25f, headCenter.y - headR * 0.2f),
            radius = headR * 1.4f
        ),
        radius = headR,
        center = headCenter
    )
    drawCircle(color = InkOutline, radius = headR, center = headCenter, style = Stroke(width = 3.2f))

    when (f.def.visualStyle) {
        FighterVisualStyle.GLITCHERMAN -> {
            // Spiky anime cyber-hair
            val hair = Path().apply {
                moveTo(headCenter.x - headR * 0.95f, headCenter.y - headR * 0.1f)
                lineTo(headCenter.x - headR * 1.1f, headCenter.y - headR * 0.95f)
                lineTo(headCenter.x - headR * 0.25f, headCenter.y - headR * 0.72f)
                lineTo(headCenter.x + dir * headR * 0.25f, headCenter.y - headR * 1.25f)
                lineTo(headCenter.x + headR * 0.85f, headCenter.y - headR * 0.65f)
                lineTo(headCenter.x + headR * 0.95f, headCenter.y - headR * 0.15f)
                close()
            }
            drawPath(hair, color = Color(0xFF141A2E))
            drawPath(hair, color = InkOutline, style = Stroke(width = 2.6f))
            // Glowing magenta-cyan holographic visor!
            val visorL = headCenter.x + (if (dir > 0) -headR * 0.12f else -headR * 0.96f)
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFFFF2A85), Color(0xFF00E5FF))),
                topLeft = Offset(visorL, headCenter.y - headR * 0.34f),
                size = Size(headR * 1.08f, headR * 0.44f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.75f),
                topLeft = Offset(visorL + headR * 0.12f, headCenter.y - headR * 0.22f),
                size = Size(headR * 0.82f, headR * 0.10f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }

        FighterVisualStyle.JARDIN -> {
            // Leaf crown horns
            val crown = Path().apply {
                moveTo(headCenter.x - headR * 0.9f, headCenter.y - headR * 0.45f)
                lineTo(headCenter.x - headR * 1.15f, headCenter.y - headR * 1.30f)
                lineTo(headCenter.x - headR * 0.2f, headCenter.y - headR * 0.85f)
                lineTo(headCenter.x + headR * 0.15f, headCenter.y - headR * 1.38f)
                lineTo(headCenter.x + headR * 0.95f, headCenter.y - headR * 0.50f)
                close()
            }
            drawPath(crown, color = Color(0xFF00C853))
            drawPath(crown, color = InkOutline, style = Stroke(width = 2.8f))
            // Glowing golden amber eye
            drawCircle(
                color = Color(0xFFFFC400),
                radius = headR * 0.24f,
                center = Offset(headCenter.x + dir * headR * 0.42f, headCenter.y - headR * 0.12f)
            )
        }

        FighterVisualStyle.CHINOIS_BAGUETTES -> {
            // High black hair bun + sharp hair cap
            drawCircle(color = Color(0xFF11131B), radius = headR * 0.42f, center = Offset(headCenter.x - dir * headR * 0.1f, headCenter.y - headR * 1.18f))
            drawArc(
                color = Color(0xFF11131B),
                startAngle = 175f,
                sweepAngle = 190f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headR * 1.02f, headCenter.y - headR * 1.08f),
                size = Size(headR * 2.04f, headR * 1.15f)
            )
            // Crimson headband strip
            drawRect(
                color = Color(0xFFD50000),
                topLeft = Offset(headCenter.x - headR * 0.95f, headCenter.y - headR * 0.48f),
                size = Size(headR * 1.90f, headR * 0.22f)
            )
            // Focused white/dark eye
            drawCircle(
                color = Color(0xFF10131C),
                radius = headR * 0.15f,
                center = Offset(headCenter.x + dir * headR * 0.46f, headCenter.y - headR * 0.08f)
            )
        }

        FighterVisualStyle.DARON_BRESILIEN -> {
            // Slicked dark hair
            drawArc(
                color = Color(0xFF1E1714),
                startAngle = 185f,
                sweepAngle = 170f,
                useCenter = true,
                topLeft = Offset(headCenter.x - headR * 1.02f, headCenter.y - headR * 1.05f),
                size = Size(headR * 2.04f, headR * 1.05f)
            )
            // Cool dark aviator sunglasses with white reflection
            val glassL = headCenter.x + (if (dir > 0) -headR * 0.08f else -headR * 0.92f)
            drawRoundRect(
                color = Color(0xFF0E1118),
                topLeft = Offset(glassL, headCenter.y - headR * 0.34f),
                size = Size(headR * 1.00f, headR * 0.42f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.65f),
                start = Offset(glassL + headR * 0.15f, headCenter.y - headR * 0.26f),
                end = Offset(glassL + headR * 0.36f, headCenter.y - headR * 0.02f),
                strokeWidth = 2.5f
            )
            // Lush handlebar mustache
            drawArc(
                color = Color(0xFF241610),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(headCenter.x + dir * headR * 0.05f - headR * 0.42f, headCenter.y + headR * 0.22f),
                size = Size(headR * 0.84f, headR * 0.36f),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun DrawScope.drawTaperedLimb(
    start: Offset,
    end: Offset,
    rStart: Float,
    rEnd: Float,
    fillLight: Color,
    fillDark: Color
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux

    val path = Path().apply {
        moveTo(start.x + px * rStart, start.y + py * rStart)
        lineTo(end.x + px * rEnd, end.y + py * rEnd)
        quadraticTo(end.x + ux * rEnd * 1.15f, end.y + uy * rEnd * 1.15f, end.x - px * rEnd, end.y - py * rEnd)
        lineTo(start.x - px * rStart, start.y - py * rStart)
        quadraticTo(start.x - ux * rStart * 1.15f, start.y - uy * rStart * 1.15f, start.x + px * rStart, start.y + py * rStart)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(fillLight, fillDark),
            start = Offset(start.x + px * rStart, start.y + py * rStart),
            end = Offset(end.x - px * rEnd, end.y - py * rEnd)
        )
    )
    drawPath(path, color = InkOutline, style = Stroke(width = 3.0f, join = StrokeJoin.Round))
}

private fun DrawScope.drawArmoredFist(
    center: Offset,
    radius: Float,
    fill: Color,
    highlight: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(highlight, fill),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
            radius = radius * 1.3f
        ),
        radius = radius,
        center = center
    )
    drawCircle(color = InkOutline, radius = radius, center = center, style = Stroke(width = 3.0f))
}

private fun DrawScope.drawFighterBoot(
    ankle: Offset,
    dir: Float,
    unit: Float,
    shoeColor: Color
) {
    val w = unit * 0.135f
    val h = unit * 0.068f
    val left = if (dir > 0) ankle.x - w * 0.28f else ankle.x - w * 0.72f
    drawRoundRect(
        color = shoeColor,
        topLeft = Offset(left, ankle.y - h * 0.35f),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = InkOutline,
        topLeft = Offset(left, ankle.y - h * 0.35f),
        size = Size(w, h),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.8f)
    )
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
    val pantsLight: Color,
    val pantsDark: Color,
    val shoeColor: Color,
    val skinLight: Color,
    val skinShade: Color
)

private fun fighterPalette(style: FighterVisualStyle, flashWhite: Boolean): FighterPalette {
    if (flashWhite) {
        val w = Color.White
        return FighterPalette(w, w, w, w, w, w, w, w, w, w, w, w)
    }
    return when (style) {
        FighterVisualStyle.GLITCHERMAN -> FighterPalette(
            torsoLight = Color(0xFF223052),
            torsoDark = Color(0xFF12182C),
            sleeveLight = Color(0xFF223052),
            sleeveDark = Color(0xFF141B30),
            armLight = Color(0xFF1A223B),
            armDark = Color(0xFF101526),
            gloveColor = Color(0xFF00E5FF),
            pantsLight = Color(0xFF1C2338),
            pantsDark = Color(0xFF0E121F),
            shoeColor = Color(0xFFFF2A85),
            skinLight = Color(0xFFF2D8C4),
            skinShade = Color(0xFFC9A68E)
        )
        FighterVisualStyle.JARDIN -> FighterPalette(
            torsoLight = Color(0xFF276B3F),
            torsoDark = Color(0xFF143820),
            sleeveLight = Color(0xFF2D5935),
            sleeveDark = Color(0xFF18361E),
            armLight = Color(0xFF3E7539),
            armDark = Color(0xFF234A22),
            gloveColor = Color(0xFF00E676),
            pantsLight = Color(0xFF2B4D2E),
            pantsDark = Color(0xFF172B19),
            shoeColor = Color(0xFF4E342E),
            skinLight = Color(0xFF478241),
            skinShade = Color(0xFF274D24)
        )
        FighterVisualStyle.CHINOIS_BAGUETTES -> FighterPalette(
            torsoLight = Color(0xFFD32F2F),
            torsoDark = Color(0xFF8E0000),
            sleeveLight = Color(0xFFD32F2F),
            sleeveDark = Color(0xFF8E0000),
            armLight = Color(0xFFF3D4B8),
            armDark = Color(0xFFC8A182),
            gloveColor = Color(0xFFFFD600),
            pantsLight = Color(0xFF252836),
            pantsDark = Color(0xFF12141C),
            shoeColor = Color(0xFFF5F5F5),
            skinLight = Color(0xFFF3D4B8),
            skinShade = Color(0xFFC8A182)
        )
        FighterVisualStyle.DARON_BRESILIEN -> FighterPalette(
            torsoLight = Color(0xFFFFE033),
            torsoDark = Color(0xFFD4A700),
            sleeveLight = Color(0xFFFFE033),
            sleeveDark = Color(0xFFD4A700),
            armLight = Color(0xFFE0B08A),
            armDark = Color(0xFFB5825E),
            gloveColor = Color(0xFF00C853),
            pantsLight = Color(0xFF1E88E5),
            pantsDark = Color(0xFF0D47A1),
            shoeColor = Color(0xFFFFD600),
            skinLight = Color(0xFFE0B08A),
            skinShade = Color(0xFFB5825E)
        )
    }
}
