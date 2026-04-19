package dev.xitee.sleeptimer.core.service.shizuku

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    enum class State {
        NotInstalled,        // Shizuku app package not present
        NotRunning,          // Installed but binder not alive — user must start the service in the Shizuku app
        PermissionRequired,  // Running but our app has not been granted access yet
        Ready,               // All good — we can execute commands
    }

    private val _state = MutableStateFlow(computeState())
    val state: StateFlow<State> = _state.asStateFlow()

    // Flips true the first time the Shizuku binder connects this process.
    // Before that, `pingBinder()` returning false is ambiguous — it may mean
    // "not running" or simply "not yet received".
    private val _binderReceivedOnce = MutableStateFlow(false)

    private val binderReceived = Shizuku.OnBinderReceivedListener {
        _binderReceivedOnce.value = true
        refresh()
    }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    fun refresh() {
        _state.value = computeState()
    }

    fun isReady(): Boolean = _state.value == State.Ready

    fun requestPermission() {
        if (_state.value != State.PermissionRequired) return
        if (Shizuku.isPreV11()) return
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Exception) {
            refresh()
        }
    }

    /**
     * Waits until the Shizuku binder connects (or timeout) before returning the
     * state. Call this for startup checks so an early-boot `pingBinder()` race
     * doesn't masquerade as NotRunning.
     */
    suspend fun awaitInitialState(timeoutMs: Long = 1500): State {
        if (!isShizukuInstalled()) return State.NotInstalled
        if (!_binderReceivedOnce.value) {
            withTimeoutOrNull(timeoutMs) {
                _binderReceivedOnce.first { it }
            }
        }
        refresh()
        return _state.value
    }

    private fun computeState(): State {
        if (!isShizukuInstalled()) return State.NotInstalled
        val running = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        if (!running) return State.NotRunning
        if (Shizuku.isPreV11()) return State.PermissionRequired
        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
        return if (granted) State.Ready else State.PermissionRequired
    }

    private fun isShizukuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val REQUEST_CODE = 9871
    }
}
