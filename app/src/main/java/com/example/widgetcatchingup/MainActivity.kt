package com.example.widgetcatchingup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.widgetcatchingup.ui.screens.GoalManagementScreen
import com.example.widgetcatchingup.ui.theme.WidgetCatchingUpTheme
import com.example.widgetcatchingup.ui.viewmodel.GoalViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GoalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WidgetCatchingUpTheme {
                GoalManagementScreen(viewModel = viewModel)
            }
        }
    }
}
