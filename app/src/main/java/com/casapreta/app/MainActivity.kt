package com.casapreta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.casapreta.app.ui.screens.HomeScreen
import com.casapreta.app.ui.screens.SettingsScreen
import com.casapreta.app.ui.theme.CasaPretaTheme
import com.casapreta.app.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SettingsViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            CasaPretaTheme(themeMode = themeMode) {
                AppNavigation(viewModel)
            }
        }
    }
}

@Composable
private fun AppNavigation(viewModel: SettingsViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateSettings = { nav.navigate("settings") },
                viewModel = viewModel
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateHome = { nav.popBackStack() },
                viewModel = viewModel
            )
        }
    }
}
