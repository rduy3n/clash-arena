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
import com.example.game.data.TrainingOpponentMode

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
    ROUND_INTRO,
    FIGHTING,
    ROUND_END,
    MATCH_OVER
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
    val chargingMegaPunch: Boolean = false, // Jardin charge son coup de poing 75% PV
    val megaPunchDelayMs: Long = 0L,
    val isBlocking: Boolean = false,
    val comboCount: Int = 0,
    val comboDamage: Int = 0,
    val comboExpireMs: Long = 0L,
    val dealtDamageTotal: Int = 0
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
    val ageMs: Long = 0L,
    val maxAgeMs: Long = 400L
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
    val phase: MatchPhase = MatchPhase.ROUND_INTRO,
    val phaseTimerMs: Long = 1600L,
    val roundNumber: Int = 1,
    val roundTimeRemainingSec: Int = 99,
    val roundClockAccumMs: Long = 0L,
    val p1RoundsWon: Int = 0,
    val p2RoundsWon: Int = 0,
    val winnerSide: Int = 0,
    val bannerText: String = "ROUND 1",
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
    val soundEvents: List<SoundEffect> = emptyList(),
    val training: TrainingHudState = TrainingHudState()
)
