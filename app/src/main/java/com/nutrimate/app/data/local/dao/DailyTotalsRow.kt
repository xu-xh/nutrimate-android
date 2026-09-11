package com.nutrimate.app.data.local.dao

import androidx.room.ColumnInfo

/** Result row of [FoodLogDao.getDailyTotals] (per-day aggregates). */
data class DailyTotalsRow(
    val dateEpochDay: Long,
    @ColumnInfo(name = "calories") val calories: Double?,
    @ColumnInfo(name = "protein") val protein: Double?,
    @ColumnInfo(name = "carbs") val carbs: Double?,
    @ColumnInfo(name = "fat") val fat: Double?
)