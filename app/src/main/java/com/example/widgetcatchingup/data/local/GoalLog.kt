package com.example.widgetcatchingup.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_logs",
    indices = [
        Index(value = ["goalId", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class GoalLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val date: String, // Formato "YYYY-MM-DD"
    val isCompleted: Boolean = false
)
