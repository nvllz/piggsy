package com.nvllz.piggsy.ui

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import com.nvllz.piggsy.data.Theme
import com.nvllz.piggsy.ui.components.LocalToast
import com.nvllz.piggsy.ui.components.ToastHost
import com.nvllz.piggsy.ui.components.rememberToastState
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.PreferenceUtil

class ComposeSettingsView(context: Context) : AbstractComposeView(context) {

    private val preferences = PreferenceUtil(context)

    var themeState by mutableStateOf(preferences.getTheme())
        private set

    var dynamicColorState by mutableStateOf(preferences.isDynamicColors())
        private set

    fun setTheme(value: String) {
        themeState = value
        preferences.setTheme(value)


    }

    fun setDynamicColors(enabled: Boolean) {
        dynamicColorState = enabled
        preferences.setDynamicColors(enabled)
    }

    fun applyPendingTheme() {
        when (themeState) {
            Theme.SYSTEM.VALUE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            Theme.LIGHT.VALUE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.DARK.VALUE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    @Composable
    override fun Content() {

        val systemDark = remember {
            (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        }
        val darkTheme = when (themeState) {
            Theme.SYSTEM.VALUE -> systemDark
            Theme.LIGHT.VALUE -> false
            Theme.DARK.VALUE -> true
            else -> systemDark
        }
        PiggsyTheme(darkTheme = darkTheme, dynamicColor = dynamicColorState) {
            val toast = rememberToastState()
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalToast provides toast) {
                    SettingsScreen(
                        onBack = { (context as? Activity)?.finish() },
                        theme = themeState,
                        onThemeChange = ::setTheme,
                        dynamicColor = dynamicColorState,
                        onDynamicColorChange = ::setDynamicColors,
                    )
                }
                ToastHost(
                    state = toast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}
