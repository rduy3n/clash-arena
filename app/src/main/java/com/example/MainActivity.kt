package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.GameRepository
import com.example.game.audio.AndroidSoundEffectsManager
import com.example.game.audio.LocalSoundPlayer
import com.example.game.audio.SoundPlayer
import com.example.game.data.BotDifficulty
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.BattleScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MatchSetupScreen
import com.example.ui.screens.OnlineLobbyScreen
import com.example.ui.screens.PlaytestShareScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RosterScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.theme.ClashArenaTheme
import com.example.ui.viewmodel.BattleViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClashArenaTheme {
                AppNavigation()
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val ROSTER = "roster"
    const val SETUP_BOT = "setup_bot"
    const val SETUP_TRAINING = "setup_training"
    const val ONLINE_LOBBY = "online_lobby"
    const val TOURNAMENT = "tournament"
    const val PLAYTEST_SHARE = "playtest_share"
    const val BATTLE_LOCAL = "battle_local/{mode}/{p1}/{p2}/{stage}/{diff}/{oppMode}"
    const val BATTLE_TOURNAMENT = "battle_tournament/{matchId}/{p1}/{p2}/{stage}/{diff}/{oppName}"
    const val BATTLE_ONLINE = "battle_online/{roomCode}"
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val soundManager = remember(context) { AndroidSoundEffectsManager(context) }
    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    CompositionLocalProvider(LocalSoundPlayer provides soundManager) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedApp(
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Combattant",
                    userEmail = user.email,
                    soundPlayer = soundManager
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedApp(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    soundPlayer: SoundPlayer
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val mainViewModel: MainViewModel = viewModel(
        key = "main_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                MainViewModel(GameRepository(db), currentUserId, userDisplayName, userEmail)
            }
        }
    )

    val battleViewModel: BattleViewModel = viewModel(
        key = "battle_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                BattleViewModel(GameRepository(db), currentUserId, soundPlayer)
            }
        }
    )

    val profileState by mainViewModel.profileState.collectAsStateWithLifecycle()
    val matchHistoryState by mainViewModel.matchHistoryState.collectAsStateWithLifecycle()
    val adminModifiers by mainViewModel.adminModifiers.collectAsStateWithLifecycle()
    val adminStatus by mainViewModel.adminStatus.collectAsStateWithLifecycle()

    LaunchedEffect(adminModifiers) {
        battleViewModel.syncGlobalAdminModifiers(adminModifiers)
    }

    val profile = (profileState as? UiState.Success)?.data
    val favFighterId = profile?.favoriteFighterId ?: "glitcherman"

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                profile = profile,
                fallbackName = userDisplayName,
                isAdmin = mainViewModel.isAdmin,
                verifiedEmail = mainViewModel.verifiedEmail,
                adminModifiers = adminModifiers,
                adminStatusMessage = adminStatus,
                onUpdateAdminModifiers = { mainViewModel.updateAdminModifiers(it) },
                onAdminBoostWins = { mainViewModel.adminBoostWins(5L) },
                onAdminResetStats = { mainViewModel.adminResetProfileStats() },
                onClearAdminStatus = { mainViewModel.clearAdminStatus() },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onPlayVsBot = { navController.navigate(Routes.SETUP_BOT) },
                onPlayTraining = { navController.navigate(Routes.SETUP_TRAINING) },
                onPlayOnline = { navController.navigate(Routes.ONLINE_LOBBY) },
                onPlayTournament = { navController.navigate(Routes.TOURNAMENT) },
                onOpenRoster = { navController.navigate(Routes.ROSTER) },
                onOpenPlaytestShare = { navController.navigate(Routes.PLAYTEST_SHARE) },
                onSignOut = { signOutUser(credentialManager, scope) }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                profile = profile,
                fallbackName = userDisplayName,
                matchHistoryState = matchHistoryState,
                onSetFavoriteFighter = { mainViewModel.setFavoriteFighter(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ROSTER) {
            RosterScreen(
                favoriteFighterId = favFighterId,
                onSetFavorite = { mainViewModel.setFavoriteFighter(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETUP_BOT) {
            MatchSetupScreen(
                mode = GameMode.VS_BOT,
                defaultFighterId = favFighterId,
                onStartMatch = { p1, p2, stage, diff, oppMode ->
                    navController.navigate("battle_local/${GameMode.VS_BOT.name}/$p1/$p2/$stage/${diff.name}/${oppMode.name}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETUP_TRAINING) {
            MatchSetupScreen(
                mode = GameMode.TRAINING,
                defaultFighterId = favFighterId,
                onStartMatch = { p1, p2, stage, diff, oppMode ->
                    navController.navigate("battle_local/${GameMode.TRAINING.name}/$p1/$p2/$stage/${diff.name}/${oppMode.name}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ONLINE_LOBBY) {
            OnlineLobbyScreen(
                mainViewModel = mainViewModel,
                currentUserId = currentUserId,
                favoriteFighterId = favFighterId,
                onEnterOnlineMatch = { roomCode ->
                    navController.navigate("battle_online/$roomCode") {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.TOURNAMENT) {
            TournamentScreen(
                mainViewModel = mainViewModel,
                favoriteFighterId = favFighterId,
                onLaunchHumanMatch = { matchId, p1, p2, stage, diff, oppName ->
                    val encodedOpp = Uri.encode(oppName)
                    navController.navigate("battle_tournament/$matchId/$p1/$p2/$stage/${diff.name}/$encodedOpp")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PLAYTEST_SHARE) {
            PlaytestShareScreen(
                mainViewModel = mainViewModel,
                currentUserId = currentUserId,
                defaultTesterName = profile?.displayName ?: userDisplayName,
                favoriteFighterId = favFighterId,
                onGoToOnlineLobby = { navController.navigate(Routes.ONLINE_LOBBY) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BATTLE_LOCAL) { backStackEntry ->
            val mode = GameMode.valueOf(backStackEntry.arguments?.getString("mode") ?: GameMode.VS_BOT.name)
            val p1 = backStackEntry.arguments?.getString("p1") ?: "glitcherman"
            val p2 = backStackEntry.arguments?.getString("p2") ?: "jardin"
            val stage = backStackEntry.arguments?.getString("stage") ?: "cyber_street"
            val diff = BotDifficulty.valueOf(backStackEntry.arguments?.getString("diff") ?: BotDifficulty.NORMAL.name)
            val oppMode = TrainingOpponentMode.valueOf(backStackEntry.arguments?.getString("oppMode") ?: TrainingOpponentMode.STATIONARY.name)

            LaunchedEffect(mode, p1, p2, stage, diff, oppMode) {
                battleViewModel.startLocalMatch(mode, p1, p2, stage, diff, oppMode)
            }

            BattleScreen(
                viewModel = battleViewModel,
                isAdmin = mainViewModel.isAdmin,
                verifiedEmail = mainViewModel.verifiedEmail,
                onRematchLocal = {
                    battleViewModel.startLocalMatch(mode, p1, p2, stage, diff, oppMode)
                },
                onExitBattle = {
                    battleViewModel.stopAll()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.BATTLE_TOURNAMENT) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId") ?: return@composable
            val p1 = backStackEntry.arguments?.getString("p1") ?: "glitcherman"
            val p2 = backStackEntry.arguments?.getString("p2") ?: "jardin"
            val stage = backStackEntry.arguments?.getString("stage") ?: "cyber_street"
            val diff = BotDifficulty.valueOf(backStackEntry.arguments?.getString("diff") ?: BotDifficulty.NORMAL.name)
            val oppName = Uri.decode(backStackEntry.arguments?.getString("oppName") ?: "Bot Tournoi")

            LaunchedEffect(matchId, p1, p2, stage, diff) {
                battleViewModel.startLocalMatch(
                    mode = GameMode.TOURNAMENT,
                    p1FighterId = p1,
                    p2FighterId = p2,
                    stageId = stage,
                    difficulty = diff,
                    tournamentMatchId = matchId,
                    opponentDisplayName = oppName
                )
            }

            BattleScreen(
                viewModel = battleViewModel,
                isAdmin = mainViewModel.isAdmin,
                verifiedEmail = mainViewModel.verifiedEmail,
                onRematchLocal = {},
                onTournamentContinue = { winnerSide, p1Score, p2Score ->
                    mainViewModel.recordTournamentBattleResult(
                        matchId = matchId,
                        winnerSide = winnerSide,
                        p1Score = p1Score,
                        p2Score = p2Score
                    )
                    battleViewModel.stopAll()
                    navController.popBackStack()
                },
                onExitBattle = {
                    battleViewModel.stopAll()
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.BATTLE_ONLINE) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: return@composable
            val onlineReturnedToLobby by battleViewModel.onlineReturnedToLobby.collectAsStateWithLifecycle()

            LaunchedEffect(roomCode) {
                mainViewModel.setActiveOnlineRoomCode(roomCode)
                battleViewModel.startOnlineMatch(roomCode)
            }

            LaunchedEffect(onlineReturnedToLobby) {
                if (onlineReturnedToLobby) {
                    mainViewModel.setActiveOnlineRoomCode(roomCode)
                    battleViewModel.stopAll()
                    navController.popBackStack()
                }
            }

            BattleScreen(
                viewModel = battleViewModel,
                isAdmin = mainViewModel.isAdmin,
                verifiedEmail = mainViewModel.verifiedEmail,
                onRematchLocal = {},
                onOnlineRematchToSelect = {
                    battleViewModel.requestOnlineRematch()
                },
                onExitBattle = {
                    battleViewModel.leaveOnlineRoom {
                        mainViewModel.setActiveOnlineRoomCode(null)
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                }
            )
        }
    }
}
