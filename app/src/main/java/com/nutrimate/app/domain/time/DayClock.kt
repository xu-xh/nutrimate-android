package com.nutrimate.app.domain.time

import kotlinx.coroutines.flow.Flow

/**
 * Injectable clock so date/day boundaries are testable.
 * All domain logic must obtain "today" through this provider.
 */
interface DayClock {

    /** Today's local date as epoch day. */
    fun todayEpochDay(): Long

    /** Hours in the local day (0-23), used for meal-type inference. */
    fun currentHour(): Int

    /** Monotonic wall-clock millis for timestamps. */
    fun nowEpochMillis(): Long
}

/** Real implementation used in production. */
class SystemDayClock : DayClock {
    private val zone = java.time.ZoneId.systemDefault()

    override fun todayEpochDay(): Long = java.time.LocalDate.now(zone).toEpochDay()

    override fun currentHour(): Int = java.time.LocalTime.now(zone).hour

    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

/** Convenience alias: expose today's date + hour changes as a Flow for reactive UI. */
interface DayFlow {
    fun todayEpochDayFlow(): Flow<Long>
}