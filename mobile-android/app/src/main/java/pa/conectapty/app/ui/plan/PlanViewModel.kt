package pa.conectapty.app.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pa.conectapty.app.ConectaApp
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.Repositorio
import pa.conectapty.app.datos.remoto.PlanDto

enum class Campo { ORIGEN, DESTINO }

sealed interface Pantalla {
    data object Inicio : Pantalla
    data object Resultados : Pantalla
    data class Detalle(val indice: Int) : Pantalla
}

sealed interface EstadoPlan {
    data object Vacio : EstadoPlan
    data object Cargando : EstadoPlan
    data class Listo(val plan: PlanDto) : EstadoPlan
    data class Error(val mensaje: String) : EstadoPlan
}

data class UiEstado(
    val pantalla: Pantalla = Pantalla.Inicio,
    val origen: Estacion? = null,
    val destino: Estacion? = null,
    val eligiendo: Campo? = null,
    val plan: EstadoPlan = EstadoPlan.Vacio,
    val catalogoSinConexion: Boolean = false,
)

class PlanViewModel(private val repo: Repositorio) : ViewModel() {

    private val _ui = MutableStateFlow(UiEstado())
    val ui: StateFlow<UiEstado> = _ui.asStateFlow()

    // WhileSubscribed(5 s): deja de leer la BD cuando la app está en segundo plano
    val estaciones: StateFlow<List<Estacion>> =
        repo.estaciones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoritos: StateFlow<List<Estacion>> =
        repo.favoritos.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var busqueda: Job? = null

    init {
        refrescarCatalogo()
    }

    fun refrescarCatalogo(forzar: Boolean = false) = viewModelScope.launch {
        val r = repo.refrescarEstaciones(forzar)
        _ui.update { it.copy(catalogoSinConexion = r.isFailure) }
    }

    fun abrirSelector(campo: Campo) = _ui.update { it.copy(eligiendo = campo) }
    fun cerrarSelector() = _ui.update { it.copy(eligiendo = null) }

    fun elegir(e: Estacion) = _ui.update {
        when (it.eligiendo) {
            Campo.ORIGEN -> it.copy(origen = e, eligiendo = null)
            Campo.DESTINO -> it.copy(destino = e, eligiendo = null)
            null -> it
        }
    }

    fun usarFavoritoComoDestino(e: Estacion) = _ui.update { it.copy(destino = e) }

    fun intercambiar() = _ui.update { it.copy(origen = it.destino, destino = it.origen) }

    fun alternarFavorito(e: Estacion) = viewModelScope.launch {
        repo.alternarFavorito(e, favoritos.value.any { it.id == e.id })
    }

    fun buscar() {
        val (o, d) = _ui.value.origen to _ui.value.destino
        if (o == null || d == null) return
        if (o.id == d.id) {
            _ui.update { it.copy(pantalla = Pantalla.Resultados, plan = EstadoPlan.Error("El origen y el destino son la misma estación.")) }
            return
        }
        busqueda?.cancel() // si el usuario toca dos veces, solo cuenta la última
        _ui.update { it.copy(pantalla = Pantalla.Resultados, plan = EstadoPlan.Cargando) }
        busqueda = viewModelScope.launch {
            val r = repo.planificar(o, d)
            _ui.update {
                it.copy(plan = r.fold(
                    onSuccess = { p -> EstadoPlan.Listo(p) },
                    onFailure = { e -> EstadoPlan.Error(mensajeAmigable(e)) },
                ))
            }
        }
    }

    fun verDetalle(indice: Int) = _ui.update { it.copy(pantalla = Pantalla.Detalle(indice)) }

    /** @return false si ya estamos en Inicio (deja que el sistema cierre la app). */
    fun atras(): Boolean {
        val actual = _ui.value
        return when {
            actual.eligiendo != null -> { cerrarSelector(); true }
            actual.pantalla is Pantalla.Detalle -> { _ui.update { it.copy(pantalla = Pantalla.Resultados) }; true }
            actual.pantalla is Pantalla.Resultados -> { _ui.update { it.copy(pantalla = Pantalla.Inicio) }; true }
            else -> false
        }
    }

    private fun mensajeAmigable(e: Throwable): String = when (e) {
        is java.net.UnknownHostException, is java.net.ConnectException ->
            "Sin conexión con el servidor. Revisa tu internet e inténtalo de nuevo."
        is java.net.SocketTimeoutException -> "El servidor tardó demasiado. Inténtalo de nuevo."
        is retrofit2.HttpException -> when (e.code()) {
            503 -> "El planificador no está disponible en este momento."
            400 -> "No pudimos entender ese origen o destino."
            else -> "Error del servidor (${e.code()})."
        }
        else -> "Algo salió mal: ${e.message ?: e::class.simpleName}"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ConectaApp
                PlanViewModel(app.repositorio)
            }
        }
    }
}
