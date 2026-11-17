package com.example.game.engine

import androidx.compose.ui.graphics.Color
import com.example.game.audio.SoundEffect
import com.example.game.data.ArenaStage
import com.example.game.data.BotDifficulty
import com.example.game.data.ComboChallenge
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterVisualStyle
import com.example.game.data.GameMode
import com.example.game.data.MoveEffectType
import com.example.game.data.MoveSlot
import com.example.game.data.MoveSpec
import com.example.game.data.TrainingOpponentMode

enum class DirectionalComboKind(val inputHint: String) {
    LEFT_RIGHT_BASE("← → + CAP"),
    RIGHT_LEFT_CLASSIC("→ ← + C")
}

enum class ProjectileStyle {
    CYBER_SHARD,
    GOLDEN_CHOPSTICK,
    COCONUT_BOMB
}

enum class CombatAction(val wireName: String) {
    IDLE("idle"),
    WALK("walk"),
    JUMP("jump"),
    BLOCK("block"),
    CLASSIC("classic"),
    BASE_ABILITY("base"),
    ULTI("ulti"),
    HIT_STUN("hit"),
    KO("ko");

    companion object {
        fun fromWire(value: String): CombatAction =
            entries.firstOrNull { it.wireName == value } ?: IDLE
    }
}

enum class MatchPhase {
    INTRO_CINEMATIC,
    ROUND_INTRO,
    FIGHTING,
    ROUND_END,
    KO_SLOWMO,
    MATCH_OVER
}

enum class FinishType(val label: String) {
    KO("K.O. !"),
    ULTI_FINISH("ULTI FINISH !"),
    PERFECT("PERFECT !"),
    TIME_UP("TEMPS ÉCOULÉ !")
}

enum class ArcadeRankTier(val letter: String, val subtitle: String, val badgeColor: Color) {
    S("S", "MAÎTRISE LÉGENDAIRE", Color(0xFFFFD600)),
    A("A", "COMBAT BRILLANT", Color(0xFF00E5FF)),
    B("B", "BELLE PERFORMANCE", Color(0xFF00E676)),
    C("C", "COMBAT RUDE", Color(0xFFB0BEC5))
}

data class ArcadeRankSummary(
    val tier: ArcadeRankTier,
    val score: Int
)

object ArcadeRankCalculator {
    fun evaluate(
        won: Boolean,
        roundsWon: Int,
        roundsLost: Int,
        remainingHpRatio: Float,
        maxCombo: Int,
        totalDamage: Int,
        finishType: FinishType
    ): ArcadeRankSummary {
        val rawScore = if (won) {
            val cleanSweepBonus = if (roundsLost == 0) 15 else 4
            val hpBonus = if (remainingHpRatio >= 0.85f) 14 else (remainingHpRatio.coerceIn(0f, 1f) * 12f).toInt()
            val comboBonus = (maxCombo.coerceIn(0, 8) * 3)
            val dmgBonus = (totalDamage / 260).coerceIn(0, 10)
            val finishBonus = when (finishType) {
                FinishType.PERFECT -> 16
                FinishType.ULTI_FINISH -> 11
                FinishType.KO -> 5
                FinishType.TIME_UP -> 0
            }
            (50 + cleanSweepBonus + hpBonus + comboBonus + dmgBonus + finishBonus).coerceIn(45, 100)
        } else {
            val roundsBonus = roundsWon.coerceIn(0, 2) * 12
            val comboBonus = (maxCombo.coerceIn(0, 6) * 3)
            val dmgBonus = (totalDamage / 280).coerceIn(0, 12)
            (18 + roundsBonus + comboBonus + dmgBonus).coerceIn(15, 58)
        }

        val tier = when {
            rawScore >= 85 -> ArcadeRankTier.S
            rawScore >= 65 -> ArcadeRankTier.A
            rawScore >= 42 -> ArcadeRankTier.B
            else -> ArcadeRankTier.C
        }
        return ArcadeRankSummary(tier = tier, score = rawScore)
    }
}

data class PlayerInput(
    val moveDir: Float = 0f,
    val jumpPressed: Boolean = false,
    val blockHeld: Boolean = false,
    val classicPressed: Boolean = false,
    val baseAbilityPressed: Boolean = false,
    val ultiPressed: Boolean = false
)

data class FighterRuntime(
    val side: Int,
    val def: FighterDefinition,
    val x: Float,
    val y: Float = 0f,
    val vy: Float = 0f,
    val facing: Int,
    val hp: Int,
    val ultiMeter: Int = 0,
    val action: CombatAction = CombatAction.IDLE,
    val actionTimerMs: Long = 0L,
    val actionDurationMs: Long = 0L,
    val actionSeq: Long = 0L,
    val hasDealtHitForSeq: Boolean = false,
    val classicCooldownMs: Long = 0L,
    val baseCooldownMs: Long = 0L,
    val hitStunMs: Long = 0L,
    val vineSnaredMs: Long = 0L,      // Immobilisé par les vignes de Jardin
    val glitchJamMs: Long = 0L,      // Perturbé / brouillé par l'onde EMP de Glitcherman (contrôles inversés + glitch)
    val chargingMegaPunch: Boolean = false, // Jardin charge son coup de poing 45% PV
    val megaPunchDelayMs: Long = 0L,
    val isBlocking: Boolean = false,
    val comboCount: Int = 0,
    val comboDamage: Int = 0,
    val comboExpireMs: Long = 0L,
    val dealtDamageTotal: Int = 0,
    val maxComboCount: Int = 0,
    val matchDamageTotal: Int = 0,
    val lastMoveSlot: MoveSlot? = null,
    val secretComboActive: Boolean = false,
    val activeComboMove: MoveSpec? = null,
    val prevMoveDirSign: Int = 0,
    val lastDirTapSign: Int = 0,
    val lastDirTapAgeMs: Long = 0L,
    val primedComboKind: DirectionalComboKind? = null,
    val primedComboTimerMs: Long = 0L
)

data class Projectile(
    val id: Long,
    val ownerSide: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val damage: Int,
    val knockback: Float,
    val ultiGain: Int,
    val color: Color,
    val accentColor: Color,
    val style: ProjectileStyle = ProjectileStyle.CYBER_SHARD,
    val hitSound: SoundEffect = SoundEffect.CLASSIC_HIT,
    val isDirectionalCombo: Boolean = true,
    val ageMs: Long = 0L,
    val ttlMs: Long = 2200L
)

data class HitSpark(
    val id: Long,
    val x: Float,
    val y: Float,
    val color: Color,
    val isUlti: Boolean,
    val ageMs: Long = 0L,
    val maxAgeMs: Long = 360L
)

data class SlashEffect(
    val id: Long,
    val x: Float,
    val y: Float,
    val facing: Int,
    val color: Color,
    val accentColor: Color = Color.White,
    val effectType: MoveEffectType,
    val range: Float,
    val targetX: Float = x,
    val ageMs: Long = 0L,
    val maxAgeMs: Long = 400L
)

data class FatalParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val rotationDeg: Float,
    val vRotDeg: Float,
    val color: Color,
    val isShard: Boolean,
    val ageMs: Long = 0L,
    val maxAgeMs: Long = 1650L
)

data class FloatingPopup(
    val id: Long,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Color,
    val isCritical: Boolean = false,
    val vx: Float = 0f,
    val vy: Float = 0.24f,
    val scaleMultiplier: Float = 1.0f,
    val subLabel: String? = null,
    val ageMs: Long = 0L,
    val maxAgeMs: Long = 860L
)

data class TrainingHudState(
    val opponentMode: TrainingOpponentMode = TrainingOpponentMode.STATIONARY,
    val botDifficulty: BotDifficulty = BotDifficulty.NORMAL,
    val autoRegenHp: Boolean = true,
    val infiniteUlti: Boolean = true,
    val selectedChallengeIndex: Int = 0,
    val activeChallenge: ComboChallenge? = null,
    val currentStepIndex: Int = 0,
    val challengeSuccessBannerMs: Long = 0L,
    val completedChallengesCount: Int = 0,
    val lastComboHits: Int = 0,
    val lastComboDamage: Int = 0
)

data class UltiCinematicState(
    val casterSide: Int,
    val fighterId: String,
    val moveName: String,
    val remainingMs: Long,
    val totalMs: Long = 2000L,
    val visualStyle: FighterVisualStyle,
    val primaryColor: Color,
    val accentColor: Color
)

data class ArenaWorldState(
    val mode: GameMode,
    val stage: ArenaStage,
    val phase: MatchPhase = MatchPhase.INTRO_CINEMATIC,
    val phaseTimerMs: Long = 2500L,
    val introDurationMs: Long = 2500L,
    val roundNumber: Int = 1,
    val roundTimeRemainingSec: Int = 99,
    val roundClockAccumMs: Long = 0L,
    val p1RoundsWon: Int = 0,
    val p2RoundsWon: Int = 0,
    val winnerSide: Int = 0,
    val finishType: FinishType = FinishType.KO,
    val koFreezeRemainingMs: Long = 0L,
    val koFreezeTotalMs: Long = 420L,
    val koSlowMoTotalMs: Long = 1300L,
    val bannerText: String = "",
    val ultiFlashMs: Long = 0L,
    val ultiFlashColor: Color = Color.Yellow,
    val screenShakeMs: Long = 0L,
    val impactFlashMs: Long = 0L,
    val hitStopMs: Long = 0L,
    val impactX: Float = 0.5f,
    val impactCallout: String? = null,
    val ultiCinematic: UltiCinematicState? = null,
    val p1: FighterRuntime,
    val p2: FighterRuntime,
    val projectiles: List<Projectile> = emptyList(),
    val sparks: List<HitSpark> = emptyList(),
    val slashEffects: List<SlashEffect> = emptyList(),
    val popups: List<FloatingPopup> = emptyList(),
    val fatalParticles: List<FatalParticle> = emptyList(),
    val soundEvents: List<SoundEffect> = emptyList(),
    val training: TrainingHudState = TrainingHudState()
)
