package com.nvllz.piggsy.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nvllz.piggsy.ui.theme.PiggsyTheme

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType? = null,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester? = null,
) {


    var textFieldValueState by remember { mutableStateOf(TextFieldValue(value)) }
    val textFieldValue = textFieldValueState.copy(text = value)
    FormTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValueState = newValue
            onValueChange(newValue.text)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        prefix = prefix,
        singleLine = singleLine,
        minLines = minLines,
        keyboardType = keyboardType,
        trailing = trailing,
        isError = isError,
        supportingText = supportingText,
        readOnly = readOnly,
        enabled = enabled,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        focusRequester = focusRequester,
    )
}


@Composable
fun FormTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType? = null,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester? = null,
) {
    Column(modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            shape = RoundedCornerShape(16.dp),
            singleLine = singleLine,
            minLines = minLines,
            readOnly = readOnly,
            enabled = enabled,
            visualTransformation = visualTransformation,


            placeholder = if (placeholder != null) {
                val hint = placeholder
                @Composable { Text(hint) }
            } else if (label != null) {
                val hint = label
                @Composable { Text(hint) }
            } else {
                null
            },
            prefix = if (prefix != null) {
                val symbol = prefix
                @Composable { Text(symbol, color = PiggsyTheme.colors.accent) }
            } else {
                null
            },
            trailingIcon = trailing,
            isError = isError,
            supportingText = if (supportingText != null) {
                val message = supportingText
                @Composable { Text(message, color = MaterialTheme.colorScheme.error) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType ?: KeyboardType.Text,
            ),
            interactionSource = interactionSource,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PiggsyTheme.colors.accent.copy(alpha = 18f / 255f),
                unfocusedContainerColor = PiggsyTheme.colors.accent.copy(alpha = 18f / 255f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}
