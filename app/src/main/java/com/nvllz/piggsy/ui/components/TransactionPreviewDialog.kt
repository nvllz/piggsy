package com.nvllz.piggsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.transaction.Transaction
import com.nvllz.piggsy.data.transaction.TransactionType
import com.nvllz.piggsy.ui.theme.PiggsyTheme

private enum class PreviewKind { CREATED, DEPOSIT, WITHDRAW }

@Composable
fun TransactionPreviewDialog(
    transaction: Transaction,
    formattedDateTime: String,
    formattedAmount: String,
    actionsEnabled: Boolean,
    onDateTimeClick: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val kind = when {
        TransactionType.DEPOSIT.VALUE.equals(transaction.getType()) -> PreviewKind.DEPOSIT
        TransactionType.WITHDRAW.VALUE.equals(transaction.getType()) -> PreviewKind.WITHDRAW
        else -> PreviewKind.CREATED
    }
    val canDelete = kind != PreviewKind.CREATED
    val note = transaction.getNote()?.trim()?.takeIf { it.isNotEmpty() }

    SavingDialogShell(onDismiss = onDismiss) {
        Column(
            Modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                formattedDateTime,
                fontSize = 17.sp,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)


                    .then(
                        if (onDateTimeClick != null) {
                            Modifier.clickable(role = Role.Button, onClick = onDateTimeClick)
                        } else {
                            Modifier
                        },
                    ),
            )

            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val signedAmount = when (kind) {
                    PreviewKind.DEPOSIT -> "+$formattedAmount"
                    PreviewKind.WITHDRAW -> "-$formattedAmount"
                    PreviewKind.CREATED -> formattedAmount
                }
                Text(
                    signedAmount,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (kind) {
                        PreviewKind.DEPOSIT -> PiggsyTheme.colors.deposit
                        PreviewKind.WITHDRAW -> PiggsyTheme.colors.withdraw
                        PreviewKind.CREATED -> colorScheme.onSurface
                    },
                )
            }

            if (actionsEnabled) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 12.dp),
                    color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                )
            }

            note?.let {
                Column(
                    Modifier
                        .fillMaxWidth()

                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PiggsyTheme.colors.accent.copy(alpha = 18f / 255f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        it,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = colorScheme.onSurface,
                    )
                }
            }

            if (actionsEnabled) {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DialogActionRow(
                        icon = R.drawable.ic_edit,
                        label = stringResource(R.string.edit),
                        tint = PiggsyTheme.colors.accent,
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                    )
                    if (canDelete) {
                        DialogActionRow(
                            icon = R.drawable.ic_trash,
                            label = stringResource(R.string.delete),
                            tint = colorScheme.error,
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
