package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.MatchHistoryEntry
import com.example.data.firebase.PlayerProfile
import com.example.game.data.FighterRoster
import com.example.ui.components.FighterAvatar
import com.example.ui.theme.CrimsonHp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: PlayerProfile?,
    fallbackName: String,
    matchHistoryState: UiState<List<MatchHistoryEntry>>,
    onSetFavoriteFighter: (String) -> Unit,
    onBack: () -> Unit
) {
    val favFighter = FighterRoster.getFighter(profile?.favoriteFighterId ?: "glitcherman")
    val playerName = profile?.displayName ?: fallbackName
    val wins = profile?.wins ?: 0L
    val losses = profile?.losses ?: 0L
    val totalMatches = wins + losses
    val winRatePercent = if (totalMatches > 0L) ((wins * 100f) / totalMatches).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    modifier = Modifier.testTag("profile_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Column {
                    Text(
                        text = "PROFIL DE COMBATTANT",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Text(
                        text = "$playerName • Ratio V/D & Historique des matchs",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }
            Surface(
                color = UltiGold.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, UltiGold)
            ) {
                Text(
                    text = "$totalMatches COMBATS",
                    style = MaterialTheme.typography.labelLarge,
                    color = UltiGold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Main 2-Column Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT COLUMN: W/L Ratio Ring + Detailed Combat Stats + Favorite Picker
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Win/Loss Ring Canvas
                        Box(
                            modifier = Modifier.size(104.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 12.dp.toPx()
                                drawArc(
                                    color = ObsidianOutline,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                if (totalMatches > 0L) {
                                    val winSweep = (wins.toFloat() / totalMatches.toFloat()) * 360f
                                    drawArc(
                                        color = CrimsonHp,
                                        startAngle = -90f + winSweep,
                                        sweepAngle = 360f - winSweep,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = EmeraldHp,
                                        startAngle = -90f,
                                        sweepAngle = winSweep,
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$winRatePercent%",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = if (winRatePercent >= 50) EmeraldHp else UltiGold
                                )
                                Text("RATIO V/D", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }

                        // Identity + W/L Counter
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = playerName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatBadge(label = "$wins VICTOIRES", color = EmeraldHp)
                                StatBadge(label = "$losses DÉFAITES", color = CrimsonHp)
                            }
                            Text(
                                text = "Favori : ${favFighter.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = favFighter.primaryColor
                            )
                        }
                    }

                    // 4 Combat Metrics Cards (2 rows of 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileMetricCard(
                            label = "SÉRIE ACTUELLE",
                            value = "${profile?.winStreak ?: 0L}",
                            accent = FlameOrange,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileMetricCard(
                            label = "RECORD SÉRIE",
                            value = "${profile?.bestStreak ?: 0L}",
                            accent = UltiGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileMetricCard(
                            label = "DÉGÂTS TOTAUX",
                            value = "${profile?.totalDamageDealt ?: 0L}",
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileMetricCard(
                            label = "ROUNDS GAGNÉS",
                            value = "${profile?.roundsWonTotal ?: 0L}",
                            accent = EmeraldHp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Favorite Fighter Selector
                    Text(
                        text = "COMBATTANT FAVORI",
                        style = MaterialTheme.typography.labelLarge,
                        color = UltiGold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FighterRoster.fighters.forEach { fighter ->
                            val selected = fighter.id == favFighter.id
                            Surface(
                                onClick = { onSetFavoriteFighter(fighter.id) },
                                color = if (selected) fighter.primaryColor.copy(alpha = 0.22f) else ObsidianBg,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) fighter.primaryColor else ObsidianOutline
                                ),
                                modifier = Modifier.testTag("profile_fav_${fighter.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = fighter.portraitRes),
                                        contentDescription = fighter.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Text(
                                        text = fighter.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: Chronological Match History
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, UltiGold.copy(alpha = 0.45f)),
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = UltiGold, modifier = Modifier.size(20.dp))
                        Text(
                            text = "DERNIERS AFFRONTEMENTS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = UltiGold
                        )
                    }

                    when (matchHistoryState) {
                        is UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ElectricCyan)
                            }
                        }

                        is UiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(matchHistoryState.message, color = CrimsonHp)
                            }
                        }

                        is UiState.Success -> {
                            val entries = matchHistoryState.data
                            if (entries.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                                        Text(
                                            text = "Aucun combat enregistré pour le moment.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Terminez un Duel Bot, En ligne ou Tournoi pour remplir votre historique !",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricCyan
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    entries.forEach { entry ->
                                        MatchHistoryRowCard(entry)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.65f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ProfileMetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = accent
            )
        }
    }
}

@Composable
private fun MatchHistoryRowCard(entry: MatchHistoryEntry) {
    val myFighter = FighterRoster.getFighter(entry.myFighterId)
    val oppFighter = FighterRoster.getFighter(entry.opponentFighterId)
    val stage = FighterRoster.getStage(entry.stageId)
    val resultColor = if (entry.won) EmeraldHp else CrimsonHp
    val modeLabel = when (entry.mode) {
        "ONLINE" -> "EN LIGNE"
        "TOURNAMENT" -> "TOURNOI"
        else -> "VS BOT"
    }
    val dateStr = entry.createdAt?.toDate()?.let {
        SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(it)
    } ?: "Récent"

    Surface(
        color = ObsidianBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, resultColor.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FighterAvatar(fighter = myFighter, size = 38.dp)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (entry.won) "VICTOIRE" else "DÉFAITE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = resultColor
                    )
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "${myFighter.name} vs ${oppFighter.name} (${entry.opponentName})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${stage.name} • ${entry.damageDealt} dégâts infligés",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.myRoundsWon} - ${entry.opponentRoundsWon}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = UltiGold
                )
            }

            FighterAvatar(fighter = oppFighter, size = 34.dp)
        }
    }
}
