package com.nutrimate.app.domain.repository

import com.nutrimate.app.domain.model.WeightLogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Body-weight tracking (P1 "体重追踪").
 * One row per day: recording again on the same day overwrites that day's value.
 */
interface WeightRepository {

    /** All records, newest first (UI lists). */
    fun observeLatestFirst(): Flow<List<WeightLogEntry>>

    /** Records within [fromDay]..[toDay] for trend charts (one per day). */
    suspend fun getRange(fromDay: Long, toDay: Long): List<WeightLogEntry>

    /** Insert, or update the existing record of the same day when present. */
    suspend fun recordWeight(dateEpochDay: Long, weightKg: Double)

    suspend fun delete(id: Long)
}