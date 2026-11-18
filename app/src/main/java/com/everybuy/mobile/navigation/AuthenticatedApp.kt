package com.everybuy.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.window.core.layout.WindowSizeClass
import com.everybuy.mobile.R
import com.everybuy.mobile.data.AuthUser
import com.everybuy.mobile.data.StaffProfile
import com.everybuy.mobile.ui.audit.AuditScreen
import com.everybuy.mobile.ui.auth.AppTheme
import com.everybuy.mobile.ui.components.LocalAppHazeState
import com.everybuy.mobile.ui.components.LocalBottomChromeHeight
import com.everybuy.mobile.ui.components.LocalNavigationRailVisible
import com.everybuy.mobile.ui.components.glass
import com.everybuy.mobile.ui.components.hazeBlurEnabled
import com.everybuy.mobile.ui.customers.CustomerDetailScreen
import com.everybuy.mobile.ui.customers.CustomerFormScreen
import com.everybuy.mobile.ui.customers.CustomersScreen
import com.everybuy.mobile.ui.expenses.ExpensesScreen
import com.everybuy.mobile.ui.inventory.InventoryScreen
import com.everybuy.mobile.ui.inventory.ProductDetailScreen
import com.everybuy.mobile.ui.inventory.ProductFormScreen
import com.everybuy.mobile.ui.more.MoreScreen
import com.everybuy.mobile.ui.reports.OverviewScreen
import com.everybuy.mobile.ui.sales.RecordSaleScreen
import com.everybuy.mobile.ui.sales.SaleDetailScreen
import com.everybuy.mobile.ui.sales.SalesScreen
import com.everybuy.mobile.ui.settings.CatalogScreen
import com.everybuy.mobile.ui.settings.SettingsScreen
import com.everybuy.mobile.ui.settings.SuppliersAdminScreen
import com.everybuy.mobile.ui.settings.TeamScreen
import com.everybuy.mobile.ui.stock.ReceiveStockScreen
import com.everybuy.mobile.ui.stock.StockMovementDetailScreen
import com.everybuy.mobile.ui.stock.StockMovementsScreen
import com.everybuy.mobile.ui.stocktakes.StockTakeDetailScreen
import com.everybuy.mobile.ui.stocktakes.StockTakesScreen
import dev.chrisbanes.haze.rememberHazeState

private data class MainDestination(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val destinations = listOf(
    MainDestination("inventory", R.string.nav_stock, Icons.Rounded.Inventory2),
    MainDestination("sales", R.string.nav_sales, Icons.Rounded.ShoppingBag),
    MainDestination("overview", R.string.nav_overview, Icons.Rounded.Assessment),
    MainDestination("customers", R.string.nav_customers, Icons.Rounded.People),
    MainDestination("more", R.string.nav_more, Icons.Rounded.MoreHoriz),
)

private val mainRoutes = destinations.map { it.route }.toSet()

private val fullScreenRoutes = setOf(
    "inventory/product/{productId}",
    "inventory/new",
    "inventory/product/{productId}/edit",
    "inventory/product/{productId}/receive",
    "movements",
    "movements/{movementId}",
    "movements/product/{productId}",
    "stocktakes",
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthenticatedApp(
    user: AuthUser,
    staff: StaffProfile,
    theme: AppTheme,
    onSignOut: () -> Unit,
    setTheme: (AppTheme) -> Unit,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showChrome = destination?.route !in fullScreenRoutes
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val widthAtLeastMedium = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val widthAtLeastExpanded = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val useRail = showChrome && widthAtLeastMedium
    val hazeState = rememberHazeState(blurEnabled = hazeBlurEnabled())

    fun navigateTo(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) { saveState = true }
        }
    }

    CompositionLocalProvider(
        LocalAppHazeState provides hazeState,
        LocalBottomChromeHeight provides if (showChrome && !useRail) 80.dp else 0.dp,
        LocalNavigationRailVisible provides useRail,
    ) {
        if (useRail) {
            val railState = rememberWideNavigationRailState(
                initialValue = if (widthAtLeastExpanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed,
            )
            LaunchedEffect(widthAtLeastExpanded) {
                if (widthAtLeastExpanded) railState.expand() else railState.collapse()
            }
            Row(Modifier.fillMaxSize()) {
                WideNavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .glass(hazeState),
                    state = railState,
                    colors = WideNavigationRailDefaults.colors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    windowInsets = WindowInsets.systemBars,
                ) {
                    NavigationRailItems(
                        selectedRoute = destination?.route,
                        selectedHierarchy = destination,
                        railExpanded = widthAtLeastExpanded,
                        onNavigate = ::navigateTo,
                    )
                }
                NavGraph(
                    navController = navController,
                    user = user,
                    staff = staff,
                    theme = theme,
                    onSignOut = onSignOut,
                    setTheme = setTheme,
                    modifier = Modifier.weight(1f),
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                NavGraph(
                    navController = navController,
                    user = user,
                    staff = staff,
                    theme = theme,
                    onSignOut = onSignOut,
                    setTheme = setTheme,
                    modifier = Modifier.fillMaxSize(),
                    onOpenSettings = { navController.navigate("settings") },
                )
                if (showChrome) {
                    ShortNavigationBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .glass(hazeState),
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        destinations.forEach { item ->
                            val selected = destination?.hierarchy?.any { it.route == item.route } == true
                            val label = stringResource(item.labelRes)
                            ShortNavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = { Icon(item.icon, contentDescription = label) },
                                label = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationRailItems(
    selectedRoute: String?,
    selectedHierarchy: androidx.navigation.NavDestination?,
    railExpanded: Boolean,
    onNavigate: (String) -> Unit,
) {
    destinations.forEach { item ->
        val selected = selectedHierarchy?.hierarchy?.any { it.route == item.route } == true
        val label = stringResource(item.labelRes)
        WideNavigationRailItem(
            selected = selected,
            onClick = { onNavigate(item.route) },
            icon = { Icon(item.icon, contentDescription = label) },
            label = {
                Text(
                    text = label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            railExpanded = railExpanded,
        )
    }
}

@Composable
private fun NavGraph(
    navController: androidx.navigation.NavHostController,
    user: AuthUser,
    staff: StaffProfile,
    theme: AppTheme,
    onSignOut: () -> Unit,
    setTheme: (AppTheme) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier,
) {
    NavHost(navController, startDestination = "inventory", modifier = modifier) {
        composable("inventory") {
            InventoryScreen(
                user = user,
                isOwner = staff.role == "Owner",
                onOpenSettings = onOpenSettings,
                onSessionExpired = onSignOut,
                onProductSelected = { navController.navigate("inventory/product/$it") },
                onAddProduct = { navController.navigate("inventory/new") },
            )
        }
        composable("overview") {
            OverviewScreen(
                user = user,
                onOpenSettings = onOpenSettings,
                onOpenSale = { id -> navController.navigate("sales/$id") },
                onOpenProduct = { id -> navController.navigate("inventory/product/$id") },
                onOpenSales = {
                    navController.navigate("sales") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                    }
                },
                onOpenCustomers = {
                    navController.navigate("customers") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                    }
                },
                onOpenStock = {
                    navController.navigate("inventory") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                    }
                },
                onRecordSale = { navController.navigate("sales/new") },
                onSessionExpired = onSignOut,
            )
        }
        composable("more") {
            MoreScreen(
                userName = user.name.ifBlank { user.email },
                onOpenHistory = { navController.navigate("movements") },
                onOpenCounts = { navController.navigate("stocktakes") },
                onOpenExpenses = { navController.navigate("expenses") },
                onOpenAudit = { navController.navigate("audit") },
                onOpenSettings = onOpenSettings,
            )
        }
        composable("expenses") { ExpensesScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
        composable("audit") { AuditScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
        composable("settings") {
            SettingsScreen(
                userName = user.name.ifBlank { user.email },
                userEmail = user.email,
                role = staff.role,
                theme = theme,
                onThemeChange = setTheme,
                isOwner = staff.role == "Owner",
                onBack = { navController.popBackStack() },
                onOpenTeam = { navController.navigate("settings/team") },
                onOpenCatalog = { navController.navigate("settings/catalog") },
                onOpenSuppliers = { navController.navigate("settings/suppliers") },
                onSignOut = onSignOut,
                onSessionExpired = onSignOut,
            )
        }
        composable("settings/team") { TeamScreen(currentUserId = user.id, onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
        composable("settings/catalog") { CatalogScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
        composable("settings/suppliers") { SuppliersAdminScreen(onBack = { navController.popBackStack() }, onSessionExpired = onSignOut) }
        composable("movements") {
            StockMovementsScreen(
                isOwner = staff.role == "Owner",
                onBack = { navController.popBackStack() },
                onOpenSettings = onOpenSettings,
                onSessionExpired = onSignOut,
                onProductSelected = { id -> navController.navigate("inventory/product/$id") },
                onMovementSelected = { id -> navController.navigate("movements/$id") },
            )
        }
        composable("movements/product/{productId}", arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
            StockMovementsScreen(
                isOwner = staff.role == "Owner",
                onBack = { navController.popBackStack() },
                onOpenSettings = onOpenSettings,
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
        composable("sales") {
            SalesScreen(
                userName = user.name.ifBlank { user.email },
                onOpenSale = { id -> navController.navigate("sales/$id") },
                onRecordSale = { navController.navigate("sales/new") },
                onOpenSettings = onOpenSettings,
                onSessionExpired = onSignOut,
            )
        }
        composable("sales/new") { RecordSaleScreen(onBack = { navController.popBackStack() }, onViewMovements = { navController.navigate("movements") { launchSingleTop = true } }, onSessionExpired = onSignOut) }
        composable("sales/{saleId}", arguments = listOf(navArgument("saleId") { type = NavType.StringType })) { entry ->
            val saleId = entry.arguments?.getString("saleId")
            if (saleId != null) SaleDetailScreen(saleId, onBack = { navController.popBackStack() }, onCustomerSelected = { id -> navController.navigate("customers/$id") }, onSessionExpired = onSignOut)
        }
        composable("customers") {
            CustomersScreen(
                userName = user.name.ifBlank { user.email },
                onOpenCustomer = { id -> navController.navigate("customers/$id") },
                onAddCustomer = { navController.navigate("customers/new") },
                onOpenSettings = onOpenSettings,
                onSessionExpired = onSignOut,
            )
        }
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
        composable("stocktakes") {
            StockTakesScreen(
                onOpenStockTake = { id -> navController.navigate("stocktakes/$id") },
                onBack = { navController.popBackStack() },
                onOpenSettings = onOpenSettings,
                onSessionExpired = onSignOut,
            )
        }
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
