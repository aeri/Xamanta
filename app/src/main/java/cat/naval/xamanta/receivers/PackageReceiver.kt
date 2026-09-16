package cat.naval.xamanta.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackageReceiver(
    private val onPackageEvent: (action: String, packageName: String) -> Unit,
) : BroadcastReceiver() {

    private companion object {
        const val TAG = "PackageReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pkg = intent.data?.schemeSpecificPart ?: return
        Log.d(TAG, "event action=$action pkg=$pkg")
        onPackageEvent(action, pkg)
    }
}
