package manoellribeiro.dev.martp.core.data.local.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity

@Dao
interface MapArtsDao {

    @Query("""
        SELECT * FROM map_arts
    """)
    suspend fun getAll(): List<MapArtEntity>

    @Query("""
        DELETE FROM map_arts
        WHERE id = :id
    """)
    suspend fun deleteById(id: String)

    @Insert(entity = MapArtEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapArt: MapArtEntity)

}