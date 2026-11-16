package com.example.game.engine

import androidx.compose.ui.graphics.Color
import com.example.game.audio.SoundEffect
import com.example.game.data.ArenaStage
import com.example.game.data.BotDifficulty
import com.example.game.data.ComboInputStep
import com.example.game.data.FighterDefinition
import com.example.game.data.GameMode
import com.example.game.data.MoveEffectType
import com.example.game.data.MoveSlot
import com.example.game.data.MoveSpec
import com.example.game.data.TrainingOpponentMode
import kotlin.math.abs
import kotlin.random.Random

class CombatEngine(
    private var nextEntityId: Long = 1L
) {
    private var botDecisionAccumMs = 0L
    private var currentBotInput = PlayerInput()
    private var trainingIdleAccumMs = 0L

    fun createInitialState(
        mode: GameMode,
        stage: ArenaStage,
        p1Def: FighterDefinition,
        p2Def: FighterDefinition,
        botDifficulty: BotDifficulty = BotDifficulty.NORMAL,
        trainingOpponentMode: TrainingOpponentMode = TrainingOpponentMode.STATIONARY
    ): ArenaWorldState {
        val isTraining = mode == GameMode.TRAINING
        val p1 = FighterRuntime(
            side = 1,
            def = p1Def,
            x = 0.25f,
            facing = 1,
            hp = p1Def.stats.maxHp,
            ultiMeter = if (isTraining) 100 else 0
        )
        val p2 = FighterRuntime(
            side = 2,
            def = p2Def,
            x = 0.75f,
            facing = -1,
            hp = p2Def.stats.maxHp,
            ultiMeter = 0
        )
        val firstChallenge = p1Def.comboChallenges.firstOrNull()
        return ArenaWorldState(
            mode = mode,
            stage = stage,
            phase = if (isTraining) MatchPhase.FIGHTING else MatchPhase.ROUND_INTRO,
            phaseTimerMs = if (isTraining) 0L else 1500L,
            roundNumber = 1,
            roundTimeRemainingSec = 99,
            bannerText = if (isTraining) "" else "ROUND 1",
            p1 = p1,
            p2 = p2,
            training = TrainingHudState(
                opponentMode = trainingOpponentMode,
                botDifficulty = botDifficulty,
                autoRegenHp = true,
                infiniteUlti = true,
                selectedChallengeIndex = 0,
                activeChallenge = firstChallenge
            )
        )
    }

    fun resetTrainingHealth(state: ArenaWorldState): ArenaWorldState {
        return state.copy(
            p1 = state.p1.copy(
                hp = state.p1.def.stats.maxHp,
                ultiMeter = if (state.training.infiniteUlti) 100 else state.p1.ultiMeter,
                hitStunMs = 0L,
                vineSnaredMs = 0L,
                chargingMegaPunch = false,
                megaPunchDelayMs = 0L,
                comboCount = 0,
                comboDamage = 0
            ),
            p2 = state.p2.copy(
                hp = state.p2.def.stats.maxHp,
                hitStunMs = 0L,
                vineSnaredMs = 0L,
                chargingMegaPunch = false,
                megaPunchDelayMs = 0L,
                comboCount = 0,
                comboDamage = 0,
                x = 0.72f,
                y = 0f,
                vy = 0f,
                action = CombatAction.IDLE
            ),
            bannerText = state.bannerText,
            ultiCinematic = null,
            projectiles = emptyList(),
            popups = state.popups + FloatingPopup(
                id = nextId(),
                x = 0.5f,
                y = 0.35f,
                text = "PV RÉINITIALISÉS",
                color = Color(0xFF00E676)
            )
        )
    }

    fun step(
        state: ArenaWorldState,
        dtMs: Long,
        p1Input: PlayerInput,
        botDifficulty: BotDifficulty = BotDifficulty.NORMAL
    ): ArenaWorldState {
        val clampedDt = dtMs.coerceIn(1L, 50L)
        val clearedState = state.copy(soundEvents = emptyList())

        // If a 2-second slow-motion Ulti cinematic is active, run bullet-time simulation
        val activeCine = clearedState.ultiCinematic
        if (activeCine != null) {
            val nextRem = (activeCine.remainingMs - clampedDt).coerceAtLeast(0L)
            val slowDt = (clampedDt * 0.16f).toLong().coerceAtLeast(1L)
            var next = ageEffects(clearedState, slowDt)
            next = simulateSlowMoTick(next, slowDt)
            return if (nextRem > 0L) {
                next.copy(ultiCinematic = activeCine.copy(remainingMs = nextRem))
            } else {
                // Cinematic finished: release the real Ulti flash & impact!
                val caster = if (activeCine.casterSide == 1) next.p1 else next.p2
                next.copy(
                    ultiCinematic = null,
                    ultiFlashMs = 380L,
                    ultiFlashColor = activeCine.primaryColor,
                    screenShakeMs = 260L,
                    p1 = if (activeCine.casterSide == 1) caster.copy(actionTimerMs = caster.def.ultimateMove.startupMs) else next.p1,
                    p2 = if (activeCine.casterSide == 2) caster.copy(actionTimerMs = caster.def.ultimateMove.startupMs) else next.p2
                )
            }
        }

        var next = ageEffects(clearedState, clampedDt)

        if (next.mode == GameMode.TRAINING) {
            return stepTraining(next, clampedDt, p1Input)
        }

        return when (next.phase) {
            MatchPhase.ROUND_INTRO -> {
                val rem = next.phaseTimerMs - clampedDt
                when {
                    rem > 600L -> next.copy(phaseTimerMs = rem, bannerText = "ROUND ${next.roundNumber}")
                    rem > 0L -> next.copy(phaseTimerMs = rem, bannerText = "COMBAT !")
                    else -> next.copy(
                        phase = MatchPhase.FIGHTING,
                        phaseTimerMs = 0L,
                        bannerText = "",
                        soundEvents = next.soundEvents + SoundEffect.ROUND_START
                    )
                }
            }

            MatchPhase.FIGHTING -> {
                val p2Input = computeBotInput(next, clampedDt, botDifficulty)
                next = simulateCombatTick(next, clampedDt, p1Input, p2Input)

                val newClockAccum = next.roundClockAccumMs + clampedDt
                var sec = next.roundTimeRemainingSec
                var clockRem = newClockAccum
                if (clockRem >= 1000L) {
                    clockRem -= 1000L
                    sec = (sec - 1).coerceAtLeast(0)
                }
                next = next.copy(roundTimeRemainingSec = sec, roundClockAccumMs = clockRem)

                val p1Dead = next.p1.hp <= 0
                val p2Dead = next.p2.hp <= 0
                if (p1Dead || p2Dead || sec <= 0) {
                    val roundWinner = when {
                        p2Dead && !p1Dead -> 1
                        p1Dead && !p2Dead -> 2
                        next.p1.hp.toFloat() / next.p1.def.stats.maxHp >= next.p2.hp.toFloat() / next.p2.def.stats.maxHp -> 1
                        else -> 2
                    }
                    val p1Wins = next.p1RoundsWon + if (roundWinner == 1) 1 else 0
                    val p2Wins = next.p2RoundsWon + if (roundWinner == 2) 1 else 0
                    val matchFinished = p1Wins >= 2 || p2Wins >= 2
                    val banner = if (p1Dead || p2Dead) "K.O. !" else "TEMPS ÉCOULÉ !"
                    val endSounds = buildList {
                        add(SoundEffect.KO_BOOM)
                        if (matchFinished) add(SoundEffect.VICTORY_FANFARE)
                    }
                    next.copy(
                        phase = if (matchFinished) MatchPhase.MATCH_OVER else MatchPhase.ROUND_END,
                        phaseTimerMs = if (matchFinished) 0L else 2200L,
                        p1RoundsWon = p1Wins,
                        p2RoundsWon = p2Wins,
                        winnerSide = if (matchFinished) roundWinner else 0,
                        bannerText = banner,
                        soundEvents = next.soundEvents + endSounds,
                        p1 = if (p1Dead) next.p1.copy(action = CombatAction.KO) else next.p1,
                        p2 = if (p2Dead) next.p2.copy(action = CombatAction.KO) else next.p2
                    )
                } else {
                    next
                }
            }

            MatchPhase.ROUND_END -> {
                val rem = next.phaseTimerMs - clampedDt
                if (rem > 0L) {
                    next.copy(phaseTimerMs = rem)
                } else {
                    val nextRound = next.roundNumber + 1
                    next.copy(
                        phase = MatchPhase.ROUND_INTRO,
                        phaseTimerMs = 1500L,
                        roundNumber = nextRound,
                        roundTimeRemainingSec = 99,
                        roundClockAccumMs = 0L,
                        bannerText = "ROUND $nextRound",
                        ultiCinematic = null,
                        projectiles = emptyList(),
                        p1 = next.p1.copy(
                            x = 0.25f, y = 0f, vy = 0f, facing = 1,
                            hp = next.p1.def.stats.maxHp,
                            action = CombatAction.IDLE,
                            hitStunMs = 0L, vineSnaredMs = 0L,
                            chargingMegaPunch = false, megaPunchDelayMs = 0L,
                            classicCooldownMs = 0L, baseCooldownMs = 0L,
                            comboCount = 0, comboDamage = 0, dealtDamageTotal = 0
                        ),
                        p2 = next.p2.copy(
                            x = 0.75f, y = 0f, vy = 0f, facing = -1,
                            hp = next.p2.def.stats.maxHp,
                            action = CombatAction.IDLE,
                            hitStunMs = 0L, vineSnaredMs = 0L,
                            chargingMegaPunch = false, megaPunchDelayMs = 0L,
                            classicCooldownMs = 0L, baseCooldownMs = 0L,
                            comboCount = 0, comboDamage = 0, dealtDamageTotal = 0
                        )
                    )
                }
            }

            MatchPhase.MATCH_OVER -> next
        }
    }

    private fun stepTraining(
        state: ArenaWorldState,
        dtMs: Long,
        p1Input: PlayerInput
    ): ArenaWorldState {
        val p2Input = when (state.training.opponentMode) {
            TrainingOpponentMode.STATIONARY -> PlayerInput()
            TrainingOpponentMode.AUTO_BLOCK -> PlayerInput(blockHeld = true)
            TrainingOpponentMode.AI_BOT -> computeBotInput(state, dtMs, state.training.botDifficulty)
        }
        var next = simulateCombatTick(state, dtMs, p1Input, p2Input)

        if (next.p1.hp <= 0) {
            next = next.copy(p1 = next.p1.copy(hp = next.p1.def.stats.maxHp))
        }
        if (next.p2.hp <= 0) {
            next = next.copy(p2 = next.p2.copy(hp = 1))
        }

        if (next.training.infiniteUlti && next.p1.ultiMeter < 100 && next.p1.action != CombatAction.ULTI) {
            next = next.copy(p1 = next.p1.copy(ultiMeter = 100))
        }

        if (next.training.autoRegenHp && next.p2.hitStunMs <= 0L && next.p2.vineSnaredMs <= 0L && next.p1.comboExpireMs <= 0L) {
            trainingIdleAccumMs += dtMs
            if (trainingIdleAccumMs >= 1400L && next.p2.hp < next.p2.def.stats.maxHp) {
                next = next.copy(p2 = next.p2.copy(hp = next.p2.def.stats.maxHp))
                trainingIdleAccumMs = 0L
            }
        } else {
            trainingIdleAccumMs = 0L
        }

        val bannerRem = (next.training.challengeSuccessBannerMs - dtMs).coerceAtLeast(0L)
        if (bannerRem != next.training.challengeSuccessBannerMs) {
            next = next.copy(training = next.training.copy(challengeSuccessBannerMs = bannerRem))
        }

        return next
    }

    private fun simulateSlowMoTick(state: ArenaWorldState, slowDt: Long): ArenaWorldState {
        // Keep fighters in slow-motion kinematics while freezing Ulti hit startup until cinematic ends
        val p1 = updateFighterKinematics(state.p1, PlayerInput(), state.p2.x, slowDt).let { f ->
            if (f.action == CombatAction.ULTI) {
                f.copy(actionTimerMs = f.actionTimerMs.coerceAtMost((f.def.ultimateMove.startupMs - 10L).coerceAtLeast(0L)))
            } else f
        }
        val p2 = updateFighterKinematics(state.p2, PlayerInput(), state.p1.x, slowDt).let { f ->
            if (f.action == CombatAction.ULTI) {
                f.copy(actionTimerMs = f.actionTimerMs.coerceAtMost((f.def.ultimateMove.startupMs - 10L).coerceAtLeast(0L)))
            } else f
        }
        return state.copy(p1 = p1, p2 = p2)
    }

    fun simulateCombatTick(
        state: ArenaWorldState,
        dtMs: Long,
        p1Input: PlayerInput,
        p2Input: PlayerInput
    ): ArenaWorldState {
        val spawnedSounds = mutableListOf<SoundEffect>()
        val p1WasGrounded = state.p1.y <= 0.001f
        val p2WasGrounded = state.p2.y <= 0.001f

        var p1 = updateFighterKinematics(state.p1, p1Input, state.p2.x, dtMs)
        var p2 = updateFighterKinematics(state.p2, p2Input, state.p1.x, dtMs)

        if (p1WasGrounded && p1.y > 0.001f) spawnedSounds += SoundEffect.JUMP
        if (p2WasGrounded && p2.y > 0.001f) spawnedSounds += SoundEffect.JUMP

        val spawnEffects = mutableListOf<SlashEffect>()
        val spawnProjectiles = mutableListOf<Projectile>()

        val p1PrevSeq = p1.actionSeq
        val p2PrevSeq = p2.actionSeq

        p1 = tryTriggerAttack(p1, p1Input, spawnEffects, spawnProjectiles, spawnedSounds)
        p2 = tryTriggerAttack(p2, p2Input, spawnEffects, spawnProjectiles, spawnedSounds)

        val p1StartedUlti = p1.action == CombatAction.ULTI && p1.actionSeq != p1PrevSeq
        val p2StartedUlti = p2.action == CombatAction.ULTI && p2.actionSeq != p2PrevSeq

        var nextCine = state.ultiCinematic
        var flashMs = state.ultiFlashMs
        var flashColor = state.ultiFlashColor
        if (p1StartedUlti) {
            flashMs = 340L
            flashColor = p1.def.primaryColor
            nextCine = UltiCinematicState(
                casterSide = 1,
                fighterId = p1.def.id,
                moveName = p1.def.ultimateMove.name,
                remainingMs = 2000L,
                totalMs = 2000L,
                visualStyle = p1.def.visualStyle,
                primaryColor = p1.def.primaryColor,
                accentColor = p1.def.accentColor
            )
            // Extend duration slightly so Ulti remains active after 2s slow-mo
            p1 = p1.copy(actionDurationMs = p1.actionDurationMs + 360L)
        } else if (p2StartedUlti) {
            flashMs = 340L
            flashColor = p2.def.primaryColor
            nextCine = UltiCinematicState(
                casterSide = 2,
                fighterId = p2.def.id,
                moveName = p2.def.ultimateMove.name,
                remainingMs = 2000L,
                totalMs = 2000L,
                visualStyle = p2.def.visualStyle,
                primaryColor = p2.def.primaryColor,
                accentColor = p2.def.accentColor
            )
            p2 = p2.copy(actionDurationMs = p2.actionDurationMs + 360L)
        }

        // Extend spawned Ulti slash effect lifetime if entering 2s slow-mo cinematic
        val adjustedEffects = if (p1StartedUlti || p2StartedUlti) {
            spawnEffects.map { it.copy(maxAgeMs = it.maxAgeMs + 420L) }
        } else {
            spawnEffects
        }

        var nextState = state.copy(
            p1 = p1,
            p2 = p2,
            ultiFlashMs = flashMs,
            ultiFlashColor = flashColor,
            ultiCinematic = nextCine,
            slashEffects = state.slashEffects + adjustedEffects,
            projectiles = state.projectiles + spawnProjectiles,
            soundEvents = state.soundEvents + spawnedSounds
        )

        if (nextCine == null) {
            nextState = resolveAttackHit(nextState, attackerSide = 1, dtMs = dtMs)
            nextState = resolveAttackHit(nextState, attackerSide = 2, dtMs = dtMs)
            nextState = updateProjectiles(nextState, dtMs)
            nextState = preventOverlap(nextState)
        }

        return nextState
    }

    fun stepOnlineLocal(
        state: ArenaWorldState,
        dtMs: Long,
        localSide: Int,
        localInput: PlayerInput
    ): ArenaWorldState {
        val clampedDt = dtMs.coerceIn(1L, 50L)
        var next = ageEffects(state.copy(soundEvents = emptyList()), clampedDt)
        if (next.phase != MatchPhase.FIGHTING) return next

        val emptyInput = PlayerInput()
        val p1In = if (localSide == 1) localInput else emptyInput
        val p2In = if (localSide == 2) localInput else emptyInput

        val localFighter = if (localSide == 1) next.p1 else next.p2
        val remoteFighter = if (localSide == 1) next.p2 else next.p1

        val wasGrounded = localFighter.y <= 0.001f
        val updatedLocal = updateFighterKinematics(localFighter, if (localSide == 1) p1In else p2In, remoteFighter.x, clampedDt)
        val spawnedSounds = mutableListOf<SoundEffect>()
        if (wasGrounded && updatedLocal.y > 0.001f) spawnedSounds += SoundEffect.JUMP

        val effects = mutableListOf<SlashEffect>()
        val projs = mutableListOf<Projectile>()
        val attackedLocal = tryTriggerAttack(updatedLocal, if (localSide == 1) p1In else p2In, effects, projs, spawnedSounds)

        var flashMs = next.ultiFlashMs
        var flashColor = next.ultiFlashColor
        if (attackedLocal.action == CombatAction.ULTI && attackedLocal.actionTimerMs <= clampedDt) {
            flashMs = 320L
            flashColor = attackedLocal.def.primaryColor
        }

        next = next.copy(
            p1 = if (localSide == 1) attackedLocal else next.p1,
            p2 = if (localSide == 2) attackedLocal else next.p2,
            ultiFlashMs = flashMs,
            ultiFlashColor = flashColor,
            slashEffects = next.slashEffects + effects,
            projectiles = next.projectiles + projs,
            soundEvents = next.soundEvents + spawnedSounds
        )

        next = resolveAttackHit(next, attackerSide = localSide, dtMs = clampedDt)
        next = updateProjectiles(next, clampedDt)
        return next
    }

    private fun updateFighterKinematics(
        f: FighterRuntime,
        input: PlayerInput,
        opponentX: Float,
        dtMs: Long
    ): FighterRuntime {
        val dtSec = dtMs / 1000f
        val newClassicCd = (f.classicCooldownMs - dtMs).coerceAtLeast(0L)
        val newBaseCd = (f.baseCooldownMs - dtMs).coerceAtLeast(0L)
        val newHitStun = (f.hitStunMs - dtMs).coerceAtLeast(0L)
        val newVineSnared = (f.vineSnaredMs - dtMs).coerceAtLeast(0L)
        val newComboExp = (f.comboExpireMs - dtMs).coerceAtLeast(0L)

        var y = f.y + f.vy * dtSec
        var vy = f.vy - GRAVITY * dtSec
        if (y <= 0f) {
            y = 0f
            vy = 0f
        }

        if (newHitStun > 0L || newVineSnared > 0L) {
            return f.copy(
                y = if (newVineSnared > 0L) 0f else y,
                vy = if (newVineSnared > 0L) 0f else vy,
                classicCooldownMs = newClassicCd,
                baseCooldownMs = newBaseCd,
                hitStunMs = newHitStun,
                vineSnaredMs = newVineSnared,
                comboExpireMs = newComboExp,
                comboCount = if (newComboExp == 0L) 0 else f.comboCount,
                comboDamage = if (newComboExp == 0L) 0 else f.comboDamage,
                isBlocking = false,
                chargingMegaPunch = false,
                megaPunchDelayMs = 0L,
                action = CombatAction.HIT_STUN
            )
        }

        val isAttacking = f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)
        if (isAttacking) {
            val nextTimer = f.actionTimerMs + dtMs
            var nextX = f.x
            val moveSpec = when (f.action) {
                CombatAction.CLASSIC -> f.def.classicMove
                CombatAction.BASE_ABILITY -> f.def.baseAbility
                CombatAction.ULTI -> f.def.ultimateMove
                else -> null
            }
            if (moveSpec != null && (
                    moveSpec.effectType == MoveEffectType.CHOPSTICK_FLURRY ||
                    moveSpec.effectType == MoveEffectType.ULTI_CHOPSTICKS ||
                    moveSpec.effectType == MoveEffectType.SWEEP_KICK ||
                    moveSpec.effectType == MoveEffectType.ULTI_TROPICAL_SLAM
                )
            ) {
                val speedMult = if (moveSpec.effectType == MoveEffectType.SWEEP_KICK) 0.65f else 0.80f
                nextX = (nextX + f.facing * speedMult * dtSec).coerceIn(0.08f, 0.92f)
            }

            if (nextTimer < f.actionDurationMs) {
                return f.copy(
                    x = nextX,
                    y = y,
                    vy = vy,
                    classicCooldownMs = newClassicCd,
                    baseCooldownMs = newBaseCd,
                    hitStunMs = 0L,
                    vineSnaredMs = 0L,
                    comboExpireMs = newComboExp,
                    comboCount = if (newComboExp == 0L) 0 else f.comboCount,
                    comboDamage = if (newComboExp == 0L) 0 else f.comboDamage,
                    actionTimerMs = nextTimer,
                    isBlocking = false
                )
            }
        }

        val facing = if (opponentX >= f.x) 1 else -1
        val blocking = input.blockHeld && y == 0f
        val moveSpeed = if (blocking) f.def.stats.moveSpeed * 0.25f else f.def.stats.moveSpeed
        val nextX = (f.x + input.moveDir * moveSpeed * dtSec).coerceIn(0.08f, 0.92f)

        if (input.jumpPressed && y == 0f && !blocking) {
            vy = f.def.stats.jumpPower
            y = 0.01f
        }

        val nextAction = when {
            blocking -> CombatAction.BLOCK
            y > 0.01f -> CombatAction.JUMP
            abs(input.moveDir) > 0.1f -> CombatAction.WALK
            else -> CombatAction.IDLE
        }

        return f.copy(
            x = nextX,
            y = y,
            vy = vy,
            facing = facing,
            classicCooldownMs = newClassicCd,
            baseCooldownMs = newBaseCd,
            hitStunMs = 0L,
            vineSnaredMs = 0L,
            chargingMegaPunch = false,
            megaPunchDelayMs = 0L,
            comboExpireMs = newComboExp,
            comboCount = if (newComboExp == 0L) 0 else f.comboCount,
            comboDamage = if (newComboExp == 0L) 0 else f.comboDamage,
            isBlocking = blocking,
            action = nextAction,
            actionTimerMs = 0L,
            actionDurationMs = 0L
        )
    }

    private fun tryTriggerAttack(
        f: FighterRuntime,
        input: PlayerInput,
        effects: MutableList<SlashEffect>,
        projectiles: MutableList<Projectile>,
        sounds: MutableList<SoundEffect>
    ): FighterRuntime {
        if (f.hitStunMs > 0L || f.vineSnaredMs > 0L || f.isBlocking) return f
        val alreadyAttacking = f.action in setOf(CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI)
        if (alreadyAttacking) return f

        val chosenMove: MoveSpec? = when {
            input.ultiPressed && f.ultiMeter >= f.def.ultimateMove.ultiCost -> f.def.ultimateMove
            input.baseAbilityPressed && f.baseCooldownMs <= 0L -> f.def.baseAbility
            input.classicPressed && f.classicCooldownMs <= 0L -> f.def.classicMove
            else -> null
        }

        if (chosenMove == null) return f

        val newAction = when (chosenMove.slot) {
            MoveSlot.CLASSIC -> CombatAction.CLASSIC
            MoveSlot.BASE_ABILITY -> CombatAction.BASE_ABILITY
            MoveSlot.ULTI -> CombatAction.ULTI
        }

        sounds += when (chosenMove.effectType) {
            MoveEffectType.BAYONETTA_FIST -> SoundEffect.GLITCH_PORTAL
            MoveEffectType.VINE_WHIP -> SoundEffect.VINE_ERUPT
            MoveEffectType.CHOPSTICK_FLURRY -> SoundEffect.CHOPSTICK_FLURRY
            MoveEffectType.SWEEP_KICK -> SoundEffect.CARNIVAL_SWEEP
            MoveEffectType.MELEE -> SoundEffect.SWING_WHOOSH
            MoveEffectType.ULTI_GLITCH_BARRAGE,
            MoveEffectType.ULTI_VINE_PUNCH,
            MoveEffectType.ULTI_CHOPSTICKS,
            MoveEffectType.ULTI_TROPICAL_SLAM -> SoundEffect.ULTI_CINEMATIC
        }

        val nextUlti = if (chosenMove.slot == MoveSlot.ULTI) 0 else f.ultiMeter
        val nextClassicCd = if (chosenMove.slot == MoveSlot.CLASSIC) chosenMove.cooldownMs else f.classicCooldownMs
        val nextBaseCd = if (chosenMove.slot == MoveSlot.BASE_ABILITY) chosenMove.cooldownMs else f.baseCooldownMs

        val duration = if (chosenMove.effectType == MoveEffectType.ULTI_VINE_PUNCH) {
            540L // S'étend à 1200ms si les vignes touchent !
        } else {
            chosenMove.startupMs + chosenMove.activeMs
        }

        effects += SlashEffect(
            id = nextId(),
            x = f.x,
            y = f.y,
            facing = f.facing,
            color = f.def.primaryColor,
            accentColor = f.def.accentColor,
            effectType = chosenMove.effectType,
            range = chosenMove.range,
            maxAgeMs = duration + 140L
        )

        return f.copy(
            action = newAction,
            actionTimerMs = 0L,
            actionDurationMs = duration,
            actionSeq = f.actionSeq + 1L,
            hasDealtHitForSeq = false,
            chargingMegaPunch = false,
            megaPunchDelayMs = 0L,
            ultiMeter = nextUlti,
            classicCooldownMs = nextClassicCd,
            baseCooldownMs = nextBaseCd
        )
    }

    private fun resolveAttackHit(state: ArenaWorldState, attackerSide: Int, dtMs: Long): ArenaWorldState {
        val attacker = if (attackerSide == 1) state.p1 else state.p2
        val defender = if (attackerSide == 1) state.p2 else state.p1

        val move = when (attacker.action) {
            CombatAction.CLASSIC -> attacker.def.classicMove
            CombatAction.BASE_ABILITY -> attacker.def.baseAbility
            CombatAction.ULTI -> attacker.def.ultimateMove
            else -> null
        } ?: return state

        // Mécanique spéciale Jardin : Vignes au sol -> capture -> charge un énorme poing (75% PV max)
        if (move.effectType == MoveEffectType.ULTI_VINE_PUNCH) {
            if (attacker.chargingMegaPunch) {
                val remDelay = attacker.megaPunchDelayMs - dtMs
                if (remDelay > 0L) {
                    val updatedAtt = attacker.copy(megaPunchDelayMs = remDelay)
                    return state.copy(
                        p1 = if (attackerSide == 1) updatedAtt else defender,
                        p2 = if (attackerSide == 2) updatedAtt else defender
                    )
                }
                // IMPACT DU POING COLOSSAL (75% des PV max de l'adversaire !)
                val threeQuartersDmg = (defender.def.stats.maxHp * 0.75f).toInt()
                val nextHp = (defender.hp - threeQuartersDmg).coerceAtLeast(0)
                val pushDir = if (defender.x >= attacker.x) 1f else -1f
                val pushedX = (defender.x + pushDir * 0.25f).coerceIn(0.08f, 0.92f)

                val uAtt = attacker.copy(
                    chargingMegaPunch = false,
                    megaPunchDelayMs = 0L,
                    hasDealtHitForSeq = true,
                    comboCount = attacker.comboCount + 1,
                    comboDamage = attacker.comboDamage + threeQuartersDmg,
                    comboExpireMs = 1600L,
                    dealtDamageTotal = attacker.dealtDamageTotal + threeQuartersDmg
                )
                val uDef = defender.copy(
                    x = pushedX,
                    hp = nextHp,
                    vineSnaredMs = 0L,
                    hitStunMs = 460L,
                    action = CombatAction.HIT_STUN
                )
                var nextTraining = state.training
                if (state.mode == GameMode.TRAINING && attackerSide == 1) {
                    nextTraining = updateTrainingChallenge(
                        training = nextTraining,
                        moveSlot = MoveSlot.ULTI,
                        wasAirborne = false,
                        comboHits = uAtt.comboCount,
                        comboDmg = uAtt.comboDamage
                    )
                }
                val colossalPopup = buildDamagePopup(
                    id = nextId(),
                    defender = defender,
                    damage = threeQuartersDmg,
                    blocked = false,
                    isUlti = true,
                    isColossal = true
                )
                return state.copy(
                    p1 = if (attackerSide == 1) uAtt else uDef,
                    p2 = if (attackerSide == 2) uAtt else uDef,
                    ultiFlashMs = 360L,
                    ultiFlashColor = Color(0xFFFFC400),
                    screenShakeMs = 420L,
                    impactFlashMs = 110L,
                    hitStopMs = 120L,
                    impactX = defender.x,
                    impactCallout = "COLOSSAL !",
                    soundEvents = state.soundEvents + SoundEffect.ULTI_IMPACT,
                    sparks = state.sparks + HitSpark(
                        id = nextId(),
                        x = defender.x,
                        y = defender.y + 0.14f,
                        color = Color(0xFFFFC400),
                        isUlti = true
                    ),
                    popups = state.popups + colossalPopup,
                    training = nextTraining
                )
            }

            if (attacker.hasDealtHitForSeq || attacker.actionTimerMs < move.startupMs) {
                return state
            }

            // Vignes au sol juste devant Jardin
            val dx = (defender.x - attacker.x) * attacker.facing
            val inFront = dx >= 0.0f && dx <= move.range
            val onGround = defender.y <= 0.15f
            if (!inFront || !onGround) return state

            // Les vignes ont touché ! Immobilisation + phase de charge du poing colossal
            val uAtt = attacker.copy(
                hasDealtHitForSeq = true,
                chargingMegaPunch = true,
                megaPunchDelayMs = 680L,
                actionDurationMs = attacker.actionTimerMs + 850L
            )
            val uDef = defender.copy(
                vineSnaredMs = 760L,
                hitStunMs = 760L,
                isBlocking = false,
                action = CombatAction.HIT_STUN
            )
            return state.copy(
                p1 = if (attackerSide == 1) uAtt else uDef,
                p2 = if (attackerSide == 2) uAtt else uDef,
                soundEvents = state.soundEvents + SoundEffect.VINE_ERUPT,
                popups = state.popups + FloatingPopup(
                    id = nextId(),
                    x = defender.x,
                    y = defender.y + 0.18f * defender.def.stats.heightScale,
                    text = "PIÉGÉ !",
                    subLabel = "VIGNES",
                    color = Color(0xFF00E676),
                    isCritical = true,
                    vy = 0.18f,
                    scaleMultiplier = 1.25f
                )
            )
        }

        if (attacker.hasDealtHitForSeq || attacker.actionTimerMs < move.startupMs) {
            return state
        }

        val dx = (defender.x - attacker.x) * attacker.facing
        val inFront = dx >= -0.05f && dx <= move.range
        val dy = abs(defender.y - attacker.y)
        val yTolerance = when {
            move.effectType == MoveEffectType.SWEEP_KICK -> 0.12f
            attacker.action == CombatAction.ULTI -> 0.36f
            else -> 0.22f
        }

        if (!inFront || dy > yTolerance) return state

        val outcome = applyDamage(
            attacker = attacker.copy(hasDealtHitForSeq = true),
            defender = defender,
            rawDamage = move.damage,
            knockback = move.knockback,
            ultiGain = move.ultiGainOnHit,
            isUlti = move.slot == MoveSlot.ULTI,
            hitX = (attacker.x + defender.x) * 0.5f,
            hitY = defender.y + 0.12f
        )

        var nextTraining = state.training
        if (state.mode == GameMode.TRAINING && attackerSide == 1) {
            nextTraining = updateTrainingChallenge(
                training = nextTraining,
                moveSlot = move.slot,
                wasAirborne = attacker.y > 0.03f,
                comboHits = outcome.attacker.comboCount,
                comboDmg = outcome.attacker.comboDamage
            )
        }

        val isHeavyOrUlti = move.slot == MoveSlot.ULTI || move.damage >= 115
        val nextShake = when {
            move.slot == MoveSlot.ULTI -> 320L
            move.damage >= 115 -> 160L
            else -> state.screenShakeMs
        }
        val nextImpactFlash = when {
            move.slot == MoveSlot.ULTI -> 95L
            move.damage >= 115 -> 55L
            else -> state.impactFlashMs
        }
        val nextHitStop = when {
            move.slot == MoveSlot.ULTI -> 105L
            move.damage >= 115 -> 70L
            else -> 40L
        }
        val callout = when {
            defender.isBlocking -> "GARDE"
            move.slot == MoveSlot.ULTI -> "ULTIME !"
            move.damage >= 115 -> "SLASH !"
            else -> null
        }

        return state.copy(
            p1 = if (attackerSide == 1) outcome.attacker else outcome.defender,
            p2 = if (attackerSide == 2) outcome.attacker else outcome.defender,
            screenShakeMs = nextShake,
            impactFlashMs = nextImpactFlash,
            hitStopMs = nextHitStop,
            impactX = defender.x,
            impactCallout = callout ?: state.impactCallout,
            soundEvents = state.soundEvents + outcome.sound,
            sparks = state.sparks + outcome.spark,
            popups = state.popups + outcome.popup,
            training = nextTraining
        )
    }

    private fun updateProjectiles(state: ArenaWorldState, dtMs: Long): ArenaWorldState {
        val dtSec = dtMs / 1000f
        val remaining = mutableListOf<Projectile>()
        var p1 = state.p1
        var p2 = state.p2
        val newSparks = mutableListOf<HitSpark>()
        val newPopups = mutableListOf<FloatingPopup>()
        var nextTraining = state.training

        val newSounds = mutableListOf<SoundEffect>()

        for (proj in state.projectiles) {
            val nextX = proj.x + proj.vx * dtSec
            val nextTtl = proj.ttlMs - dtMs
            if (nextX < 0f || nextX > 1f || nextTtl <= 0L) continue

            val target = if (proj.ownerSide == 1) p2 else p1
            val attacker = if (proj.ownerSide == 1) p1 else p2
            val hitX = abs(nextX - target.x) <= 0.065f
            val hitY = abs(proj.y - (target.y + 0.09f)) <= 0.13f

            if (hitX && hitY) {
                val outcome = applyDamage(
                    attacker = attacker,
                    defender = target,
                    rawDamage = proj.damage,
                    knockback = proj.knockback,
                    ultiGain = proj.ultiGain,
                    isUlti = false,
                    hitX = nextX,
                    hitY = proj.y
                )
                if (proj.ownerSide == 1) {
                    p1 = outcome.attacker
                    p2 = outcome.defender
                    if (state.mode == GameMode.TRAINING) {
                        nextTraining = updateTrainingChallenge(
                            training = nextTraining,
                            moveSlot = MoveSlot.BASE_ABILITY,
                            wasAirborne = false,
                            comboHits = outcome.attacker.comboCount,
                            comboDmg = outcome.attacker.comboDamage
                        )
                    }
                } else {
                    p2 = outcome.attacker
                    p1 = outcome.defender
                }
                newSparks += outcome.spark
                newPopups += outcome.popup
                newSounds += outcome.sound
            } else {
                remaining += proj.copy(x = nextX, ttlMs = nextTtl)
            }
        }

        return state.copy(
            p1 = p1,
            p2 = p2,
            projectiles = remaining,
            sparks = state.sparks + newSparks,
            popups = state.popups + newPopups,
            soundEvents = state.soundEvents + newSounds,
            training = nextTraining
        )
    }

    private data class HitOutcome(
        val attacker: FighterRuntime,
        val defender: FighterRuntime,
        val popup: FloatingPopup,
        val spark: HitSpark,
        val sound: SoundEffect
    )

    private fun applyDamage(
        attacker: FighterRuntime,
        defender: FighterRuntime,
        rawDamage: Int,
        knockback: Float,
        ultiGain: Int,
        isUlti: Boolean,
        hitX: Float,
        hitY: Float
    ): HitOutcome {
        val blocked = defender.isBlocking
        val defenseFactor = 1f - (defender.def.stats.defensePercent / 100f)
        val blockMultiplier = when {
            !blocked -> 1f
            isUlti -> 0.55f
            else -> 0.25f
        }
        val finalDmg = (rawDamage * defenseFactor * blockMultiplier).toInt().coerceAtLeast(5)
        val nextHp = (defender.hp - finalDmg).coerceAtLeast(0)

        val pushDir = if (defender.x >= attacker.x) 1f else -1f
        val actualKnockback = if (blocked) knockback * 0.45f else knockback
        val nextDefX = (defender.x + pushDir * actualKnockback).coerceIn(0.08f, 0.92f)
        val nextHitStun = when {
            blocked && !isUlti -> 0L
            isUlti -> 380L
            else -> 250L
        }

        val nextComboCount = if (!blocked) attacker.comboCount + 1 else attacker.comboCount
        val nextComboDmg = if (!blocked) attacker.comboDamage + finalDmg else attacker.comboDamage
        val nextUltiAttacker = (attacker.ultiMeter + if (blocked) ultiGain / 2 else ultiGain).coerceIn(0, 100)
        val nextUltiDefender = (defender.ultiMeter + 10).coerceIn(0, 100)

        val popup = buildDamagePopup(
            id = nextId(),
            defender = defender,
            damage = finalDmg,
            blocked = blocked,
            isUlti = isUlti
        )

        val hitSound = when {
            blocked && !isUlti -> SoundEffect.BLOCK_CLANG
            isUlti || finalDmg >= 220 -> SoundEffect.ULTI_IMPACT
            finalDmg >= 120 -> SoundEffect.HEAVY_HIT
            else -> SoundEffect.CLASSIC_HIT
        }

        return HitOutcome(
            attacker = attacker.copy(
                ultiMeter = nextUltiAttacker,
                comboCount = nextComboCount,
                comboDamage = nextComboDmg,
                comboExpireMs = if (!blocked) 1600L else attacker.comboExpireMs,
                dealtDamageTotal = attacker.dealtDamageTotal + finalDmg
            ),
            defender = defender.copy(
                x = nextDefX,
                hp = nextHp,
                ultiMeter = nextUltiDefender,
                hitStunMs = nextHitStun,
                action = if (nextHitStun > 0L) CombatAction.HIT_STUN else defender.action
            ),
            popup = popup,
            spark = HitSpark(
                id = nextId(),
                x = hitX,
                y = hitY,
                color = if (blocked) Color(0xFF40C4FF) else popup.color,
                isUlti = isUlti
            ),
            sound = hitSound
        )
    }

    private fun buildDamagePopup(
        id: Long,
        defender: FighterRuntime,
        damage: Int,
        blocked: Boolean,
        isUlti: Boolean,
        isColossal: Boolean = false
    ): FloatingPopup {
        val style = classifyDamageVisual(damage = damage, blocked = blocked, isUlti = isUlti, isColossal = isColossal)
        val spreadSlot = ((id % 3L) - 1L).toFloat() // -1, 0, +1 so rapid hits don't stack directly
        return FloatingPopup(
            id = id,
            x = (defender.x + spreadSlot * 0.024f).coerceIn(0.08f, 0.92f),
            y = defender.y + 0.16f * defender.def.stats.heightScale + 0.03f,
            text = "-$damage",
            color = style.color,
            isCritical = style.isCritical,
            vx = spreadSlot * 0.045f,
            vy = if (style.isCritical) 0.28f else 0.22f,
            scaleMultiplier = style.scale,
            subLabel = style.subLabel,
            maxAgeMs = if (isUlti || damage >= 220) 1100L else if (damage >= 120) 940L else 840L
        )
    }

    private fun updateTrainingChallenge(
        training: TrainingHudState,
        moveSlot: MoveSlot,
        wasAirborne: Boolean,
        comboHits: Int,
        comboDmg: Int
    ): TrainingHudState {
        val challenge = training.activeChallenge ?: return training.copy(
            lastComboHits = comboHits,
            lastComboDamage = comboDmg
        )
        val expected = challenge.steps.getOrNull(training.currentStepIndex)
        val matches = when (expected) {
            ComboInputStep.CLASSIC -> moveSlot == MoveSlot.CLASSIC
            ComboInputStep.JUMP_CLASSIC -> moveSlot == MoveSlot.CLASSIC && wasAirborne
            ComboInputStep.BASE_ABILITY -> moveSlot == MoveSlot.BASE_ABILITY
            ComboInputStep.ULTI -> moveSlot == MoveSlot.ULTI
            null -> false
        }

        val nextStep = if (matches) {
            training.currentStepIndex + 1
        } else {
            val first = challenge.steps.firstOrNull()
            val matchesFirst = when (first) {
                ComboInputStep.CLASSIC -> moveSlot == MoveSlot.CLASSIC
                ComboInputStep.JUMP_CLASSIC -> moveSlot == MoveSlot.CLASSIC && wasAirborne
                ComboInputStep.BASE_ABILITY -> moveSlot == MoveSlot.BASE_ABILITY
                ComboInputStep.ULTI -> moveSlot == MoveSlot.ULTI
                null -> false
            }
            if (matchesFirst) 1 else 0
        }

        return if (nextStep >= challenge.steps.size) {
            training.copy(
                currentStepIndex = 0,
                challengeSuccessBannerMs = 1500L,
                completedChallengesCount = training.completedChallengesCount + 1,
                lastComboHits = comboHits,
                lastComboDamage = comboDmg
            )
        } else {
            training.copy(
                currentStepIndex = nextStep,
                lastComboHits = comboHits,
                lastComboDamage = comboDmg
            )
        }
    }

    private fun preventOverlap(state: ArenaWorldState): ArenaWorldState {
        val dist = abs(state.p1.x - state.p2.x)
        val minSep = 0.09f
        if (dist < minSep && abs(state.p1.y - state.p2.y) < 0.14f) {
            val push = (minSep - dist) * 0.5f
            val dir = if (state.p1.x <= state.p2.x) -1f else 1f
            return state.copy(
                p1 = state.p1.copy(x = (state.p1.x + dir * push).coerceIn(0.08f, 0.92f)),
                p2 = state.p2.copy(x = (state.p2.x - dir * push).coerceIn(0.08f, 0.92f))
            )
        }
        return state
    }

    private fun ageEffects(state: ArenaWorldState, dtMs: Long): ArenaWorldState {
        val dtSec = dtMs / 1000f
        val nextImpactFlash = (state.impactFlashMs - dtMs).coerceAtLeast(0L)
        val nextHitStop = (state.hitStopMs - dtMs).coerceAtLeast(0L)
        return state.copy(
            ultiFlashMs = (state.ultiFlashMs - dtMs).coerceAtLeast(0L),
            screenShakeMs = (state.screenShakeMs - dtMs).coerceAtLeast(0L),
            impactFlashMs = nextImpactFlash,
            hitStopMs = nextHitStop,
            impactCallout = if (nextHitStop > 0L || nextImpactFlash > 0L) state.impactCallout else null,
            sparks = state.sparks.mapNotNull {
                val age = it.ageMs + dtMs
                if (age >= it.maxAgeMs) null else it.copy(ageMs = age)
            },
            slashEffects = state.slashEffects.mapNotNull {
                val age = it.ageMs + dtMs
                if (age >= it.maxAgeMs) null else it.copy(ageMs = age)
            },
            popups = state.popups.mapNotNull {
                val age = it.ageMs + dtMs
                if (age >= it.maxAgeMs) {
                    null
                } else {
                    val p = age.toFloat() / it.maxAgeMs.toFloat()
                    val decel = (1f - p * 0.72f).coerceAtLeast(0.15f)
                    it.copy(
                        ageMs = age,
                        x = (it.x + it.vx * dtSec).coerceIn(0.06f, 0.94f),
                        y = it.y + it.vy * decel * dtSec
                    )
                }
            }
        )
    }

    private fun computeBotInput(
        state: ArenaWorldState,
        dtMs: Long,
        difficulty: BotDifficulty
    ): PlayerInput {
        botDecisionAccumMs += dtMs
        if (botDecisionAccumMs < difficulty.reactionIntervalMs) {
            return currentBotInput
        }
        botDecisionAccumMs = 0L

        val bot = state.p2
        val player = state.p1
        val dist = abs(player.x - bot.x)
        val dirToPlayer = if (player.x > bot.x) 1f else -1f

        val playerAttacking = player.action in setOf(
            CombatAction.CLASSIC, CombatAction.BASE_ABILITY, CombatAction.ULTI
        )

        if (playerAttacking && dist < 0.30f && Random.nextFloat() < difficulty.blockChance) {
            currentBotInput = PlayerInput(blockHeld = true)
            return currentBotInput
        }

        if (bot.ultiMeter >= 100 && dist <= bot.def.ultimateMove.range + 0.04f && Random.nextFloat() < difficulty.aggression) {
            currentBotInput = PlayerInput(ultiPressed = true)
            return currentBotInput
        }

        if (bot.baseCooldownMs <= 0L && dist <= bot.def.baseAbility.range && Random.nextFloat() < difficulty.aggression * 0.65f) {
            currentBotInput = PlayerInput(baseAbilityPressed = true)
            return currentBotInput
        }

        if (dist <= bot.def.classicMove.range && bot.classicCooldownMs <= 0L && Random.nextFloat() < difficulty.aggression) {
            currentBotInput = PlayerInput(classicPressed = true)
            return currentBotInput
        }

        val moveDir = when {
            dist > 0.16f -> dirToPlayer
            dist < 0.09f && Random.nextFloat() < 0.4f -> -dirToPlayer
            else -> 0f
        }
        currentBotInput = PlayerInput(moveDir = moveDir)
        return currentBotInput
    }

    private fun nextId(): Long = nextEntityId++

    data class DamagePopupVisual(
        val color: Color,
        val scale: Float,
        val isCritical: Boolean,
        val subLabel: String?
    )

    companion object {
        private const val GRAVITY = 4.6f

        /**
         * Adapte la couleur, l'échelle et le badge des dégâts flottants selon la puissance du coup.
         */
        fun classifyDamageVisual(
            damage: Int,
            blocked: Boolean = false,
            isUlti: Boolean = false,
            isColossal: Boolean = false
        ): DamagePopupVisual {
            return when {
                blocked && isUlti -> DamagePopupVisual(
                    color = Color(0xFFFFAB40),
                    scale = 1.24f,
                    isCritical = true,
                    subLabel = "PERCÉE"
                )
                blocked -> DamagePopupVisual(
                    color = Color(0xFF40C4FF),
                    scale = 0.92f,
                    isCritical = false,
                    subLabel = "BLOQUÉ"
                )
                isColossal -> DamagePopupVisual(
                    color = Color(0xFFFFC400),
                    scale = 1.72f,
                    isCritical = true,
                    subLabel = "COLOSSAL !"
                )
                isUlti || damage >= 220 -> DamagePopupVisual(
                    color = Color(0xFFFF1744),
                    scale = 1.62f,
                    isCritical = true,
                    subLabel = "ULTIME !"
                )
                damage >= 120 -> DamagePopupVisual(
                    color = Color(0xFFFF6D00),
                    scale = 1.38f,
                    isCritical = true,
                    subLabel = "PUISSANT !"
                )
                damage >= 75 -> DamagePopupVisual(
                    color = Color(0xFFFFD600),
                    scale = 1.18f,
                    isCritical = false,
                    subLabel = null
                )
                else -> DamagePopupVisual(
                    color = Color(0xFFF5FAFF),
                    scale = 1.0f,
                    isCritical = false,
                    subLabel = null
                )
            }
        }
    }
}
