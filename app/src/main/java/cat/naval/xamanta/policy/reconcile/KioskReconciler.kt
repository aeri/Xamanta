package cat.naval.xamanta.policy.reconcile

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.util.Log
import cat.naval.xamanta.packages.ManagedPackages
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.kiosk.KioskManager
import cat.naval.xamanta.policy.kiosk.KioskPolicy

private const val TAG = "KioskReconciler"

class KioskReconciler(
    private val context: Context,
    private val packages: ManagedPackages,
    private val policyStore: PolicyStore,
) {

    fun reconcile(
        policy: DevicePolicy,
        forceTransition: Boolean,
        crashLooping: Boolean,
    ): List<NonComplianceDetail> {
        val kioskApp = KioskPolicy.app(policy)
        if (kioskApp == null || !packages.isInstalled(kioskApp.packageName)) {
            if (kioskApp != null) {
                Log.w(TAG, "kiosk app '${kioskApp.packageName}' not installed yet — kiosk stays off")
            }
            KioskManager.disableKioskMode(context)
            return emptyList()
        }

        val allowed = KioskPolicy.lockTaskAllowlist(context, policy)
        val requested = KioskManager.lockTaskFeaturesFor(policy.kioskCustomization)
        val rejection = lockTaskFeaturesRejection(requested)
        val features = if (rejection == null) requested else KioskManager.DEFAULT_LOCK_TASK_FEATURES
        val featureDetail = rejection?.let {
            Log.e(TAG, "invalid lockTaskFeatures $requested ($it) — falling back to default")
            NonCompliance.invalidValue(NonCompliance.LOCK_TASK_FEATURES, requested.toString())
        } ?: lockTaskFeaturesTooNew(policy.kioskCustomization != null, requested)

        KioskManager.applyLockdownScaffolding(context, allowed, features)

        if (crashLooping) {
            Log.e(TAG, "kiosk app '${kioskApp.packageName}' crash looping — holding lockdown")
            return listOfNotNull(
                featureDetail,
                NonCompliance.kioskAppCrashLooping(kioskApp.packageName),
            )
        }

        val alreadyUp = KioskManager.isLockTaskActive(context) &&
                policyStore.launchedKiosk() == kioskApp.packageName
        val unpinned = policyStore.isKioskUnpinned(kioskApp.packageName)
        if (forceTransition || (!alreadyUp && !unpinned)) {
            Log.i(TAG, "entering kiosk mode on '${kioskApp.packageName}' (force=$forceTransition)")
            KioskManager.startKiosk(context)
        }

        val unpinnedDetail = if (unpinned) {
            Log.e(TAG, "kiosk app '${kioskApp.packageName}' never entered lock task — next attempt on retry")
            policyStore.setKioskUnpinned(null)
            NonCompliance.kioskNotPinned(kioskApp.packageName)
        } else {
            null
        }
        return listOfNotNull(featureDetail, unpinnedDetail)
    }

    @SuppressLint("InlinedApi")
    private fun lockTaskFeaturesTooNew(customized: Boolean, features: Int): NonComplianceDetail? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || !customized) return null
        if (features == DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS) return null
        return NonCompliance.apiLevel(
            NonCompliance.LOCK_TASK_FEATURES,
            "lock task features need API 28, device is on API ${Build.VERSION.SDK_INT}",
        )
    }

    private fun lockTaskFeaturesRejection(features: Int): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            KioskManager.invalidLockTaskFeatures(features)
        } else {
            null
        }
}
