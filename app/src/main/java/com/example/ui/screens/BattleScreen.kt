package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.audio.LocalSoundPlayer
import com.example.game.audio.SoundEffect
import com.example.game.data.BotDifficulty
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.FighterRuntime
import com.example.game.engine.MatchPhase
import com.example.game.engine.TrainingHudState
import com.example.ui.components.ArcadeControls
import com.example.ui.components.ArenaCanvas
import com.example.ui.components.FighterAvatar
import com.example.ui.components.MoveListSheet
import com.example.ui.components.UltiCinematicOverlay
import com.example.ui.components.VictoryCinematicOverlay
import com.example.ui.theme.CrimsonHp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold
import com.example.ui.viewmodel.BattleViewModel

@Composable
fun BattleScreen(
    viewModel: BattleViewModel,
    onRematchLocal: () -> Unit,
    onExitBattle: () -> Unit
) {
    val worldState by viewModel.world.collectAsStateWithLifecycle()
    val timeMs by viewModel.timeMs.collectAsStateWithLifecycle()
    val localSide by viewModel.localSide.collectAsStateWithLifecycle()
    val onlineError by viewModel.onlineError.collectAsStateWithLifecycle()

    var showMoveListFor by remember { mutableStateOf<FighterDefinition?>(null) }
    var showTrainingSettings by remember { mutableStateOf(false) }

    val soundPlayer = LocalSoundPlayer.current
    val isMuted by soundPlayer.isMuted.collectAsStateWithLifecycle()

    val world = worldState
    if (world == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Chargement du combat...", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        return
    }

    val controlledFighter = if (localSide == 1) world.p1 else world.p2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding()
    ) {
        // 1. Fullscreen 2D Landscape Fighting Canvas
        ArenaCanvas(
            world = world,
            timeMs = timeMs,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Translucent Arcade HUD + Compact Training Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            CombatHudHeader(
                world = world,
                localSide = localSide,
                isMuted = isMuted,
                onToggleMute = { soundPlayer.toggleMute() },
                onExit = {
                    soundPlayer.play(SoundEffect.UI_CLICK)
                    onExitBattle()
                }
            )

            if (world.mode == GameMode.TRAINING) {
                TrainingQuickToolbar(
                    training = world.training,
                    p1Name = world.p1.def.name,
                    p2Name = world.p2.def.name,
                    onResetHealth = {
                        soundPlayer.play(SoundEffect.UI_CLICK)
                        viewModel.resetTrainingHealth()
                    },
                    onOpenMoveList = {
                        soundPlayer.play(SoundEffect.UI_CLICK)
                        showMoveListFor = world.p1.def
                    },
                    onOpenSettings = {
                        soundPlayer.play(SoundEffect.UI_CLICK)
                        showTrainingSettings = true
                    }
                )
            }
        }

        // 3. Center Slanted Guilty Gear Round / KO Banner
        if (world.bannerText.isNotEmpty() && world.phase != MatchPhase.MATCH_OVER && world.ultiCinematic == null) {
            val bannerShape = remember { skewedParallelogramShape(skewFraction = 0.08f, leanRight = true) }
            Surface(
                color = Color(0xEE080B14),
                shape = bannerShape,
                border = BorderStroke(2.5.dp, UltiGold),
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { rotationZ = -2.8f }
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1A0910),
                                    Color(0xFF0A0F1E),
                                    Color(0xFF1A0910)
                                )
                            )
                        )
                        .padding(horizontal = 36.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = world.bannerText,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Black
                        ),
                        color = UltiGold
                    )
                }
            }
        }

        // 6. Translucent Corner Arcade Touch Controls (hidden during Ulti / Match Over cinematics)
        if (world.ultiCinematic == null && world.phase != MatchPhase.MATCH_OVER) {
            ArcadeControls(
                fighter = controlledFighter.def,
                runtime = controlledFighter,
                onMoveLeftChanged = { viewModel.setMoveLeft(it) },
                onMoveRightChanged = { viewModel.setMoveRight(it) },
                onJumpPressed = { viewModel.triggerJump() },
                onBlockChanged = { viewModel.setBlock(it) },
                onClassicPressed = { viewModel.triggerClassic() },
                onBaseAbilityPressed = { viewModel.triggerBaseAbility() },
                onUltiPressed = { viewModel.triggerUlti() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // 7. 2-Second Slow-Motion Ulti Anime Cut-In Cinematic!
        world.ultiCinematic?.let { cine ->
            UltiCinematicOverlay(
                cinematic = cine,
                timeMs = timeMs,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 8. Custom Animated Victory Cinematic per Character!
        if (world.phase == MatchPhase.MATCH_OVER) {
            VictoryCinematicOverlay(
                world = world,
                localSide = localSide,
                onRematchLocal = onRematchLocal,
                onExitBattle = onExitBattle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 9. Online Disconnect Error Overlay
        if (onlineError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, CrimsonHp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("CONNEXION SALON", style = MaterialTheme.typography.headlineMedium, color = CrimsonHp)
                        Text(onlineError!!, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
                        Button(onClick = onExitBattle) {
                            Text("RETOUR AU MENU")
                        }
                    }
                }
            }
        }
    }

    showMoveListFor?.let { fighter ->
        MoveListSheet(
            fighter = fighter,
            onDismiss = { showMoveListFor = null }
        )
    }

    if (showTrainingSettings) {
        TrainingSettingsSheet(
            world = world,
            onSelectOpponentMode = { viewModel.setTrainingOpponentMode(it) },
            onSelectBotDifficulty = { viewModel.setTrainingBotDifficulty(it) },
            onToggleAutoRegen = { viewModel.toggleTrainingAutoRegen() },
            onToggleInfiniteUlti = { viewModel.toggleTrainingInfiniteUlti() },
            onSwitchFighters = { p1, p2 -> viewModel.switchTrainingFighters(p1, p2) },
            onDismiss = { showTrainingSettings = false }
        )
    }
}

private fun skewedParallelogramShape(skewFraction: Float = 0.05f, leanRight: Boolean = true): Shape =
    GenericShape { size, _ ->
        val skew = (size.width * skewFraction).coerceAtMost(size.height * 0.9f)
        if (leanRight) {
            moveTo(skew, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - skew, size.height)
            lineTo(0f, size.height)
        } else {
            moveTo(0f, 0f)
            lineTo(size.width - skew, 0f)
            lineTo(size.width, size.height)
            lineTo(skew, size.height)
        }
        close()
    }

@Composable
private fun CombatHudHeader(
    world: ArenaWorldState,
    localSide: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onExit: () -> Unit
) {
    val timerShape = remember { skewedParallelogramShape(skewFraction = 0.11f, leanRight = true) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xD805070F),
                        Color(0x8805070F),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FighterAvatar(fighter = world.p1.def, size = 42.dp)
            FighterHealthAndUlti(
                fighter = world.p1,
                roundsWon = world.p1RoundsWon,
                isLocal = localSide == 1,
                alignRight = false,
                modifier = Modifier.weight(1f)
            )

            Surface(
                color = Color(0xF0080B14),
                shape = timerShape,
                border = BorderStroke(1.8.dp, UltiGold.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (world.mode == GameMode.TRAINING) "∞" else "${world.roundTimeRemainingSec}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (world.mode == GameMode.TRAINING) EmeraldHp else UltiGold
                    )
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("battle_sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Activer le son" else "Couper le son",
                            tint = if (isMuted) CrimsonHp else ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("battle_exit_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Quitter le combat",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            FighterHealthAndUlti(
                fighter = world.p2,
                roundsWon = world.p2RoundsWon,
                isLocal = localSide == 2,
                alignRight = true,
                modifier = Modifier.weight(1f)
            )
            FighterAvatar(fighter = world.p2.def, size = 42.dp)
        }
    }
}

@Composable
private fun FighterHealthAndUlti(
    fighter: FighterRuntime,
    roundsWon: Int,
    isLocal: Boolean,
    alignRight: Boolean,
    modifier: Modifier = Modifier
) {
    val hpRatio = (fighter.hp.toFloat() / fighter.def.stats.maxHp.toFloat()).coerceIn(0f, 1f)
    val trailingRatio by animateFloatAsState(
        targetValue = hpRatio,
        animationSpec = tween(durationMillis = 550),
        label = "hpTrail"
    )
    val hpBrush = when {
        hpRatio > 0.5f -> Brush.horizontalGradient(listOf(EmeraldHp, Color(0xFF00E5FF)))
        hpRatio > 0.25f -> Brush.horizontalGradient(listOf(FlameOrange, UltiGold))
        else -> Brush.horizontalGradient(listOf(CrimsonHp, Color(0xFFFF6E6E)))
    }
    val ultiRatio = (fighter.ultiMeter / 100f).coerceIn(0f, 1f)
    val hpShape = remember(alignRight) { skewedParallelogramShape(skewFraction = 0.045f, leanRight = !alignRight) }
    val ultiShape = remember(alignRight) { skewedParallelogramShape(skewFraction = 0.035f, leanRight = !alignRight) }

    Column(
        modifier = modifier,
        horizontalAlignment = if (alignRight) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!alignRight) {
                Text(
                    text = fighter.def.name.uppercase() + if (isLocal) " · VOUS" else "",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White,
                    maxLines = 1
                )
                RoundPips(roundsWon, fighter.def.primaryColor)
            } else {
                RoundPips(roundsWon, fighter.def.primaryColor)
                Text(
                    text = fighter.def.name.uppercase() + if (isLocal) " · VOUS" else "",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White,
                    maxLines = 1
                )
            }
        }

        // Slanted Guilty Gear Strive Parallelogram HP Bar + Crimson Damage Trail + Segment Notches
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(hpShape)
                .background(Color(0xEE060810))
                .border(1.4.dp, Color.White.copy(alpha = 0.72f), hpShape)
        ) {
            val fillAlignment = if (alignRight) Alignment.CenterStart else Alignment.CenterEnd
            Box(
                modifier = Modifier
                    .fillMaxWidth(trailingRatio.coerceAtLeast(hpRatio))
                    .fillMaxHeight()
                    .align(fillAlignment)
                    .background(Color(0xFFFF2A5F))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(hpRatio)
                    .fillMaxHeight()
                    .align(fillAlignment)
                    .background(hpBrush)
            )
            // Top metallic gleam
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(Color.White.copy(alpha = 0.30f))
            )
            // Guilty Gear subtle 25% bar divider notches
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .size(width = 1.dp, height = 14.dp)
                            .background(Color(0x6604060D))
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${fighter.hp}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White.copy(alpha = 0.85f)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .height(5.dp)
                    .clip(ultiShape)
                    .background(Color(0xCC060810))
                    .border(0.8.dp, ObsidianOutline, ultiShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ultiRatio)
                        .fillMaxHeight()
                        .align(if (alignRight) Alignment.CenterStart else Alignment.CenterEnd)
                        .background(
                            if (fighter.ultiMeter >= 100) {
                                Brush.horizontalGradient(listOf(UltiGold, Color.White, UltiGold))
                            } else {
                                Brush.horizontalGradient(listOf(fighter.def.primaryColor, ElectricCyan))
                            }
                        )
                )
            }
            Text(
                text = if (fighter.ultiMeter >= 100) "ULTI !" else "${fighter.ultiMeter}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Black
                ),
                color = if (fighter.ultiMeter >= 100) UltiGold else ElectricCyan
            )
        }
    }
}

@Composable
private fun RoundPips(roundsWon: Int, accentColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(2) { idx ->
            val won = idx < roundsWon
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .graphicsLayer { rotationZ = 45f }
                    .background(if (won) UltiGold else Color(0x661C2438))
                    .border(1.dp, if (won) Color.White else accentColor.copy(alpha = 0.5f))
            )
        }
    }
}

@Composable
private fun TrainingQuickToolbar(
    training: TrainingHudState,
    p1Name: String,
    p2Name: String,
    onResetHealth: () -> Unit,
    onOpenMoveList: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = ObsidianSurfaceVariant.copy(alpha = 0.80f),
        border = BorderStroke(1.dp, ObsidianOutline.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onResetHealth,
                color = EmeraldHp.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, EmeraldHp),
                modifier = Modifier.testTag("training_reset_hp_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = EmeraldHp, modifier = Modifier.size(14.dp))
                    Text("RESET PV", style = MaterialTheme.typography.labelSmall, color = EmeraldHp)
                }
            }

            Surface(
                onClick = onOpenMoveList,
                color = ElectricCyan.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ElectricCyan),
                modifier = Modifier.testTag("training_movelist_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                    Text("COUPS ($p1Name)", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                }
            }

            Surface(
                onClick = onOpenSettings,
                color = FlameOrange.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, FlameOrange),
                modifier = Modifier.testTag("training_settings_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(14.dp))
                    val oppSummary = if (training.opponentMode == TrainingOpponentMode.AI_BOT) {
                        "$p2Name: IA ${training.botDifficulty.label}"
                    } else {
                        "$p2Name: ${training.opponentMode.label}"
                    }
                    Text(oppSummary, style = MaterialTheme.typography.labelSmall, color = FlameOrange)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingSettingsSheet(
    world: ArenaWorldState,
    onSelectOpponentMode: (TrainingOpponentMode) -> Unit,
    onSelectBotDifficulty: (BotDifficulty) -> Unit,
    onToggleAutoRegen: () -> Unit,
    onToggleInfiniteUlti: () -> Unit,
    onSwitchFighters: (p1Id: String, p2Id: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "OPTIONS D'ENTRAÎNEMENT",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Text("COMPORTEMENT DU MANNEQUIN", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrainingOpponentMode.entries.forEach { mode ->
                    FilterChip(
                        selected = world.training.opponentMode == mode,
                        onClick = { onSelectOpponentMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }

            if (world.training.opponentMode == TrainingOpponentMode.AI_BOT) {
                Text("DIFFICULTÉ DE L'IA", style = MaterialTheme.typography.labelSmall, color = FlameOrange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = world.training.botDifficulty == diff,
                            onClick = { onSelectBotDifficulty(diff) },
                            label = { Text(diff.label) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Régénération auto des PV", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Switch(checked = world.training.autoRegenHp, onCheckedChange = { onToggleAutoRegen() })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Jauge d'Ultime infinie (100%)", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Switch(checked = world.training.infiniteUlti, onCheckedChange = { onToggleInfiniteUlti() })
            }

            Text("CHANGER VOTRE COMBATTANT (J1)", style = MaterialTheme.typography.labelSmall, color = UltiGold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FighterRoster.fighters.forEach { f ->
                    FilterChip(
                        selected = world.p1.def.id == f.id,
                        onClick = { onSwitchFighters(f.id, world.p2.def.id) },
                        label = { Text(f.name) }
                    )
                }
            }

            Text("CHANGER LE MANNEQUIN (J2)", style = MaterialTheme.typography.labelSmall, color = UltiGold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FighterRoster.fighters.forEach { f ->
                    FilterChip(
                        selected = world.p2.def.id == f.id,
                        onClick = { onSwitchFighters(world.p1.def.id, f.id) },
                        label = { Text(f.name) }
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("REPRENDRE L'ENTRAÎNEMENT")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
