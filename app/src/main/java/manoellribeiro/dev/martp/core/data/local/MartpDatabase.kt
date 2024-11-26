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
        lateinit var instance: MartpDatabase
            private set


        fun initialize(applicationContext: Context) {
            instance = Room.databaseBuilder(
                applicationContext,
                MartpDatabase::class.java, "martp-database.db"
            ).build()
        }

    }

    abstract fun mapArtDao(): MapArtsDao

}