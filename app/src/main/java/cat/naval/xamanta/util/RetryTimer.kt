package cat.naval.xamanta.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "RetryTimer"

class RetryTimer(
    private val name: String,
    private val scope: CoroutineScope,
    initialDelayMs: Long,
    maxDelayMs: Long,
    private val onRetry: () -> Unit,
) {

    private val backoff = Backoff(initialDelayMs, maxDelayMs)
    private var job: Job? = null

    private var owedDelayMs: Long? = null

    @Synchronized
    fun schedule() {
        job?.cancel()
        val delayMs = owedDelayMs ?: backoff.nextDelayMs()
        owedDelayMs = delayMs
        Log.i(TAG, "$name retry #${backoff.attempts} in ${delayMs}ms")
        job = scope.launch {
            delay(delayMs)
            markFired()
            onRetry()
        }
    }

    @Synchronized
    fun cancel() {
        job?.cancel()
        job = null
    }

    @Synchronized
    fun reset() {
        cancel()
        owedDelayMs = null
        backoff.reset()
    }

    @Synchronized
    private fun markFired() {
        owedDelayMs = null
    }
}
