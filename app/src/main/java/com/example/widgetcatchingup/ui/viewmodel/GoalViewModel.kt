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
import com.example.widgetcatchingup.data.updater.GitHubUpdateChecker
import com.example.widgetcatchingup.data.updater.UpdateInfo
import com.example.widgetcatchingup.widget.GoalWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = GoalRepository(db.goalDao())

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

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    init {
        checkForUpdates()
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
