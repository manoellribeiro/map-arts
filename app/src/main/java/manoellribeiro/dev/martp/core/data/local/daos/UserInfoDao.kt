package manoellribeiro.dev.martp.core.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import manoellribeiro.dev.martp.core.data.local.entities.UserInfoEntity

@Dao
interface UserInfoDao {

    @Insert(entity = UserInfoEntity::class, onConflict = OnConflictStrategy.REPLACE)
    fun insert(userInfoEntity: UserInfoEntity)

    @Query("""
        SELECT * FROM user_info LIMIT 1
    """)
    fun getUser(): UserInfoEntity?

    @Query("""
        UPDATE user_info
        SET username = :username
        WHERE id = :id;
    """)
    fun setUserName(id: String, username: String)

    @Query("""
        UPDATE user_info
        SET email = :email
        WHERE id = :id;
    """)
    fun setUserEmail(id: String, email: String)

}