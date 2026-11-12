package com.nvllz.piggsy.ui

import android.app.Activity
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.nvllz.piggsy.R
import com.nvllz.piggsy.ui.components.LocalToast
import com.nvllz.piggsy.ui.components.ToastHost
import com.nvllz.piggsy.ui.components.ToastKind
import com.nvllz.piggsy.ui.components.ToastState
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.PreferenceUtil

class ComposeAboutView(context: Context) : AbstractComposeView(context) {

    private val preferences = PreferenceUtil(context)


    val toast = ToastState()


    var dynamicColors by mutableStateOf(preferences.isDynamicColors())
        private set

    private var easterEggCounter = 0

    init {
        (context as? LifecycleOwner)?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                dynamicColors = preferences.isDynamicColors()
            }
        })
    }

    fun onVersionChipClick() {
        easterEggCounter++
        if (easterEggCounter == 7) {
            toast.show(context.getString(R.string.app_easter_egg), ToastKind.INFO)
        }
    }

    @Composable
    override fun Content() {

        val darkTheme = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val versionName = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
            } catch (_: Exception) {
                ""
            }
        }

        PiggsyTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
            Box(Modifier.fillMaxSize()) {
                androidx.compose.runtime.CompositionLocalProvider(LocalToast provides toast) {
                    AboutScreen(
                        onBack = { (context as? Activity)?.finish() },
                        versionName = versionName,
                        onVersionChipClick = ::onVersionChipClick,
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
        }
    }
}
