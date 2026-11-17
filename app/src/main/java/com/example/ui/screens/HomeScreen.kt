package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.firebase.PlayerProfile
import com.example.game.audio.LocalSoundPlayer
import com.example.game.audio.SoundEffect
import com.example.game.data.AdminCombatModifiers
import com.example.game.data.FighterRoster
import com.example.ui.components.AdminPanelDialog
import com.example.ui.components.FighterAvatar
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

@Composable
fun HomeScreen(
    profile: PlayerProfile?,
    fallbackName: String,
    isAdmin: Boolean,
    verifiedEmail: String,
    adminModifiers: AdminCombatModifiers,
    adminStatusMessage: String?,
    onUpdateAdminModifiers: (AdminCombatModifiers) -> Unit,
    onAdminBoostWins: () -> Unit,
    onAdminResetStats: () -> Unit,
    onClearAdminStatus: () -> Unit,
    onOpenProfile: () -> Unit,
    onPlayVsBot: () -> Unit,
    onPlayTraining: () -> Unit,
    onPlayOnline: () -> Unit,
    onPlayTournament: () -> Unit,
    onOpenRoster: () -> Unit,
    onOpenPlaytestShare: () -> Unit,
    onSignOut: () -> Unit
) {
    val soundPlayer = LocalSoundPlayer.current
    val isMuted by soundPlayer.isMuted.collectAsStateWithLifecycle()

    var showAdminPanel by remember { mutableStateOf(false) }

    val favFighter = FighterRoster.getFighter(profile?.favoriteFighterId ?: "glitcherman")
    val playerName = profile?.displayName ?: fallbackName
    val wins = profile?.wins ?: 0L
    val losses = profile?.losses ?: 0L
    val hasActiveCheats = adminModifiers.godModeP1 || adminModifiers.infiniteUltiP1 || adminModifiers.damageMultiplierP1 > 1f

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBg)
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // LEFT COLUMN: Clickable Profile Card + Hero Banner + Playtest Share Card
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clickable Profile Card -> Opens Profile & Stats View
                    Surface(
                        onClick = {
                            soundPlayer.play(SoundEffect.UI_CLICK)
                            onOpenProfile()
                        },
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_profile_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FighterAvatar(fighter = favFighter, size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$wins V • $losses D • Voir le profil",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Ouvrir le profil",
                                tint = ElectricCyan
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        if (isAdmin) {
                            Surface(
                                onClick = {
                                    soundPlayer.play(SoundEffect.UI_CLICK)
                                    onClearAdminStatus()
                                    showAdminPanel = true
                                },
                                color = if (hasActiveCheats) UltiGold else UltiGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, UltiGold),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("admin_panel_button")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = "⚡ ADMIN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (hasActiveCheats) ObsidianBg else UltiGold
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { soundPlayer.toggleMute() },
                            modifier = Modifier.testTag("home_sound_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Activer les sons" else "Couper les sons",
                                tint = if (isMuted) TextSecondary else ElectricCyan
                            )
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Se déconnecter",
                                tint = TextSecondary
                            )
                        }
                    }
                }

                // Hero Arena Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_arena),
                        contentDescription = "Arène Clash Arena",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ObsidianBg.copy(alpha = 0.94f),
                                        ObsidianBg.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CLASH ARENA",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Text(
                            text = "4 combattants SF6 • 5 arènes • Tournois & En ligne",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                    }
                }

                // Share & Feedback Card
                ModeActionCard(
                    title = "PARTAGER & RETOURS",
                    subtitle = "Invitez des amis et notez votre session",
                    badge = "COMMUNAUTÉ",
                    accentColor = Color(0xFFFF2A85),
                    icon = Icons.Default.Share,
                    testTag = "mode_playtest_share_card",
                    onClick = onOpenPlaytestShare
                )
            }

            // RIGHT COLUMN: Game Mode Cards
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                ModeActionCard(
                    title = "DUEL CONTRE BOT",
                    subtitle = "Affrontez l'IA en 2 rounds gagnants",
                    badge = "SOLO",
                    accentColor = FlameOrange,
                    icon = Icons.Default.SmartToy,
                    testTag = "mode_vs_bot_card",
                    onClick = onPlayVsBot
                )

                ModeActionCard(
                    title = "TOURNOI CHAMPIONNAT",
                    subtitle = "Bracket à 2, 3, 4, 8 ou 10 combattants",
                    badge = "TOURNOI",
                    accentColor = UltiGold,
                    icon = Icons.Default.EmojiEvents,
                    testTag = "mode_tournament_card",
                    onClick = onPlayTournament
                )

                ModeActionCard(
                    title = "DUEL EN LIGNE",
                    subtitle = "Salons temps réel avec revanche directe",
                    badge = "MULTIJOUEUR",
                    accentColor = ElectricCyan,
                    icon = Icons.Default.Public,
                    testTag = "mode_online_card",
                    onClick = onPlayOnline
                )

                ModeActionCard(
                    title = "ENTRAÎNEMENT",
                    subtitle = "Testez vos coups librement contre le mannequin",
                    badge = "LIBRE",
                    accentColor = EmeraldHp,
                    icon = Icons.Default.FitnessCenter,
                    testTag = "mode_training_card",
                    onClick = onPlayTraining
                )

                ModeActionCard(
                    title = "COMBATTANTS & STATS",
                    subtitle = "Fiches et liste des coups des 4 personnages",
                    badge = "ROSTER",
                    accentColor = Color(0xFFAB47BC),
                    icon = Icons.Default.Groups,
                    testTag = "mode_roster_card",
                    onClick = onOpenRoster
                )
            }
        }

        if (showAdminPanel && isAdmin) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                AdminPanelDialog(
                    verifiedEmail = verifiedEmail,
                    isInBattle = false,
                    modifiers = adminModifiers,
                    statusMessage = adminStatusMessage,
                    onUpdateModifiers = onUpdateAdminModifiers,
                    onBoostWins = onAdminBoostWins,
                    onResetStats = onAdminResetStats,
                    onDismiss = { showAdminPanel = false }
                )
            }
        }
    }
}

@Composable
private fun ModeActionCard(
    title: String,
    subtitle: String,
    badge: String,
    accentColor: Color,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val soundPlayer = LocalSoundPlayer.current
    Surface(
        onClick = {
            soundPlayer.play(SoundEffect.UI_CLICK)
            onClick()
        },
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
