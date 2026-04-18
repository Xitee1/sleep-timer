package dev.xitee.sleeptimer.core.service.shizuku

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
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
        if (computeState() != State.PermissionRequired) return
        if (Shizuku.isPreV11()) return
        Shizuku.requestPermission(REQUEST_CODE)
    }

    private fun computeState(): State {
        if (!isShizukuInstalled()) return State.NotInstalled
        val running = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
        if (!running) return State.NotRunning
        if (Shizuku.isPreV11()) return State.PermissionRequired
        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) { false }
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
