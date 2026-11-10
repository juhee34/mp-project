package com.tinyrunners.school.game

import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Shared physics for every character. World units: x grows to the right, y is height above the ground.
 * The engine deliberately takes no character input, so all six friends run, jump and collide identically.
 */
data class RunnerConfig(
    val gravity: Float = 2400f,
    val jumpVelocity: Float = 820f,
    val startSpeed: Float = 260f,
    val maxSpeed: Float = 520f,
    val acceleration: Float = 6f,
    val playerX: Float = 96f,
    val playerSize: Float = 44f,
    /** Hitbox inset on each side of the player sprite; keeps near misses forgiving. */
    val playerInset: Float = 7f,
    val obstacleInset: Float = 3f,
    val obstacleMinWidth: Float = 24f,
    val obstacleMaxWidth: Float = 44f,
    val obstacleMinHeight: Float = 30f,
    val obstacleMaxHeight: Float = 70f,
    val spawnX: Float = 1000f,
    /** Seconds of reaction time added to every gap, on top of one full jump. */
    val reactionTime: Float = 0.35f,
    /** Extra random gap in seconds of travel at the current speed. */
    val gapVariance: Float = 0.9f,
    val unitsPerMeter: Float = 40f,
    val pointsPerCleared: Int = 5,
    /** Fixed physics step for stable, frame-rate independent results. */
    val step: Float = 1f / 120f,
    /** Longest frame the engine will simulate; avoids tunnelling after a stall. */
    val maxFrame: Float = 1f / 20f,
    /** Wait before a tap on the result screen restarts, so a late jump tap is not swallowed as restart. */
    val restartDelay: Float = 0.6f,
    /** Second jump in the air: one per airtime, same for every character. */
    val airJumps: Int = 1,
    val airJumpVelocity: Float = 700f,
) {
    val airTime: Float get() = 2f * jumpVelocity / gravity
    val apexHeight: Float get() = jumpVelocity * jumpVelocity / (2f * gravity)
    val playerHitWidth: Float get() = playerSize - 2f * playerInset

    /** Time the player's hitbox bottom spends above [height] during one jump. */
    fun timeAbove(height: Float): Float {
        val rem = jumpVelocity * jumpVelocity - 2f * gravity * height
        return if (rem <= 0f) 0f else 2f * sqrt(rem) / gravity
    }

    /** Smallest clear distance between obstacles that still allows landing and jumping again. */
    fun minGap(speed: Float): Float = speed * (airTime + reactionTime) + playerSize
}

data class Obstacle(var x: Float, val width: Float, val height: Float, var cleared: Boolean = false)

class RunnerEngine(seed: Long = 1L, val config: RunnerConfig = RunnerConfig()) {
    enum class Phase { READY, RUNNING, PAUSED, OVER }

    private var random = Random(seed)
    private var accumulator = 0f
    private var nextGap = 0f
    private var overTime = 0f

    var phase = Phase.READY; private set
    var playerY = 0f; private set
    var playerVy = 0f; private set
    var speed = config.startSpeed; private set
    var distance = 0f; private set
    var cleared = 0; private set
    var elapsed = 0f; private set
    var airJumpsUsed = 0; private set
    private val _obstacles = mutableListOf<Obstacle>()
    val obstacles: List<Obstacle> get() = _obstacles

    val grounded: Boolean get() = playerY <= 0f && playerVy <= 0f
    val meters: Int get() = (distance / config.unitsPerMeter).toInt()
    val score: Int get() = meters + cleared * config.pointsPerCleared
    val canRestart: Boolean get() = phase == Phase.OVER && overTime >= config.restartDelay

    init { reset() }

    /** Start from READY, jump while running, or restart after the result delay. Returns true if it did something. */
    fun tap(): Boolean = when (phase) {
        Phase.READY -> { phase = Phase.RUNNING; jump(); true }
        Phase.RUNNING -> jump()
        Phase.PAUSED -> { phase = Phase.RUNNING; true }
        Phase.OVER -> if (canRestart) { restart(); true } else false
    }

    fun pause() { if (phase == Phase.RUNNING) phase = Phase.PAUSED }
    fun resume() { if (phase == Phase.PAUSED) phase = Phase.RUNNING }

    /** New run with a fresh obstacle sequence; nothing from the previous run carries over. */
    fun restart() {
        random = Random(random.nextLong())
        reset()
    }

    fun update(frameSeconds: Float) {
        if (phase == Phase.OVER) { overTime += min(frameSeconds, config.maxFrame); return }
        if (phase != Phase.RUNNING) return
        accumulator += min(frameSeconds.coerceAtLeast(0f), config.maxFrame)
        while (accumulator >= config.step && phase == Phase.RUNNING) {
            accumulator -= config.step
            tick(config.step)
        }
    }

    private fun jump(): Boolean {
        if (grounded) {
            airJumpsUsed = 0
            playerVy = config.jumpVelocity
            return true
        }
        if (airJumpsUsed >= config.airJumps) return false
        airJumpsUsed++
        playerVy = config.airJumpVelocity
        return true
    }

    private fun reset() {
        phase = Phase.READY
        playerY = 0f; playerVy = 0f
        speed = config.startSpeed
        distance = 0f; cleared = 0; elapsed = 0f
        accumulator = 0f; overTime = 0f
        airJumpsUsed = 0
        _obstacles.clear()
        // First obstacle spawns after about one second of running, then needs a few more seconds to arrive.
        nextGap = config.startSpeed
    }

    private fun tick(dt: Float) {
        elapsed += dt
        speed = min(config.maxSpeed, speed + config.acceleration * dt)

        playerVy -= config.gravity * dt
        playerY += playerVy * dt
        if (playerY <= 0f) { playerY = 0f; playerVy = 0f; airJumpsUsed = 0 }

        val dx = speed * dt
        distance += dx
        for (o in _obstacles) o.x -= dx
        _obstacles.removeAll { it.x + it.width < -config.playerSize }

        val last = _obstacles.lastOrNull()
        val clearSinceLast = if (last == null) Float.MAX_VALUE else config.spawnX - (last.x + last.width)
        if (last == null && distance >= nextGap || last != null && clearSinceLast >= nextGap) spawn()

        val pl = config.playerX + config.playerInset
        val pr = config.playerX + config.playerSize - config.playerInset
        val pb = playerY
        for (o in _obstacles) {
            val ol = o.x + config.obstacleInset
            val or = o.x + o.width - config.obstacleInset
            val ot = o.height - config.obstacleInset
            if (pr > ol && pl < or && pb < ot) { phase = Phase.OVER; return }
            if (!o.cleared && or < pl) { o.cleared = true; cleared++ }
        }
    }

    private fun spawn() {
        val width = random.nextFloat() * (config.obstacleMaxWidth - config.obstacleMinWidth) + config.obstacleMinWidth
        val height = random.nextFloat() * (config.obstacleMaxHeight - config.obstacleMinHeight) + config.obstacleMinHeight
        _obstacles += Obstacle(config.spawnX, width, height)
        nextGap = config.minGap(speed) + random.nextFloat() * config.gapVariance * speed
    }
}
