package manoellribeiro.dev.martp.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_arts")
data class MapArtEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val latitude: Float,
    val longitude: Float,
    val dateInMillis: Long,
    val imagePathLocation: String
)