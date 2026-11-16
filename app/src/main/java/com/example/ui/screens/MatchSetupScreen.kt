package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.data.BotDifficulty
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.ui.components.FighterAvatar
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

/**
 * Écran de sélection Arcade VS horizontal avec grands portraits J1 vs J2,
 * grille centrale des 4 combattants (Glitcherman, Jardin, Maître Chen, Daron Brésilien),
 * barres de statistiques comparatives et réglages d'arène / IA / entraînement.
 */
@Composable
fun MatchSetupScreen(
    mode: GameMode,
    defaultFighterId: String,
    onStartMatch: (
        p1Id: String,
        p2Id: String,
        stageId: String,
        difficulty: BotDifficulty,
        trainingOpponentMode: TrainingOpponentMode
    ) -> Unit,
    onBack: () -> Unit
) {
    var p1Id by remember { mutableStateOf(defaultFighterId) }
    var p2Id by remember {
        mutableStateOf(
            FighterRoster.fighters.firstOrNull { it.id != defaultFighterId }?.id ?: "jardin"
        )
    }
    var activeSlot by remember { mutableIntStateOf(1) } // 1 = J1, 2 = J2
    var stageId by remember { mutableStateOf(FighterRoster.stages.first().id) }
    var difficulty by remember { mutableStateOf(BotDifficulty.NORMAL) }
    var trainingOppMode by remember { mutableStateOf(TrainingOpponentMode.STATIONARY) }

    val p1Def = FighterRoster.getFighter(p1Id)
    val p2Def = FighterRoster.getFighter(p2Id)
    val accent = if (mode == GameMode.TRAINING) EmeraldHp else FlameOrange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TOP COMPACT BAR: Back + Title + Stage / IA / Dummy options
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("setup_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = Color.White
                )
            }
            Text(
                text = if (mode == GameMode.TRAINING) "ENTRAÎNEMENT" else "SÉLECTION VS",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stage chips
                FighterRoster.stages.forEach { stage ->
                    val selected = stage.id == stageId
                    FilterChip(
                        selected = selected,
                        onClick = { stageId = stage.id },
                        label = { Text(stage.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = stage.accentGlow.copy(alpha = 0.24f),
                            selectedLabelColor = stage.accentGlow
                        ),
                        modifier = Modifier.testTag("stage_pick_${stage.id}")
                    )
                }

                if (mode == GameMode.TRAINING) {
                    TrainingOpponentMode.entries.forEach { oppMode ->
                        FilterChip(
                            selected = trainingOppMode == oppMode,
                            onClick = { trainingOppMode = oppMode },
                            label = { Text(oppMode.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldHp.copy(alpha = 0.25f),
                                selectedLabelColor = EmeraldHp
                            ),
                            modifier = Modifier.testTag("training_opp_${oppMode.name.lowercase()}")
                        )
                    }
                }

                if (mode == GameMode.VS_BOT || (mode == GameMode.TRAINING && trainingOppMode == TrainingOpponentMode.AI_BOT)) {
                    BotDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text("IA ${diff.label}", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameOrange.copy(alpha = 0.25f),
                                selectedLabelColor = FlameOrange
                            ),
                            modifier = Modifier.testTag("difficulty_${diff.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // MAIN 3-COLUMN LANDSCAPE ARCADE VS STAGE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: P1 LARGE PORTRAIT & STATS
            FighterShowcaseCard(
                sideLabel = "J1 • VOUS",
                sideColor = ElectricCyan,
                fighter = p1Def,
                isActiveSlot = activeSlot == 1,
                onActivateSlot = { activeSlot = 1 },
                pickPrefix = "p1_pick",
                onQuickPick = { p1Id = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            // CENTER: VS CREST + 2x2 FIGHTER GRID + START MATCH CTA
            Column(
                modifier = Modifier
                    .width(248.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // VS Header & Slot Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SlotChip(
                        label = "J1",
                        selected = activeSlot == 1,
                        color = ElectricCyan,
                        onClick = { activeSlot = 1 }
                    )
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.headlineMedium.copy(fontStyle = FontStyle.Italic),
                        color = UltiGold
                    )
                    SlotChip(
                        label = if (mode == GameMode.TRAINING) "MANNEQUIN" else "J2 IA",
                        selected = activeSlot == 2,
                        color = FlameOrange,
                        onClick = { activeSlot = 2 }
                    )
                }

                // 2x2 Grid of the 4 fighters
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                ) {
                    val rows = FighterRoster.fighters.chunked(2)
                    rows.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pair.forEach { fighter ->
                                val isP1 = fighter.id == p1Id
                                val isP2 = fighter.id == p2Id
                                val borderColor = when {
                                    isP1 && isP2 -> UltiGold
                                    isP1 -> ElectricCyan
                                    isP2 -> FlameOrange
                                    else -> ObsidianOutline
                                }
                                Surface(
                                    onClick = {
                                        if (activeSlot == 1) p1Id = fighter.id else p2Id = fighter.id
                                    },
                                    color = if (isP1 || isP2) fighter.primaryColor.copy(alpha = 0.18f) else ObsidianSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(if (isP1 || isP2) 2.dp else 1.dp, borderColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        FighterAvatar(fighter = fighter, size = 44.dp)
                                        Text(
                                            text = fighter.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = when {
                                                isP1 && isP2 -> "J1 • J2"
                                                isP1 -> "J1"
                                                isP2 -> "J2"
                                                else -> fighter.archetype.substringBefore(" /")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isP1 || isP2) borderColor else TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Launch CTA
                Button(
                    onClick = {
                        onStartMatch(p1Def.id, p2Def.id, stageId, difficulty, trainingOppMode)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = if (mode == GameMode.TRAINING) ObsidianBg else Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_match_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (mode == GameMode.TRAINING) "ENTRAÎNEMENT" else "COMBATTRE !",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // RIGHT: P2 LARGE PORTRAIT & STATS
            FighterShowcaseCard(
                sideLabel = if (mode == GameMode.TRAINING) "J2 • MANNEQUIN" else "J2 • ADVERSAIRE IA",
                sideColor = FlameOrange,
                fighter = p2Def,
                isActiveSlot = activeSlot == 2,
                onActivateSlot = { activeSlot = 2 },
                pickPrefix = "p2_pick",
                onQuickPick = { p2Id = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SlotChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) color.copy(alpha = 0.24f) else ObsidianSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) color else ObsidianOutline)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) color else TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FighterShowcaseCard(
    sideLabel: String,
    sideColor: Color,
    fighter: FighterDefinition,
    isActiveSlot: Boolean,
    onActivateSlot: () -> Unit,
    pickPrefix: String,
    onQuickPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onActivateSlot,
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isActiveSlot) 2.2.dp else 1.2.dp,
            color = if (isActiveSlot) sideColor else ObsidianOutline
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Portrait + Name Hero
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(104.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, fighter.primaryColor, RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = fighter.portraitRes),
                        contentDescription = fighter.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.72f)))
                            )
                    )
                    Text(
                        text = "${fighter.stats.maxHp} PV",
                        style = MaterialTheme.typography.labelSmall,
                        color = UltiGold,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = sideLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = sideColor
                    )
                    Text(
                        text = fighter.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = fighter.archetype,
                        style = MaterialTheme.typography.labelSmall,
                        color = fighter.primaryColor
                    )

                    MiniStatBar("ATK", fighter.stats.ratingAttack, FlameOrange)
                    MiniStatBar("VIT", fighter.stats.ratingSpeed, ElectricCyan)
                    MiniStatBar("DEF", fighter.stats.ratingDefense, EmeraldHp)
                    MiniStatBar("POR", fighter.stats.ratingRange, UltiGold)

                    Text(
                        text = "ULTI: ${fighter.ultimateMove.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = UltiGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick-select row (keeps p1_pick_<id> & p2_pick_<id> always directly reachable!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FighterRoster.fighters.forEach { f ->
                    val chosen = f.id == fighter.id
                    Surface(
                        onClick = { onQuickPick(f.id) },
                        color = if (chosen) f.primaryColor.copy(alpha = 0.25f) else ObsidianBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (chosen) f.primaryColor else ObsidianOutline),
                        modifier = Modifier.testTag("${pickPrefix}_${f.id}")
                    ) {
                        Text(
                            text = f.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (chosen) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatBar(label: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(30.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ObsidianBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}
