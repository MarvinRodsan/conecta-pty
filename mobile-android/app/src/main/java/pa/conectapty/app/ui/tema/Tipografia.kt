package pa.conectapty.app.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pa.conectapty.app.R

/**
 * Plus Jakarta Sans (OFL, assets/licencias) para títulos: geométrica, con personalidad.
 * Un solo archivo variable (176 KB) da todos los pesos. El texto corrido usa Roboto (la del
 * sistema), que se lee mejor en tamaños chicos. Todo en sp: respeta el tamaño de letra del teléfono.
 */
@OptIn(ExperimentalTextApi::class)
val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private val base = Typography()

private fun TextStyle.titulo(peso: FontWeight, tamano: Int, alto: Int) =
    copy(fontFamily = Jakarta, fontWeight = peso, fontSize = tamano.sp, lineHeight = alto.sp, letterSpacing = 0.sp)

internal val TipografiaConecta = Typography(
    // Jerarquía: headline = cifra o título de pantalla; title = encabezados y valores; body = lectura; label = controles
    displaySmall = base.displaySmall.titulo(FontWeight.Bold, 32, 40),
    headlineLarge = base.headlineLarge.titulo(FontWeight.Bold, 28, 36),
    headlineMedium = base.headlineMedium.titulo(FontWeight.Bold, 24, 32),
    headlineSmall = base.headlineSmall.titulo(FontWeight.SemiBold, 22, 28),
    titleLarge = base.titleLarge.titulo(FontWeight.SemiBold, 20, 28),
    titleMedium = base.titleMedium.titulo(FontWeight.SemiBold, 16, 24),
    titleSmall = base.titleSmall.titulo(FontWeight.SemiBold, 14, 20),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge,
    labelMedium = base.labelMedium,
    labelSmall = base.labelSmall,
)

/** Horas y minutos con cifras de igual ancho: "12:05" y "11:10" no cambian de largo. */
val TextStyle.numerico: TextStyle get() = copy(fontFeatureSettings = "tnum")
