package manoellribeiro.dev.martp.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_arts")
data class MapArtEntity(
    @PrimaryKey
    private val id: String,
    private val title: String,
    private val description: String,
    private val latitude: Float,
    private val longitude: Float,
    private val dateInMillis: Long,
    private val imagePathLocation: String
)