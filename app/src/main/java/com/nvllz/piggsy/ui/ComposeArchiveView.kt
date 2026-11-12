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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.data.saving.SavingOperation
import com.nvllz.piggsy.data.transaction.Transaction
import com.nvllz.piggsy.ui.components.MessageDialog
import com.nvllz.piggsy.ui.components.SavingDetailsDialog
import com.nvllz.piggsy.ui.components.ToastHost
import com.nvllz.piggsy.ui.components.ToastKind
import com.nvllz.piggsy.ui.components.ToastState
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.PreferenceUtil


class ComposeArchiveView(
    context: Context,
    private val listener: Listener,
) : AbstractComposeView(context) {

    interface Listener {
        fun onBack()
        fun onEdit(saving: Saving)
        fun onOperation(operation: SavingOperation, saving: Saving)


        fun onDeleteConfirmed(saving: Saving)


        fun onSavingDetails(saving: Saving)
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

    @JvmOverloads
    fun showToast(text: String, durationMs: Long = ToastState.DURATION_SHORT) {
        toast.show(text, ToastKind.SUCCESS, durationMs)
    }

    var savings by mutableStateOf<List<Saving>>(emptyList())
        private set


    fun updateSavings(list: List<Saving>) {
        savings = ArrayList(list)
    }

    fun showDeleteDialog(saving: Saving) {
        pendingDelete = saving
    }


    fun showSavingDetails(saving: Saving, transactions: ArrayList<Transaction>) {
        pendingDetails = ComposeMainView.SavingDetails(saving, transactions)
    }

    private var pendingDelete by mutableStateOf<Saving?>(null)
    private var pendingDetails by mutableStateOf<ComposeMainView.SavingDetails?>(null)

    @Composable
    override fun Content() {

        val darkTheme = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        PiggsyTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
            Box(Modifier.fillMaxSize()) {
                ArchiveScreen(
                    savings = savings,
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

            pendingDetails?.let { details ->
                val dateFormat = remember {
                    PreferenceUtil(context).getDateFormat()
                }
                SavingDetailsDialog(
                    saving = details.saving,
                    transactions = details.transactions,
                    dateFormat = dateFormat,

                    onDeadlineClick = null,

                    onTransactionCountClick = {
                        pendingDetails = null
                        listener.onOperation(SavingOperation.HISTORY, details.saving)
                    },
                    onDismiss = { pendingDetails = null },
                )
            }
        }
    }
}
