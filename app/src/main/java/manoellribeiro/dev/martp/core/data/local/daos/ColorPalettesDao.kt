package manoellribeiro.dev.martp.core.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity

@Dao
interface ColorPalettesDao {

    @Query("""
        SELECT * FROM color_palettes
    """)
    suspend fun getAll(): List<ColorPaletteEntity>

    @Insert(entity = ColorPaletteEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(palettes: List<ColorPaletteEntity>)

    @Query("""
        DELETE FROM color_palettes
    """)
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(palettes: List<ColorPaletteEntity>) {
        deleteAll()
        insertAll(palettes)
    }

    @Query("""
        SELECT * FROM color_palettes where name = :name
    """)
    suspend fun getByName(name: String): ColorPaletteEntity?

}
