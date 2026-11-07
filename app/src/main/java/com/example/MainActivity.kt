package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.PosRepository
import com.example.data.auth.UserProfile
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
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
import com.example.ui.theme.*
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
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.authState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                
                // Initialize sample data and auto-restore offline backup if available
                LaunchedEffect(Unit) {
                    if (viewModel.hasAutoBackup(context)) {
                        viewModel.restoreAutoBackup(context)
                    }
                    viewModel.addSampleProducts()
                    viewModel.addJuiceProducts()
                    viewModel.addSampleWorkersIfEmpty()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(targetState = authState, label = "AuthFlow") { state ->
                        when (state) {
                            is AuthState.Loading -> {
                                LoadingSplashScreen()
                            }
                            is AuthState.Unauthenticated -> {
                                AuthNavigation(authViewModel = authViewModel)
                            }
                            is AuthState.Authenticated -> {
                                AppNavigation(
                                    viewModel = viewModel,
                                    authViewModel = authViewModel,
                                    userProfile = state.profile
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CleanBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BrandPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PointOfSale,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            CircularProgressIndicator(
                color = BrandSecondary,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Connecting Secure Session...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AuthNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    // AuthState listener in AuthViewModel automatically transitions state to Authenticated
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    // AuthState listener in AuthViewModel automatically transitions state to Authenticated
                }
            )
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: PosViewModel,
    authViewModel: AuthViewModel,
    userProfile: UserProfile
) {
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
                    userProfile = userProfile,
                    onLogout = { authViewModel.logout() },
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
                    userProfile = userProfile,
                    onLogout = { authViewModel.logout() },
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
