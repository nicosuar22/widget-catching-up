package com.example.widgetcatchingup.data.repository

import com.example.widgetcatchingup.data.local.Goal
import com.example.widgetcatchingup.data.local.GoalDao
import com.example.widgetcatchingup.data.local.GoalLog
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class MonthlyStreakStats(
    val bestStreak: Int,
    val worstStreak: Int
)

data class SingleGoalMonthlyStats(
    val completedDaysCount: Int,
    val totalDaysInMonth: Int,
    val percentage: Int,
    val maxStreak: Int
)

class GoalRepository(private val goalDao: GoalDao) {

    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    fun getCurrentWeekDates(): List<LocalDate> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun getLogsForCurrentWeek(): Flow<List<GoalLog>> {
        val weekDates = getCurrentWeekDates()
        val startDate = weekDates.first().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDate = weekDates.last().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return goalDao.getLogsBetweenDates(startDate, endDate)
    }

    suspend fun getLogsForCurrentWeekDirect(): List<GoalLog> {
        val weekDates = getCurrentWeekDates()
        val startDate = weekDates.first().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDate = weekDates.last().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return goalDao.getLogsBetweenDatesDirect(startDate, endDate)
    }

    fun getLogsForGoalAndMonth(goalId: Long, yearMonth: YearMonth): Flow<List<GoalLog>> {
        val monthPrefix = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return goalDao.getLogsForGoalAndMonth(goalId, monthPrefix)
    }

    suspend fun toggleGoalLog(goalId: Long, date: LocalDate, currentState: Boolean) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val newLog = GoalLog(
            goalId = goalId,
            date = dateString,
            isCompleted = !currentState
        )
        goalDao.insertOrUpdateLog(newLog)
    }

    suspend fun addGoal(title: String) {
        val currentGoals = goalDao.getAllGoalsDirect()
        val nextPosition = if (currentGoals.isEmpty()) 0 else currentGoals.maxOf { it.position } + 1
        val goal = Goal(title = title, position = nextPosition)
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoalTitle(goalId: Long, newTitle: String) {
        val currentGoals = goalDao.getAllGoalsDirect()
        val existing = currentGoals.find { it.id == goalId } ?: return
        goalDao.updateGoal(existing.copy(title = newTitle))
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoalWithLogs(goal)
    }

    suspend fun moveGoal(fromIndex: Int, toIndex: Int) {
        val goals = goalDao.getAllGoalsDirect().toMutableList()
        if (fromIndex in goals.indices && toIndex in goals.indices) {
            val movedItem = goals.removeAt(fromIndex)
            goals.add(toIndex, movedItem)
            val reordered = goals.mapIndexed { index, goal ->
                goal.copy(position = index)
            }
            goalDao.updateGoals(reordered)
        }
    }

    suspend fun calculateMonthlyStreakStats(): MonthlyStreakStats {
        val today = LocalDate.now()
        val monthPrefix = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val goals = goalDao.getAllGoalsDirect()

        if (goals.isEmpty()) {
            return MonthlyStreakStats(bestStreak = 0, worstStreak = 0)
        }

        val completedLogs = goalDao.getCompletedLogsForMonth(monthPrefix)
        
        // Conteo de días completados en el mes por cada meta
        val countsByGoal = goals.associate { goal ->
            goal.id to completedLogs.count { it.goalId == goal.id }
        }

        val best = countsByGoal.values.maxOrNull() ?: 0
        val worst = countsByGoal.values.minOrNull() ?: 0

        return MonthlyStreakStats(bestStreak = best, worstStreak = worst)
    }

    fun calculateSingleGoalStats(
        yearMonth: YearMonth,
        logs: List<GoalLog>
    ): SingleGoalMonthlyStats {
        val totalDays = yearMonth.lengthOfMonth()
        val completedDays = logs.filter { it.isCompleted }.map { LocalDate.parse(it.date).dayOfMonth }.toSet()
        val count = completedDays.size
        val percentage = if (totalDays > 0) (count * 100) / totalDays else 0

        // Calcular racha consecutiva máxima en el mes
        var maxStreak = 0
        var currentStreak = 0
        for (day in 1..totalDays) {
            if (completedDays.contains(day)) {
                currentStreak++
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak
                }
            } else {
                currentStreak = 0
            }
        }

        return SingleGoalMonthlyStats(
            completedDaysCount = count,
            totalDaysInMonth = totalDays,
            percentage = percentage,
            maxStreak = maxStreak
        )
    }
}
