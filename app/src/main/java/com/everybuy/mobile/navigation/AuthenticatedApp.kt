package com.everybuy.mobile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ShoppingBag
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
import com.everybuy.mobile.ui.audit.AuditScreen
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.ui.customers.CustomerDetailScreen
import com.everybuy.mobile.ui.customers.CustomerFormScreen
import com.everybuy.mobile.ui.customers.CustomersScreen
import com.everybuy.mobile.ui.expenses.ExpensesScreen
import com.everybuy.mobile.ui.inventory.InventoryScreen
import com.everybuy.mobile.ui.inventory.ProductDetailScreen
import com.everybuy.mobile.ui.inventory.ProductFormScreen
import com.everybuy.mobile.ui.reports.OverviewScreen
import com.everybuy.mobile.ui.sales.RecordSaleScreen
import com.everybuy.mobile.ui.sales.SaleDetailScreen
import com.everybuy.mobile.ui.sales.SalesScreen
import com.everybuy.mobile.ui.settings.CatalogScreen
import com.everybuy.mobile.ui.settings.SettingsScreen
import com.everybuy.mobile.ui.settings.SuppliersAdminScreen
import com.everybuy.mobile.ui.settings.TeamScreen
import com.everybuy.mobile.ui.stock.StockMovementDetailScreen
import com.everybuy.mobile.ui.stock.StockMovementsScreen
import com.everybuy.mobile.ui.stock.ReceiveStockScreen
import com.everybuy.mobile.ui.stocktakes.StockTakeDetailScreen
import com.everybuy.mobile.ui.stocktakes.StockTakesScreen

private data class MainDestination(val route: String, val label: String)
private val destinations = listOf(
    MainDestination("inventory", "Stock"),
    MainDestination("overview", "Overview"),
    MainDestination("movements", "History"),
    MainDestination("stocktakes", "Counts"),
    MainDestination("sales", "Sales"),
    MainDestination("customers", "Customers"),
)

@Composable fun AuthenticatedApp(user: AuthUser, staff: StaffProfile, theme: AppTheme, onSignOut: () -> Unit, setTheme: (AppTheme) -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showTabs = destination?.route !in setOf(
        "inventory/product/{productId}",
        "inventory/new",
        "inventory/product/{productId}/edit",
        "inventory/product/{productId}/receive",
        "movements/{movementId}",
        "movements/product/{productId}",
        "stocktakes/{stockTakeId}",
        "sales/new",
        "sales/{saleId}",
        "customers/new",
        "customers/{customerId}",
        "customers/{customerId}/edit",
        "expenses",
        "audit",
        "settings",
        "settings/team",
        "settings/catalog",
        "settings/suppliers",
    )
    Scaffold(bottomBar = {
        if (showTabs) {
        NavigationBar {
            destinations.forEach { item ->
                val selected = destination?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = { navController.navigate(item.route) { launchSingleTop = true; restoreState = true; popUpTo(navController.graph.startDestinationId) { saveState = true } } },
                    icon = { Icon(imageVector = when (item.route) { "inventory" -> Icons.Rounded.Inventory2; "overview" -> Icons.Rounded.Assessment; "stocktakes" -> Icons.Rounded.Checklist; "sales" -> Icons.Rounded.ShoppingBag; "customers" -> Icons.Rounded.People; else -> Icons.Rounded.History }, contentDescription = item.label) },
                    label = { Text(item.label) },
                )
            }
        }
        }
    }) { insets ->
        NavHost(navController, startDestination = "inventory", modifier = Modifier.padding(insets)) {
            composable("inventory") { InventoryScreen(user, theme, onSignOut, onSignOut, setTheme, staff.role == "Owner", { navController.navigate("inventory/product/$it") }, { navController.navigate("inventory/new") }) }
            composable("overview") { OverviewScreen(user, theme, onSignOut, onSignOut, setTheme, onOpenSale = { id -> navController.navigate("sales/$id") }, onOpenProduct = { id -> navController.navigate("inventory/product/$id") }, onRecordSale = { navController.navigate("sales/new") }, onOpenExpenses = { navController.navigate("expenses") }, onOpenAudit = { navController.navigate("audit") }, onOpenSettings = { navController.navigate("settings") }) }
            composable("expenses") { ExpensesScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
            composable("audit") { AuditScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
            composable("settings") {
                SettingsScreen(
                    userName = user.name,
                    isOwner = staff.role == "Owner",
                    onBack = { navController.popBackStack() },
                    onOpenTeam = { navController.navigate("settings/team") },
                    onOpenCatalog = { navController.navigate("settings/catalog") },
                    onOpenSuppliers = { navController.navigate("settings/suppliers") },
                    onSessionExpired = onSignOut,
                )
            }
            composable("settings/team") { TeamScreen(currentUserId = user.id, onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
            composable("settings/catalog") { CatalogScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
            composable("settings/suppliers") { SuppliersAdminScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
            composable("movements") { StockMovementsScreen(isOwner = staff.role == "Owner", onSessionExpired = onSignOut, onProductSelected = { id -> navController.navigate("inventory/product/$id") }, onMovementSelected = { id -> navController.navigate("movements/$id") }) }
            composable("movements/product/{productId}", arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                StockMovementsScreen(
                    isOwner = staff.role == "Owner",
                    onSessionExpired = onSignOut,
                    onProductSelected = { id -> navController.navigate("inventory/product/$id") },
                    onMovementSelected = { id -> navController.navigate("movements/$id") },
                    initialProductId = entry.arguments?.getString("productId"),
                )
            }
            composable("movements/{movementId}", arguments = listOf(navArgument("movementId") { type = NavType.StringType })) { entry ->
                val movementId = entry.arguments?.getString("movementId")
                if (movementId != null) StockMovementDetailScreen(movementId, onBack = { navController.popBackStack() }, onProductSelected = { id -> navController.navigate("inventory/product/$id") }, onSessionExpired = onSignOut)
            }
            composable("sales") { SalesScreen(onOpenSale = { id -> navController.navigate("sales/$id") }, onRecordSale = { navController.navigate("sales/new") }, onSessionExpired = onSignOut) }
            composable("sales/new") { RecordSaleScreen(onBack = { navController.popBackStack() }, onViewMovements = { navController.navigate("movements") { launchSingleTop = true } }, onSessionExpired = onSignOut) }
            composable("sales/{saleId}", arguments = listOf(navArgument("saleId") { type = NavType.StringType })) { entry ->
                val saleId = entry.arguments?.getString("saleId")
                if (saleId != null) SaleDetailScreen(saleId, onBack = { navController.popBackStack() }, onCustomerSelected = { id -> navController.navigate("customers/$id") }, onSessionExpired = onSignOut)
            }
            composable("customers") { CustomersScreen(onOpenCustomer = { id -> navController.navigate("customers/$id") }, onAddCustomer = { navController.navigate("customers/new") }, onSessionExpired = onSignOut) }
            composable("customers/new") { CustomerFormScreen(null, onBack = { navController.popBackStack() }, onSaved = { id -> navController.navigate("customers/$id") { popUpTo("customers") } }, onSessionExpired = onSignOut) }
            composable("customers/{customerId}", arguments = listOf(navArgument("customerId") { type = NavType.StringType })) { entry ->
                val customerId = entry.arguments?.getString("customerId")
                if (customerId != null) CustomerDetailScreen(
                    id = customerId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("customers/$customerId/edit") },
                    onOpenSale = { saleId -> navController.navigate("sales/$saleId") },
                    onSessionExpired = onSignOut,
                )
            }
            composable("customers/{customerId}/edit", arguments = listOf(navArgument("customerId") { type = NavType.StringType })) { entry ->
                val customerId = entry.arguments?.getString("customerId")
                if (customerId != null) CustomerFormScreen(customerId, onBack = { navController.popBackStack() }, onSaved = { id -> navController.navigate("customers/$id") { popUpTo("customers") } }, onSessionExpired = onSignOut)
            }
            composable("stocktakes") { StockTakesScreen(onOpenStockTake = { id -> navController.navigate("stocktakes/$id") }, onSessionExpired = onSignOut) }
            composable("stocktakes/{stockTakeId}", arguments = listOf(navArgument("stockTakeId") { type = NavType.StringType })) { entry ->
                val stockTakeId = entry.arguments?.getString("stockTakeId")
                if (stockTakeId != null) StockTakeDetailScreen(
                    id = stockTakeId,
                    isOwner = staff.role == "Owner",
                    currentUserId = user.id,
                    onBack = { navController.popBackStack() },
                    onEditProduct = { productId -> navController.navigate("inventory/product/$productId/edit") },
                    onSessionExpired = onSignOut,
                )
            }
            composable("inventory/product/{productId}", arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                val productId = entry.arguments?.getString("productId")
                if (productId != null) ProductDetailScreen(productId, isOwner = staff.role == "Owner", onBack = { navController.popBackStack() }, onEdit = { navController.navigate("inventory/product/$productId/edit") }, onReceiveStock = { navController.navigate("inventory/product/$productId/receive") }, onViewMovements = { navController.navigate("movements/product/$productId") }, onSessionExpired = onSignOut)
            }
            composable("inventory/product/{productId}/receive", arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                val productId = entry.arguments?.getString("productId")
                ReceiveStockScreen(productId, onBack = { navController.popBackStack() }, onSessionExpired = onSignOut)
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
