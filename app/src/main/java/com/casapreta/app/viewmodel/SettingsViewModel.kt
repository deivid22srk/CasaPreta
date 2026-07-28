package com.casapreta.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.casapreta.app.data.SettingsRepository
import com.casapreta.app.shizuku.ShizukuManager
import com.casapreta.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Central ViewModel. Holds:
 * - persisted theme mode (Modo Noturno)
 * - live Shizuku status / permission state for the UI
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val themeMode: StateFlow<ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    // --- Shizuku state ----------------------------------------------------

    private val shizuku = ShizukuManager()

    private val _shizukuStatus = MutableStateFlow<ShizukuManager.Status>(ShizukuManager.Status.NotRunning)
    val shizukuStatus: StateFlow<ShizukuManager.Status> = _shizukuStatus.asStateFlow()

    private val _shizukuPermission = MutableStateFlow(false)
    val shizukuPermission: StateFlow<Boolean> = _shizukuPermission.asStateFlow()

    init {
        shizuku.register()

        // React to binder coming/going
        shizkuBinderListeners()

        // Initial snapshot
        refreshShizukuState()
    }

    private fun shizkuBinderListeners() {
        shizuku.onBinderReceived { refreshShizukuState() }
        shizuku.onBinderDead { _shizukuStatus.value = ShizukuManager.Status.NotRunning }
    }

    fun refreshShizukuState() {
        _shizukuStatus.value = shizuku.currentStatus()
        _shizukuPermission.value = shizuku.hasPermission()
    }

    fun requestShizukuPermission() {
        // Hook a one-shot listener so the StateFlows update when the user answers
        val listener: (Int, Boolean) -> Unit = { _, granted ->
            _shizukuPermission.value = granted
        }
        shizuku.addPermissionResultListener(listener)
        shizuku.requestPermission()
        // The listener is removed after a delay to keep things simple.
        viewModelScope.launch {
            kotlinx.coroutines.delay(10_000)
            shizuku.removePermissionResultListener(listener)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    override fun onCleared() {
        shizuku.unregister()
        super.onCleared()
    }
}
