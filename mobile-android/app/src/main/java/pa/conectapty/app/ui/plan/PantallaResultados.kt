package pa.conectapty.app.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pa.conectapty.app.datos.remoto.CalidadDto
import pa.conectapty.app.datos.remoto.OpcionDto
import pa.conectapty.app.datos.remoto.TramoDto
import pa.conectapty.app.ui.tema.colorLinea

@Composable
fun PantallaResultados(
    ui: UiEstado,
    onVerDetalle: (Int) -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${ui.origen?.nombre ?: "?"} → ${ui.destino?.nombre ?: "?"}", style = MaterialTheme.typography.titleLarge)
        when (val p = ui.plan) {
            EstadoPlan.Vacio -> Unit
            EstadoPlan.Cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is EstadoPlan.Error -> {
                Text(p.mensaje, color = MaterialTheme.colorScheme.error)
                Button(onClick = onReintentar) { Text("Reintentar") }
            }
            is EstadoPlan.Listo -> {
                AvisoCalidad(p.plan.calidad)
                if (p.plan.opciones.isEmpty()) {
                    Text("No encontramos una ruta en Metro entre estas estaciones con los datos actuales.")
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(p.plan.opciones) { i, o -> TarjetaOpcion(o, onClick = { onVerDetalle(i) }) }
                }
            }
        }
    }
}

/** Distingue visualmente lo confirmado de lo estimado (sección 9.2). */
@Composable
fun AvisoCalidad(c: CalidadDto) {
    val estimado = c.tiempos != "verificado" || c.frecuencias != "verificado"
    Card(colors = CardDefaults.cardColors(
        containerColor = if (estimado) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
    )) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (estimado) Icons.Filled.Warning else Icons.Filled.Info, contentDescription = null)
            Column {
                Text(c.aviso, style = MaterialTheme.typography.bodyMedium)
                if (c.tarifas == "desconocido") {
                    Text("Costo del pasaje: no confirmado.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TarjetaOpcion(o: OpcionDto, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("≈ ${o.duracionMin} min", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Text(
                    when (o.transbordos) { 0 -> "Directo"; 1 -> "1 transbordo"; else -> "${o.transbordos} transbordos" },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            ResumenLineas(o.tramos)
            Text("Caminas ${o.caminataM} m en total", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ResumenLineas(tramos: List<TramoDto>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        tramos.forEachIndexed { i, t ->
            if (i > 0) Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (t.modo == "CAMINAR") {
                Text("🚶 ${t.duracionMin}′")
            } else {
                InsigniaLinea(t.linea ?: "?", colorLinea(t.colorLinea, MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
fun InsigniaLinea(linea: String, color: Color) {
    Box(
        Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) { Text("L$linea", color = Color.White, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun PuntoLinea(color: Color) {
    Box(Modifier.size(12.dp).background(color, CircleShape))
}

@Composable
fun SeparadorVertical(color: Color) {
    Box(Modifier.width(4.dp).height(28.dp).background(color))
}
