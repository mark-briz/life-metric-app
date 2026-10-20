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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.ui.bp.BloodPressureFormScreen
import com.markbriz.lifemetric.ui.bp.BloodPressureScreen
import com.markbriz.lifemetric.ui.components.GradientBackground
import com.markbriz.lifemetric.ui.home.HomeScreen
import com.markbriz.lifemetric.ui.profile.AboutScreen
import com.markbriz.lifemetric.ui.profile.ActivityFormScreen
import com.markbriz.lifemetric.ui.profile.DisclaimerScreen
import com.markbriz.lifemetric.ui.profile.PlanDetailScreen
import com.markbriz.lifemetric.ui.profile.PlanFormScreen
import com.markbriz.lifemetric.ui.profile.PlansScreen
import com.markbriz.lifemetric.ui.profile.ProfileScreen
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import com.markbriz.lifemetric.ui.weight.WeightFormScreen
import com.markbriz.lifemetric.ui.weight.WeightScreen
import com.markbriz.lifemetric.ui.welcome.WelcomeScreen
import com.markbriz.lifemetric.ui.workout.WorkoutDetailScreen
import com.markbriz.lifemetric.ui.workout.WorkoutEntryFormScreen
import com.markbriz.lifemetric.ui.workout.WorkoutFormScreen
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
                    HomeScreen(state.store, { nav.navigate("bp") }, { nav.navigate("weight") }, { nav.navigate("workouts") })
                }
                composable("bp") {
                    BloodPressureScreen(
                        state.store,
                        onAdd = { nav.navigate("bpForm/$NEW") },
                        onEdit = { nav.navigate("bpForm/$it") },
                        onDelete = viewModel::deleteBloodPressure,
                    )
                }
                composable("bpForm/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val existing = state.store.bloodPressureReadings.find { it.id == id }
                    BloodPressureFormScreen(
                        existing = existing,
                        missing = id != NEW && existing == null,
                        onBack = { nav.popBackStack() },
                        onSave = { date, time, sys, dia, hr ->
                            viewModel.saveBloodPressure(existing?.id, date, time, sys, dia, hr)
                            nav.popBackStack()
                        },
                    )
                }
                composable("weight") {
                    WeightScreen(
                        state.store,
                        onAdd = { nav.navigate("weightForm/$NEW") },
                        onEdit = { nav.navigate("weightForm/$it") },
                        onDelete = viewModel::deleteWeight,
                    )
                }
                composable("weightForm/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val existing = state.store.weightRecords.find { it.id == id }
                    val suggested = existing?.weight ?: latestWeight(state.store.weightRecords)?.weight ?: 150.0
                    WeightFormScreen(
                        existing = existing,
                        suggested = suggested,
                        missing = id != NEW && existing == null,
                        onBack = { nav.popBackStack() },
                        onSave = { date, time, weight ->
                            viewModel.saveWeight(existing?.id, date, time, weight)
                            nav.popBackStack()
                        },
                    )
                }
                composable("workouts") {
                    WorkoutsScreen(
                        state.store,
                        onAdd = {
                            viewModel.beginNewWorkout()
                            nav.navigate("workoutForm/$NEW")
                        },
                        onOpen = { nav.navigate("workoutDetail/$it") },
                        onEdit = {
                            viewModel.beginEditWorkout(it, backToDetail = false)
                            nav.navigate("workoutForm/$it")
                        },
                        onDelete = viewModel::deleteWorkout,
                    )
                }
                composable("workoutDetail/{id}") { entry ->
                    val id = entry.arguments?.getString("id")
                    val workout = state.store.workouts.find { it.id == id }
                    val plan = state.store.plans.find { it.id == workout?.planId }
                    WorkoutDetailScreen(
                        workout = workout,
                        plan = plan,
                        onBack = { nav.popBackStack() },
                        onAddEntry = { nav.navigate("workoutEntry/${workout?.id ?: NEW}/$NEW") },
                        onEditEntry = { item -> nav.navigate("workoutEntry/${workout?.id}/$item.id") },
                        onDeleteEntry = { item -> if (workout != null) viewModel.deleteWorkoutEntry(workout.id, item.id) },
                    )
                }
                composable("workoutForm/{id}") {
                    val draft = state.workoutDraft
                    WorkoutFormScreen(
                        store = state.store,
                        draft = draft,
                        missing = draft == null,
                        onBack = { nav.popBackStack() },
                        onDate = viewModel::updateDraftDate,
                        onPlan = viewModel::updateDraftPlan,
                        onAddEntry = { nav.navigate("workoutEntry/draft/$NEW") },
                        onEditEntry = { item -> nav.navigate("workoutEntry/draft/${item.id}") },
                        onRemoveEntry = viewModel::removeDraftEntry,
                        onSave = {
                            val error = viewModel.saveDraftWorkout()
                            if (error == null) nav.popBackStack()
                            error
                        },
                    )
                }
                composable("workoutEntry/{target}/{entryId}") { entry ->
                    val target = entry.arguments?.getString("target").orEmpty()
                    val entryId = entry.arguments?.getString("entryId").orEmpty()
                    val draft = state.workoutDraft
                    val fromDraft = target == "draft"
                    val planId = if (fromDraft) draft?.planId else state.store.workouts.find { it.id == target }?.planId
                    val plan = state.store.plans.find { it.id == planId }
                    val existing = when {
                        entryId == NEW -> null
                        fromDraft -> draft?.entries?.find { it.id == entryId }
                        else -> state.store.workouts.find { it.id == target }?.entries?.find { it.id == entryId }
                    }
                    WorkoutEntryFormScreen(
                        plan = plan,
                        existing = existing,
                        workouts = state.store.workouts,
                        currentWorkoutId = if (fromDraft) draft?.workoutId else target,
                        currentWorkoutDate = if (fromDraft) draft?.date.orEmpty() else state.store.workouts.find { it.id == target }?.date.orEmpty(),
                        currentEntries = if (fromDraft) draft?.entries.orEmpty() else state.store.workouts.find { it.id == target }?.entries.orEmpty(),
                        onBack = { nav.popBackStack() },
                        onSave = { saved ->
                            if (fromDraft) viewModel.upsertDraftEntry(saved)
                            else viewModel.upsertSavedEntry(target, saved)
                            nav.popBackStack()
                        },
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        store = state.store,
                        themeId = state.themeId,
                        onPreviewTheme = viewModel::previewTheme,
                        onRevertTheme = viewModel::revertThemePreview,
                        onSave = viewModel::saveProfile,
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
                composable("plans") {
                    PlansScreen(
                        state.store.plans,
                        onBack = { nav.popBackStack() },
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
                        onBack = { nav.popBackStack() },
                        onSave = { name, description, typeId ->
                            val error = viewModel.savePlan(existing?.id, name, description, typeId)
                            if (error == null) nav.popBackStack()
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
                    ActivityFormScreen(
                        planMissing = plan == null,
                        activityMissing = machineId != NEW && machine == null,
                        existing = machine,
                        onBack = { nav.popBackStack() },
                        onSave = { name, description ->
                            val error = if (planId == null) "Plan not found." else viewModel.saveActivity(planId, machine?.id, name, description)
                            if (error == null) nav.popBackStack()
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
