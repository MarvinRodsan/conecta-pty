package pa.conectapty.app.ui.plan

import pa.conectapty.app.ui.componentes.BarraSuperior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

const val MENSAJE_MIN = 5
const val MENSAJE_MAX = 1000

/** "Reportar un problema": canal de comentarios del piloto. Anónimo, sin ubicación. */
@Composable
fun PantallaReporte(
    envio: EstadoEnvio,
    hayRuta: Boolean,
    onEnviar: (TipoReporte, String, Boolean) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tipo by rememberSaveable { mutableStateOf(if (hayRuta) TipoReporte.RUTA else TipoReporte.ESTACION) }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var incluirRuta by rememberSaveable { mutableStateOf(true) }

    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BarraSuperior("Reportar un problema", onVolver)

        if (envio is EstadoEnvio.Enviado) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Text(envio.mensaje, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Button(onClick = onVolver, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Volver") }
            return@Column
        }

        Text("¿Qué pasó?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Column(Modifier.selectableGroup()) {
            TipoReporte.entries.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = tipo == t, onClick = { tipo = t }, role = Role.RadioButton),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = tipo == t, onClick = null)
                    Text(t.etiqueta, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        OutlinedTextField(
            value = mensaje,
            onValueChange = { if (it.length <= MENSAJE_MAX) mensaje = it },
            label = { Text("Cuéntanos qué viste") },
            placeholder = { Text("Ej.: la entrada de la estación está en la otra calle") },
            supportingText = { Text("${mensaje.length} / $MENSAJE_MAX") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )

        if (hayRuta) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = incluirRuta, onValueChange = { incluirRuta = it }, role = Role.Checkbox),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = incluirRuta, onCheckedChange = null)
                Text("Incluir la ruta que estaba viendo (nombres de origen y destino)", Modifier.padding(start = 8.dp))
            }
        }

        Text(
            "No pedimos tu nombre ni enviamos tu ubicación. Solo llega lo que escribes aquí.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (envio is EstadoEnvio.Error) {
            Text(
                envio.mensaje,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }

        val enviando = envio == EstadoEnvio.Enviando
        Button(
            onClick = { onEnviar(tipo, mensaje, hayRuta && incluirRuta) },
            enabled = !enviando && mensaje.trim().length >= MENSAJE_MIN,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            if (enviando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Enviar reporte")
        }
    }
}
