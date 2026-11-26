package pa.conectapty.app.datos.remoto

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ConectaApi {

    @GET("api/v1/paradas")
    suspend fun paradas(): List<ParadaDto>

    @GET("api/v1/plan")
    suspend fun plan(
        @Query("desdeLat") desdeLat: Double,
        @Query("desdeLon") desdeLon: Double,
        @Query("hastaLat") hastaLat: Double,
        @Query("hastaLon") hastaLon: Double,
        /** Hora local de Panamá "2026-10-26T08:00"; null = ahora. */
        @Query("salida") salida: String? = null,
    ): PlanDto

    @GET("api/v1/lugares")
    suspend fun lugares(@Query("q") consulta: String, @Query("limite") limite: Int = 5): List<LugarDto>

    @GET("api/v1/salud")
    suspend fun salud(): SaludDto

    @POST("api/v1/reportes")
    suspend fun reportar(@Body reporte: NuevoReporteDto): ReporteRecibidoDto

    companion object {
        fun crear(urlBase: String, dirCache: File): ConectaApi {
            val cliente = OkHttpClient.Builder()
                // Caché HTTP de 5 MB: respeta el Cache-Control del servidor (catálogo = 1 h)
                .cache(Cache(File(dirCache, "http"), 5L * 1024 * 1024))
                .connectTimeout(5, TimeUnit.SECONDS)
                // Mayor que el timeout del backend hacia OTP (20 s): la app nunca se rinde antes que el servidor
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
            return Retrofit.Builder()
                .baseUrl(urlBase)
                .client(cliente)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ConectaApi::class.java)
        }
    }
}
