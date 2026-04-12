package manoellribeiro.dev.martp.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_info")
data class UserInfoEntity (
    @PrimaryKey(autoGenerate = false)
    val id: String,
    val username: String?,
    val email: String?
)

