package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard)
    object Pos : Screen("pos", "POS", Icons.Filled.PointOfSale)
    object Menu : Screen("menu", "Menu", Icons.AutoMirrored.Filled.List)
    object Tables : Screen("tables", "Tables", Icons.Filled.TableRestaurant)
    object Workers : Screen("workers", "Staff", Icons.Filled.People)
    object Profile : Screen("profile", "Profile")
    object Delivery : Screen("delivery", "Delivery")
    object Invoices : Screen("invoices", "Invoices")
    object SalesSummary : Screen("sales_summary", "Analytics")
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Pos,
    Screen.Menu,
    Screen.Tables,
    Screen.Workers
)
