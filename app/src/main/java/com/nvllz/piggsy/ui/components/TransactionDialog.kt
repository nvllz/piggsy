package com.nvllz.piggsy.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.Currency
import com.nvllz.piggsy.data.transaction.TransactionType
import kotlinx.coroutines.delay

@Composable
fun TransactionDialog(
    title: String,
    currency: String,
    initialType: String?,
    initialAmount: String,
    initialNote: String,
    onConfirm: (amount: Double, type: String, note: String) -> Unit,
    onDismiss: () -> Unit,
    autoFocus: Boolean = false,
    confirmText: String? = null,
) {
    val isCreated = TransactionType.CREATED.VALUE.equals(initialType)

    var amount by remember {
        mutableStateOf(TextFieldValue(initialAmount, TextRange(initialAmount.length)))
    }
    var note by remember { mutableStateOf(initialNote) }
    var selectedType by remember {
        mutableStateOf(
            if (TransactionType.WITHDRAW.VALUE.equals(initialType)) {
                TransactionType.WITHDRAW.VALUE
            } else {
                TransactionType.DEPOSIT.VALUE
            },
        )
    }

    var amountError by remember { mutableStateOf<String?>(null) }
    val invalidAmountText = stringResource(R.string.error_invalid_amount)
    val requiredText = stringResource(R.string.field_error_required)
    val positiveText = stringResource(R.string.error_amount_positive)

    val symbol = Currency.getSymbol(currency) ?: ""

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        if (!autoFocus) return@LaunchedEffect

        focusRequester.requestFocus()
        keyboardController?.show()
        delay(300)
        keyboardController?.show()
    }

    PiggsyDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = confirmText ?: stringResource(R.string.save),
        onConfirm = {

            val trimmed = amount.text.trim().replace(',', '.')
            val parsed = trimmed.toDoubleOrNull()
            when {
                trimmed.isEmpty() -> amountError = requiredText
                parsed == null -> amountError = invalidAmountText
                parsed <= 0 && !isCreated -> amountError = positiveText
                else -> onConfirm(parsed, if (isCreated) TransactionType.CREATED.VALUE else selectedType, note.trim())
            }
        },
    ) {
        Column {
            FormTextField(
                value = amount,
                onValueChange = { amount = it; amountError = null },
                label = stringResource(R.string.field_transaction_amount),
                prefix = "$symbol  ",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                isError = amountError != null,
                supportingText = amountError,
                focusRequester = focusRequester,
            )

            FormTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.field_transaction_notes),
                singleLine = false,
                minLines = 1,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (!isCreated) {
                PillSelector(
                    options = listOf(
                        TransactionType.DEPOSIT.VALUE to stringResource(R.string.radio_button_deposit),
                        TransactionType.WITHDRAW.VALUE to stringResource(R.string.radio_button_withdraw),
                    ),
                    selected = selectedType,
                    onSelect = { selectedType = it },
                    equalWidth = true,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
