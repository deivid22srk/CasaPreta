package com.casapreta.app.shizuku

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import com.casapreta.app.aidl.IAppManagerService

/**
 * Wrapper around the Shizuku API (v13.1.5).
 *
 * Responsibilities:
 *  1. Track the binder lifecycle (alive / dead) so the UI can react.
 *  2. Check and request the runtime permission that Shizuku requires (per-app grant).
 *  3. Bind the UserService (running as uid 0/2000) and expose its AIDL stub.
 *
 * Reference: https://github.com/RikkaApps/Shizuku-API
 */
class ShizukuManager {

    /** Visible binder state for the UI. */
    sealed class Status {
        data object NotInstalled : Status()        // Shizuku/Sui not present
        data object NotRunning   : Status()        // Installed but server not started
        data object Running      : Status()        // Binder alive, ready to use
    }

    // --- Binder lifecycle listeners ----------------------------------------

    private val binderReceivedListeners = mutableListOf<() -> Unit>()
    private val binderDeadListeners     = mutableListOf<() -> Unit>()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        binderReceivedListeners.forEach { it() }
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        binderDeadListeners.forEach { it() }
    }

    /**
     * Must be called once from the Activity's onCreate (or any Application scope).
     * Registers Shizuku binder listeners; they survive until [unregister].
     */
    fun register() {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    fun unregister() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        permissionResultListeners.clear()
    }

    fun onBinderReceived(listener: () -> Unit) { binderReceivedListeners.add(listener) }
    fun onBinderDead(listener: () -> Unit)     { binderDeadListeners.add(listener) }

    // --- Status & permission ----------------------------------------------

    /** True if either Shizuku or Sui is installed and the binder is alive. */
    fun isBinderAlive(): Boolean =
        try { Shizuku.pingBinder() } catch (_: Throwable) { false }

    fun currentStatus(): Status {
        return if (isBinderAlive()) Status.Running else Status.NotRunning
    }

    /** True if the user already granted the per-app Shizuku permission. */
    fun hasPermission(): Boolean =
        try {
            if (Shizuku.isPreV11()) false
            else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) { false }

    /** True if the user previously denied with "don't ask again". */
    fun shouldShowRationale(): Boolean =
        try { Shizuku.shouldShowRequestPermissionRationale() } catch (_: Throwable) { false }

    // --- Permission request ------------------------------------------------

    private val permissionResultListeners = mutableListOf<(Int, Boolean) -> Unit>()
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        val granted = grantResult == PackageManager.PERMISSION_GRANTED
        permissionResultListeners.forEach { it(requestCode, granted) }
    }

    fun addPermissionResultListener(listener: (Int, Boolean) -> Unit) {
        if (permissionResultListeners.isEmpty()) {
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        permissionResultListeners.add(listener)
    }

    fun removePermissionResultListener(listener: (Int, Boolean) -> Unit) {
        permissionResultListeners.remove(listener)
        if (permissionResultListeners.isEmpty()) {
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
    }

    /** Trigger the system permission dialog for Shizuku. */
    fun requestPermission(requestCode: Int = DEFAULT_REQUEST_CODE) {
        if (!Shizuku.isPreV11()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    // --- UserService binding ----------------------------------------------

    /**
     * The UserService runs in a separate process under the Shizuku identity.
     * It exposes [IAppManagerService] which can hide/unhide packages via
     * `PackageManager.setApplicationEnabledSetting`.
     */
    private fun userServiceArgs(): Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(
            ComponentName(
                "com.casapreta.app",
                "com.casapreta.app.shizuku.AppManagerService"
            )
        )
            .tag("app_manager_service")
            .version(1)
            .daemon(false)
            .processNameSuffix("app_manager")
            .debuggable(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)

    fun bindUserService(connection: ServiceConnection) {
        Shizuku.bindUserService(userServiceArgs(), connection)
    }

    fun unbindUserService(connection: ServiceConnection, remove: Boolean = true) {
        Shizuku.unbindUserService(userServiceArgs(), connection, remove)
    }

    companion object {
        const val DEFAULT_REQUEST_CODE = 1001
    }
}

/**
 * AIDL stub for the privileged UserService. Lives in the Shizuku server process.
 */
class AppManagerService : IAppManagerService.Stub() {

    private fun pm(): PackageManager =
        android.app.ActivityThread.currentApplication().packageManager

    override fun hidePackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            pm().setApplicationEnabledSetting(
                packageName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                0
            )
            true
        } catch (_: Throwable) { false }
    }

    override fun unhidePackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            pm().setApplicationEnabledSetting(
                packageName,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                0
            )
            true
        } catch (_: Throwable) { false }
    }

    override fun isPackageHidden(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            val state = pm().getApplicationEnabledSetting(packageName)
            state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        } catch (_: Throwable) { false }
    }

    override fun getPrivilegedUid(): Int =
        try { Shizuku.getUid() } catch (_: Throwable) { -1 }
}
