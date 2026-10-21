package pa.conectapty.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pa.conectapty.app.ui.plan.Campo
import pa.conectapty.app.ui.plan.EstadoPlan
import pa.conectapty.app.ui.plan.PantallaDetalle
import pa.conectapty.app.ui.plan.PantallaInicio
import pa.conectapty.app.ui.plan.PantallaResultados
import pa.conectapty.app.ui.plan.Pantalla
import pa.conectapty.app.ui.plan.PlanViewModel
import pa.conectapty.app.ui.plan.SelectorEstacion
import pa.conectapty.app.ui.tema.ConectaTema

class MainActivity : ComponentActivity() {

    private val vm: PlanViewModel by viewModels { PlanViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ConectaTema { Raiz(vm) } }
    }
}

@Composable
private fun Raiz(vm: PlanViewModel) {
    // collectAsStateWithLifecycle: deja de escuchar cuando la app no está visible
    val ui by vm.ui.collectAsStateWithLifecycle()
    val estaciones by vm.estaciones.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()

    BackHandler(enabled = ui.eligiendo != null || ui.pantalla != Pantalla.Inicio) { vm.atras() }

    Scaffold { relleno ->
        val m = Modifier.padding(relleno)
        val campo = ui.eligiendo
        val pantalla = ui.pantalla
        when {
            campo != null -> SelectorEstacion(
                titulo = if (campo == Campo.ORIGEN) "¿Desde dónde sales?" else "¿A dónde vas?",
                estaciones = estaciones,
                favoritos = favoritos,
                onElegir = vm::elegir,
                onAlternarFavorito = { vm.alternarFavorito(it) },
                modifier = m,
            )
            pantalla is Pantalla.Detalle -> {
                val listo = ui.plan as? EstadoPlan.Listo
                val opcion = listo?.plan?.opciones?.getOrNull(pantalla.indice)
                if (listo != null && opcion != null) PantallaDetalle(opcion, listo.plan.calidad, m)
                else LaunchedEffect(pantalla) { vm.atras() } // nunca cambiar estado durante la composición
            }
            pantalla == Pantalla.Resultados -> PantallaResultados(
                ui = ui,
                onVerDetalle = vm::verDetalle,
                onReintentar = vm::buscar,
                modifier = m,
            )
            else -> PantallaInicio(
                ui = ui,
                favoritos = favoritos,
                hayCatalogo = estaciones.isNotEmpty(),
                onElegir = vm::abrirSelector,
                onIntercambiar = vm::intercambiar,
                onFavorito = vm::usarFavoritoComoDestino,
                onBuscar = vm::buscar,
                onReintentarCatalogo = { vm.refrescarCatalogo(forzar = true) },
                modifier = m,
            )
        }
    }
}
