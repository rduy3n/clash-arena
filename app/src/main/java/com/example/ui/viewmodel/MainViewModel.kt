package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GameRepository
import com.example.data.firebase.MatchHistoryEntry
import com.example.data.firebase.PlayerProfile
import com.example.data.firebase.PlaytestFeedback
import com.example.game.data.AdminAccess
import com.example.game.data.AdminCombatModifiers
import com.example.game.data.BotDifficulty
import com.example.game.data.TournamentEngine
import com.example.game.data.TournamentParticipant
import com.example.game.data.TournamentState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel(
    private val repository: GameRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String? = null
) : ViewModel() {

    val isAdmin: Boolean = AdminAccess.isVerifiedAdminEmail(userEmail)
    val verifiedEmail: String = userEmail.orEmpty()

    val profileState: StateFlow<UiState<PlayerProfile?>> = repository.observeProfile(currentUserId)
        .map<PlayerProfile?, UiState<PlayerProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Profile observe error", fsErr ?: error)
            emit(UiState.Error((fsErr ?: error).message ?: "Erreur de profil"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val matchHistoryState: StateFlow<UiState<List<MatchHistoryEntry>>> = repository.observeMatchHistory(currentUserId)
        .map<List<MatchHistoryEntry>, UiState<List<MatchHistoryEntry>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Match history observe error", fsErr ?: error)
            emit(UiState.Error((fsErr ?: error).message ?: "Erreur historique"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _activeOnlineRoomCode = MutableStateFlow<String?>(null)
    val activeOnlineRoomCode: StateFlow<String?> = _activeOnlineRoomCode.asStateFlow()

    private val _lobbyMessage = MutableStateFlow<String?>(null)
    val lobbyMessage: StateFlow<String?> = _lobbyMessage.asStateFlow()

    private val _lobbyBusy = MutableStateFlow(false)
    val lobbyBusy: StateFlow<Boolean> = _lobbyBusy.asStateFlow()

    private val _adminModifiers = MutableStateFlow(AdminCombatModifiers())
    val adminModifiers: StateFlow<AdminCombatModifiers> = _adminModifiers.asStateFlow()

    private val _adminStatus = MutableStateFlow<String?>(null)
    val adminStatus: StateFlow<String?> = _adminStatus.asStateFlow()

    private val _tournamentState = MutableStateFlow<TournamentState?>(null)
    val tournamentState: StateFlow<TournamentState?> = _tournamentState.asStateFlow()

    val feedbacksState: StateFlow<UiState<List<PlaytestFeedback>>> = repository.observeFeedbacks()
        .map<List<PlaytestFeedback>, UiState<List<PlaytestFeedback>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Feedback observe error", fsErr ?: error)
            emit(UiState.Error((fsErr ?: error).message ?: "Erreur chargement retours"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _feedbackBusy = MutableStateFlow(false)
    val feedbackBusy: StateFlow<Boolean> = _feedbackBusy.asStateFlow()

    private val _feedbackStatus = MutableStateFlow<String?>(null)
    val feedbackStatus: StateFlow<String?> = _feedbackStatus.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureProfile(userDisplayName)
        }
    }

    fun setActiveOnlineRoomCode(code: String?) {
        _activeOnlineRoomCode.value = code
    }

    fun clearLobbyMessage() {
        _lobbyMessage.value = null
    }

    fun setFavoriteFighter(fighterId: String) {
        viewModelScope.launch {
            repository.updateFavoriteFighter(fighterId)
        }
    }

    fun updateAdminModifiers(modifiers: AdminCombatModifiers) {
        if (!isAdmin) return
        _adminModifiers.value = modifiers
    }

    fun adminBoostWins(amount: Long = 5L) {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminBoostWins(amount)
                .onSuccess { _adminStatus.value = "+$amount victoires ajoutées au profil." }
                .onFailure { _adminStatus.value = it.message ?: "Erreur admin." }
        }
    }

    fun adminResetProfileStats() {
        if (!isAdmin) return
        viewModelScope.launch {
            repository.adminResetProfileStats()
                .onSuccess { _adminStatus.value = "Statistiques réinitialisées à 0." }
                .onFailure { _adminStatus.value = it.message ?: "Erreur admin." }
        }
    }

    fun clearAdminStatus() {
        _adminStatus.value = null
    }

    fun startTournament(
        size: Int,
        humanFighterId: String,
        difficulty: BotDifficulty,
        stageId: String
    ) {
        val playerName = (profileState.value as? UiState.Success)?.data?.displayName ?: userDisplayName
        val participants = TournamentEngine.createParticipants(
            size = size,
            humanName = playerName,
            humanFighterId = humanFighterId
        )
        _tournamentState.value = TournamentEngine.generateBracket(
            participants = participants,
            difficulty = difficulty,
            stageId = stageId
        )
    }

    fun updateTournamentParticipants(
        participants: List<TournamentParticipant>,
        difficulty: BotDifficulty,
        stageId: String
    ) {
        _tournamentState.value = TournamentEngine.generateBracket(
            participants = participants,
            difficulty = difficulty,
            stageId = stageId
        )
    }

    fun simulateNextAiTournamentMatch() {
        val current = _tournamentState.value ?: return
        val nextMatch = current.nextPlayableMatch ?: return
        if (nextMatch.involvesHuman) return
        _tournamentState.value = TournamentEngine.simulateAiMatch(current, nextMatch)
    }

    fun simulateAllReadyAiMatches() {
        var current = _tournamentState.value ?: return
        var safety = 0
        while (safety < 32) {
            val next = current.nextPlayableMatch ?: break
            if (next.involvesHuman) break
            current = TournamentEngine.simulateAiMatch(current, next)
            safety++
        }
        _tournamentState.value = current
    }

    fun recordTournamentBattleResult(
        matchId: String,
        winnerSide: Int,
        p1Score: Int,
        p2Score: Int
    ) {
        val current = _tournamentState.value ?: return
        val alreadyRecorded = current.rounds.any { r ->
            r.matches.any { it.id == matchId && it.winner != null }
        }
        if (alreadyRecorded) return
        _tournamentState.value = TournamentEngine.recordMatchResult(
            state = current,
            matchId = matchId,
            winnerSide = winnerSide,
            p1Score = p1Score,
            p2Score = p2Score
        )
    }

    fun resetTournament() {
        _tournamentState.value = null
    }

    fun createOnlineRoom(
        fighterId: String,
        stageId: String,
        onRoomReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            _lobbyBusy.value = true
            _lobbyMessage.value = null
            val name = (profileState.value as? UiState.Success)?.data?.displayName ?: userDisplayName
            val result = repository.createMatchRoom(name, fighterId, stageId)
            _lobbyBusy.value = false
            result.onSuccess { room ->
                _activeOnlineRoomCode.value = room.roomCode
                onRoomReady(room.roomCode)
            }.onFailure { err ->
                _lobbyMessage.value = err.message ?: "Impossible de créer le salon."
            }
        }
    }

    fun joinOnlineRoom(
        code: String,
        onRoomReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            _lobbyBusy.value = true
            _lobbyMessage.value = null
            val name = (profileState.value as? UiState.Success)?.data?.displayName ?: userDisplayName
            val result = repository.joinMatchRoom(code, name)
            _lobbyBusy.value = false
            result.onSuccess { room ->
                _activeOnlineRoomCode.value = room.roomCode
                onRoomReady(room.roomCode)
            }.onFailure { err ->
                _lobbyMessage.value = err.message ?: "Impossible de rejoindre ce salon."
            }
        }
    }

    fun submitPlaytestFeedback(
        authorName: String,
        modeTested: String,
        category: String,
        fighterId: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _feedbackBusy.value = true
            _feedbackStatus.value = null
            val res = repository.submitFeedback(
                authorName = authorName,
                modeTested = modeTested,
                category = category,
                fighterId = fighterId,
                rating = rating,
                comment = comment
            )
            _feedbackBusy.value = false
            res.onSuccess {
                _feedbackStatus.value = "Merci ! Retour envoyé et sauvegardé."
                onSuccess()
            }.onFailure { err ->
                _feedbackStatus.value = err.message ?: "Erreur lors de l'envoi du retour."
            }
        }
    }

    fun deletePlaytestFeedback(feedbackId: String) {
        viewModelScope.launch {
            repository.deleteFeedback(feedbackId)
        }
    }

    fun clearFeedbackStatus() {
        _feedbackStatus.value = null
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "MainViewModel"
    }
}
