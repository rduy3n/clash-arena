package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterVisualStyle
import com.example.game.engine.FighterRuntime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% animated 60 FPS end cinematics featuring each fighter full-body
 * performing their choreographed victory action (no static images).
 */
@Composable
fun VictoryChoreographyScene(
    winner: FighterDefinition,
    elapsedMs: Long,
    modifier: Modifier = Modifier
) {
    val runtime = remember(winner.id) {
        FighterRuntime(side = 1, def = winner, x = 0.5f, facing = 1, hp = winner.stats.maxHp)
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        when (winner.visualStyle) {
            FighterVisualStyle.CHINOIS_BAGUETTES -> drawChenSamuraiNoodlesScene(runtime, elapsedMs)
            FighterVisualStyle.JARDIN -> drawJardinCameraSmashScene(runtime, elapsedMs)
            FighterVisualStyle.DARON_BRESILIEN -> drawDaronBeachCoconutScene(runtime, elapsedMs)
            FighterVisualStyle.GLITCHERMAN -> drawGlitchermanPixelShatterScene(runtime, elapsedMs)
        }
    }
}

private fun DrawScope.drawChenSamuraiNoodlesScene(f: FighterRuntime, elapsedMs: Long) {
    drawSamuraiTempleBackdrop(elapsedMs)

    val w = size.width
    val h = size.height
    val groundY = h * 0.78f
    val unit = h * 0.44f
    val cx = w * 0.50f
    val dir = 1f
    val palette = fighterPalette(f.def.visualStyle, false)

    // Phase 1 (0..900ms): Chopstick spin flourish
    // Phase 2 (900..1700ms): Dip into steaming ramen bowl
    // Phase 3 (1700ms+): Continuous noodle-lifting & eating loop
    val tSec = elapsedMs / 1000f
    val eatCycle = if (tSec > 1.7f) ((tSec - 1.7f) % 1.4f) / 1.4f else 0f
    val eatBob = if (tSec > 1.7f) sin(eatCycle * 2f * PI.toFloat()) * 0.5f + 0.5f else 0f
    val crouch = if (tSec in 0.9f..1.7f) 0.06f else 0.02f

    // Ground contact shadow
    drawOval(
        color = Color.Black.copy(alpha = 0.58f),
        topLeft = Offset(cx - unit * 0.34f, groundY - 8f),
        size = Size(unit * 0.68f, 18f)
    )

    val hipCenter = Offset(cx, groundY - unit * (0.43f - crouch))
    val waistCenter = Offset(cx, hipCenter.y - unit * 0.11f)
    val shoulderCenter = Offset(cx + unit * 0.02f, hipCenter.y - unit * 0.31f)
    val neckBase = Offset(shoulderCenter.x + unit * 0.01f, shoulderCenter.y - unit * 0.03f)
    val headTilt = if (tSec > 1.7f) -eatBob * unit * 0.015f else 0f
    val headCenter = Offset(shoulderCenter.x + unit * 0.03f, shoulderCenter.y - unit * 0.165f + headTilt)

    drawBackAccessories(f, shoulderCenter, headCenter, unit, dir, elapsedMs, flashWhite = false)

    // Heroic martial stance legs
    val thighR = unit * 0.092f
    val kneeR = thighR * 0.70f
    val ankleR = thighR * 0.50f
    val bHip = Offset(hipCenter.x - unit * 0.09f, hipCenter.y)
    val bKnee = Offset(bHip.x - unit * 0.12f, groundY - unit * 0.21f)
    val bAnk = Offset(bHip.x - unit * 0.18f, groundY - unit * 0.03f)
    drawAnatomicalLimb(bHip, bKnee, thighR, kneeR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
    drawAnatomicalLimb(bKnee, bAnk, kneeR, ankleR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
    drawDetailedBoot(bAnk, dir, unit, palette.shoeColor, palette.shoeTrim)

    val fHip = Offset(hipCenter.x + unit * 0.09f, hipCenter.y)
    val fKnee = Offset(fHip.x + unit * 0.14f, groundY - unit * 0.21f)
    val fAnk = Offset(fHip.x + unit * 0.18f, groundY - unit * 0.03f)
    drawAnatomicalLimb(fHip, fKnee, thighR, kneeR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.35f))
    drawAnatomicalLimb(fKnee, fAnk, kneeR, ankleR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.35f))
    drawDetailedBoot(fAnk, dir, unit, palette.shoeColor, palette.shoeTrim)

    val bicepR = unit * 0.070f
    val forearmR = bicepR * 0.82f
    val wristR = bicepR * 0.64f

    // Back arm holds the steaming ramen bowl comfortably forward
    val backShoulder = Offset(shoulderCenter.x - unit * 0.14f, shoulderCenter.y + unit * 0.02f)
    val bowlHand = Offset(cx + unit * 0.22f, shoulderCenter.y + unit * 0.14f)
    val backElbow = Offset(cx + unit * 0.02f, shoulderCenter.y + unit * 0.22f)
    drawAnatomicalLimb(backShoulder, backElbow, bicepR, forearmR, palette.sleeveDark, palette.pantsDeepShadow, dir, null)
    drawAnatomicalLimb(backElbow, bowlHand, forearmR, wristR, palette.armDark, palette.skinShade, dir, null)

    drawSculptedTorso(
        f = f,
        shoulderCenter = shoulderCenter,
        waistCenter = waistCenter,
        hipCenter = hipCenter,
        shoulderHalfW = unit * 0.215f,
        waistHalfW = unit * 0.130f,
        hipHalfW = unit * 0.145f,
        unit = unit,
        dir = dir,
        palette = palette,
        flashWhite = false
    )

    drawAnatomicalLimb(neckBase, headCenter, unit * 0.050f, unit * 0.042f, palette.skinShade, palette.skinDeepShade, dir, null)
    drawAnimeHead(f, headCenter, unit, dir, palette, elapsedMs, flashWhite = false)

    // Animated happy eating mouth expression during Phase 3
    if (tSec > 1.7f) {
        val mouthOpen = 0.3f + 0.7f * sin(eatCycle * 4f * PI.toFloat()).coerceAtLeast(0f)
        val mouthCenter = Offset(headCenter.x + unit * 0.052f, headCenter.y + unit * 0.056f)
        drawOval(
            color = Color(0xFF880E4F),
            topLeft = Offset(mouthCenter.x - 6f, mouthCenter.y - 3f * mouthOpen),
            size = Size(12f, 7f * mouthOpen + 2f)
        )
    }

    // Ramen bowl held up in front
    val bowlCenter = Offset(bowlHand.x + unit * 0.04f, bowlHand.y - unit * 0.03f)
    val bowlR = unit * 0.165f

    // Front arm & Chopsticks animation across the 3 phases
    val frontShoulder = Offset(shoulderCenter.x + unit * 0.14f, shoulderCenter.y + unit * 0.02f)
    val mouthTarget = Offset(headCenter.x + unit * 0.09f, headCenter.y + unit * 0.055f)
    val bowlDipTarget = Offset(bowlCenter.x, bowlCenter.y - bowlR * 0.35f)

    val activeHandPos: Offset
    val stickEnd: Offset
    val noodleLiftFrac: Float
    when {
        tSec < 0.9f -> {
            // Phase 1: Dramatic chopstick twirl
            val ang = elapsedMs * 0.022f
            activeHandPos = Offset(frontShoulder.x + unit * 0.20f, shoulderCenter.y - unit * 0.04f)
            stickEnd = Offset(activeHandPos.x + cos(ang) * unit * 0.24f, activeHandPos.y + sin(ang) * unit * 0.24f)
            noodleLiftFrac = 0f
            drawCircle(
                color = Color(0xFFFFD600).copy(alpha = 0.42f),
                radius = unit * 0.22f,
                center = activeHandPos,
                style = Stroke(width = 5f)
            )
        }
        tSec < 1.7f -> {
            // Phase 2: Dip chopsticks cleanly into ramen broth
            val p = ((tSec - 0.9f) / 0.8f).coerceIn(0f, 1f)
            activeHandPos = Offset(
                frontShoulder.x + unit * 0.20f + (bowlDipTarget.x - frontShoulder.x - unit * 0.14f) * p,
                shoulderCenter.y - unit * 0.04f + (bowlDipTarget.y - shoulderCenter.y - unit * 0.12f) * p
            )
            stickEnd = bowlDipTarget
            noodleLiftFrac = p * 0.25f
        }
        else -> {
            // Phase 3: Lift noodles from bowl up to mouth & slurp!
            val liftCurve = sin(eatCycle * PI.toFloat()).coerceIn(0f, 1f)
            val targetTip = Offset(
                bowlDipTarget.x + (mouthTarget.x - bowlDipTarget.x) * liftCurve,
                bowlDipTarget.y + (mouthTarget.y - bowlDipTarget.y) * liftCurve
            )
            activeHandPos = Offset(targetTip.x + unit * 0.14f, targetTip.y - unit * 0.05f)
            stickEnd = targetTip
            noodleLiftFrac = liftCurve.coerceAtLeast(0.18f)
        }
    }

    drawRamenBowlAndNoodles(
        bowlCenter = bowlCenter,
        bowlR = bowlR,
        noodleLiftFrac = noodleLiftFrac,
        chopstickTip = stickEnd,
        elapsedMs = elapsedMs
    )

    val frontElbow = Offset((frontShoulder.x + activeHandPos.x) * 0.52f + unit * 0.06f, shoulderCenter.y + unit * 0.12f)
    drawAnatomicalLimb(frontShoulder, frontElbow, bicepR, forearmR, palette.sleeveLight, palette.sleeveDark, dir, f.def.primaryColor.copy(alpha = 0.35f))
    drawAnatomicalLimb(frontElbow, activeHandPos, forearmR, wristR, palette.armLight, palette.armDark, dir, null)
    drawDetailedFist(activeHandPos, wristR * 1.35f, dir, palette.gloveColor, palette.gloveShade, f.def.accentColor)

    // Twin Golden & Jade Chopsticks connecting hand to tip
    drawLine(InkOutline, activeHandPos, stickEnd, strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(Color(0xFFFFD600), activeHandPos, stickEnd, strokeWidth = 4.2f, cap = StrokeCap.Round)
    drawLine(InkOutline, activeHandPos, Offset(stickEnd.x + 5f, stickEnd.y - 6f), strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Color(0xFF00E676), activeHandPos, Offset(stickEnd.x + 5f, stickEnd.y - 6f), strokeWidth = 3.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawJardinCameraSmashScene(f: FighterRuntime, elapsedMs: Long) {
    val w = size.width
    val h = size.height
    val tSec = elapsedMs / 1000f

    // Phase 1 (0..0.85s): Charge golden vines around arm
    // Phase 2 (0.85..1.60s): Perspective rush forward toward camera
    // Phase 3 (1.60s+): Colossal punch smashes camera lens!
    val impactTime = 1.60f
    val postImpact = (tSec - impactTime).coerceAtLeast(0f)
    val shakeAmp = if (tSec >= impactTime && postImpact < 0.75f) (1f - postImpact / 0.75f) * 20f else 0f
    val shakeOffset = Offset(
        sin(elapsedMs * 0.11f) * shakeAmp,
        cos(elapsedMs * 0.14f) * shakeAmp * 0.7f
    )

    translate(left = shakeOffset.x, top = shakeOffset.y) {
        drawForestBackdrop(elapsedMs, shakeOffset)

        val rushFrac = ((tSec - 0.85f) / 0.75f).coerceIn(0f, 1f)
        val easedRush = rushFrac * rushFrac
        val perspectiveScale = 0.68f + easedRush * 0.92f
        val unit = h * 0.42f * perspectiveScale
        val groundY = h * (0.79f + easedRush * 0.18f)
        val cx = w * 0.50f
        val dir = 1f
        val palette = fighterPalette(f.def.visualStyle, false)

        val crouch = if (tSec < 0.85f) 0.08f else 0.02f
        val leanForward = easedRush * unit * 0.16f

        val hipCenter = Offset(cx - leanForward * 0.3f, groundY - unit * (0.43f - crouch))
        val waistCenter = Offset(cx, hipCenter.y - unit * 0.11f)
        val shoulderCenter = Offset(cx + leanForward * 0.6f, hipCenter.y - unit * 0.31f)
        val neckBase = Offset(shoulderCenter.x + unit * 0.02f, shoulderCenter.y - unit * 0.03f)
        val headCenter = Offset(shoulderCenter.x + unit * 0.04f, shoulderCenter.y - unit * 0.16f)

        drawBackAccessories(f, shoulderCenter, headCenter, unit, dir, elapsedMs, flashWhite = false)

        // Heavy striding legs
        val thighR = unit * 0.110f
        val kneeR = thighR * 0.72f
        val ankleR = thighR * 0.52f
        val bAnk = Offset(cx - unit * 0.24f, groundY - unit * 0.03f)
        val bKnee = Offset(cx - unit * 0.12f, groundY - unit * 0.22f)
        drawAnatomicalLimb(hipCenter, bKnee, thighR, kneeR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
        drawAnatomicalLimb(bKnee, bAnk, kneeR, ankleR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
        drawDetailedBoot(bAnk, dir, unit, palette.shoeColor, palette.shoeTrim)

        val fAnk = Offset(cx + unit * 0.22f, groundY - unit * 0.03f)
        val fKnee = Offset(cx + unit * 0.16f, groundY - unit * 0.22f)
        drawAnatomicalLimb(hipCenter, fKnee, thighR, kneeR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.4f))
        drawAnatomicalLimb(fKnee, fAnk, kneeR, ankleR, palette.pantsLight, palette.pantsDark, dir, f.def.primaryColor.copy(alpha = 0.4f))
        drawDetailedBoot(fAnk, dir, unit, palette.shoeColor, palette.shoeTrim)

        drawSculptedTorso(
            f = f,
            shoulderCenter = shoulderCenter,
            waistCenter = waistCenter,
            hipCenter = hipCenter,
            shoulderHalfW = unit * 0.26f,
            waistHalfW = unit * 0.16f,
            hipHalfW = unit * 0.18f,
            unit = unit,
            dir = dir,
            palette = palette,
            flashWhite = false
        )
        drawAnatomicalLimb(neckBase, headCenter, unit * 0.056f, unit * 0.046f, palette.skinShade, palette.skinDeepShade, dir, null)
        drawAnimeHead(f, headCenter, unit, dir, palette, elapsedMs, flashWhite = false)

        // Massive foreshortened punching fist thrusting straight at the camera lens!
        val frontShoulder = Offset(shoulderCenter.x + unit * 0.22f, shoulderCenter.y + unit * 0.04f)
        val punchThrust = ((tSec - 1.35f) / 0.25f).coerceIn(0f, 1f)
        val fistCenter = Offset(
            frontShoulder.x + (w * 0.50f - frontShoulder.x) * punchThrust,
            shoulderCenter.y + (h * 0.48f - shoulderCenter.y) * punchThrust
        )
        val fistRadius = unit * (0.14f + punchThrust * 0.34f)
        val bicepR = unit * 0.088f
        drawAnatomicalLimb(frontShoulder, fistCenter, bicepR, fistRadius * 0.65f, palette.armLight, palette.armDark, dir, Color(0xFFFFD600))

        // Coiled glowing golden-emerald vines around charging forearm & fist
        for (coil in 0..3) {
            val cr = fistRadius * (0.85f + coil * 0.15f)
            drawCircle(
                color = if (coil % 2 == 0) Color(0xFF00E676) else Color(0xFFFFD600),
                radius = cr,
                center = fistCenter,
                style = Stroke(width = 5.5f)
            )
        }
        drawDetailedFist(fistCenter, fistRadius, dir, palette.gloveColor, palette.gloveShade, Color(0xFFFFD600))

        // Shattered Camera Lens Cracks & Falling Glass Shards upon impact!
        if (tSec >= impactTime) {
            val shatterFrac = (postImpact / 0.24f).coerceIn(0.25f, 1f)
            drawCameraLensShatterOverlay(
                impactCenter = Offset(w * 0.50f, h * 0.48f),
                shatterFrac = shatterFrac,
                elapsedMs = elapsedMs
            )
            if (postImpact < 0.24f) {
                drawRect(color = Color.White.copy(alpha = (1f - postImpact / 0.24f) * 0.65f))
            }
        }
    }
}

private fun DrawScope.drawDaronBeachCoconutScene(f: FighterRuntime, elapsedMs: Long) {
    drawCopacabanaBackdrop(elapsedMs)

    val w = size.width
    val h = size.height
    val groundY = h * 0.80f
    val unit = h * 0.43f
    val cx = w * 0.48f
    val dir = 1f
    val palette = fighterPalette(f.def.visualStyle, false)

    val tSec = elapsedMs / 1000f
    // Phase 1 (0..0.85s): Unfold transat
    // Phase 2 (0.85..1.60s): Recline + toss & catch coconut
    // Phase 3 (1.60s+): Chill sipping loop with foot sway!
    val unfoldFrac = (tSec / 0.65f).coerceIn(0f, 1f)
    val reclineFrac = ((tSec - 0.60f) / 0.55f).coerceIn(0f, 1f)

    // Draw Transat Lounger behind/under Daron
    drawBeachLoungerAndCoconut(
        anchor = Offset(cx, groundY),
        unit = unit,
        unfoldFrac = unfoldFrac
    )

    val hipCenter = Offset(cx - reclineFrac * unit * 0.04f, groundY - unit * (0.43f - reclineFrac * 0.23f))
    val waistCenter = Offset(hipCenter.x - reclineFrac * unit * 0.14f, hipCenter.y - unit * (0.11f - reclineFrac * 0.04f))
    val shoulderCenter = Offset(hipCenter.x - reclineFrac * unit * 0.34f, hipCenter.y - unit * (0.31f - reclineFrac * 0.12f))
    val neckBase = Offset(shoulderCenter.x + unit * 0.02f, shoulderCenter.y - unit * 0.03f)
    val headCenter = Offset(shoulderCenter.x + unit * 0.04f - reclineFrac * unit * 0.04f, shoulderCenter.y - unit * 0.16f)

    drawBackAccessories(f, shoulderCenter, headCenter, unit, dir, elapsedMs, flashWhite = false)

    // Reclined legs stretched onto lounger + joyful toe sway in Phase 3
    val thighR = unit * 0.100f
    val kneeR = thighR * 0.72f
    val ankleR = thighR * 0.52f
    val sway = if (tSec > 1.6f) sin(elapsedMs * 0.005f) * unit * 0.035f else 0f

    val fKnee = Offset(
        hipCenter.x + unit * (0.12f + reclineFrac * 0.16f),
        hipCenter.y + unit * (0.19f - reclineFrac * 0.19f)
    )
    val fAnk = Offset(
        hipCenter.x + unit * (0.18f + reclineFrac * 0.38f),
        groundY - unit * (0.03f + reclineFrac * 0.12f) + sway
    )
    drawAnatomicalLimb(hipCenter, fKnee, thighR, kneeR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
    drawAnatomicalLimb(fKnee, fAnk, kneeR, ankleR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
    drawDetailedBoot(fAnk, dir, unit, palette.shoeColor, palette.shoeTrim)

    drawSculptedTorso(
        f = f,
        shoulderCenter = shoulderCenter,
        waistCenter = waistCenter,
        hipCenter = hipCenter,
        shoulderHalfW = unit * 0.255f,
        waistHalfW = unit * 0.230f,
        hipHalfW = unit * 0.200f,
        unit = unit,
        dir = dir,
        palette = palette,
        flashWhite = false
    )
    drawAnatomicalLimb(neckBase, headCenter, unit * 0.056f, unit * 0.046f, palette.skinShade, palette.skinDeepShade, dir, null)
    drawAnimeHead(f, headCenter, unit, dir, palette, elapsedMs, flashWhite = false)

    // Arm raising coconut with straw to lips during Phase 3!
    val frontShoulder = Offset(shoulderCenter.x + unit * 0.16f, shoulderCenter.y + unit * 0.03f)
    val sipWave = if (tSec > 1.60f) (sin((tSec - 1.60f) * 2.4f) * 0.5f + 0.5f) else 0f
    val tossArc = if (tSec in 0.85f..1.55f) sin(((tSec - 0.85f) / 0.70f) * PI.toFloat()) * unit * 0.34f else 0f

    val restingHand = Offset(hipCenter.x + unit * 0.18f, hipCenter.y - unit * 0.04f)
    val sippingHand = Offset(headCenter.x + unit * 0.20f, headCenter.y + unit * 0.12f)
    val handTarget = Offset(
        restingHand.x + (sippingHand.x - restingHand.x) * (reclineFrac * (0.45f + sipWave * 0.55f)),
        restingHand.y + (sippingHand.y - restingHand.y) * (reclineFrac * (0.45f + sipWave * 0.55f))
    )
    val elbow = Offset((frontShoulder.x + handTarget.x) * 0.5f - unit * 0.04f, (frontShoulder.y + handTarget.y) * 0.5f + unit * 0.10f)
    val bicepR = unit * 0.082f
    drawAnatomicalLimb(frontShoulder, elbow, bicepR, bicepR * 0.82f, palette.sleeveLight, palette.sleeveDark, dir, null)
    drawAnatomicalLimb(elbow, handTarget, bicepR * 0.82f, bicepR * 0.64f, palette.armLight, palette.armDark, dir, null)
    drawDetailedFist(handTarget, bicepR * 0.90f, dir, palette.gloveColor, palette.gloveShade, f.def.accentColor)

    // Green Coconut with Striped Straw & Cocktail Umbrella
    val cocoCenter = Offset(handTarget.x + unit * 0.04f, handTarget.y - unit * 0.05f - tossArc)
    drawCoconutWithStraw(center = cocoCenter, radius = unit * 0.105f)
}

private fun DrawScope.drawGlitchermanPixelShatterScene(f: FighterRuntime, elapsedMs: Long) {
    drawCyberVoidBackdrop(elapsedMs)

    val w = size.width
    val h = size.height
    val tSec = elapsedMs / 1000f
    val unit = h * 0.41f
    val cx = w * 0.50f
    val dir = 1f
    val palette = fighterPalette(f.def.visualStyle, false)

    // Phase 1 (0..0.90s): Levitate smoothly into mid-air + open 4 orbiting cyber portals
    // Phase 2 (0.90..1.65s): Channel lightning arcs from portals + finger snap!
    // Phase 3 (1.65s+): Screen shatters into radial stepped pixel cracks & floating RGB pixel blocks!
    val levitateFrac = (tSec / 0.85f).coerceIn(0f, 1f)
    val floatBob = sin(elapsedMs * 0.004f) * unit * 0.045f
    val baseFloatY = h * (0.76f - levitateFrac * 0.15f) + floatBob

    // 4 Spinning Cyber Portals around levitating Glitcherman
    val portalOpen = (tSec / 0.65f).coerceIn(0f, 1f)
    val portalPositions = listOf(
        Offset(cx - w * 0.22f, baseFloatY - unit * 0.72f),
        Offset(cx + w * 0.22f, baseFloatY - unit * 0.72f),
        Offset(cx - w * 0.26f, baseFloatY - unit * 0.22f),
        Offset(cx + w * 0.26f, baseFloatY - unit * 0.22f)
    )
    val snapCenter = Offset(cx + unit * 0.26f, baseFloatY - unit * 0.88f)

    portalPositions.forEachIndexed { idx, pCenter ->
        val pR = unit * 0.19f * portalOpen
        val pColor = if (idx % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF2A85)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.65f), pColor.copy(alpha = 0.45f), Color.Transparent), center = pCenter, radius = pR * 1.3f),
            radius = pR * 1.3f,
            center = pCenter
        )
        drawCircle(color = pColor, radius = pR, center = pCenter, style = Stroke(width = 5f))
        drawCircle(color = Color.White.copy(alpha = 0.8f), radius = pR * 0.62f, center = pCenter, style = Stroke(width = 2.5f))
        for (dot in 0..5) {
            val ang = elapsedMs * 0.005f * (if (idx % 2 == 0) 1 else -1) + dot * (PI.toFloat() / 3f)
            drawCircle(Color.White, radius = 3.5f, center = Offset(pCenter.x + cos(ang) * pR, pCenter.y + sin(ang) * pR))
        }
        // Converging lightning arcs during Phase 2
        if (tSec in 0.80f..1.75f) {
            val mid = Offset((pCenter.x + snapCenter.x) * 0.5f + sin(elapsedMs * 0.05f + idx) * 18f, (pCenter.y + snapCenter.y) * 0.5f - 16f)
            drawLine(pColor, pCenter, mid, strokeWidth = 4.2f, cap = StrokeCap.Round)
            drawLine(Color.White, mid, snapCenter, strokeWidth = 3.4f, cap = StrokeCap.Round)
        }
    }

    // Levitating Shadow far below
    drawOval(
        color = Color(0xFF00E5FF).copy(alpha = 0.26f),
        topLeft = Offset(cx - unit * 0.30f, h * 0.78f),
        size = Size(unit * 0.60f, 16f)
    )

    // Levitating Glitcherman Body
    val hipCenter = Offset(cx, baseFloatY - unit * 0.40f)
    val waistCenter = Offset(cx, hipCenter.y - unit * 0.11f)
    val shoulderCenter = Offset(cx, hipCenter.y - unit * 0.31f)
    val neckBase = Offset(shoulderCenter.x + unit * 0.01f, shoulderCenter.y - unit * 0.03f)
    val headCenter = Offset(shoulderCenter.x + unit * 0.025f, shoulderCenter.y - unit * 0.165f)

    drawBackAccessories(f, shoulderCenter, headCenter, unit, dir, elapsedMs, flashWhite = false)

    val thighR = unit * 0.090f
    val kneeR = thighR * 0.70f
    val ankleR = thighR * 0.50f
    // Asymmetric hero hover legs (one bent, one extended)
    val bKnee = Offset(cx - unit * 0.16f, hipCenter.y + unit * 0.16f)
    val bAnk = Offset(cx - unit * 0.06f, hipCenter.y + unit * 0.34f)
    drawAnatomicalLimb(hipCenter, bKnee, thighR, kneeR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
    drawAnatomicalLimb(bKnee, bAnk, kneeR, ankleR, palette.pantsDark, palette.pantsDeepShadow, dir, null)
    drawDetailedBoot(bAnk, dir, unit, palette.shoeColor, palette.shoeTrim)

    val fKnee = Offset(cx + unit * 0.12f, hipCenter.y + unit * 0.20f)
    val fAnk = Offset(cx + unit * 0.14f, hipCenter.y + unit * 0.41f)
    drawAnatomicalLimb(hipCenter, fKnee, thighR, kneeR, palette.pantsLight, palette.pantsDark, dir, Color(0xFF00E5FF))
    drawAnatomicalLimb(fKnee, fAnk, kneeR, ankleR, palette.pantsLight, palette.pantsDark, dir, Color(0xFF00E5FF))
    drawDetailedBoot(fAnk, dir, unit, palette.shoeColor, palette.shoeTrim)

    drawSculptedTorso(
        f = f,
        shoulderCenter = shoulderCenter,
        waistCenter = waistCenter,
        hipCenter = hipCenter,
        shoulderHalfW = unit * 0.21f,
        waistHalfW = unit * 0.13f,
        hipHalfW = unit * 0.15f,
        unit = unit,
        dir = dir,
        palette = palette,
        flashWhite = false
    )
    drawAnatomicalLimb(neckBase, headCenter, unit * 0.050f, unit * 0.042f, palette.skinShade, palette.skinDeepShade, dir, null)
    drawAnimeHead(f, headCenter, unit, dir, palette, elapsedMs, flashWhite = false)

    // Raised Snap Hand!
    val frontShoulder = Offset(shoulderCenter.x + unit * 0.16f, shoulderCenter.y + unit * 0.02f)
    val elbow = Offset(frontShoulder.x + unit * 0.10f, shoulderCenter.y - unit * 0.10f)
    val bicepR = unit * 0.070f
    drawAnatomicalLimb(frontShoulder, elbow, bicepR, bicepR * 0.82f, palette.sleeveLight, palette.sleeveDark, dir, Color(0xFF00E5FF))
    drawAnatomicalLimb(elbow, snapCenter, bicepR * 0.82f, bicepR * 0.64f, palette.armLight, palette.armDark, dir, Color(0xFFFF2A85))
    drawDetailedFist(snapCenter, bicepR * 0.90f, dir, palette.gloveColor, palette.gloveShade, Color.White)

    // Phase 3 (1.65s+): Pixel Screen Shatter + Floating Pixel Shards!
    if (tSec >= 1.65f) {
        val postSnap = (tSec - 1.65f).coerceAtLeast(0f)
        val shatterFrac = (postSnap / 0.25f).coerceIn(0.25f, 1f)
        drawPixelScreenShatterOverlay(
            center = Offset(w * 0.50f, h * 0.46f),
            shatterFrac = shatterFrac,
            elapsedMs = elapsedMs
        )
        if (postSnap < 0.20f) {
            drawRect(color = Color(0xFF00E5FF).copy(alpha = (1f - postSnap / 0.20f) * 0.48f))
        }
    }
}
