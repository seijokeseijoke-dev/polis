package se.polishandelser.app

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import se.polishandelser.app.ui.EventDetailScreen
import se.polishandelser.app.ui.EventsScreen
import se.polishandelser.app.ui.EventsViewModel
import se.polishandelser.app.ui.OrtScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme { AppNavigation() }
        }
    }
}

@Composable
private fun AppNavigation() {
    val nav = rememberNavController()
    // Skapas här (aktivitetsnivå) så att lista och detaljvy delar samma data.
    val vm: EventsViewModel = viewModel()

    NavHost(navController = nav, startDestination = "ort") {
        composable("ort") {
            OrtScreen(vm = vm, onOrtSelected = { nav.navigate("events/${Uri.encode(it)}") })
        }
        composable(
            route = "events/{ort}",
            arguments = listOf(navArgument("ort") { type = NavType.StringType })
        ) { entry ->
            val ort = entry.arguments?.getString("ort").orEmpty()
            EventsScreen(
                vm = vm,
                ort = ort,
                onBack = { nav.popBackStack() },
                onEventClick = { nav.navigate("event/${it.id}") }
            )
        }
        composable(
            route = "event/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: -1
            EventDetailScreen(event = vm.eventById(id), onBack = { nav.popBackStack() })
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
