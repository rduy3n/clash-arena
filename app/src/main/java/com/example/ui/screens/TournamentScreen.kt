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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.data.BotDifficulty
import com.example.game.data.FighterRoster
import com.example.game.data.TournamentEngine
import com.example.game.data.TournamentMatch
import com.example.game.data.TournamentParticipant
import com.example.game.data.TournamentState
import com.example.ui.components.FighterAvatar
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TournamentScreen(
    mainViewModel: MainViewModel,
    favoriteFighterId: String,
    onLaunchHumanMatch: (
        matchId: String,
        p1FighterId: String,
        p2FighterId: String,
        stageId: String,
        difficulty: BotDifficulty,
        opponentName: String
    ) -> Unit,
    onBack: () -> Unit
) {
    val tournamentState by mainViewModel.tournamentState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("tournament_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Column {
                    Text(
                        text = if (tournamentState == null) "MODE TOURNOI" else "TOURNOI • ${tournamentState!!.size} COMBATTANTS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = UltiGold
                    )
                    Text(
                        text = if (tournamentState == null) {
                            "Configurez votre bracket à 2, 3, 4, 8 ou 10 combattants"
                        } else {
                            tournamentState?.lastCompletedNote ?: "Progression vers le titre de Champion"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }

            if (tournamentState != null) {
                OutlinedButton(
                    onClick = { mainViewModel.resetTournament() },
                    border = BorderStroke(1.dp, FlameOrange),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = FlameOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("NOUVEAU TOURNOI", color = FlameOrange)
                }
            }
        }

        val state = tournamentState
        if (state == null) {
            TournamentSetupContent(
                defaultFighterId = favoriteFighterId,
                onGenerateBracket = { size, fighterId, difficulty, stageId ->
                    mainViewModel.startTournament(size, fighterId, difficulty, stageId)
                }
            )
        } else {
            TournamentBracketContent(
                state = state,
                onSimulateNextAi = { mainViewModel.simulateNextAiTournamentMatch() },
                onSimulateAllAi = { mainViewModel.simulateAllReadyAiMatches() },
                onPlayHumanMatch = { match ->
                    val humanIsP1 = match.p1?.isHuman == true
                    val p1 = match.p1 ?: return@TournamentBracketContent
                    val p2 = match.p2 ?: return@TournamentBracketContent
                    val humanFighter = if (humanIsP1) p1.fighterId else p2.fighterId
                    val oppFighter = if (humanIsP1) p2.fighterId else p1.fighterId
                    val oppName = if (humanIsP1) p2.name else p1.name
                    val stage = if (state.stageId == "random") {
                        FighterRoster.stages.random().id
                    } else {
                        state.stageId
                    }
                    onLaunchHumanMatch(
                        match.id,
                        humanFighter,
                        oppFighter,
                        stage,
                        state.difficulty,
                        oppName
                    )
                },
                onNewTournament = { mainViewModel.resetTournament() }
            )
        }
    }
}

@Composable
private fun TournamentSetupContent(
    defaultFighterId: String,
    onGenerateBracket: (size: Int, fighterId: String, difficulty: BotDifficulty, stageId: String) -> Unit
) {
    var selectedSize by remember { mutableIntStateOf(4) }
    var selectedFighterId by remember { mutableStateOf(defaultFighterId) }
    var selectedDifficulty by remember { mutableStateOf(BotDifficulty.NORMAL) }
    var selectedStageId by remember { mutableStateOf("random") }
    val selectedFighter = FighterRoster.getFighter(selectedFighterId)

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left: Bracket Size + Difficulty + Stage
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, UltiGold.copy(alpha = 0.5f)),
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("NOMBRE DE PARTICIPANTS", style = MaterialTheme.typography.labelLarge, color = UltiGold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TournamentEngine.supportedSizes.forEach { size ->
                        val active = selectedSize == size
                        Surface(
                            onClick = { selectedSize = size },
                            color = if (active) UltiGold else ObsidianBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, if (active) Color.White else ObsidianOutline),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("tournament_size_$size")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$size",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = if (active) ObsidianBg else Color.White
                                )
                            }
                        }
                    }
                }
                Text(
                    text = when (selectedSize) {
                        2 -> "Grande Finale directe (1v1)."
                        3 -> "1 tête de série exemptée (BYE) directement en Finale, 1 demi-finale."
                        4 -> "2 Demi-finales puis Finale."
                        8 -> "Quarts de finale, Demi-finales puis Finale."
                        else -> "Bracket à 10 joueurs : 2 matchs préliminaires + 6 têtes de série exemptées en Quarts."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Text("DIFFICULTÉ DE L'IA", style = MaterialTheme.typography.labelLarge, color = FlameOrange)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = selectedDifficulty == diff,
                            onClick = { selectedDifficulty = diff },
                            label = { Text(diff.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlameOrange.copy(alpha = 0.25f),
                                selectedLabelColor = FlameOrange
                            )
                        )
                    }
                }

                Text("ARÈNE DU TOURNOI", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStageId == "random",
                        onClick = { selectedStageId = "random" },
                        label = { Text("Aléatoire", style = MaterialTheme.typography.labelSmall) }
                    )
                    FighterRoster.stages.forEach { stage ->
                        FilterChip(
                            selected = selectedStageId == stage.id,
                            onClick = { selectedStageId = stage.id },
                            label = { Text(stage.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        onGenerateBracket(selectedSize, selectedFighterId, selectedDifficulty, selectedStageId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UltiGold, contentColor = ObsidianBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_tournament_button")
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GÉNÉRER LE TOURNOI ($selectedSize)", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                }
            }
        }

        // Right: Your Fighter Showcase & Picker
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, selectedFighter.primaryColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .weight(0.95f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("VOTRE COMBATTANT (TÊTE DE SÉRIE #1)", style = MaterialTheme.typography.labelLarge, color = selectedFighter.primaryColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = selectedFighter.portraitRes),
                        contentDescription = selectedFighter.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, selectedFighter.primaryColor, RoundedCornerShape(14.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(selectedFighter.name, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text(selectedFighter.archetype, style = MaterialTheme.typography.labelLarge, color = selectedFighter.primaryColor)
                        Text("${selectedFighter.stats.maxHp} PV • ${selectedFighter.ultimateMove.name}", style = MaterialTheme.typography.bodyMedium, color = UltiGold)
                        Text(selectedFighter.lore, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 3)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FighterRoster.fighters.forEach { f ->
                        val picked = f.id == selectedFighterId
                        Surface(
                            onClick = { selectedFighterId = f.id },
                            color = if (picked) f.primaryColor.copy(alpha = 0.24f) else ObsidianBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(if (picked) 2.dp else 1.dp, if (picked) f.primaryColor else ObsidianOutline)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FighterAvatar(fighter = f, size = 30.dp)
                                Text(f.name, style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentBracketContent(
    state: TournamentState,
    onSimulateNextAi: () -> Unit,
    onSimulateAllAi: () -> Unit,
    onPlayHumanMatch: (TournamentMatch) -> Unit,
    onNewTournament: () -> Unit
) {
    val nextMatch = state.nextPlayableMatch
    val champion = state.champion

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Action / Champion Bar
        if (champion != null) {
            val champFighter = FighterRoster.getFighter(champion.fighterId)
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, UltiGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(UltiGold.copy(alpha = 0.22f), ObsidianSurface)
                            )
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FighterAvatar(fighter = champFighter, size = 54.dp)
                        Column {
                            Text(
                                text = "CHAMPION DU TOURNOI !",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                color = UltiGold
                            )
                            Text(
                                text = "${champion.name} (${champFighter.name})",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                    }
                    Button(
                        onClick = onNewTournament,
                        colors = ButtonDefaults.buttonColors(containerColor = UltiGold, contentColor = ObsidianBg),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("REJOUER UN TOURNOI", fontWeight = FontWeight.Black)
                    }
                }
            }
        } else if (nextMatch != null) {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, if (nextMatch.involvesHuman) EmeraldHp else ElectricCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nextMatch.involvesHuman) "VOTRE PROCHAIN COMBAT EST PRÊT !" else "MATCH IA EN ATTENTE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = if (nextMatch.involvesHuman) EmeraldHp else ElectricCyan
                        )
                        Text(
                            text = "${nextMatch.p1?.name ?: "?"} vs ${nextMatch.p2?.name ?: "?"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    if (nextMatch.involvesHuman) {
                        Button(
                            onClick = { onPlayHumanMatch(nextMatch) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = ObsidianBg),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("tournament_play_match_button")
                        ) {
                            Icon(Icons.Default.SportsMma, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COMBATTRE !", fontWeight = FontWeight.Black)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onSimulateNextAi,
                                border = BorderStroke(1.dp, ElectricCyan),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SIMULER 1 IA", color = ElectricCyan)
                            }
                            Button(
                                onClick = onSimulateAllAi,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("tournament_simulate_ai_button")
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SIMULER IA", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Interactive Horizontal Bracket Tree
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            state.rounds.forEach { round ->
                Column(
                    modifier = Modifier
                        .width(236.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, UltiGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = round.title,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                            color = UltiGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    round.matches.forEach { match ->
                        val isNext = match.id == nextMatch?.id
                        BracketMatchCard(
                            match = match,
                            isHighlighted = isNext,
                            onPlay = if (isNext && match.involvesHuman) {
                                { onPlayHumanMatch(match) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BracketMatchCard(
    match: TournamentMatch,
    isHighlighted: Boolean,
    onPlay: (() -> Unit)? = null
) {
    val borderColor = when {
        match.winner != null -> EmeraldHp.copy(alpha = 0.65f)
        isHighlighted -> UltiGold
        else -> ObsidianOutline
    }
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isHighlighted) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BracketSlotRow(
                participant = match.p1,
                score = if (match.winner != null && !match.isByeMatch) match.p1Score else null,
                isWinner = match.winner != null && match.winner.id == match.p1?.id
            )
            BracketSlotRow(
                participant = match.p2,
                score = if (match.winner != null && !match.isByeMatch) match.p2Score else null,
                isWinner = match.winner != null && match.winner.id == match.p2?.id,
                isByeSlot = match.isByeMatch && match.p2 == null
            )

            if (match.isByeMatch && match.winner != null) {
                Text(
                    text = "QUALIFIÉ D'OFFICE (BYE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            } else if (onPlay != null) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = UltiGold, contentColor = ObsidianBg),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("JOUER CE MATCH", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                }
            }
        }
    }
}

@Composable
private fun BracketSlotRow(
    participant: TournamentParticipant?,
    score: Int?,
    isWinner: Boolean,
    isByeSlot: Boolean = false
) {
    val fighter = participant?.let { FighterRoster.getFighter(it.fighterId) }
    val bgColor = when {
        isWinner -> EmeraldHp.copy(alpha = 0.16f)
        participant?.isHuman == true -> ElectricCyan.copy(alpha = 0.12f)
        else -> ObsidianBg
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (fighter != null) {
                FighterAvatar(fighter = fighter, size = 24.dp)
            }
            Text(
                text = when {
                    participant != null -> participant.name
                    isByeSlot -> "BYE"
                    else -> "À déterminer"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isWinner || participant?.isHuman == true) FontWeight.Black else FontWeight.Normal
                ),
                color = when {
                    isWinner -> EmeraldHp
                    participant?.isHuman == true -> ElectricCyan
                    participant != null -> Color.White
                    else -> TextSecondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (score != null) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = if (isWinner) UltiGold else TextSecondary
            )
        }
    }
}
