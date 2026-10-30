package com.example.tiffinapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tiffinapp.ui.screens.detail.KitchenDetailScreen
import com.example.tiffinapp.ui.screens.list.KitchenListScreen
import com.example.tiffinapp.ui.screens.paywall.PaywallScreen
import com.example.tiffinapp.ui.viewmodel.TiffinViewModel

/** Sealed class representing all navigation destinations. */
sealed class Screen(val route: String) {
    data object KitchenList   : Screen("kitchen_list")
    data object KitchenDetail : Screen("kitchen_detail/{kitchenId}") {
        fun createRoute(kitchenId: String) = "kitchen_detail/$kitchenId"
    }
    data object Paywall : Screen("paywall")
}

/**
 * Root composable that sets up the [NavHost] and wires up the shared [TiffinViewModel].
 * The paywall is layered as a navigation destination so it sits above the back-stack naturally.
 */
@Composable
fun AppNavigation(
    viewModel: TiffinViewModel = viewModel()
) {
    val navController = rememberNavController()
    val showPaywall   by viewModel.showPaywall.collectAsState()
    val paywallTrigger by viewModel.paywallTrigger.collectAsState()

    // Navigate to paywall whenever the flag flips true
    androidx.compose.runtime.LaunchedEffect(showPaywall) {
        if (showPaywall) {
            navController.navigate(Screen.Paywall.route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController   = navController,
        startDestination = Screen.KitchenList.route
    ) {

        composable(Screen.KitchenList.route) {
            KitchenListScreen(
                viewModel    = viewModel,
                onNavigateToDetail = { kitchen ->
                    viewModel.onKitchenSelected(kitchen)
                    navController.navigate(Screen.KitchenDetail.createRoute(kitchen.id))
                }
            )
        }

        composable(
            route = Screen.KitchenDetail.route,
            arguments = listOf(navArgument("kitchenId") { type = NavType.StringType })
        ) {
            KitchenDetailScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Screen.Paywall.route) {
            PaywallScreen(
                trigger         = paywallTrigger,
                onSubscribe     = {
                    viewModel.completeSubscription()
                    // Pop paywall off the stack
                    navController.popBackStack()
                },
                onDismiss       = {
                    viewModel.dismissPaywall()
                    navController.popBackStack()
                }
            )
        }
    }
}
