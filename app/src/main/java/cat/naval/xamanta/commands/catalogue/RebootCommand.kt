package cat.naval.xamanta.commands.catalogue

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand

private const val TAG = "RebootCommand"

@RequiresApi(Build.VERSION_CODES.N)
internal class RebootCommand(private val scope: CommandScope) : DeviceCommand() {
    override val name = "reboot"
    override val minSdk = Build.VERSION_CODES.N

    override suspend fun execute() {
        if (callInProgress()) error("the device has an ongoing or ringing call")
    }

    override val afterReported: suspend () -> Unit = {
        Log.w(TAG, "rebooting device by remote command")
        runCatching { scope.dpm.reboot(scope.admin) }
            .onFailure { Log.e(TAG, "reboot refused after it was reported: ${it.message}") }
    }

    @Suppress("DEPRECATION")
    private fun callInProgress(): Boolean {
        val telephony = scope.context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return false
        if (!canReadPhoneState()) return false
        return runCatching { telephony.callState != TelephonyManager.CALL_STATE_IDLE }
            .onFailure { Log.w(TAG, "could not read the call state: ${it.message}") }
            .getOrDefault(false)
    }

    private fun canReadPhoneState(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasPhoneState()) return true
        runCatching {
            scope.dpm.setPermissionGrantState(
                scope.admin,
                scope.context.packageName,
                Manifest.permission.READ_PHONE_STATE,
                DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED,
            )
        }.onFailure { Log.w(TAG, "could not grant READ_PHONE_STATE to the DPC: ${it.message}") }
        return hasPhoneState()
    }

    private fun hasPhoneState(): Boolean = ContextCompat.checkSelfPermission(
        scope.context,
        Manifest.permission.READ_PHONE_STATE,
    ) == PackageManager.PERMISSION_GRANTED
}
