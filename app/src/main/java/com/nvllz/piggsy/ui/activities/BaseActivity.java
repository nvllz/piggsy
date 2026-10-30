package com.nvllz.piggsy.ui.activities;

import android.app.KeyguardManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.data.Theme;
import com.nvllz.piggsy.util.BiometricLockUtil;
import com.nvllz.piggsy.util.NotificationUtil;
import com.nvllz.piggsy.util.PreferenceUtil;
import com.google.android.material.color.DynamicColors;

public abstract class BaseActivity extends AppCompatActivity {

    protected PreferenceUtil preferences;

    private final ActivityResultLauncher<Intent> credentialUnlockLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    BiometricLockUtil.onUnlocked(this);
                } else {
                    BiometricLockUtil.onLockRejected(this);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        preferences = new PreferenceUtil(this);
        super.onCreate(savedInstanceState);

        if (preferences.isScreenPrivacyEnabled()
                || (preferences.isBiometricLockEnabled() && BiometricLockUtil.isLocked())) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        NotificationUtil.createChannels(this);

        String theme = preferences.getTheme();
        if (theme.equals(Theme.SYSTEM.VALUE)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (theme.equals(Theme.LIGHT.VALUE)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        if (theme.equals(Theme.DARK.VALUE)) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        setTheme(R.style.Theme_Piggsy);

        if (preferences.isDynamicColors()) DynamicColors.applyToActivityIfAvailable(this);

        BiometricLockUtil.ensureLockCover(this, preferences);
    }

    @Override
    protected void onDestroy() {
        BiometricLockUtil.onActivityDestroyed(this);
        super.onDestroy();
    }

    public void launchCredentialUnlock(String title, String message) {
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        if (keyguard != null && keyguard.isDeviceSecure()) {
            credentialUnlockLauncher.launch(keyguard.createConfirmDeviceCredentialIntent(title, message));
        } else {
            BiometricLockUtil.onUnlocked(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BiometricLockUtil.onActivityResumed(this, preferences);
    }

    @Override
    protected void onStop() {
        super.onStop();
        BiometricLockUtil.onActivityStopped(this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }
}

