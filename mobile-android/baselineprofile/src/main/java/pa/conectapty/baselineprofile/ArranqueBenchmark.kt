package pa.conectapty.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mide el arranque en frío con y sin Baseline Profile, en vez de suponer la mejora.
 * Anota los resultados (timeToInitialDisplayMs) en documentation/eficiencia.md.
 *
 *   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ArranqueBenchmark {

    @get:Rule
    val regla = MacrobenchmarkRule()

    @Test
    fun sinPrecompilar() = arrancar(CompilationMode.None())

    @Test
    fun conBaselineProfile() = arrancar(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun arrancar(modo: CompilationMode) = regla.measureRepeated(
        packageName = PAQUETE_APP,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = modo,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }
}
