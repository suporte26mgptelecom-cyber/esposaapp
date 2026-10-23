package br.com.wellness.ui.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import br.com.wellness.R

/** Fixed palette: no system light/dark selection or dynamic wallpaper colors. */
@Composable
fun WellnessTheme(content: @Composable () -> Unit) {
    val background = colorResource(R.color.wellness_background)
    val surface = colorResource(R.color.wellness_surface)
    val input = colorResource(R.color.wellness_input)
    val text = colorResource(R.color.wellness_text)
    val secondaryText = colorResource(R.color.wellness_text_secondary)
    val selected = colorResource(R.color.wellness_selected)
    val border = colorResource(R.color.wellness_border)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = selected, onPrimary = text,
            primaryContainer = surface, onPrimaryContainer = text,
            inversePrimary = selected,
            secondary = selected, onSecondary = text,
            secondaryContainer = surface, onSecondaryContainer = text,
            tertiary = selected, onTertiary = text,
            tertiaryContainer = surface, onTertiaryContainer = text,
            background = background, onBackground = text,
            surface = surface, onSurface = text,
            surfaceVariant = input, onSurfaceVariant = secondaryText,
            // Matching the surface also keeps elevated dialogs/cards exactly #151515.
            surfaceTint = surface,
            inverseSurface = input, inverseOnSurface = text,
            error = selected, onError = text,
            errorContainer = surface, onErrorContainer = text,
            outline = border, outlineVariant = border, scrim = background,
            surfaceBright = surface, surfaceDim = surface,
            surfaceContainer = surface, surfaceContainerHigh = surface,
            surfaceContainerHighest = surface, surfaceContainerLow = surface,
            surfaceContainerLowest = surface
        ),
        content = content
    )
}

@Composable
fun wellnessButtonColors() = ButtonDefaults.buttonColors(
    containerColor = colorResource(R.color.wellness_button),
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun wellnessTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun wellnessOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = MaterialTheme.colorScheme.primary,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun wellnessTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
)
