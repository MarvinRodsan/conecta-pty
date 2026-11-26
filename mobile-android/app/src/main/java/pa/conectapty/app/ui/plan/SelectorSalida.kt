package pa.conectapty.app.ui.plan

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** "Ahora", "En 30 min" o "Elegir hora" (fecha y luego hora). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorSalida(salida: Salida, onCambiar: (Salida) -> Unit, modifier: Modifier = Modifier) {
    var pidiendoFecha by rememberSaveable { mutableStateOf(false) }
    // Fecha elegida en el primer diálogo, a la espera de la hora (epoch day: sobrevive a rotar la pantalla)
    var fechaElegida by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(modifier) {
        Text(
            "¿Cuándo sales?",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpcionSalida("Ahora", salida == Salida.Ahora) { onCambiar(Salida.Ahora) }
            OpcionSalida("En 30 min", salida == Salida.En30Min) { onCambiar(Salida.En30Min) }
            val fija = salida as? Salida.A
            OpcionSalida(
                texto = fija?.let { describirMomento(it.cuando, ahoraEnPanama().toLocalDate()) } ?: "Elegir hora",
                elegida = fija != null,
                icono = true,
            ) { pidiendoFecha = true }
        }
    }

    if (pidiendoFecha) {
        DialogoFecha(
            inicial = (salida as? Salida.A)?.cuando?.toLocalDate(),
            onElegir = { fechaElegida = it.toEpochDay(); pidiendoFecha = false },
            onCancelar = { pidiendoFecha = false },
        )
    }
    fechaElegida?.let { dia ->
        DialogoHora(
            inicial = (salida as? Salida.A)?.cuando?.toLocalTime() ?: ahoraEnPanama().toLocalTime(),
            onElegir = { hora ->
                onCambiar(Salida.A(LocalDate.ofEpochDay(dia).atTime(hora)))
                fechaElegida = null
            },
            onCancelar = { fechaElegida = null },
        )
    }
}

@Composable
private fun OpcionSalida(texto: String, elegida: Boolean, icono: Boolean = false, onClick: () -> Unit) {
    FilterChip(
        selected = elegida,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = when {
            elegida -> ({ Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) })
            icono -> ({ Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) })
            else -> null
        },
        shape = MaterialTheme.shapes.small,
    )
}

/** Solo hoy y los próximos 60 días: el mismo rango que acepta el backend. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoFecha(inicial: LocalDate?, onElegir: (LocalDate) -> Unit, onCancelar: () -> Unit) {
    val hoy = remember { ahoraEnPanama().toLocalDate() }
    val estado = rememberDatePickerState(
        // El DatePicker trabaja en milisegundos UTC a medianoche
        initialSelectedDateMillis = (inicial ?: hoy).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val dia = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !dia.isBefore(hoy) && !dia.isAfter(hoy.plusDays(60))
            }

            override fun isSelectableYear(year: Int) = year in hoy.year..hoy.plusDays(60).year
        },
    )
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(
                onClick = {
                    estado.selectedDateMillis?.let {
                        onElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = estado.selectedDateMillis != null,
            ) { Text("Siguiente") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    ) { DatePicker(state = estado) }
}

/** TimeInput en vez del reloj: se puede escribir la hora y funciona mejor con TalkBack. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoHora(inicial: LocalTime, onElegir: (LocalTime) -> Unit, onCancelar: () -> Unit) {
    val formato24 = DateFormat.is24HourFormat(LocalContext.current)
    val estado = rememberTimePickerState(initialHour = inicial.hour, initialMinute = inicial.minute, is24Hour = formato24)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Hora de salida") },
        text = { TimeInput(state = estado) },
        confirmButton = { TextButton(onClick = { onElegir(LocalTime.of(estado.hour, estado.minute)) }) { Text("Listo") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}
