package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.data.FighterDefinition
import com.example.game.engine.FighterRuntime
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.GuardBlue
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

@Composable
fun ArcadeControls(
    fighter: FighterDefinition,
    runtime: FighterRuntime,
    onMoveLeftChanged: (Boolean) -> Unit,
    onMoveRightChanged: (Boolean) -> Unit,
    onJumpPressed: () -> Unit,
    onBlockChanged: (Boolean) -> Unit,
    onClassicPressed: () -> Unit,
    onBaseAbilityPressed: () -> Unit,
    onUltiPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ultiReady = runtime.ultiMeter >= fighter.ultimateMove.ultiCost
    val ultiFrac = (runtime.ultiMeter.toFloat() / fighter.ultimateMove.ultiCost.coerceAtLeast(1)).coerceIn(0f, 1f)
    val baseCdSec = runtime.baseCooldownMs / 1000f
    val baseTotalMs = fighter.baseAbility.cooldownMs.coerceAtLeast(1L).toFloat()
    val baseReadyFrac = 1f - (runtime.baseCooldownMs / baseTotalMs).coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // LEFT THUMB ZONE
        Surface(
            color = ObsidianBg.copy(alpha = 0.56f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HoldActionButton(
                    icon = Icons.Default.ArrowUpward,
                    label = "SAUT",
                    tint = Color.White,
                    width = 108.dp,
                    height = 48.dp,
                    testTag = "control_jump",
                    onHoldChanged = { pressed -> if (pressed) onJumpPressed() }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldActionButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        label = "GAUCHE",
                        tint = Color.White,
                        width = 68.dp,
                        height = 52.dp,
                        testTag = "control_left",
                        onHoldChanged = onMoveLeftChanged
                    )
                    HoldActionButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        label = "DROITE",
                        tint = Color.White,
                        width = 68.dp,
                        height = 52.dp,
                        testTag = "control_right",
                        onHoldChanged = onMoveRightChanged
                    )
                }
            }
        }

        // RIGHT THUMB ZONE
        Surface(
            color = ObsidianBg.copy(alpha = 0.56f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Row: Garde + Ultime
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HoldActionButton(
                        icon = Icons.Default.Shield,
                        label = "GARDE",
                        tint = GuardBlue,
                        width = 66.dp,
                        height = 48.dp,
                        testTag = "control_block",
                        onHoldChanged = onBlockChanged
                    )

                    Surface(
                        onClick = onUltiPressed,
                        enabled = ultiReady,
                        color = Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (ultiReady) 2.2.dp else 1.dp,
                            color = if (ultiReady) UltiGold else ObsidianOutline
                        ),
                        modifier = Modifier
                            .width(108.dp)
                            .height(48.dp)
                            .testTag("control_ulti")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (ultiReady) {
                                        Brush.horizontalGradient(listOf(UltiGold.copy(alpha = 0.34f), FlameOrange.copy(alpha = 0.28f)))
                                    } else {
                                        Brush.horizontalGradient(listOf(ObsidianSurface.copy(alpha = 0.80f), ObsidianBg.copy(alpha = 0.85f)))
                                    }
                                )
                        ) {
                            if (!ultiReady) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ultiFrac)
                                        .fillMaxHeight()
                                        .background(UltiGold.copy(alpha = 0.16f))
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Ultime",
                                        tint = if (ultiReady) UltiGold else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (ultiReady) "ULTI !" else "${runtime.ultiMeter}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (ultiReady) UltiGold else TextSecondary
                                    )
                                }
                                Text(
                                    text = fighter.ultimateMove.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ultiReady) Color.White else TextSecondary.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Bottom Row: Capacité + Classique
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleAttackButton(
                        title = if (runtime.baseCooldownMs > 0L) String.format("%.1fs", baseCdSec) else "CAPACITÉ",
                        subtitle = fighter.baseAbility.name,
                        icon = Icons.Default.Bolt,
                        accentColor = FlameOrange,
                        enabled = runtime.baseCooldownMs <= 0L,
                        readyFraction = baseReadyFrac,
                        buttonSize = 58.dp,
                        testTag = "control_base",
                        onClick = onBaseAbilityPressed
                    )

                    CircleAttackButton(
                        title = "CLASSIQUE",
                        subtitle = fighter.classicMove.name,
                        icon = Icons.Default.SportsMma,
                        accentColor = ElectricCyan,
                        enabled = runtime.classicCooldownMs <= 0L,
                        readyFraction = if (runtime.classicCooldownMs <= 0L) 1f else 0.5f,
                        buttonSize = 64.dp,
                        testTag = "control_classic",
                        onClick = onClassicPressed
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    width: Dp,
    height: Dp,
    testTag: String,
    onHoldChanged: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        color = if (isPressed) tint.copy(alpha = 0.34f) else ObsidianSurface.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (isPressed) tint else ObsidianOutline.copy(alpha = 0.85f)),
        modifier = Modifier
            .size(width = width, height = height)
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onHoldChanged(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    onHoldChanged(false)
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = 0.92f)
            )
        }
    }
}

@Composable
private fun CircleAttackButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    enabled: Boolean,
    readyFraction: Float,
    buttonSize: Dp,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            color = Color.Transparent,
            shape = CircleShape,
            border = BorderStroke(
                width = if (enabled) 2.2.dp else 1.2.dp,
                color = if (enabled) accentColor else ObsidianOutline
            ),
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .testTag(testTag)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (enabled) {
                            Brush.radialGradient(listOf(accentColor.copy(alpha = 0.38f), ObsidianSurface.copy(alpha = 0.88f)))
                        } else {
                            Brush.radialGradient(listOf(ObsidianSurface.copy(alpha = 0.82f), ObsidianBg.copy(alpha = 0.92f)))
                        }
                    )
            ) {
                if (!enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(readyFraction)
                            .align(Alignment.BottomCenter)
                            .background(accentColor.copy(alpha = 0.16f))
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (enabled) accentColor else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) Color.White else TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) accentColor else TextSecondary.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
