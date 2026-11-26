package pa.conectapty.app.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pa.conectapty.app.ui.tema.Espacio

/**
 * Fila tocable de una lista (estación, lugar, favorito). Ícono a la izquierda en un fondo
 * suave, texto, y una acción opcional a la derecha (p. ej. la estrella de favorito).
 * [elegida] marca la opción que ya está puesta en el campo (fondo + ✓ + "elegida" para TalkBack).
 */
@Composable
fun FilaLista(
    titulo: String,
    icono: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    elegida: Boolean = false,
    enabled: Boolean = true,
    formaIcono: Shape = MaterialTheme.shapes.small,
    fondoIcono: Color = MaterialTheme.colorScheme.surfaceContainer,
    tinteIcono: Color = MaterialTheme.colorScheme.primary,
    iconoPersonalizado: (@Composable () -> Unit)? = null,
    final: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .background(if (elegida) cs.primaryContainer else Color.Transparent)
            .padding(end = Espacio.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .selectable(selected = elegida, enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { if (elegida) stateDescription = "elegida" }
                .padding(start = Espacio.l, end = Espacio.s, top = Espacio.s, bottom = Espacio.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).background(fondoIcono, formaIcono), contentAlignment = Alignment.Center) {
                if (iconoPersonalizado != null) iconoPersonalizado()
                else Icon(icono, contentDescription = null, tint = tinteIcono, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(start = Espacio.m)) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (elegida) cs.onPrimaryContainer else cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                detalle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (elegida) cs.onPrimaryContainer else cs.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (elegida) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = cs.primary, modifier = Modifier.padding(start = Espacio.s))
            }
        }
        final?.invoke()
    }
}
