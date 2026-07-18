package com.chipstrap.rbx.ui.screens.integrations

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chipstrap.rbx.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class IntegrationsViewModel : ViewModel() {

    data class State(
        val strategy: String = "shizuku",
        val preferredApp: String = "global",
        val customPackage: String = ""
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // combine() will throw if any of the source flows throws — typically
            // when DataStore hits an IOException on the first read. Wrap in
            // .catch() so we keep the default State instead of crashing the
            // composition.
            try {
                combine(
                    SettingsStore.injectionStrategy,
                    SettingsStore.preferredRobloxApp,
                    SettingsStore.customRobloxPackage
                ) { strat, app, custom ->
                    State(strat, app, custom)
                }.catch { e ->
                    Log.e(TAG, "Flow combine failed, keeping default state", e)
                    // emit nothing — keep the default _state.value
                }.collect { _state.value = it }
            } catch (e: Throwable) {
                Log.e(TAG, "IntegrationsViewModel init failed", e)
                // _state.value remains the default State()
            }
        }
    }

    fun setStrategy(id: String) {
        viewModelScope.launch {
            try {
                SettingsStore.setInjectionStrategy(id)
            } catch (e: Throwable) {
                Log.e(TAG, "setStrategy($id) failed", e)
            }
        }
    }

    fun setPreferredApp(id: String) {
        viewModelScope.launch {
            try {
                SettingsStore.setPreferredRobloxApp(id)
            } catch (e: Throwable) {
                Log.e(TAG, "setPreferredApp($id) failed", e)
            }
        }
    }

    fun setCustomPackage(pkg: String) {
        viewModelScope.launch {
            try {
                SettingsStore.setCustomRobloxPackage(pkg)
            } catch (e: Throwable) {
                Log.e(TAG, "setCustomPackage($pkg) failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "Chipstrap.IntegrationsVM"
    }
}
