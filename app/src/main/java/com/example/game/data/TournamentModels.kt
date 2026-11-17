package com.example.game.data

import kotlin.random.Random

data class TournamentParticipant(
    val seed: Int,
    val id: String,
    val name: String,
    val fighterId: String,
    val isHuman: Boolean,
    val isBye: Boolean = false
)

data class TournamentMatch(
    val id: String,
    val roundIndex: Int, // 0 = premier tour ... totalRounds - 1 = finale
    val matchIndexInRound: Int,
    val p1: TournamentParticipant?,
    val p2: TournamentParticipant?,
    val winner: TournamentParticipant? = null,
    val p1Score: Int = 0,
    val p2Score: Int = 0,
    val isByeMatch: Boolean = false
) {
    val isReadyToPlay: Boolean
        get() = winner == null && p1 != null && p2 != null && !p1.isBye && !p2.isBye

    val involvesHuman: Boolean
        get() = (p1?.isHuman == true) || (p2?.isHuman == true)
}

data class TournamentRound(
    val roundIndex: Int,
    val title: String,
    val matches: List<TournamentMatch>
)

data class TournamentState(
    val size: Int,
    val difficulty: BotDifficulty,
    val stageId: String, // "random" ou id d'arène
    val participants: List<TournamentParticipant>,
    val rounds: List<TournamentRound>,
    val champion: TournamentParticipant? = null,
    val lastCompletedNote: String? = null
) {
    val nextPlayableMatch: TournamentMatch?
        get() = rounds
            .asSequence()
            .flatMap { it.matches.asSequence() }
            .firstOrNull { it.isReadyToPlay }
}

object TournamentEngine {
    val supportedSizes = listOf(2, 3, 4, 8, 10)

    private val botRosterNames = listOf(
        "Cyber-Viper", "Kenshin-X", "Titan Vert", "Tigre de Rio",
        "Néo-Shadow", "Lame Dorée", "Brawler Z", "Spectre 77", "Furia Prime"
    )

    fun createParticipants(
        size: Int,
        humanName: String,
        humanFighterId: String
    ): List<TournamentParticipant> {
        val validSize = if (size in supportedSizes) size else 4
        val allFighters = FighterRoster.fighters
        val list = mutableListOf<TournamentParticipant>()
        list += TournamentParticipant(
            seed = 1,
            id = "player_human",
            name = humanName.ifBlank { "Vous" },
            fighterId = humanFighterId,
            isHuman = true
        )
        for (idx in 1 until validSize) {
            val botFighter = allFighters[(idx + Random.nextInt(allFighters.size)) % allFighters.size]
            val botName = botRosterNames.getOrElse(idx - 1) { "Bot #$idx" }
            list += TournamentParticipant(
                seed = idx + 1,
                id = "bot_$idx",
                name = botName,
                fighterId = botFighter.id,
                isHuman = false
            )
        }
        return list
    }

    fun generateBracket(
        participants: List<TournamentParticipant>,
        difficulty: BotDifficulty,
        stageId: String
    ): TournamentState {
        val n = participants.size
        val bracketSlots = when {
            n <= 2 -> 2
            n <= 4 -> 4
            n <= 8 -> 8
            else -> 16
        }
        val totalRounds = when (bracketSlots) {
            2 -> 1
            4 -> 2
            8 -> 3
            else -> 4
        }

        // Construit l'ordre standard des paires en ajoutant des BYE sur les têtes de série supérieures
        // pour que tous les BYE soient résolus dès le tour 0 (ex: 3 -> 1 bye, 10 -> 6 byes).
        val padded = MutableList(bracketSlots) { idx ->
            participants.getOrNull(idx) ?: TournamentParticipant(
                seed = idx + 1,
                id = "bye_$idx",
                name = "EXEMPT (BYE)",
                fighterId = FighterRoster.fighters.first().id,
                isHuman = false,
                isBye = true
            )
        }

        // Pour n = 10 dans un bracket à 16 : 6 premières têtes de série jouent contre BYE (qualifiées d'office),
        // et les seeds 7..10 jouent les 2 matchs préliminaires (7v10, 8v9).
        val round0Pairs: List<Pair<TournamentParticipant, TournamentParticipant>> = when (bracketSlots) {
            2 -> listOf(padded[0] to padded[1])
            4 -> {
                if (n == 3) {
                    listOf(
                        padded[0] to padded[3], // Seed 1 vs BYE -> Qualifié d'office en finale
                        padded[1] to padded[2]  // Seed 2 vs Seed 3 -> Demi-finale réelle
                    )
                } else {
                    listOf(padded[0] to padded[3], padded[1] to padded[2])
                }
            }
            8 -> listOf(
                padded[0] to padded[7],
                padded[3] to padded[4],
                padded[1] to padded[6],
                padded[2] to padded[5]
            )
            else -> { // 16 slots (pour N = 10 -> 6 BYE, 2 matchs préliminaires)
                listOf(
                    padded[0] to padded[15], // Seed 1 vs BYE
                    padded[7] to padded[8],  // Match prélim A : Seed 8 vs Seed 9
                    padded[3] to padded[12], // Seed 4 vs BYE
                    padded[4] to padded[11], // Seed 5 vs BYE
                    padded[1] to padded[14], // Seed 2 vs BYE
                    padded[6] to padded[9],  // Match prélim B : Seed 7 vs Seed 10
                    padded[2] to padded[13], // Seed 3 vs BYE
                    padded[5] to padded[10]  // Seed 6 vs BYE
                )
            }
        }

        val rounds = MutableList(totalRounds) { rIdx ->
            val matchCount = bracketSlots shr (rIdx + 1)
            val title = roundTitle(rIdx, totalRounds, n)
            val matches = List(matchCount) { mIdx ->
                if (rIdx == 0) {
                    val (a, b) = round0Pairs[mIdx]
                    val isBye = a.isBye || b.isBye
                    val autoWinner = when {
                        a.isBye && !b.isBye -> b
                        b.isBye && !a.isBye -> a
                        else -> null
                    }
                    TournamentMatch(
                        id = "r${rIdx}_m$mIdx",
                        roundIndex = rIdx,
                        matchIndexInRound = mIdx,
                        p1 = if (a.isBye) null else a,
                        p2 = if (b.isBye) null else b,
                        winner = autoWinner,
                        isByeMatch = isBye
                    )
                } else {
                    TournamentMatch(
                        id = "r${rIdx}_m$mIdx",
                        roundIndex = rIdx,
                        matchIndexInRound = mIdx,
                        p1 = null,
                        p2 = null
                    )
                }
            }
            TournamentRound(roundIndex = rIdx, title = title, matches = matches)
        }

        val initialState = TournamentState(
            size = n,
            difficulty = difficulty,
            stageId = stageId,
            participants = participants,
            rounds = rounds
        )
        return propagateWinners(initialState)
    }

    fun recordMatchResult(
        state: TournamentState,
        matchId: String,
        winnerSide: Int,
        p1Score: Int,
        p2Score: Int
    ): TournamentState {
        var winnerParticipant: TournamentParticipant? = null
        var note: String? = null

        val updatedRounds = state.rounds.map { round ->
            round.copy(
                matches = round.matches.map { match ->
                    if (match.id == matchId) {
                        val w = if (winnerSide == 1) match.p1 else match.p2
                        winnerParticipant = w
                        if (w != null && match.p1 != null && match.p2 != null) {
                            note = "${w.name} bat ${if (winnerSide == 1) match.p2.name else match.p1.name} ($p1Score - $p2Score)"
                        }
                        match.copy(
                            winner = w,
                            p1Score = p1Score,
                            p2Score = p2Score
                        )
                    } else {
                        match
                    }
                }
            )
        }

        val next = state.copy(
            rounds = updatedRounds,
            lastCompletedNote = note ?: state.lastCompletedNote
        )
        return propagateWinners(next)
    }

    fun simulateAiMatch(state: TournamentState, match: TournamentMatch): TournamentState {
        val p1 = match.p1 ?: return state
        val p2 = match.p2 ?: return state
        val winnerSide = if (Random.nextBoolean()) 1 else 2
        val loserScore = if (Random.nextFloat() < 0.45f) 1 else 0
        val p1Score = if (winnerSide == 1) 2 else loserScore
        val p2Score = if (winnerSide == 2) 2 else loserScore
        return recordMatchResult(state, match.id, winnerSide, p1Score, p2Score)
    }

    private fun propagateWinners(state: TournamentState): TournamentState {
        val mutableRounds = state.rounds.map { it.copy(matches = it.matches.toMutableList()) }.toMutableList()
        for (r in 0 until mutableRounds.size - 1) {
            val currentMatches = mutableRounds[r].matches
            val nextMatches = mutableRounds[r + 1].matches.toMutableList()
            for (m in currentMatches.indices) {
                val winner = currentMatches[m].winner ?: continue
                val targetIdx = m / 2
                val existingNext = nextMatches[targetIdx]
                nextMatches[targetIdx] = if (m % 2 == 0) {
                    existingNext.copy(p1 = winner)
                } else {
                    existingNext.copy(p2 = winner)
                }
            }
            mutableRounds[r + 1] = mutableRounds[r + 1].copy(matches = nextMatches)
        }

        val finalMatch = mutableRounds.lastOrNull()?.matches?.firstOrNull()
        return state.copy(
            rounds = mutableRounds,
            champion = finalMatch?.winner
        )
    }

    private fun roundTitle(roundIndex: Int, totalRounds: Int, participantCount: Int): String {
        val fromEnd = totalRounds - 1 - roundIndex
        return when (fromEnd) {
            0 -> "FINALE"
            1 -> "DEMI-FINALES"
            2 -> "QUARTS DE FINALE"
            else -> if (participantCount == 10) "TOUR PRÉLIMINAIRE" else "HUITIÈMES"
        }
    }
}
