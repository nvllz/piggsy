package com.nvllz.piggsy.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONObject;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupWorker extends Worker {

    private static final String TAG = "BackupWorker";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        PreferenceUtil prefs = new PreferenceUtil(getApplicationContext());
        String uriString = prefs.getBackupLocationUri();

        if (uriString == null || uriString.isEmpty()) {
            Log.e(TAG, "No backup location set");
            return Result.failure();
        }

        Uri uri = Uri.parse(uriString);
        boolean isManual = getTags().contains("manual_export");

        if (!isManual && prefs.getBackupFrequency() <= 0) {
            return Result.success();
        }

        try {
            boolean success = performBackup(uri);

            if (!success) {
                prefs.setBackupPending(false);
                return Result.retry();
            }

            prefs.setLastBackupTime(System.currentTimeMillis());
            prefs.setBackupPending(false);

            if (getTags().contains("backup_and_cleanup")) {
                cleanupOldBackups(uri, prefs);
            }

            return Result.success();

        } catch (Exception e) {
            prefs.setBackupPending(false);
            return Result.retry();
        }
    }

    private boolean performBackup(Uri uri) {
        Context context = getApplicationContext();

        try {
            try {
                context.getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } catch (Exception e) {
                Log.w(TAG, "Permission already granted or failed: " + e.getMessage());
            }

            DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
            if (dir == null || !dir.exists() || !dir.canWrite()) {
                Log.e(TAG, "Invalid backup directory");
                return false;
            }

            String fileName = generateFileName();

            DocumentFile file = dir.createFile("application/json", fileName);
            if (file == null) {
                Log.e(TAG, "Failed to create backup file");
                return false;
            }

            JSONObject root = BackupJsonExporter.export(context);

            ContentResolver resolver = context.getContentResolver();

            try (OutputStream os = resolver.openOutputStream(file.getUri())) {
                if (os == null) {
                    Log.e(TAG, "Output stream is null");
                    return false;
                }
                os.write(root.toString().getBytes());
                os.flush();
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Backup failed", e);
            return false;
        }
    }

    private void cleanupOldBackups(Uri uri, PreferenceUtil prefs) {
        try {
            int retention = prefs.getBackupRetention();
            if (retention <= 0) return;

            Context context = getApplicationContext();
            DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
            if (dir == null) return;

            DocumentFile[] files = dir.listFiles();

            java.util.ArrayList<DocumentFile> backups = new java.util.ArrayList<>();

            for (DocumentFile file : files) {
                if (file.isFile()
                        && file.getName() != null
                        && file.getName().startsWith("piggsy_")
                        && file.getName().endsWith(".json")) {
                    backups.add(file);
                }
            }

            backups.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));

            if (backups.size() <= retention) return;

            java.util.List<DocumentFile> toDelete = backups.subList(retention, backups.size());

            for (DocumentFile file : toDelete) {
                String name = file.getName();
                try {
                    file.delete();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete: " + name, e);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Cleanup failed", e);
        }
    }

    private String generateFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
        return "piggsy_" + sdf.format(new Date()) + ".json";
    }
}