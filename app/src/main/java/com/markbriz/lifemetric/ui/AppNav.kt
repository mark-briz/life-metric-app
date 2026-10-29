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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.markbriz.lifemetric.data.newId
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.domain.latestBloodPressure
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.ui.bp.BloodPressureFormScreen
import com.markbriz.lifemetric.ui.bp.BloodPressureMeasureScreen
import com.markbriz.lifemetric.ui.bp.BloodPressureScreen
import com.markbriz.lifemetric.ui.bp.BloodPressureSettingsScreen
import com.markbriz.lifemetric.ui.components.BloodPressureCategoriesDialog
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
import com.markbriz.lifemetric.ui.weight.WeightSettingsScreen
import com.markbriz.lifemetric.ui.welcome.WelcomeScreen
import com.markbriz.lifemetric.ui.workout.PlanPickerSheet
import com.markbriz.lifemetric.ui.workout.RestTimerEditorScreen
import com.markbriz.lifemetric.ui.workout.WorkoutFormScreen
import com.markbriz.lifemetric.ui.workout.WorkoutHowToScreen
import com.markbriz.lifemetric.ui.workout.WorkoutSettingsScreen
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
    var categoriesFor by remember { mutableStateOf<BpStatus?>(null) }
    var startSheetOpen by remember { mutableStateOf(false) }
    var startSheetSelected by remember { mutableStateOf<String?>(null) }
    /** Set while a plan is being created from the start sheet, so the sheet can reopen with it selected. */
    var pendingNewPlanId by remember { mutableStateOf<String?>(null) }
    val openStartSheet: () -> Unit = {
        startSheetSelected = null
        startSheetOpen = true
    }
    val startSession: (String?) -> Unit = { planId ->
        viewModel.startWorkout(planId)?.let { nav.navigate("workoutForm/$it") }
    }
    val savedAndBack: () -> Unit = {
        nav.popBackStack()
        viewModel.showSaved()
    }
    val createPlanFromStartSheet: () -> Unit = {
        pendingNewPlanId = newId("plan")
        nav.navigate("planForm/$NEW")
    }
    LaunchedEffect(undo?.token) {
        val toast = undo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = toast.message,
            actionLabel = "Undo",
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
        else viewModel.dismissUndo(toast.token)
    }
    LaunchedEffect(notice?.token, undo?.token) {
        if (undo != null) return@LaunchedEffect
        val toast = notice ?: return@LaunchedEffect
        val status = toast.bpStatus
        val result = if (status != null) {
            snackbarHostState.showSnackbar(
                message = toast.message,
                actionLabel = "Details",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        } else {
            snackbarHostState.showSnackbar(message = toast.message, duration = SnackbarDuration.Short)
        }
        if (result == SnackbarResult.ActionPerformed && status != null) categoriesFor = status
        viewModel.dismissNotice(toast.token)
    }
    categoriesFor?.let { status ->
        BloodPressureCategoriesDialog(selected = status, onDismiss = { categoriesFor = null })
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
                    actionColor = colors.snackbarAction,
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
                                selectedIconColor = colors.brandDark,
                                selectedTextColor = colors.brandDark,
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
                        onAddWorkout = openStartSheet,
                        onSetHeight = { nav.navigate("profileHeight") },
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
                        onSettings = { nav.navigate("bpSettings") },
                        highlightedId = state.highlightedId,
                    )
                }
                composable("bpSettings") {
                    BloodPressureSettingsScreen(
                        store = state.store,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onTarget = { nav.navigate("profileBpTarget") },
                        onWindow = { nav.navigate("profileBpWindow") },
                        onReminders = { nav.navigate("profileBpReminders") },
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
                        onFinished = { status ->
                            nav.popBackStack()
                            viewModel.showBloodPressureSaved(status)
                        },
                        onDelete = existing?.let { reading ->
                            {
                                viewModel.deleteBloodPressure(reading.id)
                                nav.popBackStack()
                            }
                        },
                    )
                }
                composable("weight") {
                    WeightScreen(
                        state.store,
                        onAdd = { nav.navigate("weightForm/$NEW") },
                        onEdit = { nav.navigate("weightForm/$it") },
                        onDelete = viewModel::deleteWeight,
                        onEditGoal = { nav.navigate("profileGoal") },
                        onHowToWeigh = { nav.navigate("weightHowTo") },
                        onSettings = { nav.navigate("weightSettings") },
                        highlightedId = state.highlightedId,
                        onSaveBodyMeasures = { feet, inches, waist ->
                            viewModel.saveBodyMeasures(feet, inches, waist).also { if (it == null) viewModel.showSaved() }
                        },
                    )
                }
                composable("weightSettings") {
                    WeightSettingsScreen(
                        store = state.store,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onGoal = { nav.navigate("profileGoal") },
                        onReminders = { nav.navigate("profileWeightReminders") },
                        onUnits = { nav.navigate("profileWeightUnits") },
                        onHeight = { nav.navigate("profileHeight") },
                        onWaist = { nav.navigate("profileWaist") },
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
                        onDelete = existing?.let { record ->
                            {
                                viewModel.deleteWeight(record.id)
                                nav.popBackStack()
                            }
                        },
                    )
                }
                composable("workouts") {
                    WorkoutsScreen(
                        state.store,
                        onAdd = openStartSheet,
                        onStartPlan = startSession,
                        onOpen = { nav.navigate("workoutForm/$it") },
                        onDelete = viewModel::deleteWorkout,
                        onPlans = { nav.navigate("plans") },
                        onCreatePlan = { nav.navigate("planForm/$NEW") },
                        onHowToLog = { nav.navigate("workoutHowTo") },
                        onSettings = { nav.navigate("workoutSettings") },
                        highlightedId = state.highlightedId,
                    )
                }
                composable("workoutSettings") {
                    WorkoutSettingsScreen(
                        store = state.store,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onPlans = { nav.navigate("plans") },
                        onRestTimer = { nav.navigate("workoutRestTimer") },
                        onReminders = { nav.navigate("profileWorkoutReminders") },
                        onUnits = { nav.navigate("profileWeightUnits") },
                    )
                }
                composable("workoutRestTimer") {
                    RestTimerEditorScreen(
                        restSec = state.store.profile.workoutRestTimerSec,
                        onSave = viewModel::saveWorkoutRestTimer,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onSaved = viewModel::showSaved,
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
                        isDraft = state.draftWorkout?.id == id,
                        onBack = {
                            viewModel.discardEmptyDraft(id)
                            nav.popBackStack()
                        },
                        onFinish = {
                            val persisted = workout != null && state.draftWorkout?.id != id
                            viewModel.discardEmptyDraft(id)
                            nav.popBackStack()
                            if (persisted) {
                                workout?.id?.let(viewModel::highlightRecord)
                                viewModel.showSaved()
                            }
                        },
                        onSaveDetails = { date, duration, note ->
                            if (workout != null) viewModel.updateWorkoutDetails(workout.id, date, duration, note)
                        },
                        onPlan = { planId -> if (workout != null) viewModel.updateWorkoutPlan(workout.id, planId) },
                        onSaveEntry = { saved -> if (workout != null) viewModel.upsertSavedEntry(workout.id, saved) },
                        onRemoveEntry = { entryId -> if (workout != null) viewModel.deleteWorkoutEntry(workout.id, entryId) },
                        onDeleteWorkout = {
                            if (workout != null) viewModel.deleteWorkout(workout.id)
                            nav.popBackStack()
                        },
                        onPlans = { nav.navigate("plans") },
                        onCreatePlan = { nav.navigate("planForm/$NEW") },
                        onAddActivity = { planId -> nav.navigate("activityForm/$planId/$NEW") },
                        onRestTaken = { entryId, rest -> if (workout != null) viewModel.updateWorkoutEntryRest(workout.id, entryId, rest) },
                        onRestTimerSettings = { nav.navigate("workoutRestTimer") },
                        onHowToLog = { nav.navigate("workoutHowTo") },
                        backLabel = when (nav.previousBackStackEntry?.destination?.route) {
                            "home" -> "Dashboard"
                            "planDetail/{id}" -> "Plan"
                            else -> "Workouts"
                        },
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onHeight = { nav.navigate("profileHeight") },
                        onWaist = { nav.navigate("profileWaist") },
                        onUnits = { nav.navigate("profileWeightUnits") },
                        onBpSettings = { nav.navigate("bpSettings") },
                        onWeightSettings = { nav.navigate("weightSettings") },
                        onWorkoutSettings = { nav.navigate("workoutSettings") },
                        onTheme = { nav.navigate("profileTheme") },
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
                        onSaved = savedAndBack,
                    )
                }
                composable("profileGoal") {
                    WeightGoalEditorScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onSave = viewModel::saveProfile,
                        onBack = { nav.popBackStack() },
                        onSaved = savedAndBack,
                    )
                }
                composable("profileBpWindow") {
                    BpWindowEditorScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onSave = viewModel::saveProfile,
                        onBack = { nav.popBackStack() },
                        onSaved = savedAndBack,
                    )
                }
                composable("profileBpTarget") {
                    BpTargetEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveBpTarget,
                        onBack = { nav.popBackStack() },
                        onSaved = savedAndBack,
                    )
                }
                composable("profileBpReminders") {
                    BpReminderEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveBpReminders,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onSaved = viewModel::showSaved,
                    )
                }
                composable("profileWeightReminders") {
                    WeightReminderEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveWeightReminders,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onSaved = viewModel::showSaved,
                    )
                }
                composable("profileWorkoutReminders") {
                    WorkoutReminderEditorScreen(
                        store = state.store,
                        onSave = viewModel::saveWorkoutReminders,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onSaved = viewModel::showSaved,
                    )
                }
                composable("profileWeightUnits") {
                    WeightUnitsEditorScreen(
                        useKg = state.store.profile.weightDisplayKg,
                        onSave = viewModel::saveWeightDisplayKg,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onSaved = viewModel::showSaved,
                    )
                }
                composable("profileWaist") {
                    WaistEditorScreen(
                        waistInches = state.store.profile.waistInches,
                        onSave = viewModel::saveWaist,
                        onBack = { nav.popBackStack() },
                        onSaved = savedAndBack,
                    )
                }
                composable("profileTheme") {
                    ThemeEditorScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onSave = viewModel::saveProfile,
                        onBack = { nav.popBackStack() },
                        backLabel = profileEditorBackLabel(nav.previousBackStackEntry?.destination?.route),
                        onSaved = viewModel::showSaved,
                    )
                }
                composable("plans") {
                    val previous = nav.previousBackStackEntry?.destination?.route
                    PlansScreen(
                        state.store.plans,
                        onBack = { nav.popBackStack() },
                        backLabel = when {
                            previous == "workoutSettings" -> "Workout settings"
                            previous?.startsWith("workoutForm") == true -> "Workout"
                            previous == "workouts" -> "Workouts"
                            else -> "Profile"
                        },
                        onAdd = { nav.navigate("planForm/$NEW") },
                        onEdit = { nav.navigate("planForm/$it") },
                        onOpen = { nav.navigate("planDetail/$it") },
                    )
                }
                composable("planForm/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val existing = state.store.plans.find { it.id == id }
                    PlanFormScreen(
                        existing = existing,
                        missing = id != NEW && existing == null,
                        onBack = {
                            nav.popBackStack()
                            if (pendingNewPlanId != null) {
                                pendingNewPlanId = null
                                openStartSheet()
                            }
                        },
                        onSave = { name, description, typeId ->
                            val newPlanId = pendingNewPlanId ?: newId("plan")
                            val error = viewModel.savePlan(existing?.id, name, description, typeId, newPlanId)
                            if (error == null) {
                                nav.popBackStack()
                                viewModel.showSaved()
                                if (existing == null && pendingNewPlanId != null) {
                                    pendingNewPlanId = null
                                    startSheetSelected = newPlanId
                                    startSheetOpen = true
                                }
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
                        onEditPlan = { if (plan != null) nav.navigate("planForm/${plan.id}") },
                        onStartWorkout = { if (plan != null) startSession(plan.id) },
                    )
                }
                composable("activityForm/{planId}/{machineId}") { entry ->
                    val planId = entry.arguments?.getString("planId")
                    val machineId = entry.arguments?.getString("machineId")
                    val plan = state.store.plans.find { it.id == planId }
                    val machine = plan?.machines?.find { it.id == machineId }
                    ActivityFormScreen(
                        planMissing = plan == null,
                        activityMissing = machineId != NEW && machine == null,
                        existing = machine,
                        onBack = { nav.popBackStack() },
                        planName = plan?.name ?: "this plan",
                        onDelete = if (plan != null && machine != null) {
                            {
                                viewModel.deleteActivity(plan.id, machine.id)
                                nav.popBackStack()
                            }
                        } else {
                            null
                        },
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
    if (startSheetOpen) {
        PlanPickerSheet(
            plans = state.store.plans,
            workouts = state.store.workouts,
            selectedId = startSheetSelected,
            confirmLabel = "Start workout",
            onConfirm = startSession,
            onDismiss = { startSheetOpen = false },
            onCreatePlan = createPlanFromStartSheet,
            onManagePlans = { nav.navigate("plans") },
        )
    }
}

private fun profileEditorBackLabel(route: String?): String = when (route) {
    "bp" -> "Blood pressure"
    "bpHowTo" -> "Blood pressure"
    "weight" -> "Weight"
    "weightHowTo" -> "Weight"
    "workouts" -> "Workouts"
    "workoutHowTo" -> "Workouts"
    "workoutSettings" -> "Workout settings"
    "workoutForm/{id}" -> "Workout"
    "bpSettings" -> "Blood pressure settings"
    "weightSettings" -> "Weight settings"
    else -> "Profile"
}
