package com.nvllz.piggsy.ui.activities;

import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.DateFormat;
import com.nvllz.piggsy.data.Theme;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.saving.SavingSort;
import com.nvllz.piggsy.ui.ComposeBackupView;
import com.nvllz.piggsy.ui.components.ToastKind;
import com.nvllz.piggsy.util.AlarmUtil;
import com.nvllz.piggsy.util.BackupCrypto;
import com.nvllz.piggsy.util.BackupJsonExporter;
import com.nvllz.piggsy.util.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;


public class BackupActivity extends BaseActivity {

    private static final String TAG = "BackupActivity";

    private ComposeBackupView backupView;

    private SavingRepository savingRepository;

    private ActivityResultLauncher<Intent> backupLocationLauncher;
    private ActivityResultLauncher<Intent> exportDataLauncher;
    private ActivityResultLauncher<Intent> importLauncher;


    private boolean pendingFrequencyDialog;


    private byte[] pendingEncryptedImport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        savingRepository = new SavingRepository(this);

        backupView = new ComposeBackupView(this, new ComposeBackupView.Listener() {
            @Override
            public void onBack() {
                finish();
            }

            @Override
            public void onImportClicked() {
                if (hasSavings()) {
                    backupView.showImportWarning();
                } else {
                    launchImportPicker();
                }
            }

            @Override
            public void onImportWarningConfirmed() {
                launchImportPicker();
            }

            @Override
            public void onFrequencyClicked() {
                String locationUri = preferences.getBackupLocationUri();
                if (locationUri == null || locationUri.isEmpty()) {
                    pendingFrequencyDialog = true;
                    promptForBackupLocation();
                } else {
                    backupView.showFrequencyDialog();
                }
            }

            @Override
            public void onBackupLocationClicked() {
                promptForBackupLocation();
            }

            @Override
            public void onManualBackupClicked() {
                launchManualExportPicker();
            }

            @Override
            public void onSetBackupPassword(char[] password) {
                new Thread(() -> {
                    boolean success = false;
                    try {
                        BackupCrypto.setBackupPassword(BackupActivity.this, password);
                        success = true;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to set up backup encryption", e);
                    } finally {
                        Arrays.fill(password, '\0');
                    }
                    boolean finalSuccess = success;
                    runOnUiThread(() -> backupView.onBackupPasswordSet(finalSuccess));
                }).start();
            }

            @Override
            public void onDisableEncryptionConfirmed() {
                BackupCrypto.clearBackupPassword(BackupActivity.this);
                backupView.refresh();
                backupView.showToast(
                        getString(R.string.backup_encryption_disabled_snackbar),
                        ToastKind.INFO);
            }

            @Override
            public void onImportPasswordSubmitted(char[] password) {
                new Thread(() -> {
                    try {
                        String json = BackupCrypto.decryptBackup(pendingEncryptedImport, password);
                        if (!BackupCrypto.isLocalConfigHealthy(BackupActivity.this)) {
                            BackupCrypto.repairWithPassword(BackupActivity.this, password);
                        }
                        pendingEncryptedImport = null;
                        runOnUiThread(() -> {
                            backupView.onImportPasswordAccepted();
                            applyImport(json);
                        });
                    } catch (javax.crypto.AEADBadTagException tagFailure) {
                        runOnUiThread(() -> backupView.onPasswordWrong());
                    } catch (Exception | OutOfMemoryError failure) {
                        Log.e(TAG, "Cannot decrypt backup", failure);
                        runOnUiThread(() -> backupView.onImportFailed());
                    } finally {
                        Arrays.fill(password, '\0');
                    }
                }).start();
            }
        });
        setContentView(backupView);

        backupLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Uri uri = result.getData() != null ? result.getData().getData() : null;
                    if (result.getResultCode() == RESULT_OK && uri != null) {
                        persistBackupLocation(uri);
                        backupView.refresh();
                    }
                    if (pendingFrequencyDialog) {
                        pendingFrequencyDialog = false;
                        String persistedUri = preferences.getBackupLocationUri();
                        if (persistedUri != null && !persistedUri.isEmpty()) {
                            backupView.showFrequencyDialog();
                        } else {
                            backupView.showToast(getString(R.string.select_backup_location_first), ToastKind.INFO);
                        }
                    }
                });

        exportDataLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) exportJSON(uri);
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) importJSON(uri);
                    }
                });

        if (preferences.isBackupEncrypted()) {
            new Thread(() -> {
                if (!BackupCrypto.isLocalConfigHealthy(getApplicationContext())) {
                    runOnUiThread(() -> {
                        if (!isDestroyed()) backupView.showSetPasswordDialog();
                    });
                }
            }).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        validateBackupLocationAccess();
    }

    private boolean hasSavings() {
        try (Database database = new Database(this)) {
            android.database.Cursor cursor = database.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*) FROM " + Database.TABLE_SAVING, null);
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                cursor.close();
                return count > 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check savings count", e);
        }
        return false;
    }

    private void validateBackupLocationAccess() {
        String locationUri = preferences.getBackupLocationUri();
        if (locationUri == null || locationUri.isEmpty()) return;

        Uri uri = Uri.parse(locationUri);
        boolean accessGranted = false;
        for (UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
            if (permission.getUri().equals(uri) && permission.isWritePermission()) {
                accessGranted = true;
                break;
            }
        }
        if (!accessGranted) {
            preferences.setBackupLocationUri(null);

            preferences.setBackupFrequency(0);
            backupView.showToast(getString(R.string.select_backup_location_first), ToastKind.INFO);
            backupView.refresh();
        }
    }

    private void launchImportPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        importLauncher.launch(intent);
    }

    private void launchManualExportPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, generateFileName());

        String locationUri = preferences.getBackupLocationUri();
        if (locationUri != null && !locationUri.isEmpty()) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(locationUri));
        }

        exportDataLauncher.launch(intent);
    }

    private void promptForBackupLocation() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        backupLocationLauncher.launch(intent);
    }

    private void persistBackupLocation(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            preferences.setBackupLocationUri(uri.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist URI permission", e);
        }
    }

    private void exportJSON(Uri uri) {
        Context context = getApplicationContext();
        ContentResolver resolver = context.getContentResolver();
        new Thread(() -> {
            boolean exported = false;
            try {
                JSONObject jsonExport = BackupJsonExporter.export(context);

                byte[] payload = jsonExport.toString().getBytes(StandardCharsets.UTF_8);
                if (preferences.isBackupEncrypted()) {
                    payload = BackupCrypto.encryptBackup(context, payload);
                }

                OutputStream outputStream = null;
                try {
                    outputStream = resolver.openOutputStream(uri);
                    if (outputStream == null) {
                        throw new IOException("Cannot open output stream");
                    }
                    outputStream.write(payload);
                    outputStream.flush();
                } finally {
                    if (outputStream != null) outputStream.close();
                }
                exported = true;
            } catch (Exception | OutOfMemoryError e) {
                Log.e(TAG, "Error exporting JSON", e);
            }
            boolean finalExported = exported;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                backupView.showToast(
                        getString(finalExported ? R.string.snackbar_export_successful : R.string.cannot_open_file),
                        finalExported ? ToastKind.SUCCESS : ToastKind.ERROR);
            });
        }).start();
    }

    private static final class BackupFileTooLargeException extends IOException {
        BackupFileTooLargeException(String message) {
            super(message);
        }
    }

    private void importJSON(Uri uri) {
        ContentResolver resolver = getContentResolver();
        new Thread(() -> {
            try {
                byte[] fileBytes = readBackupBytes(resolver, uri);
                boolean encrypted = BackupCrypto.isEncryptedBackup(fileBytes);
                String plaintext = encrypted ? null : new String(fileBytes, StandardCharsets.UTF_8);
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    if (encrypted) {
                        pendingEncryptedImport = fileBytes;
                        backupView.showImportPasswordDialog();
                    } else {
                        applyImport(plaintext);
                    }
                });
            } catch (BackupFileTooLargeException tooLarge) {
                Log.w(TAG, "Backup import rejected: file too large", tooLarge);
                postToast(R.string.backup_file_too_large);
            } catch (OutOfMemoryError outOfMemory) {
                Log.e(TAG, "Backup import ran out of memory", outOfMemory);
                postToast(R.string.backup_file_too_large);
            } catch (Exception e) {
                Log.e(TAG, "Cannot open import file", e);
                postToast(R.string.cannot_open_file);
            }
        }).start();
    }

    private void postToast(int messageRes) {
        runOnUiThread(() -> {
            if (isDestroyed()) return;
            backupView.showToast(getString(messageRes), ToastKind.ERROR);
        });
    }

    private byte[] readBackupBytes(ContentResolver resolver, Uri uri) throws Exception {
        InputStream inputStream = resolver.openInputStream(uri);
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            int total = 0;
            if (inputStream != null) {
                while ((read = inputStream.read(chunk)) != -1) {
                    total += read;
                    if (total > BackupCrypto.MAX_IMPORT_BYTES) {
                        throw new BackupFileTooLargeException("Backup file exceeds the maximum supported size");
                    }
                    buffer.write(chunk, 0, read);
                }
            }
            return buffer.toByteArray();
        } finally {
            if (inputStream != null) inputStream.close();
        }
    }

    private void applyImport(String json) {
        new Thread(() -> {
            boolean imported = false;
            try {
                performImport(this, json);
                imported = true;
            } catch (Exception | OutOfMemoryError e) {
                Log.e(TAG, "Cannot import backup", e);
            }
            boolean finalImported = imported;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (finalImported) {
                    backupView.showToast(getString(R.string.snackbar_import_successful), ToastKind.SUCCESS);
                    restartApp();
                } else {
                    backupView.showToast(getString(R.string.cannot_open_file), ToastKind.ERROR);
                }
            });
        }).start();
    }

    private void performImport(Context context, String json) throws Exception {
        JSONObject jsonImport = new JSONObject(json);
        JSONArray savingJsonArray = jsonImport.getJSONArray(Database.TABLE_SAVING);
        validateImportedSavings(savingJsonArray);

        HashMap<String, Saving> staleAlarmCandidates = new HashMap<>();
        for (Saving existing : savingRepository.getAllSavings()) {
            if (existing.getDeadline() != AlarmUtil.NO_ALARM) {
                staleAlarmCandidates.put(existing.getID(), existing);
            }
        }

        try (Database database = new Database(context)) {
            SQLiteDatabase writableDatabase = database.getWritableDatabase();
            ArrayList<Saving> scheduledAlarms = new ArrayList<>();
            HashSet<String> importedIds = new HashSet<>();
            boolean committed = false;
            try {
                writableDatabase.beginTransaction();
                try {
                    writableDatabase.delete(Database.TABLE_TRANSACTION, null, null);
                    writableDatabase.delete(Database.TABLE_SAVING, null, null);

                    for (int i = 0; i < savingJsonArray.length(); i++) {
                        JSONObject obj = savingJsonArray.getJSONObject(i);
                        ContentValues values = new ContentValues();

                        String savingId = obj.getString(Database.COLUMN_SAVING_ID);
                        long deadline = obj.getLong(Database.COLUMN_SAVING_DEADLINE);
                        String currency = obj.optString(Database.COLUMN_SAVING_CURRENCY, "USD");

                        values.put(Database.COLUMN_SAVING_ID, savingId);
                        values.put(Database.COLUMN_SAVING_NAME, obj.getString(Database.COLUMN_SAVING_NAME));
                        values.put(Database.COLUMN_SAVING_CURRENT_SAVING, obj.getDouble(Database.COLUMN_SAVING_CURRENT_SAVING));
                        values.put(Database.COLUMN_SAVING_GOAL, obj.getDouble(Database.COLUMN_SAVING_GOAL));
                        values.put(Database.COLUMN_SAVING_CURRENCY, currency);

                        String description = obj.optString(Database.COLUMN_SAVING_DESCRIPTION,
                                obj.optString("notes", ""));
                        values.put(Database.COLUMN_SAVING_DESCRIPTION, description);

                        int isArchived = obj.getInt(Database.COLUMN_SAVING_IS_ARCHIVED);
                        values.put(Database.COLUMN_SAVING_IS_ARCHIVED, isArchived);
                        values.put(Database.COLUMN_SAVING_DEADLINE, deadline);

                        if (writableDatabase.insert(Database.TABLE_SAVING, null, values) == -1) {
                            throw new IllegalStateException("Failed to import saving " + savingId);
                        }
                        importedIds.add(savingId);

                        if (deadline != AlarmUtil.NO_ALARM) {
                            Saving scheduled = new Saving();
                            scheduled.setID(savingId);
                            scheduled.setName(obj.getString(Database.COLUMN_SAVING_NAME));
                            scheduled.setDeadline(deadline);
                            scheduled.setIsArchived(isArchived);
                            scheduledAlarms.add(scheduled);
                        }

                        JSONArray savingTransactions = obj.optJSONArray(BackupJsonExporter.KEY_TRANSACTIONS);
                        if (savingTransactions != null) {
                            for (int j = 0; j < savingTransactions.length(); j++) {
                                JSONObject tObj = savingTransactions.getJSONObject(j);
                                ContentValues tValues = new ContentValues();

                                tValues.put(Database.COLUMN_TRANSACTION_SAVING_ID, savingId);
                                tValues.put(Database.COLUMN_TRANSACTION_AMOUNT, tObj.getDouble(Database.COLUMN_TRANSACTION_AMOUNT));
                                tValues.put(Database.COLUMN_TRANSACTION_TYPE, tObj.getString(Database.COLUMN_TRANSACTION_TYPE));
                                tValues.put(Database.COLUMN_TRANSACTION_DATE, tObj.getLong(Database.COLUMN_TRANSACTION_DATE));
                                tValues.put(Database.COLUMN_TRANSACTION_NOTE, tObj.optString(Database.COLUMN_TRANSACTION_NOTE, ""));

                                if (writableDatabase.insert(Database.TABLE_TRANSACTION, null, tValues) == -1) {
                                    throw new IllegalStateException("Failed to import transaction for saving " + savingId);
                                }
                            }
                        }
                    }

                    writableDatabase.setTransactionSuccessful();
                } finally {
                    writableDatabase.endTransaction();
                }
                committed = true;
            } finally {
                if (!committed) {
                    scheduledAlarms.clear();
                }
            }

            for (Saving stale : staleAlarmCandidates.values()) {
                if (!importedIds.contains(stale.getID())) {
                    try {
                        AlarmUtil.cancel(context, stale);
                    } catch (Exception alarmFailure) {
                        Log.e(TAG, "Failed to cancel stale alarm", alarmFailure);
                    }
                }
            }
            for (Saving toSchedule : scheduledAlarms) {
                try {
                    AlarmUtil.cancel(context, toSchedule);
                    AlarmUtil.set(context, toSchedule);
                } catch (Exception alarmFailure) {
                    Log.e(TAG, "Failed to schedule alarm", alarmFailure);
                }
            }
        }

        JSONObject settings = jsonImport.optJSONObject(BackupJsonExporter.KEY_SETTINGS);
        if (settings != null) {
            try {
                restoreSettings(context, settings);
            } catch (Exception settingsFailure) {
                Log.e(TAG, "Failed to restore settings", settingsFailure);
            }
        }
    }

    private static void requireFinite(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Non-finite number in backup");
        }
    }

    private void restoreSettings(Context context, JSONObject settings) {
        PreferenceUtil prefs = new PreferenceUtil(context);

        String dateFormat = settings.optString("date_format", null);
        if (dateFormat != null && isKnownValue(DateFormat.getPatterns(), dateFormat)) {
            prefs.setDateFormat(dateFormat);
        }

        String theme = settings.optString("theme", null);
        if (theme != null && isKnownValue(Theme.getValues(), theme)) {
            prefs.setTheme(theme);
        }

        if (settings.has("dynamic_colors")) {
            prefs.setDynamicColors(settings.optBoolean("dynamic_colors", false));
        }

        if (settings.has("screen_privacy")) {
            prefs.setScreenPrivacy(settings.optBoolean("screen_privacy", false));
        }

        String[] sortValues = new String[SavingSort.values().length];
        for (int i = 0; i < sortValues.length; i++) {
            sortValues[i] = SavingSort.values()[i].SORT;
        }
        String sortCriteria = settings.optString("sort_criteria", null);
        if (sortCriteria != null && isKnownValue(sortValues, sortCriteria)) {
            prefs.setSortCriteria(sortCriteria);
        }

        if (settings.has("sort_order")) {
            prefs.setSortOrder(settings.optBoolean("sort_order", true));
        }
    }

    private static boolean isKnownValue(String[] valid, String value) {
        for (String candidate : valid) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static void validateImportedSavings(JSONArray savings) throws Exception {
        HashSet<String> ids = new HashSet<>();
        for (int i = 0; i < savings.length(); i++) {
            JSONObject obj = savings.getJSONObject(i);
            String id = obj.getString(Database.COLUMN_SAVING_ID);
            if (id.isEmpty() || !ids.add(id)) {
                throw new IllegalArgumentException("Invalid or duplicate saving id in backup");
            }
            obj.getString(Database.COLUMN_SAVING_NAME);
            requireFinite(obj.getDouble(Database.COLUMN_SAVING_CURRENT_SAVING));
            requireFinite(obj.getDouble(Database.COLUMN_SAVING_GOAL));
            obj.getLong(Database.COLUMN_SAVING_DEADLINE);
            int isArchived = obj.getInt(Database.COLUMN_SAVING_IS_ARCHIVED);
            if (isArchived != 0 && isArchived != 1) {
                throw new IllegalArgumentException("Invalid archived flag in backup");
            }
            JSONArray transactions = obj.optJSONArray(BackupJsonExporter.KEY_TRANSACTIONS);
            if (transactions != null) {
                for (int j = 0; j < transactions.length(); j++) {
                    JSONObject t = transactions.getJSONObject(j);
                    requireFinite(t.getDouble(Database.COLUMN_TRANSACTION_AMOUNT));
                    t.getString(Database.COLUMN_TRANSACTION_TYPE);
                    t.getLong(Database.COLUMN_TRANSACTION_DATE);
                }
            }
        }
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private String generateFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
        return "piggsy_" + sdf.format(new Date()) + ".json";
    }
}
