package com.nvllz.piggsy.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.ui.components.ScreenHeader
import com.nvllz.piggsy.ui.components.SettingDivider
import com.nvllz.piggsy.ui.components.SettingGroup
import com.nvllz.piggsy.ui.components.SettingRow
import com.nvllz.piggsy.ui.theme.PiggsyTheme

private const val URL_SOURCE = "https://github.com/nvllz/piggsy"
private const val URL_LICENSE = "https://www.gnu.org/licenses/gpl-3.0.html"

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    versionName: String,
    onVersionChipClick: () -> Unit,
) {
    val colors = PiggsyTheme.colors
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val authorUrl = stringResource(R.string.link_app_author)
    val ogCreatorUrl = stringResource(R.string.link_app_og_creator)

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Column(Modifier.fillMaxSize().background(scheme.background).statusBarsPadding()) {
        ScreenHeader(stringResource(R.string.toolbar_about), onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(scheme.onSurface.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.requiredSize(106.dp),
                    )
                }
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(top = 16.dp),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Text(
                    "${stringResource(R.string.preference_app_version)} $versionName",
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(CircleShape)
                        .background(colors.accent.copy(alpha = 18f / 255f))
                        .border(1.dp, colors.accent.copy(alpha = 0.3f), CircleShape)
                        .clickable(role = Role.Button, onClick = onVersionChipClick)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                )
            }

            SettingGroup {
                SettingRow(
                    title = stringResource(R.string.preference_app_source_code_title),
                    summary = stringResource(R.string.preference_app_source_code_summary),
                    icon = R.drawable.ic_about_code,
                    onClick = { open(URL_SOURCE) },
                )
                SettingDivider()
                SettingRow(
                    title = stringResource(R.string.preference_app_author),
                    summary = stringResource(R.string.app_author),
                    icon = R.drawable.ic_about_author,
                    onClick = { open(authorUrl) },
                )
                SettingDivider()
                SettingRow(
                    title = stringResource(R.string.preference_app_og_creator),
                    summary = stringResource(R.string.app_og_creator),
                    icon = R.drawable.ic_bulb,
                    onClick = { open(ogCreatorUrl) },
                )
            }

            Spacer(Modifier.height(12.dp))

            SettingGroup {
                SettingRow(
                    title = stringResource(R.string.preference_app_license),
                    summary = stringResource(R.string.app_license),
                    icon = R.drawable.ic_about_license,
                    onClick = { open(URL_LICENSE) },
                )
            }

            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}
