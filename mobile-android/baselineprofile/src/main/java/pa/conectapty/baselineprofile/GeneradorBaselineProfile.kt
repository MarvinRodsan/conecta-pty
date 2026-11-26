package pa.conectapty.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

internal const val PAQUETE_APP = "pa.conectapty.app"

/**
 * Recorre el camino más común (abrir la app y elegir una estación) para que Android
 * precompile ese código al instalar. Resultado: app/src/release/generated/baselineProfiles.
 *
 *   ./gradlew :app:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class GeneradorBaselineProfile {

    @get:Rule
    val regla = BaselineProfileRule()

    @Test
    fun generar() = regla.collect(packageName = PAQUETE_APP, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Buscar ruta")), 5_000)

        // Abrir el selector de estaciones y volver: es la primera pantalla que usa casi todo el mundo
        device.findObject(By.textContains("Elige una estación"))?.let {
            it.click()
            device.wait(Until.hasObject(By.text("Busca una estación o lugar")), 3_000)
            device.pressBack()
        }
    }
}
