package pa.conectapty.app.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import pa.conectapty.app.BuildConfig
import pa.conectapty.app.datos.Coordenada
import pa.conectapty.app.datos.decodificarPolilinea
import pa.conectapty.app.datos.remoto.TramoDto
import pa.conectapty.app.ui.tema.colorLinea

/** Trazo de un tramo listo para dibujar. */
data class TrazoMapa(val modo: String, val colorHex: String, val puntos: List<Coordenada>)

fun trazosDe(tramos: List<TramoDto>, colorDefecto: Color, colorCaminar: Color): List<TrazoMapa> =
    tramos.mapNotNull { t ->
        val puntos = t.geometria?.let(::decodificarPolilinea).orEmpty()
        if (puntos.size < 2) return@mapNotNull null
        val color = if (t.modo == "CAMINAR") colorCaminar else colorLinea(t.colorLinea, colorDefecto)
        TrazoMapa(t.modo, hex(color), puntos)
    }

/**
 * Recorrido sobre un mapa de MapLibre con teselas de OpenFreeMap (gratis, sin API key).
 * Metro en el color de su línea, caminatas punteadas, inicio y fin marcados.
 *
 * Es un complemento: TalkBack lo salta y lee la lista paso a paso, que es la fuente principal.
 */
@Composable
fun MapaRuta(trazos: List<TrazoMapa>, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    val margen = with(LocalDensity.current) { 32.dp.roundToPx() }
    val colorBorde = hex(MaterialTheme.colorScheme.surface)
    val geoJson = remember(trazos) { geoJsonDe(trazos) }

    val mapa = remember {
        MapLibre.getInstance(contexto) // carga la librería nativa solo al abrir un detalle
        MapView(contexto).apply { onCreate(null) }
    }

    DisposableEffect(ciclo, mapa) {
        // MapView necesita que le pasen el ciclo de vida de la pantalla
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> mapa.onStart()
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                Lifecycle.Event.ON_STOP -> mapa.onStop()
                else -> Unit
            }
        }
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            if (ciclo.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapa.onPause()
            if (ciclo.currentState.isAtLeast(Lifecycle.State.STARTED)) mapa.onStop()
            mapa.onDestroy()
        }
    }

    DisposableEffect(mapa, geoJson) {
        mapa.getMapAsync { m ->
            m.uiSettings.isRotateGesturesEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.uiSettings.isCompassEnabled = false
            m.setStyle(Style.Builder().fromUri(BuildConfig.MAPA_ESTILO)) { estilo ->
                estilo.addSource(GeoJsonSource(FUENTE, geoJson))
                estilo.addLayer(
                    LineLayer("ruta-caminar", FUENTE)
                        .withFilter(Expression.eq(Expression.get("modo"), Expression.literal("CAMINAR")))
                        .withProperties(
                            PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineDasharray(arrayOf(1f, 1.5f)),
                        ),
                )
                estilo.addLayer(
                    LineLayer("ruta-metro", FUENTE)
                        .withFilter(Expression.all(
                            Expression.eq(Expression.geometryType(), Expression.literal("LineString")),
                            Expression.neq(Expression.get("modo"), Expression.literal("CAMINAR")),
                        ))
                        .withProperties(
                            PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
                            PropertyFactory.lineWidth(6f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        ),
                )
                estilo.addLayer(
                    CircleLayer("ruta-extremos", FUENTE)
                        .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Point")))
                        .withProperties(
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleColor(Expression.toColor(Expression.get("color"))),
                            PropertyFactory.circleStrokeWidth(3f),
                            PropertyFactory.circleStrokeColor(colorBorde),
                        ),
                )
                encuadrar(m, trazos.flatMap { it.puntos }, margen)
            }
        }
        onDispose { }
    }

    Box(modifier.clearAndSetSemantics { contentDescription = "Mapa del recorrido. Los pasos están en la lista." }) {
        AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize())
    }
}

private fun encuadrar(m: org.maplibre.android.maps.MapLibreMap, puntos: List<Coordenada>, margen: Int) {
    val distintos = puntos.map { LatLng(it.lat, it.lon) }.distinct()
    when {
        distintos.isEmpty() -> Unit
        distintos.size == 1 -> m.moveCamera(CameraUpdateFactory.newLatLngZoom(distintos.first(), 14.0))
        else -> m.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(distintos).build(), margen))
    }
}

/** GeoJSON con una línea por tramo y dos puntos (inicio y fin del viaje). */
fun geoJsonDe(trazos: List<TrazoMapa>): String = buildJsonObject {
    put("type", "FeatureCollection")
    putJsonArray("features") {
        trazos.forEach { t ->
            addJsonObject {
                put("type", "Feature")
                putJsonObject("properties") { put("modo", t.modo); put("color", t.colorHex) }
                putJsonObject("geometry") {
                    put("type", "LineString")
                    put("coordinates", JsonArray(t.puntos.map { coordenada(it) }))
                }
            }
        }
        val inicio = trazos.firstOrNull()?.puntos?.firstOrNull()
        val fin = trazos.lastOrNull()?.puntos?.lastOrNull()
        listOfNotNull(inicio?.let { it to COLOR_INICIO }, fin?.let { it to COLOR_FIN }).forEach { (c, color) ->
            addJsonObject {
                put("type", "Feature")
                putJsonObject("properties") { put("modo", "PUNTO"); put("color", color) }
                putJsonObject("geometry") {
                    put("type", "Point")
                    put("coordinates", coordenada(c))
                }
            }
        }
    }
}.toString()

// GeoJSON usa [longitud, latitud], al revés que LatLng
private fun coordenada(c: Coordenada) = buildJsonArray { add(JsonPrimitive(c.lon)); add(JsonPrimitive(c.lat)) }

private fun hex(c: Color) = String.format("#%06X", 0xFFFFFF and c.toArgb())

private const val FUENTE = "ruta"
private const val COLOR_INICIO = "#2E7D5B"
private const val COLOR_FIN = "#C62828"
