package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition

@Composable
fun FighterAvatar(
    fighter: FighterDefinition,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color(0xFF0B0E17))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        fighter.primaryColor,
                        fighter.accentColor,
                        fighter.primaryColor
                    )
                ),
                shape = shape
            )
    ) {
        Image(
            painter = painterResource(id = fighter.portraitRes),
            contentDescription = fighter.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Arcade glossy sheen & bottom rim vignette
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Subtle bottom color rim lighting
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        fighter.primaryColor.copy(alpha = 0.28f)
                    )
                )
            )

            // Diagonal glass sheen at top-left
            val sheen = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.55f, 0f)
                lineTo(w * 0.16f, h * 0.45f)
                lineTo(0f, h * 0.45f)
                close()
            }
            drawPath(
                path = sheen,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.45f, h * 0.45f)
                )
            )
        }
    }
}
