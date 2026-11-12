package com.nvllz.piggsy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.data.Currency
import com.nvllz.piggsy.data.saving.Saving
import com.nvllz.piggsy.data.saving.SavingOperation
import com.nvllz.piggsy.data.saving.SavingSort
import com.nvllz.piggsy.ui.components.SavingActionsDialog
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import com.nvllz.piggsy.util.AlarmUtil
import com.nvllz.piggsy.util.DateUtil
import com.nvllz.piggsy.util.PreferenceUtil
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    savings: List<Saving>,
    sortCriteria: String,
    sortAscending: Boolean,
    listener: ComposeMainView.Listener,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val dateFormat = remember(savings) { PreferenceUtil(context).getDateFormat() }

    var fabShown by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var prev = listState.firstVisibleItemIndex * 100_000 + listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex * 100_000 + listState.firstVisibleItemScrollOffset }
            .collect { value ->
                val delta = value - prev
                prev = value
                when {
                    value <= 0 -> fabShown = true
                    delta > 2 -> fabShown = false
                    delta < -2 -> fabShown = true
                }
            }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            MainTopBar(sortCriteria, sortAscending, listener)

            if (savings.isEmpty()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_piggy),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.empty_indicator_main),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                val navBars = WindowInsets.navigationBars.asPaddingValues()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 88.dp + navBars.calculateBottomPadding()),
                ) {
                    items(savings, key = { it.getID() }) { saving ->
                        Box(Modifier.animateItem()) {
                            SavingRow(
                                saving = saving,
                                dateFormat = dateFormat,
                                onDetails = { listener.onSavingDetails(it) },
                                onOpen = { listener.onItemClick(it) },
                                onOperation = { operation, s -> listener.onOperation(operation, s) },
                                onDeadlineEdit = { listener.onSavingDeadlineEdit(it) },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = fabShown,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),


            enter = scaleIn(initialScale = 0f, animationSpec = tween(200)),
            exit = scaleOut(targetScale = 0f, animationSpec = tween(150)),
        ) {


            Box(
                modifier = Modifier
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(role = Role.Button, onClick = listener::onCreateSaving)
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_plus),
                    contentDescription = stringResource(R.string.add),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun MainTopBar(
    sortCriteria: String,
    sortAscending: Boolean,
    listener: ComposeMainView.Listener,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.toolbar_main),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )

        var sortOpen by remember { mutableStateOf(false) }
        Box {
            TopBarButton(R.drawable.ic_sort, stringResource(R.string.menu_item_sort)) {
                sortOpen = true
            }
            DropdownMenu(
                expanded = sortOpen,
                onDismissRequest = { sortOpen = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                SortMenuGroup(
                    selected = sortCriteria,
                    options = listOf(
                        SavingSort.NAME.SORT to stringResource(R.string.menu_item_sort_name),
                        SavingSort.VALUE.SORT to stringResource(R.string.menu_item_sort_current_saving),
                        SavingSort.GOAL.SORT to stringResource(R.string.menu_item_sort_goal),
                        SavingSort.DEADLINE.SORT to stringResource(R.string.menu_item_sort_deadline),
                        SavingSort.CREATION.SORT to stringResource(R.string.menu_item_sort_creation),
                    ),
                    onSelect = { listener.onSortCriteria(it) },
                    onDismiss = { sortOpen = false },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                SortMenuGroup(
                    selected = if (sortAscending) {
                        SavingSort.ASCENDING.SORT
                    } else {
                        SavingSort.DESCENDING.SORT
                    },
                    options = listOf(
                        SavingSort.ASCENDING.SORT to stringResource(R.string.menu_item_sort_ascending),
                        SavingSort.DESCENDING.SORT to stringResource(R.string.menu_item_sort_descending),
                    ),
                    onSelect = { value ->
                        listener.onSortOrder(value == SavingSort.ASCENDING.SORT)
                    },
                    onDismiss = { sortOpen = false },
                )
            }
        }

        var moreOpen by remember { mutableStateOf(false) }
        Box {
            TopBarButton(R.drawable.ic_more, stringResource(R.string.menu_item_overflow)) {
                moreOpen = true
            }
            DropdownMenu(
                expanded = moreOpen,
                onDismissRequest = { moreOpen = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                listOf(
                    R.drawable.ic_backup to R.string.menu_item_backup to MainDestination.BACKUP,
                    R.drawable.ic_archive to R.string.toolbar_archive to MainDestination.ARCHIVE,
                    R.drawable.ic_settings to R.string.toolbar_settings to MainDestination.SETTINGS,
                    R.drawable.ic_info to R.string.toolbar_about to MainDestination.ABOUT,
                ).forEach { (iconLabel, destination) ->
                    val (icon, label) = iconLabel
                    DropdownMenuItem(
                        text = { Text(stringResource(label), fontSize = 17.sp) },
                        leadingIcon = {
                            Icon(
                                painterResource(icon),
                                contentDescription = null,
                                tint = PiggsyTheme.colors.accent,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        onClick = {
                            moreOpen = false
                            listener.onDestination(destination)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBarButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(start = 8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(PiggsyTheme.colors.accent.copy(alpha = 18f / 255f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = description,
            tint = PiggsyTheme.colors.accent,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SortMenuGroup(
    selected: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    options.forEach { (value, label) ->
        DropdownMenuItem(
            text = { Text(label, fontSize = 17.sp) },
            leadingIcon = {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (value == selected) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            onClick = {
                onDismiss()
                onSelect(value)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SavingRow(
    saving: Saving,
    dateFormat: String,
    onDetails: (Saving) -> Unit,
    onOpen: (Saving) -> Unit,
    onOperation: (SavingOperation, Saving) -> Unit,
    onDeadlineEdit: (Saving) -> Unit,
) {
    val percentValue = ((saving.getCurrentSaving() / saving.getGoal()) * 100).toInt()
        .coerceIn(0, 100)
    val hasGoal = saving.getGoal() > 0
    val isArchived = saving.getIsArchived() == Saving.IS_ARCHIVE
    val context = LocalContext.current

    val hasDeadline = saving.getDeadline() != AlarmUtil.NO_ALARM.toLong()
    var isDeadlineDue = false
    var deadlineText = ""
    if (hasDeadline) {
        deadlineText = DateUtil.getStringDateTime(saving.getDeadline(), dateFormat, context)


        isDeadlineDue = saving.getDeadline() <= System.currentTimeMillis()
    }

    val hasDescription = saving.getDescription() != null &&
            saving.getDescription().trim().isNotEmpty()

    val colorScheme = MaterialTheme.colorScheme
    val goalReached = saving.getCurrentSaving() >= saving.getGoal() && saving.getGoal() != 0.0
    var showActionsDialog by remember { mutableStateOf(false) }


    val dimmed: (Color) -> Color = if (isArchived) {
        { c -> lerp(c, colorScheme.background, 0.4f) }
    } else {
        { c -> c }
    }

    Box(Modifier.fillMaxWidth()) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .then(
                when {
                    goalReached -> Modifier.border(
                        2.dp, dimmed(colorScheme.primary), RoundedCornerShape(32.dp),
                    )
                    isDeadlineDue -> Modifier.border(
                        2.dp, dimmed(colorScheme.error), RoundedCornerShape(32.dp),
                    )
                    else -> Modifier
                },
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = dimmed(colorScheme.surfaceContainer)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(


            Modifier
                .then(if (isArchived) Modifier.alpha(0.6f) else Modifier)
                .combinedClickable(
                    role = Role.Button,
                    onClick = { onDetails(saving) },
                    onLongClick = { showActionsDialog = true },
                )
                .padding(4.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (hasDeadline) {

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        deadlineText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDeadlineDue) colorScheme.onErrorContainer
                        else colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (PiggsyTheme.dark) {
                                    if (isDeadlineDue) colorScheme.errorContainer
                                    else colorScheme.secondaryContainer
                                } else {


                                    colorScheme.outlineVariant
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

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isArchived) {


                    Spacer(Modifier.fillMaxWidth())
                } else {
                    UpdateButton { onOperation(SavingOperation.TRANSACTION, saving) }

                    RowActionButton(R.drawable.ic_logs, stringResource(R.string.toolbar_history)) {
                        onOperation(SavingOperation.HISTORY, saving)
                    }

                    if (hasDescription) {
                        RowActionButton(R.drawable.ic_note, stringResource(R.string.desc_note)) {
                            onDetails(saving)
                        }
                    }
                }
            }
        }
    }

    if (isArchived) {


        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 26.dp, end = 26.dp, bottom = 24.dp)
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowActionButton(R.drawable.ic_logs, stringResource(R.string.toolbar_history)) {
                onOperation(SavingOperation.HISTORY, saving)
            }

            if (hasDescription) {
                RowActionButton(R.drawable.ic_note, stringResource(R.string.desc_note)) {
                    onDetails(saving)
                }
            }

            RowActionButton(
                R.drawable.ic_unarchive,
                stringResource(R.string.desc_unarchive),
            ) {
                onOperation(SavingOperation.UNARCHIVE, saving)
            }

            RowActionButton(
                R.drawable.ic_trash,
                stringResource(R.string.delete),
                tint = colorScheme.error,
            ) {
                onOperation(SavingOperation.DELETE, saving)
            }
        }
    }
    }

    if (showActionsDialog) {
        SavingActionsDialog(
            saving = saving,
            dateFormat = dateFormat,
            onDeadlineClick = if (isArchived) null else ({ onDeadlineEdit(saving) }),
            onEdit = {
                showActionsDialog = false
                onOpen(saving)
            },
            onArchiveToggle = {
                showActionsDialog = false
                onOperation(
                    if (isArchived) SavingOperation.UNARCHIVE else SavingOperation.ARCHIVE,
                    saving,
                )
            },
            onDelete = {
                showActionsDialog = false
                onOperation(SavingOperation.DELETE, saving)
            },
            onDismiss = { showActionsDialog = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RowActionButton(
    icon: Int,
    description: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    val tip = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(description, fontSize = 14.sp) } },
        state = tip,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)

                .border(1.dp, tint.copy(alpha = 0.7f), CircleShape)
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = { scope.launch { tip.show() } },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun UpdateButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val tip = rememberTooltipState()
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(stringResource(R.string.desc_add), fontSize = 14.sp) } },
        state = tip,
    ) {
        Box(
            Modifier
                .size(width = 68.dp, height = 36.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(PiggsyTheme.colors.accent.copy(alpha = 18f / 255f))
                .border(1.dp, colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(36.dp))
                .combinedClickable(
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = { scope.launch { tip.show() } },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_plus),
                contentDescription = stringResource(R.string.desc_add),
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
