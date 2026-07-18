package com.chipstrap.rbx.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.chipstrap.rbx.R
import com.chipstrap.rbx.roblox.RobloxPackages
import com.chipstrap.rbx.service.LauncherForegroundService
import com.chipstrap.rbx.ui.screens.home.HomeViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: NavController, vm: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.refresh(context) }

    val isInstalled by vm.isInstalled.collectAsState()
    val fflagsCount by vm.fflagsCount.collectAsState()
    val activePreset by vm.activePreset.collectAsState()
    val lastLaunch by vm.lastLaunch.collectAsState()
    val isLaunching by vm.isLaunching.collectAsState()
    val strategyLabel by vm.strategyLabel.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Status card
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isInstalled) stringResource(R.string.home_roblox_installed)
                           else stringResource(R.string.home_roblox_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.home_fflags_count, fflagsCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.home_active_preset) + ": " +
                        (activePreset.ifBlank { stringResource(R.string.home_active_preset_none) }),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.home_last_launch) + ": " +
                        (lastLaunch.ifBlank { stringResource(R.string.home_never_launched) }),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Strategy card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_profile_card_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.home_profile_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = strategyLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = {
                    runCatching { nav.navigate("integrations") }
                }) {
                    Text(stringResource(R.string.home_profile_card_title))
                }
            }
        }

        // Launch buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    scope.launch { vm.launch(context) }
                },
                enabled = isInstalled && !isLaunching,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.home_apply_and_launch))
                }
            }
        }

        if (!isInstalled) {
            Text(
                stringResource(R.string.err_not_installed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
