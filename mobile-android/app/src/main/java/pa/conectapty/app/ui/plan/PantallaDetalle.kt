package pa.conectapty.app.ui.plan

import pa.conectapty.app.ui.componentes.BarraSuperior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pa.conectapty.app.datos.remoto.CalidadDto
import pa.conectapty.app.datos.remoto.OpcionDto
import pa.conectapty.app.ui.tema.colorLinea

/**
 * Lista paso a paso del viaje, con el mapa como complemento arriba. La lista es la fuente
 * principal a propósito (sección 9.2): funciona con lector de pantalla y casi no consume datos.
 * El mapa se puede ocultar y solo descarga teselas cuando está visible.
 */
@Composable
fun PantallaDetalle(
    opcion: OpcionDto,
    calidad: CalidadDto,
    onVolver: () -> Unit,
    onReportar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorDefecto = MaterialTheme.colorScheme.primary
    val colorCaminar = MaterialTheme.colorScheme.outline
    val trazos = remember(opcion) { trazosDe(opcion.tramos, colorDefecto, colorCaminar) }
    var verMapa by rememberSaveable { mutableStateOf(true) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        BarraSuperior("Tu viaje · ≈ ${opcion.duracionMin} min", onVolver, Modifier.padding(top = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${opcion.transbordos} transbordo(s) · ${opcion.caminataM} m a pie",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (trazos.isNotEmpty()) {
                TextButton(onClick = { verMapa = !verMapa }) { Text(if (verMapa) "Ocultar mapa" else "Ver mapa") }
            }
        }
        if (verMapa && trazos.isNotEmpty()) {
            MapaRuta(
                trazos,
                Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
            )
        }

        LazyColumn(
            Modifier.weight(1f).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(opcion.tramos) { i, t ->
                val color = if (t.modo == "CAMINAR") MaterialTheme.colorScheme.outline
                else colorLinea(t.colorLinea, MaterialTheme.colorScheme.primary)
                val tiempos = listOfNotNull(t.sale?.let { "≈ $it" }, "${t.duracionMin} min").joinToString(" · ")
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "Paso ${i + 1}: ${t.instruccion}. ${t.sale?.let { "Hacia las $it, " } ?: ""}${t.duracionMin} minutos."
                    },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                        PuntoLinea(color)
                        SeparadorVertical(color)
                    }
                    Column(Modifier.padding(start = 8.dp, bottom = 12.dp)) {
                        Text("${i + 1}. ${t.instruccion}", style = MaterialTheme.typography.bodyLarge)
                        Text(tiempos, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Text(
                    "🏁 Llegas a tu destino",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp).semantics {
                        contentDescription = "Llegas a tu destino"
                        heading()
                    },
                )
                AvisoCalidad(calidad)
                OutlinedButton(onClick = onReportar, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text("¿Algo no cuadra? Reportar un problema con esta ruta")
                }
            }
        }
    }
}
