package manoellribeiro.dev.martp.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.entities.MapArtEntity

@Database(
    entities = [
        MapArtEntity::class
    ],
    version = 1
)
abstract class MartpDatabase: RoomDatabase() {

    companion object {
        const val databaseName = "martp-database.db"
        const val artsDirectoryName = "/map-arts/"
    }

    abstract fun mapArtDao(): MapArtsDao

}