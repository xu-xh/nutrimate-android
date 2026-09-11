package com.nutrimate.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nutrimate.app.domain.model.WeightLogEntry

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dateEpochDay: Long,
    val weightKg: Double,
    val createdAt: Long
) {
    fun toDomain(): WeightLogEntry = WeightLogEntry(
        id = id,
        dateEpochDay = dateEpochDay,
        weightKg = weightKg,
        createdAtEpochMillis = createdAt
    )

    companion object {
        fun fromDomain(e: WeightLogEntry): WeightLogEntity = WeightLogEntity(
            id = e.id,
            dateEpochDay = e.dateEpochDay,
            weightKg = e.weightKg,
            createdAt = e.createdAtEpochMillis
        )
    }
}