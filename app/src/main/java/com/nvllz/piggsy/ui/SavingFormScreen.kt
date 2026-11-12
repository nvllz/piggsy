package com.nvllz.piggsy.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.Currency
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.ui.components.ChoiceDialog
import com.nvllz.piggsy.ui.components.FormTextField
import com.nvllz.piggsy.ui.components.PrimaryButton
import com.nvllz.piggsy.ui.components.ScreenHeader
import com.nvllz.piggsy.ui.components.SettingGroup
import com.nvllz.piggsy.ui.components.SettingRow
import com.nvllz.piggsy.ui.components.SwitchSetting
import com.nvllz.piggsy.util.AlarmUtil
import com.nvllz.piggsy.util.DateUtil
import com.nvllz.piggsy.util.PreferenceUtil
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavingFormScreen(
    isEdit: Boolean,
    initial: Saving,
    deadline: Long,
    onDeadlinePick: () -> Unit,
    onDeadlineClear: () -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onSave: (name: String, currencyCode: String, current: String, goal: String, description: String, archiveWhenCompleted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val dateFormat = remember { PreferenceUtil(context).getDateFormat() }
    val currencyOptions = remember { Currency.currencies }

    var selectedCurrency by remember { mutableStateOf(initial.getCurrency() ?: "") }
    var name by remember { mutableStateOf(initial.getName() ?: "") }
    var current by remember { mutableStateOf("") }
    var goal by remember {
        mutableStateOf(
            if (isEdit && initial.getGoal() != 0.0) {

                String.format(Locale.US, "%.2f", initial.getGoal())
            } else {
                ""
            },
        )
    }
    var description by remember { mutableStateOf(initial.getDescription() ?: "") }
    var archiveWhenCompleted by remember { mutableStateOf(initial.getArchiveWhenCompleted()) }
    var descriptionFocused by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<String?>(null) }
    var goalError by remember { mutableStateOf<String?>(null) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    val scrollState = rememberScrollState()

    val invalidAmountText = stringResource(R.string.error_invalid_amount)

    val currencySymbol = Currency.getSymbol(selectedCurrency) ?: ""
    val hasDeadline = deadline != AlarmUtil.NO_ALARM.toLong()
    val deadlineText = if (hasDeadline) DateUtil.getStringDateTime(deadline, dateFormat, context) else ""

    val nameFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(isEdit) {
        if (!isEdit) {
            nameFocusRequester.requestFocus()
            keyboard?.show()
        }
    }


    LaunchedEffect(imeVisible, descriptionFocused) {
        if (imeVisible && descriptionFocused) {
            var lastMax = -1
            while (scrollState.maxValue != lastMax) {
                lastMax = scrollState.maxValue
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }


    fun performSave() {
        when {
            name.isEmpty() -> nameError = true
            else -> {

                val normalizedCurrent = current.trim().replace(',', '.')
                val normalizedGoal = goal.trim().replace(',', '.')
                val currentAmount = if (normalizedCurrent.isBlank()) 0.0 else normalizedCurrent.toDoubleOrNull()
                val goalAmount = if (normalizedGoal.isBlank()) 0.0 else normalizedGoal.toDoubleOrNull()
                when {
                    currentAmount == null -> currentError = invalidAmountText
                    goalAmount == null -> goalError = invalidAmountText
                    else -> onSave(
                        name,
                        selectedCurrency,
                        if (normalizedCurrent.isBlank()) "0.0" else normalizedCurrent,
                        if (normalizedGoal.isBlank()) "0.0" else normalizedGoal,
                        description,
                        archiveWhenCompleted,
                    )
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(
            title = stringResource(if (isEdit) R.string.toolbar_edit else R.string.toolbar_create),
            onBack = onBack,
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            FormTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.field_saving_name),
                isError = nameError,
                supportingText = if (nameError) {
                    stringResource(R.string.field_error_required)
                } else {
                    null
                },
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .focusRequester(nameFocusRequester),
            )

            if (!isEdit) {
                Row(Modifier.padding(bottom = 12.dp)) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        FormTextField(
                            value = current,
                            onValueChange = { current = it; currentError = null },
                            label = stringResource(R.string.field_saving_current_saving),
                            prefix = currencySymbol + "  ",
                            keyboardType = KeyboardType.Decimal,
                            isError = currentError != null,
                            supportingText = currentError,
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        FormTextField(
                            value = goal,
                            onValueChange = { goal = it; goalError = null },
                            label = stringResource(R.string.field_saving_goal),
                            prefix = currencySymbol + "  ",
                            keyboardType = KeyboardType.Decimal,
                            isError = goalError != null,
                            supportingText = goalError,
                        )
                    }
                }
            } else {
                FormTextField(
                    value = goal,
                    onValueChange = { goal = it; goalError = null },
                    label = stringResource(R.string.field_saving_goal),
                    prefix = currencySymbol + "  ",
                    keyboardType = KeyboardType.Decimal,
                    isError = goalError != null,
                    supportingText = goalError,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            SettingGroup(Modifier.padding(bottom = 12.dp)) {
                SettingRow(
                    title = stringResource(R.string.preference_list_currency),
                    summary = Currency.getName(selectedCurrency) ?: "",
                    icon = R.drawable.ic_currency,
                    onClick = { showCurrencyDialog = true },
                )
            }

            DeadlineField(
                deadlineText = deadlineText,
                hasDeadline = hasDeadline,
                onPick = onDeadlinePick,
                onClear = onDeadlineClear,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SettingGroup(Modifier.padding(bottom = 12.dp)) {
                SwitchSetting(
                    title = stringResource(R.string.field_saving_archive_when_completed),
                    summary = stringResource(R.string.field_saving_archive_when_completed_summary),
                    checked = archiveWhenCompleted,
                    onCheckedChange = { archiveWhenCompleted = it },
                    icon = R.drawable.ic_archive,
                )
            }

            FormTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.field_saving_description),
                singleLine = false,
                minLines = 5,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .onFocusChanged { descriptionFocused = it.isFocused },
            )

            PrimaryButton(
                text = stringResource(R.string.save),
                onClick = ::performSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            )
        }
    }

    if (showCurrencyDialog) {
        ChoiceDialog(
            title = stringResource(R.string.preference_list_currency),
            options = currencyOptions.map { it.CODE to "${it.NAME} (${it.SYMBOL})" },
            selected = selectedCurrency,
            onSelect = { selectedCurrency = it },
            onDismiss = { showCurrencyDialog = false },
        )
    }
}

@Composable
private fun DeadlineField(
    deadlineText: String,
    hasDeadline: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactions = remember { MutableInteractionSource() }
    LaunchedEffect(interactions) {
        interactions.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                onPick()
            }
        }
    }

    FormTextField(
        value = if (hasDeadline) stringResource(R.string.deadline_on, deadlineText) else "",
        onValueChange = {},
        label = stringResource(R.string.field_saving_deadline),
        readOnly = true,
        trailing = if (hasDeadline) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        painterResource(R.drawable.ic_x),
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            null
        },
        interactionSource = interactions,
        modifier = modifier,
    )
}
