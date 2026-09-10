package com.nutrimate.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nutrimate.app.data.local.entity.GroceryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroceryDao {

    @Query("SELECT * FROM grocery_items ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<GroceryItemEntity>>

    @Query("SELECT * FROM grocery_items")
    suspend fun getAll(): List<GroceryItemEntity>

    @Insert
    suspend fun insert(item: GroceryItemEntity): Long

    @Update
    suspend fun update(item: GroceryItemEntity)

    @Query("DELETE FROM grocery_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM grocery_items")
    suspend fun deleteAll()
}