package manoellribeiro.dev.martp.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import manoellribeiro.dev.martp.core.data.local.converters.ColorsConverter
import manoellribeiro.dev.martp.core.data.local.daos.ColorPalettesDao
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import manoellribeiro.dev.martp.core.data.local.entities.ColorPaletteEntity
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity

@Database(
    entities = [
        MapArtEntity::class,
        UserInfoEntity::class,
        ColorPaletteEntity::class
    ],
    version = 4
)
@TypeConverters(ColorsConverter::class)
abstract class MartpDatabase: RoomDatabase() {
    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_info` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `username` TEXT DEFAULT NULL,
                            `email` TEXT DEFAULT NULL
                        ) 
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `sketchArtType` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `colorPalette` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `map_layer_roadsColor` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `map_layer_buildingStrokeColor` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `map_layer_buildingColor` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `map_layer_roadsPathColor` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `map_layer_waterColor` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `map_arts` ADD COLUMN `map_layer_landOthersColor` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `color_palettes` (
                            `name` TEXT NOT NULL PRIMARY KEY,
                            `colors` TEXT NOT NULL,
                            `position` INTEGER NOT NULL
                        )
                """)
            }
        }

        const val databaseName = "martp-database.db"
        const val artsDirectoryName = "/map-arts/"
    }

    abstract fun mapArtDao(): MapArtsDao
    abstract fun userEntityDao(): UserInfoDao
    abstract fun colorPalettesDao(): ColorPalettesDao

}