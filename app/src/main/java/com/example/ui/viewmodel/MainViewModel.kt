package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GameRepository
import com.example.data.firebase.MatchRoom
import com.example.data.firebase.PlayerProfile
import com.example.data.firebase.PlaytestFeedback
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
    private val userDisplayName: String
) : ViewModel() {

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

    val myRoomsState: StateFlow<UiState<List<MatchRoom>>> = repository.observeMyRooms(currentUserId)
        .map<List<MatchRoom>, UiState<List<MatchRoom>>> { list ->
            UiState.Success(list.sortedByDescending { it.updatedAt?.seconds ?: 0L })
        }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Rooms observe error", fsErr ?: error)
            emit(UiState.Error((fsErr ?: error).message ?: "Erreur salons en ligne"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _lobbyMessage = MutableStateFlow<String?>(null)
    val lobbyMessage: StateFlow<String?> = _lobbyMessage.asStateFlow()

    private val _lobbyBusy = MutableStateFlow(false)
    val lobbyBusy: StateFlow<Boolean> = _lobbyBusy.asStateFlow()

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

    fun clearLobbyMessage() {
        _lobbyMessage.value = null
    }

    fun setFavoriteFighter(fighterId: String) {
        viewModelScope.launch {
            repository.updateFavoriteFighter(fighterId)
        }
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
