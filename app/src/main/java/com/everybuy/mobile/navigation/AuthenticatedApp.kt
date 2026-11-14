package com.everybuy.mobile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.everybuy.mobile.data.AuthUser
import com.everybuy.mobile.data.StaffProfile
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.ui.inventory.InventoryScreen
import com.everybuy.mobile.ui.inventory.ProductDetailScreen
import com.everybuy.mobile.ui.inventory.ProductFormScreen
import com.everybuy.mobile.ui.reports.OverviewScreen
import com.everybuy.mobile.ui.stock.StockMovementsScreen

private data class MainDestination(val route: String, val label: String)
private val destinations = listOf(MainDestination("inventory", "Stock"), MainDestination("overview", "Overview"), MainDestination("movements", "Movements"))

@Composable fun AuthenticatedApp(user: AuthUser, staff: StaffProfile, theme: AppTheme, onSignOut: () -> Unit, setTheme: (AppTheme) -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showTabs = destination?.route !in setOf("inventory/product/{productId}", "inventory/new", "inventory/product/{productId}/edit")
    Scaffold(bottomBar = {
        if (showTabs) {
        NavigationBar {
            destinations.forEach { item ->
                val selected = destination?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = { navController.navigate(item.route) { launchSingleTop = true; restoreState = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } },
                    icon = { Icon(imageVector = when (item.route) { "inventory" -> Icons.Rounded.Inventory2; "overview" -> Icons.Rounded.Assessment; else -> Icons.Rounded.History }, contentDescription = item.label) },
                    label = { Text(item.label) },
                )
            }
        }
        }
    }) { insets ->
        NavHost(navController, startDestination = "inventory", modifier = Modifier.padding(insets)) {
            composable("inventory") { InventoryScreen(user, theme, onSignOut, onSignOut, setTheme, staff.role == "Owner", { navController.navigate("inventory/product/$it") }, { navController.navigate("inventory/new") }) }
            composable("overview") { OverviewScreen(user, theme, onSignOut, onSignOut, setTheme) }
            composable("movements") { StockMovementsScreen(onSessionExpired = onSignOut, onProductSelected = { id -> navController.navigate("inventory/product/$id") }) }
            composable("inventory/product/{productId}", arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                val productId = entry.arguments?.getString("productId")
                if (productId != null) ProductDetailScreen(productId, isOwner = staff.role == "Owner", onBack = { navController.popBackStack() }, onEdit = { navController.navigate("inventory/product/$productId/edit") }, onSessionExpired = onSignOut)
            }
            composable("inventory/new") {
                ProductFormScreen(null, onBack = { navController.popBackStack() }, onSaved = { id -> navController.navigate("inventory/product/$id") { popUpTo("inventory") } }, onSessionExpired = onSignOut)
            }
            composable("inventory/product/{productId}/edit", arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                val productId = entry.arguments?.getString("productId")
                if (productId != null) ProductFormScreen(productId, onBack = { navController.popBackStack() }, onSaved = { id -> navController.navigate("inventory/product/$id") { popUpTo("inventory") } }, onSessionExpired = onSignOut)
            }
        }
    }
}
