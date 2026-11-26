package pa.conectapty.app.ui.plan

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import pa.conectapty.app.R
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.Punto
import pa.conectapty.app.datos.remoto.DatosDto
import pa.conectapty.app.ui.componentes.Aviso
import pa.conectapty.app.ui.componentes.BotonPrincipal
import pa.conectapty.app.ui.componentes.IconosConecta
import pa.conectapty.app.ui.componentes.TipoAviso
import pa.conectapty.app.ui.componentes.TituloSeccion
import pa.conectapty.app.ui.tema.ConectaTema
import pa.conectapty.app.ui.tema.Elevacion
import pa.conectapty.app.ui.tema.Espacio

/**
 * Inicio: planificar un viaje. Jerarquía (de arriba abajo): encabezado de marca con saludo,
 * bloque del viaje (origen → destino, intercambiar, hora de salida), favoritos y datos.
 * "Buscar ruta" queda fijo abajo, al alcance del pulgar, aunque el contenido se desplace
 * (letra grande, pantallas chicas).
 *
 * El encabezado se dibuja detrás de la barra de estado: [modifier] no debe traer relleno superior.
 */
@Composable
fun PantallaInicio(
    ui: UiEstado,
    favoritos: List<Estacion>,
    hayCatalogo: Boolean,
    onElegir: (Campo) -> Unit,
    onUsarUbicacion: () -> Unit,
    onIntercambiar: () -> Unit,
    onFavorito: (Estacion) -> Unit,
    onSalida: (Salida) -> Unit,
    onBuscar: () -> Unit,
    onReintentarCatalogo: () -> Unit,
    onReportar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconosClarosEnBarraDeEstado()
    val scroll = rememberScrollState()
    val listo = ui.origen != null && ui.destino != null

    // Surface (no solo un fondo): fija también el color del texto, así se lee en claro y en oscuro
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Column {
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            Encabezado()
            // La tarjeta sube sobre el encabezado: une la marca con la acción principal
            Column(
                Modifier.offset(y = -Espacio.xl).padding(horizontal = Espacio.l),
                verticalArrangement = Arrangement.spacedBy(Espacio.l),
            ) {
                BloqueViaje(ui, onElegir, onUsarUbicacion, onIntercambiar, onSalida)
                if (ui.catalogoSinConexion) {
                    Aviso(
                        texto = if (hayCatalogo) "Sin conexión: usando las estaciones guardadas." else "No se pudieron cargar las estaciones.",
                        tipo = if (hayCatalogo) TipoAviso.ADVERTENCIA else TipoAviso.ERROR,
                        detalle = if (hayCatalogo) null else "Revisa tu internet: sin estaciones no se puede elegir origen ni destino.",
                        accion = "Reintentar",
                        onAccion = onReintentarCatalogo,
                    )
                }
                Favoritos(favoritos, onFavorito)
                PieDatos(ui.datos, onReportar)
            }
        }

        // Barra inferior fija: una sola acción principal
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth()) {
                if (scroll.canScrollForward) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    Modifier.padding(horizontal = Espacio.l, vertical = Espacio.m),
                    verticalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    AnimatedVisibility(visible = !listo) {
                        Text(
                            "Elige el origen y el destino para buscar tu ruta",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    BotonPrincipal("Buscar ruta", onClick = onBuscar, enabled = listo, icono = Icons.Filled.Search)
                }
            }
        }
    } }
}

/** Saludo según la hora de Panamá: discreto, no compite con el bloque del viaje. */
internal fun saludo(hora: Int) = when (hora) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
private fun Encabezado() {
    val extra = ConectaTema.extra
    Box(Modifier.fillMaxWidth().background(extra.encabezado)) {
        // Skyline de Ciudad de Panamá como fondo, casi transparente: identidad sin ruido
        Image(
            painterResource(R.drawable.skyline_panama),
            contentDescription = null,
            colorFilter = ColorFilter.tint(extra.onEncabezado.copy(alpha = 0.10f)),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
        )
        Column(
            Modifier.statusBarsPadding().padding(start = Espacio.l, end = Espacio.l, top = Espacio.l, bottom = Espacio.xxl + Espacio.xl),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logo_c), contentDescription = null, modifier = Modifier.size(28.dp))
                Text(
                    "Conecta PTY",
                    style = MaterialTheme.typography.titleMedium,
                    color = extra.onEncabezado,
                    modifier = Modifier.padding(start = Espacio.s),
                )
            }
            val hora = remember { ahoraEnPanama().hour }
            Column(Modifier.padding(top = Espacio.xl).semantics(mergeDescendants = true) { heading() }) {
                Text(saludo(hora), style = MaterialTheme.typography.bodyMedium, color = extra.onEncabezadoSecundario)
                Text("¿A dónde vas hoy?", style = MaterialTheme.typography.headlineMedium, color = extra.onEncabezado)
            }
        }
    }
}

/**
 * Origen y destino como un solo bloque: anillo (origen) → línea punteada → marcador (destino).
 * Se distinguen por forma, posición y texto, no solo por color.
 */
@Composable
private fun BloqueViaje(
    ui: UiEstado,
    onElegir: (Campo) -> Unit,
    onUsarUbicacion: () -> Unit,
    onIntercambiar: () -> Unit,
    onSalida: (Salida) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = cs.surfaceContainerLowest,
        shadowElevation = Elevacion.flotante,
        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CampoPunto(
                        etiqueta = "Desde",
                        punto = ui.origen,
                        esOrigen = true,
                        onClick = { onElegir(Campo.ORIGEN) },
                        final = {
                            IconButton(onClick = onUsarUbicacion, enabled = !ui.buscandoUbicacion) {
                                if (ui.buscandoUbicacion) {
                                    CircularProgressIndicator(
                                        Modifier.size(20.dp).semantics { contentDescription = "Buscando tu ubicación" },
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(IconosConecta.MiUbicacion, contentDescription = "Usar mi ubicación como origen", tint = cs.primary)
                                }
                            }
                        },
                    )
                    HorizontalDivider(Modifier.padding(start = 56.dp), color = cs.outlineVariant)
                    CampoPunto(etiqueta = "Hasta", punto = ui.destino, esOrigen = false, onClick = { onElegir(Campo.DESTINO) })
                }
                // Intercambiar: entre los dos campos, donde se espera (no como un botón de texto largo)
                IconButton(
                    onClick = onIntercambiar,
                    enabled = ui.origen != null || ui.destino != null,
                    modifier = Modifier.padding(end = Espacio.xs),
                ) {
                    Box(
                        Modifier.size(36.dp).border(1.dp, cs.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(IconosConecta.Intercambiar, contentDescription = "Intercambiar origen y destino", modifier = Modifier.size(20.dp))
                    }
                }
            }
            HorizontalDivider(color = cs.outlineVariant)
            Row(
                Modifier.padding(start = Espacio.l, end = Espacio.l, top = Espacio.m, bottom = Espacio.s),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    IconosConecta.Reloj, contentDescription = null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, end = Espacio.l).size(20.dp),
                )
                SelectorSalida(ui.salida, onSalida, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CampoPunto(
    etiqueta: String,
    punto: Punto?,
    esOrigen: Boolean,
    onClick: () -> Unit,
    final: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val extra = ConectaTema.extra
    val conector = cs.outline
    val descripcion = "$etiqueta: ${punto?.nombre ?: "sin elegir"}${punto?.detalle?.let { ", $it" } ?: ""}. Toca para cambiar."
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                // Línea punteada que une el origen con el destino, a la altura de los íconos
                .drawBehind {
                    val x = (Espacio.l + 12.dp).toPx()
                    val efecto = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
                    val (desde, hasta) = if (esOrigen) size.height / 2 + 12.dp.toPx() to size.height
                    else 0f to size.height / 2 - 14.dp.toPx()
                    drawLine(conector, Offset(x, desde), Offset(x, hasta), strokeWidth = 2.dp.toPx(), pathEffect = efecto)
                }
                .clearAndSetSemantics {
                    contentDescription = descripcion
                    role = Role.Button
                    onClick(label = "cambiar") { onClick(); true }
                }
                .padding(start = Espacio.l, end = Espacio.s, top = Espacio.m, bottom = Espacio.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                if (esOrigen) Icon(IconosConecta.Origen, contentDescription = null, tint = extra.origen, modifier = Modifier.size(16.dp))
                else Icon(Icons.Filled.LocationOn, contentDescription = null, tint = extra.destino, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.padding(start = Espacio.l)) {
                Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                if (punto != null) {
                    Text(punto.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    punto.detalle?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Text(
                        if (esOrigen) "¿Desde dónde sales?" else "¿A dónde vas?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
        final?.invoke()
    }
}

@Composable
private fun Favoritos(favoritos: List<Estacion>, onFavorito: (Estacion) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        TituloSeccion("Tus favoritos")
        if (favoritos.isEmpty()) {
            // Estado vacío que enseña la función en vez de esconder la sección
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(cs.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(Espacio.l)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Espacio.m),
            ) {
                Icon(IconosConecta.EstrellaVacia, contentDescription = null, tint = cs.primary)
                Column {
                    Text("Aún no tienes favoritos", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Al elegir una estación, toca la estrella para tenerla aquí a un toque.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Espacio.s)) {
                items(favoritos, key = { it.id }) { f ->
                    AssistChip(
                        onClick = { onFavorito(f) },
                        label = { Text(f.nombre) },
                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp)) },
                        shape = MaterialTheme.shapes.small,
                        colors = AssistChipDefaults.assistChipColors(containerColor = cs.surfaceContainerLowest),
                        modifier = Modifier.semantics { contentDescription = "Ir a ${f.nombre} (favorito)" },
                    )
                }
            }
            Text(
                "Toca uno para usarlo como destino.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = Espacio.xs),
            )
        }
    }
}

/** "Datos actualizados el …" con su fuente y licencia (lo exige el documento base y la ODbL). */
@Composable
fun PieDatos(datos: DatosDto?, onReportar: () -> Unit) {
    val texto = if (datos != null) {
        "Datos: ${datos.fuente}${datos.licencia?.let { " ($it)" } ?: ""} · actualizados el ${fechaLegible(datos.actualizadoEl)}"
    } else {
        "Estaciones: © colaboradores de OpenStreetMap (ODbL)"
    }
    Column(Modifier.fillMaxWidth().padding(top = Espacio.s)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Espacio.m),
        )
        TextButton(onClick = onReportar, modifier = Modifier.offset(x = -Espacio.m)) { Text("Reportar un problema") }
    }
}

/**
 * El encabezado es azul oscuro: mientras Inicio está visible, los íconos de la barra de estado
 * van en blanco. Al salir se restauran según el tema (claro → íconos oscuros).
 */
@Composable
private fun IconosClarosEnBarraDeEstado() {
    if (LocalInspectionMode.current) return
    val vista = LocalView.current
    val temaClaro = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(vista, temaClaro) {
        val ventana = (vista.context as? Activity)?.window
        val control = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        control?.isAppearanceLightStatusBars = false
        onDispose { control?.isAppearanceLightStatusBars = temaClaro }
    }
}
