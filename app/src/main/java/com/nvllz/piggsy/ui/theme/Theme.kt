package com.nvllz.piggsy.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.core.content.ContextCompat
import com.nvllz.piggsy.R

@Immutable
data class PiggsyColors(
    val accent: Color,
    val special: Color,
    val deposit: Color,
    val withdraw: Color,
    val dialogSurface: Color,
)

private val LocalColors = staticCompositionLocalOf<PiggsyColors> { error("PiggsyTheme missing") }
private val LocalPiggsyDark = staticCompositionLocalOf { false }

object PiggsyTheme {
    val colors: PiggsyColors @Composable get() = LocalColors.current
    val dark: Boolean @Composable get() = LocalPiggsyDark.current
}

val Chivo = FontFamily(
    Font(R.font.chivo_extralight, FontWeight.Light),
    Font(R.font.chivo_light, FontWeight.Normal),
    Font(R.font.chivo_regular, FontWeight.SemiBold),
    Font(R.font.chivo_medium, FontWeight.Bold),
    Font(R.font.chivo_extralight_italic, FontWeight.Light, FontStyle.Italic),
)

val ChivoMono = FontFamily(
    Font(R.font.chivomono_light, FontWeight.Light),
    Font(R.font.chivomono_regular, FontWeight.Normal),
)

private fun TextStyle.c() = copy(fontFamily = Chivo)

@Composable
fun PiggsyTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themedContext = remember(darkTheme, context) {
        val configuration = Configuration(context.resources.configuration)
        val mask = Configuration.UI_MODE_NIGHT_MASK.inv()
        configuration.uiMode = (configuration.uiMode and mask) or
                if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        context.createConfigurationContext(configuration)
    }

    @Composable
    fun palette(id: Int): Color = Color(ContextCompat.getColor(themedContext, id))

    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    val full = scheme.copy(
        primary = palette(R.color.md_theme_primary),
        onPrimary = palette(R.color.md_theme_onPrimary),
        primaryContainer = palette(R.color.md_theme_primaryContainer),
        onPrimaryContainer = palette(R.color.md_theme_onPrimaryContainer),
        secondary = palette(R.color.md_theme_secondary),
        onSecondary = palette(R.color.md_theme_onSecondary),
        secondaryContainer = palette(R.color.md_theme_secondaryContainer),
        onSecondaryContainer = palette(R.color.md_theme_onSecondaryContainer),
        tertiary = palette(R.color.md_theme_tertiary),
        onTertiary = palette(R.color.md_theme_onTertiary),
        tertiaryContainer = palette(R.color.md_theme_tertiaryContainer),
        onTertiaryContainer = palette(R.color.md_theme_onTertiaryContainer),
        error = palette(R.color.md_theme_error),
        onError = palette(R.color.md_theme_onError),
        errorContainer = palette(R.color.md_theme_errorContainer),
        onErrorContainer = palette(R.color.md_theme_onErrorContainer),
        background = palette(R.color.md_theme_background),
        onBackground = palette(R.color.md_theme_onBackground),
        surface = palette(R.color.md_theme_surface),
        onSurface = palette(R.color.md_theme_onSurface),
        surfaceVariant = palette(R.color.md_theme_surfaceVariant),
        onSurfaceVariant = palette(R.color.md_theme_onSurfaceVariant),
        outline = palette(R.color.md_theme_outline),
        outlineVariant = palette(R.color.md_theme_outlineVariant),
        inverseSurface = palette(R.color.md_theme_inverseSurface),
        inverseOnSurface = palette(R.color.md_theme_inverseOnSurface),
        inversePrimary = palette(R.color.md_theme_inversePrimary),
        surfaceDim = palette(R.color.md_theme_surfaceDim),
        surfaceBright = palette(R.color.md_theme_surfaceBright),
        surfaceContainerLowest = palette(R.color.md_theme_surfaceContainerLowest),
        surfaceContainerLow = palette(R.color.md_theme_surfaceContainerLow),
        surfaceContainer = palette(R.color.md_theme_surfaceContainer),
        surfaceContainerHigh = palette(R.color.md_theme_surfaceContainerHigh),
        surfaceContainerHighest = palette(R.color.md_theme_surfaceContainerHighest),
    )
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val effective = if (useDynamic) {
        if (darkTheme) dynamicDarkColorScheme(themedContext) else dynamicLightColorScheme(themedContext)
    } else {
        full
    }
    val colors = PiggsyColors(

        accent = if (useDynamic) effective.primary else palette(R.color.colorAccent),
        special = palette(R.color.colorSpecial),
        deposit = palette(R.color.colorDeposit),
        withdraw = palette(R.color.colorWithdraw),
        dialogSurface = if (useDynamic) effective.surfaceContainer else palette(R.color.colorDialogSurface),
    )
    val t = Typography()
    val typography = t.copy(
        displayLarge = t.displayLarge.c(), displayMedium = t.displayMedium.c(), displaySmall = t.displaySmall.c(),
        headlineLarge = t.headlineLarge.c(), headlineMedium = t.headlineMedium.c(), headlineSmall = t.headlineSmall.c(),
        titleLarge = t.titleLarge.c(), titleMedium = t.titleMedium.c(), titleSmall = t.titleSmall.c(),
        bodyLarge = t.bodyLarge.c(), bodyMedium = t.bodyMedium.c(), bodySmall = t.bodySmall.c(),
        labelLarge = t.labelLarge.c(), labelMedium = t.labelMedium.c(), labelSmall = t.labelSmall.c(),
    )
    CompositionLocalProvider(LocalColors provides colors, LocalPiggsyDark provides darkTheme) {
        MaterialTheme(colorScheme = effective, typography = typography) {
            Surface(color = effective.background, contentColor = effective.onSurface, content = content)
        }
    }
}
