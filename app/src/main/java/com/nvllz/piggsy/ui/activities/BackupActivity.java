package com.nvllz.piggsy.ui.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
import com.nvllz.piggsy.util.AlarmUtil;
import com.nvllz.piggsy.util.BackupJsonExporter;
import com.nvllz.piggsy.util.BackupScheduler;
import com.nvllz.piggsy.util.PreferenceUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class BackupActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.preference_category_backup));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.backup_container, new BackupPreferenceFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class BackupPreferenceFragment extends PreferenceFragmentCompat {

        private static final String TAG = "BackupPrefFragment";

        private static final String KEY_IMPORT = "import";
        private static final String KEY_BACKUP_LOCATION = "backup_location";
        private static final String KEY_BACKUP_FREQUENCY = "backup_frequency";
        private static final String KEY_RETENTION = "backup_retention_count";
        private static final String KEY_MANUAL_BACKUP = "manual_backup";

        private PreferenceUtil preferences;
        private SavingRepository savingRepository;
        private TransactionRepository transactionRepository;

        private ActivityResultLauncher<Intent> backupLocationLauncher;
        private ActivityResultLauncher<Intent> exportDataLauncher;
        private ActivityResultLauncher<Intent> importLauncher;

        private void clearAllData() {
            try (Database database = new Database(requireContext())) {
                SQLiteDatabase db = database.getWritableDatabase();

                db.beginTransaction();
                try {
                    db.delete(Database.TABLE_TRANSACTION, null, null);
                    db.delete(Database.TABLE_SAVING, null, null);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

            } catch (Exception ignored) {
            }
        }

        private void launchImportPicker() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            importLauncher.launch(intent);
        }

        private boolean hasSavings() {
            try (Database database = new Database(requireContext())) {
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

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.backup_preferences, rootKey);

            preferences = new PreferenceUtil(requireContext());
            savingRepository = new SavingRepository(requireContext());
            transactionRepository = new TransactionRepository(requireContext());

            backupLocationLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) {
                                persistBackupLocation(uri);
                                updateBackupLocationSummary(uri);
                                updateDependentPreferences();
                            }
                        }
                    });

            exportDataLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) exportJSON(uri);
                        }
                    });

            importLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) importJSON(uri);
                        }
                    });

            findPreference(KEY_IMPORT).setOnPreferenceClickListener(pref -> {
                if (hasSavings()) {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.warning))
                            .setMessage(getString(R.string.import_warning_desc))
                            .setPositiveButton(getString(R.string.import_data), (dialog, which) -> launchImportPicker())
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show();
                } else {
                    launchImportPicker();
                }
                return true;
            });

            findPreference(KEY_BACKUP_LOCATION).setOnPreferenceClickListener(pref -> {
                promptForBackupLocation();
                return true;
            });

            ListPreference frequencyPref = findPreference(KEY_BACKUP_FREQUENCY);
            if (frequencyPref != null) {
                frequencyPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    int frequency = Integer.parseInt(newValue.toString());
                    preferences.setBackupFrequency(frequency);
                    updateDependentPreferences();
                    return true;
                });
            }

            EditTextPreference retentionPref = findPreference(KEY_RETENTION);
            if (retentionPref != null) {
                retentionPref.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    editText.setKeyListener(DigitsKeyListener.getInstance("0123456789"));
                    editText.setSelection(editText.getText().length());
                    editText.setHint(getString(R.string.backup_retention_hint));
                });
                retentionPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    try {
                        int retention = Integer.parseInt(newValue.toString());
                        if (retention >= 0) {
                            preferences.setBackupRetention(retention);
                            return true;
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(requireContext(), R.string.enter_valid_value, Toast.LENGTH_SHORT).show();
                    return false;
                });
            }

            findPreference(KEY_MANUAL_BACKUP).setOnPreferenceClickListener(pref -> {
                String locationUri = preferences.getBackupLocationUri();
                if (locationUri == null || locationUri.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.select_backup_location_first, Toast.LENGTH_SHORT).show();
                    promptForBackupLocation();
                    return true;
                }
                BackupScheduler.scheduleManualExport(requireContext(), preferences);
                Snackbar.make(requireView(), R.string.snackbar_export_successful, Snackbar.LENGTH_SHORT).show();
                return true;
            });

            initializePreferences();
        }

        private void initializePreferences() {
            int frequency = preferences.getBackupFrequency();
            ListPreference frequencyPref = findPreference(KEY_BACKUP_FREQUENCY);
            if (frequencyPref != null) {
                frequencyPref.setValue(String.valueOf(frequency));
            }

            int retention = preferences.getBackupRetention();
            EditTextPreference retentionPref = findPreference(KEY_RETENTION);
            if (retentionPref != null) {
                retentionPref.setText(String.valueOf(retention));
            }

            String uriString = preferences.getBackupLocationUri();
            Uri uri = uriString != null && !uriString.isEmpty() ? Uri.parse(uriString) : null;
            updateBackupLocationSummary(uri);

            updateDependentPreferences();
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
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                preferences.setBackupLocationUri(uri.toString());
            } catch (Exception e) {
                Log.e(TAG, "Failed to persist URI permission", e);
            }
        }

        private void updateBackupLocationSummary(Uri uri) {
            Preference locationPref = findPreference(KEY_BACKUP_LOCATION);
            if (locationPref == null) return;

            if (uri == null) {
                locationPref.setSummary(getString(R.string.backup_location_not_set));
                return;
            }

            String displayPath = "";
            try {
                String treeId = DocumentsContract.getTreeDocumentId(uri);
                if (treeId.contains(":")) {
                    displayPath = treeId.substring(treeId.indexOf(':') + 1);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not resolve display path", e);
            }

            locationPref.setSummary(displayPath.isEmpty()
                    ? uri.getLastPathSegment()
                    : displayPath);
        }

        private void updateDependentPreferences() {
            String locationUri = preferences.getBackupLocationUri();
            boolean locationSet = locationUri != null && !locationUri.isEmpty();
            int frequency = preferences.getBackupFrequency();
            boolean backupEnabled = frequency > 0;

            ListPreference frequencyPref = findPreference(KEY_BACKUP_FREQUENCY);
            if (frequencyPref != null) frequencyPref.setEnabled(locationSet);

            EditTextPreference retentionPref = findPreference(KEY_RETENTION);
            if (retentionPref != null) retentionPref.setEnabled(backupEnabled);

            Preference manualPref = findPreference(KEY_MANUAL_BACKUP);
            if (manualPref != null) manualPref.setEnabled(locationSet);
        }

        private void exportJSON(Uri uri) {
            try {
                JSONObject jsonExport = BackupJsonExporter.export(requireContext());

                OutputStream outputStream =
                        requireContext().getContentResolver().openOutputStream(uri);

                if (outputStream != null) {
                    outputStream.write(jsonExport.toString().getBytes());
                    outputStream.close();
                }

                Snackbar.make(requireView(),
                        R.string.snackbar_export_successful,
                        Snackbar.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Error exporting JSON", e);
                Snackbar.make(requireView(),
                        R.string.cannot_open_file,
                        Snackbar.LENGTH_SHORT).show();
            }
        }

        private void importJSON(Uri uri) {
            StringBuilder jsonBuilder = new StringBuilder();

            try (Database database = new Database(requireContext())) {
                SQLiteDatabase writableDatabase = database.getWritableDatabase();

                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                reader.close();

                JSONObject jsonImport = new JSONObject(jsonBuilder.toString());
                JSONArray savingJsonArray = jsonImport.getJSONArray(Database.TABLE_SAVING);
                JSONArray transactionJsonArray = jsonImport.getJSONArray(Database.TABLE_TRANSACTION);

                try {
                    clearAllData();
                    writableDatabase.beginTransaction();

                    for (int i = 0; i < savingJsonArray.length(); i++) {
                        JSONObject obj = savingJsonArray.getJSONObject(i);
                        ContentValues values = new ContentValues();

                        long deadline = obj.getLong(Database.COLUMN_SAVING_DEADLINE);
                        String currency = obj.optString(Database.COLUMN_SAVING_CURRENCY, "USD");

                        if (deadline != AlarmUtil.NO_ALARM) {
                            Saving rescheduled = new Saving();
                            rescheduled.setID(obj.getString(Database.COLUMN_SAVING_ID));
                            rescheduled.setName(obj.getString(Database.COLUMN_SAVING_NAME));
                            rescheduled.setDeadline(deadline);
                            AlarmUtil.set(requireContext(), rescheduled);
                        }

                        values.put(Database.COLUMN_SAVING_ID, obj.getString(Database.COLUMN_SAVING_ID));
                        values.put(Database.COLUMN_SAVING_NAME, obj.getString(Database.COLUMN_SAVING_NAME));
                        values.put(Database.COLUMN_SAVING_CURRENT_SAVING, obj.getDouble(Database.COLUMN_SAVING_CURRENT_SAVING));
                        values.put(Database.COLUMN_SAVING_GOAL, obj.getDouble(Database.COLUMN_SAVING_GOAL));
                        values.put(Database.COLUMN_SAVING_CURRENCY, currency);

                        String description = obj.optString(Database.COLUMN_SAVING_DESCRIPTION,
                                obj.optString("notes", ""));
                        values.put(Database.COLUMN_SAVING_DESCRIPTION, description);

                        values.put(Database.COLUMN_SAVING_IS_ARCHIVED, obj.getInt(Database.COLUMN_SAVING_IS_ARCHIVED));
                        values.put(Database.COLUMN_SAVING_DEADLINE, deadline);

                        writableDatabase.insert(Database.TABLE_SAVING, null, values);
                    }

                    for (int i = 0; i < transactionJsonArray.length(); i++) {
                        JSONObject obj = transactionJsonArray.getJSONObject(i);
                        ContentValues values = new ContentValues();

                        values.put(Database.COLUMN_TRANSACTION_SAVING_ID, obj.getString(Database.COLUMN_TRANSACTION_SAVING_ID));
                        values.put(Database.COLUMN_TRANSACTION_AMOUNT, obj.getDouble(Database.COLUMN_TRANSACTION_AMOUNT));
                        values.put(Database.COLUMN_TRANSACTION_TYPE, obj.getString(Database.COLUMN_TRANSACTION_TYPE));
                        values.put(Database.COLUMN_TRANSACTION_DATE, obj.getLong(Database.COLUMN_TRANSACTION_DATE));
                        values.put(Database.COLUMN_TRANSACTION_NOTE, obj.optString(Database.COLUMN_TRANSACTION_NOTE, ""));

                        writableDatabase.insert(Database.TABLE_TRANSACTION, null, values);
                    }

                    writableDatabase.setTransactionSuccessful();
                    Snackbar.make(requireView(), R.string.snackbar_import_successful, Snackbar.LENGTH_SHORT).show();

                    restartApp();

                } finally {
                    writableDatabase.endTransaction();
                }

            } catch (Exception e) {
                Log.e(TAG, "Cannot open import file", e);
                Snackbar.make(requireView(), R.string.cannot_open_file, Snackbar.LENGTH_SHORT).show();
            }
        }

        private void restartApp() {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finishAffinity();
        }
    }
}