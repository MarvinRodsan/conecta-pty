package pa.conectapty.app.datos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Mensaje listo para mostrar al usuario cuando no se puede obtener la ubicación. */
class UbicacionNoDisponible(mensaje: String) : Exception(mensaje)

/**
 * Ubicación del teléfono con el LocationManager de Android: no necesita Google Play Services.
 * Solo se usa cuando el usuario toca "Usar mi ubicación" y nunca sale del teléfono:
 * la estación más cercana se calcula con el catálogo local (ver [estacionMasCercana]).
 */
class FuenteUbicacion(context: Context) {

    private val contexto = context.applicationContext
    private val gestor = contexto.getSystemService(LocationManager::class.java)

    fun tienePermiso(): Boolean = PERMISOS.any {
        ContextCompat.checkSelfPermission(contexto, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // se comprueba con tienePermiso()
    suspend fun actual(): Coordenada {
        if (!tienePermiso()) throw UbicacionNoDisponible("Sin permiso de ubicación. Puedes elegir la estación a mano.")
        if (!LocationManagerCompat.isLocationEnabled(gestor)) {
            throw UbicacionNoDisponible("La ubicación del teléfono está apagada. Actívala en Ajustes o elige la estación a mano.")
        }
        val proveedores = PROVEEDORES.filter { runCatching { gestor.isProviderEnabled(it) }.getOrDefault(false) }

        // Una lectura reciente (< 2 min) basta para elegir estación y responde al instante
        proveedores.mapNotNull { gestor.getLastKnownLocation(it) }
            .filter { edadMs(it) < DOS_MINUTOS_MS }
            .maxByOrNull { it.elapsedRealtimeNanos }
            ?.let { return it.comoCoordenada() }

        for (proveedor in proveedores) {
            val lectura = withTimeoutOrNull(ESPERA_MS) { lecturaNueva(proveedor) }
            if (lectura != null) return lectura.comoCoordenada()
        }
        throw UbicacionNoDisponible("No pudimos saber dónde estás. Inténtalo al aire libre o elige la estación a mano.")
    }

    @SuppressLint("MissingPermission")
    private suspend fun lecturaNueva(proveedor: String): Location? = suspendCancellableCoroutine { cont ->
        val cancelar = CancellationSignal()
        cont.invokeOnCancellation { cancelar.cancel() }
        LocationManagerCompat.getCurrentLocation(gestor, proveedor, cancelar, ContextCompat.getMainExecutor(contexto)) {
            if (cont.isActive) cont.resume(it)
        }
    }

    private fun edadMs(l: Location) = (SystemClock.elapsedRealtimeNanos() - l.elapsedRealtimeNanos) / 1_000_000

    private fun Location.comoCoordenada() = Coordenada(latitude, longitude)

    companion object {
        /** Basta con la aproximada; si el usuario da la precisa, mejor. */
        val PERMISOS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

        // "fused" (Android 12+) combina red y GPS; la red responde rápido bajo techo
        private val PROVEEDORES = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        private const val DOS_MINUTOS_MS = 2 * 60 * 1000L
        private const val ESPERA_MS = 10_000L
    }
}
