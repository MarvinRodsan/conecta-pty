package pa.conectapty.app.ui.tema

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta de marca de Conecta PTY (azul y turquesa del logo).
 * Contraste comprobado (WCAG AA, texto normal >= 4.5:1 sobre blanco y sobre [Fondo]):
 * AzulMarca 5.6, AzulProfundo 12.8, TurquesaTexto 4.6, Texto 13.4, TextoSecundario 4.8,
 * Error 4.7, Exito 4.8 (cifras sobre el fondo, el caso más exigente). [Turquesa] (2.2:1) NO se usa para texto: solo acentos y marcadores.
 */
object Paleta {
    val AzulProfundo = Color(0xFF092D5C)
    val AzulMarca = Color(0xFF1264B1)
    val Turquesa = Color(0xFF12BBD0)
    val TurquesaTexto = Color(0xFF0A7C8C)
    val Fondo = Color(0xFFF4F8FC)
    val Blanco = Color(0xFFFFFFFF)
    val Texto = Color(0xFF172B45)
    val TextoSecundario = Color(0xFF5F6F86)   // #64748B daba 4.46:1 sobre el fondo
    val Borde = Color(0xFFDCE5EF)
    val Error = Color(0xFFC93C3C)   // #D64545 daba 4.4:1; un poco más oscuro llega a AA
    val Exito = Color(0xFF137D55)   // #16865B daba 4.28:1 sobre el fondo
}

internal val EsquemaClaro = lightColorScheme(
    primary = Paleta.AzulMarca,
    onPrimary = Paleta.Blanco,
    primaryContainer = Color(0xFFDCEBFA),
    onPrimaryContainer = Paleta.AzulProfundo,
    inversePrimary = Color(0xFF8CC4FF),
    secondary = Paleta.TurquesaTexto,
    onSecondary = Paleta.Blanco,
    secondaryContainer = Color(0xFFD3F4F8),
    onSecondaryContainer = Color(0xFF053F47),
    tertiary = Paleta.AzulProfundo,
    onTertiary = Paleta.Blanco,
    tertiaryContainer = Color(0xFFFFF1D6),     // avisos de datos estimados (ámbar suave)
    onTertiaryContainer = Color(0xFF4A3300),
    background = Paleta.Fondo,
    onBackground = Paleta.Texto,
    surface = Paleta.Fondo,
    onSurface = Paleta.Texto,
    surfaceVariant = Color(0xFFE7EEF6),
    onSurfaceVariant = Paleta.TextoSecundario,
    surfaceTint = Paleta.AzulMarca,
    surfaceContainerLowest = Paleta.Blanco,    // tarjetas
    surfaceContainerLow = Paleta.Blanco,
    surfaceContainer = Color(0xFFEEF3F9),
    surfaceContainerHigh = Color(0xFFE8EEF6),
    surfaceContainerHighest = Color(0xFFE2E9F2),
    outline = Color(0xFF7A8CA1),               // bordes de controles (>= 3:1)
    outlineVariant = Paleta.Borde,             // divisores y bordes de tarjetas
    error = Paleta.Error,
    onError = Paleta.Blanco,
    errorContainer = Color(0xFFFBE4E4),
    onErrorContainer = Color(0xFF6B1414),
)

internal val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF8CC4FF),
    onPrimary = Color(0xFF00315F),
    primaryContainer = Color(0xFF0E4A86),
    onPrimaryContainer = Color(0xFFD6E8FF),
    inversePrimary = Paleta.AzulMarca,
    secondary = Color(0xFF5ED6E6),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFFB8F1F8),
    tertiary = Color(0xFFB8D3F5),
    onTertiary = Color(0xFF0B2A4F),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE6B3),
    background = Color(0xFF0B1626),
    onBackground = Color(0xFFE3EAF3),
    surface = Color(0xFF0B1626),
    onSurface = Color(0xFFE3EAF3),
    surfaceVariant = Color(0xFF1D2B3D),
    onSurfaceVariant = Color(0xFFA9B6C6),
    surfaceTint = Color(0xFF8CC4FF),
    surfaceContainerLowest = Color(0xFF122033),
    surfaceContainerLow = Color(0xFF122033),
    surfaceContainer = Color(0xFF16263A),
    surfaceContainerHigh = Color(0xFF1C2D43),
    surfaceContainerHighest = Color(0xFF22354D),
    outline = Color(0xFF7D8DA2),
    outlineVariant = Color(0xFF2A3A50),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0A0A),
    errorContainer = Color(0xFF7A1F1F),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Colores con significado propio que Material 3 no trae (éxito, encabezado de marca, mapa). */
@Immutable
data class ColoresExtra(
    val exito: Color,
    val exitoContenedor: Color,
    val onExitoContenedor: Color,
    /** Turquesa decorativo: indicadores, punto de origen. Nunca para texto. */
    val acento: Color,
    /** Banda de marca (encabezado de Inicio). */
    val encabezado: Color,
    val onEncabezado: Color,
    val onEncabezadoSecundario: Color,
    /** Tramos a pie (línea punteada + ícono: nunca solo el color). */
    val caminar: Color,
    val origen: Color,
    val destino: Color,
)

internal val ExtraClaro = ColoresExtra(
    exito = Paleta.Exito,
    exitoContenedor = Color(0xFFDDF3E9),
    onExitoContenedor = Color(0xFF0A4A31),
    acento = Paleta.Turquesa,
    encabezado = Paleta.AzulProfundo,
    onEncabezado = Paleta.Blanco,
    onEncabezadoSecundario = Color(0xFFB9D3EE),
    caminar = Color(0xFF7A8CA1),
    origen = Paleta.Turquesa,
    destino = Paleta.AzulProfundo,
)

internal val ExtraOscuro = ColoresExtra(
    exito = Color(0xFF6FD3A6),
    exitoContenedor = Color(0xFF0E3B2A),
    onExitoContenedor = Color(0xFFC4F0DB),
    acento = Paleta.Turquesa,
    encabezado = Color(0xFF0D2340),
    onEncabezado = Color(0xFFE3EAF3),
    onEncabezadoSecundario = Color(0xFF9FB7D3),
    caminar = Color(0xFF7D8DA2),
    origen = Paleta.Turquesa,
    destino = Color(0xFF8CC4FF),
)

val LocalColoresExtra = staticCompositionLocalOf { ExtraClaro }
