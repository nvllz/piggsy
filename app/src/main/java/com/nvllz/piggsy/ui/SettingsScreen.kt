package com.nvllz.piggsy.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.google.android.material.color.DynamicColors
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.DateFormat
import com.nvllz.piggsy.data.Theme
import com.nvllz.piggsy.ui.components.ChoiceDialog
import com.nvllz.piggsy.ui.components.LocalToast
import com.nvllz.piggsy.ui.components.PillSelector
import com.nvllz.piggsy.ui.components.ScreenHeader
import com.nvllz.piggsy.ui.components.SectionLabel
import com.nvllz.piggsy.ui.components.SettingDivider
import com.nvllz.piggsy.ui.components.SettingGroup
import com.nvllz.piggsy.ui.components.SettingRow
import com.nvllz.piggsy.ui.components.SwitchSetting
import com.nvllz.piggsy.ui.components.ToastKind
import com.nvllz.piggsy.util.BiometricLockUtil
import com.nvllz.piggsy.util.PreferenceUtil

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    theme: String,
    onThemeChange: (String) -> Unit,
    dynamicColor: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val fragmentActivity = context as? FragmentActivity
    val toast = LocalToast.current
    val preferences = remember { PreferenceUtil(context) }

    var dateFormat by remember { mutableStateOf(preferences.getDateFormat()) }
    var screenPrivacy by remember { mutableStateOf(preferences.isScreenPrivacyEnabled()) }
    var biometricLock by remember { mutableStateOf(preferences.isBiometricLockEnabled()) }
    var showDateFormatDialog by remember { mutableStateOf(false) }

    val dynamicColorsAvailable = remember { DynamicColors.isDynamicColorAvailable() }
    val dateFormatOptions = remember {
        DateFormat.getPatterns().map { pattern -> pattern to DateFormat.getNameByPattern(pattern) }
    }
    val themeOptions = remember {
        listOf(
            Theme.SYSTEM.VALUE to Theme.SYSTEM.VALUE,
            Theme.LIGHT.VALUE to Theme.LIGHT.VALUE,
            Theme.DARK.VALUE to Theme.DARK.VALUE,
        )
    }
    val themeIconOf: (String) -> Int? = { value ->
        when (value) {
            Theme.SYSTEM.VALUE -> R.drawable.ic_theme
            Theme.LIGHT.VALUE -> R.drawable.ic_theme_light
            Theme.DARK.VALUE -> R.drawable.ic_theme_dark
            else -> null
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(stringResource(R.string.toolbar_settings), onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            SectionLabel(stringResource(R.string.preference_category_savings), topPadding = 4.dp)
            SettingGroup {
                SettingRow(
                    title = stringResource(R.string.preference_list_date_format),
                    summary = DateFormat.getNameByPattern(dateFormat),
                    icon = R.drawable.ic_calendar,
                    onClick = { showDateFormatDialog = true },
                )
            }

            SectionLabel(stringResource(R.string.preference_category_appearance))
            SettingGroup {
                SettingRow(
                    title = stringResource(R.string.preference_list_theme),
                    summary = theme,
                    icon = R.drawable.ic_theme,
                )
                PillSelector(
                    options = themeOptions,
                    selected = theme,
                    onSelect = onThemeChange,
                    iconOf = themeIconOf,
                    chipPadding = PaddingValues(horizontal = 28.dp, vertical = 7.dp),
                    modifier = Modifier.padding(start = 56.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
                )
                if (dynamicColorsAvailable) {
                    SettingDivider()
                    SwitchSetting(
                        title = stringResource(R.string.preference_switch_dynamic_colors_title),
                        checked = dynamicColor,
                        onCheckedChange = onDynamicColorChange,
                        summary = stringResource(R.string.preference_switch_dynamic_colors_summary),
                        icon = R.drawable.ic_palette,
                    )
                }
            }

            SectionLabel(stringResource(R.string.preference_category_security))
            SettingGroup {
                SwitchSetting(
                    title = stringResource(R.string.preference_switch_screen_privacy_title),
                    checked = screenPrivacy,
                    onCheckedChange = { checked ->
                        screenPrivacy = checked
                        preferences.setScreenPrivacy(checked)
                        if (checked) {
                            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    },
                    summary = stringResource(R.string.preference_switch_screen_privacy_summary),
                    icon = R.drawable.ic_screen_privacy,
                )
                SettingDivider()
                SwitchSetting(
                    title = stringResource(R.string.preference_switch_biometric_title),
                    checked = biometricLock,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (!BiometricLockUtil.isSupported(context)) {
                                toast.show(
                                    context.getString(R.string.biometric_toast_unavailable),
                                    ToastKind.ERROR,
                                )
                                return@SwitchSetting
                            }
                            if (fragmentActivity != null) {
                                BiometricLockUtil.showPrompt(fragmentActivity, false) {
                                    preferences.setBiometricLockEnabled(true)
                                    biometricLock = true
                                }
                            }
                        } else {
                            if (!BiometricLockUtil.isSupported(context)) {
                                preferences.setBiometricLockEnabled(false)
                                biometricLock = false
                                return@SwitchSetting
                            }
                            if (fragmentActivity != null) {
                                BiometricLockUtil.showPrompt(fragmentActivity, false) {
                                    preferences.setBiometricLockEnabled(false)
                                    biometricLock = false
                                }
                            }
                        }
                    },
                    summary = stringResource(R.string.preference_switch_biometric_summary),
                    icon = R.drawable.ic_fingerprint,
                )
            }
        }
    }

    if (showDateFormatDialog) {
        ChoiceDialog(
            title = stringResource(R.string.preference_list_date_format),
            options = dateFormatOptions,
            selected = dateFormat,
            onSelect = { pattern ->
                preferences.setDateFormat(pattern)
                dateFormat = pattern
            },
            onDismiss = { showDateFormatDialog = false },
        )
    }
}
