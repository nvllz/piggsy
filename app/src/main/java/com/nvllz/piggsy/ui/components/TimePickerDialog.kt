package com.nvllz.piggsy.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.nvllz.piggsy.ui.theme.PiggsyTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour,
    )
    val config = LocalConfiguration.current
    val compact = config.screenHeightDp < 640 || config.screenWidthDp < 360


    val accent = PiggsyTheme.colors.accent
    val colors = TimePickerDefaults.colors(
        clockDialColor = accent.copy(alpha = 18f / 255f),
        clockDialSelectedContentColor = MaterialTheme.colorScheme.background,
        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
        selectorColor = accent,
        containerColor = PiggsyTheme.colors.dialogSurface,
        periodSelectorBorderColor = accent,
        periodSelectorSelectedContainerColor = accent,
        periodSelectorUnselectedContainerColor = Color.Transparent,
        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.background,
        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
        timeSelectorSelectedContainerColor = accent,
        timeSelectorUnselectedContainerColor = accent.copy(alpha = 18f / 255f),
        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.background,
        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
    )
    PiggsyDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(state.hour, state.minute) },
    ) {
        if (compact) {
            TimeInput(state = state, modifier = Modifier.fillMaxWidth(), colors = colors)
        } else {
            TimePicker(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                colors = colors,
                layoutType = TimePickerLayoutType.Vertical,
            )
        }
    }
}
