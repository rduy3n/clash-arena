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
    fun jardinUlti_vineSnareAndColossalPunch_dealsThreeQuartersMaxHp() {
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

        // Trigger Jardin's Ulti (vines grow in front of him)
        world = engine.step(world, 20L, PlayerInput(ultiPressed = true))
        // Advance past startupMs (140ms) so vines hit & snare Chen
        repeat(10) {
            world = engine.step(world, 20L, PlayerInput())
        }
        assertTrue("Jardin should be charging colossal punch after vine hit", world.p1.chargingMegaPunch)
        assertTrue("Opponent should be snared by vines", world.p2.vineSnaredMs > 0L)

        // Advance past 680ms charge delay so colossal punch lands
        repeat(40) {
            world = engine.step(world, 20L, PlayerInput())
        }
        val expectedThreeQuartersDmg = (chen.stats.maxHp * 0.75f).toInt()
        assertEquals(chen.stats.maxHp - expectedThreeQuartersDmg, world.p2.hp)
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
}
