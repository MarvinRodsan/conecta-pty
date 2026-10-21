package pa.conectapty.app.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pa.conectapty.app.datos.Estacion

@Composable
fun PantallaInicio(
    ui: UiEstado,
    favoritos: List<Estacion>,
    hayCatalogo: Boolean,
    onElegir: (Campo) -> Unit,
    onIntercambiar: () -> Unit,
    onFavorito: (Estacion) -> Unit,
    onBuscar: () -> Unit,
    onReintentarCatalogo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Conecta PTY", style = MaterialTheme.typography.headlineMedium)
        Text("Tu ciudad. Tus rutas. Tu destino.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))

        if (ui.catalogoSinConexion) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (hayCatalogo) "Sin conexión: usando estaciones guardadas." else "No se pudieron cargar las estaciones.",
                        Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    TextButton(onClick = onReintentarCatalogo) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text("Reintentar")
                    }
                }
            }
        }

        CampoEstacion("Desde", ui.origen, onClick = { onElegir(Campo.ORIGEN) })
        TextButton(onClick = onIntercambiar, enabled = ui.origen != null || ui.destino != null) {
            Text("⇅  Intercambiar")
        }
        CampoEstacion("Hasta", ui.destino, onClick = { onElegir(Campo.DESTINO) })

        if (favoritos.isNotEmpty()) {
            Text("Favoritos", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favoritos, key = { it.id }) { f ->
                    AssistChip(
                        onClick = { onFavorito(f) },
                        label = { Text(f.nombre) },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBuscar,
            enabled = ui.origen != null && ui.destino != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Buscar ruta") }
        Text(
            "Estaciones: © colaboradores de OpenStreetMap (ODbL)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CampoEstacion(etiqueta: String, estacion: Estacion?, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = "$etiqueta: ${estacion?.nombre ?: "sin elegir"}. Toca para cambiar." },
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium)
            Text(estacion?.nombre ?: "Elige una estación", style = MaterialTheme.typography.titleMedium)
        }
    }
}
