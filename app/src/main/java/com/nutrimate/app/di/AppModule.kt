package com.nutrimate.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.nutrimate.app.data.ai.DefaultNutritionAiService
import com.nutrimate.app.data.local.NutrimateDatabase
import com.nutrimate.app.data.local.dao.FoodLogDao
import com.nutrimate.app.data.local.dao.GroceryDao
import com.nutrimate.app.data.local.dao.ProfileDao
import com.nutrimate.app.data.local.dao.RecipeDao
import com.nutrimate.app.data.local.dao.WeightDao
import com.nutrimate.app.data.local.preferences.PreferencesDataSource
import com.nutrimate.app.domain.repository.AiConfigRepository
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.repository.GroceryRepository
import com.nutrimate.app.domain.repository.NutritionAiService
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.repository.RecipeRepository
import com.nutrimate.app.domain.repository.WeightRepository
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.time.SystemDayClock
import com.nutrimate.app.domain.usecase.RecipeGenerationCounter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nutrimate_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NutrimateDatabase =
        Room.databaseBuilder(context, NutrimateDatabase::class.java, "nutrimate.db")
            .addMigrations(NutrimateDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideProfileDao(db: NutrimateDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideFoodLogDao(db: NutrimateDatabase): FoodLogDao = db.foodLogDao()

    @Provides
    fun provideRecipeDao(db: NutrimateDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun provideGroceryDao(db: NutrimateDatabase): GroceryDao = db.groceryDao()

    @Provides
    fun provideWeightDao(db: NutrimateDatabase): WeightDao = db.weightDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideDayClock(): DayClock = SystemDayClock()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun bindProfileRepository(impl: com.nutrimate.app.data.repository.ProfileRepositoryImpl): ProfileRepository = impl

    @Provides
    @Singleton
    fun bindFoodLogRepository(impl: com.nutrimate.app.data.repository.FoodLogRepositoryImpl): FoodLogRepository = impl

    @Provides
    @Singleton
    fun bindRecipeRepository(impl: com.nutrimate.app.data.repository.RecipeRepositoryImpl): RecipeRepository = impl

    @Provides
    @Singleton
    fun bindGroceryRepository(impl: com.nutrimate.app.data.repository.GroceryRepositoryImpl): GroceryRepository = impl

    @Provides
    @Singleton
    fun bindAiConfigRepository(impl: com.nutrimate.app.data.repository.AiConfigRepositoryImpl): AiConfigRepository = impl

    @Provides
    @Singleton
    fun bindNutritionAiService(impl: DefaultNutritionAiService): NutritionAiService = impl

    @Provides
    @Singleton
    fun bindGenerationCounter(impl: com.nutrimate.app.data.repository.RecipeGenerationCounterImpl): RecipeGenerationCounter = impl

    @Provides
    @Singleton
    fun bindWeightRepository(impl: com.nutrimate.app.data.repository.WeightRepositoryImpl): WeightRepository = impl

    @Provides
    fun providePreferencesDataSource(dataStore: DataStore<Preferences>): PreferencesDataSource =
        PreferencesDataSource(dataStore)
}