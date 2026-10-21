package pa.conectapty.app.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFF0B5C7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6E7F3),
    onPrimaryContainer = Color(0xFF002B3A),
    secondary = Color(0xFF2E7D5B),
    tertiaryContainer = Color(0xFFFFE6B3),   // avisos de datos estimados
    onTertiaryContainer = Color(0xFF3D2B00),
    background = Color(0xFFF7F9FB),
    surface = Color(0xFFF7F9FB),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF7FD3E8),
    onPrimary = Color(0xFF003545),
    primaryContainer = Color(0xFF004C63),
    onPrimaryContainer = Color(0xFFC6E7F3),
    secondary = Color(0xFF8ED6B0),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE6B3),
)

@Composable
fun ConectaTema(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = contenido)
}

/** Color oficial de la línea si viene del GTFS; si no, el primario. */
fun colorLinea(hex: String?, defecto: Color): Color =
    hex?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: defecto
