package pa.conectapty.app.ui.plan

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.Punto
import pa.conectapty.app.datos.TipoPunto
import pa.conectapty.app.datos.filtrarEstaciones
import pa.conectapty.app.ui.componentes.Aviso
import pa.conectapty.app.ui.componentes.BarraSuperior
import pa.conectapty.app.ui.componentes.EstadoVista
import pa.conectapty.app.ui.componentes.FilaLista
import pa.conectapty.app.ui.componentes.IconosConecta
import pa.conectapty.app.ui.componentes.TipoAviso
import pa.conectapty.app.ui.tema.Espacio

/** Mínimo de letras para buscar direcciones en el servidor (lo mismo que espera el ViewModel). */
private const val MIN_LUGARES = 3

/**
 * Elegir origen o destino: mi ubicación, una estación (catálogo local, sin red)
 * o una dirección/lugar (búsqueda en el servidor a partir de 3 letras).
 *
 * Orden pensado para decidir rápido (Ley de Hick): primero lo más probable (mi ubicación y
 * favoritos), luego todas las estaciones, y al escribir, direcciones y lugares.
 *
 * @param seleccionado lo que ya está puesto en este campo: se marca como "elegida"
 */
@Composable
fun SelectorEstacion(
    titulo: String,
    estaciones: List<Estacion>,
    favoritos: List<Estacion>,
    lugares: EstadoLugares,
    buscandoUbicacion: Boolean,
    onConsulta: (String) -> Unit,
    onElegir: (Punto) -> Unit,
    onUsarUbicacion: () -> Unit,
    onAlternarFavorito: (Estacion) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    seleccionado: Punto? = null,
    esOrigen: Boolean = true,
) {
    var consulta by rememberSaveable { mutableStateOf("") }
    // Solo se vuelve a filtrar cuando cambia el texto o el catálogo, no en cada recomposición
    val visibles = remember(consulta, estaciones) { filtrarEstaciones(estaciones, consulta) }
    val idsFavoritos = remember(favoritos) { favoritos.mapTo(HashSet()) { it.id } }
    val teclado = LocalSoftwareKeyboardController.current
    val buscando = consulta.isNotBlank()
    val cambiarConsulta = { texto: String -> consulta = texto; onConsulta(texto) }

    val fila: @Composable (Estacion) -> Unit = { e ->
        FilaEstacion(
            estacion = e,
            esFavorita = e.id in idsFavoritos,
            elegida = seleccionado?.estacionId == e.id,
            onElegir = { onElegir(e.comoPunto()) },
            onAlternarFavorito = { onAlternarFavorito(e) },
        )
    }

    // Surface: fondo y color de texto propios (se lee bien en claro y en oscuro)
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column {
        BarraSuperior(titulo, onVolver, subtitulo = if (esOrigen) "Origen del viaje" else "Destino del viaje")
        OutlinedTextField(
            value = consulta,
            onValueChange = cambiarConsulta,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (buscando) {
                { IconButton(onClick = { cambiarConsulta("") }) { Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda") } }
            } else null,
            placeholder = { Text("Estación, dirección o lugar") },
            supportingText = { Text("Ej.: Vía Argentina, UTP, Albrook Mall") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { teclado?.hide() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.l),
        )

        // imePadding: con el teclado abierto, el final de la lista sigue alcanzable
        LazyColumn(Modifier.weight(1f).imePadding()) {
            item(key = "ubicacion") {
                FilaLista(
                    titulo = if (buscandoUbicacion) "Buscando tu ubicación…" else "Usar mi ubicación",
                    detalle = "Solo se usa en tu teléfono para sugerir la estación más cercana",
                    icono = IconosConecta.MiUbicacion,
                    onClick = onUsarUbicacion,
                    enabled = !buscandoUbicacion,
                    elegida = seleccionado?.tipo == TipoPunto.UBICACION,
                    formaIcono = CircleShape,
                    fondoIcono = MaterialTheme.colorScheme.primaryContainer,
                    iconoPersonalizado = if (buscandoUbicacion) {
                        { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                    } else null,
                    modifier = Modifier.padding(top = Espacio.s),
                )
            }

            if (!buscando && favoritos.isNotEmpty()) {
                seccion("titulo-favoritos", "Tus favoritos")
                items(favoritos, key = { "fav-${it.id}" }) { fila(it) }
            }

            seccion("titulo-estaciones", if (buscando) "Estaciones del Metro (${visibles.size})" else "Estaciones del Metro")
            when {
                estaciones.isEmpty() -> item(key = "sin-catalogo") {
                    EstadoVista(
                        icono = IconosConecta.SinConexion,
                        titulo = "Todavía no hay estaciones",
                        mensaje = "Se descargan la primera vez que la app tiene internet. Mientras tanto, puedes usar tu ubicación o buscar una dirección.",
                    )
                }
                visibles.isEmpty() -> item(key = "sin-coincidencias") {
                    Mensaje(
                        "Ninguna estación coincide con \"${consulta.trim()}\"." +
                            if (consulta.trim().length >= MIN_LUGARES) " Mira las direcciones y lugares abajo." else " Escribe al menos $MIN_LUGARES letras para buscar direcciones.",
                    )
                }
            }
            items(visibles, key = { it.id }) { fila(it) }

            lugares(lugares, seleccionado, onElegir)
            item(key = "fin") { Spacer(Modifier.height(Espacio.xl)) }
        }
    } }
}

private fun LazyListScope.lugares(lugares: EstadoLugares, seleccionado: Punto?, onElegir: (Punto) -> Unit) {
    when (lugares) {
        EstadoLugares.Inactivo -> Unit
        EstadoLugares.Cargando -> {
            seccion("titulo-lugares", "Direcciones y lugares")
            item(key = "lugares-cargando") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Espacio.l, vertical = Espacio.m)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Espacio.m),
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Buscando lugares…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        is EstadoLugares.Error -> {
            seccion("titulo-lugares", "Direcciones y lugares")
            item(key = "lugares-error") {
                Aviso(lugares.mensaje, TipoAviso.ERROR, Modifier.padding(horizontal = Espacio.l))
            }
        }
        is EstadoLugares.Listo -> {
            seccion("titulo-lugares", "Direcciones y lugares")
            if (lugares.lugares.isEmpty()) {
                item(key = "lugares-vacio") { Mensaje("No encontramos direcciones ni lugares con ese nombre en Panamá.") }
            }
            items(lugares.lugares, key = { "lugar-${it.lat},${it.lon},${it.nombre}" }) { l ->
                FilaLista(
                    titulo = l.nombre,
                    detalle = l.detalle,
                    icono = Icons.Filled.Place,
                    onClick = { onElegir(l) },
                    elegida = seleccionado != null && seleccionado.tipo == TipoPunto.LUGAR &&
                        seleccionado.nombre == l.nombre && seleccionado.lat == l.lat && seleccionado.lon == l.lon,
                    formaIcono = CircleShape,
                    tinteIcono = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (lugares.lugares.isNotEmpty()) {
                item(key = "lugares-fuente") {
                    Text(
                        "Lugares: © colaboradores de OpenStreetMap (búsqueda con Photon)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Espacio.l, vertical = Espacio.m),
                    )
                }
            }
        }
    }
}

/**
 * Estación del Metro con su estrella. La línea (L1/L2) no se muestra: el catálogo todavía no
 * la trae (requiere cambiar backend y BD, fuera de este rediseño).
 */
@Composable
private fun FilaEstacion(
    estacion: Estacion,
    esFavorita: Boolean,
    elegida: Boolean,
    onElegir: () -> Unit,
    onAlternarFavorito: () -> Unit,
) {
    FilaLista(
        titulo = estacion.nombre,
        icono = IconosConecta.Metro,
        onClick = onElegir,
        elegida = elegida,
        final = {
            IconButton(onClick = onAlternarFavorito) {
                // Cambio suave de estrella vacía a llena: confirma la acción sin distraer
                Crossfade(esFavorita, label = "estrella") { fav ->
                    Icon(
                        if (fav) Icons.Filled.Star else IconosConecta.EstrellaVacia,
                        contentDescription = if (fav) "Quitar ${estacion.nombre} de favoritos" else "Guardar ${estacion.nombre} en favoritos",
                        tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

private fun LazyListScope.seccion(clave: String, texto: String) = item(key = clave) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = Espacio.l, end = Espacio.l, top = Espacio.l, bottom = Espacio.xs)
            .semantics { heading() },
    )
}

@Composable
private fun Mensaje(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = Espacio.l, vertical = Espacio.m)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
