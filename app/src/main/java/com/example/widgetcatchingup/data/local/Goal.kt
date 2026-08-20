package com.example.widgetcatchingup.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
