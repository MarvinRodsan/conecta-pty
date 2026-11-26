package pa.conectapty.app.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Tema de Conecta PTY: colores, tipografía y formas de la marca (ver Colores, Tipografia, Medidas). */
@Composable
fun ConectaTema(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColoresExtra provides if (oscuro) ExtraOscuro else ExtraClaro) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = TipografiaConecta,
            shapes = FormasConecta,
            content = contenido,
        )
    }
}

/** Acceso a los colores propios: `ConectaTema.extra.exito`. */
object ConectaTema {
    val extra: ColoresExtra
        @Composable @ReadOnlyComposable get() = LocalColoresExtra.current
}

/** Color oficial de la línea si viene del GTFS; si no, el primario. */
fun colorLinea(hex: String?, defecto: Color): Color =
    hex?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: defecto
