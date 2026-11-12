package com.nvllz.piggsy.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.ui.components.ScreenHeader
import com.nvllz.piggsy.util.PreferenceUtil

@Composable
fun ArchiveScreen(
    savings: List<Saving>,
    listener: ComposeArchiveView.Listener,
) {
    val context = LocalContext.current

    val dateFormat = PreferenceUtil(context).getDateFormat()
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(
            title = stringResource(R.string.toolbar_archive),
            onBack = listener::onBack,
        )

        if (savings.isEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_archive),
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(10.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.empty_indicator_archive),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = navBars.calculateBottomPadding()),
            ) {
                items(savings, key = { it.getID() }) { saving ->
                    Box(Modifier.animateItem()) {
                        SavingRow(
                            saving = saving,
                            dateFormat = dateFormat,
                            onDetails = listener::onSavingDetails,
                            onOpen = listener::onEdit,
                            onOperation = listener::onOperation,

                            onDeadlineEdit = {},
                        )
                    }
                }
            }
        }
    }
}
