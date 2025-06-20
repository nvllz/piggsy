package com.eipna.centsation.ui.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

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

import com.eipna.centsation.R;
import com.eipna.centsation.data.Contrast;
import com.eipna.centsation.data.Currency;
import com.eipna.centsation.data.Database;
import com.eipna.centsation.data.DateFormat;
import com.eipna.centsation.data.Theme;
import com.eipna.centsation.data.saving.Saving;
import com.eipna.centsation.data.saving.SavingRepository;
import com.eipna.centsation.data.transaction.Transaction;
import com.eipna.centsation.data.transaction.TransactionRepository;
import com.eipna.centsation.databinding.ActivitySettingsBinding;
import com.eipna.centsation.util.AlarmUtil;
import com.eipna.centsation.util.PreferenceUtil;
import com.google.android.material.color.DynamicColors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

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
        private ListPreference listContrast;
        private ListPreference listTheme;
        private ListPreference listCurrency;

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
            listDeadlineFormat.setValue(preferences.getDeadlineFormat());
            listDeadlineFormat.setSummary(DateFormat.getNameByPattern(preferences.getDeadlineFormat()));
            listDeadlineFormat.setOnPreferenceChangeListener((preference, newValue) -> {
                preferences.setDeadlineFormat((String) newValue);
                listDeadlineFormat.setSummary(DateFormat.getNameByPattern((String) newValue));
                return true;
            });

            listContrast.setEntries(Contrast.toNameArray());
            listContrast.setEntryValues(Contrast.toValueArray());
            listContrast.setValue(preferences.getContrast());
            listContrast.setSummary(Contrast.getName(preferences.getContrast()));
            listContrast.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedContrast = (String) newValue;
                if (selectedContrast.equals(Contrast.LOW.VALUE)) requireActivity().setTheme(R.style.Theme_Centsation);
                if (selectedContrast.equals(Contrast.MEDIUM.VALUE)) requireActivity().setTheme(R.style.Theme_Centsation_MediumContrast);
                if (selectedContrast.equals(Contrast.HIGH.VALUE)) requireActivity().setTheme(R.style.Theme_Centsation_HighContrast);
                requireActivity().recreate();

                preferences.setContrast(selectedContrast);
                listContrast.setSummary(Contrast.getName(selectedContrast));
                return true;
            });

            listCurrency.setEntries(Currency.getNames());
            listCurrency.setEntryValues(Currency.getCodes());
            listCurrency.setValue(preferences.getCurrency());
            listCurrency.setSummary(Currency.getName(preferences.getCurrency()));
            listCurrency.setOnPreferenceChangeListener((preference, currency) -> {
                preferences.setCurrency((String) currency);
                listCurrency.setSummary(Currency.getName((String) currency));
                return true;
            });

            switchDynamicColors.setVisible(DynamicColors.isDynamicColorAvailable());
            switchDynamicColors.setChecked(preferences.isDynamicColors());
            switchDynamicColors.setOnPreferenceChangeListener((preference, isChecked) -> {
                preferences.setDynamicColors((boolean) isChecked);
                requireActivity().recreate();
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
                if (selectedTheme.equals(Theme.BATTERY.VALUE)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
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

        private void exportData() {
            if (noSavingsFound()) {
                Toast.makeText(requireContext(), R.string.toast_export_no_savings_found, Toast.LENGTH_SHORT).show();
            } else {
                Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
                exportIntent.setType("application/json");
                exportIntent.putExtra(Intent.EXTRA_TITLE, "exported_savings.json");
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
            listCurrency = findPreference("currency");
            listTheme = findPreference("theme");
            listContrast = findPreference("contrast");

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
                    savingObject.put(Database.COLUMN_SAVING_NOTES, saving.getNotes());
                    savingObject.put(Database.COLUMN_SAVING_IS_ARCHIVED, saving.getIsArchived());
                    savingObject.put(Database.COLUMN_SAVING_DEADLINE, saving.getDeadline());
                    savingJsonArray.put(savingObject);
                }
            } catch (Exception e) {
                Log.e("Export", "Something went wrong when collecting savings", e);
            }

            try {
                for (Transaction transaction : transactions) {
                    JSONObject transactionObject = new JSONObject();
                    transactionObject.put(Database.COLUMN_TRANSACTION_ID, transaction.getID());
                    transactionObject.put(Database.COLUMN_TRANSACTION_SAVING_ID, transaction.getSavingID());
                    transactionObject.put(Database.COLUMN_TRANSACTION_AMOUNT, transaction.getAmount());
                    transactionObject.put(Database.COLUMN_TRANSACTION_TYPE, transaction.getType());
                    transactionObject.put(Database.COLUMN_TRANSACTION_DATE, transaction.getDate());
                    transactionJsonArray.put(transactionObject);
                }
            } catch (Exception e) {
                Log.e("Export", "Something went wrong when collecting saving transactions", e);
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

                Toast.makeText(requireContext(), R.string.toast_export_successful, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("Export", "Something went wrong when exporting", e);
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
                        savingValues.put(Database.COLUMN_SAVING_NOTES, savingObject.getString(Database.COLUMN_SAVING_NOTES));
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
                        writableDatabase.insert(Database.TABLE_TRANSACTION, null, transactionValues);
                    }

                    writableDatabase.setTransactionSuccessful();
                    Toast.makeText(requireContext(), R.string.toast_import_successful, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("Import", "Something went wrong while importing savings or transactions", e);
                } finally {
                    writableDatabase.endTransaction();
                }
            } catch (Exception e) {
                Log.e("Import", "Something went wrong while importing", e);
            }
        }
    }
}