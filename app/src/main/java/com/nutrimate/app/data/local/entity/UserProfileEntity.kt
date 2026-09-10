package com.nutrimate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nutrimate.app.domain.model.ActivityLevel
import com.nutrimate.app.domain.model.Allergen
import com.nutrimate.app.domain.model.Gender
import com.nutrimate.app.domain.model.Goal
import com.nutrimate.app.domain.model.TastePreference
import com.nutrimate.app.domain.model.UserProfile

/** Single-row profile table (id always = 1). */
@Entity(tableName = "profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val gender: String,
    val birthdayEpochDay: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val activityLevel: String,
    val tastePreferences: String, // comma-separated enum names
    val allergens: String,        // comma-separated enum names
    val caloriesBudget: Int,
    val proteinGram: Int,
    val carbGram: Int,
    val fatGram: Int,
    val lastRecalcAt: Long,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): UserProfile = UserProfile(
        gender = Gender.valueOf(gender),
        birthdayEpochDay = birthdayEpochDay,
        heightCm = heightCm,
        weightKg = weightKg,
        goal = Goal.valueOf(goal),
        activityLevel = ActivityLevel.valueOf(activityLevel),
        tastePreferences = if (tastePreferences.isBlank()) emptySet()
        else tastePreferences.split(',').map { TastePreference.valueOf(it) }.toSet(),
        allergens = if (allergens.isBlank()) emptySet()
        else allergens.split(',').map { Allergen.valueOf(it) }.toSet(),
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt
    )

    companion object {
        fun fromDomain(p: UserProfile): UserProfileEntity = UserProfileEntity(
            gender = p.gender.name,
            birthdayEpochDay = p.birthdayEpochDay,
            heightCm = p.heightCm,
            weightKg = p.weightKg,
            goal = p.goal.name,
            activityLevel = p.activityLevel.name,
            tastePreferences = p.tastePreferences.joinToString(",") { it.name },
            allergens = p.allergens.joinToString(",") { it.name },
            caloriesBudget = 0, // recomputed on save by the repository
            proteinGram = 0,
            carbGram = 0,
            fatGram = 0,
            lastRecalcAt = 0L,
            createdAt = p.createdAtEpochMillis,
            updatedAt = p.updatedAtEpochMillis
        )
    }
}