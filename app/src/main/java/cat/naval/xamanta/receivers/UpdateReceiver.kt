package cat.naval.xamanta.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cat.naval.xamanta.services.PolicyService

class UpdateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UpdateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.w(TAG, "ignoring unexpected action '${intent.action}'")
            return
        }
        Log.i(TAG, "MY_PACKAGE_REPLACED - restarting PolicyService")
        PolicyService.start(context)
    }
}
