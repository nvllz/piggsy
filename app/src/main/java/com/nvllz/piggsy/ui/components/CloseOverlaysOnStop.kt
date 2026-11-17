package com.nvllz.piggsy.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.nvllz.piggsy.util.BiometricLockUtil
import com.nvllz.piggsy.util.PreferenceUtil

@Composable
fun CloseOverlaysOnStop(onStop: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner ?: return
    val preferences = remember(context) { PreferenceUtil(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP
                && preferences.isBiometricLockEnabled()
                && BiometricLockUtil.willLockAfterCurrentStop()
            ) {
                onStop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
