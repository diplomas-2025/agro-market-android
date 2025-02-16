package ru.driving.agro_market

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.driving.agro_market.api.RetrofitClient
import ru.driving.agro_market.api.SharedPrefManager
import ru.driving.agro_market.ui.screens.AuthScreen
import ru.driving.agro_market.ui.screens.MainScreen
import ru.driving.agro_market.ui.screens.ProductDetailsScreen
import ru.driving.agro_market.ui.theme.AgromarketTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RetrofitClient.initialize(this)
        val sharedPrefManager = SharedPrefManager(this)

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            AgromarketTheme {
                NavHost(
                    navController = navController,
                    startDestination = if (sharedPrefManager.getAccessToken() == null) "auth" else "main"
                ) {
                    composable("auth") {
                        AuthScreen(navController = navController)
                    }

                    composable("main") {
                        MainScreen(navController)
                    }

                    composable("products/{id}", arguments = listOf(
                        navArgument("id") {
                            type = NavType.IntType
                        }
                    )) {
                        ProductDetailsScreen(
                            id = it.arguments!!.getInt("id"),
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}