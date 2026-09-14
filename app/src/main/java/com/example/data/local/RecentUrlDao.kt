package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RecentUrl
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentUrlDao {

    @Query("SELECT * FROM recent_urls ORDER BY lastPlayedTimestamp DESC LIMIT 30")
    fun getRecentUrls(): Flow<List<RecentUrl>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentUrl(recentUrl: RecentUrl)

    @Query("DELETE FROM recent_urls WHERE url = :url")
    suspend fun deleteRecentUrl(url: String)

    @Query("DELETE FROM recent_urls")
    suspend fun clearAllRecentUrls()
}
