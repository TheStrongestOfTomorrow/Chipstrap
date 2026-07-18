package com.chipstrap.rbx.ui.screens.home

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chipstrap.rbx.data.SettingsStore
import com.chipstrap.rbx.fflags.repository.FFlagRepository
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
            // Each step is in its own runCatching so a failure in one doesn't
            // abort the others — and the UI never sees an exception.
            runCatching {
                val pkg = RobloxPackages.resolvePreferred(context)
                _isInstalled.value = RobloxPackages.isInstalled(context, pkg)
                _robloxVersion.value = RobloxPackages.versionName(context, pkg)
            }.onFailure { Log.w(TAG, "resolvePreferred failed", it) }

            runCatching {
                fflags.load()
                _fflagsCount.value = fflags.count()
            }.onFailure { Log.w(TAG, "fflags.load failed", it) }

            runCatching {
                _activePreset.value = SettingsStore.lastPreset.first()
            }.onFailure { Log.w(TAG, "lastPreset.first failed", it) }

            runCatching {
                _lastLaunch.value = SettingsStore.lastLaunchTs.first()
            }.onFailure { Log.w(TAG, "lastLaunchTs.first failed", it) }

            runCatching {
                val id = SettingsStore.injectionStrategy.first()
                _strategyLabel.value = when (id) {
                    "shizuku" -> "Shizuku"
                    "root" -> "Root"
                    "virtual" -> "Virtual space"
                    else -> "Local profile"
                }
            }.onFailure { Log.w(TAG, "injectionStrategy.first failed", it) }
        }
    }

    fun launch(context: Context) {
        if (_isLaunching.value) return
        _isLaunching.value = true
        runCatching {
            val intent = Intent(context, LauncherForegroundService::class.java).apply {
                action = LauncherForegroundService.ACTION_LAUNCH
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }.onFailure { Log.e(TAG, "startForegroundService failed", it) }
        // Optimistic — the real launch happens in the service. Reset the spinner after a bit.
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            _isLaunching.value = false
            refresh(context)
        }
    }

    companion object {
        private const val TAG = "Chipstrap.HomeVM"
    }
}
