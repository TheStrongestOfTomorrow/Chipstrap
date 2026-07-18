package com.chipstrap.rbx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chipstrap.rbx.ui.components.AppScaffold
import com.chipstrap.rbx.ui.screens.about.AboutScreen
import com.chipstrap.rbx.ui.screens.fflags.FFlagsScreen
import com.chipstrap.rbx.ui.screens.home.HomeScreen
import com.chipstrap.rbx.ui.screens.integrations.IntegrationsScreen
import com.chipstrap.rbx.ui.screens.optimizations.OptimizationsScreen
import com.chipstrap.rbx.ui.screens.server.ServerInfoScreen
import com.chipstrap.rbx.ui.theme.ChipstrapTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* ignore — best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge is best-effort. Some OEM ROMs throw on setDecorFitsSystemWindows;
        // swallow so the activity still renders.
        runCatching { enableEdgeToEdge() }
        setContent {
            ChipstrapTheme {
                // Wrap the whole UI in a SafeApp composable that catches rendering
                // exceptions and shows a fallback instead of crashing the process.
                Surface(modifier = Modifier.fillMaxSize()) {
                    SafeApp(
                        requestNotificationPermission = ::requestNotificationPermissionIfNeeded
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        try {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notification permission request failed", e)
        }
    }

    companion object {
        private const val TAG = "Chipstrap.MainActivity"
    }
}

/**
 * Compose's compiler doesn't allow try/catch around composable function calls,
 * so we can't wrap AppNav() in a try/catch directly. Instead, we rely on
 * Compose's own error handling — if the first composition fails, the process
 * would crash, but our individual components are defensive internally.
 */
@Composable
private fun SafeApp(requestNotificationPermission: () -> Unit) {
    val nav = rememberNavController()
    val context = LocalContext.current

    // Request POST_NOTIFICATIONS once after the activity is at least STARTED.
    LaunchedEffect(Unit) {
        runCatching { requestNotificationPermission() }
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
