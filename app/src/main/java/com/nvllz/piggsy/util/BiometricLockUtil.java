package com.nvllz.piggsy.util;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.Window;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.ui.activities.BaseActivity;

public final class BiometricLockUtil {

    public static final long GRACE_PERIOD_MS = 30_000L;

    private static boolean locked = true;
    private static boolean forceLock = true;
    private static boolean hasBackgrounded = false;
    private static long lastBackgroundedAt = 0L;
    private static int resumedCount = 0;
    private static boolean promptShowing = false;
    private static Dialog lockCover = null;

    private BiometricLockUtil() {
    }

    public static boolean isLocked() {
        return locked;
    }

    public static boolean isSupported(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        boolean biometricOk = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;

        KeyguardManager keyguard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean credentialOk = keyguard != null && keyguard.isDeviceSecure();

        return biometricOk || credentialOk;
    }

    public static void onActivityResumed(FragmentActivity activity, PreferenceUtil preferences) {
        resumedCount++;

        if (resumedCount == 1) {
            boolean readyToLock = locked && preferences.isBiometricLockEnabled() && isSupported(activity);

            boolean withinGrace = !forceLock && hasBackgrounded
                    && SystemClock.elapsedRealtime() - lastBackgroundedAt < GRACE_PERIOD_MS;

            if (readyToLock && !withinGrace) {
                ensureLockCover(activity, preferences);
                if (!promptShowing) {
                    showPrompt(activity, true, null);
                }
            } else if (locked) {
                locked = false;
                dismissLockCover();
            }
        }

        applySecurityFlag(activity, preferences);
    }

    public static void onActivityStopped(FragmentActivity activity) {
        if (resumedCount > 0) {
            resumedCount--;

            if (resumedCount == 0 && !activity.isChangingConfigurations()) {
                hasBackgrounded = true;
                lastBackgroundedAt = SystemClock.elapsedRealtime();
                locked = true;
                promptShowing = false;
            }
        }
    }

    public static void onUnlocked(FragmentActivity activity) {
        locked = false;
        promptShowing = false;
        forceLock = false;
        dismissLockCover();
        applySecurityFlag(activity, new PreferenceUtil(activity));
    }

    public static void onLockRejected(FragmentActivity activity) {
        promptShowing = false;
        locked = true;

        forceLock = true;
        activity.moveTaskToBack(true);
    }

    public static void onActivityDestroyed(FragmentActivity activity) {
        if (lockCover != null && lockCover.getOwnerActivity() == activity) {
            dismissLockCover();
        }
    }

    public static void applySecurityFlag(FragmentActivity activity, PreferenceUtil preferences) {
        if (preferences.isScreenPrivacyEnabled()) return;

        Window window = activity.getWindow();
        if (locked && preferences.isBiometricLockEnabled()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public static void ensureLockCover(FragmentActivity activity, PreferenceUtil preferences) {
        if (!preferences.isBiometricLockEnabled() || !isLocked()) return;
        if (lockCover != null && lockCover.isShowing()) return;

        Dialog dialog = new Dialog(activity, R.style.Theme_Piggsy);
        dialog.setOwnerActivity(activity);
        dialog.setContentView(R.layout.dialog_biometric_lock);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0f);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        dialog.show();
        lockCover = dialog;
    }

    public static void dismissLockCover() {
        if (lockCover != null) {
            if (lockCover.isShowing()) {
                lockCover.dismiss();
            }
            lockCover = null;
        }
    }

    public static void showPrompt(FragmentActivity activity, boolean isLockScreen, Runnable onConfirmed) {
        if (promptShowing || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        boolean biometricOk = BiometricManager.from(activity)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;

        if (biometricOk && Build.VERSION.SDK_INT >= 28) {
            promptShowing = true;

            BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.biometric_prompt_title));

            if (Build.VERSION.SDK_INT >= 30) {
                builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            } else {
                builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        .setNegativeButtonText(activity.getString(R.string.biometric_prompt_use_screen_lock));
            }

            BiometricPrompt prompt = new BiometricPrompt(activity,
                    ContextCompat.getMainExecutor(activity),
                    authenticationCallback(activity, isLockScreen, onConfirmed));

            prompt.authenticate(builder.build());
        } else {
            launchCredentialUnlock(activity);
        }
    }

    private static BiometricPrompt.AuthenticationCallback authenticationCallback(
            FragmentActivity activity, boolean isLockScreen, Runnable onConfirmed) {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                if (isLockScreen) {
                    onUnlocked(activity);
                } else {
                    promptShowing = false;
                    if (onConfirmed != null) onConfirmed.run();
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                promptShowing = false;
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    launchCredentialUnlock(activity);
                    return;
                }
                if (isLockScreen) {
                    onLockRejected(activity);
                }
            }
        };
    }

    private static void launchCredentialUnlock(FragmentActivity activity) {
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).launchCredentialUnlock(
                    activity.getString(R.string.biometric_prompt_title),
                    activity.getString(R.string.biometric_prompt_subtitle));
        }
    }
}

