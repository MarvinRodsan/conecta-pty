package pa.conectapty.app.ui.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pa.conectapty.app.datos.Punto
import pa.conectapty.app.datos.remoto.CalidadDto
import pa.conectapty.app.datos.remoto.OpcionDto
import pa.conectapty.app.datos.remoto.PlanDto
import pa.conectapty.app.datos.remoto.TramoDto
import pa.conectapty.app.ui.componentes.Aviso
import pa.conectapty.app.ui.componentes.BarraSuperior
import pa.conectapty.app.ui.componentes.Esqueleto
import pa.conectapty.app.ui.componentes.EstadoVista
import pa.conectapty.app.ui.componentes.IconosConecta
import pa.conectapty.app.ui.componentes.Tarjeta
import pa.conectapty.app.ui.componentes.TipoAviso
import pa.conectapty.app.ui.componentes.TituloSeccion
import pa.conectapty.app.ui.tema.ConectaTema
import pa.conectapty.app.ui.tema.Espacio
import pa.conectapty.app.ui.tema.colorLinea
import pa.conectapty.app.ui.tema.numerico

/**
 * Opciones de viaje. Arriba, fijo, el resumen de la búsqueda (de dónde, a dónde, cuándo) con
 * "Cambiar"; debajo, el estado: esqueleto mientras carga, error con su acción, o las tarjetas.
 * Cada tarjeta responde primero "¿cuánto tardo y a qué hora llego?" y después "¿cómo?".
 */
@Composable
fun PantallaResultados(
    ui: UiEstado,
    onVerDetalle: (Int) -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
    onReportar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            BarraSuperior(
                titulo = "Tus rutas",
                onVolver = onVolver,
                acciones = {
                    // Útil con "Salir ahora": los horarios cambian si la persona espera un rato
                    if (ui.plan is EstadoPlan.Listo) {
                        IconButton(onClick = onReintentar) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar rutas") }
                    }
                },
            )
            ResumenBusqueda(ui.origen, ui.destino, ui.salida, onCambiar = onVolver)
            AnimatedContent(
                targetState = ui.plan,
                contentKey = { it::class }, // anima solo al cambiar de estado, no al refrescar datos
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "resultados",
                modifier = Modifier.weight(1f),
            ) { plan ->
                when (plan) {
                    EstadoPlan.Vacio -> Unit
                    EstadoPlan.Cargando -> Cargando()
                    is EstadoPlan.Error -> ErrorPlan(plan, onReintentar, onCambiar = onVolver)
                    is EstadoPlan.Listo -> ListaOpciones(plan.plan, onVerDetalle, onCambiar = onVolver, onReportar)
                }
            }
        }
    }
}

/** "Albrook → UTP · Salir ahora" en una tarjeta compacta: confirma qué se buscó sin volver atrás. */
@Composable
private fun ResumenBusqueda(origen: Punto?, destino: Punto?, salida: Salida, onCambiar: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val extra = ConectaTema.extra
    val cuando = salida.describir(ahoraEnPanama())
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cs.surfaceContainerLowest,
        border = BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Espacio.l),
    ) {
        Row(
            Modifier.padding(start = Espacio.l, top = Espacio.m, bottom = Espacio.m, end = Espacio.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription = "Desde ${origen?.nombre ?: "sin elegir"} hasta ${destino?.nombre ?: "sin elegir"}. $cuando."
                },
                verticalArrangement = Arrangement.spacedBy(Espacio.xs),
            ) {
                LineaResumen(origen?.nombre ?: "—") {
                    Icon(IconosConecta.Origen, contentDescription = null, tint = extra.origen, modifier = Modifier.size(12.dp))
                }
                LineaResumen(destino?.nombre ?: "—") {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = extra.destino, modifier = Modifier.size(16.dp))
                }
                LineaResumen(cuando, secundaria = true) {
                    Icon(IconosConecta.Reloj, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
            }
            TextButton(onClick = onCambiar) { Text("Cambiar") }
        }
    }
}

@Composable
private fun LineaResumen(texto: String, secundaria: Boolean = false, icono: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) { icono() }
        Text(
            texto,
            style = if (secundaria) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
            color = if (secundaria) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = Espacio.s),
        )
    }
}

/** Esqueletos con la forma de las tarjetas: se siente más rápido que un círculo girando. */
@Composable
private fun Cargando() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Espacio.l),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        Text(
            "Buscando las mejores rutas…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        repeat(3) {
            Tarjeta { Esqueleto(Modifier.fillMaxWidth(), lineas = 3, descripcion = "Buscando rutas") }
        }
    }
}

@Composable
private fun ErrorPlan(error: EstadoPlan.Error, onReintentar: () -> Unit, onCambiar: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        when (error.tipo) {
            TipoError.MISMO_LUGAR -> EstadoVista(
                icono = Icons.Filled.LocationOn,
                titulo = "Elige otro destino",
                mensaje = error.mensaje,
                accion = "Cambiar origen o destino",
                onAccion = onCambiar,
            )
            TipoError.SIN_CONEXION -> EstadoVista(
                icono = IconosConecta.SinConexion,
                titulo = "Sin conexión",
                mensaje = error.mensaje,
                accion = "Reintentar",
                onAccion = onReintentar,
                esError = true,
            )
            TipoError.SERVIDOR -> EstadoVista(
                icono = IconosConecta.ErrorCirculo,
                titulo = "El servicio no responde",
                mensaje = error.mensaje,
                accion = "Reintentar",
                onAccion = onReintentar,
                esError = true,
            )
            TipoError.OTRO -> EstadoVista(
                icono = IconosConecta.ErrorCirculo,
                titulo = "No pudimos buscar la ruta",
                mensaje = error.mensaje,
                accion = "Reintentar",
                onAccion = onReintentar,
                esError = true,
            )
        }
    }
}

@Composable
private fun ListaOpciones(plan: PlanDto, onVerDetalle: (Int) -> Unit, onCambiar: () -> Unit, onReportar: () -> Unit) {
    val etiquetas = etiquetasOpciones(plan.opciones)
    val estimado = plan.calidad.tiempos != "verificado"
    LazyColumn(
        contentPadding = PaddingValues(start = Espacio.l, end = Espacio.l, top = Espacio.l, bottom = Espacio.xl),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
    ) {
        plan.aviso?.let { item { AvisoImportante(it) } }
        if (plan.opciones.isEmpty()) {
            item {
                EstadoVista(
                    icono = IconosConecta.Metro,
                    titulo = "No hay rutas en Metro",
                    mensaje = "No encontramos una ruta en Metro entre estos puntos con los datos actuales. " +
                        "Prueba con otra hora o con una estación cercana.",
                    accion = "Cambiar búsqueda",
                    onAccion = onCambiar,
                )
            }
        } else {
            item { TituloSeccion(if (plan.opciones.size == 1) "1 opción" else "${plan.opciones.size} opciones") }
            itemsIndexed(plan.opciones) { i, o ->
                TarjetaOpcion(
                    indice = i,
                    o = o,
                    etiquetas = etiquetas.getOrElse(i) { emptyList() },
                    estimado = estimado,
                    onClick = { onVerDetalle(i) },
                )
            }
        }
        item { AvisoCalidad(plan.calidad) }
        item {
            TextButton(onClick = onReportar, modifier = Modifier.fillMaxWidth()) {
                Text("¿Algo no cuadra? Reportar un problema")
            }
        }
    }
}

/** Aviso que cambia la decisión del usuario (p. ej. Metro cerrado). Va antes que todo. */
@Composable
fun AvisoImportante(texto: String) {
    val cs = MaterialTheme.colorScheme
    Surface(
        color = cs.errorContainer,
        contentColor = cs.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(Espacio.m), horizontalArrangement = Arrangement.spacedBy(Espacio.m)) {
            Icon(Icons.Filled.Warning, contentDescription = "Importante", modifier = Modifier.size(20.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Distingue visualmente lo confirmado de lo estimado (sección 9.2). */
@Composable
fun AvisoCalidad(c: CalidadDto) {
    val estimado = c.tiempos != "verificado" || c.frecuencias != "verificado"
    Aviso(
        texto = c.aviso,
        tipo = if (estimado) TipoAviso.ADVERTENCIA else TipoAviso.INFO,
        detalle = if (c.tarifas == "desconocido") "Costo del pasaje: no confirmado." else null,
    )
}

/**
 * Qué hace especial a cada opción ("Más rápida", "Directa", "Menos caminata"). Solo se marca
 * lo que de verdad la distingue de las demás: con una sola opción, o con empates, no hay etiqueta.
 */
fun etiquetasOpciones(opciones: List<OpcionDto>): List<List<String>> {
    val etiquetas = List(opciones.size) { mutableListOf<String>() }
    if (opciones.size < 2) return etiquetas

    /** Índice de la opción con el valor más bajo, si le saca al menos [margen] a la siguiente. */
    fun minimoUnico(margen: Int = 1, valor: (OpcionDto) -> Int): Int? {
        val orden = opciones.indices.sortedBy { valor(opciones[it]) }
        return orden[0].takeIf { valor(opciones[orden[1]]) - valor(opciones[it]) >= margen }
    }
    minimoUnico { it.duracionMin }?.let { etiquetas[it] += "Más rápida" }
    minimoUnico { it.transbordos }?.let { etiquetas[it] += if (opciones[it].transbordos == 0) "Directa" else "Menos transbordos" }
    minimoUnico(margen = 100) { it.caminataM }?.let { etiquetas[it] += "Menos caminata" }
    return etiquetas
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaOpcion(indice: Int, o: OpcionDto, etiquetas: List<String>, estimado: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val transbordos = when (o.transbordos) { 0 -> "Directo"; 1 -> "1 transbordo"; else -> "${o.transbordos} transbordos" }
    // TalkBack lee la tarjeta como una sola frase en vez de saltar por cada insignia e ícono
    val resumen = buildString {
        append("Opción ${indice + 1}")
        if (etiquetas.isNotEmpty()) append(", ${etiquetas.joinToString(", ").lowercase()}")
        append(": aproximadamente ${o.duracionMin} minutos, ${transbordos.lowercase()}. ")
        if (o.sale != null && o.llega != null) append("Sales ${o.sale}, llegas ${o.llega}. ")
        append(describirTramos(o.tramos))
        append(" Caminas ${o.caminataM} metros en total. Toca para ver el detalle.")
    }
    Tarjeta(onClick = onClick, resaltada = "Más rápida" in etiquetas) {
        Column(
            Modifier.clearAndSetSemantics { contentDescription = resumen },
            verticalArrangement = Arrangement.spacedBy(Espacio.m),
        ) {
            if (etiquetas.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s), verticalArrangement = Arrangement.spacedBy(Espacio.xs)) {
                    etiquetas.forEach { EtiquetaOpcion(it, destacada = it == "Más rápida") }
                }
            }
            // Lo que más importa, grande: la duración; a la derecha, a qué hora sales y llegas
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    if (estimado) {
                        Text("≈ ", style = MaterialTheme.typography.titleMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Text("${o.duracionMin}", style = MaterialTheme.typography.headlineMedium.numerico)
                    Text(" min", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 2.dp))
                }
                if (o.sale != null && o.llega != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${o.sale} – ${o.llega}", style = MaterialTheme.typography.titleMedium.numerico)
                        Text("salida – llegada", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
            }
            ResumenLineas(o.tramos)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$transbordos · ${o.caminataM} m a pie",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text("Ver detalle", style = MaterialTheme.typography.labelLarge, color = cs.primary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EtiquetaOpcion(texto: String, destacada: Boolean) {
    val cs = MaterialTheme.colorScheme
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = if (destacada) cs.onPrimary else cs.onSecondaryContainer,
        modifier = Modifier
            .background(if (destacada) cs.primary else cs.secondaryContainer, MaterialTheme.shapes.small)
            .padding(horizontal = Espacio.s, vertical = 2.dp),
    )
}

/** "Caminas 5 minutos, luego Línea 1, luego Línea 2." */
fun describirTramos(tramos: List<TramoDto>): String =
    tramos.joinToString(", luego ", postfix = ".") {
        if (it.modo == "CAMINAR") "caminas ${it.duracionMin} minutos" else "Línea ${it.linea ?: "desconocida"}"
    }.replaceFirstChar { it.uppercase() }

/** La secuencia del viaje de un vistazo: caminar 5 min › L1 › L2 › caminar 3 min. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumenLineas(tramos: List<TramoDto>) {
    val cs = MaterialTheme.colorScheme
    // Un tramo a pie de 0 min (bajar y salir) es ruido en el resumen; en el detalle sí aparece
    val visibles = tramos.filter { it.modo != "CAMINAR" || it.duracionMin > 0 }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
        verticalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        visibles.forEachIndexed { i, t ->
            if (i > 0) {
                Box(Modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = cs.outline, modifier = Modifier.size(16.dp))
                }
            }
            if (t.modo == "CAMINAR") {
                Row(
                    Modifier.heightIn(min = 24.dp).clearAndSetSemantics { contentDescription = "Caminar ${t.duracionMin} minutos" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(IconosConecta.Caminar, contentDescription = null, tint = ConectaTema.extra.caminar, modifier = Modifier.size(18.dp))
                    Text("${t.duracionMin} min", style = MaterialTheme.typography.labelMedium.numerico, color = cs.onSurfaceVariant)
                }
            } else {
                InsigniaLinea(t.linea ?: "?", colorLinea(t.colorLinea, cs.primary))
            }
        }
    }
}

/** Insignia de línea con el color oficial y el ícono del tren: no depende solo del color. */
@Composable
fun InsigniaLinea(linea: String, color: Color) {
    val texto = textoSobre(color)
    Row(
        Modifier
            .heightIn(min = 24.dp)
            .background(color, MaterialTheme.shapes.small)
            .padding(horizontal = Espacio.s, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = "Línea $linea" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Espacio.xs),
    ) {
        Icon(IconosConecta.Metro, contentDescription = null, tint = texto, modifier = Modifier.size(14.dp))
        Text("L$linea", color = texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Texto negro sobre colores claros y blanco sobre oscuros: contraste legible con cualquier color de línea. */
fun textoSobre(fondo: Color): Color = if (fondo.luminance() > 0.4f) Color.Black else Color.White

@Composable
fun PuntoLinea(color: Color) {
    Box(Modifier.size(12.dp).background(color, CircleShape))
}

@Composable
fun SeparadorVertical(color: Color) {
    Box(Modifier.width(4.dp).heightIn(min = 28.dp).background(color))
}
