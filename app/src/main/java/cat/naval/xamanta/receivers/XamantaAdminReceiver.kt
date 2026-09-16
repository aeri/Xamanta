package cat.naval.xamanta.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.security.KeyChain
import android.util.Log
import androidx.work.ExistingWorkPolicy
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.enrollment.EnrollmentWorker
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.services.PolicyService
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.provisioningAdminExtras

class XamantaAdminReceiver : DeviceAdminReceiver() {

    companion object {
        const val TAG = "AdminReceiver"

        private const val DENIED_ALIAS = "xamanta-denied"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Owner enabled")

        val store = EnrollmentStore(context)
        if (store.hasEnrollmentToken() && !store.isEnrolled()) {
            Log.d(TAG, "Stale enrollment params found — scheduling enrollment")
            EnrollmentWorker.enqueue(context, ExistingWorkPolicy.REPLACE)
        }
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d(TAG, "Profile provisioning complete")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.dpm.setOrganizationName(context.dpcAdmin, "Xamanta")
        }

        EnrollmentWorker.beginFromProvisioning(context, intent.provisioningAdminExtras())
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Owner disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence? {
        Log.d(TAG, "Device Owner disable requested")
        return super.onDisableRequested(context, intent)
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "Entering lock task mode: $pkg")
        PolicyStore(context).setPinnedPackage(pkg)
        PolicyService.requestReconcile(context, "lock task entered")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "Exiting lock task mode")
        PolicyStore(context).setPinnedPackage(null)
        PolicyService.requestReconcile(context, "lock task exited")
    }

    override fun onChoosePrivateKeyAlias(
        context: Context,
        intent: Intent,
        uid: Int,
        uri: Uri?,
        alias: String?,
    ): String? {
        if (alias != null) return alias

        val rules = PolicyStore(context).load()?.devicePolicy?.choosePrivateKeyRules.orEmpty()
        if (rules.isEmpty()) return null

        val packages = context.packageManager.getPackagesForUid(uid).orEmpty().toSet()
        val url = uri?.toString().orEmpty()

        val rule = rules.firstOrNull { rule ->
            val packageMatches = rule.packageNames.isEmpty() ||
                    rule.packageNames.any { it in packages }
            val urlMatches = rule.urlPattern == null ||
                    runCatching { rule.urlPattern.toRegex().containsMatchIn(url) }.getOrDefault(false)
            packageMatches && urlMatches
        } ?: return null

        return rule.privateKeyAlias
            ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                KeyChain.KEY_ALIAS_SELECTION_DENIED
            } else {
                DENIED_ALIAS
            }
    }
}
