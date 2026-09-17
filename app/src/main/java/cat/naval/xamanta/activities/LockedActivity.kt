package cat.naval.xamanta.activities

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import cat.naval.xamanta.R
import cat.naval.xamanta.policy.kiosk.KioskManager
import cat.naval.xamanta.policy.kiosk.KioskPolicy
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.isDeviceOwner
import cat.naval.xamanta.services.PolicyService

class LockedActivity : Activity() {

    companion object {
        const val TAG = "LockedActivity"

        private const val LOCK_TASK_MAX_ATTEMPTS = 3
        private const val LOCK_TASK_RETRY_DELAY_MS = 1_000L
    }

    private lateinit var policyStore: PolicyStore
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SDK_INT >= VERSION_CODES.TIRAMISU) BackBlocker.register(this)
        policyStore = PolicyStore(applicationContext)

        if (!isDeviceOwner()) {
            Log.e(TAG, "Device admin is not device owner — finishing")
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()

        handler.removeCallbacksAndMessages(null)

        val kioskPackage = kioskInForce() ?: run {
            Log.i(TAG, "no kiosk in force — leaving lockdown")
            exitKiosk()
            return
        }

        if (policyStore.isKioskCrashLooping(kioskPackage)) {
            Log.w(TAG, "kiosk '$kioskPackage' is degraded — showing escape screen, not relaunching")
            showDegraded(kioskPackage)
            return
        }
        if (kioskProcessInErrorState(kioskPackage) && policyStore.recordKioskCrash()) {
            enterDegraded(kioskPackage)
            return
        }

        if (hasWindowFocus()) enterKiosk()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return

        val kioskPackage = kioskInForce() ?: return
        if (policyStore.isKioskCrashLooping(kioskPackage)) return
        enterKiosk()
    }

    @Suppress("OVERRIDE_DEPRECATION", "MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
    }

    override fun onDestroy() {
        super.onDestroy()
        if (SDK_INT >= VERSION_CODES.TIRAMISU) BackBlocker.unregister(this)
        handler.removeCallbacksAndMessages(null)
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    private object BackBlocker : OnBackInvokedCallback {
        override fun onBackInvoked() = Unit

        fun register(activity: Activity) = activity.onBackInvokedDispatcher
            .registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY, this)

        fun unregister(activity: Activity) = activity.onBackInvokedDispatcher
            .unregisterOnBackInvokedCallback(this)
    }

    private fun enterKiosk(attempt: Int = 0) {
        if (attempt == 0) handler.removeCallbacksAndMessages(null)

        val kioskPackage = kioskInForce() ?: run {
            Log.i(TAG, "no kiosk in force — leaving lockdown")
            exitKiosk()
            return
        }

        val launch = packageManager.getLaunchIntentForPackage(kioskPackage)
        if (launch == null) {
            Log.e(TAG, "Launch intent not found for '$kioskPackage' — falling back to SyncActivity")
            startActivity(Intent(this, SyncActivity::class.java))
            return
        }

        launch.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        Log.d(TAG, "Launching kiosk app '$kioskPackage' (attempt ${attempt + 1})")

        try {
            if (SDK_INT >= VERSION_CODES.P) {
                val options = ActivityOptions.makeBasic().apply { setLockTaskEnabled(true) }
                startActivity(launch, options.toBundle())
            } else if (SDK_INT >= VERSION_CODES.M) {
                if (!isInLockTask() && dpm.isLockTaskPermitted(packageName)) {
                    try {
                        startLockTask()
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "startLockTask rejected: ${e.message}")
                    }
                }
                startActivity(launch)
            } else {
                startActivity(launch)
            }
            suppressLaunchAnimation()
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Kiosk activity not resolvable: ${e.message}")
            startActivity(Intent(this, SyncActivity::class.java))
            return
        }

        policyStore.setLaunchedKiosk(kioskPackage)
        verifyPinned(kioskPackage, attempt)
    }

    private fun verifyPinned(kioskPackage: String, attempt: Int) {
        handler.postDelayed({
            if (isInLockTask()) {
                Log.d(TAG, "lock task confirmed")
                policyStore.setKioskUnpinned(null)
            } else if (attempt + 1 < LOCK_TASK_MAX_ATTEMPTS) {
                Log.w(TAG, "not in lock task after attempt ${attempt + 1} — retrying")
                enterKiosk(attempt + 1)
            } else {
                Log.e(
                    TAG,
                    "'$kioskPackage' did not enter lock task after $LOCK_TASK_MAX_ATTEMPTS " +
                            "attempts (platform last reported '${policyStore.pinnedPackage()}')"
                )
                policyStore.setKioskUnpinned(kioskPackage)
                PolicyService.requestReconcile(this, "kiosk not pinned")
            }
        }, LOCK_TASK_RETRY_DELAY_MS)
    }

    private fun exitKiosk() {
        if (isInLockTask() && SDK_INT >= VERSION_CODES.M) {
            try {
                stopLockTask()
                Log.d(TAG, "Stopped lock task")
            } catch (e: SecurityException) {
                Log.w(TAG, "stopLockTask threw: ${e.message}")
            }
        }
        finishAndRemoveTask()
        packageManager.setComponentEnabledSetting(
            ComponentName(applicationContext, LockedActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun kioskProcessInErrorState(kioskPackage: String): Boolean {
        val processName = runCatching {
            packageManager.getPackageInfo(kioskPackage, 0).applicationInfo?.processName
        }.getOrNull() ?: return false
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.processesInErrorState?.any { it.processName == processName } == true
    }

    private fun enterDegraded(kioskPackage: String) {
        Log.e(TAG, "kiosk '$kioskPackage' crash loop detected — entering degraded mode")
        policyStore.markKioskCrashLooping(kioskPackage)
        PolicyService.requestReconcile(this, "kiosk crash loop")
        showDegraded(kioskPackage)
    }

    private fun showDegraded(kioskPackage: String) {
        handler.removeCallbacksAndMessages(null)
        if (SDK_INT >= VERSION_CODES.M && !isInLockTask() && dpm.isLockTaskPermitted(packageName)) {
            runCatching { startLockTask() }
                .onFailure { Log.w(TAG, "startLockTask (degraded) threw: ${it.message}") }
        }
        setContentView(R.layout.activity_locked_degraded)
        findViewById<Button>(R.id.retry_button).setOnClickListener {
            Log.i(TAG, "user retrying kiosk '$kioskPackage'")
            policyStore.clearKioskCrashState()
            enterKiosk()
        }
    }

    private fun kioskInForce(): String? {
        val policy = policyStore.load()?.devicePolicy ?: return null
        val kioskPackage = KioskPolicy.app(policy)?.packageName ?: return null
        if (!dpm.isLockTaskPermitted(kioskPackage)) {
            Log.i(TAG, "'$kioskPackage' is no longer allowlisted for lock task")
            return null
        }
        return kioskPackage
    }

    private fun suppressLaunchAnimation() {
        if (SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
        }
    }

    private fun isInLockTask(): Boolean = KioskManager.isLockTaskActive(this)
}
