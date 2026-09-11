package com.nutrimate.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nutrimate.app.data.local.dao.FoodLogDao
import com.nutrimate.app.data.local.dao.GroceryDao
import com.nutrimate.app.data.local.dao.ProfileDao
import com.nutrimate.app.data.local.dao.RecipeDao
import com.nutrimate.app.data.local.dao.WeightDao
import com.nutrimate.app.data.local.entity.FoodLogEntity
import com.nutrimate.app.data.local.entity.GroceryItemEntity
import com.nutrimate.app.data.local.entity.RecipeLogEntity
import com.nutrimate.app.data.local.entity.UserProfileEntity
import com.nutrimate.app.data.local.entity.WeightLogEntity

@Database(
    entities = [
        UserProfileEntity::class,
        FoodLogEntity::class,
        RecipeLogEntity::class,
        GroceryItemEntity::class,
        WeightLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class NutrimateDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun recipeDao(): RecipeDao
    abstract fun groceryDao(): GroceryDao
    abstract fun weightDao(): WeightDao

    companion object {
        /** v1 -> v2: add weight_logs table (one row per day, unique). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weight_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dateEpochDay INTEGER NOT NULL,
                        weightKg REAL NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_weight_logs_dateEpochDay ON weight_logs (dateEpochDay)"
                )
            }
        }
    }
}