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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.firebase.GameRepository
import com.example.data.firebase.MatchRoom
import com.example.game.data.FighterRoster
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
import kotlinx.coroutines.launch

@Composable
fun OnlineLobbyScreen(
    mainViewModel: MainViewModel,
    currentUserId: String,
    favoriteFighterId: String,
    onEnterOnlineMatch: (roomCode: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { GameRepository(context) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val activeRoomCode by mainViewModel.activeOnlineRoomCode.collectAsStateWithLifecycle()
    val lobbyBusy by mainViewModel.lobbyBusy.collectAsStateWithLifecycle()
    val lobbyMessage by mainViewModel.lobbyMessage.collectAsStateWithLifecycle()

    var joinCodeInput by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf(false) }

    val activeRoom by remember(activeRoomCode) {
        val code = activeRoomCode
        if (code == null) kotlinx.coroutines.flow.flowOf(null)
        else repository.observeMatchRoom(code)
    }.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(activeRoom?.status, activeRoomCode) {
        val code = activeRoomCode ?: return@LaunchedEffect
        if (activeRoom?.status == "fighting") {
            onEnterOnlineMatch(code)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 780.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val code = activeRoomCode
                        if (code != null) {
                            val isHost = activeRoom?.hostId == currentUserId
                            scope.launch {
                                repository.leaveMatchRoom(code, isHost = isHost)
                                mainViewModel.setActiveOnlineRoomCode(null)
                            }
                        } else {
                            onBack()
                        }
                    },
                    modifier = Modifier.testTag("online_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = if (activeRoomCode == null) "DUEL EN LIGNE" else "SALON $activeRoomCode",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (activeRoomCode == null) {
                            "Créez un salon ou rejoignez un ami par code à 6 caractères"
                        } else {
                            "Sélection de personnage • La revanche vous ramène ici sans recréer de salon"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan
                    )
                }
            }

            if (lobbyMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = lobbyMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (activeRoomCode == null) {
                // Create room card
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "CRÉER UN SALON PRIVÉ",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Générez un code unique à 6 caractères et partagez-le à votre ami.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Button(
                            onClick = {
                                mainViewModel.createOnlineRoom(
                                    fighterId = favoriteFighterId,
                                    stageId = FighterRoster.stages.first().id,
                                    onRoomReady = { code -> mainViewModel.setActiveOnlineRoomCode(code) }
                                )
                            },
                            enabled = !lobbyBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_room_button")
                        ) {
                            if (lobbyBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CRÉER UN SALON", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Join room card (no room history!)
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, FlameOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "REJOINDRE UN AMI",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { joinCodeInput = it.uppercase().take(6) },
                            label = { Text("Code du salon (ex: K9X4M2)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_room_code_input")
                        )
                        Button(
                            onClick = {
                                mainViewModel.joinOnlineRoom(
                                    code = joinCodeInput,
                                    onRoomReady = { code -> mainViewModel.setActiveOnlineRoomCode(code) }
                                )
                            },
                            enabled = !lobbyBusy && joinCodeInput.trim().length == 6,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlameOrange,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("join_room_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REJOINDRE LE SALON", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                // Active Room Lobby View
                val room = activeRoom
                if (room == null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan)
                    }
                } else {
                    ActiveRoomLobbyContent(
                        room = room,
                        currentUserId = currentUserId,
                        copiedNotice = copiedNotice,
                        onCopyCode = {
                            clipboard.setText(AnnotatedString(room.roomCode))
                            copiedNotice = true
                        },
                        onSelectFighter = { fighterId ->
                            val isHost = room.hostId == currentUserId
                            scope.launch {
                                repository.updateLobbySelection(
                                    roomCode = room.roomCode,
                                    isHost = isHost,
                                    fighterId = fighterId,
                                    ready = false // Reset ready so partner sees updated pick cleanly
                                )
                            }
                        },
                        onSelectStage = { stageId ->
                            if (room.hostId == currentUserId) {
                                scope.launch {
                                    repository.updateLobbySelection(
                                        roomCode = room.roomCode,
                                        isHost = true,
                                        fighterId = room.hostFighterId,
                                        ready = room.hostReady,
                                        stageId = stageId
                                    )
                                }
                            }
                        },
                        onToggleReady = {
                            val isHost = room.hostId == currentUserId
                            val currentReady = if (isHost) room.hostReady else room.guestReady
                            val fighterId = if (isHost) room.hostFighterId else room.guestFighterId
                            scope.launch {
                                repository.updateLobbySelection(
                                    roomCode = room.roomCode,
                                    isHost = isHost,
                                    fighterId = fighterId,
                                    ready = !currentReady
                                )
                            }
                        },
                        onStartMatch = {
                            scope.launch {
                                repository.updateLobbySelection(
                                    roomCode = room.roomCode,
                                    isHost = true,
                                    fighterId = room.hostFighterId,
                                    ready = true,
                                    stageId = room.stageId,
                                    startFight = true
                                )
                            }
                        },
                        onLeaveOrCloseRoom = {
                            val isHost = room.hostId == currentUserId
                            scope.launch {
                                repository.leaveMatchRoom(room.roomCode, isHost = isHost)
                                mainViewModel.setActiveOnlineRoomCode(null)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveRoomLobbyContent(
    room: MatchRoom,
    currentUserId: String,
    copiedNotice: Boolean,
    onCopyCode: () -> Unit,
    onSelectFighter: (String) -> Unit,
    onSelectStage: (String) -> Unit,
    onToggleReady: () -> Unit,
    onStartMatch: () -> Unit,
    onLeaveOrCloseRoom: () -> Unit
) {
    val isHost = room.hostId == currentUserId
    val myFighterId = if (isHost) room.hostFighterId else room.guestFighterId
    val myReady = if (isHost) room.hostReady else room.guestReady
    val bothJoined = room.guestId.isNotBlank()
    val bothReady = room.hostReady && room.guestReady && bothJoined
    val currentStage = FighterRoster.getStage(room.stageId)

    val context = LocalContext.current

    // Room Code Card
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, ElectricCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CODE DU SALON", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                Text(room.roomCode, style = MaterialTheme.typography.displaySmall, color = Color.White)
                if (copiedNotice) {
                    Text("Code copié !", style = MaterialTheme.typography.labelSmall, color = EmeraldHp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCopyCode,
                    modifier = Modifier.testTag("copy_room_code_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copier", tint = ElectricCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPIER", color = ElectricCyan)
                }
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Rejoins mon combat sur Clash Arena ! Code du salon : ${room.roomCode}"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Partager le code du salon"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlameOrange, contentColor = Color.White),
                    modifier = Modifier.testTag("share_room_code_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Partager", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PARTAGER")
                }
            }
        }
    }

    // Players Status Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LobbyPlayerSlot(
            slotLabel = "J1 (HÔTE)",
            playerName = room.hostName,
            fighterId = room.hostFighterId,
            ready = room.hostReady,
            occupied = true,
            modifier = Modifier.weight(1f)
        )
        LobbyPlayerSlot(
            slotLabel = "J2 (AMI)",
            playerName = room.guestName.ifBlank { "En attente d'un ami..." },
            fighterId = room.guestFighterId,
            ready = room.guestReady,
            occupied = bothJoined,
            modifier = Modifier.weight(1f)
        )
    }

    // Host Stage Selector / Guest Stage Display
    Text(
        text = if (isHost) "CHOIX DE L'ARÈNE (HÔTE)" else "ARÈNE : ${currentStage.name.uppercase()}",
        style = MaterialTheme.typography.labelLarge,
        color = UltiGold
    )
    if (isHost) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FighterRoster.stages.forEach { stage ->
                val selected = stage.id == room.stageId
                FilterChip(
                    selected = selected,
                    onClick = { onSelectStage(stage.id) },
                    label = { Text(stage.name, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = stage.accentGlow.copy(alpha = 0.24f),
                        selectedLabelColor = stage.accentGlow
                    )
                )
            }
        }
    }

    // Fighter selector for local player
    Text(
        text = "CHOISISSEZ VOTRE COMBATTANT",
        style = MaterialTheme.typography.labelLarge,
        color = Color.White
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FighterRoster.fighters.forEach { fighter ->
            val selected = fighter.id == myFighterId
            Surface(
                onClick = { onSelectFighter(fighter.id) },
                color = if (selected) fighter.primaryColor.copy(alpha = 0.24f) else ObsidianSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) fighter.primaryColor else ObsidianOutline),
                modifier = Modifier.testTag("lobby_pick_${fighter.id}")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FighterAvatar(fighter = fighter, size = 48.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(fighter.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }

    // Ready button
    Button(
        onClick = onToggleReady,
        enabled = bothJoined,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (myReady) EmeraldHp else FlameOrange,
            contentColor = if (myReady) ObsidianBg else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("lobby_ready_button")
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (myReady) "PRÊT (ANNULER)" else "JE SUIS PRÊT !",
            style = MaterialTheme.typography.labelLarge
        )
    }

    if (isHost) {
        Button(
            onClick = onStartMatch,
            enabled = bothReady,
            colors = ButtonDefaults.buttonColors(
                containerColor = UltiGold,
                contentColor = ObsidianBg
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("lobby_start_fight_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (bothReady) "LANCER LE COMBAT !" else "ATTENTE DES JOUEURS PRÊTS...",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    OutlinedButton(
        onClick = onLeaveOrCloseRoom,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("lobby_leave_room_button")
    ) {
        Icon(
            imageVector = if (isHost) Icons.Default.DeleteOutline else Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isHost) "FERMER LE SALON" else "QUITTER LE SALON",
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun LobbyPlayerSlot(
    slotLabel: String,
    playerName: String,
    fighterId: String,
    ready: Boolean,
    occupied: Boolean,
    modifier: Modifier = Modifier
) {
    val fighter = FighterRoster.getFighter(fighterId)
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, if (ready) EmeraldHp else ObsidianOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(slotLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            if (occupied) {
                FighterAvatar(fighter = fighter, size = 50.dp)
                Text(playerName, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                Text(
                    text = if (ready) "PRÊT (${fighter.name})" else "Choix : ${fighter.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (ready) EmeraldHp else UltiGold
                )
            } else {
                Spacer(modifier = Modifier.height(14.dp))
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = ElectricCyan, strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(playerName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}
