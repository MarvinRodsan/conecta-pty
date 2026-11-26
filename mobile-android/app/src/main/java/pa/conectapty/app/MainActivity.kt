package pa.conectapty.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pa.conectapty.app.datos.Estacion
import pa.conectapty.app.datos.FuenteUbicacion
import pa.conectapty.app.ui.plan.Campo
import pa.conectapty.app.ui.plan.ESPANOL
import pa.conectapty.app.ui.plan.EstadoPlan
import pa.conectapty.app.ui.plan.Pantalla
import pa.conectapty.app.ui.plan.PantallaDetalle
import pa.conectapty.app.ui.plan.PantallaInicio
import pa.conectapty.app.ui.plan.PantallaReporte
import pa.conectapty.app.ui.plan.PantallaResultados
import pa.conectapty.app.ui.plan.PlanViewModel
import pa.conectapty.app.ui.plan.SelectorEstacion
import pa.conectapty.app.ui.plan.UiEstado
import pa.conectapty.app.ui.tema.ConectaTema

class MainActivity : ComponentActivity() {

    private val vm: PlanViewModel by viewModels { PlanViewModel.Factory }

    /**
     * Los textos de la app están en español, así que también lo están los de Material
     * (calendario, selector de hora, TalkBack) aunque el teléfono esté en inglés u otro idioma.
     */
    override fun attachBaseContext(base: Context) {
        val config = Configuration(base.resources.configuration).apply { setLocale(ESPANOL) }
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // antes de super.onCreate: pantalla de inicio del sistema (Android 12+ y compat)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ubicacion = (application as ConectaApp).ubicacion
        setContent { ConectaTema { Raiz(vm, ubicacion) } }
    }
}

@Composable
private fun Raiz(vm: PlanViewModel, ubicacion: FuenteUbicacion) {
    // collectAsStateWithLifecycle: deja de escuchar cuando la app no está visible
    val ui by vm.ui.collectAsStateWithLifecycle()
    val estaciones by vm.estaciones.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    // El permiso de ubicación se pide solo al tocar "Usar mi ubicación", nunca al abrir la app
    var campoUbicacion by rememberSaveable { mutableStateOf(Campo.ORIGEN) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) vm.usarMiUbicacion(campoUbicacion)
        else vm.avisar("Sin permiso de ubicación. Puedes elegir la estación a mano.")
    }
    val usarUbicacion: (Campo) -> Unit = { campo ->
        campoUbicacion = campo
        if (ubicacion.tienePermiso()) vm.usarMiUbicacion(campo) else pedirPermiso.launch(FuenteUbicacion.PERMISOS)
    }

    LaunchedEffect(ui.mensaje) {
        ui.mensaje?.let {
            avisos.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    BackHandler(enabled = ui.eligiendo != null || ui.pantalla != Pantalla.Inicio) { vm.atras() }

    Scaffold(snackbarHost = { SnackbarHost(avisos) }) { relleno ->
        // Transición corta entre pantallas: entrar desliza desde la derecha, volver desde la izquierda.
        // Se anima por "destino" (selector o pantalla); los datos se leen siempre del estado actual.
        AnimatedContent(
            targetState = Destino(ui.eligiendo, ui.pantalla),
            transitionSpec = {
                val avanza = targetState.profundidad >= initialState.profundidad
                val desliz = { ancho: Int -> (if (avanza) ancho else -ancho) / 8 }
                (fadeIn(tween(220, delayMillis = 60)) + slideInHorizontally(tween(280)) { desliz(it) })
                    .togetherWith(fadeOut(tween(120)) + slideOutHorizontally(tween(280)) { -desliz(it) })
            },
            label = "pantallas",
        ) { destino ->
            Pantallas(destino, ui, vm, estaciones, favoritos, usarUbicacion, relleno)
        }
    }
}

/** Qué se ve: el selector de estación (si está abierto) o una pantalla. */
private data class Destino(val campo: Campo?, val pantalla: Pantalla) {
    /** Para saber si se avanza (desliza a la izquierda) o se vuelve (a la derecha). */
    val profundidad: Int
        get() = when {
            campo != null -> 1
            pantalla is Pantalla.Reporte -> 3
            pantalla is Pantalla.Detalle -> 2
            pantalla == Pantalla.Resultados -> 1
            else -> 0
        }
}

@Composable
private fun Pantallas(
    destino: Destino,
    ui: UiEstado,
    vm: PlanViewModel,
    estaciones: List<Estacion>,
    favoritos: List<Estacion>,
    usarUbicacion: (Campo) -> Unit,
    relleno: PaddingValues,
) {
    // consumeWindowInsets: imePadding() de cada pantalla no vuelve a sumar la barra de navegación
    val m = Modifier.padding(relleno).consumeWindowInsets(relleno)
    val campo = destino.campo
    val pantalla = destino.pantalla
    when {
        campo != null -> SelectorEstacion(
            titulo = if (campo == Campo.ORIGEN) "¿Desde dónde sales?" else "¿A dónde vas?",
            estaciones = estaciones,
            favoritos = favoritos,
            lugares = ui.lugares,
            buscandoUbicacion = ui.buscandoUbicacion,
            onConsulta = vm::buscarLugares,
            onElegir = vm::elegir,
            onUsarUbicacion = { usarUbicacion(campo) },
            onAlternarFavorito = { vm.alternarFavorito(it) },
            onVolver = { vm.atras() },
            modifier = m,
            seleccionado = if (campo == Campo.ORIGEN) ui.origen else ui.destino,
            esOrigen = campo == Campo.ORIGEN,
        )
        pantalla is Pantalla.Reporte -> PantallaReporte(
            envio = ui.envio,
            hayRuta = ui.origen != null && ui.destino != null && pantalla.volverA != Pantalla.Inicio,
            onEnviar = vm::enviarReporte,
            onVolver = { vm.atras() },
            modifier = m,
        )
        pantalla is Pantalla.Detalle -> {
            val listo = ui.plan as? EstadoPlan.Listo
            val opcion = listo?.plan?.opciones?.getOrNull(pantalla.indice)
            if (listo != null && opcion != null) {
                PantallaDetalle(opcion, listo.plan.calidad, onVolver = { vm.atras() }, onReportar = vm::abrirReporte, modifier = m)
            } else if (destino == Destino(ui.eligiendo, ui.pantalla)) {
                // Solo la pantalla visible (no la que sale con la animación) puede volver atrás.
                LaunchedEffect(pantalla) { vm.atras() } // nunca cambiar estado durante la composición
            }
        }
        pantalla == Pantalla.Resultados -> PantallaResultados(
            ui = ui,
            onVerDetalle = vm::verDetalle,
            onReintentar = vm::buscar,
            onVolver = { vm.atras() },
            onReportar = vm::abrirReporte,
            modifier = m,
        )
        else -> PantallaInicio(
            ui = ui,
            favoritos = favoritos,
            hayCatalogo = estaciones.isNotEmpty(),
            onElegir = vm::abrirSelector,
            onUsarUbicacion = { usarUbicacion(Campo.ORIGEN) },
            onIntercambiar = vm::intercambiar,
            onFavorito = vm::usarFavoritoComoDestino,
            onSalida = vm::elegirSalida,
            onBuscar = vm::buscar,
            onReintentarCatalogo = { vm.refrescarCatalogo(forzar = true) },
            onReportar = vm::abrirReporte,
            // El encabezado de Inicio va detrás de la barra de estado: sin relleno superior
            modifier = Modifier.padding(bottom = relleno.calculateBottomPadding()),
        )
    }
}
