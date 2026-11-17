package com.nvllz.piggsy.util;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.savedstate.ViewTreeSavedStateRegistryOwner;

import com.nvllz.piggsy.R;
import com.nvllz.piggsy.ui.ComposeLockCoverView;
import com.nvllz.piggsy.ui.activities.BaseActivity;

import java.util.ArrayList;

public final class BiometricLockUtil {

    public static final long GRACE_PERIOD_MS = 30_000L;
    private static final int MAX_PROMPT_RETRIES = 5;
    private static final long PROMPT_RETRY_DELAY_MS = 400L;
    private static final long PROMPT_TIMEOUT_MS = 5_000L;

    private static boolean locked = true;
    private static int authSession = 0;
    private static boolean wasUnlockedAtBackground = false;
    private static boolean hasBackgrounded = false;
    private static long lastBackgroundedAt = 0L;
    private static int resumedCount = 0;

    private static boolean promptShowing = false;
    private static long promptShownAt = 0L;
    private static int promptRetries = 0;
    private static boolean retryPending = false;

    private static boolean credentialFlowActive = false;
    private static int credentialSession = -1;

    private static FragmentActivity activeActivity = null;

    private static View lockCoverView = null;

    private static final ArrayList<Runnable> unlockActions = new ArrayList<>();

    private BiometricLockUtil() {
    }

    public static boolean isLocked() {
        return locked;
    }

    public static boolean willLockAfterCurrentStop() {
        return resumedCount == 1;
    }

    public static void runOnUnlock(Runnable action) {
        if (action == null) return;
        if (!locked) {
            action.run();
            return;
        }
        unlockActions.add(action);
    }

    public static void removeUnlockAction(Runnable action) {
        unlockActions.remove(action);
    }

    private static void runUnlockActions() {
        ArrayList<Runnable> actions = new ArrayList<>(unlockActions);
        unlockActions.clear();
        for (Runnable action : actions) {
            action.run();
        }
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
        activeActivity = activity;

        if (preferences.isBiometricLockEnabled() && isSupported(activity)) {
            if (locked) {
                boolean withinGrace = hasBackgrounded && wasUnlockedAtBackground
                        && SystemClock.elapsedRealtime() - lastBackgroundedAt < GRACE_PERIOD_MS;

                if (withinGrace) {
                    locked = false;
                    dismissLockCover(activity);
                    runUnlockActions();
                } else {
                    ensureLockCover(activity, preferences);
                    promptRetries = 0;
                    retryPending = false;
                    showPrompt(activity, true, null);
                }
            }
        } else if (locked) {
            locked = false;
            authSession++;
            dismissLockCover(activity);
            runUnlockActions();
        }

        applySecurityFlag(activity, preferences);
    }

    public static void onActivityPaused(FragmentActivity activity, PreferenceUtil preferences) {
        if (preferences.isBiometricLockEnabled()) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public static void onActivityStopped(FragmentActivity activity, PreferenceUtil preferences) {
        if (resumedCount > 0) {
            resumedCount--;

            if (resumedCount == 0 && !activity.isChangingConfigurations()) {
                hasBackgrounded = true;
                wasUnlockedAtBackground = !locked;
                lastBackgroundedAt = SystemClock.elapsedRealtime();
                locked = true;
                activeActivity = null;

                if (!credentialFlowActive) {
                    authSession++;
                }

                promptShowing = false;
                retryPending = false;
                promptRetries = 0;

                applySecurityFlag(activity, preferences);

                if (preferences.isBiometricLockEnabled()) {
                    if (activity instanceof BaseActivity) {
                        ((BaseActivity) activity).onLockEngaged();
                    }
                    ensureLockCover(activity, preferences);
                }
            }
        }
    }

    public static void beginCredentialUnlock() {
        credentialSession = authSession;
        credentialFlowActive = true;
    }

    public static void onCredentialResult(FragmentActivity activity, boolean success) {
        int session = credentialSession;
        credentialSession = -1;
        credentialFlowActive = false;

        if (success) {
            onUnlocked(activity, session);
        } else {
            onLockRejected(activity, session);
        }
    }

    public static void onUnlocked(FragmentActivity activity, int session) {
        if (!locked || session != authSession) return;

        locked = false;
        credentialFlowActive = false;
        credentialSession = -1;
        promptShowing = false;
        retryPending = false;
        promptRetries = 0;

        dismissLockCover(activity);
        applySecurityFlag(activity, new PreferenceUtil(activity));
        runUnlockActions();
    }

    public static void onLockRejected(FragmentActivity activity, int session) {
        if (!locked || session != authSession) return;

        promptShowing = false;
        credentialFlowActive = false;
        credentialSession = -1;

        FragmentActivity target = activity;
        if (target.isFinishing() || target.isDestroyed()) {
            target = activeActivity;
        }
        if (target == null || target.isFinishing() || target.isDestroyed()) return;

        ensureLockCover(target, new PreferenceUtil(target));
        schedulePromptRetry(target, new PreferenceUtil(target));
    }

    private static void schedulePromptRetry(FragmentActivity activity, PreferenceUtil preferences) {
        if (retryPending || promptRetries >= MAX_PROMPT_RETRIES) return;
        promptRetries++;
        retryPending = true;

        activity.getWindow().getDecorView().postDelayed(() -> {
            retryPending = false;
            if (activity.isDestroyed() || activity.isFinishing()) return;
            if (!locked || !preferences.isBiometricLockEnabled()) return;
            if (promptShowing || resumedCount == 0) return;

            ensureLockCover(activity, preferences);
            showPrompt(activity, true, null);
        }, PROMPT_RETRY_DELAY_MS);
    }

    public static void onActivityDestroyed(FragmentActivity activity) {
        credentialFlowActive = false;
        credentialSession = -1;
        dismissLockCover(activity);
    }

    public static void applySecurityFlag(FragmentActivity activity, PreferenceUtil preferences) {
        Window window = activity.getWindow();
        if (preferences.isScreenPrivacyEnabled()) {

            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            return;
        }

        if (locked && preferences.isBiometricLockEnabled()) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    public static void ensureLockCover(FragmentActivity activity, PreferenceUtil preferences) {
        if (!preferences.isBiometricLockEnabled() || !locked) return;

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) {

            activity.getWindow().getDecorView().post(() -> ensureLockCover(activity, preferences));
            return;
        }

        View existing = null;
        for (int i = content.getChildCount() - 1; i >= 0; i--) {
            View child = content.getChildAt(i);
            if (child instanceof ComposeLockCoverView) {
                if (existing == null) {
                    existing = child;
                } else {
                    content.removeView(child);
                }
            }
        }

        if (existing != null) {
            lockCoverView = existing;
            return;
        }

        ComposeLockCoverView coverView = new ComposeLockCoverView(activity);


        ViewTreeLifecycleOwner.set(coverView, activity);
        ViewTreeSavedStateRegistryOwner.set(coverView, activity);

        coverView.setClickable(true);
        coverView.setLongClickable(true);
        coverView.setFocusable(true);
        coverView.setFocusableInTouchMode(true);
        coverView.setOnKeyListener((v, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
        coverView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.requestFocus();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
            }
        });
        coverView.setOnClickListener(v -> showPrompt(activity, true, null));

        content.addView(coverView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        lockCoverView = coverView;
    }

    public static void dismissLockCover(FragmentActivity activity) {
        lockCoverView = null;

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        for (int i = content.getChildCount() - 1; i >= 0; i--) {
            View child = content.getChildAt(i);
            if (child instanceof ComposeLockCoverView) {
                content.removeView(child);
            }
        }
    }

    public static void showPrompt(FragmentActivity activity, boolean isLockScreen, Runnable onConfirmed) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (promptShowing) {
            if (SystemClock.elapsedRealtime() - promptShownAt < PROMPT_TIMEOUT_MS) return;
            promptShowing = false;
        }

        if (isLockScreen) {
            if (!locked) return;
            ensureLockCover(activity, new PreferenceUtil(activity));
        }

        boolean biometricOk = BiometricManager.from(activity)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;

        if (biometricOk && Build.VERSION.SDK_INT >= 28) {
            promptShowing = true;
            promptShownAt = SystemClock.elapsedRealtime();
            int session = authSession;

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
                    authenticationCallback(activity, isLockScreen, onConfirmed, session));

            prompt.authenticate(builder.build());
        } else {
            launchCredentialUnlock(activity);
        }
    }

    private static BiometricPrompt.AuthenticationCallback authenticationCallback(
            FragmentActivity activity, boolean isLockScreen, Runnable onConfirmed, int session) {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                if (isLockScreen) {
                    onUnlocked(activity, session);
                } else {
                    promptShowing = false;
                    if (locked) return;
                    if (onConfirmed != null) onConfirmed.run();
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                promptShowing = false;
                if (!isLockScreen) return;
                if (session != authSession || !locked) return;

                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    launchCredentialUnlock(activity);
                    return;
                }
                if (errorCode == BiometricPrompt.ERROR_CANCELED) {
                    onLockRejected(activity, session);
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
