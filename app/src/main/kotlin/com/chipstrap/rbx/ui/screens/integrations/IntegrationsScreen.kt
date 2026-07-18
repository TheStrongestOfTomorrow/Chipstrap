package com.chipstrap.rbx.ui.screens.integrations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.chipstrap.rbx.R
import com.chipstrap.rbx.shizuku.ShizukuManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IntegrationsScreen(nav: NavController, vm: IntegrationsViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    // Poll Shizuku status every 2 seconds so the UI reflects whether the user
    // started Shizuku or granted permission outside our app.
    var shizukuStatus by remember { mutableStateOf(ShizukuStatus.NOT_INSTALLED) }
    LaunchedEffect(Unit) {
        while (true) {
            shizukuStatus = runCatching {
                when {
                    !ShizukuManager.isShizukuInstalled(context) -> ShizukuStatus.NOT_INSTALLED
                    !ShizukuManager.isBinderAlive() -> ShizukuStatus.NOT_RUNNING
                    !ShizukuManager.hasPermission() -> ShizukuStatus.NO_PERMISSION
                    else -> ShizukuStatus.READY
                }
            }.getOrDefault(ShizukuStatus.NOT_INSTALLED)
            delay(2000)
        }
    }

    // Local text-field state for the custom package — saves to DataStore on a
    // 500ms debounce instead of every keystroke.
    var customPackageText by remember(state.customPackage) {
        mutableStateOf(state.customPackage)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.home_profile_card_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.home_profile_card_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // ─── Strategy picker ──────────────────────────────────────────────
        listOf(
            Triple("shizuku", R.string.strategy_shizuku, R.string.strategy_shizuku_desc),
            Triple("root", R.string.strategy_root, R.string.strategy_root_desc),
            Triple("virtual", R.string.strategy_virtual, R.string.strategy_virtual_desc),
            Triple("local", R.string.strategy_local, R.string.strategy_local_desc)
        ).forEach { (id, titleRes, descRes) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = state.strategy == id,
                        onClick = { runCatching { scope.launch { vm.setStrategy(id) } } }
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(descRes), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ─── Shizuku status card (only shown when Shizuku is selected) ────
        if (state.strategy == "shizuku") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Shizuku status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    val (statusText, statusColor) = when (shizukuStatus) {
                        ShizukuStatus.READY -> stringResource(R.string.shizuku_status_running) to MaterialTheme.colorScheme.primary
                        ShizukuStatus.NOT_RUNNING -> stringResource(R.string.shizuku_status_not_running) to MaterialTheme.colorScheme.error
                        ShizukuStatus.NO_PERMISSION -> stringResource(R.string.shizuku_status_no_permission) to MaterialTheme.colorScheme.error
                        ShizukuStatus.NOT_INSTALLED -> stringResource(R.string.shizuku_status_not_installed) to MaterialTheme.colorScheme.error
                    }
                    Text(statusText, color = statusColor, style = MaterialTheme.typography.bodyMedium)

                    when (shizukuStatus) {
                        ShizukuStatus.NOT_INSTALLED -> {
                            Text(stringResource(R.string.shizuku_permission_rationale),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = {
                                runCatching {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            }) { Text(stringResource(R.string.shizuku_install)) }
                        }
                        ShizukuStatus.NOT_RUNNING -> {
                            Text("Open Shizuku and start the service (via wireless debugging or ADB).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = {
                                runCatching {
                                    val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    if (intent != null) context.startActivity(intent)
                                }
                            }) { Text("Open Shizuku") }
                        }
                        ShizukuStatus.NO_PERMISSION -> {
                            Text(stringResource(R.string.shizuku_permission_rationale),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = {
                                runCatching { ShizukuManager.requestPermission() }
                            }) { Text(stringResource(R.string.shizuku_request_permission)) }
                        }
                        ShizukuStatus.READY -> {
                            // Nothing — everything is good.
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text(stringResource(R.string.preferred_roblox_app), style = MaterialTheme.typography.titleMedium)
        listOf(
            "global" to R.string.roblox_global,
            "vng" to R.string.roblox_vng,
            "custom" to R.string.roblox_custom
        ).forEach { (id, labelRes) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = state.preferredApp == id,
                        onClick = { runCatching { scope.launch { vm.setPreferredApp(id) } } }
                    )
                    Text(stringResource(labelRes), modifier = Modifier.weight(1f).padding(top = 4.dp))
                }
            }
        }

        if (state.preferredApp == "custom") {
            OutlinedTextField(
                value = customPackageText,
                onValueChange = { newValue ->
                    customPackageText = newValue
                    scope.launch {
                        delay(500)
                        runCatching { vm.setCustomPackage(newValue) }
                    }
                },
                label = { Text("com.roblox.client…") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private enum class ShizukuStatus { NOT_INSTALLED, NOT_RUNNING, NO_PERMISSION, READY }
