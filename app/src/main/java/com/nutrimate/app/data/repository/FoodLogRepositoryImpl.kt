package com.nutrimate.app.data.repository

import com.nutrimate.app.data.local.dao.FoodLogDao
import com.nutrimate.app.data.local.entity.FoodLogEntity
import com.nutrimate.app.domain.model.FoodLogEntry
import com.nutrimate.app.domain.repository.DailyTotalAggregate
import com.nutrimate.app.domain.repository.FoodLogRepository
import com.nutrimate.app.domain.time.DayClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodLogRepositoryImpl @Inject constructor(
    private val dao: FoodLogDao,
    private val clock: DayClock
) : FoodLogRepository {

    override fun observeByDate(dateEpochDay: Long): Flow<List<FoodLogEntry>> =
        dao.observeByDate(dateEpochDay).map { list -> list.map { it.toDomain() } }

    override suspend fun getByDate(dateEpochDay: Long): List<FoodLogEntry> =
        dao.getByDate(dateEpochDay).map { it.toDomain() }

    override suspend fun getDailyTotals(fromDay: Long, toDay: Long): List<DailyTotalAggregate> =
        dao.getDailyTotals(fromDay, toDay).map {
            DailyTotalAggregate(
                dateEpochDay = it.dateEpochDay,
                calories = it.calories ?: 0.0,
                protein = it.protein ?: 0.0,
                carbs = it.carbs ?: 0.0,
                fat = it.fat ?: 0.0
            )
        }

    override suspend fun insert(entry: FoodLogEntry): Long =
        dao.insert(FoodLogEntity.fromDomain(entry))

    override suspend fun update(entry: FoodLogEntry) {
        dao.update(FoodLogEntity.fromDomain(entry))
    }

    override suspend fun softDelete(id: Long) {
        dao.softDelete(id, clock.nowEpochMillis())
    }
}