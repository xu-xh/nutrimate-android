package com.nutrimate.app.data.repository

import com.nutrimate.app.data.local.dao.RecipeDao
import com.nutrimate.app.data.local.entity.RecipeLogEntity
import com.nutrimate.app.domain.model.RecipeRecommendation
import com.nutrimate.app.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val dao: RecipeDao
) : RecipeRepository {

    override fun observeByDate(dateEpochDay: Long): Flow<List<RecipeRecommendation>> =
        dao.observeByDate(dateEpochDay).map { list -> list.map { it.toDomain() } }

    override suspend fun insert(recipe: RecipeRecommendation): Long =
        dao.insert(RecipeLogEntity.fromDomain(recipe))
}