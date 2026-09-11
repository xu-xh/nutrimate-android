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

    /** Daily calorie/protein/carb/fat totals (non-deleted) over a date range. */
    @Query(
        """
        SELECT dateEpochDay,
               SUM(calories) AS calories,
               SUM(protein) AS protein,
               SUM(carbs) AS carbs,
               SUM(fat) AS fat
        FROM food_logs
        WHERE dateEpochDay BETWEEN :fromDay AND :toDay AND deleted = 0
        GROUP BY dateEpochDay
        """
    )
    suspend fun getDailyTotals(fromDay: Long, toDay: Long): List<DailyTotalsRow>

    @Insert
    suspend fun insert(entry: FoodLogEntity): Long

    @Update
    suspend fun update(entry: FoodLogEntity)

    @Query("UPDATE food_logs SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: Long, updatedAt: Long)

    @Query("DELETE FROM food_logs")
    suspend fun deleteAll()
}