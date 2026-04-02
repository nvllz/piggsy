package com.nvllz.piggsy.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.DateFormat;
import com.nvllz.piggsy.data.Theme;
import com.nvllz.piggsy.databinding.ActivitySettingsBinding;
import com.nvllz.piggsy.util.PreferenceUtil;
import com.google.android.material.color.DynamicColors;

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

        private ListPreference listDeadlineFormat;
        private ListPreference listTheme;

        private SwitchPreferenceCompat switchDynamicColors;
        private SwitchPreferenceCompat switchScreenPrivacy;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_settings, rootKey);
            setPreferences();

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

        private void setPreferences() {
            preferences = new PreferenceUtil(requireContext());

            listDeadlineFormat = findPreference("deadline_format");
            listTheme = findPreference("theme");

            switchDynamicColors = findPreference("dynamic_colors");
            switchScreenPrivacy = findPreference("screen_privacy");

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