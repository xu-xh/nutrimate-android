package com.nutrimate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nutrimate.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profiles WHERE id = 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM profiles WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?

    @Insert
    suspend fun upsert(profile: UserProfileEntity)

    /** Recompute-only write: persists budget + macros + recalc timestamp. */
    @Query(
        """
        UPDATE profiles SET
            caloriesBudget = :budget,
            proteinGram = :protein,
            carbGram = :carb,
            fatGram = :fat,
            lastRecalcAt = :recalcAt
        WHERE id = 1
        """
    )
    suspend fun updatePlan(budget: Int, protein: Int, carb: Int, fat: Int, recalcAt: Long)

    @Query("DELETE FROM profiles WHERE id = 1")
    suspend fun delete()
}