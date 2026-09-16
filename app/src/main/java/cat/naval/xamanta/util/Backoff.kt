package cat.naval.xamanta.util

import kotlin.random.Random

class Backoff(
    private val initialMs: Long,
    private val maxMs: Long,
    private val jitterRatio: Double = 0.2,
) {
    var attempts: Int = 0
        private set

    fun reset() {
        attempts = 0
    }

    fun nextDelayMs(): Long {
        val base = (initialMs shl attempts.coerceAtMost(MAX_SHIFT)).coerceIn(initialMs, maxMs)
        attempts++
        val jitter = (base * jitterRatio).toLong()
        return if (jitter <= 0L) base else base - jitter + Random.nextLong(2 * jitter + 1)
    }

    private companion object {
        const val MAX_SHIFT = 14
    }
}
