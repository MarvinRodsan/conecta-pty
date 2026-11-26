package pa.conectapty.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pa.conectapty.app.BuildConfig
import pa.conectapty.app.ConectaApp
import pa.conectapty.app.datos.Coordenada
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.Punto
import pa.conectapty.app.datos.Repositorio
import pa.conectapty.app.datos.TipoPunto
import pa.conectapty.app.datos.UbicacionNoDisponible
import pa.conectapty.app.datos.enPanama
import pa.conectapty.app.datos.estacionMasCercana
import pa.conectapty.app.datos.remoto.DatosDto
import pa.conectapty.app.datos.remoto.NuevoReporteDto
import pa.conectapty.app.datos.remoto.PlanDto

enum class Campo { ORIGEN, DESTINO }

sealed interface Pantalla {
    data object Inicio : Pantalla
    data object Resultados : Pantalla
    data class Detalle(val indice: Int) : Pantalla
    /** @param volverA pantalla desde la que se abrió, para regresar a ella */
    data class Reporte(val volverA: Pantalla) : Pantalla
}

sealed interface EstadoPlan {
    data object Vacio : EstadoPlan
    data object Cargando : EstadoPlan
    data class Listo(val plan: PlanDto) : EstadoPlan
    data class Error(val mensaje: String, val tipo: TipoError = TipoError.OTRO) : EstadoPlan
}

/** Para que la pantalla elija el ícono, el título y la acción correcta (reintentar o cambiar la búsqueda). */
enum class TipoError { SIN_CONEXION, SERVIDOR, MISMO_LUGAR, OTRO }

/** Búsqueda de direcciones y lugares en el selector. */
sealed interface EstadoLugares {
    data object Inactivo : EstadoLugares
    data object Cargando : EstadoLugares
    data class Listo(val lugares: List<Punto>) : EstadoLugares
    data class Error(val mensaje: String) : EstadoLugares
}

sealed interface EstadoEnvio {
    data object Editando : EstadoEnvio
    data object Enviando : EstadoEnvio
    data class Enviado(val mensaje: String) : EstadoEnvio
    data class Error(val mensaje: String) : EstadoEnvio
}

enum class TipoReporte(val etiqueta: String) {
    ESTACION("Una estación está mal (nombre, ubicación, accesos)"),
    RUTA("La ruta o el tiempo sugerido no es correcto"),
    SUGERENCIA("Tengo una sugerencia"),
    OTRO("Otro problema"),
}

data class UiEstado(
    val pantalla: Pantalla = Pantalla.Inicio,
    val origen: Punto? = null,
    val destino: Punto? = null,
    val eligiendo: Campo? = null,
    val salida: Salida = Salida.Ahora,
    val plan: EstadoPlan = EstadoPlan.Vacio,
    val catalogoSinConexion: Boolean = false,
    val lugares: EstadoLugares = EstadoLugares.Inactivo,
    val buscandoUbicacion: Boolean = false,
    /** Fecha y fuente de los datos ("Datos actualizados el …"); null mientras no se sepa. */
    val datos: DatosDto? = null,
    val envio: EstadoEnvio = EstadoEnvio.Editando,
    /** Mensaje breve de una sola vez (Snackbar): se borra con [PlanViewModel.mensajeMostrado]. */
    val mensaje: String? = null,
)

/**
 * @param ubicar ubicación actual del teléfono; lanza [UbicacionNoDisponible] con un mensaje para el usuario
 * @param reloj  hora de Panamá; se inyecta para poder probar "en 30 min"
 */
class PlanViewModel(
    private val repo: Repositorio,
    private val ubicar: suspend () -> Coordenada,
    private val reloj: () -> java.time.LocalDateTime = ::ahoraEnPanama,
) : ViewModel() {

    private val _ui = MutableStateFlow(UiEstado())
    val ui: StateFlow<UiEstado> = _ui.asStateFlow()

    // WhileSubscribed(5 s): deja de leer la BD cuando la app está en segundo plano
    val estaciones: StateFlow<List<Estacion>> =
        repo.estaciones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoritos: StateFlow<List<Estacion>> =
        repo.favoritos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var busqueda: Job? = null
    private var busquedaLugares: Job? = null

    init {
        refrescarCatalogo()
    }

    fun refrescarCatalogo(forzar: Boolean = false) = viewModelScope.launch {
        val r = repo.refrescarEstaciones(forzar)
        _ui.update { it.copy(catalogoSinConexion = r.isFailure) }
        repo.datosActualizados().onSuccess { d -> _ui.update { it.copy(datos = d) } }
    }

    fun abrirSelector(campo: Campo) = _ui.update { it.copy(eligiendo = campo) }
    fun cerrarSelector() {
        busquedaLugares?.cancel()
        _ui.update { it.copy(eligiendo = null, lugares = EstadoLugares.Inactivo) }
    }

    fun elegir(p: Punto) {
        busquedaLugares?.cancel()
        _ui.update {
            when (it.eligiendo) {
                Campo.ORIGEN -> it.copy(origen = p, eligiendo = null, lugares = EstadoLugares.Inactivo)
                Campo.DESTINO -> it.copy(destino = p, eligiendo = null, lugares = EstadoLugares.Inactivo)
                null -> it
            }
        }
    }

    fun usarFavoritoComoDestino(e: Estacion) = _ui.update { it.copy(destino = e.comoPunto()) }

    fun intercambiar() = _ui.update { it.copy(origen = it.destino, destino = it.origen) }

    /** Guarda o quita la estación y lo confirma con un aviso breve (antes no había ninguna señal). */
    fun alternarFavorito(e: Estacion) = viewModelScope.launch {
        val eraFavorita = favoritos.value.any { it.id == e.id }
        repo.alternarFavorito(e, eraFavorita)
        avisar(if (eraFavorita) "${e.nombre} se quitó de favoritos" else "${e.nombre} se guardó en favoritos")
    }

    fun elegirSalida(s: Salida) = _ui.update { it.copy(salida = s) }

    /**
     * Busca direcciones y lugares mientras el usuario escribe. Espera a que deje de teclear
     * (450 ms) para no gastar el límite de búsquedas del servidor con cada letra.
     */
    fun buscarLugares(consulta: String) {
        busquedaLugares?.cancel()
        val q = consulta.trim()
        if (q.length < 3) {
            _ui.update { it.copy(lugares = EstadoLugares.Inactivo) }
            return
        }
        busquedaLugares = viewModelScope.launch {
            delay(450)
            _ui.update { it.copy(lugares = EstadoLugares.Cargando) }
            val r = repo.buscarLugares(q)
            _ui.update {
                it.copy(lugares = r.fold(
                    onSuccess = { l -> EstadoLugares.Listo(l) },
                    onFailure = { e -> EstadoLugares.Error(mensajeAmigable(e)) },
                ))
            }
        }
    }

    /** "Usar mi ubicación": el permiso ya lo pidió la pantalla al tocar el botón. */
    fun usarMiUbicacion(campo: Campo) {
        if (_ui.value.buscandoUbicacion) return
        _ui.update { it.copy(buscandoUbicacion = true) }
        viewModelScope.launch {
            val r = try {
                Result.success(ubicar())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            _ui.update { estado ->
                val sinBusqueda = estado.copy(buscandoUbicacion = false)
                r.fold(
                    onSuccess = { c ->
                        if (!enPanama(c)) {
                            sinBusqueda.copy(mensaje = "Tu ubicación está fuera de Panamá. Elige la estación a mano.")
                        } else {
                            val punto = puntoDeUbicacion(c, estaciones.value)
                            when (campo) {
                                Campo.ORIGEN -> sinBusqueda.copy(origen = punto)
                                Campo.DESTINO -> sinBusqueda.copy(destino = punto)
                            }.copy(eligiendo = null, lugares = EstadoLugares.Inactivo)
                        }
                    },
                    onFailure = { e ->
                        sinBusqueda.copy(mensaje = (e as? UbicacionNoDisponible)?.message
                            ?: "No pudimos obtener tu ubicación. Elige la estación a mano.")
                    },
                )
            }
        }
    }

    fun avisar(mensaje: String) = _ui.update { it.copy(mensaje = mensaje) }
    fun mensajeMostrado() = _ui.update { it.copy(mensaje = null) }

    fun buscar() {
        val (o, d) = _ui.value.origen to _ui.value.destino
        if (o == null || d == null) return
        if (mismoLugar(o, d)) {
            _ui.update { it.copy(pantalla = Pantalla.Resultados, plan = EstadoPlan.Error("El origen y el destino son el mismo lugar.", TipoError.MISMO_LUGAR)) }
            return
        }
        busqueda?.cancel() // si el usuario toca dos veces, solo cuenta la última
        val salida = _ui.value.salida.comoParametro(reloj())
        _ui.update { it.copy(pantalla = Pantalla.Resultados, plan = EstadoPlan.Cargando) }
        busqueda = viewModelScope.launch {
            val r = repo.planificar(o, d, salida)
            _ui.update {
                it.copy(plan = r.fold(
                    onSuccess = { p -> EstadoPlan.Listo(p) },
                    onFailure = { e -> EstadoPlan.Error(mensajeAmigable(e), tipoDeError(e)) },
                ))
            }
        }
    }

    fun verDetalle(indice: Int) = _ui.update { it.copy(pantalla = Pantalla.Detalle(indice)) }

    fun abrirReporte() = _ui.update {
        if (it.pantalla is Pantalla.Reporte) it
        else it.copy(pantalla = Pantalla.Reporte(volverA = it.pantalla), envio = EstadoEnvio.Editando)
    }

    /** @param incluirRuta adjunta "Origen → Destino, hora" (solo nombres, nunca coordenadas) */
    fun enviarReporte(tipo: TipoReporte, mensaje: String, incluirRuta: Boolean) {
        if (_ui.value.envio == EstadoEnvio.Enviando) return
        val estado = _ui.value
        val contexto = if (incluirRuta) contextoRuta(estado) else null
        val paradaId = if (tipo == TipoReporte.ESTACION) {
            estado.origen?.estacionId ?: estado.destino?.estacionId
        } else null
        _ui.update { it.copy(envio = EstadoEnvio.Enviando) }
        viewModelScope.launch {
            val r = repo.reportar(NuevoReporteDto(tipo.name, mensaje.trim(), paradaId, contexto, BuildConfig.VERSION_NAME))
            _ui.update {
                it.copy(envio = r.fold(
                    onSuccess = { m -> EstadoEnvio.Enviado(m) },
                    onFailure = { e -> EstadoEnvio.Error(mensajeAmigable(e)) },
                ))
            }
        }
    }

    /** @return false si ya estamos en Inicio (deja que el sistema cierre la app). */
    fun atras(): Boolean {
        val actual = _ui.value
        return when {
            actual.eligiendo != null -> { cerrarSelector(); true }
            actual.pantalla is Pantalla.Reporte -> { _ui.update { it.copy(pantalla = actual.pantalla.volverA) }; true }
            actual.pantalla is Pantalla.Detalle -> { _ui.update { it.copy(pantalla = Pantalla.Resultados) }; true }
            actual.pantalla is Pantalla.Resultados -> { _ui.update { it.copy(pantalla = Pantalla.Inicio) }; true }
            else -> false
        }
    }

    private fun contextoRuta(e: UiEstado): String? {
        val o = e.origen ?: return null
        val d = e.destino ?: return null
        val opcion = (e.pantalla as? Pantalla.Reporte)?.volverA as? Pantalla.Detalle
        return buildString {
            append("${o.nombre} → ${d.nombre}, ${e.salida.describir(reloj())}")
            if (opcion != null) append(", opción ${opcion.indice + 1}")
        }.take(300)
    }

    private fun mensajeAmigable(e: Throwable): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException ->
            "Sin conexión con el servidor. Revisa tu internet e inténtalo de nuevo."
        is java.net.SocketTimeoutException -> "El servidor tardó demasiado. Inténtalo de nuevo."
        is retrofit2.HttpException -> when (e.code()) {
            503 -> "El servicio no está disponible en este momento."
            429 -> "Hiciste muchas solicitudes seguidas. Espera un minuto e inténtalo de nuevo."
            400 -> detalleDelServidor(e) ?: "No pudimos entender esa solicitud."
            else -> "Error del servidor (${e.code()})."
        }
        else -> "Algo salió mal: ${e.message ?: e::class.simpleName}"
    }

    private fun tipoDeError(e: Throwable): TipoError = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException -> TipoError.SIN_CONEXION
        is java.net.SocketTimeoutException -> TipoError.SERVIDOR
        is retrofit2.HttpException -> if (e.code() == 400) TipoError.OTRO else TipoError.SERVIDOR
        else -> TipoError.OTRO
    }

    /** El backend responde errores como application/problem+json; su "detail" ya está en español. */
    private fun detalleDelServidor(e: retrofit2.HttpException): String? = runCatching {
        val cuerpo = e.response()?.errorBody()?.string() ?: return null
        Json.parseToJsonElement(cuerpo).jsonObject["detail"]?.jsonPrimitive?.content
    }.getOrNull()?.takeIf { it.isNotBlank() && it.length < 200 }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ConectaApp
                PlanViewModel(app.repositorio, ubicar = app.ubicacion::actual)
            }
        }
    }
}

/** Mismo lugar si es la misma estación o si están a menos de ~10 m. */
fun mismoLugar(a: Punto, b: Punto): Boolean =
    (a.estacionId != null && a.estacionId == b.estacionId) ||
        (kotlin.math.abs(a.lat - b.lat) < 1e-4 && kotlin.math.abs(a.lon - b.lon) < 1e-4)

/** "Mi ubicación" con la estación más cercana como pista, calculada en el teléfono. */
fun puntoDeUbicacion(c: Coordenada, estaciones: List<Estacion>): Punto {
    val cercana = estacionMasCercana(c, estaciones)
    return Punto(
        nombre = "Mi ubicación",
        lat = c.lat,
        lon = c.lon,
        tipo = TipoPunto.UBICACION,
        detalle = cercana?.let { (e, m) -> "Estación más cercana: ${e.nombre} (${distanciaLegible(m)})" },
    )
}
