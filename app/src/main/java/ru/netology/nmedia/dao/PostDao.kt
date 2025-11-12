package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {

    @Query(
        """
    SELECT * FROM PostEntity 
    WHERE isVisible = 1    
    ORDER BY 
        CASE 
            WHEN id <= 0 THEN 0   
            ELSE 1               
        END,
        id DESC
"""
    )
    fun getAll(): Flow<List<PostEntity>>

    @Query(
        """
    SELECT * FROM PostEntity
    ORDER BY 
        CASE 
            WHEN id <= 0 THEN 0   
            ELSE 1               
        END,
        id DESC
"""
    )
    fun getAllInvisibleAndVisible(): Flow<List<PostEntity>>

    @Query(
        """
            SELECT * FROM PostEntity 
            ORDER BY
                CASE
                    WHEN id <= 0 THEN 0
                    ELSE 1
                END,
                id DESC
            LIMIT :count
            """
    )
    suspend fun getLatest(count: Int): List<PostEntity>

    @Query(
        """
            SELECT * FROM PostEntity
            WHERE id < :id AND id >= 1
            ORDER BY
                CASE
                    WHEN id <= 0 THEN 0
                     ELSE 1
                END,
                id DESC
            LIMIT :count
            """
    )
    suspend fun getBefore(id: Long, count: Int): List<PostEntity>

    @Query(
        """
    SELECT * FROM PostEntity 
    WHERE id > :id
    ORDER BY 
        CASE 
            WHEN id <= 0 THEN 0   
            ELSE 1               
        END,
        id DESC
    LIMIT :count
    """
    )
    suspend fun getAfter(id: Long, count: Int): List<PostEntity>

    @Query("UPDATE PostEntity SET isVisible = 1")
    suspend fun setAllVisible()

    @Query("SELECT COUNT(*) FROM PostEntity")
    suspend fun count(): Int

    @Query("SELECT * FROM PostEntity WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Long): PostEntity?

    @Query("SELECT COUNT(*) == 0 FROM PostEntity")
    suspend fun isEmpty(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("UPDATE PostEntity SET content = :content WHERE id = :id")
    suspend fun updateContentById(id: Long, content: String)

    suspend fun save(post: PostEntity) =
        if (post.id == 0L) insert(post) else updateContentById(post.id, post.content)

    @Query(
        """
        UPDATE PostEntity SET
                likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
                WHERE id = :id;
    """
    )
    suspend fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query(
        """
        UPDATE PostEntity SET
                share = share + 1
                WHERE id = :id;
    """
    )
    suspend fun sharedById(id: Long)

}