package com.example.widgetcatchingup.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.widgetcatchingup.data.local.AppDatabase
import com.example.widgetcatchingup.data.local.Goal
import com.example.widgetcatchingup.data.local.GoalLog
import com.example.widgetcatchingup.data.repository.GoalRepository
import com.example.widgetcatchingup.data.repository.SingleGoalMonthlyStats
import com.example.widgetcatchingup.data.updater.GitHubUpdateChecker
import com.example.widgetcatchingup.data.updater.UpdateInfo
import com.example.widgetcatchingup.widget.GoalWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = GoalRepository(db.goalDao())

    val goals: StateFlow<List<Goal>> = repository.allGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<GoalLog>> = repository.getLogsForCurrentWeek().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val weekDates: List<LocalDate> = repository.getCurrentWeekDates()

    // --- ESTADO NAVEGACIÓN Y PROGRESO MENSUAL ---
    private val _selectedNavIndex = MutableStateFlow(0)
    val selectedNavIndex: StateFlow<Int> = _selectedNavIndex.asStateFlow()

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    val selectedYearMonth: StateFlow<YearMonth> = _selectedYearMonth.asStateFlow()

    private val _selectedGoalId = MutableStateFlow<Long?>(null)
    val selectedGoalId: StateFlow<Long?> = _selectedGoalId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyLogsForSelectedGoal: StateFlow<List<GoalLog>> = combine(
        _selectedGoalId,
        _selectedYearMonth
    ) { goalId, yearMonth ->
        goalId to yearMonth
    }.flatMapLatest { (goalId, yearMonth) ->
        if (goalId != null) {
            repository.getLogsForGoalAndMonth(goalId, yearMonth)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    init {
        checkForUpdates()
        // Seleccionar automáticamente la primera meta si existe
        viewModelScope.launch {
            goals.collect { list ->
                if (_selectedGoalId.value == null && list.isNotEmpty()) {
                    _selectedGoalId.value = list.first().id
                }
            }
        }
    }

    fun setSelectedNavIndex(index: Int) {
        _selectedNavIndex.value = index
    }

    fun setSelectedGoalId(goalId: Long) {
        _selectedGoalId.value = goalId
    }

    fun previousMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedYearMonth.value = _selectedYearMonth.value.plusMonths(1)
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            val info = GitHubUpdateChecker.checkLatestRelease()
            if (info.hasUpdate) {
                _updateInfo.value = info
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }

    fun downloadUpdate(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addGoal(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addGoal(title.trim())
            refreshWidget()
        }
    }

    fun updateGoalTitle(goalId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.updateGoalTitle(goalId, newTitle.trim())
            refreshWidget()
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            refreshWidget()
        }
    }

    fun moveGoalUp(index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            repository.moveGoal(index, index - 1)
            refreshWidget()
        }
    }

    fun moveGoalDown(index: Int) {
        viewModelScope.launch {
            val currentList = goals.value
            if (index >= currentList.size - 1) return@launch
            repository.moveGoal(index, index + 1)
            refreshWidget()
        }
    }

    fun toggleLog(goalId: Long, date: LocalDate, currentState: Boolean) {
        viewModelScope.launch {
            repository.toggleGoalLog(goalId, date, currentState)
            refreshWidget()
        }
    }

    private suspend fun refreshWidget() {
        try {
            GoalWidget().updateAll(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
