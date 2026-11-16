package com.landpoint.app

import android.app.Application
import android.util.Log
import com.landpoint.app.data.RecentlyDeletedWorker
import com.landpoint.app.map.MapLibreInit

class LandPointApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Before any MapView is constructed, and here rather than on a map screen
        // because it loads the native rendering library — a cost worth paying while
        // the launcher icon is still animating instead of on the first frame of a map.
        MapLibreInit.start(this)

        // The recently-deleted bin empties itself through the daily WorkManager
        // job, which is cheap to re-assert here and self-heals when the OS has
        // lost the schedule. The purge itself deliberately does NOT run inline
        // at launch: an eager database sweep on the cold-start path slows every
        // launch for a task that can always wait a day, and in the JVM test
        // environment it races the test harness's own database setup. The
        // worker is the only purge entry point; a binned parcel is invisible to
        // every live query the moment it is deleted, so a purge that waits for
        // the worker is only ever reclaiming storage, never correctness.
        runCatching { RecentlyDeletedWorker.schedule(this) }
            .onFailure { Log.w(TAG, "could not schedule the bin purge", it) }
    }

    private companion object {
        const val TAG = "LandPointApp"
    }
}
