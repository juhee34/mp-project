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
    /** Left edge of the player's 48x48 sprite cell; right of the score text even at the top of a double jump. */
    val playerX: Float = 120f,
    /** Sprite cell size: 48 art pixels at 1.75 world units each, so a body of ~32 pixels is ~56 units tall. */
    val playerSize: Float = 84f,
    /** Hitbox inset on each side of the cell; the 52-unit hitbox matches the body, not the tail or ears. */
    val playerInset: Float = 16f,
    /** Hitbox height from the feet; only floating obstacles can reach it. */
    val playerHitHeight: Float = 50f,
    val obstacleInset: Float = 3f,
    /**
     * Low box: one jump clears it at every speed. Height is width x [0.6, 1.0], the same proportions as the hometown
     * prop sprites (logs, crates, baskets), so the art is barely stretched. Same rule for every friend.
     */
    val obstacleMinWidth: Float = 44f,
    val obstacleMaxWidth: Float = 64f,
    val obstacleMinAspect: Float = 0.6f,
    val obstacleMaxAspect: Float = 1.0f,
    val obstacleMinHeight: Float = 30f,
    val obstacleMaxHeight: Float = 64f,
    /** Tower: taller than the single-jump peak (140), so it needs the double jump. */
    val towerMinWidth: Float = 30f,
    val towerMaxWidth: Float = 40f,
    val towerMinHeight: Float = 150f,
    val towerMaxHeight: Float = 170f,
    /** Floating bar at a random height: running under it is always safe; jumping into it is not. */
    val floatMinWidth: Float = 60f,
    val floatMaxWidth: Float = 100f,
    val floatThickness: Float = 28f,
    val floatMinBottom: Float = 80f,
    val floatMaxBottom: Float = 120f,
    /** Only boxes for the first seconds; afterwards each spawn picks a type at random. */
    val mixedAfterSeconds: Float = 8f,
    val towerChance: Float = 0.25f,
    val floatChance: Float = 0.2f,
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

    /** Longest time in the air: first jump to its peak, then the air jump, then the fall from the highest point. */
    val doubleAirTime: Float get() {
        val toApex = jumpVelocity / gravity
        val up = airJumpVelocity / gravity
        val peak = apexHeight + airJumpVelocity * airJumpVelocity / (2f * gravity)
        return toApex + up + sqrt(2f * peak / gravity)
    }

    /**
     * Seconds after take-off during which the feet are above [height] when the air jump is used at the first
     * jump's peak, or null if even that does not reach [height].
     */
    fun doubleJumpWindow(height: Float): Pair<Float, Float>? {
        val toApex = jumpVelocity / gravity
        val rem = airJumpVelocity * airJumpVelocity - 2f * gravity * (height - apexHeight)
        if (rem <= 0f) return null
        val root = sqrt(rem)
        return (toApex + (airJumpVelocity - root) / gravity) to (toApex + (airJumpVelocity + root) / gravity)
    }

    /** Time the player's hitbox bottom spends above [height] during one jump. */
    fun timeAbove(height: Float): Float {
        val rem = jumpVelocity * jumpVelocity - 2f * gravity * height
        return if (rem <= 0f) 0f else 2f * sqrt(rem) / gravity
    }

    /**
     * Smallest clear distance between obstacles that still allows landing and jumping again. Next to a tower the
     * whole double jump has to fit.
     */
    fun minGap(speed: Float, nearTower: Boolean = false): Float =
        speed * ((if (nearTower) doubleAirTime else airTime) + reactionTime) + playerSize
}

enum class ObstacleKind { BOX, TOWER, FLOAT }

data class Obstacle(
    var x: Float,
    val width: Float,
    val height: Float,
    var cleared: Boolean = false,
    /** Height of the obstacle's underside; 0 for anything standing on the ground. */
    val bottom: Float = 0f,
    val kind: ObstacleKind = ObstacleKind.BOX,
) {
    val top: Float get() = bottom + height
}

class RunnerEngine(seed: Long = 1L, val config: RunnerConfig = RunnerConfig()) {
    enum class Phase { READY, RUNNING, PAUSED, OVER }

    private var random = Random(seed)
    private var accumulator = 0f
    private var nextGap = 0f
    private var overTime = 0f
    /** Type of the obstacle that will be spawned next; chosen early so the gap before it can fit it. */
    private var nextKind = ObstacleKind.BOX

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
        nextKind = ObstacleKind.BOX
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
        val pt = playerY + config.playerHitHeight
        for (o in _obstacles) {
            val ol = o.x + config.obstacleInset
            val or = o.x + o.width - config.obstacleInset
            val ot = o.top - config.obstacleInset
            val ob = if (o.bottom > 0f) o.bottom + config.obstacleInset else Float.NEGATIVE_INFINITY
            if (pr > ol && pl < or && pb < ot && pt > ob) { phase = Phase.OVER; return }
            if (!o.cleared && or < pl) { o.cleared = true; cleared++ }
        }
    }

    private fun spawn() {
        val c = config
        val kind = nextKind
        _obstacles += when (kind) {
            ObstacleKind.BOX -> {
                val width = between(c.obstacleMinWidth, c.obstacleMaxWidth)
                val height = (width * between(c.obstacleMinAspect, c.obstacleMaxAspect))
                    .coerceIn(c.obstacleMinHeight, c.obstacleMaxHeight)
                Obstacle(c.spawnX, width, height)
            }
            ObstacleKind.TOWER -> Obstacle(c.spawnX, between(c.towerMinWidth, c.towerMaxWidth),
                between(c.towerMinHeight, c.towerMaxHeight), kind = ObstacleKind.TOWER)
            ObstacleKind.FLOAT -> Obstacle(c.spawnX, between(c.floatMinWidth, c.floatMaxWidth), c.floatThickness,
                bottom = between(c.floatMinBottom, c.floatMaxBottom), kind = ObstacleKind.FLOAT)
        }
        nextKind = if (elapsed < c.mixedAfterSeconds) ObstacleKind.BOX else {
            val r = random.nextFloat()
            when {
                r < c.towerChance -> ObstacleKind.TOWER
                r < c.towerChance + c.floatChance -> ObstacleKind.FLOAT
                else -> ObstacleKind.BOX
            }
        }
        val nearTower = kind == ObstacleKind.TOWER || nextKind == ObstacleKind.TOWER
        nextGap = c.minGap(speed, nearTower) + random.nextFloat() * c.gapVariance * speed
    }

    private fun between(min: Float, max: Float) = random.nextFloat() * (max - min) + min
}
