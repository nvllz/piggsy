package com.nvllz.piggsy.ui.components

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.Currency
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.AlarmUtil
import com.nvllz.piggsy.util.DateUtil


@Composable
internal fun SavingCardPreview(
    saving: Saving,
    dateFormat: String,
    onDeadlineClick: (() -> Unit)? = null,
) {
    val isArchived = saving.getIsArchived() == Saving.IS_ARCHIVE
    val context = LocalContext.current
    val percentValue = ((saving.getCurrentSaving() / saving.getGoal()) * 100).toInt()
        .coerceIn(0, 100)
    val hasGoal = saving.getGoal() > 0
    val colorScheme = MaterialTheme.colorScheme

    val hasDeadline = saving.getDeadline() != AlarmUtil.NO_ALARM.toLong()
    var deadlineText = ""
    var isDeadlineDue = false
    if (hasDeadline) {
        deadlineText = DateUtil.getStringDateTime(saving.getDeadline(), dateFormat, context)

        isDeadlineDue = saving.getDeadline() <= System.currentTimeMillis()
    }

    val contentAlpha = if (isArchived) 0.6f else 1f

    Column(Modifier.fillMaxWidth()) {
        if (hasDeadline) {

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.deadline_on, deadlineText),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDeadlineDue) colorScheme.onErrorContainer
                    else colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (PiggsyTheme.dark) {
                                if (isDeadlineDue) colorScheme.errorContainer
                                else colorScheme.secondaryContainer
                            } else {


                                colorScheme.outlineVariant
                            },
                        )
                        .then(
                            if (onDeadlineClick != null) {
                                Modifier.clickable(role = Role.Button, onClick = onDeadlineClick)
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Text(
            saving.getName(),
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.primary,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(contentAlpha)
                .fillMaxWidth()
                .padding(top = 10.dp),
        )

        CompositionLocalProvider(
            LocalLayoutDirection provides if (Currency.isRTLCurrency(saving.getCurrency())) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            },
        ) {
            Row(
                Modifier
                    .alpha(contentAlpha)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Currency.formatAmount(saving.getCurrency(), saving.getCurrentSaving()),
                    fontSize = 24.sp,
                    color = colorScheme.primary,
                    letterSpacing = 0.02.sp,
                )
                if (hasGoal) {
                    Text(
                        " / ",
                        fontSize = 24.sp,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Text(
                        Currency.formatAmount(saving.getCurrency(), saving.getGoal()),
                        fontSize = 24.sp,
                        color = colorScheme.onSurface.copy(alpha = 0.7f),
                        letterSpacing = 0.02.sp,
                    )
                }
            }
        }

        if (hasGoal) {
            Row(
                Modifier
                    .alpha(contentAlpha)
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { percentValue / 100f },
                    modifier = Modifier.weight(1f),


                    color = if (isDeadlineDue && percentValue < 100) {
                        colorScheme.error
                    } else {
                        colorScheme.primary
                    },
                    trackColor = colorScheme.outlineVariant,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    "$percentValue%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {

                        percentValue >= 100 -> if (PiggsyTheme.dark) colorScheme.onPrimary
                        else colorScheme.onSurface
                        isDeadlineDue -> colorScheme.onErrorContainer

                        else -> colorScheme.onSecondaryContainer
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                percentValue >= 100 -> if (PiggsyTheme.dark) colorScheme.primary
                                else colorScheme.outlineVariant
                                isDeadlineDue -> if (PiggsyTheme.dark) colorScheme.errorContainer
                                else colorScheme.outlineVariant
                                else -> if (PiggsyTheme.dark) colorScheme.secondaryContainer
                                else colorScheme.outlineVariant
                            },
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}


@Composable
internal fun SavingDialogShell(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)

    Dialog(onDismissRequest = onDismiss) {
        val window = generateSequence(LocalView.current) { it.parent as? View }
            .filterIsInstance<DialogWindowProvider>().firstOrNull()?.window
        LaunchedEffect(window) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setDimAmount(0.8f)
        }
        Column(
            Modifier
                .clip(shape)
                .background(PiggsyTheme.colors.dialogSurface)
                .border(1.dp, colorScheme.onSurface.copy(alpha = 0.11f), shape)
                .padding(24.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun DialogActionRow(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val wash = if (isDarkTheme) {
        Color.White.copy(alpha = 0.025f)
    } else {
        Color.Black.copy(alpha = 0.045f)
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(wash)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}


@Composable
fun SavingActionsDialog(
    saving: Saving,
    dateFormat: String,
    onDeadlineClick: (() -> Unit)?,
    onEdit: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isArchived = saving.getIsArchived() == Saving.IS_ARCHIVE
    val colorScheme = MaterialTheme.colorScheme

    SavingDialogShell(onDismiss = onDismiss) {
        SavingCardPreview(saving, dateFormat, onDeadlineClick)

        HorizontalDivider(
            modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 12.dp, bottom = 12.dp),
            color = colorScheme.outlineVariant.copy(alpha = 0.4f),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isArchived) {
                DialogActionRow(
                    icon = R.drawable.ic_edit,
                    label = stringResource(R.string.edit),
                    tint = PiggsyTheme.colors.accent,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                )
            }
            DialogActionRow(
                icon = if (isArchived) R.drawable.ic_unarchive else R.drawable.ic_archive,
                label = stringResource(
                    if (isArchived) R.string.desc_unarchive else R.string.toolbar_archive,
                ),
                tint = PiggsyTheme.colors.accent,
                onClick = onArchiveToggle,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        DialogActionRow(
            icon = R.drawable.ic_trash,
            label = stringResource(R.string.delete),
            tint = colorScheme.error,
            onClick = onDelete,
        )
    }
}
