package com.chipstrap.rbx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chipstrap.rbx.ui.screens.about.AboutScreen
import com.chipstrap.rbx.ui.screens.fflags.FFlagsScreen
import com.chipstrap.rbx.ui.screens.home.HomeScreen
import com.chipstrap.rbx.ui.screens.integrations.IntegrationsScreen
import com.chipstrap.rbx.ui.screens.optimizations.OptimizationsScreen
import com.chipstrap.rbx.ui.screens.server.ServerInfoScreen
import com.chipstrap.rbx.ui.theme.ChipstrapTheme
import com.chipstrap.rbx.ui.components.AppScaffold

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore — best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Wrap edge-to-edge in try/catch — it can throw on some OEM ROMs with custom
        // window decorators that don't honor setDecorFitsSystemWindows.
        runCatching { enableEdgeToEdge() }
        super.onCreate(savedInstanceState)
        setContent {
            ChipstrapTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(
                        requestNotificationPermission = ::requestNotificationPermissionIfNeeded
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        // Only launch if the activity is at least STARTED — ActivityResultLauncher.launch()
        // throws IllegalStateException if called before STARTED state. We check isStarted
        // (or its successors) to be safe.
        if (!granted && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            try {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: IllegalStateException) {
                // Activity not yet started — Compose LaunchedEffect will retry on next composition.
            }
        }
    }
}

@Composable
private fun AppNav(requestNotificationPermission: () -> Unit) {
    val nav = rememberNavController()

    // Request POST_NOTIFICATIONS once after the activity is at least STARTED.
    // This must happen from Compose (not onCreate) to avoid the
    // "launch() cannot be called before the activity is at least STARTED" crash.
    LaunchedEffect(Unit) {
        requestNotificationPermission()
    }

    AppScaffold(navController = nav) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") { HomeScreen(nav) }
            composable("fflags") { FFlagsScreen(nav) }
            composable("optimizations") { OptimizationsScreen(nav) }
            composable("integrations") { IntegrationsScreen(nav) }
            composable("server") { ServerInfoScreen(nav) }
            composable("about") { AboutScreen(nav) }
        }
    }
}
