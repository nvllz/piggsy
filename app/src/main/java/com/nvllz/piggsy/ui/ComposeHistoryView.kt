package com.nvllz.piggsy.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.transaction.Transaction
import com.nvllz.piggsy.ui.components.CalendarPickerDialog
import com.nvllz.piggsy.ui.components.MessageDialog
import com.nvllz.piggsy.ui.components.TimePickerDialog
import com.nvllz.piggsy.ui.components.ToastHost
import com.nvllz.piggsy.ui.components.ToastKind
import com.nvllz.piggsy.ui.components.ToastState
import com.nvllz.piggsy.ui.components.TransactionDialog
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.PreferenceUtil
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar


class ComposeHistoryView(
    context: Context,
    private val listener: Listener,
) : AbstractComposeView(context) {

    interface Listener {
        fun onBack()


        fun onEditTransaction(transaction: Transaction)


        fun onDeleteTransaction(transaction: Transaction)


        fun onDeleteConfirmed(transaction: Transaction)


        fun onTransactionUpdated(
            original: Transaction,
            amount: Double,
            type: String,
            note: String,
        )


        fun onTransactionDateTimeEdit(transaction: Transaction)


        fun onTransactionDateTimeChanged(transaction: Transaction, millis: Long)
    }

    private val preferences = PreferenceUtil(context)


    var dynamicColors by mutableStateOf(preferences.isDynamicColors())
        private set

    init {
        (context as? LifecycleOwner)?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                dynamicColors = preferences.isDynamicColors()
            }
        })
    }


    val toast = ToastState()

    fun showUndoToast(
        text: String,
        actionText: String,
        onAction: Runnable,
        onDismiss: Runnable?,
    ) {
        toast.showWithAction(
            text = text,
            kind = ToastKind.SUCCESS,
            actionText = actionText,
            onAction = { onAction.run() },
            onDismiss = onDismiss?.let { callback -> { callback.run() } },
        )
    }

    var savingName by mutableStateOf("")
        private set

    var currency by mutableStateOf("")
        private set

    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set


    var actionsEnabled by mutableStateOf(true)
        private set

    fun updateData(name: String, currency: String, list: List<Transaction>, actionsEnabled: Boolean) {
        savingName = name
        this.currency = currency
        transactions = ArrayList(list)
        this.actionsEnabled = actionsEnabled
    }


    fun updateTransactions(list: List<Transaction>) {
        transactions = ArrayList(list)
    }

    fun showDeleteDialog(transaction: Transaction) {
        pendingDelete = transaction
    }

    fun showEditDialog(transaction: Transaction) {
        pendingEdit = transaction
    }


    fun showDateTimeDialog(transaction: Transaction) {
        pendingDateTime = transaction
    }

    private var pendingDelete by mutableStateOf<Transaction?>(null)
    private var pendingEdit by mutableStateOf<Transaction?>(null)
    private var pendingDateTime by mutableStateOf<Transaction?>(null)


    private var pendingDateTimeTime by mutableStateOf<Transaction?>(null)
    private var pickedDateTimeDate: LocalDate? = null

    @Composable
    override fun Content() {

        val darkTheme = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        PiggsyTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
            Box(Modifier.fillMaxSize()) {
                HistoryScreen(
                    savingName = savingName,
                    currency = currency,
                    transactions = transactions,
                    actionsEnabled = actionsEnabled,
                    listener = listener,
                )
                ToastHost(
                    state = toast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                )
            }

            pendingDelete?.let { transaction ->
                MessageDialog(
                    title = stringResource(R.string.dialog_delete_transaction_title),
                    message = stringResource(R.string.dialog_delete_transaction_message),
                    confirmText = stringResource(R.string.delete),
                    onConfirm = {
                        pendingDelete = null
                        listener.onDeleteConfirmed(transaction)
                    },
                    onDismiss = { pendingDelete = null },
                )
            }

            pendingEdit?.let { transaction ->
                TransactionDialog(
                    title = stringResource(R.string.dialog_edit_transaction_title),
                    currency = currency,
                    initialType = transaction.getType() ?: "",
                    initialAmount = formatTransactionAmountForEdit(transaction.getAmount()),
                    initialNote = transaction.getNote() ?: "",
                    autoFocus = true,
                    onConfirm = { amount, type, note ->
                        pendingEdit = null
                        listener.onTransactionUpdated(transaction, amount, type, note)
                    },
                    onDismiss = { pendingEdit = null },
                )
            }

            pendingDateTime?.let { transaction ->
                val existingDate = Instant.ofEpochMilli(transaction.getDate())
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                CalendarPickerDialog(
                    title = stringResource(R.string.dialog_select_transaction_date),
                    initialDate = existingDate,
                    minDate = LocalDate.of(1970, 1, 1),
                    onConfirm = { date ->
                        pickedDateTimeDate = date
                        pendingDateTime = null
                        pendingDateTimeTime = transaction
                    },
                    onDismiss = { pendingDateTime = null },
                )
            }

            pendingDateTimeTime?.let { transaction ->
                val context = LocalContext.current
                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                val existingTime = Calendar.getInstance().apply {
                    timeInMillis = transaction.getDate()
                }
                TimePickerDialog(
                    title = stringResource(R.string.dialog_select_transaction_time),
                    initialHour = existingTime.get(Calendar.HOUR_OF_DAY),
                    initialMinute = existingTime.get(Calendar.MINUTE),
                    is24Hour = is24Hour,
                    onConfirm = { hour, minute ->
                        pendingDateTimeTime = null
                        pickedDateTimeDate?.let { date ->
                            pickedDateTimeDate = null
                            listener.onTransactionDateTimeChanged(
                                transaction,
                                date.atTime(hour, minute)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant().toEpochMilli(),
                            )
                        }
                    },
                    onDismiss = { pendingDateTimeTime = null },
                )
            }
        }
    }


    private fun formatTransactionAmountForEdit(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
    }
}
