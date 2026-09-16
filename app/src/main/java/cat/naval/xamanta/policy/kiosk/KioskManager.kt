package cat.naval.xamanta.policy.kiosk

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import cat.naval.xamanta.activities.LockedActivity
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.homeIntentFilter
import cat.naval.xamanta.models.KioskCustomization
import cat.naval.xamanta.models.PowerButtonActions
import cat.naval.xamanta.models.StatusBarMode
import cat.naval.xamanta.models.SystemNavigation

object KioskManager {

    @SuppressLint("InlinedApi")
    const val DEFAULT_LOCK_TASK_FEATURES =
        DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS or
                DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO

    @SuppressLint("InlinedApi")
    fun invalidLockTaskFeatures(features: Int): String? {
        val home = features and DevicePolicyManager.LOCK_TASK_FEATURE_HOME != 0
        if (features and DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW != 0 && !home) {
            return "LOCK_TASK_FEATURE_OVERVIEW requires LOCK_TASK_FEATURE_HOME"
        }
        if (features and DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS != 0 && !home) {
            return "LOCK_TASK_FEATURE_NOTIFICATIONS requires LOCK_TASK_FEATURE_HOME"
        }
        return null
    }

    fun isLockTaskActive(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_LOCKED
        } else {
            @Suppress("DEPRECATION") am.isInLockTaskMode
        }
    }

    fun isLockedActivityHome(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val activity = context.packageManager
            .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            ?: return false
        return activity.packageName == context.packageName &&
                activity.name == LockedActivity::class.java.name
    }

    @SuppressLint("InlinedApi")
    fun lockTaskFeaturesFor(c: KioskCustomization?): Int {
        if (c == null) return DEFAULT_LOCK_TASK_FEATURES
        var features = 0
        if (c.powerButtonActions != PowerButtonActions.POWER_BUTTON_BLOCKED) {
            features = features or DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS
        }
        when (c.systemNavigation) {
            SystemNavigation.NAVIGATION_ENABLED ->
                features = features or DevicePolicyManager.LOCK_TASK_FEATURE_HOME or
                        DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW

            SystemNavigation.HOME_BUTTON_ONLY ->
                features = features or DevicePolicyManager.LOCK_TASK_FEATURE_HOME

            else -> {}
        }
        when (c.statusBar) {
            StatusBarMode.NOTIFICATIONS_AND_SYSTEM_INFO_ENABLED ->
                features = features or DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS or
                        DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO

            StatusBarMode.SYSTEM_INFO_ONLY ->
                features = features or DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO

            else -> {}
        }
        return features
    }

    fun settingsPackage(context: Context): String =
        context.packageManager
            .resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)
            ?.activityInfo?.packageName
            ?: "com.android.settings"

    fun startKiosk(context: Context) {
        val launchIntent = Intent(context, LockedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        context.startActivity(launchIntent)
    }

    fun applyLockdownScaffolding(
        context: Context,
        appPackages: List<String>,
        lockTaskFeatures: Int = DEFAULT_LOCK_TASK_FEATURES,
    ) {
        val app = context.applicationContext

        app.packageManager.setComponentEnabledSetting(
            ComponentName(app, LockedActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        if (!isLockedActivityHome(app)) {
            app.dpm.addPersistentPreferredActivity(
                app.dpcAdmin,
                homeIntentFilter(),
                ComponentName(app, LockedActivity::class.java.name)
            )
        }

        applyLockTaskFeatures(app, lockTaskFeatures)
        app.dpm.setLockTaskPackages(
            app.dpcAdmin,
            (appPackages + app.packageName).distinct().toTypedArray(),
        )
    }

    @SuppressLint("InlinedApi")
    fun disableKioskMode(context: Context) {
        val app = context.applicationContext

        app.dpm.clearPackagePersistentPreferredActivities(app.dpcAdmin, app.packageName)
        app.dpm.setLockTaskPackages(app.dpcAdmin, arrayOf(app.packageName))
        applyLockTaskFeatures(app, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)

        PolicyStore(app).setLaunchedKiosk(null)

        val component = ComponentName(app, LockedActivity::class.java)
        if (app.packageManager.getComponentEnabledSetting(component)
            != PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        ) return

        val lockIntent = Intent(app, LockedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        app.startActivity(lockIntent)
    }

    fun applyLockTaskFeatures(context: Context, features: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val app = context.applicationContext
        app.dpm.setLockTaskFeatures(app.dpcAdmin, features)
    }
}
