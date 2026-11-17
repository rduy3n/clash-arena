package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.data.AdminCombatModifiers
import com.example.ui.theme.CrimsonHp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold

@Composable
fun AdminPanelDialog(
    verifiedEmail: String,
    isInBattle: Boolean,
    modifiers: AdminCombatModifiers,
    statusMessage: String? = null,
    canFinishMatchLocally: Boolean = false,
    onUpdateModifiers: (AdminCombatModifiers) -> Unit,
    onHealP1: () -> Unit = {},
    onHealP2: () -> Unit = {},
    onInstantFullUltiP1: () -> Unit = {},
    onInstantFullUltiP2: () -> Unit = {},
    onInstantKoP2: () -> Unit = {},
    onWinMatchP1: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onBoostWins: () -> Unit = {},
    onResetStats: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(2.dp, UltiGold),
        modifier = Modifier
            .widthIn(max = 580.dp)
            .fillMaxWidth(0.92f)
            .padding(12.dp)
            .testTag("admin_panel_dialog")
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = UltiGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "PANEL ADMINISTRATEUR",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = UltiGold
                    )
                }
                Surface(
                    color = EmeraldHp.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EmeraldHp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldHp, modifier = Modifier.size(14.dp))
                        Text(
                            text = verifiedEmail.ifBlank { "ADMIN VÉRIFIÉ" },
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldHp
                        )
                    }
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ElectricCyan)
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Section 1: In-Battle Real-Time Controls
            if (isInBattle) {
                Text("COMMANDES COMBAT EN DIRECT", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onHealP1,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("SOIN J1 MAX", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onHealP2,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("SOIN J2 MAX", fontWeight = FontWeight.Bold)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onInstantFullUltiP1,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("ULTI J1 100%", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onInstantFullUltiP2,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("ULTI J2 100%", fontWeight = FontWeight.Bold)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onResetTimer,
                        border = BorderStroke(1.2.dp, UltiGold),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("TIMER 99s", color = UltiGold, fontWeight = FontWeight.Bold)
                    }
                    if (canFinishMatchLocally) {
                        Button(
                            onClick = onInstantKoP2,
                            colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("K.O. J2", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onWinMatchP1,
                            colors = ButtonDefaults.buttonColors(containerColor = UltiGold, contentColor = ObsidianBg),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("WIN MATCH", fontWeight = FontWeight.Black)
                        }
                    }
                }

                HorizontalDivider(color = ObsidianOutline)
            }

            // Section 2: Global / Live Combat Modifiers
            Text("MODIFICATEURS DE COMBAT (J1)", style = MaterialTheme.typography.labelLarge, color = UltiGold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dégâts J1 :", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                listOf(1f to "x1 Normal", 2f to "x2 Fort", 3f to "x3 Boss").forEach { (mult, label) ->
                    FilterChip(
                        selected = modifiers.damageMultiplierP1 == mult,
                        onClick = { onUpdateModifiers(modifiers.copy(damageMultiplierP1 = mult)) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = UltiGold.copy(alpha = 0.25f),
                            selectedLabelColor = UltiGold
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mode Dieu J1 (PV permanents 100%)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Actif en Duel Bot & Tournoi", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Switch(
                    checked = modifiers.godModeP1,
                    onCheckedChange = { onUpdateModifiers(modifiers.copy(godModeP1 = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Jauge Ulti Infinie J1", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Ulti rechargé à 100% en continu", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Switch(
                    checked = modifiers.infiniteUltiP1,
                    onCheckedChange = { onUpdateModifiers(modifiers.copy(infiniteUltiP1 = it)) }
                )
            }

            // Section 3: Profile Administration (only from Home / outside battle)
            if (!isInBattle) {
                HorizontalDivider(color = ObsidianOutline)
                Text("ADMINISTRATION DU PROFIL", style = MaterialTheme.typography.labelLarge, color = FlameOrange)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onBoostWins,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("+5 VICTOIRES (TEST)", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onResetStats,
                        border = BorderStroke(1.2.dp, CrimsonHp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("RESET STATS (0)", color = CrimsonHp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("FERMER LE PANEL", fontWeight = FontWeight.Black)
            }
        }
    }
}
