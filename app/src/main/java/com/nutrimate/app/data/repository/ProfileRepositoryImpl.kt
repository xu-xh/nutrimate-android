package com.nutrimate.app.data.repository

import com.nutrimate.app.data.local.dao.FoodLogDao
import com.nutrimate.app.data.local.dao.GroceryDao
import com.nutrimate.app.data.local.dao.ProfileDao
import com.nutrimate.app.data.local.dao.RecipeDao
import com.nutrimate.app.data.local.entity.UserProfileEntity
import com.nutrimate.app.data.local.preferences.PreferencesDataSource
import com.nutrimate.app.domain.model.NutritionPlan
import com.nutrimate.app.domain.model.UserProfile
import com.nutrimate.app.domain.repository.ProfileRepository
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.usecase.CalculateNutritionPlanUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val foodLogDao: FoodLogDao,
    private val recipeDao: RecipeDao,
    private val groceryDao: GroceryDao,
    private val preferences: PreferencesDataSource,
    private val calculatePlan: CalculateNutritionPlanUseCase,
    private val clock: DayClock,
) : ProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        profileDao.observeProfile().map { it?.toDomain() }

    override suspend fun saveProfile(profile: UserProfile) {
        val now = clock.nowEpochMillis()
        val plan = calculatePlan.execute(profile, clock.todayEpochDay())
        val entity = UserProfileEntity.fromDomain(profile).copy(
            caloriesBudget = plan.caloriesBudget,
            proteinGram = plan.proteinGram,
            carbGram = plan.carbGram,
            fatGram = plan.fatGram,
            lastRecalcAt = now,
            updatedAt = now
        )
        profileDao.upsert(entity)
    }

    override fun observeOnboardingDone(): Flow<Boolean> = preferences.observeOnboardingDone()

    override suspend fun setOnboardingDone(done: Boolean) {
        preferences.setOnboardingDone(done)
    }

    override suspend fun clearBusinessData() {
        foodLogDao.deleteAll()
        recipeDao.deleteAll()
        groceryDao.deleteAll()
    }
}