package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GameRepository
import com.example.data.firebase.MatchRoom
import com.example.game.audio.SoundEffect
import com.example.game.audio.SoundPlayer
import com.example.game.data.AdminCombatModifiers
import com.example.game.data.BotDifficulty
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.CombatAction
import com.example.game.engine.CombatEngine
import com.example.game.engine.FinishType
import com.example.game.engine.FloatingPopup
import com.example.game.engine.HitSpark
import com.example.game.engine.MatchPhase
import com.example.game.engine.PlayerInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class BattleViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val soundPlayer: SoundPlayer? = null
) : ViewModel() {

    private val engine = CombatEngine()
    private var remoteEffectSeq = 900_000L

    private val _world = MutableStateFlow<ArenaWorldState?>(null)
    val world: StateFlow<ArenaWorldState?> = _world.asStateFlow()

    private val _timeMs = MutableStateFlow(0L)
    val timeMs: StateFlow<Long> = _timeMs.asStateFlow()

    private val _onlineRoom = MutableStateFlow<MatchRoom?>(null)
    val onlineRoom: StateFlow<MatchRoom?> = _onlineRoom.asStateFlow()

    private val _localSide = MutableStateFlow(1)
    val localSide: StateFlow<Int> = _localSide.asStateFlow()

    private val _onlineError = MutableStateFlow<String?>(null)
    val onlineError: StateFlow<String?> = _onlineError.asStateFlow()

    private val _onlineReturnedToLobby = MutableStateFlow(false)
    val onlineReturnedToLobby: StateFlow<Boolean> = _onlineReturnedToLobby.asStateFlow()

    private val _adminModifiers = MutableStateFlow(AdminCombatModifiers())
    val adminModifiers: StateFlow<AdminCombatModifiers> = _adminModifiers.asStateFlow()

    private val _tournamentCompletedMatchId = MutableStateFlow<String?>(null)
    val tournamentCompletedMatchId: StateFlow<String?> = _tournamentCompletedMatchId.asStateFlow()

    private var moveLeftHeld = false
    private var moveRightHeld = false
    private var blockHeld = false
    private var jumpQueued = false
    private var classicQueued = false
    private var baseQueued = false
    private var ultiQueued = false

    private var botDifficulty: BotDifficulty = BotDifficulty.NORMAL
    private var loopJob: Job? = null
    private var roomObserveJob: Job? = null
    private var onlineSyncJob: Job? = null
    private var activeRoomCode: String? = null
    private var activeTournamentMatchId: String? = null
    private var currentOpponentName: String = "Bot"
    private var lastSyncedRound = 1L
    private var lastSyncedActionSeq = -1L
    private var resultRecorded = false
    private var roundTransitionSent = false

    fun syncGlobalAdminModifiers(modifiers: AdminCombatModifiers) {
        _adminModifiers.value = modifiers
    }

    fun startLocalMatch(
        mode: GameMode,
        p1FighterId: String,
        p2FighterId: String,
        stageId: String,
        difficulty: BotDifficulty = BotDifficulty.NORMAL,
        trainingOpponentMode: TrainingOpponentMode = TrainingOpponentMode.STATIONARY,
        tournamentMatchId: String? = null,
        opponentDisplayName: String? = null
    ) {
        stopAll()
        botDifficulty = difficulty
        activeTournamentMatchId = tournamentMatchId
        _tournamentCompletedMatchId.value = null
        resultRecorded = false
        _localSide.value = 1
        currentOpponentName = opponentDisplayName?.takeIf { it.isNotBlank() } ?: when (mode) {
            GameMode.VS_BOT -> "Bot (${difficulty.label})"
            GameMode.TOURNAMENT -> "Adversaire Tournoi"
            else -> "Mannequin"
        }

        val initial = engine.createInitialState(
            mode = mode,
            stage = FighterRoster.getStage(stageId),
            p1Def = FighterRoster.getFighter(p1FighterId),
            p2Def = FighterRoster.getFighter(p2FighterId),
            botDifficulty = difficulty,
            trainingOpponentMode = trainingOpponentMode
        )
        _world.value = initial

        loopJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(16L)
                val now = System.currentTimeMillis()
                val dt = (now - lastTick).coerceIn(8L, 40L)
                lastTick = now
                _timeMs.value = now

                val input = consumeInput()
                val current = _world.value ?: continue
                val updated = engine.step(
                    state = current,
                    dtMs = dt,
                    p1Input = input,
                    botDifficulty = botDifficulty,
                    adminModifiers = _adminModifiers.value
                )
                _world.value = updated
                playEngineSounds(updated.soundEvents)

                if ((current.mode == GameMode.VS_BOT || current.mode == GameMode.TOURNAMENT) &&
                    updated.phase == MatchPhase.MATCH_OVER &&
                    !resultRecorded
                ) {
                    resultRecorded = true
                    if (current.mode == GameMode.TOURNAMENT) {
                        _tournamentCompletedMatchId.value = activeTournamentMatchId
                    }
                    val modeTag = if (current.mode == GameMode.TOURNAMENT) "TOURNAMENT" else "VS_BOT"
                    launch {
                        repository.recordMatchResult(
                            won = updated.winnerSide == 1,
                            mode = modeTag,
                            myFighterId = updated.p1.def.id,
                            opponentFighterId = updated.p2.def.id,
                            opponentName = currentOpponentName,
                            stageId = updated.stage.id,
                            myRoundsWon = updated.p1RoundsWon,
                            opponentRoundsWon = updated.p2RoundsWon,
                            damageDealt = updated.p1.matchDamageTotal.coerceAtLeast(updated.p1.dealtDamageTotal)
                        )
                    }
                }
            }
        }
    }

    fun skipIntro() {
        val current = _world.value ?: return
        if (current.mode == GameMode.ONLINE) return
        _world.value = engine.skipIntro(current)
    }

    fun startOnlineMatch(roomCode: String) {
        stopAll()
        activeRoomCode = roomCode
        _onlineError.value = null
        _onlineReturnedToLobby.value = false
        resultRecorded = false
        roundTransitionSent = false
        lastSyncedActionSeq = -1L

        roomObserveJob = viewModelScope.launch {
            repository.observeMatchRoom(roomCode)
                .catch { error ->
                    if (error is CancellationException && error.cause == null) throw error
                    Log.w("BattleVM", "Online room observe error", error)
                    _onlineError.value = error.message ?: "Connexion perdue."
                }
                .collect { room ->
                    if (room == null) {
                        _onlineError.value = "Le salon a été fermé par l'hôte."
                        return@collect
                    }
                    _onlineRoom.value = room
                    val side = if (room.hostId == currentUserId) 1 else 2
                    _localSide.value = side

                    // Retour vers la sélection de personnage demandé (revanche)
                    if (room.status == "selecting" || room.status == "waiting") {
                        _onlineReturnedToLobby.value = true
                        return@collect
                    }

                    if (_world.value == null && room.status == "fighting") {
                        val initWorld = engine.createInitialState(
                            mode = GameMode.ONLINE,
                            stage = FighterRoster.getStage(room.stageId),
                            p1Def = FighterRoster.getFighter(room.hostFighterId),
                            p2Def = FighterRoster.getFighter(room.guestFighterId)
                        ).copy(
                            phase = MatchPhase.INTRO_CINEMATIC,
                            phaseTimerMs = 2500L,
                            introDurationMs = 2500L,
                            bannerText = ""
                        )
                        _world.value = initWorld
                        lastSyncedRound = room.roundNumber
                        startOnlineLoops(roomCode, side)
                    } else if (_world.value != null) {
                        applyRemoteRoomState(room, side)
                    }
                }
        }
    }

    private fun startOnlineLoops(roomCode: String, side: Int) {
        loopJob?.cancel()
        onlineSyncJob?.cancel()

        loopJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive) {
                delay(16L)
                val now = System.currentTimeMillis()
                val dt = (now - lastTick).coerceIn(8L, 40L)
                lastTick = now
                _timeMs.value = now

                val current = _world.value ?: continue

                when (current.phase) {
                    MatchPhase.INTRO_CINEMATIC -> {
                        val rem = current.phaseTimerMs - dt
                        _world.value = if (rem > 0L) {
                            current.copy(phaseTimerMs = rem)
                        } else {
                            current.copy(
                                phase = MatchPhase.ROUND_INTRO,
                                phaseTimerMs = 950L,
                                bannerText = "ROUND ${current.roundNumber}"
                            )
                        }
                        continue
                    }

                    MatchPhase.ROUND_INTRO -> {
                        val rem = current.phaseTimerMs - dt
                        _world.value = when {
                            rem > 420L -> current.copy(phaseTimerMs = rem, bannerText = "ROUND ${current.roundNumber}")
                            rem > 0L -> current.copy(phaseTimerMs = rem, bannerText = "COMBAT !")
                            else -> {
                                soundPlayer?.play(SoundEffect.ROUND_START)
                                current.copy(
                                    phase = MatchPhase.FIGHTING,
                                    phaseTimerMs = 0L,
                                    bannerText = ""
                                )
                            }
                        }
                        continue
                    }

                    MatchPhase.KO_SLOWMO -> {
                        if (current.koFreezeRemainingMs > 0L) {
                            _world.value = current.copy(
                                koFreezeRemainingMs = (current.koFreezeRemainingMs - dt).coerceAtLeast(0L),
                                impactFlashMs = (current.impactFlashMs - (dt / 2L)).coerceAtLeast(0L)
                            )
                            continue
                        }
                        val rem = current.phaseTimerMs - dt
                        val stepped = engine.stepSlowMoVisuals(current, dt)
                        _world.value = if (rem > 0L) {
                            stepped.copy(phaseTimerMs = rem)
                        } else {
                            soundPlayer?.play(SoundEffect.VICTORY_FANFARE)
                            stepped.copy(
                                phase = MatchPhase.MATCH_OVER,
                                phaseTimerMs = 0L,
                                koFreezeRemainingMs = 0L,
                                screenShakeMs = 0L,
                                impactFlashMs = 0L,
                                hitStopMs = 0L,
                                fatalParticles = emptyList()
                            )
                        }
                        continue
                    }

                    MatchPhase.FIGHTING -> Unit
                    else -> continue
                }

                val input = consumeInput()
                var updated = engine.stepOnlineLocal(current, dt, side, input)

                // Countdown round timer on Host
                val nextClockAccum = updated.roundClockAccumMs + dt
                var nextSec = updated.roundTimeRemainingSec
                var clockRem = nextClockAccum
                if (clockRem >= 1000L) {
                    clockRem -= 1000L
                    nextSec = (nextSec - 1).coerceAtLeast(0)
                }
                updated = updated.copy(roundTimeRemainingSec = nextSec, roundClockAccumMs = clockRem)

                _world.value = updated
                playEngineSounds(updated.soundEvents)

                // Host checks round / match victory deterministically
                val koTriggered = updated.p1.hp <= 0 || updated.p2.hp <= 0 || (side == 1 && nextSec <= 0)
                if (side == 1 && !roundTransitionSent && koTriggered) {
                    roundTransitionSent = true
                    val roundWinner = when {
                        updated.p2.hp <= 0 && updated.p1.hp > 0 -> 1
                        updated.p1.hp <= 0 && updated.p2.hp > 0 -> 2
                        updated.p1.hp.toFloat() / updated.p1.def.stats.maxHp >=
                            updated.p2.hp.toFloat() / updated.p2.def.stats.maxHp -> 1
                        else -> 2
                    }
                    val nextP1Wins = updated.p1RoundsWon + if (roundWinner == 1) 1 else 0
                    val nextP2Wins = updated.p2RoundsWon + if (roundWinner == 2) 1 else 0
                    if (nextP1Wins >= 2 || nextP2Wins >= 2) {
                        val winnerSideStr = if (nextP1Wins >= 2) "p1" else "p2"
                        launch {
                            repository.finishMatch(
                                roomCode = roomCode,
                                winnerSide = winnerSideStr,
                                p1RoundsWon = nextP1Wins.toLong(),
                                p2RoundsWon = nextP2Wins.toLong()
                            )
                        }
                    } else {
                        launch {
                            delay(1000L)
                            repository.advanceRound(
                                roomCode = roomCode,
                                nextRound = (updated.roundNumber + 1).toLong(),
                                p1RoundsWon = nextP1Wins.toLong(),
                                p2RoundsWon = nextP2Wins.toLong()
                            )
                        }
                    }
                }
            }
        }

        // Sequential conflated Firestore writer: prevents out-of-order stale updates!
        onlineSyncJob = viewModelScope.launch {
            var lastWriteMs = 0L
            while (isActive) {
                delay(40L)
                val current = _world.value ?: continue
                if (current.phase != MatchPhase.FIGHTING || roundTransitionSent) continue
                val localFighter = if (side == 1) current.p1 else current.p2
                val now = System.currentTimeMillis()
                val actionChanged = localFighter.actionSeq != lastSyncedActionSeq
                val periodicDue = (now - lastWriteMs) >= 85L

                if (actionChanged || periodicDue) {
                    lastSyncedActionSeq = localFighter.actionSeq
                    lastWriteMs = now
                    repository.updateCombatState(
                        roomCode = roomCode,
                        isHost = (side == 1),
                        x = localFighter.x.toDouble(),
                        y = localFighter.y.toDouble(),
                        facing = localFighter.facing,
                        action = localFighter.action.wireName,
                        actionSeq = localFighter.actionSeq,
                        blocking = localFighter.isBlocking,
                        ulti = localFighter.ultiMeter,
                        dealtDmg = localFighter.dealtDamageTotal
                    )
                }
            }
        }
    }

    private fun applyRemoteRoomState(room: MatchRoom, localSide: Int) {
        val current = _world.value ?: return

        // Round change reset
        if (room.roundNumber != lastSyncedRound) {
            lastSyncedRound = room.roundNumber
            roundTransitionSent = false
            lastSyncedActionSeq = -1L
            _world.value = current.copy(
                phase = MatchPhase.ROUND_INTRO,
                phaseTimerMs = 950L,
                roundNumber = room.roundNumber.toInt(),
                roundTimeRemainingSec = 99,
                roundClockAccumMs = 0L,
                p1RoundsWon = room.p1RoundsWon.toInt(),
                p2RoundsWon = room.p2RoundsWon.toInt(),
                bannerText = "ROUND ${room.roundNumber}",
                projectiles = emptyList(),
                slashEffects = emptyList(),
                sparks = emptyList(),
                p1 = current.p1.copy(
                    x = 0.25f, y = 0f, vy = 0f, facing = 1,
                    hp = current.p1.def.stats.maxHp,
                    ultiMeter = 0, dealtDamageTotal = 0,
                    action = CombatAction.IDLE, actionSeq = 0L,
                    hitStunMs = 0L, vineSnaredMs = 0L,
                    classicCooldownMs = 0L, baseCooldownMs = 0L,
                    comboCount = 0, comboDamage = 0, isBlocking = false
                ),
                p2 = current.p2.copy(
                    x = 0.75f, y = 0f, vy = 0f, facing = -1,
                    hp = current.p2.def.stats.maxHp,
                    ultiMeter = 0, dealtDamageTotal = 0,
                    action = CombatAction.IDLE, actionSeq = 0L,
                    hitStunMs = 0L, vineSnaredMs = 0L,
                    classicCooldownMs = 0L, baseCooldownMs = 0L,
                    comboCount = 0, comboDamage = 0, isBlocking = false
                )
            )
            return
        }

        if (room.status == "finished") {
            val winnerSideInt = when (room.winnerSide) {
                "p1" -> 1
                "p2" -> 2
                else -> 0
            }
            if (!resultRecorded && winnerSideInt != 0) {
                resultRecorded = true
                soundPlayer?.play(SoundEffect.KO_BOOM)
                val myFighter = if (localSide == 1) current.p1 else current.p2
                val oppFighter = if (localSide == 1) current.p2 else current.p1
                val oppName = if (localSide == 1) room.guestName else room.hostName
                val myRounds = if (localSide == 1) room.p1RoundsWon.toInt() else room.p2RoundsWon.toInt()
                val oppRounds = if (localSide == 1) room.p2RoundsWon.toInt() else room.p1RoundsWon.toInt()
                viewModelScope.launch {
                    repository.recordMatchResult(
                        won = winnerSideInt == localSide,
                        mode = "ONLINE",
                        myFighterId = myFighter.def.id,
                        opponentFighterId = oppFighter.def.id,
                        opponentName = oppName.ifBlank { "Joueur En Ligne" },
                        stageId = current.stage.id,
                        myRoundsWon = myRounds,
                        opponentRoundsWon = oppRounds,
                        damageDealt = myFighter.matchDamageTotal.coerceAtLeast(myFighter.dealtDamageTotal)
                    )
                }
            }
            if (current.phase != MatchPhase.KO_SLOWMO && current.phase != MatchPhase.MATCH_OVER) {
                val winnerRuntime = if (winnerSideInt == 1) current.p1 else current.p2
                val loserRuntime = if (winnerSideInt == 1) current.p2 else current.p1
                val resolvedFinishType = when {
                    winnerSideInt != 0 && winnerRuntime.hp >= winnerRuntime.def.stats.maxHp -> FinishType.PERFECT
                    winnerSideInt != 0 && winnerRuntime.action == CombatAction.ULTI -> FinishType.ULTI_FINISH
                    else -> FinishType.KO
                }
                val koDuration = 1200L
                val freezeDuration = if (winnerSideInt != 0) 400L else 0L
                val fatalBurst = if (winnerSideInt != 0) {
                    engine.generateFatalParticles(
                        impactX = loserRuntime.x,
                        impactY = loserRuntime.y + 0.14f,
                        primary = winnerRuntime.def.primaryColor,
                        accent = winnerRuntime.def.accentColor
                    )
                } else {
                    emptyList()
                }
                _world.value = current.copy(
                    phase = if (winnerSideInt != 0) MatchPhase.KO_SLOWMO else MatchPhase.MATCH_OVER,
                    phaseTimerMs = if (winnerSideInt != 0) koDuration else 0L,
                    koFreezeRemainingMs = freezeDuration,
                    koFreezeTotalMs = freezeDuration.coerceAtLeast(1L),
                    koSlowMoTotalMs = koDuration,
                    p1RoundsWon = room.p1RoundsWon.toInt(),
                    p2RoundsWon = room.p2RoundsWon.toInt(),
                    winnerSide = winnerSideInt,
                    finishType = resolvedFinishType,
                    bannerText = resolvedFinishType.label,
                    screenShakeMs = if (winnerSideInt != 0) 480L else 0L,
                    impactFlashMs = if (winnerSideInt != 0) 240L else 0L,
                    impactX = loserRuntime.x,
                    impactCallout = resolvedFinishType.label,
                    fatalParticles = fatalBurst,
                    p1 = if (winnerSideInt == 2) current.p1.copy(hp = 0, action = CombatAction.KO) else current.p1,
                    p2 = if (winnerSideInt == 1) current.p2.copy(hp = 0, action = CombatAction.KO) else current.p2
                )
            }
            return
        }

        val p1Hp = (current.p1.def.stats.maxHp - room.p2DealtDmg.toInt()).coerceAtLeast(0)
        val p2Hp = (current.p2.def.stats.maxHp - room.p1DealtDmg.toInt()).coerceAtLeast(0)

        val extraPopups = mutableListOf<FloatingPopup>()
        val extraSparks = mutableListOf<HitSpark>()

        val remoteDmgToLocal = if (localSide == 1) (current.p1.hp - p1Hp) else (current.p2.hp - p2Hp)
        if (remoteDmgToLocal > 0) {
            val victim = if (localSide == 1) current.p1 else current.p2
            val isUlti = remoteDmgToLocal >= 200
            val style = CombatEngine.classifyDamageVisual(
                damage = remoteDmgToLocal,
                blocked = victim.isBlocking,
                isUlti = isUlti
            )
            val id = remoteEffectSeq++
            val spread = ((id % 3L) - 1L).toFloat()
            extraPopups += FloatingPopup(
                id = id,
                x = (victim.x + spread * 0.024f).coerceIn(0.08f, 0.92f),
                y = victim.y + 0.16f * victim.def.stats.heightScale + 0.03f,
                text = "-$remoteDmgToLocal",
                color = style.color,
                isCritical = style.isCritical,
                vx = spread * 0.045f,
                vy = if (style.isCritical) 0.28f else 0.22f,
                scaleMultiplier = style.scale,
                subLabel = style.subLabel,
                maxAgeMs = if (isUlti) 1100L else 860L
            )
            extraSparks += HitSpark(
                id = remoteEffectSeq++,
                x = victim.x,
                y = victim.y + 0.12f,
                color = style.color,
                isUlti = isUlti
            )
            val sfx = when {
                victim.isBlocking && !isUlti -> SoundEffect.BLOCK_CLANG
                isUlti -> SoundEffect.ULTI_IMPACT
                remoteDmgToLocal >= 120 -> SoundEffect.HEAVY_HIT
                else -> SoundEffect.CLASSIC_HIT
            }
            soundPlayer?.play(sfx)
        }

        val updatedP1 = if (localSide == 1) {
            current.p1.copy(hp = p1Hp)
        } else {
            val remoteAction = CombatAction.fromWire(room.p1Action)
            if (room.p1ActionSeq != current.p1.actionSeq && remoteAction == CombatAction.ULTI) {
                soundPlayer?.play(SoundEffect.ULTI_CINEMATIC)
            }
            current.p1.copy(
                x = room.p1X.toFloat(),
                y = room.p1Y.toFloat(),
                facing = room.p1Facing.toInt(),
                action = remoteAction,
                actionSeq = room.p1ActionSeq,
                isBlocking = room.p1Blocking,
                ultiMeter = room.p1Ulti.toInt(),
                hp = p1Hp
            )
        }

        val updatedP2 = if (localSide == 2) {
            current.p2.copy(hp = p2Hp)
        } else {
            val remoteAction = CombatAction.fromWire(room.p2Action)
            if (room.p2ActionSeq != current.p2.actionSeq && remoteAction == CombatAction.ULTI) {
                soundPlayer?.play(SoundEffect.ULTI_CINEMATIC)
            }
            current.p2.copy(
                x = room.p2X.toFloat(),
                y = room.p2Y.toFloat(),
                facing = room.p2Facing.toInt(),
                action = remoteAction,
                actionSeq = room.p2ActionSeq,
                isBlocking = room.p2Blocking,
                ultiMeter = room.p2Ulti.toInt(),
                hp = p2Hp
            )
        }

        _world.value = current.copy(
            p1 = updatedP1,
            p2 = updatedP2,
            p1RoundsWon = room.p1RoundsWon.toInt(),
            p2RoundsWon = room.p2RoundsWon.toInt(),
            roundNumber = room.roundNumber.toInt(),
            popups = current.popups + extraPopups,
            sparks = current.sparks + extraSparks
        )
    }

    fun requestOnlineRematch() {
        val code = activeRoomCode ?: return
        viewModelScope.launch {
            repository.requestRematchToCharacterSelect(code)
                .onFailure { _onlineError.value = it.message ?: "Impossible de relancer la revanche." }
        }
    }

    fun leaveOnlineRoom(onComplete: () -> Unit) {
        val code = activeRoomCode
        val isHost = _localSide.value == 1
        viewModelScope.launch {
            if (code != null) {
                repository.leaveMatchRoom(code, isHost = isHost)
            }
            stopAll()
            onComplete()
        }
    }

    // --- Live In-Battle Admin Commands ---
    fun adminHealFighter(side: Int) {
        val current = _world.value ?: return
        _world.value = if (side == 1) {
            current.copy(p1 = current.p1.copy(hp = current.p1.def.stats.maxHp))
        } else {
            current.copy(p2 = current.p2.copy(hp = current.p2.def.stats.maxHp))
        }
    }

    fun adminFullUlti(side: Int) {
        val current = _world.value ?: return
        _world.value = if (side == 1) {
            current.copy(p1 = current.p1.copy(ultiMeter = 100))
        } else {
            current.copy(p2 = current.p2.copy(ultiMeter = 100))
        }
    }

    fun adminInstantKoP2() {
        val current = _world.value ?: return
        if (current.mode == GameMode.ONLINE || current.mode == GameMode.TRAINING) return
        _world.value = current.copy(
            phase = MatchPhase.FIGHTING,
            p2 = current.p2.copy(hp = 0, action = CombatAction.KO)
        )
    }

    fun adminWinMatchP1() {
        val current = _world.value ?: return
        if (current.mode == GameMode.ONLINE || current.mode == GameMode.TRAINING) return
        soundPlayer?.play(SoundEffect.KO_BOOM)
        val fatalBurst = engine.generateFatalParticles(
            impactX = current.p2.x,
            impactY = current.p2.y + 0.14f,
            primary = current.p1.def.primaryColor,
            accent = current.p1.def.accentColor
        )
        _world.value = current.copy(
            phase = MatchPhase.KO_SLOWMO,
            phaseTimerMs = 1100L,
            koFreezeRemainingMs = 360L,
            koFreezeTotalMs = 360L,
            koSlowMoTotalMs = 1100L,
            p1RoundsWon = 2,
            winnerSide = 1,
            finishType = FinishType.KO,
            bannerText = FinishType.KO.label,
            screenShakeMs = 460L,
            impactFlashMs = 240L,
            impactX = current.p2.x,
            impactCallout = FinishType.KO.label,
            fatalParticles = fatalBurst,
            p2 = current.p2.copy(hp = 0, action = CombatAction.KO)
        )
    }

    fun adminResetTimer() {
        val current = _world.value ?: return
        _world.value = current.copy(roundTimeRemainingSec = 99, roundClockAccumMs = 0L)
    }

    fun adminSetModifiers(modifiers: AdminCombatModifiers) {
        _adminModifiers.value = modifiers
    }

    private fun playEngineSounds(events: List<SoundEffect>) {
        if (events.isEmpty() || soundPlayer == null) return
        events.forEach { sfx ->
            val pitch = when (sfx) {
                SoundEffect.CLASSIC_HIT,
                SoundEffect.HEAVY_HIT,
                SoundEffect.SWING_WHOOSH,
                SoundEffect.LASER_SHARD,
                SoundEffect.PROJECTILE_WHOOSH,
                SoundEffect.COCONUT_BOOM -> 0.94f + Random.nextFloat() * 0.12f
                else -> 1f
            }
            soundPlayer.play(sfx, pitch)
        }
    }

    // --- Input bindings ---
    fun setMoveLeft(held: Boolean) { moveLeftHeld = held }
    fun setMoveRight(held: Boolean) { moveRightHeld = held }
    fun setBlock(held: Boolean) { blockHeld = held }
    fun triggerJump() { jumpQueued = true }
    fun triggerClassic() { classicQueued = true }
    fun triggerBaseAbility() { baseQueued = true }
    fun triggerUlti() { ultiQueued = true }

    private fun consumeInput(): PlayerInput {
        val dir = (if (moveRightHeld) 1f else 0f) - (if (moveLeftHeld) 1f else 0f)
        val input = PlayerInput(
            moveDir = dir,
            jumpPressed = jumpQueued,
            blockHeld = blockHeld,
            classicPressed = classicQueued,
            baseAbilityPressed = baseQueued,
            ultiPressed = ultiQueued
        )
        jumpQueued = false
        classicQueued = false
        baseQueued = false
        ultiQueued = false
        return input
    }

    // --- Training Mode Controls ---
    fun resetTrainingHealth() {
        val current = _world.value ?: return
        _world.value = engine.resetTrainingHealth(current)
    }

    fun setTrainingOpponentMode(mode: TrainingOpponentMode) {
        val current = _world.value ?: return
        _world.value = current.copy(
            training = current.training.copy(opponentMode = mode)
        )
    }

    fun setTrainingBotDifficulty(difficulty: BotDifficulty) {
        val current = _world.value ?: return
        botDifficulty = difficulty
        _world.value = current.copy(
            training = current.training.copy(botDifficulty = difficulty)
        )
    }

    fun toggleTrainingAutoRegen() {
        val current = _world.value ?: return
        _world.value = current.copy(
            training = current.training.copy(autoRegenHp = !current.training.autoRegenHp)
        )
    }

    fun toggleTrainingInfiniteUlti() {
        val current = _world.value ?: return
        val nextInf = !current.training.infiniteUlti
        _world.value = current.copy(
            p1 = if (nextInf) current.p1.copy(ultiMeter = 100) else current.p1,
            training = current.training.copy(infiniteUlti = nextInf)
        )
    }

    fun selectTrainingChallenge(index: Int) {
        val current = _world.value ?: return
        val challenges = current.p1.def.comboChallenges
        if (challenges.isEmpty()) return
        val safeIdx = index.coerceIn(0, challenges.lastIndex)
        _world.value = current.copy(
            training = current.training.copy(
                selectedChallengeIndex = safeIdx,
                activeChallenge = challenges[safeIdx],
                currentStepIndex = 0
            )
        )
    }

    fun switchTrainingFighters(p1Id: String, p2Id: String) {
        val current = _world.value ?: return
        val p1Def = FighterRoster.getFighter(p1Id)
        val p2Def = FighterRoster.getFighter(p2Id)
        val next = engine.createInitialState(
            mode = GameMode.TRAINING,
            stage = current.stage,
            p1Def = p1Def,
            p2Def = p2Def,
            botDifficulty = current.training.botDifficulty,
            trainingOpponentMode = current.training.opponentMode
        )
        _world.value = next.copy(
            training = next.training.copy(
                autoRegenHp = current.training.autoRegenHp,
                infiniteUlti = current.training.infiniteUlti
            )
        )
    }

    fun stopAll() {
        loopJob?.cancel()
        loopJob = null
        roomObserveJob?.cancel()
        roomObserveJob = null
        onlineSyncJob?.cancel()
        onlineSyncJob = null
        activeRoomCode = null
        activeTournamentMatchId = null
        _onlineRoom.value = null
        _onlineError.value = null
        _onlineReturnedToLobby.value = false
        _world.value = null
        moveLeftHeld = false
        moveRightHeld = false
        blockHeld = false
        jumpQueued = false
        classicQueued = false
        baseQueued = false
        ultiQueued = false
    }

    override fun onCleared() {
        super.onCleared()
        stopAll()
    }
}
