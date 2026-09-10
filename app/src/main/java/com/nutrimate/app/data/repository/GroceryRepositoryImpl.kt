package com.nutrimate.app.data.repository

import com.nutrimate.app.data.local.dao.GroceryDao
import com.nutrimate.app.data.local.entity.GroceryItemEntity
import com.nutrimate.app.domain.model.GroceryItem
import com.nutrimate.app.domain.model.Ingredient
import com.nutrimate.app.domain.repository.GroceryRepository
import com.nutrimate.app.domain.time.DayClock
import com.nutrimate.app.domain.usecase.AddIngredientsToGroceryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroceryRepositoryImpl @Inject constructor(
    private val dao: GroceryDao,
    private val clock: DayClock
) : GroceryRepository {

    override fun observeAll(): Flow<List<GroceryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addIngredients(ingredients: List<Ingredient>): List<GroceryItem> {
        val existing = dao.getAll().map { it.toDomain() }
        val merged = AddIngredientsToGroceryUseCase.merge(existing, ingredients)
        return merged.map { item ->
            // match existing rows by the same (normalized name, unit) key so
            // distinct-unit rows never collide on id
            val existingId = existing.firstOrNull {
                AddIngredientsToGroceryUseCase.normalize(it.name) ==
                    AddIngredientsToGroceryUseCase.normalize(item.name) &&
                    it.unit.trim().lowercase() == item.unit.trim().lowercase()
            }?.id
            val entity = GroceryItemEntity.fromDomain(item).copy(
                id = existingId ?: 0L,
                createdAt = if (existingId != null) 0L else clock.nowEpochMillis()
            )
            if (existingId != null) dao.update(entity) else dao.insert(entity)
            item.copy(id = if (existingId != null) existingId else entity.id)
        }
    }

    override suspend fun setChecked(id: Long, checked: Boolean) {
        val item = dao.getAll().firstOrNull { it.id == id } ?: return
        dao.update(item.copy(checked = checked))
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearAll() {
        dao.deleteAll()
    }
}