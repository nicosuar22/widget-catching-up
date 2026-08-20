package com.example.widgetcatchingup.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY position ASC, id ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY position ASC, id ASC")
    suspend fun getAllGoalsDirect(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goal_logs WHERE goalId = :goalId")
    suspend fun deleteLogsForGoal(goalId: Long)

    @Transaction
    suspend fun deleteGoalWithLogs(goal: Goal) {
        deleteLogsForGoal(goal.id)
        deleteGoal(goal)
    }

    @Update
    suspend fun updateGoals(goals: List<Goal>)

    @Query("SELECT * FROM goal_logs WHERE date >= :startDate AND date <= :endDate")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<GoalLog>>

    @Query("SELECT * FROM goal_logs WHERE date >= :startDate AND date <= :endDate")
    suspend fun getLogsBetweenDatesDirect(startDate: String, endDate: String): List<GoalLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLog(log: GoalLog)

    @Query("SELECT * FROM goal_logs WHERE date LIKE :monthPrefix || '%' AND isCompleted = 1")
    suspend fun getCompletedLogsForMonth(monthPrefix: String): List<GoalLog>
}
