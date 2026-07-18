package com.chipstrap.rbx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.chipstrap.rbx.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(navController: NavController, content: @Composable () -> Unit) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Track the current route so we can highlight the active drawer item.
    // NavController.currentBackStackEntryAsState() would be ideal, but to
    // avoid pulling in extra deps we read it lazily on each composition.
    val currentRoute = remember { mutableStateOf("home") }
    try {
        val entry = navController.currentBackStackEntry
        entry?.destination?.route?.let { currentRoute.value = it }
    } catch (_: Throwable) { /* ignore */ }

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
            // ─── Drawer sheet ────────────────────────────────────────────
            // The drawerContent lambda must lay out its own children. Without
            // a Column wrapper, all NavigationDrawerItem children would be
            // placed at position (0,0) and overlap — which is the bug the
            // user reported ("everything mashed into one line").
            //
            // ModalDrawerSheet gives us a properly-padded container with the
            // Material 3 drawer background color. Inside it, a Column stacks
            // the header + items vertically with proper spacing.
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                // ── Header ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ── Nav items ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentRoute.value == item.route,
                            icon = { Icon(item.icon, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
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
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { runCatching { scope.launch { drawerState.open() } } }) {
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
    val icon: ImageVector
)
