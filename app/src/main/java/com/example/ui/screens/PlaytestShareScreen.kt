package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.PlaytestFeedback
import com.example.game.data.FighterRoster
import com.example.ui.theme.CrimsonHp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldHp
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltiGold
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun PlaytestShareScreen(
    mainViewModel: MainViewModel,
    currentUserId: String,
    defaultTesterName: String,
    favoriteFighterId: String,
    onGoToOnlineLobby: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val feedbacksState by mainViewModel.feedbacksState.collectAsStateWithLifecycle()
    val feedbackBusy by mainViewModel.feedbackBusy.collectAsStateWithLifecycle()
    val feedbackStatus by mainViewModel.feedbackStatus.collectAsStateWithLifecycle()
    val lobbyBusy by mainViewModel.lobbyBusy.collectAsStateWithLifecycle()

    var generatedRoomCode by remember { mutableStateOf<String?>(null) }
    var copiedInvite by remember { mutableStateOf(false) }

    var testerName by remember(defaultTesterName) { mutableStateOf(defaultTesterName) }
    var selectedMode by remember { mutableStateOf("IRL") }
    var selectedCategory by remember { mutableStateOf("Graphismes") }
    var selectedFighterId by remember { mutableStateOf("all") }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    val inviteMessage = remember(generatedRoomCode, defaultTesterName) {
        buildString {
            append("Viens tester Clash Arena avec moi ($defaultTesterName) !\n")
            append("4 combattants : Glitcherman, Jardin, Maître Chen et Daron Brésilien.\n")
            if (generatedRoomCode != null) {
                append("Rejoins mon salon en ligne avec le code : $generatedRoomCode\n")
            } else {
                append("Lance le mode En ligne ou Entraînement pour tester les coups et les cinématiques !\n")
            }
            append("Dis-moi ce que tu en penses directement dans l'onglet Partage & Retours du jeu.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    modifier = Modifier.testTag("playtest_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
                Column {
                    Text(
                        text = "PARTAGE & RETOURS TESTEURS",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Invitez des amis IRL ou en ligne et recueillez leurs avis en direct",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }
            OutlinedButton(
                onClick = onGoToOnlineLobby,
                border = BorderStroke(1.dp, ElectricCyan)
            ) {
                Text("OUVRIR LE SALON EN LIGNE", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
            }
        }

        // Landscape 2-Column Workspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT COLUMN: IRL & Online Sharing Card
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "PARTAGER IRL OU EN LIGNE",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = "1. Générez un code salon instantané pour affronter un ami en face-à-face (IRL) sur deux téléphones ou à distance.\n2. Pour installer sur le téléphone d'un ami IRL, exportez l'APK depuis le menu Paramètres en haut d'AI Studio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // Big IRL Display Card for Generated Code
                    Surface(
                        color = ObsidianBg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, if (generatedRoomCode != null) UltiGold else ObsidianOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (generatedRoomCode != null) "CODE SALON IRL / EN LIGNE" else "AUCUN SALON OUVERT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (generatedRoomCode != null) UltiGold else TextSecondary
                                )
                                Text(
                                    text = generatedRoomCode ?: "------",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = if (generatedRoomCode != null) Color.White else TextSecondary
                                )
                            }
                            Button(
                                onClick = {
                                    mainViewModel.createOnlineRoom(
                                        fighterId = favoriteFighterId,
                                        stageId = FighterRoster.stages.first().id,
                                        onRoomReady = { code -> generatedRoomCode = code }
                                    )
                                },
                                enabled = !lobbyBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = UltiGold, contentColor = ObsidianBg),
                                modifier = Modifier.testTag("playtest_generate_code_button")
                            ) {
                                if (lobbyBusy) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianBg)
                                } else {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CRÉER CODE", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Native Android Share Sheet + Clipboard Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Invitation Playtest — Clash Arena")
                                    putExtra(Intent.EXTRA_TEXT, inviteMessage)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Partager Clash Arena")
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlameOrange, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("playtest_share_intent_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PARTAGER", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(inviteMessage))
                                copiedInvite = true
                            },
                            border = BorderStroke(1.dp, ElectricCyan),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("playtest_copy_invite_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (copiedInvite) "COPIÉ !" else "COPIER",
                                style = MaterialTheme.typography.labelLarge,
                                color = ElectricCyan
                            )
                        }
                    }
                }
            }

            // RIGHT COLUMN: Submit Feedback Form + Live Community Feedback Wall
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, FlameOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DONNER UN RETOUR",
                            style = MaterialTheme.typography.titleMedium,
                            color = FlameOrange
                        )
                        // 5-Star Rating Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            for (star in 1..5) {
                                IconButton(
                                    onClick = { rating = star },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("feedback_star_$star")
                                ) {
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "$star étoiles",
                                        tint = if (star <= rating) UltiGold else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = testerName,
                            onValueChange = { testerName = it.take(36) },
                            label = { Text("Nom du testeur (IRL / En ligne)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("feedback_author_input")
                        )
                    }

                    // Mode & Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("IRL", "En ligne", "Vs Bot", "Entraînement").forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.24f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Graphismes", "Équilibrage", "Contrôles", "Bug", "Idée").forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FlameOrange.copy(alpha = 0.24f),
                                    selectedLabelColor = FlameOrange
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFighterId == "all",
                            onClick = { selectedFighterId = "all" },
                            label = { Text("Tout le jeu", style = MaterialTheme.typography.labelSmall) }
                        )
                        FighterRoster.fighters.forEach { f ->
                            FilterChip(
                                selected = selectedFighterId == f.id,
                                onClick = { selectedFighterId = f.id },
                                label = { Text(f.name, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = comment,
                        onValueChange = {
                            comment = it.take(380)
                            mainViewModel.clearFeedbackStatus()
                        },
                        label = { Text("Votre avis (équilibrage, persos, contrôles, bugs...)") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_comment_input")
                    )

                    if (feedbackStatus != null) {
                        Text(
                            text = feedbackStatus!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (feedbackStatus!!.startsWith("Merci")) EmeraldHp else CrimsonHp
                        )
                    }

                    Button(
                        onClick = {
                            mainViewModel.submitPlaytestFeedback(
                                authorName = testerName,
                                modeTested = selectedMode,
                                category = selectedCategory,
                                fighterId = selectedFighterId,
                                rating = rating,
                                comment = comment,
                                onSuccess = { comment = "" }
                            )
                        },
                        enabled = !feedbackBusy && comment.trim().length >= 3,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldHp, contentColor = ObsidianBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_feedback_button")
                    ) {
                        if (feedbackBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianBg)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ENVOYER LE RETOUR", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RETOURS RÉCENTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = UltiGold
                    )

                    when (val fbState = feedbacksState) {
                        is UiState.Loading -> {
                            CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(24.dp))
                        }
                        is UiState.Error -> {
                            Text(fbState.message, style = MaterialTheme.typography.bodyMedium, color = CrimsonHp)
                        }
                        is UiState.Success -> {
                            if (fbState.data.isEmpty()) {
                                Text(
                                    text = "Aucun retour pour l'instant. Soyez le premier testeur à laisser un avis !",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            } else {
                                fbState.data.forEach { fb ->
                                    FeedbackCard(
                                        feedback = fb,
                                        canDelete = fb.authorId == currentUserId || mainViewModel.isAdmin,
                                        onDelete = { mainViewModel.deletePlaytestFeedback(fb.id) }
                                    )
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
private fun FeedbackCard(
    feedback: PlaytestFeedback,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        color = ObsidianBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(feedback.authorName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text("• ${feedback.modeTested} • ${feedback.category}", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "★".repeat(feedback.rating.toInt().coerceIn(1, 5)),
                        style = MaterialTheme.typography.labelSmall,
                        color = UltiGold
                    )
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Text(
                text = feedback.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
