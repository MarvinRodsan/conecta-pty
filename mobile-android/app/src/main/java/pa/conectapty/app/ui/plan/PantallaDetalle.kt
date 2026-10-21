package pa.conectapty.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pa.conectapty.app.datos.remoto.OpcionDto
import pa.conectapty.app.ui.tema.colorLinea

/**
 * Lista paso a paso del viaje. Va primero que el mapa a propósito (sección 9.2):
 * funciona con lector de pantalla y casi no consume datos.
 */
@Composable
fun PantallaDetalle(opcion: OpcionDto, calidad: pa.conectapty.app.datos.remoto.CalidadDto, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text("Tu viaje · ≈ ${opcion.duracionMin} min", style = MaterialTheme.typography.titleLarge)
            Text(
                "${opcion.transbordos} transbordo(s) · ${opcion.caminataM} m a pie",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        itemsIndexed(opcion.tramos) { i, t ->
            val color = if (t.modo == "CAMINAR") MaterialTheme.colorScheme.outline
            else colorLinea(t.colorLinea, MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                    PuntoLinea(color)
                    SeparadorVertical(color)
                }
                Column(Modifier.padding(start = 8.dp, bottom = 12.dp)) {
                    Text("${i + 1}. ${t.instruccion}", style = MaterialTheme.typography.bodyLarge)
                    Text("≈ ${t.duracionMin} min", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text("🏁 Llegas a tu destino", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            AvisoCalidad(calidad)
        }
    }
}
