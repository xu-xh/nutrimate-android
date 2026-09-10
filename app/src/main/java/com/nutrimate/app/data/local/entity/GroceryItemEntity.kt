package com.nutrimate.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nutrimate.app.domain.model.GroceryItem

@Entity(tableName = "grocery_items")
data class GroceryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val amount: Double,
    val unit: String,
    val checked: Boolean = false,
    val createdAt: Long
) {
    fun toDomain(): GroceryItem = GroceryItem(
        id = id,
        name = name,
        amount = amount,
        unit = unit,
        checked = checked,
        createdAtEpochMillis = createdAt
    )

    companion object {
        fun fromDomain(g: GroceryItem): GroceryItemEntity = GroceryItemEntity(
            id = g.id,
            name = g.name,
            amount = g.amount,
            unit = g.unit,
            checked = g.checked,
            createdAt = g.createdAtEpochMillis
        )
    }
}