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
    /** Calm start: the first [stageMeters] run at this speed with boxes only and wider gaps. */
    val startSpeed: Float = 240f,
    /** Every [stageMeters] the target speed rises by [stageSpeedUp], up to [maxSpeed] (reached at 800 m). The start stays fast enough that one jump clears the tallest box. */
    val stageMeters: Int = 100,
    val stageSpeedUp: Float = 50f,
    val maxSpeed: Float = 620f,
    /** How quickly the speed eases up to the next stage, in world units per second squared, so it never jolts. */
    val acceleration: Float = 60f,
    /** Extra reaction time added to gaps during the calm start (first [stageMeters]). */
    val calmExtraReaction: Float = 0.4f,
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
    /**
     * Floating thorny bar at a random height: running under it is always safe; jumping into it is not.
     * Thin (thorns above and below are part of the hit box) so it reads as a bar, not a block to land on.
     */
    val floatMinWidth: Float = 90f,
    val floatMaxWidth: Float = 140f,
    val floatThickness: Float = 26f,
    val floatMinBottom: Float = 80f,
    val floatMaxBottom: Float = 120f,
    /** Only boxes for the first seconds; afterwards each spawn picks a type at random. */
    /** Towers and floating bars appear only after the calm start. */
    val mixedAfterMeters: Int = 100,
    val towerChance: Float = 0.25f,
    val floatChance: Float = 0.2f,
    val spikeChance: Float = 0.15f,
    /**
     * Spike field on the ground with a plank above it. The field is wider than the longest double-jump flight, so
     * the only way across is to jump onto the plank (one jump reaches it) and run along it. The plank can be
     * passed through from below and is landed on from above.
     */
    /** Chance an obstacle gap holds an item, how often it floats up high, and the two heights. */
    val pickupChance: Float = 0.45f,
    /** Chance a gap also holds a bonus coin. */
    val coinChance: Float = 0.35f,
    val pickupHighChance: Float = 0.4f,
    val pickupGroundBottom: Float = 6f,
    val pickupHighBottom: Float = 80f,
    val spikeHeight: Float = 26f,
    val spikeExtra: Float = 30f,
    val plankMinTop: Float = 85f,
    val plankMaxTop: Float = 105f,
    val plankThickness: Float = 14f,
    val plankOverhang: Float = 50f,
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
    /** Target speed for a run that has covered [meters]: one step faster every [stageMeters]. */
    fun targetSpeed(meters: Int): Float =
        kotlin.math.min(maxSpeed, startSpeed + stageSpeedUp * (meters.coerceAtLeast(0) / stageMeters))

    /** Spike field width at [speed]: always longer than a full double jump travels, so it cannot be jumped over. */
    fun spikeWidth(speed: Float): Float = speed * doubleAirTime + spikeExtra

    fun minGap(speed: Float, nearTower: Boolean = false): Float =
        speed * ((if (nearTower) doubleAirTime else airTime) + reactionTime) + playerSize
}

enum class ObstacleKind { BOX, TOWER, FLOAT, SPIKES }

data class Obstacle(
    var x: Float,
    val width: Float,
    val height: Float,
    var cleared: Boolean = false,
    /** Height of the obstacle's underside; 0 for anything standing on the ground. */
    val bottom: Float = 0f,
    val kind: ObstacleKind = ObstacleKind.BOX,
    /** SPIKES only: height of the plank's walking surface (0 = no plank) and how far it reaches past the spikes. */
    val plankTop: Float = 0f,
    val plankOverhang: Float = 0f,
) {
    val top: Float get() = bottom + height
    val plankLeft: Float get() = x - plankOverhang
    val plankRight: Float get() = x + width + plankOverhang
}

/**
 * Something lying on the road: a friend's item (see quest/Quest.kt) or, with [coin], a bonus coin. Touching it picks
 * it up; it never ends the run.
 */
data class Pickup(var x: Float, val bottom: Float, val size: Float = PICKUP_SIZE, var taken: Boolean = false, val coin: Boolean = false) {
    companion object { const val PICKUP_SIZE = 34f }
}

class RunnerEngine(seed: Long = 1L, val config: RunnerConfig = RunnerConfig()) {
    enum class Phase { READY, RUNNING, PAUSED, OVER }

    private var random = Random(seed)
    /** Separate from [random] so placing items never changes the obstacle sequence. */
    private var pickupRandom = Random(seed * 31 + 7)
    /** Whether items are scattered on the road this run. */
    var pickupsOn = true
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
    private val _pickups = mutableListOf<Pickup>()
    val pickups: List<Pickup> get() = _pickups
    /** Items picked up in this run. */
    var collected = 0; private set
    /** Bonus coins picked up on the road in this run (distance coins are counted separately, see ShopRules). */
    var coins = 0; private set

    /** Height the runner is standing on: 0 on the ground, or a plank's top. */
    var floorY = 0f; private set
    val grounded: Boolean get() = playerY <= floorY && playerVy <= 0f
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
        pickupRandom = Random(pickupRandom.nextLong())
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
        playerY = 0f; playerVy = 0f; floorY = 0f
        speed = config.startSpeed
        distance = 0f; cleared = 0; elapsed = 0f
        accumulator = 0f; overTime = 0f
        airJumpsUsed = 0
        _obstacles.clear()
        _pickups.clear(); collected = 0; coins = 0
        // First obstacle spawns after about one second of running, then needs a few more seconds to arrive.
        nextGap = config.startSpeed
        nextKind = ObstacleKind.BOX
    }

    private fun tick(dt: Float) {
        elapsed += dt
        // Speed steps up every 100 m (eased, not instant); the first 100 m stay at the calm start speed.
        speed = min(config.targetSpeed(meters), speed + config.acceleration * dt)

        val previousY = playerY
        playerVy -= config.gravity * dt
        playerY += playerVy * dt

        val dx = speed * dt
        distance += dx
        for (o in _obstacles) o.x -= dx
        _obstacles.removeAll { it.plankRight < -config.playerSize && it.x + it.width < -config.playerSize }
        for (p in _pickups) p.x -= dx
        _pickups.removeAll { it.taken || it.x + it.size < -config.playerSize }

        // Land on the ground or on a plank the feet were above a moment ago; walking off a plank's end falls.
        floorY = supportBelow(previousY)
        if (playerY <= floorY) { playerY = floorY; playerVy = 0f; airJumpsUsed = 0 }

        val last = _obstacles.lastOrNull()
        val clearSinceLast = if (last == null) Float.MAX_VALUE else config.spawnX - (last.x + last.width)
        if (last == null && distance >= nextGap || last != null && clearSinceLast >= nextGap) spawn()

        val pl = config.playerX + config.playerInset
        val pr = config.playerX + config.playerSize - config.playerInset
        val pb = playerY
        val pt = playerY + config.playerHitHeight
        for (p in _pickups) {
            if (!p.taken && pr > p.x && pl < p.x + p.size && pb < p.bottom + p.size && pt > p.bottom) {
                p.taken = true
                if (p.coin) coins++ else collected++
            }
        }
        for (o in _obstacles) {
            val ol = o.x + config.obstacleInset
            val or = o.x + o.width - config.obstacleInset
            val ot = o.top - config.obstacleInset
            val ob = if (o.bottom > 0f) o.bottom + config.obstacleInset else Float.NEGATIVE_INFINITY
            if (pr > ol && pl < or && pb < ot && pt > ob) { phase = Phase.OVER; return }
            if (!o.cleared && or < pl) { o.cleared = true; cleared++ }
        }
    }

    /** Highest surface under the runner that their feet were at or above before this step (planks are one-way). */
    private fun supportBelow(previousY: Float): Float {
        val pl = config.playerX + config.playerInset
        val pr = config.playerX + config.playerSize - config.playerInset
        var best = 0f
        for (o in _obstacles) {
            if (o.plankTop <= 0f) continue
            if (pr > o.plankLeft && pl < o.plankRight && previousY >= o.plankTop - LANDING_TOLERANCE) best = maxOf(best, o.plankTop)
        }
        return best
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
            ObstacleKind.SPIKES -> Obstacle(c.spawnX, c.spikeWidth(speed), c.spikeHeight, kind = ObstacleKind.SPIKES,
                plankTop = between(c.plankMinTop, c.plankMaxTop), plankOverhang = c.plankOverhang)
        }
        nextKind = if (meters < c.mixedAfterMeters) ObstacleKind.BOX else {
            val r = random.nextFloat()
            when {
                r < c.towerChance -> ObstacleKind.TOWER
                r < c.towerChance + c.floatChance -> ObstacleKind.FLOAT
                r < c.towerChance + c.floatChance + c.spikeChance -> ObstacleKind.SPIKES
                else -> ObstacleKind.BOX
            }
        }
        val nearTower = kind == ObstacleKind.TOWER || nextKind == ObstacleKind.TOWER
        nextGap = c.minGap(speed, nearTower) + random.nextFloat() * c.gapVariance * speed +
            (if (meters < c.stageMeters) c.calmExtraReaction * speed else 0f) +
            // After spikes the runner first walks off the plank's far end and drops to the ground.
            (if (kind == ObstacleKind.SPIKES) c.plankOverhang + speed * kotlin.math.sqrt(2f * c.plankMaxTop / c.gravity) else 0f)
        placePickup(_obstacles.last())
    }

    /**
     * Sometimes leaves an item in the middle of the gap after [o], and sometimes a bonus coin in its first quarter:
     * on the ground, or up where a jump reaches it.
     */
    private fun placePickup(o: Obstacle) {
        if (!pickupsOn) return
        val gapStart = maxOf(o.x + o.width, o.plankRight)
        val gapEnd = o.x + o.width + nextGap
        val gap = gapEnd - gapStart
        val size = Pickup.PICKUP_SIZE
        // Both rolls always happen, so the item pattern does not depend on the coins.
        val item = pickupRandom.nextFloat() < config.pickupChance
        val itemHigh = pickupRandom.nextFloat() < config.pickupHighChance
        val coin = pickupRandom.nextFloat() < config.coinChance
        val coinHigh = pickupRandom.nextFloat() < config.pickupHighChance
        if (item && gap >= size * 3) {
            _pickups += Pickup((gapStart + gapEnd) / 2f - size / 2f, if (itemHigh) config.pickupHighBottom else config.pickupGroundBottom)
        }
        // The coin goes a quarter of the way in, never on top of the item.
        if (coin && gap >= size * 6) {
            _pickups += Pickup(gapStart + gap * 0.22f - size / 2f, if (coinHigh) config.pickupHighBottom else config.pickupGroundBottom, coin = true)
        }
    }

    private fun between(min: Float, max: Float) = random.nextFloat() * (max - min) + min

    private companion object {
        /** Feet within this distance above a plank's top still count as landing on it (one physics step). */
        const val LANDING_TOLERANCE = 0.5f
    }
}
