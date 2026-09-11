package com.nutrimate.app.domain.model

/** A single body-weight measurement for a day. */
data class WeightLogEntry(
    val id: Long = 0L,
    val dateEpochDay: Long,
    val weightKg: Double,
    val createdAtEpochMillis: Long
)