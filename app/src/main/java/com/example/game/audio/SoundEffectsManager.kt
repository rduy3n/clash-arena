package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

enum class SoundEffect {
    UI_CLICK,
    JUMP,
    SWING_WHOOSH,
    CLASSIC_HIT,
    HEAVY_HIT,
    BLOCK_CLANG,
    GLITCH_PORTAL,
    VINE_ERUPT,
    CHOPSTICK_FLURRY,
    CARNIVAL_SWEEP,
    ULTI_CINEMATIC,
    ULTI_IMPACT,
    ROUND_START,
    KO_BOOM,
    VICTORY_FANFARE
}

interface SoundPlayer {
    val isMuted: StateFlow<Boolean>
    fun toggleMute()
    fun play(effect: SoundEffect, pitchVariation: Float = 1f)
}

object NoOpSoundPlayer : SoundPlayer {
    private val _muted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _muted.asStateFlow()
    override fun toggleMute() {
        _muted.value = !_muted.value
    }
    override fun play(effect: SoundEffect, pitchVariation: Float) = Unit
}

val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer> { NoOpSoundPlayer }

/**
 * Moteur d'effets sonores arcade 16-bit haute fidélité synthétisés en PCM 44.1 kHz
 * avec filtrage biquad, compression douce et réverbération de salle via SoundPool.
 */
class AndroidSoundEffectsManager(context: Context) : SoundPlayer {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isMuted = MutableStateFlow(false)
    override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val soundIds = ConcurrentHashMap<SoundEffect, Int>()
    private val loadedIds = ConcurrentHashMap<Int, Boolean>()

    private val soundPool: SoundPool? = runCatching {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()
            .apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedIds[sampleId] = true
                    }
                }
            }
    }.getOrNull()

    init {
        if (soundPool != null) {
            scope.launch {
                runCatching {
                    val sfxDir = File(appContext.cacheDir, "clash_sfx_v2").apply { mkdirs() }
                    SoundEffect.entries.forEach { effect ->
                        val wavFile = File(sfxDir, "${effect.name.lowercase()}.wav")
                        if (!wavFile.exists() || wavFile.length() < 64L) {
                            val samples = synthesize(effect)
                            writeWavFile(wavFile, samples, SAMPLE_RATE)
                        }
                        val id = soundPool.load(wavFile.absolutePath, 1)
                        soundIds[effect] = id
                    }
                }
            }
        }
    }

    override fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    override fun play(effect: SoundEffect, pitchVariation: Float) {
        if (_isMuted.value) return
        val pool = soundPool ?: return
        val sampleId = soundIds[effect] ?: return
        if (loadedIds[sampleId] != true) return

        val volume = when (effect) {
            SoundEffect.UI_CLICK -> 0.45f
            SoundEffect.JUMP -> 0.56f
            SoundEffect.SWING_WHOOSH -> 0.58f
            SoundEffect.BLOCK_CLANG -> 0.74f
            SoundEffect.CLASSIC_HIT -> 0.86f
            SoundEffect.HEAVY_HIT -> 0.94f
            SoundEffect.GLITCH_PORTAL,
            SoundEffect.VINE_ERUPT,
            SoundEffect.CHOPSTICK_FLURRY,
            SoundEffect.CARNIVAL_SWEEP -> 0.88f
            SoundEffect.ULTI_CINEMATIC,
            SoundEffect.ULTI_IMPACT,
            SoundEffect.KO_BOOM -> 0.98f
            SoundEffect.ROUND_START,
            SoundEffect.VICTORY_FANFARE -> 0.88f
        }
        val safeRate = pitchVariation.coerceIn(0.84f, 1.22f)
        runCatching {
            pool.play(sampleId, volume, volume, 1, 0, safeRate)
        }
    }

    fun release() {
        scope.cancel()
        runCatching { soundPool?.release() }
    }

    private fun synthesize(effect: SoundEffect): ShortArray {
        val raw = when (effect) {
            SoundEffect.UI_CLICK -> synthUiClick()
            SoundEffect.JUMP -> synthJump()
            SoundEffect.SWING_WHOOSH -> synthWhoosh()
            SoundEffect.CLASSIC_HIT -> synthClassicHit()
            SoundEffect.HEAVY_HIT -> synthHeavyHit()
            SoundEffect.BLOCK_CLANG -> synthBlockClang()
            SoundEffect.GLITCH_PORTAL -> synthGlitchPortal()
            SoundEffect.VINE_ERUPT -> synthVineErupt()
            SoundEffect.CHOPSTICK_FLURRY -> synthChopstickFlurry()
            SoundEffect.CARNIVAL_SWEEP -> synthCarnivalSweep()
            SoundEffect.ULTI_CINEMATIC -> synthUltiCinematic()
            SoundEffect.ULTI_IMPACT -> synthUltiImpact()
            SoundEffect.ROUND_START -> synthRoundStart()
            SoundEffect.KO_BOOM -> synthKoBoom()
            SoundEffect.VICTORY_FANFARE -> synthVictoryFanfare()
        }
        return normalizeToPcm16(raw)
    }

    private fun synthUiClick(): FloatArray {
        val n = (SAMPLE_RATE * 0.042f).toInt()
        val rng = Rng(101u)
        var lp = 0f
        return FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val env = exp(-t * 115f) * smoothAttack(t, 0.002f)
            val tick = sin(TWO_PI * (960f - t * 6500f) * t) * 0.65f
            val body = sin(TWO_PI * 1920f * t) * exp(-t * 180f) * 0.30f
            lp += 0.35f * (rng.nextFloat() - lp)
            (tick + body + lp * exp(-t * 210f) * 0.35f) * env
        }
    }

    private fun synthJump(): FloatArray {
        val n = (SAMPLE_RATE * 0.14f).toInt()
        var phase = 0f
        var airLp = 0f
        val rng = Rng(202u)
        return FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val p = i / n.toFloat()
            val freq = 185f + 510f * p.pow(1.45f)
            phase += TWO_PI * freq / SAMPLE_RATE
            airLp += 0.12f * (rng.nextFloat() - airLp)
            val env = sin(p * PI_F).pow(0.75f) * smoothAttack(t, 0.008f)
            val cloth = airLp * sin(p * PI_F) * 0.35f
            ((sin(phase) * 0.65f + sin(phase * 2f) * 0.16f) * env + cloth)
        }
    }

    private fun synthWhoosh(): FloatArray {
        val n = (SAMPLE_RATE * 0.145f).toInt()
        val rng = Rng(1337u)
        var lp1 = 0f
        var lp2 = 0f
        var phase = 0f
        val raw = FloatArray(n) { i ->
            val p = i / n.toFloat()
            val noise = rng.nextFloat()
            // Swept resonant bandpass approximation using cascaded LP minus slower LP
            val cFast = 0.08f + 0.34f * sin(p * PI_F)
            val cSlow = 0.025f + 0.08f * sin(p * PI_F)
            lp1 += cFast * (noise - lp1)
            lp2 += cSlow * (noise - lp2)
            val band = (lp1 - lp2) * 1.9f
            val subFreq = 160f + 220f * sin(p * PI_F)
            phase += TWO_PI * subFreq / SAMPLE_RATE
            val env = sin(p * PI_F).pow(0.85f)
            (band + sin(phase) * 0.18f) * env
        }
        return applyRoomReverb(raw, mix = 0.10f)
    }

    private fun synthClassicHit(): FloatArray {
        val n = (SAMPLE_RATE * 0.16f).toInt()
        var phase = 0f
        val rng = Rng(4242u)
        var smackLp = 0f
        var prevNoise = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val freq = 185f * exp(-t * 22f) + 52f
            phase += TWO_PI * freq / SAMPLE_RATE
            val noise = rng.nextFloat()
            val hpClick = (noise - prevNoise) * 0.5f
            prevNoise = noise
            smackLp += 0.34f * (noise - smackLp)
            val punchBody = (sin(phase) * 0.85f + sin(phase * 1.98f) * 0.28f) * exp(-t * 24f)
            val leatherSmack = smackLp * exp(-t * 48f) * 0.85f
            val transient = hpClick * exp(-t * 140f) * 0.55f
            tanh((punchBody + leatherSmack + transient) * 1.35f) * smoothAttack(t, 0.0015f)
        }
        return applyRoomReverb(dry, mix = 0.14f)
    }

    private fun synthHeavyHit(): FloatArray {
        val n = (SAMPLE_RATE * 0.28f).toInt()
        var subPhase = 0f
        var midPhase = 0f
        val rng = Rng(9001u)
        var crunchLp = 0f
        var prevNoise = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val subFreq = 155f * exp(-t * 14f) + 36f
            subPhase += TWO_PI * subFreq / SAMPLE_RATE
            midPhase += TWO_PI * (subFreq * 2.2f) / SAMPLE_RATE
            val noise = rng.nextFloat()
            val crack = (noise - prevNoise) * 0.5f
            prevNoise = noise
            crunchLp += 0.28f * (noise - crunchLp)

            val subBoom = sin(subPhase) * exp(-t * 12.5f) * 0.95f
            val midKnock = sin(midPhase) * exp(-t * 26f) * 0.42f
            val bodyCrunch = crunchLp * exp(-t * 32f) * 0.90f
            val impactCrack = crack * exp(-t * 110f) * 0.65f
            tanh((subBoom + midKnock + bodyCrunch + impactCrack) * 1.55f) * smoothAttack(t, 0.0015f)
        }
        return applyRoomReverb(dry, mix = 0.22f)
    }

    private fun synthBlockClang(): FloatArray {
        val n = (SAMPLE_RATE * 0.18f).toInt()
        val rng = Rng(3141u)
        var thudPhase = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val fThud = 190f * exp(-t * 28f) + 72f
            thudPhase += TWO_PI * fThud / SAMPLE_RATE
            val thud = sin(thudPhase) * exp(-t * 34f) * 0.65f
            // Guard barrier shimmer with damped metallic partials
            val metal = (
                sin(TWO_PI * 620f * t) * 0.42f +
                    sin(TWO_PI * 945f * t) * 0.30f +
                    sin(TWO_PI * 1480f * t) * 0.22f
                ) * exp(-t * 26f)
            val spark = rng.nextFloat() * exp(-t * 95f) * 0.30f
            tanh((thud + metal + spark) * 1.3f) * smoothAttack(t, 0.0015f)
        }
        return applyRoomReverb(dry, mix = 0.18f)
    }

    private fun synthGlitchPortal(): FloatArray {
        val n = (SAMPLE_RATE * 0.30f).toInt()
        val rng = Rng(5150u)
        var carrierPhase = 0f
        var modPhase = 0f
        var subPhase = 0f
        var lp = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val p = i / n.toFloat()
            // Deep cyber sub-drop + smooth FM harmonic warp + crisp micro-glitch crackle
            val subFreq = 165f * exp(-t * 10f) + 46f
            subPhase += TWO_PI * subFreq / SAMPLE_RATE

            val stepNotes = floatArrayOf(440f, 587.33f, 880f, 1174.66f, 659.25f)
            val idx = (p * stepNotes.size).toInt().coerceIn(0, stepNotes.lastIndex)
            val baseFreq = stepNotes[idx] + sin(t * 55f) * 30f
            modPhase += TWO_PI * (baseFreq * 1.5f) / SAMPLE_RATE
            val fm = sin(modPhase) * (1.8f * (1f - p * 0.6f))
            carrierPhase += TWO_PI * baseFreq / SAMPLE_RATE

            val synthTone = sin(carrierPhase + fm) * 0.55f + sin(carrierPhase * 0.5f) * 0.30f
            val noise = rng.nextFloat()
            lp += 0.32f * (noise - lp)
            val crackle = (noise - lp) * exp(-t * 22f) * 0.38f
            val env = (1f - p * 0.72f) * smoothAttack(t, 0.005f)
            tanh((sin(subPhase) * exp(-t * 14f) * 0.65f + synthTone * env + crackle) * 1.25f)
        }
        return applyRoomReverb(dry, mix = 0.24f)
    }

    private fun synthVineErupt(): FloatArray {
        val n = (SAMPLE_RATE * 0.34f).toInt()
        val rng = Rng(777u)
        var earthPhase = 0f
        var lp = 0f
        var prev = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val p = i / n.toFloat()
            val fEarth = 115f * exp(-t * 7f) + 42f
            earthPhase += TWO_PI * fEarth / SAMPLE_RATE

            val noise = rng.nextFloat()
            val highAir = noise - prev
            prev = noise
            lp += 0.14f * (noise - lp)

            val rumble = (sin(earthPhase) * 0.7f + lp * 0.85f) * exp(-t * 7.5f)
            // Twin sharp organic vine whip cracks at 75ms & 155ms
            val whip1 = if (t >= 0.065f) highAir * exp(-(t - 0.065f) * 65f) * 0.95f else 0f
            val whip2 = if (t >= 0.145f) highAir * exp(-(t - 0.145f) * 58f) * 0.85f else 0f
            val env = smoothAttack(t, 0.008f) * (1f - p * 0.35f)
            tanh((rumble + whip1 + whip2) * env * 1.35f)
        }
        return applyRoomReverb(dry, mix = 0.22f)
    }

    private fun synthChopstickFlurry(): FloatArray {
        val n = (SAMPLE_RATE * 0.28f).toInt()
        val hits = floatArrayOf(0.005f, 0.060f, 0.115f, 0.170f)
        val freqs = floatArrayOf(1180f, 1340f, 1520f, 1780f)
        val rng = Rng(2468u)
        var airLp = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val p = i / n.toFloat()
            val noise = rng.nextFloat()
            airLp += 0.22f * (noise - airLp)
            var acc = (noise - airLp) * sin(p * PI_F) * 0.22f
            hits.forEachIndexed { idx, start ->
                val dt = t - start
                if (dt >= 0f) {
                    val f = freqs[idx]
                    // Crisp bamboo wood clack = fundamental + inharmonic wood overtone + sharp transient
                    val woodBody = (
                        sin(TWO_PI * f * dt) * 0.58f +
                            sin(TWO_PI * (f * 1.56f) * dt) * 0.34f +
                            sin(TWO_PI * (f * 0.48f) * dt) * 0.24f
                        ) * exp(-dt * 68f)
                    val snap = noise * exp(-dt * 160f) * 0.35f
                    acc += (woodBody + snap) * smoothAttack(dt, 0.001f)
                }
            }
            tanh(acc * 1.25f)
        }
        return applyRoomReverb(dry, mix = 0.18f)
    }

    private fun synthCarnivalSweep(): FloatArray {
        val n = (SAMPLE_RATE * 0.30f).toInt()
        val rng = Rng(555u)
        var surdoPhase = 0f
        var lp = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val p = i / n.toFloat()
            // Deep Brazilian Surdo bass kick + second syncopated beat at 110ms
            val fSurdo = 135f * exp(-t * 11f) + 46f
            surdoPhase += TWO_PI * fSurdo / SAMPLE_RATE
            val surdo1 = sin(surdoPhase) * exp(-t * 10f) * 0.85f
            val dt2 = t - 0.11f
            val surdo2 = if (dt2 >= 0f) sin(TWO_PI * (78f + 50f * exp(-dt2 * 16f)) * dt2) * exp(-dt2 * 13f) * 0.55f else 0f

            // Ground sweep friction air
            val noise = rng.nextFloat()
            val cutoff = 0.08f + 0.32f * sin(p * PI_F)
            lp += cutoff * (noise - lp)
            val sweepAir = lp * sin(p * PI_F) * 0.78f
            tanh((surdo1 + surdo2 + sweepAir) * 1.3f) * smoothAttack(t, 0.003f)
        }
        return applyRoomReverb(dry, mix = 0.20f)
    }

    private fun synthUltiCinematic(): FloatArray {
        val n = (SAMPLE_RATE * 0.76f).toInt()
        var subPhase = 0f
        var shimmerPhase = 0f
        var fifthPhase = 0f
        val rng = Rng(8080u)
        var airLp = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val p = i / n.toFloat()
            // Anime time-stop bass drop (first 0.35s) + rising crystalline tension chord
            val subFreq = 160f * exp(-t * 5.8f) + 36f
            subPhase += TWO_PI * subFreq / SAMPLE_RATE
            val bassDrop = sin(subPhase) * exp(-t * 3.6f) * 0.92f

            val riseFreq = 320f + 780f * p.pow(1.8f)
            shimmerPhase += TWO_PI * riseFreq / SAMPLE_RATE
            fifthPhase += TWO_PI * (riseFreq * 1.498f) / SAMPLE_RATE
            val chimeEnv = sin(p * PI_F).pow(0.75f)
            val shimmer = (sin(shimmerPhase) * 0.48f + sin(fifthPhase) * 0.32f + sin(shimmerPhase * 2f) * 0.16f) * chimeEnv

            airLp += (0.06f + p * 0.28f) * (rng.nextFloat() - airLp)
            val windSwell = airLp * p.pow(1.4f) * (1f - p * 0.4f) * 0.55f
            tanh((bassDrop + shimmer + windSwell) * 1.25f) * smoothAttack(t, 0.004f)
        }
        return applyRoomReverb(dry, mix = 0.28f)
    }

    private fun synthUltiImpact(): FloatArray {
        val n = (SAMPLE_RATE * 0.58f).toInt()
        var subPhase = 0f
        val rng = Rng(2026u)
        var lp = 0f
        var prev = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val f = 195f * exp(-t * 11f) + 33f
            subPhase += TWO_PI * f / SAMPLE_RATE
            val noise = rng.nextFloat()
            val crack = noise - prev
            prev = noise
            lp += 0.24f * (noise - lp)

            val sub = sin(subPhase) * exp(-t * 5.8f) * 1.0f
            val shockNoise = lp * exp(-t * 10.5f) * 0.90f
            val flashSnap = crack * exp(-t * 75f) * 0.55f
            val ring = (sin(TWO_PI * 420f * t) * 0.22f + sin(TWO_PI * 635f * t) * 0.14f) * exp(-t * 15f)
            tanh((sub + shockNoise + flashSnap + ring) * 1.45f) * smoothAttack(t, 0.0015f)
        }
        return applyRoomReverb(dry, mix = 0.30f)
    }

    private fun synthRoundStart(): FloatArray {
        val n = (SAMPLE_RATE * 0.44f).toInt()
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val env = exp(-t * 6.5f) * smoothAttack(t, 0.005f)
            // Crisp warm brass-bell arcade chord (C5 + G5 + C6)
            val c5 = sin(TWO_PI * 523.25f * t) * 0.48f + sin(TWO_PI * 1046.5f * t) * 0.18f
            val g5 = sin(TWO_PI * 783.99f * t) * 0.36f
            val e5 = sin(TWO_PI * 659.25f * t) * 0.28f
            tanh((c5 + g5 + e5) * env * 1.1f)
        }
        return applyRoomReverb(dry, mix = 0.26f)
    }

    private fun synthKoBoom(): FloatArray {
        val n = (SAMPLE_RATE * 0.68f).toInt()
        var subPhase = 0f
        val rng = Rng(8888u)
        var lp = 0f
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            val freq = 145f * exp(-t * 6.8f) + 31f
            subPhase += TWO_PI * freq / SAMPLE_RATE
            val noise = rng.nextFloat()
            lp += 0.20f * (noise - lp)
            val sub = sin(subPhase) * exp(-t * 4.2f) * 0.95f
            // Resonant stadium gong partials
            val gong = (
                sin(TWO_PI * 196f * t) * 0.42f +
                    sin(TWO_PI * 294f * t) * 0.28f +
                    sin(TWO_PI * 442f * t) * 0.16f
                ) * exp(-t * 3.9f)
            val burst = lp * exp(-t * 14f) * 0.68f
            tanh((sub + gong + burst) * 1.35f) * smoothAttack(t, 0.002f)
        }
        return applyRoomReverb(dry, mix = 0.32f)
    }

    private fun synthVictoryFanfare(): FloatArray {
        val n = (SAMPLE_RATE * 0.88f).toInt()
        val notes = listOf(
            0.00f to 523.25f, // C5
            0.11f to 659.25f, // E5
            0.22f to 783.99f, // G5
            0.34f to 1046.50f // C6
        )
        val dry = FloatArray(n) { i ->
            val t = i / SAMPLE_RATE.toFloat()
            var acc = 0f
            notes.forEachIndexed { idx, (start, freq) ->
                val dt = t - start
                if (dt >= 0f) {
                    val decay = if (idx == notes.lastIndex) 3.8f else 11f
                    val env = exp(-dt * decay) * smoothAttack(dt, 0.006f)
                    // Rich warm brassy harmonic voice + octave pad on final note
                    val voice = sin(TWO_PI * freq * dt) * 0.55f +
                        sin(TWO_PI * freq * 2f * dt) * 0.22f +
                        sin(TWO_PI * freq * 1.5f * dt) * 0.14f
                    acc += voice * env
                }
            }
            // Sustained warm C major bass pad on final hit
            val padDt = t - 0.34f
            if (padDt >= 0f) {
                acc += (sin(TWO_PI * 261.63f * padDt) * 0.32f + sin(TWO_PI * 392.00f * padDt) * 0.22f) *
                    exp(-padDt * 3.5f) * smoothAttack(padDt, 0.01f)
            }
            tanh(acc * 0.92f)
        }
        return applyRoomReverb(dry, mix = 0.30f)
    }

    private fun applyRoomReverb(dry: FloatArray, mix: Float): FloatArray {
        val taps = intArrayOf(
            (SAMPLE_RATE * 0.017f).toInt(),
            (SAMPLE_RATE * 0.029f).toInt(),
            (SAMPLE_RATE * 0.043f).toInt(),
            (SAMPLE_RATE * 0.061f).toInt()
        )
        val gains = floatArrayOf(0.48f, 0.34f, 0.24f, 0.16f)
        val wet = FloatArray(dry.size)
        for (i in dry.indices) {
            var r = 0f
            for (k in taps.indices) {
                val src = i - taps[k]
                if (src >= 0) r += dry[src] * gains[k]
            }
            wet[i] = dry[i] * (1f - mix) + r * mix
        }
        return wet
    }

    private fun smoothAttack(t: Float, attackSec: Float): Float {
        if (attackSec <= 0f) return 1f
        return (t / attackSec).coerceIn(0f, 1f)
    }

    private fun normalizeToPcm16(samples: FloatArray): ShortArray {
        var peak = 0.001f
        for (s in samples) {
            val a = abs(s)
            if (a > peak) peak = a
        }
        val gain = (0.90f / peak).coerceAtMost(1.8f)
        return ShortArray(samples.size) { idx ->
            val v = (samples[idx] * gain).coerceIn(-1f, 1f)
            (v * 32767f).toInt().toShort()
        }
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val dataSize = samples.size * 2
        val byteData = ByteArray(44 + dataSize)
        fun putStr(offset: Int, str: String) {
            str.forEachIndexed { idx, ch -> byteData[offset + idx] = ch.code.toByte() }
        }
        fun putIntLE(offset: Int, value: Int) {
            byteData[offset] = (value and 0xFF).toByte()
            byteData[offset + 1] = ((value shr 8) and 0xFF).toByte()
            byteData[offset + 2] = ((value shr 16) and 0xFF).toByte()
            byteData[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShortLE(offset: Int, value: Int) {
            byteData[offset] = (value and 0xFF).toByte()
            byteData[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        putStr(0, "RIFF")
        putIntLE(4, 36 + dataSize)
        putStr(8, "WAVE")
        putStr(12, "fmt ")
        putIntLE(16, 16)
        putShortLE(20, 1)
        putShortLE(22, 1)
        putIntLE(24, sampleRate)
        putIntLE(28, sampleRate * 2)
        putShortLE(32, 2)
        putShortLE(34, 16)
        putStr(36, "data")
        putIntLE(40, dataSize)

        var pos = 44
        for (sample in samples) {
            val s = sample.toInt()
            byteData[pos++] = (s and 0xFF).toByte()
            byteData[pos++] = ((s shr 8) and 0xFF).toByte()
        }
        FileOutputStream(file).use { it.write(byteData) }
    }

    private class Rng(private var seed: UInt) {
        fun nextFloat(): Float {
            seed = seed * 1664525u + 1013904223u
            return ((seed shr 16).toInt() / 32768f) - 1f
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val PI_F = PI.toFloat()
        private const val TWO_PI = 2f * PI_F
    }
}
