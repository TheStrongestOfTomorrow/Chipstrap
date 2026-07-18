package com.chipstrap.rbx.ui.screens.optimizations

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

class OptimizationsViewModel : ViewModel() {

    data class State(
        val cpuGovernor: Boolean = false,
        val killBg: Boolean = false,
        val clearCache: Boolean = true,
        val disableDoze: Boolean = true,
        val gpuTuning: Boolean = true,
        val btAudio: Boolean = false,
        val memoryTrim: Boolean = true,
        val dns: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                combine(
                    SettingsStore.optCpuGovernor, SettingsStore.optKillBg,
                    SettingsStore.optClearCache, SettingsStore.optDisableDoze,
                    SettingsStore.optGpuTuning, SettingsStore.optBtAudio,
                    SettingsStore.optMemoryTrim, SettingsStore.optDns
                ) { values ->
                    State(
                        cpuGovernor = values[0] as Boolean,
                        killBg = values[1] as Boolean,
                        clearCache = values[2] as Boolean,
                        disableDoze = values[3] as Boolean,
                        gpuTuning = values[4] as Boolean,
                        btAudio = values[5] as Boolean,
                        memoryTrim = values[6] as Boolean,
                        dns = values[7] as Boolean
                    )
                }.catch { e ->
                    Log.e(TAG, "Optimizations flow combine failed, keeping defaults", e)
                }.collect { _state.value = it }
            } catch (e: Throwable) {
                Log.e(TAG, "OptimizationsViewModel init failed", e)
            }
        }
    }

    fun setCpuGovernor(v: Boolean) = safeSet("setCpuGovernor") { SettingsStore.setOptCpuGovernor(v) }
    fun setKillBg(v: Boolean) = safeSet("setKillBg") { SettingsStore.setOptKillBg(v) }
    fun setClearCache(v: Boolean) = safeSet("setClearCache") { SettingsStore.setOptClearCache(v) }
    fun setDisableDoze(v: Boolean) = safeSet("setDisableDoze") { SettingsStore.setOptDisableDoze(v) }
    fun setGpuTuning(v: Boolean) = safeSet("setGpuTuning") { SettingsStore.setOptGpuTuning(v) }
    fun setBtAudio(v: Boolean) = safeSet("setBtAudio") { SettingsStore.setOptBtAudio(v) }
    fun setMemoryTrim(v: Boolean) = safeSet("setMemoryTrim") { SettingsStore.setOptMemoryTrim(v) }
    fun setDns(v: Boolean) = safeSet("setDns") { SettingsStore.setOptDns(v) }

    private fun safeSet(name: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try { block() } catch (e: Throwable) { Log.e(TAG, "$name failed", e) }
        }
    }

    companion object {
        private const val TAG = "Chipstrap.OptVM"
    }
}
