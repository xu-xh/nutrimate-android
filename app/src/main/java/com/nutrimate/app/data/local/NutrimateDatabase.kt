package com.nutrimate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nutrimate.app.data.local.dao.FoodLogDao
import com.nutrimate.app.data.local.dao.GroceryDao
import com.nutrimate.app.data.local.dao.ProfileDao
import com.nutrimate.app.data.local.dao.RecipeDao
import com.nutrimate.app.data.local.entity.FoodLogEntity
import com.nutrimate.app.data.local.entity.GroceryItemEntity
import com.nutrimate.app.data.local.entity.RecipeLogEntity
import com.nutrimate.app.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        FoodLogEntity::class,
        RecipeLogEntity::class,
        GroceryItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NutrimateDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun recipeDao(): RecipeDao
    abstract fun groceryDao(): GroceryDao
}