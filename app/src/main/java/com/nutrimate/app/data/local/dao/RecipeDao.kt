package com.nutrimate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nutrimate.app.data.local.entity.RecipeLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipe_logs WHERE dateEpochDay = :dateEpochDay")
    fun observeByDate(dateEpochDay: Long): Flow<List<RecipeLogEntity>>

    @Insert
    suspend fun insert(recipe: RecipeLogEntity): Long

    @Query("DELETE FROM recipe_logs")
    suspend fun deleteAll()
}