package com.nvllz.piggsy.util;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class BackupScheduler {

    private static final String MANUAL_WORK_NAME = "piggsy_manual_backup";
    private static final String PERIODIC_WORK_NAME = "piggsy_auto_backup";

    public static void scheduleManualExport(Context context, PreferenceUtil prefs) {

        String location = prefs.getBackupLocationUri();
        if (location == null || location.isEmpty()) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setConstraints(constraints)
                .addTag("manual_export")
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        MANUAL_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        work
                );
    }

    public static void checkAndRunIfDue(Context context, PreferenceUtil prefs) {
        int frequencyDays = prefs.getBackupFrequency();
        String location = prefs.getBackupLocationUri();

        if (frequencyDays <= 0 || location == null || location.isEmpty()) return;

        long lastBackup = prefs.getLastBackupTime();
        long now = System.currentTimeMillis();
        long frequencyMillis = TimeUnit.DAYS.toMillis(frequencyDays);

        if (prefs.isBackupPending()) {
            return;
        }

        if (now < lastBackup + frequencyMillis) {
            return;
        }

        prefs.setBackupPending(true);

        triggerOneTimeBackup(context);
    }

    private static void triggerOneTimeBackup(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setConstraints(constraints)
                .addTag("backup_and_cleanup")
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        PERIODIC_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        work
                );
    }

}