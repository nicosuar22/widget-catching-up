package com.example.widgetcatchingup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.widgetcatchingup.data.local.Goal
import com.example.widgetcatchingup.data.local.GoalLog
import com.example.widgetcatchingup.ui.viewmodel.GoalViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalManagementScreen(viewModel: GoalViewModel) {
    val goals by viewModel.goals.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val selectedNavIndex by viewModel.selectedNavIndex.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedNavIndex == 0,
                    onClick = { viewModel.setSelectedNavIndex(0) },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Metas") },
                    label = { Text("Metas") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        selectedIconColor = Color(0xFF2E7D32),
                        selectedTextColor = Color(0xFF2E7D32)
                    )
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { viewModel.setSelectedNavIndex(1) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Progreso") },
                    label = { Text("Progreso") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        selectedIconColor = Color(0xFF2E7D32),
                        selectedTextColor = Color(0xFF2E7D32)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedNavIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF0052CC),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva Meta", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedNavIndex == 0) {
                MetasTabContent(
                    goals = goals,
                    logs = logs,
                    weekDates = viewModel.weekDates,
                    onToggleLog = { goalId, date, currentState ->
                        viewModel.toggleLog(goalId, date, currentState)
                    },
                    onMoveUp = { viewModel.moveGoalUp(it) },
                    onMoveDown = { viewModel.moveGoalDown(it) },
                    onEdit = { editingGoal = it },
                    onDelete = { viewModel.deleteGoal(it) }
                )
            } else {
                ProgresoTabContent(viewModel = viewModel)
            }
        }
    }

    // --- DIÁLOGO DE ACTUALIZACIÓN DE GITHUB ---
    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Color(0xFF0052CC)) },
            title = { Text("¡Nueva Actualización (${info.latestVersion})!") },
            text = {
                Column {
                    Text("Hay una nueva versión disponible en GitHub.")
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.downloadUpdate(context, info.downloadUrl)
                        viewModel.dismissUpdateDialog()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC))
                ) {
                    Text("Descargar e Instalar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (showAddDialog) {
        GoalInputDialog(
            title = "Nueva Meta",
            initialText = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { newTitle ->
                viewModel.addGoal(newTitle)
                showAddDialog = false
            }
        )
    }

    editingGoal?.let { goal ->
        GoalInputDialog(
            title = "Editar Meta",
            initialText = goal.title,
            onDismiss = { editingGoal = null },
            onConfirm = { updatedTitle ->
                viewModel.updateGoalTitle(goal.id, updatedTitle)
                editingGoal = null
            }
        )
    }
}

// --- TAB 1: METAS SEMANALES ---
@Composable
private fun MetasTabContent(
    goals: List<Goal>,
    logs: List<GoalLog>,
    weekDates: List<LocalDate>,
    onToggleLog: (Long, LocalDate, Boolean) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onEdit: (Goal) -> Unit,
    onDelete: (Goal) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "TU PROGRESO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8A94A6),
            letterSpacing = 1.sp
        )
        Text(
            text = "Metas Semanales",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes metas registradas.\nPresiona + Nueva Meta para comenzar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(goals, key = { _, goal -> goal.id }) { index, goal ->
                    GoalCardApp(
                        goal = goal,
                        index = index,
                        totalCount = goals.size,
                        logs = logs,
                        weekDates = weekDates,
                        onToggleLog = onToggleLog,
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onEdit = { onEdit(goal) },
                        onDelete = { onDelete(goal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalCardApp(
    goal: Goal,
    index: Int,
    totalCount: Int,
    logs: List<GoalLog>,
    weekDates: List<LocalDate>,
    onToggleLog: (Long, LocalDate, Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
    val goalLogs = logs.filter { it.goalId == goal.id }
    val weeklyStreak = weekDates.count { date ->
        val dStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        goalLogs.any { it.date == dStr && it.isCompleted }
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título + Menú de acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color(0xFF94A3B8))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar nombre") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onEdit() }
                        )
                        if (index > 0) {
                            DropdownMenuItem(
                                text = { Text("Subir posición") },
                                leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                                onClick = { showMenu = false; onMoveUp() }
                            )
                        }
                        if (index < totalCount - 1) {
                            DropdownMenuItem(
                                text = { Text("Bajar posición") },
                                leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                onClick = { showMenu = false; onMoveDown() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Eliminar meta", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de 7 días (L M M J V S D)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDates.forEachIndexed { dayIdx, date ->
                    val dStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val isCompleted = goalLogs.any { it.date == dStr && it.isCompleted }
                    val label = dayLabels.getOrElse(dayIdx) { "" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) Color(0xFF0F9D58) else Color.Transparent
                                )
                                .border(
                                    width = if (isCompleted) 0.dp else 1.dp,
                                    color = if (isCompleted) Color.Transparent else Color(0xFFCBD5E1),
                                    shape = CircleShape
                                )
                                .clickable {
                                    onToggleLog(goal.id, date, isCompleted)
                                }
                        ) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Completado",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Pie de la tarjeta: "Racha Semanal" + "🔥 5"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Racha Semanal",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "🔥 $weeklyStreak",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (weeklyStreak > 0) Color(0xFFFF9800) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

// --- TAB 2: PROGRESO Y CALENDARIO MENSUAL ---
@Composable
private fun ProgresoTabContent(viewModel: GoalViewModel) {
    val goals by viewModel.goals.collectAsState()
    val selectedGoalId by viewModel.selectedGoalId.collectAsState()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsState()
    val monthlyLogs by viewModel.monthlyLogsForSelectedGoal.collectAsState()

    val selectedGoal = goals.find { it.id == selectedGoalId } ?: goals.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "ESTADÍSTICAS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8A94A6),
            letterSpacing = 1.sp
        )
        Text(
            text = "Progreso por Meta",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Crea tu primera meta para ver el historial.",
                    color = Color(0xFF64748B)
                )
            }
            return
        }

        // Selector horizontal de metas
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(goals) { goal ->
                val isSelected = goal.id == selectedGoal?.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setSelectedGoalId(goal.id) },
                    label = { Text(goal.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0F9D58),
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color(0xFF334155)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFFE2E8F0),
                        selectedBorderColor = Color(0xFF0F9D58),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedGoal?.let { goal ->
            // Selector de mes (< Agosto 2026 >)
            val monthTitle = selectedYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))).capitalize(Locale.getDefault())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = Color(0xFF334155))
                }

                Text(
                    text = monthTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente", tint = Color(0xFF334155))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendario Mensual en Grid
            MonthlyCalendarCard(
                yearMonth = selectedYearMonth,
                logs = monthlyLogs,
                onToggleDate = { date, currentState ->
                    viewModel.toggleLog(goal.id, date, currentState)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de estadísticas del mes
            val stats = viewModel.repository.calculateSingleGoalStats(selectedYearMonth, monthlyLogs)
            MonthlyStatsCard(stats = stats)
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    yearMonth: YearMonth,
    logs: List<GoalLog>,
    onToggleDate: (LocalDate, Boolean) -> Unit
) {
    val dayLabels = listOf("L", "M", "M", "J", "V", "S", "D")
    val firstDayOfMonth = yearMonth.atDay(1)
    val totalDaysInMonth = yearMonth.lengthOfMonth()
    
    // Offset para ajustar Lunes = 0 .. Domingo = 6
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1

    val completedDates = logs.filter { it.isCompleted }.map { LocalDate.parse(it.date) }.toSet()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabecera L M M J V S D
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayLabels.forEach { label ->
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid de 7 columnas para los días del mes
            val totalCells = dayOfWeekOffset + totalDaysInMonth
            val rows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - dayOfWeekOffset + 1

                            if (cellIndex >= dayOfWeekOffset && dayNumber <= totalDaysInMonth) {
                                val currentDate = yearMonth.atDay(dayNumber)
                                val isCompleted = completedDates.contains(currentDate)

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) Color(0xFF0F9D58) else Color(0xFFF8FAFC)
                                        )
                                        .border(
                                            width = if (isCompleted) 0.dp else 1.dp,
                                            color = if (isCompleted) Color.Transparent else Color(0xFFE2E8F0),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onToggleDate(currentDate, isCompleted)
                                        }
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCompleted) Color.White else Color(0xFF475569)
                                    )
                                }
                            } else {
                                // Espacio vacío de relleno
                                Spacer(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyStatsCard(stats: com.example.widgetcatchingup.data.repository.SingleGoalMonthlyStats) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Resumen del Mes",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(title = "Días Logrados", value = "${stats.completedDaysCount}/${stats.totalDaysInMonth}")
                StatItem(title = "Cumplimiento", value = "${stats.percentage}%")
                StatItem(title = "Mejor Racha", value = "🔥 ${stats.maxStreak}d")
            }
        }
    }
}

@Composable
private fun StatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F9D58)
        )
    }
}

@Composable
private fun GoalInputDialog(
    title: String,
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nombre de la Meta / Objetivo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
