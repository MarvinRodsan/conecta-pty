package pa.conectapty.app

import android.app.Application
import io.sentry.android.core.SentryAndroid
import pa.conectapty.app.datos.FuenteUbicacion
import pa.conectapty.app.datos.Repositorio
import pa.conectapty.app.datos.local.BaseDatos
import pa.conectapty.app.datos.remoto.ConectaApi

/**
 * Inyección de dependencias manual: sin Hilt/Dagger el proyecto compila más rápido
 * y la app arranca con menos trabajo. Si crece mucho, se puede migrar a Hilt.
 */
class ConectaApp : Application() {
    val repositorio: Repositorio by lazy {
        Repositorio(
            api = ConectaApi.crear(BuildConfig.API_URL, cacheDir),
            bd = BaseDatos.crear(this),
        )
    }

    val ubicacion: FuenteUbicacion by lazy { FuenteUbicacion(this) }

    override fun onCreate() {
        super.onCreate()
        iniciarReporteDeFallos()
    }

    /**
     * Sentry solo se activa si se configuró un DSN (local.properties `sentry.dsn` o variable SENTRY_DSN).
     * Envía el error y el modelo de teléfono; nunca datos personales (sendDefaultPii viene apagado),
     * capturas ni ubicación.
     */
    private fun iniciarReporteDeFallos() {
        if (BuildConfig.SENTRY_DSN.isBlank()) return
        SentryAndroid.init(this) { o ->
            o.dsn = BuildConfig.SENTRY_DSN
            o.environment = if (BuildConfig.DEBUG) "desarrollo" else "produccion"
            o.release = "conecta-pty@${BuildConfig.VERSION_NAME}"
            o.isAttachScreenshot = false
            o.isAttachViewHierarchy = false
            o.tracesSampleRate = 0.0 // solo fallos, sin medir rendimiento (menos datos móviles)
        }
    }
}
