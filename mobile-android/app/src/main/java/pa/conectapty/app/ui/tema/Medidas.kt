package pa.conectapty.app.ui.tema

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Escala de espaciado (múltiplos de 4 dp). No usar otros valores sin una razón. */
object Espacio {
    /** Separación mínima (ícono-texto dentro de un control). */
    val xs = 4.dp
    /** Elementos relacionados. */
    val s = 8.dp
    /** Intermedia (entre filas de un bloque). */
    val m = 12.dp
    /** Relleno estándar de pantalla y tarjeta. */
    val l = 16.dp
    /** Entre secciones. */
    val xl = 24.dp
    /** Bloques importantes. */
    val xxl = 32.dp

    /** Objetivo táctil mínimo (Material / Ley de Fitts). */
    val tactil = 48.dp
    /** Alto del botón principal. */
    val botonPrincipal = 56.dp
}

/**
 * Radios: 8 insignias y chips, 12 campos y botones, 16 tarjetas, 28 diálogos y hojas.
 * Sombras casi nulas: las tarjetas se separan con borde, no con elevación.
 */
internal val FormasConecta = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object Elevacion {
    val plana = 0.dp
    /** Solo para lo que flota sobre el contenido (botón fijo abajo, tarjeta sobre el encabezado). */
    val flotante = 2.dp
}
