package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.PosRepository
import com.example.ui.components.PosBottomNavigation
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.delivery.DeliveryScreen
import com.example.ui.invoices.InvoicesScreen
import com.example.ui.menu.MenuScreen
import com.example.ui.pos.PosScreen
import com.example.ui.pos.PosViewModel
import com.example.ui.pos.PosViewModelFactory
import com.example.ui.profile.RestaurantProfileScreen
import com.example.ui.summary.SalesSummaryScreen
import com.example.ui.tables.TablesScreen
import com.example.ui.theme.AppTheme
import com.example.ui.workers.WorkersScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppDatabase.getDatabase(this)
        val repository = PosRepository(database.posDao())
        
        setContent {
            AppTheme {
                val viewModel: PosViewModel = viewModel(factory = PosViewModelFactory(repository))
                val context = LocalContext.current
                
                // Initialize sample data and auto-restore offline backup if available
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (viewModel.hasAutoBackup(context)) {
                        viewModel.restoreAutoBackup(context)
                    }
                    viewModel.addSampleProducts()
                    viewModel.addJuiceProducts()
                    viewModel.addSampleWorkersIfEmpty()
                }

                AppNavigation(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: PosViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Pos.route,
        Screen.Menu.route,
        Screen.Tables.route,
        Screen.Workers.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                PosBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Screen.Dashboard.route, 
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToPos = { navController.navigate(Screen.Pos.route) },
                    onNavigateToDelivery = { navController.navigate(Screen.Delivery.route) },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) },
                    onNavigateToSalesSummary = { navController.navigate(Screen.SalesSummary.route) },
                    onNavigateToMenu = { navController.navigate(Screen.Menu.route) },
                    onNavigateToTables = { navController.navigate(Screen.Tables.route) },
                    onNavigateToWorkers = { navController.navigate(Screen.Workers.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Pos.route) {
                PosScreen(viewModel = viewModel)
            }
            composable(Screen.Menu.route) {
                MenuScreen(viewModel = viewModel)
            }
            composable(Screen.Tables.route) {
                TablesScreen(viewModel = viewModel)
            }
            composable(Screen.Workers.route) {
                WorkersScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) {
                RestaurantProfileScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Delivery.route) {
                DeliveryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SalesSummary.route) {
                SalesSummaryScreen(
                    viewModel = viewModel, 
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Invoices.route) {
                InvoicesScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
