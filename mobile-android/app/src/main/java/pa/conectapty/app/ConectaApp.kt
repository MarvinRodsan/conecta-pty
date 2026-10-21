package pa.conectapty.app

import android.app.Application
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
}
