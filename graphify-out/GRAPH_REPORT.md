# Graph Report - Widget catching up  (2026-08-20)

## Corpus Check
- Corpus is ~33,175 words - fits in a single context window. You may not need a graph.

## Summary
- 110 nodes · 214 edges · 12 communities
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 2 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Persistencia y Room Database
- Widget Interactivo Glance
- UI Compose y ViewModel
- Auto-updater y Distribución
- Modulo 4
- Modulo 5
- Modulo 6
- Modulo 7
- Modulo 8

## God Nodes (most connected - your core abstractions)
1. `GoalViewModel` - 27 edges
2. `Goal` - 21 edges
3. `GoalLog` - 21 edges
4. `GoalRepository` - 20 edges
5. `GoalDao` - 16 edges
6. `AppDatabase` - 7 edges
7. `GoalManagementScreen()` - 7 edges
8. `GoalWidget` - 7 edges
9. `WidgetContent()` - 6 edges
10. `MonthlyStreakStats` - 5 edges

## Surprising Connections (you probably didn't know these)
- `GoalWidgetReceiver` --calls--> `GoalWidget`  [INFERRED]
  app/src/main/java/com/example/widgetcatchingup/widget/GoalWidgetReceiver.kt → app/src/main/java/com/example/widgetcatchingup/widget/GoalWidget.kt
- `MainActivity` --references--> `GoalViewModel`  [EXTRACTED]
  app/src/main/java/com/example/widgetcatchingup/MainActivity.kt → app/src/main/java/com/example/widgetcatchingup/ui/viewmodel/GoalViewModel.kt
- `GoalRepository` --references--> `Goal`  [EXTRACTED]
  app/src/main/java/com/example/widgetcatchingup/data/repository/GoalRepository.kt → app/src/main/java/com/example/widgetcatchingup/data/local/Goal.kt
- `GoalCardApp()` --references--> `Goal`  [EXTRACTED]
  app/src/main/java/com/example/widgetcatchingup/ui/screens/GoalManagementScreen.kt → app/src/main/java/com/example/widgetcatchingup/data/local/Goal.kt
- `MetasTabContent()` --references--> `Goal`  [EXTRACTED]
  app/src/main/java/com/example/widgetcatchingup/ui/screens/GoalManagementScreen.kt → app/src/main/java/com/example/widgetcatchingup/data/local/Goal.kt

## Import Cycles
- None detected.

## Communities (12 total, 0 thin omitted)

### Community 0 - "Persistencia y Room Database"
Cohesion: 0.12
Nodes (7): AndroidViewModel, GitHubUpdateChecker, UpdateInfo, GoalViewModel, Context, YearMonth, StateFlow

### Community 1 - "Widget Interactivo Glance"
Cohesion: 0.16
Nodes (5): GoalLog, GoalRepository, Flow, YearMonth, SingleGoalMonthlyStats

### Community 2 - "UI Compose y ViewModel"
Cohesion: 0.24
Nodes (3): Goal, GoalDao, Flow

### Community 3 - "Auto-updater y Distribución"
Cohesion: 0.29
Nodes (10): GoalCardApp(), GoalInputDialog(), GoalManagementScreen(), YearMonth, MetasTabContent(), MonthlyCalendarCard(), MonthlyStatsCard(), ProgresoTabContent() (+2 more)

### Community 4 - "Modulo 4"
Cohesion: 0.38
Nodes (8): MonthlyStreakStats, GoalCard(), GoalWidget, Context, GlanceAppWidget, GlanceId, Quadruple, WidgetContent()

### Community 5 - "Modulo 5"
Cohesion: 0.48
Nodes (5): ActionCallback, ActionParameters, Context, GlanceId, ToggleGoalAction

### Community 6 - "Modulo 6"
Cohesion: 0.43
Nodes (4): MainActivity, WidgetCatchingUpTheme(), Bundle, ComponentActivity

### Community 7 - "Modulo 7"
Cohesion: 0.47
Nodes (3): AppDatabase, Context, RoomDatabase

### Community 8 - "Modulo 8"
Cohesion: 0.83
Nodes (3): GoalWidgetReceiver, GlanceAppWidget, GlanceAppWidgetReceiver

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `GoalViewModel` connect `Persistencia y Room Database` to `Widget Interactivo Glance`, `UI Compose y ViewModel`, `Auto-updater y Distribución`, `Modulo 6`?**
  _High betweenness centrality (0.369) - this node is a cross-community bridge._
- **Why does `GoalRepository` connect `Widget Interactivo Glance` to `Persistencia y Room Database`, `UI Compose y ViewModel`, `Modulo 4`, `Modulo 5`?**
  _High betweenness centrality (0.235) - this node is a cross-community bridge._
- **Why does `Goal` connect `UI Compose y ViewModel` to `Persistencia y Room Database`, `Widget Interactivo Glance`, `Auto-updater y Distribución`, `Modulo 4`?**
  _High betweenness centrality (0.210) - this node is a cross-community bridge._
- **Should `Persistencia y Room Database` be split into smaller, more focused modules?**
  _Cohesion score 0.12333333333333334 - nodes in this community are weakly interconnected._