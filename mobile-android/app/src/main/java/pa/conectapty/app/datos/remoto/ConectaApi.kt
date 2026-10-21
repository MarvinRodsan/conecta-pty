package pa.conectapty.app.datos.remoto

import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
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
    ): PlanDto

    companion object {
        fun crear(urlBase: String, dirCache: File): ConectaApi {
            val cliente = OkHttpClient.Builder()
                // Caché HTTP de 5 MB: respeta el Cache-Control del servidor (catálogo = 1 h)
                .cache(Cache(File(dirCache, "http"), 5L * 1024 * 1024))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
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
