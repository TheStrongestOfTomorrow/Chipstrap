package com.chipstrap.rbx.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chipstrap.rbx.data.SettingsStore
import com.chipstrap.rbx.fflags.repository.FFlagRepository
import com.chipstrap.rbx.fflags.strategies.StrategyResolver
import com.chipstrap.rbx.roblox.RobloxPackages
import com.chipstrap.rbx.service.LauncherForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val fflags = FFlagRepository()

    private val _isInstalled = MutableStateFlow(false)
    val isInstalled: StateFlow<Boolean> = _isInstalled.asStateFlow()

    private val _fflagsCount = MutableStateFlow(0)
    val fflagsCount: StateFlow<Int> = _fflagsCount.asStateFlow()

    private val _activePreset = MutableStateFlow("")
    val activePreset: StateFlow<String> = _activePreset.asStateFlow()

    private val _lastLaunch = MutableStateFlow("")
    val lastLaunch: StateFlow<String> = _lastLaunch.asStateFlow()

    private val _isLaunching = MutableStateFlow(false)
    val isLaunching: StateFlow<Boolean> = _isLaunching.asStateFlow()

    private val _strategyLabel = MutableStateFlow("—")
    val strategyLabel: StateFlow<String> = _strategyLabel.asStateFlow()

    private val _robloxVersion = MutableStateFlow<String?>(null)
    val robloxVersion: StateFlow<String?> = _robloxVersion.asStateFlow()

    fun refresh(context: Context) {
        viewModelScope.launch {
            val pkg = RobloxPackages.resolvePreferred(context)
            _isInstalled.value = RobloxPackages.isInstalled(context, pkg)
            _robloxVersion.value = RobloxPackages.versionName(context, pkg)
            runCatching { fflags.load() }
            _fflagsCount.value = fflags.count()
            _activePreset.value = SettingsStore.lastPreset.first()
            _lastLaunch.value = SettingsStore.lastLaunchTs.first()
            val id = SettingsStore.injectionStrategy.first()
            _strategyLabel.value = when (id) {
                "shizuku" -> "Shizuku"
                "root" -> "Root"
                "virtual" -> "Virtual space"
                else -> "Local profile"
            }
        }
    }

    fun launch(context: Context) {
        if (_isLaunching.value) return
        _isLaunching.value = true
        val intent = Intent(context, LauncherForegroundService::class.java).apply {
            action = LauncherForegroundService.ACTION_LAUNCH
        }
        runCatching {
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
        // Optimistic — the real launch happens in the service. Reset the spinner after a bit.
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _isLaunching.value = false
            refresh(context)
        }
    }
}
