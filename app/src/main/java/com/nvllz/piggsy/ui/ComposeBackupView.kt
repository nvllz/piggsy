package com.nvllz.piggsy.ui

import android.content.Context
import android.content.res.Configuration
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nvllz.piggsy.R
import com.nvllz.piggsy.ui.components.ChoiceDialog
import com.nvllz.piggsy.ui.components.FormTextField
import com.nvllz.piggsy.ui.components.LocalToast
import com.nvllz.piggsy.ui.components.MessageDialog
import com.nvllz.piggsy.ui.components.PiggsyDialog
import com.nvllz.piggsy.ui.components.ToastHost
import com.nvllz.piggsy.ui.components.ToastKind
import com.nvllz.piggsy.ui.components.ToastState
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.BackupScheduler
import com.nvllz.piggsy.util.PreferenceUtil
import kotlinx.coroutines.delay

class ComposeBackupView(
    context: Context,
    private val listener: Listener,
) : AbstractComposeView(context) {

    interface Listener {
        fun onBack()


        fun onImportClicked()


        fun onImportWarningConfirmed()


        fun onFrequencyClicked()


        fun onBackupLocationClicked()


        fun onManualBackupClicked()


        fun onSetBackupPassword(password: CharArray)


        fun onDisableEncryptionConfirmed()

        fun onImportPasswordSubmitted(password: CharArray)
    }

    companion object {
        const val MIN_BACKUP_PASSWORD_LENGTH = 8
    }

    private val preferences = PreferenceUtil(context)


    val toast = ToastState()


    var dynamicColors by mutableStateOf(preferences.isDynamicColors())
        private set

    var frequency by mutableStateOf(0)
        private set

    var retention by mutableStateOf(5)
        private set

    var locationSummary by mutableStateOf("")
        private set

    var encrypted by mutableStateOf(false)
        private set

    init {
        (context as? LifecycleOwner)?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                dynamicColors = preferences.isDynamicColors()
                refresh()
            }
        })
    }


    fun refresh() {
        frequency = preferences.getBackupFrequency()
        retention = preferences.getBackupRetention()
        encrypted = preferences.isBackupEncrypted()
        val uriString = preferences.getBackupLocationUri()
        locationSummary = if (uriString.isNullOrEmpty()) {
            context.getString(R.string.backup_location_not_set)
        } else {
            displayPath(uriString)
        }
    }


    private fun onRetentionSelected(value: Int) {
        if (value == retention) return
        preferences.setBackupRetention(value)
        retention = value
        if (!preferences.getBackupLocationUri().isNullOrEmpty()) {
            BackupScheduler.scheduleImmediateCleanup(context)
        }
    }


    private fun onEncryptionToggled(enable: Boolean) {
        if (enable == encrypted) return
        if (enable) showSetPasswordDialog() else showDisableEncryption = true
    }

    private fun displayPath(uriString: String): String {
        return try {
            val treeId = DocumentsContract.getTreeDocumentId(android.net.Uri.parse(uriString))
            if (treeId.contains(":")) treeId.substring(treeId.indexOf(':') + 1) else treeId
        } catch (_: Exception) {
            android.net.Uri.parse(uriString).lastPathSegment ?: ""
        }
    }

    fun showToast(text: String, kind: ToastKind) {
        toast.show(text, kind)
    }

    fun showImportWarning() {
        showImportWarning = true
    }

    fun showFrequencyDialog() {
        showFrequency = true
    }

    fun showSetPasswordDialog() {
        setPassword = TextFieldValue("")
        confirmPassword = TextFieldValue("")
        passwordMismatch = false
        showSetPassword = true
    }

    fun showImportPasswordDialog() {
        importPassword = TextFieldValue("")
        importPasswordWorking = false
        showImportPassword = true
    }

    fun onBackupPasswordSet(success: Boolean) {
        setPasswordWorking = false
        if (success) {
            showSetPassword = false
            refresh()
            toast.show(context.getString(R.string.backup_encrypted_snackbar), ToastKind.INFO)
        } else {
            toast.show(context.getString(R.string.backup_encryption_failed), ToastKind.ERROR)
        }
    }

    fun onImportPasswordAccepted() {
        importPasswordWorking = false
        showImportPassword = false
    }

    fun onPasswordWrong() {
        importPasswordWorking = false
        toast.show(context.getString(R.string.backup_password_wrong), ToastKind.ERROR)
    }

    fun onImportFailed() {
        importPasswordWorking = false
        toast.show(context.getString(R.string.cannot_open_file), ToastKind.ERROR)
    }

    private var showImportWarning by mutableStateOf(false)
    private var showFrequency by mutableStateOf(false)
    private var showSetPassword by mutableStateOf(false)
    private var showDisableEncryption by mutableStateOf(false)
    private var showImportPassword by mutableStateOf(false)

    private var setPassword by mutableStateOf(TextFieldValue(""))
    private var confirmPassword by mutableStateOf(TextFieldValue(""))
    private var passwordMismatch by mutableStateOf(false)
    private var setPasswordWorking by mutableStateOf(false)

    private var importPassword by mutableStateOf(TextFieldValue(""))
    private var importPasswordWorking by mutableStateOf(false)

    @Composable
    override fun Content() {

        val darkTheme = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        PiggsyTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(LocalToast provides toast) {
                    BackupScreen(
                        frequency = frequency,
                        retention = retention,
                        locationSummary = locationSummary,
                        encrypted = encrypted,
                        listener = listener,
                        onRetentionSelected = ::onRetentionSelected,
                        onEncryptionToggled = ::onEncryptionToggled,
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

            if (showImportWarning) {
                MessageDialog(
                    title = stringResource(R.string.warning),
                    message = stringResource(R.string.import_warning_desc),
                    confirmText = stringResource(R.string.import_data),
                    onConfirm = {
                        showImportWarning = false
                        listener.onImportWarningConfirmed()
                    },
                    onDismiss = { showImportWarning = false },
                )
            }

            if (showFrequency) {
                val context = context
                val frequencyOptions = remember(context) {
                    val entries = context.resources.getStringArray(R.array.backup_frequency_entries)
                    val values = context.resources.getStringArray(R.array.backup_frequency_values)
                    entries.zip(values) { label, value -> value.toInt() to label }
                }
                ChoiceDialog(
                    title = stringResource(R.string.backup_frequency),
                    options = frequencyOptions,
                    selected = frequency,
                    onSelect = { value ->
                        preferences.setBackupFrequency(value)
                        frequency = value
                    },
                    onDismiss = { showFrequency = false },
                )
            }

            if (showDisableEncryption) {
                MessageDialog(
                    title = stringResource(R.string.backup_encryption_disable_title),
                    message = stringResource(R.string.backup_encryption_disable_message),
                    confirmText = stringResource(R.string.backup_encryption_disable_button),
                    onConfirm = {
                        showDisableEncryption = false
                        listener.onDisableEncryptionConfirmed()
                    },
                    onDismiss = { showDisableEncryption = false },
                )
            }

            if (showSetPassword) {
                SetPasswordDialog()
            }

            if (showImportPassword) {
                ImportPasswordDialog()
            }
        }
    }

    @Composable
    private fun SetPasswordDialog() {
        val emptyText = stringResource(R.string.backup_password_empty)
        val tooShortText = stringResource(R.string.backup_password_too_short, MIN_BACKUP_PASSWORD_LENGTH)
        val mismatchText = stringResource(R.string.backup_password_mismatch)

        PiggsyDialog(
            title = stringResource(R.string.backup_encryption_enable_title),
            onDismiss = { if (!setPasswordWorking) showSetPassword = false },
            confirmText = stringResource(R.string.set),
            confirmEnabled = !setPasswordWorking,
            onConfirm = {
                val password = setPassword.text
                val confirm = confirmPassword.text
                when {
                    password.isEmpty() -> toast.show(emptyText, ToastKind.ERROR)
                    password.length < MIN_BACKUP_PASSWORD_LENGTH -> toast.show(tooShortText, ToastKind.ERROR)
                    password != confirm -> {
                        passwordMismatch = true
                    }
                    else -> {
                        passwordMismatch = false
                        setPasswordWorking = true
                        listener.onSetBackupPassword(password.toCharArray())
                    }
                }
            },
        ) {
            Column {
                Text(
                    stringResource(R.string.backup_encryption_enable_message),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                FormTextField(
                    value = setPassword,
                    onValueChange = {
                        setPassword = it
                        passwordMismatch = false
                    },
                    label = stringResource(R.string.backup_password_hint),
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !setPasswordWorking,
                )
                FormTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        passwordMismatch = false
                    },
                    label = stringResource(R.string.backup_password_confirm_hint),
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !setPasswordWorking,
                    isError = passwordMismatch,
                    supportingText = if (passwordMismatch) mismatchText else null,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (setPasswordWorking) {
                    LinearProgressIndicator(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun ImportPasswordDialog() {
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {


            focusRequester.requestFocus()
            keyboardController?.show()
            delay(300)
            keyboardController?.show()
        }

        PiggsyDialog(
            title = stringResource(R.string.backup_encrypted_file_title),
            onDismiss = { if (!importPasswordWorking) showImportPassword = false },
            confirmText = stringResource(R.string.import_data),
            confirmEnabled = !importPasswordWorking,
            onConfirm = {
                val password = importPassword.text
                if (password.isNotEmpty()) {
                    importPasswordWorking = true
                    listener.onImportPasswordSubmitted(password.toCharArray())
                }
            },
        ) {
            Column {
                FormTextField(
                    value = importPassword,
                    onValueChange = { importPassword = it },
                    label = stringResource(R.string.backup_password_hint),
                    keyboardType = KeyboardType.Password,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !importPasswordWorking,
                    focusRequester = focusRequester,
                )
                if (importPasswordWorking) {
                    LinearProgressIndicator(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
