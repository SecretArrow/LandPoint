package com.landpoint.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.landpoint.app.LandPointApp
import java.util.concurrent.TimeUnit

/**
 * Empties the recently-deleted bin: hard-deletes parcels whose
 * [LandEntity.deletedAt] is more than thirty days old, along with their photo
 * files.
 *
 * [LandRepository.purgeExpired] is deterministic and idempotent — the same call
 * twice in a row deletes nothing the second time — so this worker does not
 * track state, and a run that overlaps the app-start sweep in
 * [com.landpoint.app.LandPointApp] is harmless. The worker exists because the
 * app-start sweep only fires when the app is opened: someone who deletes a
 * parcel and does not touch the app again for a month still gets their storage
 * back on the next day the phone is willing to run work.
 *
 * The schedule is unique and `KEEP`, so every launch re-asserts the same daily
 * job without resetting its window; constraints keep it off a dying battery or
 * a full disk, where skipping a day costs nothing (the parcel is already gone
 * from every list) but running everything at once would not be kind.
 */
class RecentlyDeletedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val app = applicationContext as? LandPointApp
        if (app == null) {
            // No container to reach: nothing can be purged from here. Not
            // retryable — the next app start sweeps anyway.
            Result.failure()
        } else {
            app.container.repository.purgeExpired()
            Result.success()
        }
    } catch (e: Exception) {
        // A locked or unreadable database, a full disk — all of it is transient
        // from a background job's point of view. Backing off and trying again
        // tomorrow is exactly right for a purge that has already waited 30 days.
        Result.retry()
    }

    companion object {

        /** Unique name of the daily purge, so re-scheduling never duplicates it. */
        const val WORK_NAME = "recently-deleted-purge"

        /**
         * Makes sure the daily purge is scheduled, keeping any existing
         * schedule rather than restarting its window.
         *
         * Called from Application.onCreate; cheap when nothing changed, and
         * self-healing when WorkManager's jobs were lost to a cleared app or a
         * backup restore.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecentlyDeletedWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
