package manoellribeiro.dev.martp.core.data.local.daos

import androidx.room.Dao
import androidx.room.Query
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity

@Dao
interface ColorPalettesDao {

    @Query("""
        SELECT * FROM color_palettes ORDER BY position ASC
    """)
    suspend fun getAll(): List<ColorPaletteEntity>

}
