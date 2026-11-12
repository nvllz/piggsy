package com.nvllz.piggsy.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.ui.components.CalendarPickerDialog
import com.nvllz.piggsy.ui.components.MessageDialog
import com.nvllz.piggsy.ui.components.TimePickerDialog
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.AlarmUtil
import com.nvllz.piggsy.util.PreferenceUtil
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar


class ComposeSavingFormView(
    context: Context,
    private val isEdit: Boolean,
    private val initial: Saving,
    private val listener: Listener,
) : AbstractComposeView(context) {

    interface Listener {

        fun onDeadlineFieldClick()
        fun onBack()


        fun onDelete()
        fun onDeleteConfirmed()


        fun onGrantAlarmPermission()
        fun onSave(
            name: String,
            currencyCode: String,
            current: String,
            goal: String,
            description: String,
            archiveWhenCompleted: Boolean,
        )
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

    private var deadlineState by mutableStateOf(initial.getDeadline())
        private set

    fun setDeadline(millis: Long) {
        deadlineState = millis
    }

    fun getDeadline(): Long = deadlineState

    fun requestDeleteDialog() {
        showDeleteDialog = true
    }

    fun requestAlarmPermissionDialog() {
        showAlarmPermissionDialog = true
    }


    fun requestDeadlinePicker() {
        showDeadlineDatePicker = true
    }

    private var showDeleteDialog by mutableStateOf(false)
    private var showAlarmPermissionDialog by mutableStateOf(false)


    private var showDeadlineDatePicker by mutableStateOf(false)
    private var showDeadlineTimePicker by mutableStateOf(false)
    private var pickedDeadlineDate: LocalDate? = null

    @Composable
    override fun Content() {

        val darkTheme = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        PiggsyTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
            SavingFormScreen(
                isEdit = isEdit,
                initial = initial,
                deadline = deadlineState,
                onDeadlinePick = listener::onDeadlineFieldClick,
                onDeadlineClear = { deadlineState = AlarmUtil.NO_ALARM.toLong() },
                onBack = listener::onBack,
                onDelete = listener::onDelete,
                onSave = listener::onSave,
            )

            if (showDeleteDialog) {
                MessageDialog(
                    title = stringResource(R.string.dialog_title_delete_saving),
                    message = stringResource(R.string.dialog_message_delete_saving),
                    confirmText = stringResource(R.string.delete),
                    onConfirm = {
                        showDeleteDialog = false
                        listener.onDeleteConfirmed()
                    },
                    onDismiss = { showDeleteDialog = false },
                )
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

            if (showDeadlineDatePicker) {
                val initialDate = if (deadlineState != AlarmUtil.NO_ALARM.toLong()) {
                    Instant.ofEpochMilli(deadlineState).atZone(ZoneId.systemDefault()).toLocalDate()
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
                val context = LocalContext.current
                val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
                val initialTime = if (deadlineState != AlarmUtil.NO_ALARM.toLong()) {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = deadlineState
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
                            setDeadline(
                                date.atTime(hour, minute)
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant().toEpochMilli(),
                            )
                            pickedDeadlineDate = null
                        }
                    },
                    onDismiss = { showDeadlineTimePicker = false },
                )
            }
        }
    }
}
