package com.nvllz.piggsy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.ui.components.PillSelector
import com.nvllz.piggsy.ui.components.ScreenHeader
import com.nvllz.piggsy.ui.components.SectionLabel
import com.nvllz.piggsy.ui.components.SettingDivider
import com.nvllz.piggsy.ui.components.SettingGroup
import com.nvllz.piggsy.ui.components.SettingRow
import com.nvllz.piggsy.ui.components.SwitchSetting


@Composable
fun BackupScreen(
    frequency: Int,
    retention: Int,
    locationSummary: String,
    encrypted: Boolean,
    listener: ComposeBackupView.Listener,
    onRetentionSelected: (Int) -> Unit,
    onEncryptionToggled: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        ScreenHeader(stringResource(R.string.preference_category_backup), listener::onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            SectionLabel(stringResource(R.string.preference_import_title), topPadding = 4.dp)

            SettingGroup {
                SettingRow(
                    title = stringResource(R.string.preference_import_title),
                    summary = stringResource(R.string.import_data_summary),
                    icon = R.drawable.ic_import,
                    onClick = listener::onImportClicked,
                )
            }

            SectionLabel(stringResource(R.string.preference_export_title))

            SettingGroup {
                SettingRow(
                    title = stringResource(R.string.backup_frequency),
                    summary = frequencyLabel(context, frequency),
                    icon = R.drawable.ic_calendar_range,
                    onClick = listener::onFrequencyClicked,
                )

                AnimatedVisibility(
                    visible = frequency > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(Modifier.padding(start = 56.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)) {
                        Text(
                            stringResource(R.string.backup_retention_count),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        PillSelector(
                            options = retentionOptions(),
                            selected = retention.takeIf { it in 0..5 },
                            onSelect = onRetentionSelected,
                        )
                    }
                }

                SettingDivider()

                SettingRow(
                    title = stringResource(R.string.backup_location),
                    summary = locationSummary,
                    icon = R.drawable.ic_folder,
                    onClick = listener::onBackupLocationClicked,
                )

                SettingDivider()

                SwitchSetting(
                    title = stringResource(R.string.backup_encryption_title),
                    checked = encrypted,
                    onCheckedChange = onEncryptionToggled,
                    summary = stringResource(R.string.backup_encryption_summary),
                    icon = R.drawable.ic_lock,
                )

                SettingDivider()

                SettingRow(
                    title = stringResource(R.string.manual_backup),
                    icon = R.drawable.ic_export_hero,
                    onClick = listener::onManualBackupClicked,
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun frequencyLabel(context: android.content.Context, frequency: Int): String {
    val entries = context.resources.getStringArray(R.array.backup_frequency_entries)
    val values = context.resources.getStringArray(R.array.backup_frequency_values)
    val index = values.indexOfFirst { it == frequency.toString() }
    return if (index >= 0) entries[index] else frequency.toString()
}


@Composable
private fun retentionOptions(): List<Pair<Int, String>> {
    val all = stringResource(R.string.backup_retention_all)
    return remember(all) { listOf(0 to all) + (1..5).map { it to it.toString() } }
}
