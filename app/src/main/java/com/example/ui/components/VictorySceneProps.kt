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
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun DrawScope.drawSamuraiTempleBackdrop(elapsedMs: Long) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.78f

    // Twilight indigo-to-crimson sky
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF0E091E), Color(0xFF2D1234), Color(0xFF7B2435), Color(0xFFD8683C)),
            startY = 0f,
            endY = groundY
        )
    )

    // Giant glowing twilight moon behind pagoda
    val moonCenter = Offset(w * 0.76f, h * 0.30f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFF3C4), Color(0xFFFFB74D).copy(alpha = 0.65f), Color.Transparent),
            center = moonCenter,
            radius = h * 0.34f
        ),
        radius = h * 0.34f,
        center = moonCenter
    )
    drawCircle(color = Color(0xFFFFF8DC), radius = h * 0.145f, center = moonCenter)

    // Distant mountain ridge
    val mountain = Path().apply {
        moveTo(0f, groundY)
        lineTo(0f, h * 0.54f)
        lineTo(w * 0.22f, h * 0.38f)
        lineTo(w * 0.48f, h * 0.56f)
        lineTo(w * 0.74f, h * 0.36f)
        lineTo(w, h * 0.50f)
        lineTo(w, groundY)
        close()
    }
    drawPath(mountain, color = Color(0xFF1A0F24).copy(alpha = 0.76f))

    // Layered Samurai Pagoda silhouette on right
    val pagX = w * 0.78f
    for (tier in 0..2) {
        val tw = w * (0.16f - tier * 0.032f)
        val ty = groundY - h * (0.14f + tier * 0.12f)
        drawRect(
            color = Color(0xFF150B1C),
            topLeft = Offset(pagX - tw * 0.38f, ty),
            size = Size(tw * 0.76f, h * 0.12f)
        )
        val roof = Path().apply {
            moveTo(pagX - tw * 0.65f, ty + 4f)
            quadraticTo(pagX, ty - h * 0.04f, pagX + tw * 0.65f, ty + 4f)
            lineTo(pagX + tw * 0.45f, ty + h * 0.03f)
            lineTo(pagX - tw * 0.45f, ty + h * 0.03f)
            close()
        }
        drawPath(roof, color = Color(0xFF1E1029))
        drawPath(roof, color = InkOutline, style = Stroke(width = 2.4f))
    }
    // Pagoda spire
    drawLine(
        color = Color(0xFFFFD600),
        start = Offset(pagX, groundY - h * 0.40f),
        end = Offset(pagX, groundY - h * 0.53f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )

    // Iconic Vermilion Torii Gate behind center-left
    val toriiX = w * 0.27f
    val pillarGap = w * 0.14f
    val toriiTopY = h * 0.22f
    // Pillars
    drawRect(color = Color(0xFFB71C1C), topLeft = Offset(toriiX - pillarGap * 0.5f - 10f, toriiTopY), size = Size(20f, groundY - toriiTopY))
    drawRect(color = Color(0xFFB71C1C), topLeft = Offset(toriiX + pillarGap * 0.5f - 10f, toriiTopY), size = Size(20f, groundY - toriiTopY))
    drawRect(color = InkOutline, topLeft = Offset(toriiX - pillarGap * 0.5f - 10f, toriiTopY), size = Size(20f, groundY - toriiTopY), style = Stroke(width = 2.6f))
    drawRect(color = InkOutline, topLeft = Offset(toriiX + pillarGap * 0.5f - 10f, toriiTopY), size = Size(20f, groundY - toriiTopY), style = Stroke(width = 2.6f))
    // Lower tie beam (Nuki)
    drawRect(color = Color(0xFFD32F2F), topLeft = Offset(toriiX - pillarGap * 0.68f, toriiTopY + h * 0.085f), size = Size(pillarGap * 1.36f, 13f))
    // Curved top lintel (Kasagi)
    val kasagi = Path().apply {
        moveTo(toriiX - pillarGap * 0.85f, toriiTopY - 8f)
        quadraticTo(toriiX, toriiTopY + 12f, toriiX + pillarGap * 0.85f, toriiTopY - 8f)
        lineTo(toriiX + pillarGap * 0.78f, toriiTopY + 14f)
        quadraticTo(toriiX, toriiTopY + 26f, toriiX - pillarGap * 0.78f, toriiTopY + 14f)
        close()
    }
    drawPath(kasagi, color = Color(0xFF1A1019))
    drawPath(kasagi, color = Color(0xFFE53935), style = Stroke(width = 3f))

    // Swaying warm paper chochin lanterns hanging from Torii / sides
    listOf(toriiX - pillarGap * 0.64f, toriiX + pillarGap * 0.64f).forEachIndexed { idx, lx ->
        val sway = sin(elapsedMs * 0.0028f + idx * 1.4f) * 6f
        val ly = toriiTopY + h * 0.17f
        drawLine(InkOutline, Offset(lx, toriiTopY + h * 0.09f), Offset(lx + sway, ly), strokeWidth = 2.5f)
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFFD600).copy(alpha = 0.42f), Color.Transparent), center = Offset(lx + sway, ly + 14f), radius = 44f),
            radius = 44f,
            center = Offset(lx + sway, ly + 14f)
        )
        drawOval(color = Color(0xFFFF6D00), topLeft = Offset(lx + sway - 12f, ly), size = Size(24f, 30f))
        drawOval(color = InkOutline, topLeft = Offset(lx + sway - 12f, ly), size = Size(24f, 30f), style = Stroke(width = 2.2f))
    }

    // Temple Stone Floor & Steps
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF3E2B38), Color(0xFF1A111A)), startY = groundY, endY = h),
        topLeft = Offset(0f, groundY),
        size = Size(w, h - groundY)
    )
    drawLine(Color(0xFFFFB74D).copy(alpha = 0.55f), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 3f)

    // Drifting Sakura Petals
    for (p in 0..22) {
        val t = ((elapsedMs * 0.00017f) + p * 0.085f) % 1f
        val px = ((p * 0.047f + sin(elapsedMs * 0.0018f + p) * 0.04f) % 1f) * w
        val py = t * h
        rotate(degrees = (elapsedMs * 0.08f + p * 30f) % 360f, pivot = Offset(px, py)) {
            drawOval(color = Color(0xFFFFB2C8).copy(alpha = 0.88f), topLeft = Offset(px - 6f, py - 3.5f), size = Size(12f, 7f))
        }
    }
}

internal fun DrawScope.drawForestBackdrop(elapsedMs: Long, shakeOffset: Offset = Offset.Zero) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.80f

    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF04140D), Color(0xFF0B2E1E), Color(0xFF12462B)),
            startY = 0f,
            endY = groundY
        )
    )

    // Canopy light shafts
    for (ray in 0..3) {
        val rx = w * (0.15f + ray * 0.24f) + shakeOffset.x * 0.3f
        val beam = Path().apply {
            moveTo(rx, 0f)
            lineTo(rx + w * 0.09f, 0f)
            lineTo(rx + w * 0.20f, groundY)
            lineTo(rx - w * 0.05f, groundY)
            close()
        }
        drawPath(
            beam,
            brush = Brush.verticalGradient(listOf(Color(0xFF69F0AE).copy(alpha = 0.14f), Color.Transparent), startY = 0f, endY = groundY)
        )
    }

    // Ancient gnarled tree trunks
    listOf(0.08f, 0.24f, 0.77f, 0.92f).forEachIndexed { i, fx ->
        val tx = w * fx + shakeOffset.x * 0.45f
        val tw = w * (0.065f + (i % 2) * 0.025f)
        drawRect(color = Color(0xFF0A1D12), topLeft = Offset(tx - tw * 0.5f, 0f), size = Size(tw, groundY + 8f))
        drawLine(Color(0xFF00E676).copy(alpha = 0.32f), Offset(tx - tw * 0.2f, 0f), Offset(tx - tw * 0.1f, groundY), strokeWidth = 3f)
    }

    // Bioluminescent mossy forest floor
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF10331D), Color(0xFF05110A)), startY = groundY, endY = h),
        topLeft = Offset(0f, groundY),
        size = Size(w, h - groundY)
    )
    drawLine(Color(0xFF00E676).copy(alpha = 0.65f), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 3.5f)

    // Floating golden & emerald spores
    for (s in 0..20) {
        val t = ((elapsedMs * 0.00019f) + s * 0.09f) % 1f
        val sx = ((s * 0.051f + sin(elapsedMs * 0.0014f + s) * 0.03f) % 1f) * w
        val sy = (1f - t) * h
        drawCircle(
            color = (if (s % 3 == 0) Color(0xFFFFD600) else Color(0xFF00E676)).copy(alpha = (1f - t) * 0.85f),
            radius = 3.2f + (s % 3) * 1.6f,
            center = Offset(sx, sy)
        )
    }
}

internal fun DrawScope.drawCopacabanaBackdrop(elapsedMs: Long) {
    val w = size.width
    val h = size.height
    val horizonY = h * 0.54f
    val beachY = h * 0.72f

    // Tropical sunset sky
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF141136), Color(0xFF5D1E5B), Color(0xFFD94A4E), Color(0xFFFFB74D)),
            startY = 0f,
            endY = horizonY
        )
    )

    // Giant Golden Sunset Sun over the Atlantic
    val sunCenter = Offset(w * 0.72f, horizonY - h * 0.04f)
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFF9800).copy(alpha = 0.55f), Color.Transparent), center = sunCenter, radius = h * 0.30f),
        radius = h * 0.30f,
        center = sunCenter
    )
    drawCircle(color = Color(0xFFFFEB3B), radius = h * 0.13f, center = sunCenter)

    // Rio Sugarloaf silhouette on left horizon
    val sugarloaf = Path().apply {
        moveTo(0f, horizonY)
        lineTo(0f, horizonY - h * 0.14f)
        quadraticTo(w * 0.09f, horizonY - h * 0.25f, w * 0.18f, horizonY - h * 0.09f)
        lineTo(w * 0.30f, horizonY)
        close()
    }
    drawPath(sugarloaf, color = Color(0xFF281337).copy(alpha = 0.85f))

    // Festive Fireworks in Rio sky
    listOf(Offset(w * 0.22f, h * 0.20f), Offset(w * 0.86f, h * 0.22f)).forEachIndexed { idx, fc ->
        val cycle = ((elapsedMs * 0.00075f + idx * 0.5f) % 1f)
        val radius = h * (0.04f + cycle * 0.13f)
        val alpha = (1f - cycle).coerceIn(0f, 1f)
        val fColor = if (idx == 0) Color(0xFF00E676) else Color(0xFFFFD600)
        for (ray in 0..11) {
            val ang = (ray / 12f) * (2f * PI.toFloat())
            drawLine(
                color = fColor.copy(alpha = alpha * 0.9f),
                start = Offset(fc.x + cos(ang) * radius * 0.25f, fc.y + sin(ang) * radius * 0.25f),
                end = Offset(fc.x + cos(ang) * radius, fc.y + sin(ang) * radius),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
    }

    // Sparkling Ocean & Waves
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF006064), Color(0xFF00838F)), startY = horizonY, endY = beachY),
        topLeft = Offset(0f, horizonY),
        size = Size(w, beachY - horizonY)
    )
    for (wave in 0..3) {
        val wy = horizonY + (beachY - horizonY) * (0.28f + wave * 0.20f) + sin(elapsedMs * 0.004f + wave) * 3f
        drawLine(Color.White.copy(alpha = 0.45f), Offset(w * 0.1f, wy), Offset(w * 0.92f, wy), strokeWidth = 2.5f)
    }

    // Golden Copacabana Sand Beach
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFE6C27A), Color(0xFFB58845)), startY = beachY, endY = h),
        topLeft = Offset(0f, beachY),
        size = Size(w, h - beachY)
    )

    // Swaying Palm Trees framing left/right
    listOf(0.06f to 1f, 0.93f to -1f).forEach { (pxFrac, lean) ->
        val base = Offset(w * pxFrac, beachY + h * 0.05f)
        val top = Offset(w * pxFrac + lean * w * 0.05f, h * 0.22f)
        drawLine(Color(0xFF4E342E), base, top, strokeWidth = 18f, cap = StrokeCap.Round)
        for (frond in -2..2) {
            val sway = sin(elapsedMs * 0.0025f + frond) * 8f
            val fPath = Path().apply {
                moveTo(top.x, top.y)
                quadraticTo(top.x + frond * 34f, top.y - 26f + sway, top.x + frond * 62f, top.y + 20f + sway)
            }
            drawPath(fPath, color = Color(0xFF1B5E20), style = Stroke(width = 11f, cap = StrokeCap.Round))
            drawPath(fPath, color = Color(0xFF4CAF50), style = Stroke(width = 5f, cap = StrokeCap.Round))
        }
    }
}

internal fun DrawScope.drawCyberVoidBackdrop(elapsedMs: Long) {
    val w = size.width
    val h = size.height
    val horizonY = h * 0.68f

    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF040510), Color(0xFF0A1028), Color(0xFF160B28)))
    )

    // Perspective Neon Cyber Floor Grid
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF080D1F), Color(0xFF04060D)), startY = horizonY, endY = h),
        topLeft = Offset(0f, horizonY),
        size = Size(w, h - horizonY)
    )
    for (i in 0..14) {
        val tx = w * (i / 14f)
        val bx = w * 0.5f + (tx - w * 0.5f) * 2.1f
        drawLine(Color(0xFF00E5FF).copy(alpha = 0.30f), Offset(tx, horizonY), Offset(bx, h), strokeWidth = 2f)
    }
    for (r in 1..4) {
        val f = r / 5f
        val ry = horizonY + (h - horizonY) * (f * f)
        drawLine(Color(0xFFFF2A85).copy(alpha = 0.28f), Offset(0f, ry), Offset(w, ry), strokeWidth = 1.8f)
    }
    drawLine(Color(0xFF00E5FF), Offset(0f, horizonY), Offset(w, horizonY), strokeWidth = 3f)

    // Rising Matrix Data Beams
    for (col in 0..16) {
        val cx = (col / 16f) * w + 10f
        val headY = (((elapsedMs * 0.26f) + col * 90f) % (h + 120f)) - 60f
        drawLine(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF00E5FF).copy(alpha = 0.45f), Color.White.copy(alpha = 0.65f)), startY = headY - 90f, endY = headY),
            start = Offset(cx, headY - 90f),
            end = Offset(cx, headY),
            strokeWidth = 2.6f
        )
    }
}

internal fun DrawScope.drawRamenBowlAndNoodles(
    bowlCenter: Offset,
    bowlR: Float,
    noodleLiftFrac: Float,
    chopstickTip: Offset,
    elapsedMs: Long
) {
    // Steaming aromatic tendrils above broth
    for (s in -1..1) {
        val sx = bowlCenter.x + s * bowlR * 0.42f
        val wave = sin(elapsedMs * 0.005f + s * 1.4f) * 8f
        val steam = Path().apply {
            moveTo(sx, bowlCenter.y - bowlR * 0.22f)
            quadraticTo(sx + wave, bowlCenter.y - bowlR * 0.72f, sx - wave * 0.6f, bowlCenter.y - bowlR * 1.2f)
        }
        drawPath(steam, color = Color.White.copy(alpha = 0.48f), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
    }

    // Golden Broth + Naruto Fishcake
    drawOval(
        color = Color(0xFFFFB300),
        topLeft = Offset(bowlCenter.x - bowlR * 0.90f, bowlCenter.y - bowlR * 0.32f),
        size = Size(bowlR * 1.80f, bowlR * 0.56f)
    )
    drawCircle(color = Color.White, radius = bowlR * 0.18f, center = Offset(bowlCenter.x + bowlR * 0.34f, bowlCenter.y - bowlR * 0.08f))
    drawCircle(color = Color(0xFFFF4081), radius = bowlR * 0.08f, center = Offset(bowlCenter.x + bowlR * 0.34f, bowlCenter.y - bowlR * 0.08f))

    // Wavy Lifted Ramen Noodle Strands up to Chopstick Tip
    if (noodleLiftFrac > 0.05f) {
        val topAnchor = Offset(
            bowlCenter.x + (chopstickTip.x - bowlCenter.x) * noodleLiftFrac,
            bowlCenter.y + (chopstickTip.y - bowlCenter.y) * noodleLiftFrac
        )
        for (n in -2..2) {
            val nxBot = bowlCenter.x + n * (bowlR * 0.11f)
            val nxTop = topAnchor.x + n * (bowlR * 0.06f)
            val sway = sin(elapsedMs * 0.012f + n) * 4f
            val noodle = Path().apply {
                moveTo(nxBot, bowlCenter.y - bowlR * 0.05f)
                quadraticTo((nxBot + nxTop) * 0.5f + sway, (bowlCenter.y + topAnchor.y) * 0.5f, nxTop, topAnchor.y)
            }
            drawPath(noodle, color = Color(0xFF4E342E), style = Stroke(width = 6.2f, cap = StrokeCap.Round))
            drawPath(noodle, color = Color(0xFFFFEE58), style = Stroke(width = 3.6f, cap = StrokeCap.Round))
        }
    }

    // Black & Red Lacquered Ceramic Ramen Bowl
    val bowlPath = Path().apply {
        moveTo(bowlCenter.x - bowlR, bowlCenter.y - bowlR * 0.06f)
        lineTo(bowlCenter.x + bowlR, bowlCenter.y - bowlR * 0.06f)
        quadraticTo(bowlCenter.x + bowlR * 0.85f, bowlCenter.y + bowlR * 0.85f, bowlCenter.x + bowlR * 0.40f, bowlCenter.y + bowlR * 0.92f)
        lineTo(bowlCenter.x - bowlR * 0.40f, bowlCenter.y + bowlR * 0.92f)
        quadraticTo(bowlCenter.x - bowlR * 0.85f, bowlCenter.y + bowlR * 0.85f, bowlCenter.x - bowlR, bowlCenter.y - bowlR * 0.06f)
        close()
    }
    drawPath(bowlPath, brush = Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFF1A0E12))))
    drawLine(Color(0xFFFFD600), Offset(bowlCenter.x - bowlR * 0.92f, bowlCenter.y + bowlR * 0.14f), Offset(bowlCenter.x + bowlR * 0.92f, bowlCenter.y + bowlR * 0.14f), strokeWidth = 4f)
    drawPath(bowlPath, color = InkOutline, style = Stroke(width = 3.4f, join = StrokeJoin.Round))
}

internal fun DrawScope.drawBeachLoungerAndCoconut(
    anchor: Offset,
    unit: Float,
    unfoldFrac: Float
) {
    // Striped Brazilian Green-Gold Wooden Beach Lounger (Transat)
    val frameAlpha = unfoldFrac.coerceIn(0f, 1f)
    if (frameAlpha <= 0.05f) return

    val backTop = Offset(anchor.x - unit * 0.56f, anchor.y - unit * (0.58f * unfoldFrac))
    val seatPivot = Offset(anchor.x - unit * 0.05f, anchor.y - unit * 0.16f)
    val frontFoot = Offset(anchor.x + unit * 0.56f, anchor.y - unit * 0.10f)

    // Wooden legs
    drawLine(Color(0xFF6D4C41).copy(alpha = frameAlpha), Offset(anchor.x - unit * 0.36f, anchor.y), seatPivot, strokeWidth = 10f, cap = StrokeCap.Round)
    drawLine(Color(0xFF6D4C41).copy(alpha = frameAlpha), Offset(anchor.x + unit * 0.40f, anchor.y), frontFoot, strokeWidth = 10f, cap = StrokeCap.Round)
    drawLine(Color(0xFF8D6E63).copy(alpha = frameAlpha), backTop, seatPivot, strokeWidth = 12f, cap = StrokeCap.Round)
    drawLine(Color(0xFF8D6E63).copy(alpha = frameAlpha), seatPivot, frontFoot, strokeWidth = 12f, cap = StrokeCap.Round)

    // Green & Gold striped fabric sling
    drawLine(Color(0xFF00C853).copy(alpha = frameAlpha), backTop, seatPivot, strokeWidth = 18f, cap = StrokeCap.Round)
    drawLine(Color(0xFFFFD600).copy(alpha = frameAlpha), seatPivot, frontFoot, strokeWidth = 18f, cap = StrokeCap.Round)
}

internal fun DrawScope.drawCoconutWithStraw(
    center: Offset,
    radius: Float
) {
    // Green tropical coconut
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF69F0AE), Color(0xFF2E7D32), Color(0xFF1B5E20)), center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f), radius = radius * 1.4f),
        radius = radius,
        center = center
    )
    drawCircle(color = InkOutline, radius = radius, center = center, style = Stroke(width = 3f))
    // Cut top opening
    drawOval(color = Color(0xFFECEFF1), topLeft = Offset(center.x - radius * 0.55f, center.y - radius * 0.85f), size = Size(radius * 1.10f, radius * 0.36f))
    drawOval(color = InkOutline, topLeft = Offset(center.x - radius * 0.55f, center.y - radius * 0.85f), size = Size(radius * 1.10f, radius * 0.36f), style = Stroke(width = 2f))

    // Bendy striped straw
    val strawMid = Offset(center.x - radius * 0.10f, center.y - radius * 1.48f)
    val strawTip = Offset(center.x - radius * 0.72f, center.y - radius * 1.65f)
    drawLine(InkOutline, Offset(center.x, center.y - radius * 0.68f), strawMid, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Color(0xFFFF2A85), Offset(center.x, center.y - radius * 0.68f), strawMid, strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(InkOutline, strawMid, strawTip, strokeWidth = 7f, cap = StrokeCap.Round)
    drawLine(Color.White, strawMid, strawTip, strokeWidth = 4f, cap = StrokeCap.Round)

    // Tiny golden cocktail umbrella
    drawLine(Color(0xFF8D6E63), Offset(center.x + radius * 0.28f, center.y - radius * 0.65f), Offset(center.x + radius * 0.52f, center.y - radius * 1.35f), strokeWidth = 2.6f)
    val umb = Path().apply {
        moveTo(center.x + radius * 0.22f, center.y - radius * 1.25f)
        lineTo(center.x + radius * 0.56f, center.y - radius * 1.50f)
        lineTo(center.x + radius * 0.82f, center.y - radius * 1.15f)
        close()
    }
    drawPath(umb, color = Color(0xFFFFD600))
    drawPath(umb, color = InkOutline, style = Stroke(width = 1.8f))
}

internal fun DrawScope.drawCameraLensShatterOverlay(
    impactCenter: Offset,
    shatterFrac: Float,
    elapsedMs: Long
) {
    if (shatterFrac <= 0f) return
    val w = size.width
    val h = size.height
    val reach = (w.coerceAtLeast(h) * 0.85f) * shatterFrac

    // Radial spiderweb glass fracture lines
    for (i in 0 until 14) {
        val ang = (i / 14f) * (2f * PI.toFloat()) + (i % 3) * 0.08f
        val segs = 4
        val path = Path().apply {
            moveTo(impactCenter.x, impactCenter.y)
            for (s in 1..segs) {
                val dist = reach * (s / segs.toFloat()) * (0.72f + (i % 3) * 0.14f)
                val jitterAng = ang + (if (s % 2 == 0) 0.06f else -0.06f)
                lineTo(impactCenter.x + cos(jitterAng) * dist, impactCenter.y + sin(jitterAng) * dist)
            }
        }
        drawPath(path, color = Color(0xFF04060D).copy(alpha = 0.82f), style = Stroke(width = 5.5f, cap = StrokeCap.Round))
        drawPath(path, color = Color.White.copy(alpha = 0.94f), style = Stroke(width = 2.4f, cap = StrokeCap.Round))
    }

    // Concentric spiderweb rings
    for (ring in 1..3) {
        val rr = reach * (ring * 0.22f)
        drawCircle(color = Color.White.copy(alpha = 0.72f), radius = rr, center = impactCenter, style = Stroke(width = 2.2f))
    }

    // Flying transparent glass triangle shards
    val fallTime = ((elapsedMs % 4000L) / 1000f)
    for (sh in 0..14) {
        val ang = (sh / 15f) * (2f * PI.toFloat())
        val dist = h * (0.14f + (sh % 4) * 0.11f) * shatterFrac + fallTime * 32f
        val sx = impactCenter.x + cos(ang) * dist
        val sy = impactCenter.y + sin(ang) * dist + fallTime * (45f + (sh % 3) * 20f)
        val sz = 14f + (sh % 3) * 7f
        rotate(degrees = (elapsedMs * 0.18f + sh * 45f) % 360f, pivot = Offset(sx, sy)) {
            val tri = Path().apply {
                moveTo(sx, sy - sz)
                lineTo(sx + sz * 0.8f, sy + sz * 0.5f)
                lineTo(sx - sz * 0.7f, sy + sz * 0.7f)
                close()
            }
            drawPath(tri, color = Color.White.copy(alpha = 0.45f))
            drawPath(tri, color = Color(0xFFE0F7FA).copy(alpha = 0.85f), style = Stroke(width = 2f))
        }
    }
}

internal fun DrawScope.drawPixelScreenShatterOverlay(
    center: Offset,
    shatterFrac: Float,
    elapsedMs: Long
) {
    if (shatterFrac <= 0f) return
    val w = size.width
    val h = size.height

    // Stepped 8-bit/pixel zigzag fracture lines radiating from Glitcherman's snap
    for (ray in 0 until 10) {
        val ang = (ray / 10f) * (2f * PI.toFloat()) + 0.18f
        val steps = 7
        var curX = center.x
        var curY = center.y
        val stepLen = (w.coerceAtLeast(h) * 0.085f) * shatterFrac
        val pixelCrack = Path().apply {
            moveTo(curX, curY)
            for (st in 1..steps) {
                val dx = cos(ang) * stepLen
                val dy = sin(ang) * stepLen
                if (st % 2 == 0) {
                    curX += dx
                    lineTo(curX, curY)
                    curY += dy
                    lineTo(curX, curY)
                } else {
                    curY += dy
                    lineTo(curX, curY)
                    curX += dx
                    lineTo(curX, curY)
                }
            }
        }
        val color = if (ray % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFF2A85)
        drawPath(pixelCrack, color = Color(0xFF04060D).copy(alpha = 0.90f), style = Stroke(width = 7f))
        drawPath(pixelCrack, color = color.copy(alpha = 0.96f), style = Stroke(width = 3.2f))
    }

    // Floating shattered RGB pixel blocks drifting in zero-gravity
    for (b in 0..23) {
        val ang = (b / 24f) * (2f * PI.toFloat())
        val dist = h * (0.14f + (b % 5) * 0.10f) * shatterFrac + sin(elapsedMs * 0.002f + b) * 12f
        val bx = center.x + cos(ang) * dist
        val by = center.y + sin(ang) * dist + cos(elapsedMs * 0.0024f + b) * 10f
        val sz = 10f + (b % 4) * 5.5f
        val blockColor = when (b % 3) {
            0 -> Color(0xFF00E5FF)
            1 -> Color(0xFFFF2A85)
            else -> Color.White
        }
        drawRect(color = Color(0xFF05070D).copy(alpha = 0.85f), topLeft = Offset(bx - 2f, by - 2f), size = Size(sz + 4f, sz + 4f))
        drawRect(color = blockColor.copy(alpha = 0.86f), topLeft = Offset(bx, by), size = Size(sz, sz))
    }
}
