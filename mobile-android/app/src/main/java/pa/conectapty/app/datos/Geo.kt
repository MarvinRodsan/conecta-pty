package pa.conectapty.app.datos

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Coordenada simple: sin depender de Location ni de MapLibre, así se prueba en la JVM. */
data class Coordenada(val lat: Double, val lon: Double)

/** Distancia en metros sobre la esfera (haversine). De sobra para "la estación más cercana". */
fun distanciaM(a: Coordenada, b: Coordenada): Double {
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLon = Math.toRadians(b.lon - a.lon)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
    return 2 * RADIO_TIERRA_M * asin(sqrt(h))
}

/** Se calcula con el catálogo guardado en el teléfono: funciona sin red y sin enviar la ubicación. */
fun estacionMasCercana(desde: Coordenada, estaciones: List<Estacion>): Pair<Estacion, Int>? =
    estaciones
        .map { it to distanciaM(desde, Coordenada(it.lat, it.lon)) }
        .minByOrNull { it.second }
        ?.let { (e, d) -> e to d.toInt() }

/** Mismos límites que valida el backend (/plan y /paradas/cercanas). */
fun enPanama(c: Coordenada): Boolean = c.lat in 7.0..10.0 && c.lon in -83.1..-77.1

/**
 * Decodifica una polilínea de Google (precisión 5), el formato en que el backend
 * envía la geometría de cada tramo. https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
fun decodificarPolilinea(codificada: String): List<Coordenada> {
    val puntos = ArrayList<Coordenada>(codificada.length / 4)
    var i = 0
    var lat = 0
    var lon = 0
    while (i < codificada.length) {
        var resultado: Int
        var desplazamiento: Int
        var b: Int

        resultado = 0; desplazamiento = 0
        do {
            b = codificada[i++].code - 63
            resultado = resultado or ((b and 0x1f) shl desplazamiento)
            desplazamiento += 5
        } while (b >= 0x20 && i < codificada.length)
        lat += if (resultado and 1 != 0) (resultado shr 1).inv() else resultado shr 1

        if (i >= codificada.length) break // polilínea cortada: se descarta el punto incompleto
        resultado = 0; desplazamiento = 0
        do {
            b = codificada[i++].code - 63
            resultado = resultado or ((b and 0x1f) shl desplazamiento)
            desplazamiento += 5
        } while (b >= 0x20 && i < codificada.length)
        lon += if (resultado and 1 != 0) (resultado shr 1).inv() else resultado shr 1

        puntos += Coordenada(lat / 1e5, lon / 1e5)
    }
    return puntos
}

private const val RADIO_TIERRA_M = 6_371_000.0
