package pa.conectapty.app.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pa.conectapty.app.ui.tema.Espacio

/**
 * Título de pantalla con botón "Volver" visible: el gesto de atrás no es obvio para
 * todo el mundo, y TalkBack anuncia el título como encabezado.
 */
@Composable
fun BarraSuperior(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).padding(end = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onVolver, modifier = Modifier.padding(start = Espacio.xs)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Column(Modifier.weight(1f).padding(start = Espacio.xs).semantics(mergeDescendants = true) { heading() }) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            subtitulo?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        acciones()
    }
}

/** Acción principal de la pantalla: una sola por pantalla, ancho completo, 56 dp de alto. */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
    icono: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !cargando,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth().heightIn(min = Espacio.botonPrincipal),
    ) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            Text(texto, Modifier.padding(start = Espacio.m), style = MaterialTheme.typography.titleMedium)
        } else {
            icono?.let { Icon(it, contentDescription = null, Modifier.padding(end = Espacio.s)) }
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Acción secundaria: borde, sin relleno. */
@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icono: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.heightIn(min = Espacio.tactil),
    ) {
        icono?.let { Icon(it, contentDescription = null, Modifier.padding(end = Espacio.s).size(18.dp)) }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Tarjeta de la app: fondo blanco, borde fino, sin sombra. Si [onClick] no es null, toda
 * la tarjeta es un botón (con ripple).
 */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    resaltada: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val borde = BorderStroke(
        if (resaltada) 2.dp else 1.dp,
        if (resaltada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    )
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val interior: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.s), content = contenido)
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colores, border = borde, content = interior)
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = colores, border = borde, content = interior)
    }
}

/** Encabezado de sección dentro de una pantalla ("Tus favoritos", "Estaciones del Metro"). */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier, accion: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = Espacio.tactil), verticalAlignment = Alignment.CenterVertically) {
        Text(
            texto,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        accion?.invoke()
    }
}
