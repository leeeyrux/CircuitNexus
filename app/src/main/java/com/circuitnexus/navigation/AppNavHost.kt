package com.circuitnexus.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.circuitnexus.di.AppContainer
import com.circuitnexus.ui.home.HomeScreen
import com.circuitnexus.ui.learn.LearnScreen
import com.circuitnexus.ui.parts.PartsScreen
import com.circuitnexus.ui.settings.SettingsScreen
import com.circuitnexus.ui.theme.CircuitColors
import com.circuitnexus.ui.theme.CircuitShapes
import com.circuitnexus.ui.workbench.WorkbenchScreen
import com.circuitnexus.viewmodel.HomeViewModel
import com.circuitnexus.viewmodel.LearnViewModel
import com.circuitnexus.viewmodel.PartsViewModel
import com.circuitnexus.viewmodel.SettingsViewModel
import com.circuitnexus.viewmodel.WorkbenchViewModel

/**
 * Creación manual de ViewModels (sin frameworks extra).
 */
class ViewModelFactory(private val container: AppContainer) {

    fun createHomeViewModel(): HomeViewModel = HomeViewModel(container.repository)

    fun createLearnViewModel(): LearnViewModel = LearnViewModel()

    fun createPartsViewModel(): PartsViewModel = PartsViewModel()

    fun createSettingsViewModel(): SettingsViewModel =
        SettingsViewModel(container.settingsDataStore, container)

    fun createWorkbenchViewModel(circuitId: Long): WorkbenchViewModel =
        WorkbenchViewModel(
            repository = container.repository,
            settings = container.settingsDataStore,
            container = container,
            circuitId = circuitId,
            tutorialViewModel = container.tutorialViewModel,
            hapticManager = container.hapticManager
        )
}

@Composable
fun AppNavHost(
    container: AppContainer,
    startDestination: String = "home"
) {
    val navController = rememberNavController()
    val factory = remember { ViewModelFactory(container) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: startDestination
    val inWorkbench = currentRoute.startsWith("workbench")

    Box(modifier = Modifier.fillMaxSize()) {

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                val homeViewModel = remember { factory.createHomeViewModel() }
                HomeScreen(
                    viewModel = homeViewModel,
                    onNewCircuit = { circuitId -> navController.navigate("workbench/$circuitId") },
                    onOpenCircuit = { circuitId -> navController.navigate("workbench/$circuitId") }
                )
            }

            composable(
                route = "workbench/{circuitId}",
                arguments = listOf(navArgument("circuitId") { type = NavType.LongType })
            ) { entry ->
                val circuitId = entry.arguments?.getLong("circuitId") ?: 0L
                val workbenchViewModel = remember(circuitId) { factory.createWorkbenchViewModel(circuitId) }
                WorkbenchScreen(
                    viewModel = workbenchViewModel,
                    onNavigateUp = { navController.popBackStack() }
                )
            }

            composable("learn") {
                val learnViewModel = remember { factory.createLearnViewModel() }
                LearnScreen(viewModel = learnViewModel)
            }
            composable("parts") {
                val partsViewModel = remember { factory.createPartsViewModel() }
                PartsScreen(viewModel = partsViewModel)
            }
            composable("settings") {
                val settingsViewModel = remember { factory.createSettingsViewModel() }
                SettingsScreen(viewModel = settingsViewModel)
            }
        }

        if (!inWorkbench) {
            FloatingPillNav(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Barra de navegación flotante en forma de cápsula oscura.
 */
@Composable
private fun FloatingPillNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(bottom = 18.dp)
            .background(CircuitColors.Surface, CircuitShapes.PillShape)
            .border(1.dp, CircuitColors.Outline, CircuitShapes.PillShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            Row(
                modifier = Modifier
                    .background(
                        if (selected) CircuitColors.Emerald.copy(alpha = 0.18f) else CircuitColors.Surface,
                        CircleShape
                    )
                    .clickable { onNavigate(item.route) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.icon, fontSize = 14.sp)
                if (selected) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        item.label,
                        color = CircuitColors.Emerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W700
                    )
                }
            }
        }
    }
}

/** Pestañas del menú inferior */
data class NavItem(
    val route: String,
    val label: String,
    val icon: String
)

val NAV_ITEMS = listOf(
    NavItem("home", "Mis Circuitos", "📁"),
    NavItem("learn", "Aprender", "📚"),
    NavItem("parts", "Piezas", "🧩"),
    NavItem("settings", "Ajustes", "⚙️")
)
