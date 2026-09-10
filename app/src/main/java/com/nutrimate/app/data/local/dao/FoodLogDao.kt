package com.nutrimate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nutrimate.app.data.local.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Query("SELECT * FROM food_logs WHERE dateEpochDay = :dateEpochDay")
    fun observeByDate(dateEpochDay: Long): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE dateEpochDay = :dateEpochDay")
    suspend fun getByDate(dateEpochDay: Long): List<FoodLogEntity>

    @Insert
    suspend fun insert(entry: FoodLogEntity): Long

    @Update
    suspend fun update(entry: FoodLogEntity)

    @Query("UPDATE food_logs SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("DELETE FROM food_logs")
    suspend fun deleteAll()
}