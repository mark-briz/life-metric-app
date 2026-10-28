package com.markbriz.lifemetric.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.markbriz.lifemetric.domain.latestBloodPressure
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.ui.bp.BloodPressureFormScreen
import com.markbriz.lifemetric.ui.bp.BloodPressureMeasureScreen
import com.markbriz.lifemetric.ui.bp.BloodPressureScreen
import com.markbriz.lifemetric.ui.components.GradientBackground
import com.markbriz.lifemetric.ui.home.HomeScreen
import com.markbriz.lifemetric.ui.profile.AboutScreen
import com.markbriz.lifemetric.ui.profile.ActivityFormScreen
import com.markbriz.lifemetric.ui.profile.BpReminderEditorScreen
import com.markbriz.lifemetric.ui.profile.BpTargetEditorScreen
import com.markbriz.lifemetric.ui.profile.BpWindowEditorScreen
import com.markbriz.lifemetric.ui.profile.DisclaimerScreen
import com.markbriz.lifemetric.ui.profile.HeightEditorScreen
import com.markbriz.lifemetric.ui.profile.PlanDetailScreen
import com.markbriz.lifemetric.ui.profile.PlanFormScreen
import com.markbriz.lifemetric.ui.profile.PlansScreen
import com.markbriz.lifemetric.ui.profile.ProfileScreen
import com.markbriz.lifemetric.ui.profile.ThemeEditorScreen
import com.markbriz.lifemetric.ui.profile.WaistEditorScreen
import com.markbriz.lifemetric.ui.profile.WeightGoalEditorScreen
import com.markbriz.lifemetric.ui.profile.WeightReminderEditorScreen
import com.markbriz.lifemetric.ui.profile.WeightUnitsEditorScreen
import com.markbriz.lifemetric.ui.profile.WorkoutReminderEditorScreen
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import com.markbriz.lifemetric.ui.weight.WeightFormScreen
import com.markbriz.lifemetric.ui.weight.WeightHowToScreen
import com.markbriz.lifemetric.ui.weight.WeightScreen
import com.markbriz.lifemetric.ui.welcome.WelcomeScreen
import com.markbriz.lifemetric.ui.workout.WorkoutEntryFormScreen
import com.markbriz.lifemetric.ui.workout.WorkoutFormScreen
import com.markbriz.lifemetric.ui.workout.WorkoutHowToScreen
import com.markbriz.lifemetric.ui.workout.WorkoutsScreen

private const val NEW = "new"

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Default.Home),
    Tab("bp", "BP", Icons.Default.MonitorHeart),
    Tab("weight", "Weight", Icons.Default.Scale),
    Tab("workouts", "Workout", Icons.Default.FitnessCenter),
    Tab("profile", "Profile", Icons.Default.Person),
)

@Composable
fun LifeMetricApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showTabs = route in tabs.map { it.route }
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    val undo = state.undo
    val notice = state.notice
    LaunchedEffect(undo?.token) {
        val toast = undo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = toast.message,
            actionLabel = "Undo",
            withDismissAction = true,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        else viewModel.dismissUndo(toast.token)
    }
    LaunchedEffect(notice?.token, undo?.token) {
        if (undo != null) return@LaunchedEffect
        val toast = notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = toast.message,
            duration = SnackbarDuration.Short,
        )
        viewModel.dismissNotice(toast.token)
    }

    if (state.booting) {
        GradientBackground {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Life Metric",
                    color = colors.ink,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colors.ink,
                    contentColor = colors.surface,
                    actionColor = colors.brand,
                    dismissActionContentColor = colors.surface,
                )
            }
        },
        bottomBar = {
            if (showTabs) {
                NavigationBar(containerColor = colors.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.brand,
                                selectedTextColor = colors.brand,
                                indicatorColor = colors.brandSoft,
                                unselectedIconColor = colors.inkMuted,
                                unselectedTextColor = colors.inkMuted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        GradientBackground(Modifier.padding(padding)) {
            NavHost(
                navController = nav,
                startDestination = if (state.disclaimerAccepted) "home" else "welcome",
            ) {
                composable("welcome") {
                    WelcomeScreen(
                        onAccept = {
                            viewModel.acceptDisclaimer()
                            nav.navigate("home") { popUpTo("welcome") { inclusive = true } }
                        },
                    )
                }
                composable("home") {
                    HomeScreen(
                        store = state.store,
                        onOpenBp = { nav.navigate("bp") },
                        onOpenWeight = { nav.navigate("weight") },
                        onOpenWorkouts = { nav.navigate("workouts") },
                        onAddBp = { nav.navigate("bpForm/$NEW") },
                        onAddWeight = { nav.navigate("weightForm/$NEW") },
                        onAddWorkout = {
                            if (state.store.plans.isEmpty()) {
                                nav.navigate("planForm/$NEW")
                            } else {
                                val id = viewModel.startWorkout()
                                if (id != null) nav.navigate("workoutForm/$id")
                            }
                        },
                    )
                }
                composable("bp") {
                    BloodPressureScreen(
                        state.store,
                        onAdd = { nav.navigate("bpForm/$NEW") },
                        onEdit = { nav.navigate("bpForm/$it") },
                        onDelete = viewModel::deleteBloodPressure,
                        onEditBpWindow = { nav.navigate("profileBpWindow") },
                        onEditBpTarget = { nav.navigate("profileBpTarget") },
                        onHowToMeasure = { nav.navigate("bpHowTo") },
                        onEditReminders = { nav.navigate("profileBpReminders") },
                    )
                }
                composable("bpHowTo") {
                    BloodPressureMeasureScreen(onBack = { nav.popBackStack() })
                }
                composable("bpForm/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val existing = state.store.bloodPressureReadings.find { it.id == id }
                    BloodPressureFormScreen(
                        existing = existing,
                        previous = if (existing == null) latestBloodPressure(state.store.bloodPressureReadings) else null,
                        missing = id != NEW && existing == null,
                        onBack = { nav.popBackStack() },
                        onHowToMeasure = { nav.navigate("bpHowTo") },
                        onSave = { draft ->
                            viewModel.saveBloodPressure(existing?.id, draft)
                        },
                        onFinished = {
                            nav.popBackStack()
                            viewModel.showSaved()
                        },
                    )
                }
                composable("weight") {
                    WeightScreen(
                        state.store,
                        onAdd = { nav.navigate("weightForm/$NEW") },
                        onEdit = { nav.navigate("weightForm/$it") },
                        onDelete = viewModel::deleteWeight,
                        onEditHeight = { nav.navigate("profileHeight") },
                        onEditGoal = { nav.navigate("profileGoal") },
                        onHowToWeigh = { nav.navigate("weightHowTo") },
                        onEditReminders = { nav.navigate("profileWeightReminders") },
                        onEditUnits = { nav.navigate("profileWeightUnits") },
                        onEditWaist = { nav.navigate("profileWaist") },
                    )
                }
                composable("weightHowTo") {
                    WeightHowToScreen(onBack = { nav.popBackStack() })
                }
                composable("weightForm/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val existing = state.store.weightRecords.find { it.id == id }
                    WeightFormScreen(
                        existing = existing,
                        lastWeight = latestWeight(state.store.weightRecords)?.weight,
                        useKg = state.store.profile.weightDisplayKg,
                        missing = id != NEW && existing == null,
                        onBack = { nav.popBackStack() },
                        onHowToWeigh = { nav.navigate("weightHowTo") },
                        onSave = { date, time, weight, tags, note ->
                            viewModel.saveWeight(existing?.id, date, time, weight, tags, note)
                            nav.popBackStack()
                            viewModel.showSaved()
                        },
                    )
                }
                composable("workouts") {
                    WorkoutsScreen(
                        state.store,
                        onAdd = {
                            if (state.store.plans.isEmpty()) {
                                nav.navigate("planForm/$NEW")
                            } else {
                                val id = viewModel.startWorkout()
                                if (id != null) nav.navigate("workoutForm/$id")
                            }
                        },
                        onOpen = { nav.navigate("workoutForm/$it") },
                        onDelete = viewModel::deleteWorkout,
                        onPlans = { nav.navigate("plans") },
                        onCreatePlan = { nav.navigate("planForm/$NEW") },
                        onHowToLog = { nav.navigate("workoutHowTo") },
                        onReminders = { nav.navigate("profileWorkoutReminders") },
                    )
                }
                composable("workoutHowTo") {
                    WorkoutHowToScreen(onBack = { nav.popBackStack() })
                }
                composable("workoutForm/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val workout = state.store.workouts.find { it.id == id }
                        ?: state.draftWorkout?.takeIf { it.id == id }
                    WorkoutFormScreen(
                        store = state.store,
                        workout = workout,
                        missing = workout == null,
                        isNew = state.draftWorkout?.id == id,
                        onBack = {
                            viewModel.discardEmptyDraft(id)
                            nav.popBackStack()
                        },
                        onDone = {
                            val persisted = workout != null && state.draftWorkout?.id != id
                            viewModel.discardEmptyDraft(id)
                            nav.popBackStack()
                            if (persisted) viewModel.showSaved()
                        },
                        onDate = { date -> if (workout != null) viewModel.updateWorkoutDate(workout.id, date) },
                        onPlan = { planId -> if (workout != null) viewModel.updateWorkoutPlan(workout.id, planId) },
                        onAddEntry = {
                            if (workout != null) nav.navigate("workoutEntry/${workout.id}/$NEW")
                        },
                        onEditEntry = { item -> nav.navigate("workoutEntry/${workout?.id}/${item.id}") },
                        onRemoveEntry = { entryId -> if (workout != null) viewModel.deleteWorkoutEntry(workout.id, entryId) },
                        onPlans = { nav.navigate("plans") },
                        onCreatePlan = { nav.navigate("planForm/$NEW") },
                        onAddActivity = { planId -> nav.navigate("activityForm/$planId/$NEW") },
                        onDuration = { duration -> if (workout != null) viewModel.updateWorkoutDuration(workout.id, duration) },
                        onNote = { note -> if (workout != null) viewModel.updateWorkoutNote(workout.id, note) },
                        onRestTimer = viewModel::saveWorkoutRestTimer,
                        onRestTaken = { entryId, rest -> if (workout != null) viewModel.updateWorkoutEntryRest(workout.id, entryId, rest) },
                        onHowToLog = { nav.navigate("workoutHowTo") },
                    )
                }
                composable("workoutEntry/{target}/{entryId}") { entry ->
                    val target = entry.arguments?.getString("target").orEmpty()
                    val entryId = entry.arguments?.getString("entryId").orEmpty()
                    val workout = state.store.workouts.find { it.id == target }
                        ?: state.draftWorkout?.takeIf { it.id == target }
                    val plan = state.store.plans.find { it.id == workout?.planId }
                    val isNew = entryId == NEW
                    val existing = workout?.entries?.find { it.id == entryId }
                    WorkoutEntryFormScreen(
                        plan = plan,
                        existing = existing,
                        missing = !isNew && existing == null,
                        plans = state.store.plans,
                        workouts = state.store.workouts,
                        currentWorkoutId = target,
                        currentWorkoutDate = workout?.date.orEmpty(),
                        currentEntries = workout?.entries.orEmpty(),
                        useKg = state.store.profile.weightDisplayKg,
                        onBack = { nav.popBackStack() },
                        onSave = { saved ->
                            viewModel.upsertSavedEntry(target, saved)
                            nav.popBackStack()
                            viewModel.showSaved()
                        },
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onHeight = { nav.navigate("profileHeight") },
                        onGoal = { nav.navigate("profileGoal") },
                        onBpWindow = { nav.navigate("profileBpWindow") },
                        onBpTarget = { nav.navigate("profileBpTarget") },
                        onBpReminders = { nav.navigate("profileBpReminders") },
                        onWeightReminders = { nav.navigate("profileWeightReminders") },
                        onWorkoutReminders = { nav.navigate("profileWorkoutReminders") },
                        onWeightUnits = { nav.navigate("profileWeightUnits") },
                        onWaist = { nav.navigate("profileWaist") },
                        onTheme = { nav.navigate("profileTheme") },
                        onPlans = { nav.navigate("plans") },
                        exportSnapshot = viewModel::exportSnapshot,
                        onImport = viewModel::importData,
                        onDisclaimer = { nav.navigate("disclaimer") },
                        onAbout = { nav.navigate("about") },
                        onEraseAll = {
                            viewModel.eraseAllData()
                            nav.navigate("home") { popUpTo(0) { inclusive = true } }
                        },
                    )
                }
                composable("profileHeight") {
                    HeightEditorScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onSave = viewModel::saveProfile,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileGoal") {
                    WeightGoalEditorScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onSave = viewModel::saveProfile,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileBpWindow") {
                    BpWindowEditorScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onSave = viewModel::saveProfile,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileBpTarget") {
                    BpTargetEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveBpTarget,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileBpReminders") {
                    BpReminderEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveBpReminders,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileWeightReminders") {
                    WeightReminderEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveWeightReminders,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileWorkoutReminders") {
                    WorkoutReminderEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveWorkoutReminders,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileWeightUnits") {
                    WeightUnitsEditorScreen(
                        useKg = state.store.profile.weightDisplayKg,
                        onSave = viewModel::saveWeightDisplayKg,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileWaist") {
                    WaistEditorScreen(
                        waistInches = state.store.profile.waistInches,
                        onSave = viewModel::saveWaist,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("profileTheme") {
                    ThemeEditorScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onSave = viewModel::saveProfile,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                    )
                }
                composable("plans") {
                    val fromWorkout = nav.previousBackStackEntry?.destination?.route
                        ?.let { it == "workouts" || it.startsWith("workoutForm") } == true
                    PlansScreen(
                        state.store.plans,
                        onBack = { nav.popBackStack() },
                        backLabel = if (fromWorkout) "Workouts" else "Profile",
                        onAdd = { nav.navigate("planForm/$NEW") },
                        onEdit = { nav.navigate("planForm/$it") },
                        onOpen = { nav.navigate("planDetail/$it") },
                    )
                }
                composable("planForm/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val existing = state.store.plans.find { it.id == id }
                    val previous = nav.previousBackStackEntry?.destination?.route
                    PlanFormScreen(
                        existing = existing,
                        missing = id != NEW && existing == null,
                        onBack = { nav.popBackStack() },
                        backLabel = if (previous == "workouts" || previous?.startsWith("workoutForm") == true) "Workouts" else "Plans",
                        onSave = { name, description, typeId ->
                            val error = viewModel.savePlan(existing?.id, name, description, typeId)
                            if (error == null) {
                                nav.popBackStack()
                                viewModel.showSaved()
                            }
                            error
                        },
                    )
                }
                composable("planDetail/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val plan = state.store.plans.find { it.id == id }
                    PlanDetailScreen(
                        plan = plan,
                        onBack = { nav.popBackStack() },
                        onAdd = { nav.navigate("activityForm/${plan?.id ?: NEW}/$NEW") },
                        onEdit = { machine -> nav.navigate("activityForm/${plan?.id}/${machine.id}") },
                        onDelete = { machine -> if (plan != null) viewModel.deleteActivity(plan.id, machine.id) },
                    )
                }
                composable("activityForm/{planId}/{machineId}") { entry ->
                    val planId = entry.arguments?.getString("planId")
                    val machineId = entry.arguments?.getString("machineId")
                    val plan = state.store.plans.find { it.id == planId }
                    val machine = plan?.machines?.find { it.id == machineId }
                    val previous = nav.previousBackStackEntry?.destination?.route
                    ActivityFormScreen(
                        planMissing = plan == null,
                        activityMissing = machineId != NEW && machine == null,
                        existing = machine,
                        onBack = { nav.popBackStack() },
                        backLabel = if (previous?.startsWith("workoutForm") == true) "Workout" else "Plan",
                        onSave = { name, description ->
                            val error = if (planId == null) "Plan not found." else viewModel.saveActivity(planId, machine?.id, name, description)
                            if (error == null) {
                                nav.popBackStack()
                                viewModel.showSaved()
                            }
                            error
                        },
                    )
                }
                composable("disclaimer") { DisclaimerScreen { nav.popBackStack() } }
                composable("about") { AboutScreen { nav.popBackStack() } }
            }
        }
    }
}

private fun profileEditorBackLabel(route: String?): String = when (route) {
    "bp" -> "Blood pressure"
    "bpHowTo" -> "Blood pressure"
    "weight" -> "Weight"
    "weightHowTo" -> "Weight"
    "workouts" -> "Workouts"
    "workoutHowTo" -> "Workouts"
    else -> "Profile"
}
