package com.nvllz.piggsy.ui.activities;

import android.content.ContentValues;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.DateFormat;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.Theme;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.saving.SavingSort;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
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
        private static final String KEY_ENCRYPT = "backup_encrypted";

        private static final int MIN_BACKUP_PASSWORD_LENGTH = 8;

        private PreferenceUtil preferences;
        private SavingRepository savingRepository;
        private TransactionRepository transactionRepository;

        private ActivityResultLauncher<Intent> backupLocationLauncher;
        private ActivityResultLauncher<Intent> exportDataLauncher;
        private ActivityResultLauncher<Intent> importLauncher;

        private boolean pendingFrequencyDialog;

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
                        if (pendingFrequencyDialog) {
                            pendingFrequencyDialog = false;
                            String persistedUri = preferences.getBackupLocationUri();
                            if (persistedUri != null && !persistedUri.isEmpty()) {
                                showFrequencyDialog();
                            } else {
                                Toast.makeText(requireContext(), R.string.select_backup_location_first, Toast.LENGTH_SHORT).show();
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

            Preference frequencyPref = findPreference(KEY_BACKUP_FREQUENCY);
            if (frequencyPref != null) {
                frequencyPref.setOnPreferenceClickListener(pref -> {
                    String locationUri = preferences.getBackupLocationUri();
                    if (locationUri == null || locationUri.isEmpty()) {
                        pendingFrequencyDialog = true;
                        promptForBackupLocation();
                    } else {
                        showFrequencyDialog();
                    }
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
                launchManualExportPicker();
                return true;
            });

            SwitchPreferenceCompat encryptSwitch = findPreference(KEY_ENCRYPT);
            if (encryptSwitch != null) {
                encryptSwitch.setOnPreferenceChangeListener((pref, newValue) -> {
                    boolean enable = (boolean) newValue;
                    if (enable == preferences.isBackupEncrypted()) return true;
                    if (enable) promptSetPassword(encryptSwitch);
                    else promptDisableEncryption(encryptSwitch);
                    return false;
                });
            }

            initializePreferences();
        }

        @Override
        public void onResume() {
            super.onResume();
            validateBackupLocationAccess();
        }

        private void validateBackupLocationAccess() {
            String locationUri = preferences.getBackupLocationUri();
            if (locationUri == null || locationUri.isEmpty()) return;

            Uri uri = Uri.parse(locationUri);
            boolean accessGranted = false;
            for (UriPermission permission : requireContext().getContentResolver().getPersistedUriPermissions()) {
                if (permission.getUri().equals(uri) && permission.isWritePermission()) {
                    accessGranted = true;
                    break;
                }
            }
            if (!accessGranted) {
                preferences.setBackupLocationUri(null);
                Toast.makeText(requireContext(), R.string.select_backup_location_first, Toast.LENGTH_SHORT).show();
                updateBackupLocationSummary(null);
                updateDependentPreferences();
            }
        }

        private void initializePreferences() {
            int retention = preferences.getBackupRetention();
            EditTextPreference retentionPref = findPreference(KEY_RETENTION);
            if (retentionPref != null) {
                retentionPref.setText(String.valueOf(retention));
            }

            String uriString = preferences.getBackupLocationUri();
            Uri uri = uriString != null && !uriString.isEmpty() ? Uri.parse(uriString) : null;
            updateBackupLocationSummary(uri);
            updateFrequencySummary();

            updateDependentPreferences();

            SwitchPreferenceCompat encryptPref = findPreference(KEY_ENCRYPT);
            if (encryptPref != null && preferences.isBackupEncrypted()) {
                Context appContext = requireContext().getApplicationContext();
                new Thread(() -> {
                    if (!BackupCrypto.isLocalConfigHealthy(appContext)) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (isAdded()) {
                                promptSetPassword(encryptPref);
                            }
                        });
                    }
                }).start();
            }
        }

        private void launchManualExportPicker() {
            String fileName = generateFileName();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, fileName);

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

            if (!locationSet && backupEnabled) {
                preferences.setBackupFrequency(0);
                backupEnabled = false;
                updateFrequencySummary();
            }

            EditTextPreference retentionPref = findPreference(KEY_RETENTION);
            if (retentionPref != null) {
                retentionPref.setVisible(backupEnabled);
                retentionPref.setEnabled(backupEnabled);
            }
        }

        private int frequencyIndex() {
            String[] values = getResources().getStringArray(R.array.backup_frequency_values);
            int current = preferences.getBackupFrequency();
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(String.valueOf(current))) {
                    return i;
                }
            }
            return 0;
        }

        private void updateFrequencySummary() {
            Preference frequencyPref = findPreference(KEY_BACKUP_FREQUENCY);
            if (frequencyPref == null) return;
            String[] entries = getResources().getStringArray(R.array.backup_frequency_entries);
            frequencyPref.setSummary(entries[frequencyIndex()]);
        }

        private void showFrequencyDialog() {
            String[] values = getResources().getStringArray(R.array.backup_frequency_values);
            String[] entries = getResources().getStringArray(R.array.backup_frequency_entries);

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.backup_frequency))
                    .setSingleChoiceItems(entries, frequencyIndex(), (dialog, which) -> {
                        preferences.setBackupFrequency(Integer.parseInt(values[which]));
                        dialog.dismiss();
                        updateFrequencySummary();
                        updateDependentPreferences();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }

        private void exportJSON(Uri uri) {
            Context context = requireContext().getApplicationContext();
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
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    Snackbar.make(requireView(), finalExported ? R.string.snackbar_export_successful : R.string.cannot_open_file, Snackbar.LENGTH_SHORT).show();
                });
            }).start();
        }

        private static final class BackupFileTooLargeException extends IOException {
            BackupFileTooLargeException(String message) {
                super(message);
            }
        }

        private void importJSON(Uri uri) {
            ContentResolver resolver = requireContext().getContentResolver();
            new Thread(() -> {
                try {
                    byte[] fileBytes = readBackupBytes(resolver, uri);
                    boolean encrypted = BackupCrypto.isEncryptedBackup(fileBytes);
                    String plaintext = encrypted ? null : new String(fileBytes, StandardCharsets.UTF_8);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!isAdded()) {
                            return;
                        }
                        if (encrypted) {
                            promptForBackupPassword(fileBytes);
                        } else {
                            applyImport(plaintext);
                        }
                    });
                } catch (BackupFileTooLargeException tooLarge) {
                    Log.w(TAG, "Backup import rejected: file too large", tooLarge);
                    postSnackbar(R.string.backup_file_too_large);
                } catch (OutOfMemoryError outOfMemory) {
                    Log.e(TAG, "Backup import ran out of memory", outOfMemory);
                    postSnackbar(R.string.backup_file_too_large);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot open import file", e);
                    postSnackbar(R.string.cannot_open_file);
                }
            }).start();
        }

        private void postSnackbar(int messageRes) {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded() && getView() != null) {
                    Snackbar.make(requireView(), messageRes, Snackbar.LENGTH_SHORT).show();
                }
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

        private void promptForBackupPassword(byte[] fileBytes) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_backup_password, null);
            TextInputLayout confirmLayout = view.findViewById(R.id.backup_password_confirm_layout);
            confirmLayout.setVisibility(View.GONE);
            TextInputEditText input = view.findViewById(R.id.backup_password_input);

            androidx.appcompat.app.AlertDialog dialog =
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.backup_encrypted_file_title))
                            .setView(view)
                            .setPositiveButton(getString(R.string.import_data), null)
                            .setNegativeButton(getString(R.string.cancel), null)
                            .create();

            dialog.setOnShowListener(d -> {
                input.requestFocus();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

                View positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                View negative = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
                View progress = view.findViewById(R.id.backup_password_progress);

                positive.setOnClickListener(v -> {
                    Context context = requireContext();
                    char[] password = passwordChars(input);
                    boolean handedOff = false;
                    try {
                        positive.setEnabled(false);
                        negative.setEnabled(false);
                        input.setEnabled(false);
                        progress.setVisibility(View.VISIBLE);
                        new Thread(() -> {
                            try {
                                String json = BackupCrypto.decryptBackup(fileBytes, password);
                                if (!BackupCrypto.isLocalConfigHealthy(context)) {
                                    BackupCrypto.repairWithPassword(context, password);
                                }
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (!isAdded() || !dialog.isShowing()) {
                                        return;
                                    }
                                    dialog.dismiss();
                                    applyImport(json);
                                });
                            } catch (javax.crypto.AEADBadTagException tagFailure) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    positive.setEnabled(true);
                                    negative.setEnabled(true);
                                    input.setEnabled(true);
                                    progress.setVisibility(View.GONE);
                                    input.requestFocus();
                                    Toast.makeText(context, R.string.backup_password_wrong, Toast.LENGTH_SHORT).show();
                                });
                            } catch (Exception | OutOfMemoryError failure) {
                                Log.e(TAG, "Cannot decrypt backup", failure);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    positive.setEnabled(true);
                                    negative.setEnabled(true);
                                    input.setEnabled(true);
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(context, R.string.cannot_open_file, Toast.LENGTH_SHORT).show();
                                });
                            } finally {
                                Arrays.fill(password, '\0');
                            }
                        }).start();
                        handedOff = true;
                    } finally {
                        if (!handedOff) {
                            Arrays.fill(password, '\0');
                        }
                    }
                });
            });
            dialog.show();
        }

        private void promptSetPassword(SwitchPreferenceCompat encryptSwitch) {
            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_backup_password, null);
            TextInputEditText input = view.findViewById(R.id.backup_password_input);
            TextInputEditText confirm = view.findViewById(R.id.backup_password_confirm_input);
            TextInputLayout confirmLayout = view.findViewById(R.id.backup_password_confirm_layout);

            androidx.appcompat.app.AlertDialog dialog =
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.backup_encryption_enable_title))
                            .setMessage(getString(R.string.backup_encryption_enable_message))
                            .setView(view)
                            .setPositiveButton(getString(R.string.backup_password_set_button), null)
                            .setNegativeButton(getString(R.string.cancel), null)
                            .create();

            dialog.setOnShowListener(d -> {
                input.requestFocus();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

                View positive = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                View negative = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
                View progress = view.findViewById(R.id.backup_password_progress);

                TextWatcher lengthWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        confirmLayout.setError(null);
                        positive.setEnabled(input.length() == confirm.length());
                    }
                };
                input.addTextChangedListener(lengthWatcher);
                confirm.addTextChangedListener(lengthWatcher);

                positive.setOnClickListener(v -> {
                    char[] password = passwordChars(input);
                    char[] confirmChars = passwordChars(confirm);
                    boolean handedOff = false;
                    try {
                        if (password.length == 0) {
                            Toast.makeText(requireContext(), R.string.backup_password_empty, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (password.length < MIN_BACKUP_PASSWORD_LENGTH) {
                            Toast.makeText(requireContext(), getString(R.string.backup_password_too_short, MIN_BACKUP_PASSWORD_LENGTH), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!Arrays.equals(password, confirmChars)) {
                            confirmLayout.setError(getString(R.string.backup_password_mismatch));
                            return;
                        }

                        Context context = requireContext();
                        positive.setEnabled(false);
                        negative.setEnabled(false);
                        input.setEnabled(false);
                        confirm.setEnabled(false);
                        progress.setVisibility(View.VISIBLE);
                        handedOff = true;

                        new Thread(() -> {
                            try {
                                BackupCrypto.setBackupPassword(context, password);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    dialog.dismiss();
                                    encryptSwitch.setChecked(preferences.isBackupEncrypted());
                                    if (getView() != null) {
                                        Snackbar.make(getView(), R.string.backup_encrypted_snackbar, Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to set up backup encryption", e);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    positive.setEnabled(true);
                                    negative.setEnabled(true);
                                    input.setEnabled(true);
                                    confirm.setEnabled(true);
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(context, R.string.backup_encryption_failed, Toast.LENGTH_SHORT).show();
                                });
                            } finally {
                                Arrays.fill(password, '\0');
                                Arrays.fill(confirmChars, '\0');
                            }
                        }).start();
                    } finally {
                        if (!handedOff) {
                            Arrays.fill(password, '\0');
                            Arrays.fill(confirmChars, '\0');
                        }
                    }
                });
            });
            dialog.show();
        }

        private void promptDisableEncryption(SwitchPreferenceCompat encryptSwitch) {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.backup_encryption_disable_title))
                    .setMessage(getString(R.string.backup_encryption_disable_message))
                    .setPositiveButton(getString(R.string.backup_encryption_disable_button), (dialog, which) -> {
                        BackupCrypto.clearBackupPassword(requireContext());
                        encryptSwitch.setChecked(false);
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }

        private char[] passwordChars(EditText input) {
            int length = input.length();
            char[] chars = new char[length];
            if (length > 0) {
                input.getText().getChars(0, length, chars, 0);
            }
            return chars;
        }

        private void applyImport(String json) {
            Context context = requireContext();
            new Thread(() -> {
                boolean imported = false;
                try {
                    performImport(context, json);
                    imported = true;
                } catch (Exception | OutOfMemoryError e) {
                    Log.e(TAG, "Cannot import backup", e);
                }
                boolean finalImported = imported;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (finalImported) {
                        if (getView() != null) {
                            Snackbar.make(requireView(), R.string.snackbar_import_successful, Snackbar.LENGTH_SHORT).show();
                        }
                        restartApp();
                    } else if (getView() != null) {
                        Snackbar.make(requireView(), R.string.cannot_open_file, Snackbar.LENGTH_SHORT).show();
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
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finishAffinity();
        }

        private String generateFileName() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
            return "piggsy_" + sdf.format(new Date()) + ".json";
        }
    }
}