package cat.naval.xamanta.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val TAG = "BackstopWorker"

private const val RECONCILE_WORK = "PERIODIC_RECONCILE"
private const val RECONCILE_INTERVAL_HOURS = 6L

private const val RETIRED_COMMAND_WORK = "PERIODIC_COMMAND_DRAIN"

class BackstopWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackstopWorker>(
                RECONCILE_INTERVAL_HOURS,
                TimeUnit.HOURS,
            ).addTag(RECONCILE_WORK).build()
            val work = WorkManager.getInstance(context)
            work.enqueueUniquePeriodicWork(
                RECONCILE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            work.cancelUniqueWork(RETIRED_COMMAND_WORK)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "periodic safety-net firing — requesting reconcile")
        PolicyService.requestReconcile(appContext, "periodic safety-net")
        return Result.success()
    }
}
