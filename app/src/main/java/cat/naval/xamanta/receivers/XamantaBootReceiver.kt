package cat.naval.xamanta.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cat.naval.xamanta.services.PolicyService

class XamantaBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"

        private const val ACTION_LOCKED_BOOT_COMPLETED =
            "android.intent.action.LOCKED_BOOT_COMPLETED"

        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

        private val ACCEPTED_ACTIONS = setOf(
            ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON,
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null || action !in ACCEPTED_ACTIONS) {
            Log.w(TAG, "ignoring unexpected action '$action'")
            return
        }

        if (action == ACTION_LOCKED_BOOT_COMPLETED) {
            Log.i(TAG, "$action - starting PolicyService (Direct Boot)")
            PolicyService.start(context)
        } else {
            Log.i(TAG, "$action received - waking PolicyService for a reconcile")
            PolicyService.requestReconcile(context, action)
        }
    }
}
