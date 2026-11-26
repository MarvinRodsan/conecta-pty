package pa.conectapty.app.datos

import java.text.Normalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pa.conectapty.app.datos.local.BaseDatos
import pa.conectapty.app.datos.local.FavoritoEntidad
import pa.conectapty.app.datos.local.ParadaEntidad
import pa.conectapty.app.datos.remoto.ConectaApi
import pa.conectapty.app.datos.remoto.DatosDto
import pa.conectapty.app.datos.remoto.NuevoReporteDto
import pa.conectapty.app.datos.remoto.PlanDto

/** Estación tal como la usa la interfaz. */
data class Estacion(val id: String, val nombre: String, val lat: Double, val lon: Double) {
    /** Precalculado una vez: buscar no vuelve a normalizar textos en cada tecla. */
    val claveBusqueda: String = normalizar(nombre)

    fun comoPunto() = Punto(nombre, lat, lon, TipoPunto.ESTACION, estacionId = id)
}

enum class TipoPunto { ESTACION, LUGAR, UBICACION }

/**
 * Origen o destino de un viaje: una estación, un lugar ("UTP") o la ubicación del teléfono.
 * El backend solo necesita lat/lon; OTP agrega la caminata hasta la estación.
 *
 * @param detalle texto secundario: "Bella Vista, Panamá" o "Cerca de Vía Argentina (≈ 300 m)"
 */
data class Punto(
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val tipo: TipoPunto,
    val detalle: String? = null,
    val estacionId: String? = null,
)

class Repositorio(
    private val api: ConectaApi,
    private val bd: BaseDatos,
    private val reloj: () -> Long = System::currentTimeMillis,
) {
    val estaciones: Flow<List<Estacion>> = bd.paradas().observar().map { lista ->
        lista.map { Estacion(it.id, it.nombre, it.lat, it.lon) }
    }

    val favoritos: Flow<List<Estacion>> = bd.favoritos().observar().map { lista ->
        lista.map { Estacion(it.paradaId, it.nombre, it.lat, it.lon) }
    }

    /**
     * Actualiza el catálogo solo si tiene más de 12 h (o no existe). Si no hay red,
     * se queda con la copia local y lo indica con el resultado.
     */
    suspend fun refrescarEstaciones(forzar: Boolean = false): Result<Unit> = resultado {
        val ultima = bd.paradas().ultimaActualizacion()
        if (!forzar && ultima != null && reloj() - ultima < DOCE_HORAS) return@resultado
        val ahora = reloj()
        val remotas = api.paradas()
        bd.paradas().reemplazar(remotas.map {
            ParadaEntidad(it.id, it.nombre, it.lat, it.lon, it.accesible, ahora)
        })
    }

    /** @param salida hora local de Panamá "2026-10-26T08:00"; null = ahora */
    suspend fun planificar(origen: Punto, destino: Punto, salida: String?): Result<PlanDto> = resultado {
        api.plan(origen.lat, origen.lon, destino.lat, destino.lon, salida)
    }

    /** Direcciones y lugares (Photon vía backend). El backend exige al menos 3 letras. */
    suspend fun buscarLugares(consulta: String): Result<List<Punto>> = resultado {
        api.lugares(consulta.trim()).map {
            Punto(it.nombre, it.lat, it.lon, TipoPunto.LUGAR, detalle = it.detalle)
        }
    }

    /** Fecha y fuente de los datos que sirve el backend; null si aún no hay datos importados. */
    suspend fun datosActualizados(): Result<DatosDto?> = resultado { api.salud().datos }

    /** @return el mensaje de agradecimiento del servidor */
    suspend fun reportar(reporte: NuevoReporteDto): Result<String> = resultado { api.reportar(reporte).mensaje }

    suspend fun alternarFavorito(e: Estacion, esFavorito: Boolean) {
        if (esFavorito) bd.favoritos().borrar(e.id)
        else bd.favoritos().guardar(FavoritoEntidad(e.id, e.nombre, e.lat, e.lon, reloj()))
    }

    private companion object {
        const val DOCE_HORAS = 12L * 60 * 60 * 1000
    }
}

/** Como runCatching, pero sin tragarse la cancelación de corrutinas. */
private inline fun <T> resultado(bloque: () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

private val MARCAS = "\\p{Mn}+".toRegex()

/** "Vía Argentina" -> "via argentina". Para buscar sin importar tildes ni mayúsculas. */
fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(MARCAS, "").lowercase().trim()

fun filtrarEstaciones(todas: List<Estacion>, consulta: String): List<Estacion> {
    val q = normalizar(consulta)
    if (q.isEmpty()) return todas
    return todas.filter { q in it.claveBusqueda }
        .sortedBy { if (it.claveBusqueda.startsWith(q)) 0 else 1 }
}
