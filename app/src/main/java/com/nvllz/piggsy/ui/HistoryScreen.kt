package com.nvllz.piggsy.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.Currency
import com.nvllz.piggsy.data.transaction.Transaction
import com.nvllz.piggsy.data.transaction.TransactionType
import com.nvllz.piggsy.ui.components.ScreenHeader
import com.nvllz.piggsy.ui.components.TransactionPreviewDialog
import com.nvllz.piggsy.ui.theme.Chivo
import com.nvllz.piggsy.ui.theme.ChivoMono
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.PreferenceUtil
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    savingName: String,
    currency: String,
    transactions: List<Transaction>,
    actionsEnabled: Boolean,
    listener: ComposeHistoryView.Listener,
) {
    val context = LocalContext.current
    val dateFormat = remember { PreferenceUtil(context).getDateFormat() }


    val formatter = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }
    val is24h = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val timeFormatter = remember(is24h) {
        SimpleDateFormat(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault())
    }
    val numberFormat = remember { NumberFormat.getInstance() }
    val subtotals = remember(transactions) { computeSubtotals(transactions) }
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    var previewTransaction by remember { mutableStateOf<Transaction?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(
            title = savingName,
            onBack = listener::onBack,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = navBars.calculateBottomPadding()),
        ) {
            itemsIndexed(transactions, key = { _, transaction -> transaction.getID() }) { index, transaction ->
                Box(Modifier.animateItem()) {
                    Column {


                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                        TransactionRow(
                            transaction = transaction,
                            topInset = if (index == 0) 16.dp else 14.dp,
                            formattedDate = formatter.format(transaction.getDate()),
                            formattedAmount = formatAmount(currency, transaction.getAmount(), numberFormat),
                            formattedSubtotal = subtotals.getOrNull(index)?.let {
                                formatAmount(currency, it, numberFormat)
                            },
                            onTap = { previewTransaction = transaction },
                        )
                    }
                }
            }
        }
    }

    previewTransaction?.let { transaction ->
        TransactionPreviewDialog(
            transaction = transaction,
            formattedDateTime = formatter.format(transaction.getDate()) +
                    " · " + timeFormatter.format(transaction.getDate()),
            formattedAmount = formatAmount(currency, transaction.getAmount(), numberFormat),
            actionsEnabled = actionsEnabled,
            onDateTimeClick = if (actionsEnabled) ({
                previewTransaction = null
                listener.onTransactionDateTimeEdit(transaction)
            }) else null,
            onEdit = {
                previewTransaction = null
                listener.onEditTransaction(transaction)
            },
            onDelete = {
                previewTransaction = null
                listener.onDeleteTransaction(transaction)
            },
            onDismiss = { previewTransaction = null },
        )
    }
}

private fun computeSubtotals(transactions: List<Transaction>): List<Double?> {
    var balance = 0.0
    for (t in transactions) {
        if (TransactionType.CREATED.VALUE.equals(t.getType())) {
            balance = t.getAmount()
            break
        }
    }

    val computed = arrayOfNulls<Double>(transactions.size)
    for (i in transactions.indices.reversed()) {
        val t = transactions[i]
        when {
            TransactionType.CREATED.VALUE.equals(t.getType()) -> balance = t.getAmount()
            TransactionType.DEPOSIT.VALUE.equals(t.getType()) -> {
                balance += t.getAmount()
                computed[i] = balance
            }
            TransactionType.WITHDRAW.VALUE.equals(t.getType()) -> {
                balance -= t.getAmount()
                computed[i] = balance
            }
        }
    }
    return computed.toList()
}

private fun formatAmount(currency: String, amount: Double, numberFormat: NumberFormat): String {
    val symbol = Currency.getSymbol(currency)
    return if (Currency.isSuffixCurrency(currency)) {
        numberFormat.format(amount) + if (symbol != null) " $symbol" else ""
    } else {
        (symbol ?: "") + numberFormat.format(amount)
    }
}

private enum class RowKind { CREATED, DEPOSIT, WITHDRAW }

@Composable
private fun TransactionRow(
    transaction: Transaction,
    topInset: Dp,
    formattedDate: String,
    formattedAmount: String,
    formattedSubtotal: String?,
    onTap: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val rowKind = when {
        TransactionType.DEPOSIT.VALUE.equals(transaction.getType()) -> RowKind.DEPOSIT
        TransactionType.WITHDRAW.VALUE.equals(transaction.getType()) -> RowKind.WITHDRAW
        else -> RowKind.CREATED
    }


    Column(
        Modifier
            .fillMaxWidth()
            .background(colorScheme.surface)
            .combinedClickable(
                role = Role.Button,
                onClick = onTap,

                onLongClick = onTap,
            )
            .padding(horizontal = 12.dp)
            .padding(top = topInset)
            .padding(bottom = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (icon, iconInk, iconBg) = when (rowKind) {
                RowKind.DEPOSIT -> Triple(
                    R.drawable.ic_chevron_up, PiggsyTheme.colors.deposit,
                    PiggsyTheme.colors.deposit.copy(alpha = 18f / 255f),
                )
                RowKind.WITHDRAW -> Triple(
                    R.drawable.ic_chevron_down, PiggsyTheme.colors.withdraw,
                    PiggsyTheme.colors.withdraw.copy(alpha = 18f / 255f),
                )
                RowKind.CREATED -> Triple(
                    R.drawable.ic_piggy, colorScheme.onSurface,
                    colorScheme.onSurface.copy(alpha = 18f / 255f),
                )
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = iconInk,
                    modifier = Modifier.size(16.dp),
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    formattedDate,
                    fontFamily = ChivoMono,
                    fontWeight = FontWeight.Light,
                    fontSize = 17.sp,
                    letterSpacing = (-0.07).em,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val note = transaction.getNote()
                if (note != null && !note.trim().isEmpty()) {
                    Text(
                        note,
                        fontFamily = Chivo,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Column(Modifier.padding(start = 16.dp), horizontalAlignment = Alignment.End) {
                val signedAmount = when (rowKind) {
                    RowKind.DEPOSIT -> "+$formattedAmount"
                    RowKind.WITHDRAW -> "-$formattedAmount"
                    RowKind.CREATED -> formattedAmount
                }
                Text(
                    signedAmount,
                    fontSize = 21.sp,
                    color = when (rowKind) {
                        RowKind.DEPOSIT -> PiggsyTheme.colors.deposit
                        RowKind.WITHDRAW -> PiggsyTheme.colors.withdraw
                        RowKind.CREATED -> colorScheme.onSurface
                    },
                )
                if (formattedSubtotal != null) {
                    Text(
                        formattedSubtotal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(y = (-1).dp),
                    )
                }
            }
        }
    }
}

