package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google.")
    }

    suspend fun ensureProfile(displayName: String): Result<PlayerProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProfile(id = uid, userId = uid, displayName = displayName)
                Result.success(profile)
            } else {
                val safeName = displayName.trim().ifBlank { "Combattant" }.take(36)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "favoriteFighterId" to "glitcherman",
                    "wins" to 0L,
                    "losses" to 0L,
                    "winStreak" to 0L,
                    "bestStreak" to 0L,
                    "totalDamageDealt" to 0L,
                    "roundsWonTotal" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PlayerProfile(id = uid, userId = uid, displayName = safeName)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeProfile(userId: String): Flow<PlayerProfile?> {
        val docRef = db.collection(COLLECTION_USERS).document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    fun observeMatchHistory(userId: String): Flow<List<MatchHistoryEntry>> {
        val colRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_MATCH_HISTORY)
        return colRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MatchHistoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(error, OperationType.LIST, colRef.path)
                }
                throw error
            }
    }

    suspend fun updateFavoriteFighter(fighterId: String): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            docRef.update(
                mapOf(
                    "favoriteFighterId" to fighterId.take(38),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(
        won: Boolean,
        mode: String = "VS_BOT",
        myFighterId: String = "glitcherman",
        opponentFighterId: String = "jardin",
        opponentName: String = "Bot",
        stageId: String = "cyber_street",
        myRoundsWon: Int = if (won) 2 else 0,
        opponentRoundsWon: Int = if (won) 0 else 2,
        damageDealt: Int = 0
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val safeMode = when (mode) {
            "VS_BOT", "ONLINE", "TOURNAMENT" -> mode
            else -> "VS_BOT"
        }
        val userRef = db.collection(COLLECTION_USERS).document(uid)
        val historyId = "m_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
        val historyRef = userRef.collection(COLLECTION_MATCH_HISTORY).document(historyId)

        return try {
            db.runTransaction { tx ->
                val snap = tx.get(userRef)
                val curWins = snap.getLong("wins") ?: 0L
                val curLosses = snap.getLong("losses") ?: 0L
                val curStreak = snap.getLong("winStreak") ?: 0L
                val curBest = snap.getLong("bestStreak") ?: 0L
                val curDmg = snap.getLong("totalDamageDealt") ?: 0L
                val curRounds = snap.getLong("roundsWonTotal") ?: 0L

                val nextStreak = if (won) curStreak + 1L else 0L
                val nextBest = maxOf(curBest, nextStreak)
                val safeDmg = damageDealt.coerceIn(0, 100_000).toLong()
                val safeMyRounds = myRoundsWon.coerceIn(0, 5).toLong()

                tx.update(
                    userRef,
                    mapOf(
                        "wins" to (if (won) curWins + 1L else curWins),
                        "losses" to (if (!won) curLosses + 1L else curLosses),
                        "winStreak" to nextStreak,
                        "bestStreak" to nextBest,
                        "totalDamageDealt" to (curDmg + safeDmg).coerceAtMost(1_000_000_000L),
                        "roundsWonTotal" to (curRounds + safeMyRounds).coerceAtMost(10_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val historyPayload = mapOf(
                    "mode" to safeMode,
                    "myFighterId" to myFighterId.ifBlank { "glitcherman" }.take(38),
                    "opponentFighterId" to opponentFighterId.ifBlank { "jardin" }.take(38),
                    "opponentName" to opponentName.trim().ifBlank { "Adversaire" }.take(38),
                    "stageId" to stageId.ifBlank { "cyber_street" }.take(38),
                    "myRoundsWon" to safeMyRounds,
                    "opponentRoundsWon" to opponentRoundsWon.coerceIn(0, 5).toLong(),
                    "damageDealt" to safeDmg,
                    "won" to won,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                tx.set(historyRef, historyPayload)
                null
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminBoostWins(amount: Long = 5L): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            docRef.update(
                mapOf(
                    "wins" to FieldValue.increment(amount.coerceIn(1L, 100L)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adminResetProfileStats(): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            docRef.update(
                mapOf(
                    "wins" to 0L,
                    "losses" to 0L,
                    "winStreak" to 0L,
                    "bestStreak" to 0L,
                    "totalDamageDealt" to 0L,
                    "roundsWonTotal" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createMatchRoom(
        hostName: String,
        fighterId: String = "glitcherman",
        stageId: String = "cyber_street"
    ): Result<MatchRoom> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val code = generateRoomCode()
        val docRef = db.collection(COLLECTION_ROOMS).document(code)
        val safeName = hostName.trim().ifBlank { "Joueur 1" }.take(36)
        val payload = mapOf(
            "roomCode" to code,
            "hostId" to uid,
            "hostName" to safeName,
            "guestId" to "",
            "guestName" to "",
            "participantIds" to listOf(uid),
            "status" to "waiting",
            "stageId" to stageId,
            "hostFighterId" to fighterId,
            "guestFighterId" to "jardin",
            "hostReady" to false,
            "guestReady" to false,
            "roundNumber" to 1L,
            "p1RoundsWon" to 0L,
            "p2RoundsWon" to 0L,
            "winnerSide" to "none",
            "p1X" to 0.25,
            "p1Y" to 0.0,
            "p1Facing" to 1L,
            "p1Action" to "idle",
            "p1ActionSeq" to 0L,
            "p1Blocking" to false,
            "p1Ulti" to 0L,
            "p1DealtDmg" to 0L,
            "p2X" to 0.75,
            "p2Y" to 0.0,
            "p2Facing" to -1L,
            "p2Action" to "idle",
            "p2ActionSeq" to 0L,
            "p2Blocking" to false,
            "p2Ulti" to 0L,
            "p2DealtDmg" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            val room = docRef.get().await().toObject(
                MatchRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: MatchRoom(
                id = code,
                roomCode = code,
                hostId = uid,
                hostName = safeName,
                participantIds = listOf(uid),
                hostFighterId = fighterId,
                stageId = stageId
            )
            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun joinMatchRoom(rawCode: String, guestName: String): Result<MatchRoom> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val code = rawCode.trim().uppercase()
        if (!ROOM_CODE_REGEX.matches(code)) {
            return Result.failure(IllegalArgumentException("Code salon invalide (6 caractères A-Z / 0-9)."))
        }
        val docRef = db.collection(COLLECTION_ROOMS).document(code)
        val safeGuest = guestName.trim().ifBlank { "Joueur 2" }.take(36)
        return try {
            val joinedRoom = db.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("Salon introuvable : $code")
                }
                val room = snapshot.toObject(
                    MatchRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: throw IllegalStateException("Données du salon invalides.")

                if (room.hostId == uid || uid in room.participantIds) {
                    return@runTransaction room
                }
                if (room.status != "waiting" || room.guestId.isNotBlank()) {
                    throw IllegalStateException("Ce salon est déjà complet ou en cours.")
                }

                tx.update(
                    docRef,
                    mapOf(
                        "guestId" to uid,
                        "guestName" to safeGuest,
                        "participantIds" to listOf(room.hostId, uid),
                        "status" to "selecting",
                        "guestReady" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                room.copy(
                    guestId = uid,
                    guestName = safeGuest,
                    participantIds = listOf(room.hostId, uid),
                    status = "selecting",
                    guestReady = false
                )
            }.await()
            Result.success(joinedRoom)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeMatchRoom(roomCode: String): Flow<MatchRoom?> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    MatchRoom::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun updateLobbySelection(
        roomCode: String,
        isHost: Boolean,
        fighterId: String,
        ready: Boolean,
        stageId: String? = null,
        startFight: Boolean = false
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isHost) {
            updates["hostFighterId"] = fighterId
            updates["hostReady"] = ready
            if (stageId != null) {
                updates["stageId"] = stageId
            }
            if (startFight) {
                updates["status"] = "fighting"
                updates["roundNumber"] = 1L
                updates["p1RoundsWon"] = 0L
                updates["p2RoundsWon"] = 0L
                updates["winnerSide"] = "none"
                updates["p1X"] = 0.25
                updates["p1Y"] = 0.0
                updates["p1Facing"] = 1L
                updates["p1Action"] = "idle"
                updates["p1ActionSeq"] = 0L
                updates["p1Blocking"] = false
                updates["p1Ulti"] = 0L
                updates["p1DealtDmg"] = 0L
                updates["p2X"] = 0.75
                updates["p2Y"] = 0.0
                updates["p2Facing"] = -1L
                updates["p2Action"] = "idle"
                updates["p2ActionSeq"] = 0L
                updates["p2Blocking"] = false
                updates["p2Ulti"] = 0L
                updates["p2DealtDmg"] = 0L
            }
        } else {
            updates["guestFighterId"] = fighterId
            updates["guestReady"] = ready
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateCombatState(
        roomCode: String,
        isHost: Boolean,
        x: Double,
        y: Double,
        facing: Int,
        action: String,
        actionSeq: Long,
        blocking: Boolean,
        ulti: Int,
        dealtDmg: Int
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        val prefix = if (isHost) "p1" else "p2"
        val updates = mapOf(
            "${prefix}X" to x.coerceIn(0.0, 1.0),
            "${prefix}Y" to y.coerceIn(0.0, 1.0),
            "${prefix}Facing" to if (facing >= 0) 1L else -1L,
            "${prefix}Action" to action.take(30),
            "${prefix}ActionSeq" to actionSeq.coerceAtLeast(0L),
            "${prefix}Blocking" to blocking,
            "${prefix}Ulti" to ulti.coerceIn(0, 100).toLong(),
            "${prefix}DealtDmg" to dealtDmg.coerceIn(0, 5000).toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun advanceRound(
        roomCode: String,
        nextRound: Long,
        p1RoundsWon: Long,
        p2RoundsWon: Long
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return try {
            docRef.update(
                mapOf(
                    "roundNumber" to nextRound,
                    "p1RoundsWon" to p1RoundsWon,
                    "p2RoundsWon" to p2RoundsWon,
                    "p1X" to 0.25,
                    "p1Y" to 0.0,
                    "p1Facing" to 1L,
                    "p1Action" to "idle",
                    "p1ActionSeq" to 0L,
                    "p1Blocking" to false,
                    "p1Ulti" to 0L,
                    "p1DealtDmg" to 0L,
                    "p2X" to 0.75,
                    "p2Y" to 0.0,
                    "p2Facing" to -1L,
                    "p2Action" to "idle",
                    "p2ActionSeq" to 0L,
                    "p2Blocking" to false,
                    "p2Ulti" to 0L,
                    "p2DealtDmg" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun finishMatch(
        roomCode: String,
        winnerSide: String,
        p1RoundsWon: Long? = null,
        p2RoundsWon: Long? = null
    ): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        val updates = mutableMapOf<String, Any>(
            "status" to "finished",
            "winnerSide" to winnerSide,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (p1RoundsWon != null) updates["p1RoundsWon"] = p1RoundsWon
        if (p2RoundsWon != null) updates["p2RoundsWon"] = p2RoundsWon
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Retourne les deux joueurs dans le même salon vers la sélection de personnage pour la revanche.
     */
    suspend fun requestRematchToCharacterSelect(roomCode: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return try {
            docRef.update(
                mapOf(
                    "status" to "selecting",
                    "hostReady" to false,
                    "guestReady" to false,
                    "roundNumber" to 1L,
                    "p1RoundsWon" to 0L,
                    "p2RoundsWon" to 0L,
                    "winnerSide" to "none",
                    "p1X" to 0.25,
                    "p1Y" to 0.0,
                    "p1Facing" to 1L,
                    "p1Action" to "idle",
                    "p1ActionSeq" to 0L,
                    "p1Blocking" to false,
                    "p1Ulti" to 0L,
                    "p1DealtDmg" to 0L,
                    "p2X" to 0.75,
                    "p2Y" to 0.0,
                    "p2Facing" to -1L,
                    "p2Action" to "idle",
                    "p2ActionSeq" to 0L,
                    "p2Blocking" to false,
                    "p2Ulti" to 0L,
                    "p2DealtDmg" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun leaveMatchRoom(roomCode: String, isHost: Boolean): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return try {
            if (isHost) {
                docRef.delete().await()
            } else {
                val snap = docRef.get().await()
                val hostId = snap.getString("hostId") ?: return Result.success(Unit)
                docRef.update(
                    mapOf(
                        "guestId" to "",
                        "guestName" to "",
                        "participantIds" to listOf(hostId),
                        "status" to "waiting",
                        "hostReady" to false,
                        "guestReady" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMatchRoom(roomCode: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_ROOMS).document(roomCode)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun submitFeedback(
        authorName: String,
        modeTested: String,
        category: String,
        fighterId: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val cleanComment = comment.trim()
        if (cleanComment.length < 3) {
            return Result.failure(IllegalArgumentException("Écrivez au moins 3 caractères pour votre retour."))
        }
        val safeAuthor = authorName.trim().ifBlank { "Testeur" }.take(38)
        val docRef = db.collection(COLLECTION_FEEDBACKS).document("fb_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}")
        val payload = mapOf(
            "authorId" to uid,
            "authorName" to safeAuthor,
            "modeTested" to modeTested,
            "category" to category,
            "fighterId" to fighterId.ifBlank { "all" }.take(38),
            "rating" to rating.coerceIn(1, 5).toLong(),
            "comment" to cleanComment.take(395),
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeFeedbacks(): Flow<List<PlaytestFeedback>> {
        return db.collection(COLLECTION_FEEDBACKS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PlaytestFeedback::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error is CancellationException && error.cause == null) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(error, OperationType.LIST, COLLECTION_FEEDBACKS)
                }
                throw error
            }
    }

    suspend fun deleteFeedback(feedbackId: String): Result<Unit> {
        val docRef = db.collection(COLLECTION_FEEDBACKS).document(feedbackId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return buildString(6) {
            repeat(6) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_MATCH_HISTORY = "match_history"
        const val COLLECTION_ROOMS = "match_rooms"
        const val COLLECTION_FEEDBACKS = "playtest_feedbacks"
        private val ROOM_CODE_REGEX = Regex("^[A-Z0-9]{6}$")
    }
}
