package ch.nikleberg.bettershared.work;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SyncManager {
    private static final String TAG = SyncManager.class.getSimpleName();
    private static final String SYNC_WORKER_NAME = "sync_worker";

    private static SyncManager INSTANCE = null;

    private final WorkManager wm;

    public static synchronized SyncManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new SyncManager(context);
        }
        return INSTANCE;
    }

    private SyncManager(Context context) {
        wm = WorkManager.getInstance(context);
        reEnqueueWork();
    }

    private void reEnqueueWork() {
        Log.d(TAG, "re-enqueued periodic work: '" + SYNC_WORKER_NAME + "'");
        wm.enqueueUniquePeriodicWork(
                SYNC_WORKER_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, getWorkRequest()
        );
    }

    public void syncNow() {
        reEnqueueWork();
    }

    private PeriodicWorkRequest getWorkRequest() {
        return new PeriodicWorkRequest.Builder(SyncWorker.class, 1, TimeUnit.HOURS)
                .setConstraints(getConstraints())
                .build();
    }

    private static Constraints getConstraints() {
        return new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.NOT_ROAMING)
                .build();
    }
}
