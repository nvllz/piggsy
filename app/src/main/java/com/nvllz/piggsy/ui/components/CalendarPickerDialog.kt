package com.nvllz.piggsy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.piggsy.R
import com.nvllz.piggsy.ui.theme.PiggsyTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale


@Composable
fun CalendarPickerDialog(
    title: String,
    initialDate: LocalDate,
    minDate: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(initialDate) }
    var shownMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    val colorScheme = MaterialTheme.colorScheme
    val accent = PiggsyTheme.colors.accent

    PiggsyDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(selected) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { shownMonth = shownMonth.minusMonths(1) },
                enabled = shownMonth > YearMonth.from(minDate),
            ) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.desc_previous_month),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(180f),
                )
            }
            Text(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(shownMonth),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { shownMonth = shownMonth.plusMonths(1) }) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Row(Modifier.padding(vertical = 8.dp)) {
            (0L until 7L).forEach { offset ->
                Text(
                    firstDay.plus(offset)
                        .getDisplayName(TextStyle.NARROW_STANDALONE, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        val leading = (shownMonth.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
        Column {
            repeat(6) { week ->
                Row {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - leading + 1
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (day in 1..shownMonth.lengthOfMonth()) {
                                val date = shownMonth.atDay(day)
                                val enabled = !date.isBefore(minDate)
                                val isSelected = date == selected
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) accent else Color.Transparent)
                                        .clickable(enabled = enabled) { selected = date },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        day.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            colorScheme.background
                                        } else {
                                            colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.3f)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
