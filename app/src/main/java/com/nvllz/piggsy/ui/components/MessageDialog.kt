package com.nvllz.piggsy.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun MessageDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(android.R.string.ok),
) {
    PiggsyDialog(title, onDismiss, confirmText, onConfirm) { Text(message) }
}
