package com.example.widgetcatchingup.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.widgetcatchingup.data.local.AppDatabase
import com.example.widgetcatchingup.data.repository.GoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ToggleGoalAction : ActionCallback {

    companion object {
        val PARAM_GOAL_ID = ActionParameters.Key<Long>("param_goal_id")
        val PARAM_DATE_STRING = ActionParameters.Key<String>("param_date_string")
        val PARAM_CURRENT_STATE = ActionParameters.Key<Boolean>("param_current_state")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val goalId = parameters[PARAM_GOAL_ID] ?: return
        val dateString = parameters[PARAM_DATE_STRING] ?: return
        val currentState = parameters[PARAM_CURRENT_STATE] ?: false

        val date = LocalDate.parse(dateString)
        val db = AppDatabase.getDatabase(context)
        val repository = GoalRepository(db.goalDao())

        withContext(Dispatchers.IO) {
            repository.toggleGoalLog(goalId, date, currentState)
        }

        // Refrescar el widget inmediatamente
        GoalWidget().update(context, glanceId)
    }
}
