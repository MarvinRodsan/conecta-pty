package pa.conectapty.app.ui.componentes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pa.conectapty.app.ui.tema.ConectaTema
import pa.conectapty.app.ui.tema.Espacio

/**
 * Estado de pantalla completa (sin resultados, sin conexión, error, lista vacía):
 * qué pasó, qué puede hacer la persona, y el botón para hacerlo.
 */
@Composable
fun EstadoVista(
    icono: ImageVector,
    titulo: String,
    mensaje: String,
    modifier: Modifier = Modifier,
    accion: String? = null,
    onAccion: () -> Unit = {},
    esError: Boolean = false,
) {
    val fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val tinte = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier.fillMaxWidth().padding(horizontal = Espacio.xl, vertical = Espacio.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Box(Modifier.size(72.dp).background(fondo, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(32.dp))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Espacio.xs),
            modifier = Modifier.semantics(mergeDescendants = true) {
                liveRegion = if (esError) LiveRegionMode.Assertive else LiveRegionMode.Polite
            },
        ) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            Text(mensaje, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (accion != null) {
            BotonSecundario(accion, onAccion, Modifier.padding(top = Espacio.s))
        }
    }
}

enum class TipoAviso { INFO, ADVERTENCIA, ERROR, EXITO }

/**
 * Aviso en línea. Cada tipo tiene su propio ícono además del color (no se comunica solo con
 * color). Opcionalmente, una acción ("Reintentar").
 */
@Composable
fun Aviso(
    texto: String,
    tipo: TipoAviso,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val extra = ConectaTema.extra
    val (fondo, frente, icono, nombre) = when (tipo) {
        TipoAviso.INFO -> Cuarteto(cs.primaryContainer, cs.onPrimaryContainer, Icons.Filled.Info, "Información")
        TipoAviso.ADVERTENCIA -> Cuarteto(cs.tertiaryContainer, cs.onTertiaryContainer, Icons.Filled.Warning, "Atención")
        TipoAviso.ERROR -> Cuarteto(cs.errorContainer, cs.onErrorContainer, IconosConecta.ErrorCirculo, "Error")
        TipoAviso.EXITO -> Cuarteto(extra.exitoContenedor, extra.onExitoContenedor, Icons.Filled.CheckCircle, "Listo")
    }
    Surface(color = fondo, contentColor = frente, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = Espacio.m, end = Espacio.xs, top = Espacio.m, bottom = Espacio.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            Icon(icono, contentDescription = nombre, modifier = Modifier.size(20.dp))
            Column(
                Modifier.weight(1f).padding(end = Espacio.s).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(texto, style = MaterialTheme.typography.bodyMedium)
                detalle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            if (accion != null) {
                TextButton(onClick = onAccion) { Text(accion, color = frente) }
            }
        }
    }
}

private data class Cuarteto(val fondo: Color, val frente: Color, val icono: ImageVector, val nombre: String)

/**
 * Bloque gris que late mientras carga (skeleton). Da idea de la forma del resultado y se
 * siente más rápido que un círculo girando. Una sola animación barata (alfa).
 */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, lineas: Int = 3, descripcion: String = "Cargando") {
    val t = rememberInfiniteTransition(label = "esqueleto")
    val alfa by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alfa")
    val color = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier.alpha(alfa).clearAndSetSemantics { contentDescription = descripcion },
        verticalArrangement = Arrangement.spacedBy(Espacio.s),
    ) {
        Box(Modifier.fillMaxWidth(0.35f).height(24.dp).background(color, MaterialTheme.shapes.small))
        repeat(lineas - 1) { i ->
            Box(Modifier.fillMaxWidth(if (i % 2 == 0) 0.8f else 0.55f).height(14.dp).background(color, MaterialTheme.shapes.small))
        }
    }
}
