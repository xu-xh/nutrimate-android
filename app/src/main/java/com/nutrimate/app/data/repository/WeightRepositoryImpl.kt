package com.nutrimate.app.data.repository

import com.nutrimate.app.data.local.dao.WeightDao
import com.nutrimate.app.data.local.entity.WeightLogEntity
import com.nutrimate.app.domain.model.WeightLogEntry
import com.nutrimate.app.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val dao: WeightDao
) : WeightRepository {

    override fun observeLatestFirst(): Flow<List<WeightLogEntry>> =
        dao.observeLatestFirst().map { list -> list.map { it.toDomain() } }

    override suspend fun getRange(fromDay: Long, toDay: Long): List<WeightLogEntry> =
        dao.getRange(fromDay, toDay).map { it.toDomain() }

    override suspend fun recordWeight(dateEpochDay: Long, weightKg: Double) {
        val existing = dao.getByDay(dateEpochDay)
        val entity = WeightLogEntity.fromDomain(
            WeightLogEntry(
                id = existing?.id ?: 0L,
                dateEpochDay = dateEpochDay,
                weightKg = weightKg,
                createdAtEpochMillis = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
        dao.insert(entity)
    }

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}