package com.nvllz.piggsy.ui.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Database;
import com.nvllz.piggsy.data.DateFormat;
import com.nvllz.piggsy.data.Theme;
import com.nvllz.piggsy.data.saving.Saving;
import com.nvllz.piggsy.data.saving.SavingRepository;
import com.nvllz.piggsy.data.transaction.Transaction;
import com.nvllz.piggsy.data.transaction.TransactionRepository;
import com.nvllz.piggsy.databinding.ActivitySettingsBinding;
import com.nvllz.piggsy.util.AlarmUtil;
import com.nvllz.piggsy.util.PreferenceUtil;
import com.google.android.material.color.DynamicColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private PreferenceUtil preferences;
        private SavingRepository savingRepository;
        private TransactionRepository transactionRepository;

        private ListPreference listDeadlineFormat;
        private ListPreference listTheme;

        private SwitchPreferenceCompat switchDynamicColors;
        private SwitchPreferenceCompat switchScreenPrivacy;

        private Preference exportSavings;
        private Preference importSavings;

        private final ActivityResultLauncher<Intent> exportDataLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            exportJSON(data.getData());
                        }
                    }
        });

        private final ActivityResultLauncher<Intent> importDataLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            importJSON(data.getData());
                        }
                    }
        });

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_settings, rootKey);
            setPreferences();

            exportSavings.setOnPreferenceClickListener(preference -> {
                exportData();
                return true;
            });

            importSavings.setOnPreferenceClickListener(preference -> {
                importData();
                return true;
            });

            listDeadlineFormat.setEntries(DateFormat.getNames());
            listDeadlineFormat.setEntryValues(DateFormat.getPatterns());
            listDeadlineFormat.setValue(preferences.getDateFormat());
            listDeadlineFormat.setSummary(DateFormat.getNameByPattern(preferences.getDateFormat()));
            listDeadlineFormat.setOnPreferenceChangeListener((preference, newValue) -> {
                preferences.setDateFormat((String) newValue);
                listDeadlineFormat.setSummary(DateFormat.getNameByPattern((String) newValue));
                restartApp();
                return true;
            });

            switchDynamicColors.setVisible(DynamicColors.isDynamicColorAvailable());
            switchDynamicColors.setChecked(preferences.isDynamicColors());
            switchDynamicColors.setOnPreferenceChangeListener((preference, isChecked) -> {
                preferences.setDynamicColors((boolean) isChecked);
                requireActivity().recreate();
                restartApp();
                return true;
            });

            switchScreenPrivacy.setChecked(preferences.isScreenPrivacyEnabled());
            switchScreenPrivacy.setOnPreferenceChangeListener((preference, isChecked) -> {
                preferences.setScreenPrivacy((boolean) isChecked);
                requireActivity().recreate();
                return true;
            });

            listTheme.setEntries(Theme.getValues());
            listTheme.setEntryValues(Theme.getValues());
            listTheme.setSummary(preferences.getTheme());
            listTheme.setValue(preferences.getTheme());
            listTheme.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedTheme = (String) newValue;
                if (selectedTheme.equals(Theme.SYSTEM.VALUE)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                if (selectedTheme.equals(Theme.LIGHT.VALUE)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                if (selectedTheme.equals(Theme.DARK.VALUE)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                preferences.setTheme(selectedTheme);
                return true;
            });
        }

        private boolean noSavingsFound() {
            try (SavingRepository savingRepository = new SavingRepository(requireContext())) {
                ArrayList<Saving> savings = new ArrayList<>(savingRepository.getAllSavings());
                if (savings.isEmpty()) return true;
            }
            return false;
        }

        private String generateExportFilename() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
            String timestamp = dateFormat.format(new Date());
            return "piggsy_" + timestamp + ".json";
        }

        private void exportData() {
            if (noSavingsFound()) {
                Snackbar.make(requireView(), R.string.toast_export_no_savings_found, Snackbar.LENGTH_SHORT).show();
            } else {
                Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
                exportIntent.setType("application/json");
                exportIntent.putExtra(Intent.EXTRA_TITLE, generateExportFilename());
                exportDataLauncher.launch(exportIntent);
            }
        }

        private void importData() {
            Intent importIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            importIntent.addCategory(Intent.CATEGORY_OPENABLE);
            importIntent.setType("application/json");
            importDataLauncher.launch(importIntent);
        }

        private void setPreferences() {
            preferences = new PreferenceUtil(requireContext());
            savingRepository = new SavingRepository(requireContext());
            transactionRepository = new TransactionRepository(requireContext());

            listDeadlineFormat = findPreference("deadline_format");
            listTheme = findPreference("theme");

            switchDynamicColors = findPreference("dynamic_colors");
            switchScreenPrivacy = findPreference("screen_privacy");

            exportSavings = findPreference("export");
            importSavings = findPreference("import");
        }

        private void exportJSON(Uri uri) {
            ArrayList<Saving> savings = new ArrayList<>(savingRepository.getAllSavings());
            ArrayList<Transaction> transactions = new ArrayList<>(transactionRepository.getAll());

            JSONArray savingJsonArray = new JSONArray();
            JSONArray transactionJsonArray = new JSONArray();

            try {
                for (Saving saving : savings) {
                    JSONObject savingObject = new JSONObject();
                    savingObject.put(Database.COLUMN_SAVING_ID, saving.getID());
                    savingObject.put(Database.COLUMN_SAVING_NAME, saving.getName());
                    savingObject.put(Database.COLUMN_SAVING_CURRENT_SAVING, saving.getCurrentSaving());
                    savingObject.put(Database.COLUMN_SAVING_GOAL, saving.getGoal());
                    savingObject.put(Database.COLUMN_SAVING_DESCRIPTION, saving.getDescription());
                    savingObject.put(Database.COLUMN_SAVING_IS_ARCHIVED, saving.getIsArchived());
                    savingObject.put(Database.COLUMN_SAVING_DEADLINE, saving.getDeadline());
                    savingObject.put(Database.COLUMN_SAVING_CURRENCY, saving.getCurrency());
                    savingJsonArray.put(savingObject);
                }
            } catch (Exception e) {
                Log.e("Export", "Something went wrong while collecting savings", e);
            }

            try {
                for (Transaction transaction : transactions) {
                    JSONObject transactionObject = new JSONObject();
                    transactionObject.put(Database.COLUMN_TRANSACTION_ID, transaction.getID());
                    transactionObject.put(Database.COLUMN_TRANSACTION_SAVING_ID, transaction.getSavingID());
                    transactionObject.put(Database.COLUMN_TRANSACTION_AMOUNT, transaction.getAmount());
                    transactionObject.put(Database.COLUMN_TRANSACTION_TYPE, transaction.getType());
                    transactionObject.put(Database.COLUMN_TRANSACTION_DATE, transaction.getDate());
                    transactionObject.put(Database.COLUMN_TRANSACTION_NOTE, transaction.getNote());
                    transactionJsonArray.put(transactionObject);
                }
            } catch (Exception e) {
                Log.e("Export", "Something went wrong while collecting saving transactions", e);
            }

            try {
                JSONObject jsonExport = new JSONObject();
                jsonExport.put(Database.TABLE_SAVING, savingJsonArray);
                jsonExport.put(Database.TABLE_TRANSACTION, transactionJsonArray);

                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    outputStream.write(jsonExport.toString().getBytes());
                    outputStream.close();
                }

                Snackbar.make(requireView(), R.string.snackbar_export_successful, Snackbar.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("Export", "Something went wrong while exporting", e);
            }
        }

        private void importJSON(Uri uri) {
            StringBuilder jsonBuilder = new StringBuilder();

            try (Database database = new Database(requireContext())) {
                SQLiteDatabase writableDatabase = database.getWritableDatabase();

                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
                bufferedReader.close();

                JSONObject jsonImport = new JSONObject(jsonBuilder.toString());
                JSONArray savingJsonArray = jsonImport.getJSONArray(Database.TABLE_SAVING);
                JSONArray transactionJsonArray = jsonImport.getJSONArray(Database.TABLE_TRANSACTION);

                try {
                    writableDatabase.beginTransaction();
                    for (int i = 0; i < savingJsonArray.length(); i++) {
                        JSONObject savingObject = savingJsonArray.getJSONObject(i);
                        ContentValues savingValues = new ContentValues();

                        long savingDeadline = savingObject.getLong(Database.COLUMN_SAVING_DEADLINE);
                        String currency = savingObject.optString(Database.COLUMN_SAVING_CURRENCY, "USD");

                        if (savingDeadline != AlarmUtil.NO_ALARM) {
                            Saving rescheduledSaving = new Saving();
                            rescheduledSaving.setID(savingObject.getString(Database.COLUMN_SAVING_ID));
                            rescheduledSaving.setName(savingObject.getString(Database.COLUMN_SAVING_NAME));
                            rescheduledSaving.setDeadline(savingObject.getLong(Database.COLUMN_SAVING_DEADLINE));
                            AlarmUtil.set(requireContext(), rescheduledSaving);
                        }

                        savingValues.put(Database.COLUMN_SAVING_ID, savingObject.getString(Database.COLUMN_SAVING_ID));
                        savingValues.put(Database.COLUMN_SAVING_NAME, savingObject.getString(Database.COLUMN_SAVING_NAME));
                        savingValues.put(Database.COLUMN_SAVING_CURRENT_SAVING, savingObject.getDouble(Database.COLUMN_SAVING_CURRENT_SAVING));
                        savingValues.put(Database.COLUMN_SAVING_GOAL, savingObject.getDouble(Database.COLUMN_SAVING_GOAL));
                        savingValues.put(Database.COLUMN_SAVING_CURRENCY, currency);

                        String description = savingObject.optString(Database.COLUMN_SAVING_DESCRIPTION,
                                savingObject.optString("notes", ""));
                        savingValues.put(Database.COLUMN_SAVING_DESCRIPTION, description);

                        savingValues.put(Database.COLUMN_SAVING_IS_ARCHIVED, savingObject.getInt(Database.COLUMN_SAVING_IS_ARCHIVED));
                        savingValues.put(Database.COLUMN_SAVING_DEADLINE, savingObject.getLong(Database.COLUMN_SAVING_DEADLINE));
                        writableDatabase.insert(Database.TABLE_SAVING, null, savingValues);
                    }

                    for (int i = 0; i < transactionJsonArray.length(); i++) {
                        JSONObject transactionObject = transactionJsonArray.getJSONObject(i);
                        ContentValues transactionValues = new ContentValues();

                        transactionValues.put(Database.COLUMN_TRANSACTION_SAVING_ID, transactionObject.getString(Database.COLUMN_TRANSACTION_SAVING_ID));
                        transactionValues.put(Database.COLUMN_TRANSACTION_AMOUNT, transactionObject.getDouble(Database.COLUMN_TRANSACTION_AMOUNT));
                        transactionValues.put(Database.COLUMN_TRANSACTION_TYPE, transactionObject.getString(Database.COLUMN_TRANSACTION_TYPE));
                        transactionValues.put(Database.COLUMN_TRANSACTION_DATE, transactionObject.getLong(Database.COLUMN_TRANSACTION_DATE));

                        String note = transactionObject.optString(Database.COLUMN_TRANSACTION_NOTE, "");
                        transactionValues.put(Database.COLUMN_TRANSACTION_NOTE, note);

                        writableDatabase.insert(Database.TABLE_TRANSACTION, null, transactionValues);
                    }

                    writableDatabase.setTransactionSuccessful();
                    Snackbar.make(requireView(), R.string.snackbar_import_successful, Snackbar.LENGTH_SHORT).show();

                    restartApp();

                } catch (Exception e) {
                    Log.e("Import", "Something went wrong while importing savings or transactions", e);
                } finally {
                    writableDatabase.endTransaction();
                }
            } catch (Exception e) {
                Log.e("Import", "Something went wrong while importing", e);
            }
        }

        private void restartApp() {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finishAffinity();
            }
        }
    }
}