package com.communitycare.ai.sync

import android.content.Context
import androidx.work.*
import com.communitycare.ai.CommunityCareApplication
import com.communitycare.ai.SyncStatus
import com.communitycare.ai.data.SyncEngine
import com.google.gson.Gson
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock

class EncounterSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CommunityCareApplication
        val container = app.container ?: return Result.failure()
        return container.syncMutex.withLock {
            val profile = container.auth.profile.value ?: return@withLock Result.success()
            container.syncStatus.value = container.syncStatus.value.copy(busy = true, message = null)
            try {
                val outcome = SyncEngine().sync(profile, container.encounters, container.remote(profile))
                if (container.auth.profile.value != profile) {
                    container.syncStatus.value = SyncStatus()
                    return@withLock Result.success()
                }
                val status = SyncStatus(message = outcome.message, checkedAt = Instant.now().toString())
                container.storage.write("sync-status.bin", Gson().toJson(status))
                container.syncStatus.value = status
                if (outcome.retry) Result.retry() else Result.success()
            } catch (cancelled: CancellationException) {
                container.syncStatus.value = container.syncStatus.value.copy(busy = false)
                throw cancelled
            } catch (error: Exception) {
                container.syncStatus.value = SyncStatus(message = "Could not sync. Records remain saved.")
                Result.retry()
            }
        }
    }

    companion object {
        private const val IMMEDIATE = "communitycare-sync-now"
        fun schedule(context: Context, periodicOnly: Boolean = false) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val manager = WorkManager.getInstance(context)
            manager.enqueueUniquePeriodicWork("communitycare-sync-periodic", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<EncounterSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints).build())
            if (!periodicOnly) manager.enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<EncounterSyncWorker>().setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build())
        }
        fun cancelImmediate(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE) }
    }
}
