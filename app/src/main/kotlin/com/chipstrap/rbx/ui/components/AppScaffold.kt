package com.chipstrap.rbx.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.chipstrap.rbx.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(navController: NavController, content: @Composable () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val items = listOf(
        NavItem("home", R.string.nav_home, Icons.Default.Home),
        NavItem("fflags", R.string.nav_fflags, Icons.Default.Flag),
        NavItem("optimizations", R.string.nav_optimizations, Icons.Default.Speed),
        NavItem("integrations", R.string.nav_integrations, Icons.Default.Memory),
        NavItem("server", R.string.nav_server_info, Icons.Default.Cloud),
        NavItem("about", R.string.nav_about, Icons.Default.Info)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(stringResource(item.labelRes)) },
                    selected = false,
                    icon = { Icon(item.icon, contentDescription = null) },
                    onClick = {
                        runCatching { scope.launch { drawerState.close() } }
                        runCatching {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                popUpTo("home")
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { inner ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(inner)) {
                content()
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
