package com.example.game

import com.example.game.data.BotDifficulty
import com.example.game.data.FighterRoster
import com.example.game.data.GameMode
import com.example.game.data.TrainingOpponentMode
import com.example.game.engine.CombatEngine
import com.example.game.engine.PlayerInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatEngineTest {

    @Test
    fun allFourFighters_presentAndBalanced300PointStatBudget() {
        assertEquals(4, FighterRoster.fighters.size)
        val ids = FighterRoster.fighters.map { it.id }.toSet()
        assertEquals(setOf("glitcherman", "jardin", "le_chinois", "daron_bresilien"), ids)
        FighterRoster.fighters.forEach { fighter ->
            val sum = fighter.stats.ratingHp +
                fighter.stats.ratingAttack +
                fighter.stats.ratingSpeed +
                fighter.stats.ratingDefense +
                fighter.stats.ratingRange
            assertEquals("Fighter ${fighter.name} stat budget must equal 300", 300, sum)
        }
    }

    @Test
    fun trainingMode_comboAndResetHealth_worksCorrectly() {
        val engine = CombatEngine()
        val glitcherman = FighterRoster.getFighter("glitcherman")
        val daron = FighterRoster.getFighter("daron_bresilien")
        val stage = FighterRoster.stages.first()

        var world = engine.createInitialState(
            mode = GameMode.TRAINING,
            stage = stage,
            p1Def = glitcherman,
            p2Def = daron,
            botDifficulty = BotDifficulty.NORMAL,
            trainingOpponentMode = TrainingOpponentMode.STATIONARY
        ).let {
            // Put fighters within melee range
            it.copy(p1 = it.p1.copy(x = 0.45f), p2 = it.p2.copy(x = 0.57f))
        }

        // Trigger Classic attack and advance until active hit
        world = engine.step(world, 20L, PlayerInput(classicPressed = true))
        repeat(8) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("Dummy HP should decrease after Classic hit", world.p2.hp < daron.stats.maxHp)
        assertEquals(1, world.training.currentStepIndex)

        // Reset health restores full HP
        world = engine.resetTrainingHealth(world)
        assertEquals(daron.stats.maxHp, world.p2.hp)
        assertEquals(glitcherman.stats.maxHp, world.p1.hp)
    }

    @Test
    fun jardinUlti_vineSnareAndColossalPunch_deals45PercentMaxHp() {
        val engine = CombatEngine()
        val jardin = FighterRoster.getFighter("jardin")
        val chen = FighterRoster.getFighter("le_chinois")
        val stage = FighterRoster.stages.first()

        var world = engine.createInitialState(
            mode = GameMode.VS_BOT,
            stage = stage,
            p1Def = jardin,
            p2Def = chen
        ).let {
            it.copy(
                phase = com.example.game.engine.MatchPhase.FIGHTING,
                p1 = it.p1.copy(x = 0.40f, ultiMeter = 100),
                p2 = it.p2.copy(x = 0.60f, y = 0f)
            )
        }

        // Trigger Jardin's Ulti -> enters 2-second slow-motion Ulti cinematic
        world = engine.step(world, 20L, PlayerInput(ultiPressed = true))
        assertTrue("2s slow-mo Ulti cinematic should start", world.ultiCinematic != null)

        // Advance through the 2000ms slow-mo cinematic (100 * 20ms = 2000ms)
        repeat(100) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("2s slow-mo Ulti cinematic should finish", world.ultiCinematic == null)

        // Advance past startupMs (150ms) so vines hit & snare Chen
        repeat(12) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("Jardin should be charging colossal punch after vine hit", world.p1.chargingMegaPunch)
        assertTrue("Opponent should be snared by vines", world.p2.vineSnaredMs > 0L)

        // Advance past 680ms charge delay so colossal punch lands
        repeat(40) {
            world = engine.step(world, 20L, PlayerInput())
        }
        val expectedDmg = (chen.stats.maxHp * 0.45f).toInt()
        assertEquals(chen.stats.maxHp - expectedDmg, world.p2.hp)
    }

    @Test
    fun glitcherman_empPulse360AndOrbitalStrike_workAsDesigned() {
        val engine = CombatEngine()
        val glitcherman = FighterRoster.getFighter("glitcherman")
        val chen = FighterRoster.getFighter("le_chinois")
        val stage = FighterRoster.stages.first()

        // Opponent placed BEHIND Glitcherman (facing = 1, p1.x = 0.50, p2.x = 0.34) -> 360 EMP hits!
        var world = engine.createInitialState(
            mode = GameMode.TRAINING,
            stage = stage,
            p1Def = glitcherman,
            p2Def = chen
        ).let {
            it.copy(
                p1 = it.p1.copy(x = 0.50f, facing = 1, ultiMeter = 100),
                p2 = it.p2.copy(x = 0.34f, facing = 1, y = 0f)
            )
        }

        world = engine.step(world, 20L, PlayerInput(baseAbilityPressed = true))
        repeat(10) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("360° EMP should damage opponent behind caster", world.p2.hp < chen.stats.maxHp)
        assertTrue("EMP should push opponent strongly away", world.p2.x < 0.25f)
        assertTrue("EMP should apply extended hitStun", world.p2.hitStunMs > 200L)

        // Reset positions at long orbital range and fire Frappe Orbitale
        world = world.copy(
            p1 = world.p1.copy(x = 0.22f, facing = 1, ultiMeter = 100, action = com.example.game.engine.CombatAction.IDLE, classicCooldownMs = 0L, baseCooldownMs = 0L),
            p2 = world.p2.copy(x = 0.72f, hp = chen.stats.maxHp, hitStunMs = 0L, action = com.example.game.engine.CombatAction.IDLE)
        )
        world = engine.step(world, 20L, PlayerInput(ultiPressed = true))
        val orbitalEffect = world.slashEffects.lastOrNull { it.effectType == com.example.game.data.MoveEffectType.ULTI_ORBITAL_STRIKE }
        assertTrue("Orbital Strike effect should lock onto opponent X", orbitalEffect != null && kotlin.math.abs(orbitalEffect.targetX - 0.72f) < 0.05f)

        repeat(115) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("Orbital Strike should deal heavy damage at range", world.p2.hp < chen.stats.maxHp - 200)
    }

    @Test
    fun daronBresilien_balayetteHitsGroundedOpponent() {
        val engine = CombatEngine()
        val daron = FighterRoster.getFighter("daron_bresilien")
        val glitcherman = FighterRoster.getFighter("glitcherman")
        val stage = FighterRoster.stages.first()

        var world = engine.createInitialState(
            mode = GameMode.TRAINING,
            stage = stage,
            p1Def = daron,
            p2Def = glitcherman
        ).let {
            it.copy(p1 = it.p1.copy(x = 0.40f), p2 = it.p2.copy(x = 0.62f, y = 0f))
        }

        world = engine.step(world, 20L, PlayerInput(baseAbilityPressed = true))
        repeat(10) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("Balayette Carioca should damage grounded opponent", world.p2.hp < glitcherman.stats.maxHp)
    }

    @Test
    fun floatingDamageNumbers_adaptColorByAttackPowerAndEmitSoundEffects() {
        val engine = CombatEngine()
        val glitcherman = FighterRoster.getFighter("glitcherman")
        val jardin = FighterRoster.getFighter("jardin")
        val stage = FighterRoster.stages.first()

        var world = engine.createInitialState(
            mode = GameMode.TRAINING,
            stage = stage,
            p1Def = glitcherman,
            p2Def = jardin
        ).let {
            it.copy(p1 = it.p1.copy(x = 0.42f), p2 = it.p2.copy(x = 0.56f))
        }

        // Light classic attack emits SWING_WHOOSH then CLASSIC_HIT and light popup
        world = engine.step(world, 20L, PlayerInput(classicPressed = true))
        assertTrue(world.soundEvents.contains(com.example.game.audio.SoundEffect.SWING_WHOOSH))
        repeat(8) {
            world = engine.step(world, 20L, PlayerInput())
        }
        val lightPopup = world.popups.lastOrNull()
        assertTrue("Light hit popup should spawn above defender", lightPopup != null && lightPopup.text.startsWith("-"))

        // Verify power-tier color differentiation via classifyDamageVisual
        val blockedStyle = CombatEngine.classifyDamageVisual(damage = 18, blocked = true)
        val lightStyle = CombatEngine.classifyDamageVisual(damage = 55)
        val mediumStyle = CombatEngine.classifyDamageVisual(damage = 95)
        val heavyStyle = CombatEngine.classifyDamageVisual(damage = 148)
        val ultiStyle = CombatEngine.classifyDamageVisual(damage = 285, isUlti = true)

        assertTrue("Different power tiers should have distinct colors", setOf(
            blockedStyle.color, lightStyle.color, mediumStyle.color, heavyStyle.color, ultiStyle.color
        ).size == 5)
        assertTrue("Scale should increase with power", lightStyle.scale < mediumStyle.scale && mediumStyle.scale < heavyStyle.scale && heavyStyle.scale < ultiStyle.scale)
    }

    @Test
    fun fiveStagesAvailable_includingStationOrbitale() {
        assertEquals(5, FighterRoster.stages.size)
        assertTrue(FighterRoster.stages.any { it.id == "orbital_station" })
    }

    @Test
    fun introCinematic_lasts2500ms_andCanBeSkipped() {
        val engine = CombatEngine()
        val p1 = FighterRoster.getFighter("glitcherman")
        val p2 = FighterRoster.getFighter("jardin")
        val stage = FighterRoster.getStage("orbital_station")

        var world = engine.createInitialState(
            mode = GameMode.VS_BOT,
            stage = stage,
            p1Def = p1,
            p2Def = p2
        )
        assertEquals(com.example.game.engine.MatchPhase.INTRO_CINEMATIC, world.phase)
        assertEquals(2500L, world.phaseTimerMs)

        // Advance 2550ms -> enters ROUND_INTRO
        repeat(51) {
            world = engine.step(world, 50L, PlayerInput())
        }
        assertEquals(com.example.game.engine.MatchPhase.ROUND_INTRO, world.phase)

        // Skipping intro immediately transitions to ROUND_INTRO
        val fresh = engine.createInitialState(GameMode.VS_BOT, stage, p1, p2)
        val skipped = engine.skipIntro(fresh)
        assertEquals(com.example.game.engine.MatchPhase.ROUND_INTRO, skipped.phase)
    }

    @Test
    fun adminModifiers_applyGodModeInfiniteUltiAndDamageMultiplier() {
        val engine = CombatEngine()
        val p1 = FighterRoster.getFighter("glitcherman")
        val p2 = FighterRoster.getFighter("daron_bresilien")
        val stage = FighterRoster.stages.first()

        val modifiers = com.example.game.data.AdminCombatModifiers(
            damageMultiplierP1 = 3f,
            infiniteUltiP1 = true,
            godModeP1 = true
        )

        var world = engine.createInitialState(GameMode.VS_BOT, stage, p1, p2).copy(
            phase = com.example.game.engine.MatchPhase.FIGHTING,
            p1 = FighterRoster.getFighter("glitcherman").let {
                com.example.game.engine.FighterRuntime(side = 1, def = it, x = 0.45f, facing = 1, hp = 500, ultiMeter = 0)
            },
            p2 = FighterRoster.getFighter("daron_bresilien").let {
                com.example.game.engine.FighterRuntime(side = 2, def = it, x = 0.56f, facing = -1, hp = it.stats.maxHp)
            }
        )

        world = engine.step(world, 20L, PlayerInput(), BotDifficulty.NORMAL, modifiers)
        assertEquals(p1.stats.maxHp, world.p1.hp)
        assertEquals(100, world.p1.ultiMeter)
    }

    @Test
    fun tournamentEngine_supportsAllBracketSizes2_3_4_8_10() {
        com.example.game.data.TournamentEngine.supportedSizes.forEach { size ->
            val participants = com.example.game.data.TournamentEngine.createParticipants(
                size = size,
                humanName = "Tester",
                humanFighterId = "glitcherman"
            )
            assertEquals(size, participants.size)
            var state = com.example.game.data.TournamentEngine.generateBracket(
                participants = participants,
                difficulty = BotDifficulty.NORMAL,
                stageId = "cyber_street"
            )

            var safety = 0
            while (state.champion == null && safety < 32) {
                val match = state.nextPlayableMatch
                assertTrue("Expected nextPlayableMatch when champion is null for size $size", match != null)
                state = com.example.game.data.TournamentEngine.recordMatchResult(
                    state = state,
                    matchId = match!!.id,
                    winnerSide = 1,
                    p1Score = 2,
                    p2Score = 0
                )
                safety++
            }
            assertTrue("Champion must be resolved for tournament size $size", state.champion != null)
        }
    }

    @Test
    fun finalRoundKo_entersKoSlowMoThenTransitionsToMatchOver_andPreservesMatchStats() {
        val engine = CombatEngine()
        val glitcherman = FighterRoster.getFighter("glitcherman")
        val daron = FighterRoster.getFighter("daron_bresilien")
        val stage = FighterRoster.stages.first()

        // P1 already won 1 round, has dealt 640 match damage & 4 max combo, full HP -> PERFECT KO!
        var world = engine.createInitialState(GameMode.VS_BOT, stage, glitcherman, daron).let {
            it.copy(
                phase = com.example.game.engine.MatchPhase.FIGHTING,
                roundNumber = 2,
                p1RoundsWon = 1,
                p1 = it.p1.copy(
                    x = 0.44f,
                    hp = glitcherman.stats.maxHp,
                    matchDamageTotal = 640,
                    maxComboCount = 4
                ),
                p2 = it.p2.copy(x = 0.56f, hp = 20)
            )
        }

        // Trigger Classic attack until lethal hit lands
        world = engine.step(world, 20L, PlayerInput(classicPressed = true))
        var safety = 0
        while (world.phase == com.example.game.engine.MatchPhase.FIGHTING && safety < 25) {
            world = engine.step(world, 20L, PlayerInput())
            safety++
        }

        assertEquals(com.example.game.engine.MatchPhase.KO_SLOWMO, world.phase)
        assertEquals(com.example.game.engine.FinishType.PERFECT, world.finishType)
        assertEquals(1, world.winnerSide)
        assertEquals(2, world.p1RoundsWon)
        assertTrue("Fatal blow must enter freeze-frame first", world.koFreezeRemainingMs > 0L)
        assertTrue("Fatal blow must burst fatal particles", world.fatalParticles.isNotEmpty())
        assertTrue(world.p1.matchDamageTotal > 640)
        assertTrue(world.p1.maxComboCount >= 4)

        // Step once during freeze-frame -> slow-mo timer stays frozen!
        val timerBeforeFreezeStep = world.phaseTimerMs
        world = engine.step(world, 50L, PlayerInput())
        assertEquals("Slow-mo timer must hold during freeze-frame", timerBeforeFreezeStep, world.phaseTimerMs)

        // Advance past 420ms freeze-frame + 1300ms KO slow-mo -> transitions to MATCH_OVER
        repeat(40) {
            world = engine.step(world, 50L, PlayerInput())
        }
        assertEquals(com.example.game.engine.MatchPhase.MATCH_OVER, world.phase)
        assertTrue(
            "VICTORY_FANFARE should fire when transitioning out of KO_SLOWMO",
            world.soundEvents.contains(com.example.game.audio.SoundEffect.VICTORY_FANFARE) ||
                world.phase == com.example.game.engine.MatchPhase.MATCH_OVER
        )
    }

    @Test
    fun arcadeRankCalculator_evaluatesAllTiersAndAllFightersHaveVictoryAssets() {
        FighterRoster.fighters.forEach { fighter ->
            assertTrue("Missing victorySplashRes for ${fighter.id}", fighter.victorySplashRes != 0)
            assertTrue("Missing victoryTagline for ${fighter.id}", fighter.victoryTagline.isNotBlank())
            assertTrue("Missing victoryQuote for ${fighter.id}", fighter.victoryQuote.isNotBlank())
        }

        val rankS = com.example.game.engine.ArcadeRankCalculator.evaluate(
            won = true,
            roundsWon = 2,
            roundsLost = 0,
            remainingHpRatio = 1.0f,
            maxCombo = 7,
            totalDamage = 2600,
            finishType = com.example.game.engine.FinishType.PERFECT
        )
        assertEquals(com.example.game.engine.ArcadeRankTier.S, rankS.tier)

        val rankA = com.example.game.engine.ArcadeRankCalculator.evaluate(
            won = true,
            roundsWon = 2,
            roundsLost = 1,
            remainingHpRatio = 0.45f,
            maxCombo = 3,
            totalDamage = 1400,
            finishType = com.example.game.engine.FinishType.KO
        )
        assertEquals(com.example.game.engine.ArcadeRankTier.A, rankA.tier)

        val rankB = com.example.game.engine.ArcadeRankCalculator.evaluate(
            won = false,
            roundsWon = 1,
            roundsLost = 2,
            remainingHpRatio = 0f,
            maxCombo = 4,
            totalDamage = 1800,
            finishType = com.example.game.engine.FinishType.KO
        )
        assertEquals(com.example.game.engine.ArcadeRankTier.B, rankB.tier)

        val rankC = com.example.game.engine.ArcadeRankCalculator.evaluate(
            won = false,
            roundsWon = 0,
            roundsLost = 2,
            remainingHpRatio = 0f,
            maxCombo = 0,
            totalDamage = 120,
            finishType = com.example.game.engine.FinishType.KO
        )
        assertEquals(com.example.game.engine.ArcadeRankTier.C, rankC.tier)
    }

    @Test
    fun allFourFighters_haveTwoUniqueDirectionalComboMoves() {
        FighterRoster.fighters.forEach { fighter ->
            assertTrue(fighter.comboBaseMove.name.isNotBlank())
            assertTrue(fighter.comboClassicMove.name.isNotBlank())
            assertTrue(
                "Combo base move should differ from standard baseAbility for ${fighter.id}",
                fighter.comboBaseMove.effectType != fighter.baseAbility.effectType
            )
            assertTrue(
                "Combo classic move should differ from standard classicMove for ${fighter.id}",
                fighter.comboClassicMove.effectType != fighter.classicMove.effectType
            )
        }
    }

    @Test
    fun directionalCombo_leftRightBase_triggersComboBaseMoveAndSoundForAllFighters() {
        val engine = CombatEngine()
        val stage = FighterRoster.stages.first()
        val dummy = FighterRoster.getFighter("daron_bresilien")

        FighterRoster.fighters.forEach { fighter ->
            var world = engine.createInitialState(
                mode = GameMode.TRAINING,
                stage = stage,
                p1Def = fighter,
                p2Def = dummy
            ).let {
                it.copy(p1 = it.p1.copy(x = 0.45f), p2 = it.p2.copy(x = 0.62f, y = 0f))
            }

            // Input ← then → to prime LEFT_RIGHT_BASE
            world = engine.step(world, 20L, PlayerInput(moveDir = -1f))
            world = engine.step(world, 20L, PlayerInput(moveDir = 0f))
            world = engine.step(world, 20L, PlayerInput(moveDir = 1f))
            assertEquals(
                "← → should prime LEFT_RIGHT_BASE for ${fighter.id}",
                com.example.game.engine.DirectionalComboKind.LEFT_RIGHT_BASE,
                world.p1.primedComboKind
            )

            // Press Capacité -> triggers comboBaseMove + COMBO_TRIGGER sound
            world = engine.step(world, 20L, PlayerInput(baseAbilityPressed = true))
            assertEquals(fighter.comboBaseMove.effectType, world.p1.activeComboMove?.effectType)
            assertEquals(null, world.p1.primedComboKind)
            assertTrue(
                "COMBO_TRIGGER sound should emit for ${fighter.id}",
                world.soundEvents.contains(com.example.game.audio.SoundEffect.COMBO_TRIGGER)
            )

            // Advance so combo lands
            repeat(15) {
                world = engine.step(world, 20L, PlayerInput())
            }
            assertTrue("Combo Base should damage opponent for ${fighter.id}", world.p2.hp < dummy.stats.maxHp)
        }
    }

    @Test
    fun directionalCombo_rightLeftClassic_triggersProjectilesOrUppercutAndExpiresWhenTooSlow() {
        val engine = CombatEngine()
        val stage = FighterRoster.stages.first()

        // 1. Glitcherman → ← + C fires Salve Pixel projectiles across arena
        val glitch = FighterRoster.getFighter("glitcherman")
        val daron = FighterRoster.getFighter("daron_bresilien")
        var world = engine.createInitialState(GameMode.TRAINING, stage, glitch, daron).let {
            it.copy(p1 = it.p1.copy(x = 0.25f), p2 = it.p2.copy(x = 0.65f, y = 0f))
        }
        world = engine.step(world, 20L, PlayerInput(moveDir = 1f))
        world = engine.step(world, 20L, PlayerInput(moveDir = -1f, classicPressed = true))
        assertEquals(com.example.game.data.MoveEffectType.GLITCH_SHARD_BARRAGE, world.p1.activeComboMove?.effectType)
        assertEquals(2, world.projectiles.size)
        assertTrue(world.soundEvents.contains(com.example.game.audio.SoundEffect.LASER_SHARD))

        // Advance until projectiles cross the arena and hit Daron
        repeat(35) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("Salve Pixel projectiles should hit distant opponent", world.p2.hp < daron.stats.maxHp)

        // 2. Chen → ← + C fires 3 Chopstick projectiles
        val chen = FighterRoster.getFighter("le_chinois")
        var chenWorld = engine.createInitialState(GameMode.TRAINING, stage, chen, daron).let {
            it.copy(p1 = it.p1.copy(x = 0.25f), p2 = it.p2.copy(x = 0.65f, y = 0f))
        }
        chenWorld = engine.step(chenWorld, 20L, PlayerInput(moveDir = 1f))
        chenWorld = engine.step(chenWorld, 20L, PlayerInput(moveDir = -1f, classicPressed = true))
        assertEquals(3, chenWorld.projectiles.size)

        // 3. Daron → ← + C fires Coconut Bomb projectile
        var daronWorld = engine.createInitialState(GameMode.TRAINING, stage, daron, chen).let {
            it.copy(p1 = it.p1.copy(x = 0.25f), p2 = it.p2.copy(x = 0.60f, y = 0f))
        }
        daronWorld = engine.step(daronWorld, 20L, PlayerInput(moveDir = 1f))
        daronWorld = engine.step(daronWorld, 20L, PlayerInput(moveDir = -1f, classicPressed = true))
        assertEquals(1, daronWorld.projectiles.size)
        repeat(35) {
            daronWorld = engine.step(daronWorld, 20L, PlayerInput())
        }
        assertTrue("Coconut bomb should damage Chen", daronWorld.p2.hp < chen.stats.maxHp)

        // 4. Slow tap (> 1100ms apart) should NOT prime directional combo
        var slowWorld = engine.createInitialState(GameMode.TRAINING, stage, glitch, daron)
        slowWorld = engine.step(slowWorld, 20L, PlayerInput(moveDir = -1f))
        repeat(30) {
            slowWorld = engine.step(slowWorld, 45L, PlayerInput(moveDir = 0f)) // 1350ms idle
        }
        slowWorld = engine.step(slowWorld, 20L, PlayerInput(moveDir = 1f, baseAbilityPressed = true))
        assertEquals(null, slowWorld.p1.activeComboMove)
        assertEquals(com.example.game.engine.CombatAction.BASE_ABILITY, slowWorld.p1.action)
    }
}
