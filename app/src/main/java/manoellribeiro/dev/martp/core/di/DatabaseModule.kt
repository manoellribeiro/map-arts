package manoellribeiro.dev.martp.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import manoellribeiro.dev.martp.core.data.local.MartpDatabase
import manoellribeiro.dev.martp.core.data.local.daos.MapArtsDao
import manoellribeiro.dev.martp.core.data.local.daos.UserInfoDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun providesMapArtsDao(martpDatabase: MartpDatabase): MapArtsDao {
        return martpDatabase.mapArtDao()
    }

    @Provides
    fun providesUserInfoEntity(martpDatabase: MartpDatabase): UserInfoDao {
        return martpDatabase.userEntityDao()
    }

    @Provides
    @Singleton
    fun providesMartpRoomDatabase(
        @ApplicationContext applicationContext: Context
    ): MartpDatabase {
        return Room.databaseBuilder(
            applicationContext,
            MartpDatabase::class.java, MartpDatabase.databaseName
        ).addMigrations(MartpDatabase.MIGRATION_1_2).build()
    }

}