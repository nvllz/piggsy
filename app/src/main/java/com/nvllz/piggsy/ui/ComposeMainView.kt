package com.nvllz.piggsy.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.data.saving.SavingOperation
import com.nvllz.piggsy.data.transaction.Transaction
import com.nvllz.piggsy.ui.components.CalendarPickerDialog
import com.nvllz.piggsy.ui.components.LocalToast
import com.nvllz.piggsy.ui.components.MessageDialog
import com.nvllz.piggsy.ui.components.SavingDetailsDialog
import com.nvllz.piggsy.ui.components.TimePickerDialog
import com.nvllz.piggsy.ui.components.ToastHost
import com.nvllz.piggsy.ui.components.ToastKind
import com.nvllz.piggsy.ui.components.ToastState
import com.nvllz.piggsy.ui.components.TransactionDialog
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.AlarmUtil
import com.nvllz.piggsy.util.PreferenceUtil
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

enum class MainDestination { BACKUP, ARCHIVE, SETTINGS, ABOUT }

class ComposeMainView(
    context: Context,
    private val listener: Listener,
) : AbstractComposeView(context) {

    interface Listener {
        fun onItemClick(saving: Saving)
        fun onOperation(operation: SavingOperation, saving: Saving)


        fun onDeleteConfirmed(saving: Saving)
        fun onSortCriteria(criteria: String)
        fun onSortOrder(ascending: Boolean)
        fun onDestination(destination: MainDestination)
        fun onCreateSaving()


        fun onTransactionConfirmed(saving: Saving, amount: Double, type: String, note: String)


        fun onSavingDetails(saving: Saving)


        fun onSavingDeadlineEdit(saving: Saving)


        fun onSavingDeadlineChanged(saving: Saving, deadlineMillis: Long)


        fun onOpenHistory(saving: Saving)


        fun onGrantAlarmPermission()
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

    fun showToast(text: String, kind: ToastKind = ToastKind.SUCCESS, durationMs: Long = ToastState.DURATION_SHORT) {
        toast.show(text, kind, durationMs)
    }


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

    var savings by mutableStateOf<List<Saving>>(emptyList())
        private set

    var sortCriteria by mutableStateOf("")
        private set

    var sortAscending by mutableStateOf(true)
        private set

    fun updateSavings(list: List<Saving>) {
        savings = list
    }

    fun updateSort(criteria: String, ascending: Boolean) {
        sortCriteria = criteria
        sortAscending = ascending
    }

    fun showDeleteDialog(saving: Saving) {
        pendingDelete = saving
    }


    fun showTransactionDialog(saving: Saving) {
        pendingTransaction = saving
    }


    fun showSavingDetails(saving: Saving, transactions: ArrayList<Transaction>) {
        pendingDetails = SavingDetails(saving, transactions)
    }


    fun requestDeadlinePicker(saving: Saving) {
        pendingDeadlineSaving = saving
        showDeadlineDatePicker = true
    }


    fun requestAlarmPermissionDialog() {
        showAlarmPermissionDialog = true
    }

    private var pendingDelete by mutableStateOf<Saving?>(null)
    private var pendingTransaction by mutableStateOf<Saving?>(null)
    private var pendingDetails by mutableStateOf<SavingDetails?>(null)


    private var pendingDeadlineSaving by mutableStateOf<Saving?>(null)
    private var showDeadlineDatePicker by mutableStateOf(false)
    private var showDeadlineTimePicker by mutableStateOf(false)
    private var pickedDeadlineDate: LocalDate? = null
    private var showAlarmPermissionDialog by mutableStateOf(false)


    class SavingDetails(val saving: Saving, val transactions: List<Transaction>)

    @Composable
    override fun Content() {


        val darkTheme = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        PiggsyTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalToast provides toast) {
                    MainScreen(
                        savings = savings,
                        sortCriteria = sortCriteria,
                        sortAscending = sortAscending,
                        listener = listener,
                    )
                }
                ToastHost(
                    state = toast,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                )
            }

            pendingDelete?.let { saving ->
                MessageDialog(
                    title = stringResource(R.string.dialog_title_delete_saving),
                    message = stringResource(R.string.dialog_message_delete_saving),
                    confirmText = stringResource(R.string.delete),
                    onConfirm = {
                        pendingDelete = null
                        listener.onDeleteConfirmed(saving)
                    },
                    onDismiss = { pendingDelete = null },
                )
            }

            pendingTransaction?.let { saving ->
                TransactionDialog(
                    title = stringResource(R.string.dialog_title_create_transaction),
                    currency = saving.getCurrency(),
                    initialType = null,
                    initialAmount = "",
                    initialNote = "",
                    autoFocus = true,
                    confirmText = stringResource(R.string.add),
                    onConfirm = { amount, type, note ->
                        pendingTransaction = null
                        listener.onTransactionConfirmed(saving, amount, type, note)
                    },
                    onDismiss = { pendingTransaction = null },
                )
            }

            pendingDetails?.let { details ->
                val dateFormat = remember {
                    PreferenceUtil(context).getDateFormat()
                }
                SavingDetailsDialog(
                    saving = details.saving,
                    transactions = details.transactions,
                    dateFormat = dateFormat,


                    onDeadlineClick = if (details.saving.getIsArchived() == Saving.IS_ARCHIVE) {
                        null
                    } else {
                        {
                            listener.onSavingDeadlineEdit(details.saving)
                        }
                    },

                    onTransactionCountClick = {
                        pendingDetails = null
                        listener.onOpenHistory(details.saving)
                    },
                    onDismiss = { pendingDetails = null },
                )
            }

            pendingDeadlineSaving?.let { saving ->
                if (showDeadlineDatePicker) {
                    val initialDate = if (saving.getDeadline() != AlarmUtil.NO_ALARM.toLong()) {
                        Instant.ofEpochMilli(saving.getDeadline())
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    } else {
                        LocalDate.now()
                    }
                    CalendarPickerDialog(
                        title = stringResource(R.string.dialog_select_deadline_date),
                        initialDate = initialDate,
                        minDate = LocalDate.now(),
                        onConfirm = { date ->
                            showDeadlineDatePicker = false
                            pickedDeadlineDate = date
                            showDeadlineTimePicker = true
                        },
                        onDismiss = { showDeadlineDatePicker = false },
                    )
                }

                if (showDeadlineTimePicker) {
                    val pickerContext = LocalContext.current
                    val is24Hour = android.text.format.DateFormat.is24HourFormat(pickerContext)
                    val initialTime = if (saving.getDeadline() != AlarmUtil.NO_ALARM.toLong()) {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = saving.getDeadline()
                        cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
                    } else {
                        12 to 0
                    }
                    TimePickerDialog(
                        title = stringResource(R.string.dialog_select_deadline_time),
                        initialHour = initialTime.first,
                        initialMinute = initialTime.second,
                        is24Hour = is24Hour,
                        onConfirm = { hour, minute ->
                            showDeadlineTimePicker = false
                            pickedDeadlineDate?.let { date ->
                                pickedDeadlineDate = null
                                listener.onSavingDeadlineChanged(
                                    saving,
                                    date.atTime(hour, minute)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli(),
                                )

                                pendingDetails?.let { details ->
                                    if (details.saving.getID() == saving.getID()) {
                                        pendingDetails =
                                            SavingDetails(details.saving, details.transactions)
                                    }
                                }
                            }
                        },
                        onDismiss = { showDeadlineTimePicker = false },
                    )
                }
            }

            if (showAlarmPermissionDialog) {
                MessageDialog(
                    title = stringResource(R.string.dialog_title_request_alarm_permission),
                    message = stringResource(R.string.dialog_message_request_alarm_permission),
                    confirmText = stringResource(R.string.grant),
                    onConfirm = {
                        showAlarmPermissionDialog = false
                        listener.onGrantAlarmPermission()
                    },
                    onDismiss = { showAlarmPermissionDialog = false },
                )
            }
        }
    }
}
