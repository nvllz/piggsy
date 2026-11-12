package com.nvllz.piggsy.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.Theme
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.PreferenceUtil

class ComposeLockCoverView(context: Context) : AbstractComposeView(context) {

    private val preferences = PreferenceUtil(context)

    @Composable
    override fun Content() {


        val systemDark = remember {
            (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        }
        val darkTheme = when (preferences.getTheme()) {
            Theme.LIGHT.VALUE -> false
            Theme.DARK.VALUE -> true
            else -> systemDark
        }
        PiggsyTheme(darkTheme = darkTheme, dynamicColor = preferences.isDynamicColors()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 256.dp),
                ) {

                    Icon(
                        painterResource(R.drawable.ic_fingerprint),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Unspecified,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.biometric_prompt_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
