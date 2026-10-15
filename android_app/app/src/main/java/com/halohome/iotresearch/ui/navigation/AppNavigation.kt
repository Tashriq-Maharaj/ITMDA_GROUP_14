package com.halohome.iotresearch.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.halohome.iotresearch.data.api.NetworkModule
import com.halohome.iotresearch.data.models.AuditReportDto
import com.halohome.iotresearch.ui.screens.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Commissioning : Screen("commissioning")
    object ActiveHub : Screen("active_hub")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    var latestAuditReport by remember { mutableStateOf<AuditReportDto?>(null) }

    val startDestination = if (NetworkModule.authToken != null) {
        Screen.Commissioning.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Commissioning.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Commissioning.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Commissioning.route) {
            CommissioningAuditorScreen(
                onCommissionSuccess = { report ->
                    latestAuditReport = report
                    navController.navigate(Screen.ActiveHub.route) {
                        popUpTo(Screen.Commissioning.route) { inclusive = true }
                    }
                },
                onAbandoned = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Commissioning.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ActiveHub.route) {
            ActiveHubScreen(
                auditReport = latestAuditReport,
                onRecommission = {
                    navController.navigate(Screen.Commissioning.route)
                },
                onSignOut = {
                    NetworkModule.clearSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ActiveHub.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
