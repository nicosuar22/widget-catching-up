package com.example.widgetcatchingup.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.widgetcatchingup.data.local.AppDatabase
import com.example.widgetcatchingup.data.local.Goal
import com.example.widgetcatchingup.data.local.GoalLog
import com.example.widgetcatchingup.data.repository.GoalRepository
import com.example.widgetcatchingup.data.repository.MonthlyStreakStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GoalWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val repository = GoalRepository(db.goalDao())

        val (goals, logs, stats, weekDates) = withContext(Dispatchers.IO) {
            val g = db.goalDao().getAllGoalsDirect()
            val l = repository.getLogsForCurrentWeekDirect()
            val s = repository.calculateMonthlyStreakStats()
            val w = repository.getCurrentWeekDates()
            Quadruple(g, l, s, w)
        }

        provideContent {
            WidgetContent(
                goals = goals,
                logs = logs,
                stats = stats,
                weekDates = weekDates
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// Paleta de colores translúcida / Glassmorphism adaptada a ColorProvider
private val GlassBackground = ColorProvider(Color(0x22000000))        // Fondo general translúcido
private val GlassCardBackground = ColorProvider(Color(0xCC181C24))    // Tarjeta translúcida efecto cristal oscuro
private val GlassFooterBackground = ColorProvider(Color(0xB3101318))  // Footer translúcido
private val DividerColor = ColorProvider(Color(0x26FFFFFF))           // Separador sutil
private val TextPrimary = ColorProvider(Color(0xFFFFFFFF))            // Texto principal blanco brillante
private val TextSecondary = ColorProvider(Color(0xFFA1A8B8))          // Texto secundario suave
private val CheckedGreen = ColorProvider(Color(0xFF10893E))           // Verde esmeralda para marcados
private val UncheckedBg = ColorProvider(Color(0x2EFFFFFF))            // Casilla translúcida sin marcar
private val FireColor = ColorProvider(Color(0xFFFF9800))              // Fuego racha naranja

@Composable
private fun WidgetContent(
    goals: List<Goal>,
    logs: List<GoalLog>,
    stats: MonthlyStreakStats,
    weekDates: List<LocalDate>
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlassBackground)
            .padding(8.dp)
    ) {
        // --- LISTADO DE TARJETAS DE METAS ---
        if (goals.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(GlassCardBackground)
                    .cornerRadius(16.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Añade metas desde la App para verlas aquí",
                    style = TextStyle(
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                items(goals) { goal ->
                    GoalCard(
                        goal = goal,
                        logs = logs,
                        weekDates = weekDates
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // --- TARJETA RESUMEN TRANSLÚCIDA: RACHAS DEL MES CON NOMBRE DE META ---
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlassFooterBackground)
                .cornerRadius(14.dp)
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 MEJOR RACHA: ",
                    style = TextStyle(
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (stats.bestGoalTitle != "-") "${stats.bestGoalTitle} (${stats.bestStreak} días)" else "${stats.bestStreak} días",
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📉 PEOR RACHA: ",
                    style = TextStyle(
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = if (stats.worstGoalTitle != "-") "${stats.worstGoalTitle} (${stats.worstStreak} días)" else "${stats.worstStreak} días",
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    logs: List<GoalLog>,
    weekDates: List<LocalDate>
) {
    val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
    val goalLogs = logs.filter { it.goalId == goal.id }
    val weeklyStreak = weekDates.count { date ->
        val dStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        goalLogs.any { it.date == dStr && it.isCompleted }
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlassCardBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        // 1. Título de la meta
        Text(
            text = goal.title,
            style = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )

        Spacer(modifier = GlanceModifier.height(10.dp))

        // 2. Fila con los 7 días (Letra arriba + Casilla abajo)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDates.forEachIndexed { index, date ->
                val dStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val isCompleted = goalLogs.any { it.date == dStr && it.isCompleted }
                val label = dayLabels.getOrElse(index) { "" }

                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Letra del día (L, M, M, J, V, S, D)
                    Text(
                        text = label,
                        style = TextStyle(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Botón circular de estado
                    Button(
                        text = if (isCompleted) "✓" else "○",
                        onClick = actionRunCallback<ToggleGoalAction>(
                            actionParametersOf(
                                ToggleGoalAction.PARAM_GOAL_ID to goal.id,
                                ToggleGoalAction.PARAM_DATE_STRING to dStr,
                                ToggleGoalAction.PARAM_CURRENT_STATE to isCompleted
                            )
                        ),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isCompleted) CheckedGreen else UncheckedBg,
                            contentColor = if (isCompleted) TextPrimary else TextSecondary
                        ),
                        modifier = GlanceModifier
                            .width(32.dp)
                            .height(30.dp)
                            .cornerRadius(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        // 3. Separador sutil
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerColor)
        ) {}

        Spacer(modifier = GlanceModifier.height(8.dp))

        // 4. Pie de la tarjeta: "Racha Semanal" + "🔥 5"
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Racha Semanal",
                style = TextStyle(
                    color = TextSecondary,
                    fontSize = 11.sp
                ),
                modifier = GlanceModifier.defaultWeight()
            )

            Text(
                text = "🔥 $weeklyStreak",
                style = TextStyle(
                    color = if (weeklyStreak > 0) FireColor else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
