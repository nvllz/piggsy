package com.nvllz.piggsy.ui.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import com.nvllz.piggsy.ui.ComposeSettingsView;

public class SettingsActivity extends BaseActivity {

    private ComposeSettingsView settingsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        settingsView = new ComposeSettingsView(this);
        setContentView(settingsView);
    }

    @Override
    protected void onPause() {
        settingsView.applyPendingTheme();
        super.onPause();
    }
}
