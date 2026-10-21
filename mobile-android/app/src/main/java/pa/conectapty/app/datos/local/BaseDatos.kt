package pa.conectapty.app.datos.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Copia local del catálogo de estaciones: la app funciona sin red para elegir origen/destino. */
@Entity(tableName = "parada")
data class ParadaEntidad(
    @PrimaryKey val id: String,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val accesible: String,
    val actualizadaEn: Long,
)

/** Favoritos: solo en el teléfono, sin cuenta (sección 10: privacidad por defecto). */
@Entity(tableName = "favorito")
data class FavoritoEntidad(
    @PrimaryKey val paradaId: String,
    val nombre: String,
    val lat: Double,
    val lon: Double,
    val creadoEn: Long,
)

@Dao
abstract class ParadaDao {
    @Query("SELECT * FROM parada ORDER BY nombre")
    abstract fun observar(): Flow<List<ParadaEntidad>>

    @Query("SELECT MAX(actualizadaEn) FROM parada")
    abstract suspend fun ultimaActualizacion(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertar(paradas: List<ParadaEntidad>)

    @Query("DELETE FROM parada")
    abstract suspend fun borrarTodo()

    /** Reemplazo atómico: nunca se ve un catálogo a medias. */
    @Transaction
    open suspend fun reemplazar(paradas: List<ParadaEntidad>) {
        borrarTodo()
        insertar(paradas)
    }
}

@Dao
interface FavoritoDao {
    @Query("SELECT * FROM favorito ORDER BY creadoEn DESC")
    fun observar(): Flow<List<FavoritoEntidad>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(f: FavoritoEntidad)

    @Query("DELETE FROM favorito WHERE paradaId = :paradaId")
    suspend fun borrar(paradaId: String)
}

@Database(entities = [ParadaEntidad::class, FavoritoEntidad::class], version = 1, exportSchema = false)
abstract class BaseDatos : RoomDatabase() {
    abstract fun paradas(): ParadaDao
    abstract fun favoritos(): FavoritoDao

    companion object {
        fun crear(context: Context): BaseDatos =
            Room.databaseBuilder(context, BaseDatos::class.java, "conecta.db").build()
    }
}
