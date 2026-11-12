package com.nvllz.piggsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.data.transaction.Transaction
import com.nvllz.piggsy.data.transaction.TransactionType
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.DateUtil
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SavingDetailsDialog(
    saving: Saving,
    transactions: List<Transaction>,
    dateFormat: String,
    onDeadlineClick: (() -> Unit)?,
    onTransactionCountClick: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val description = saving.getDescription()?.trim()?.takeIf { it.isNotEmpty() }


    val createdAt = remember(transactions) {
        transactions.filter { TransactionType.CREATED.VALUE == it.getType() }
            .minOfOrNull { it.getDate() }
            ?: transactions.minOfOrNull { it.getDate() }
    }
    val goalReachedAt = remember(saving, transactions) { computeGoalReachedAt(saving, transactions) }
    val context = LocalContext.current
    val is24h = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val dateTimeFormatter = remember(dateFormat, is24h) {
        val timePattern = if (is24h) "HH:mm" else "h:mm a"
        SimpleDateFormat("$dateFormat · $timePattern", Locale.getDefault())
    }

    SavingDialogShell(onDismiss = onDismiss) {
        Column(
            Modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SavingCardPreview(saving, dateFormat, onDeadlineClick)

            HorizontalDivider(
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 12.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            description?.let {
                Column(
                    Modifier
                        .fillMaxWidth()

                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(PiggsyTheme.colors.accent.copy(alpha = 18f / 255f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        it,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            createdAt?.let {
                DetailRow(
                    icon = R.drawable.ic_calendar,
                    label = stringResource(
                        R.string.created_on,
                        DateUtil.getStringDate(it, dateFormat),
                    ),
                )
            }
            goalReachedAt?.let { reachedAt ->
                DetailRow(
                    icon = R.drawable.ic_check,
                    label = stringResource(
                        R.string.reached_on,
                        dateTimeFormatter.format(reachedAt),
                    ),
                )
            }
            if (saving.getIsArchived() == Saving.IS_ARCHIVE && saving.getArchivedAt() > 0) {
                DetailRow(
                    icon = R.drawable.ic_archive,
                    label = stringResource(
                        R.string.archived_on,
                        DateUtil.getStringDate(saving.getArchivedAt(), dateFormat),
                    ),
                )
            }
            DetailRow(
                icon = R.drawable.ic_logs,
                label = pluralStringResource(
                    R.plurals.transaction_count,
                    transactions.size,
                    transactions.size,
                ),
                onClick = onTransactionCountClick,
            )
        }
    }
}


private fun computeGoalReachedAt(saving: Saving, transactions: List<Transaction>): Long? {
    val goal = saving.getGoal()
    if (goal <= 0.0) return null

    var balance = 0.0
    for (t in transactions.sortedBy { it.getDate() }) {
        when {
            TransactionType.CREATED.VALUE.equals(t.getType()) -> balance = t.getAmount()
            TransactionType.DEPOSIT.VALUE.equals(t.getType()) -> balance += t.getAmount()
            TransactionType.WITHDRAW.VALUE.equals(t.getType()) -> balance -= t.getAmount()
            else -> continue
        }
        if (balance >= goal) return t.getDate()
    }
    return null
}

@Composable
private fun DetailRow(
    icon: Int,
    label: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = PiggsyTheme.colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 15.sp,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
