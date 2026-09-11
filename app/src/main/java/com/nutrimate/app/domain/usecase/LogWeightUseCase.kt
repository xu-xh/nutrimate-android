package com.nutrimate.app.domain.usecase

import com.nutrimate.app.domain.repository.WeightRepository
import com.nutrimate.app.domain.time.DayClock
import javax.inject.Inject

/**
 * Records today's body weight. If the same day already has a value it is
 * overwritten (idempotent daily logging).
 */
class LogWeightUseCase @Inject constructor(
    private val weightRepository: WeightRepository,
    private val clock: DayClock
) {

    /** @return false when the value is out of the accepted range (30–300 kg). */
    suspend fun recordToday(weightKg: Double): Boolean {
        if (weightKg < 30.0 || weightKg > 300.0) return false
        weightRepository.recordWeight(clock.todayEpochDay(), weightKg)
        return true
    }
}