package com.tinyrunners.school.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunStoryTest {
    private val script = RunScript(
        storyId = "mint_intro", title = "t",
        lines = listOf(RunLine(3, true, "a"), RunLine(20, false, "b")),
        fork = RunFork(80, "q", "up", "down"),
        endings = mapOf("up" to listOf(RunLine(0, false, "went up")), "down" to listOf(RunLine(0, false, "went down"))),
    )

    private fun play(p: RunStoryPlayer, meters: Int, ahead: Boolean, choice: Boolean?, seconds: Float) {
        var t = 0f
        while (t < seconds) { p.update(meters, ahead, choice, 0.05f); t += 0.05f }
    }

    @Test fun linesAppearByDistanceOneAtATime() {
        val p = RunStoryPlayer(script)
        p.update(0, false, null, 0.05f); assertNull(p.current)
        p.update(25, false, null, 0.05f)
        assertEquals("a", p.current?.text)
        play(p, 25, false, null, 3f)
        assertEquals("b", p.current?.text)
    }

    @Test fun theQuestionShowsWhileTheCrossroadsIsAheadAndTheChoicePicksTheEnding() {
        val p = RunStoryPlayer(script)
        play(p, 30, false, null, 10f)
        assertFalse(p.asking)
        p.update(70, true, null, 0.05f)
        assertTrue(p.asking)
        p.update(85, false, true, 0.05f)
        assertFalse(p.asking)
        assertEquals("up", p.endingKey)
        assertEquals("went up", p.current?.text)
        assertFalse(p.finished)
        play(p, 90, false, true, 5f)
        assertTrue("the ending was told", p.finished)
    }

    @Test fun finaleEndings() {
        assertEquals(EndingRule.HAPPY, EndingRule.pick(allRight = true, night = false))
        assertEquals("only the night opens the hidden ending", EndingRule.HIDDEN, EndingRule.pick(allRight = true, night = true))
        assertEquals(EndingRule.BAD, EndingRule.pick(allRight = false, night = false))
        assertEquals("a wrong route is bad even at night", EndingRule.BAD, EndingRule.pick(allRight = false, night = true))
    }

    @Test fun aWrongRouteBeforeTheFinaleLeavesAHint() {
        val s = script.copy(lines = emptyList(), fork = script.fork!!.copy(correct = true))
        val p = RunStoryPlayer(s, missedLine = "missed")
        p.update(85, false, false, 0.05f)
        assertEquals("went down", p.current?.text)
        play(p, 90, false, false, 2.5f)
        assertEquals("missed", p.current?.text)
        val right = RunStoryPlayer(s, missedLine = "missed")
        right.update(85, false, true, 0.05f)
        play(right, 90, false, true, 6f)
        assertTrue(right.finished)
    }

    @Test fun weatherDifficulty() {
        fun r(sky: com.tinyrunners.school.weather.Sky, day: Boolean) =
            com.tinyrunners.school.weather.WeatherReport(com.tinyrunners.school.weather.City.SEOUL, sky, 10.0, day)
        assertEquals(Difficulty.EASY, Difficulty.of(r(com.tinyrunners.school.weather.Sky.CLEAR, true)))
        assertEquals(Difficulty.HARDEST, Difficulty.of(r(com.tinyrunners.school.weather.Sky.STORM, false)))
        assertEquals(Difficulty.HARDEST, Difficulty.of(r(com.tinyrunners.school.weather.Sky.RAIN, false)))
        assertEquals(Difficulty.NORMAL, Difficulty.of(null))
        assertTrue(Difficulty.entries.zipWithNext().all { (a, b) -> a.speed < b.speed })
    }

    @Test fun hardestWeatherStillClearsEveryObstacle() {
        val c = RunnerConfig()
        val top = c.maxSpeed * Difficulty.HARDEST.speed
        // One jump still carries the runner over the tallest box at the fastest speed of the hardest weather.
        assertTrue(top * c.timeAbove(c.obstacleMaxHeight) >= c.obstacleMaxWidth + c.playerHitWidth)
        assertTrue(c.reactionTime + Difficulty.HARDEST.reaction > 0.3f)
    }

    @Test fun theAppPackHasEveryFinale() {
        val file = listOf("src/main/assets/content/run_stories.json", "app/src/main/assets/content/run_stories.json").map(::File).first { it.exists() }
        val json = file.readText()
        for (id in listOf("mint", "cheese", "popo", "lumi", "nunu", "piko")) {
            val finale = RunScriptParser.parse(json, "${id}_episode_3", id, "t", "${id}_episode_3")
            assertNotNull(finale); finale!!
            assertTrue(finale.finale)
            assertEquals(setOf("happy", "hidden", "bad"), finale.endings.keys)
            assertNotNull(finale.fork)
        }
        val fallback = RunScriptParser.parse(json, "unknown_story", "라일", "제목", null)!!
        assertTrue("the shared script fills the name", fallback.lines.first().text.contains("라일"))
    }

    @Test fun engineCountsJumpsAndLandingsAndDecidesTheCrossroads() {
        fun run(jumpOnto: Boolean): Boolean? {
            val e = RunnerEngine(seed = 3)
            e.scheduleFork(1)
            e.tap()   // start (and a first jump)
            var t = 0f
            while (t < 40f && e.forkChoice == null && e.phase == RunnerEngine.Phase.RUNNING) {
                val c = e.config
                val pr = c.playerX + c.playerSize - c.playerInset
                val next = e.obstacles.firstOrNull { it.x + it.width > c.playerX + c.playerInset }
                when (next?.kind) {
                    // Upper route: jump just before the raised path, as onto a plank; lower route: run on.
                    ObstacleKind.FORK -> if (jumpOnto && e.grounded && e.floorY == 0f && next.x - pr <= e.speed * 0.08f) e.tap()
                    ObstacleKind.BOX -> if (e.grounded && (next.x + next.width / 2) - (c.playerX + c.playerSize / 2) <= e.speed * c.airTime / 2) e.tap()
                    else -> Unit
                }
                e.update(1f / 60f); t += 1f / 60f
            }
            assertTrue(e.jumpCount > 0)
            assertTrue(e.landCount > 0)
            return e.forkChoice
        }
        assertEquals(true, run(jumpOnto = true))
        assertEquals(false, run(jumpOnto = false))
    }
}
