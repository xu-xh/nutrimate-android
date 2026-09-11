package com.nutrimate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nutrimate.app.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_logs ORDER BY dateEpochDay DESC")
    fun observeLatestFirst(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE dateEpochDay BETWEEN :fromDay AND :toDay ORDER BY dateEpochDay ASC")
    suspend fun getRange(fromDay: Long, toDay: Long): List<WeightLogEntity>

    @Query("SELECT * FROM weight_logs WHERE dateEpochDay = :day LIMIT 1")
    suspend fun getByDay(day: Long): WeightLogEntity?

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WeightLogEntity): Long

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteById(id: Long)
}