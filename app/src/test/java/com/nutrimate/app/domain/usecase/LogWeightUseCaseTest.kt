package com.nutrimate.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Weight logging business rules: value range guard + delegation to repository. */
class LogWeightUseCaseTest {

    private val repo = com.nutrimate.app.domain.repository.WeightRepositoryStub()

    private val clock = object : com.nutrimate.app.domain.time.DayClock {
        override fun todayEpochDay(): Long = 20000L
        override fun currentHour(): Int = 8
        override fun nowEpochMillis(): Long = 1L
    }

    private val useCase = LogWeightUseCase(repo, clock)

    @Test
    fun `accepted weight range records successfully`() = runTest {
        assertThat(useCase.recordToday(70.5)).isTrue()
    }

    @Test
    fun `out of range weight is rejected`() = runTest {
        assertThat(useCase.recordToday(29.9)).isFalse()
        assertThat(useCase.recordToday(300.1)).isFalse()
        assertThat(useCase.recordToday(-1.0)).isFalse()
    }
}