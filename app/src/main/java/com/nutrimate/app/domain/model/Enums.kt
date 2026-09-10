package com.nutrimate.app.domain.model

/** Supported biological genders for BMR calculation (v0.1). */
enum class Gender(val labelId: Int) {
    MALE(1),
    FEMALE(2)
}

/** Body composition goal. Drives calorie budget adjustment and macro split. */
enum class Goal(val adjustmentRatio: Double) {
    LOSE(-0.20),
    MAINTAIN(0.0),
    GAIN(0.10)
}

/**
 * Activity level with Mifflin-St Jeor activity multiplier.
 * Level 6 (2.0) corresponds to heavy labor + daily training.
 */
enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9),
    EXTREME(2.0)
}

/** Meal slots used for daily grouping and meal-type inference. */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;

    companion object {
        /**
         * Infer the default meal type from a local clock hour (0-23):
         *  5-9   -> BREAKFAST
         * 10-13 -> LUNCH
         * 14-20 -> DINNER
         * else  -> SNACK
         */
        fun fromHour(hour: Int): MealType = when (hour) {
            in 5..9 -> BREAKFAST
            in 10..13 -> LUNCH
            in 14..20 -> DINNER
            else -> SNACK
        }
    }
}

/** Supported food logging input sources. */
enum class LogSource {
    AI_PHOTO,
    MANUAL
}

/** Allergen / dietary restriction flags used to constrain recipe generation. */
enum class Allergen {
    PEANUT,
    DAIRY,
    SEAFOOD,
    GLUTEN,
    EGG,
    SOY
}

/** Taste preference tags used as recipe-generation hints. */
enum class TastePreference {
    LIGHT,
    HIGH_PROTEIN,
    LOW_CARB,
    SPICY,
    VEGETARIAN,
    QUICK
}