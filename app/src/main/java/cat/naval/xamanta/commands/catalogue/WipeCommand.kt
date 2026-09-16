package cat.naval.xamanta.commands.catalogue

import android.app.admin.DevicePolicyManager.WIPE_EUICC
import android.app.admin.DevicePolicyManager.WIPE_EXTERNAL_STORAGE
import android.app.admin.DevicePolicyManager.WIPE_RESET_PROTECTION_DATA
import android.os.Build
import android.util.Log
import cat.naval.xamanta.commands.CommandScope
import cat.naval.xamanta.commands.DeviceCommand
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException
import cat.naval.xamanta.protos.WipeParams
import cat.naval.xamanta.system.isDeviceOwner

private const val TAG = "WipeCommand"

internal class WipeCommand(
    private val scope: CommandScope,
    private val params: WipeParams?,
) : DeviceCommand() {
    override val name = "wipe"

    private var flags = 0

    override suspend fun execute() {
        var wipeFlags = policyWipeFlags()
        if (params != null) {
            if (params.wipeExternalStorage) wipeFlags = wipeFlags or WIPE_EXTERNAL_STORAGE
            if (!params.preserveResetProtection) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                    throw UnsupportedApiLevelException(
                        "wiping factory reset protection data requires API 22+ " +
                                "(this device is API ${Build.VERSION.SDK_INT})"
                    )
                }
                wipeFlags = wipeFlags or WIPE_RESET_PROTECTION_DATA
            }
        }
        flags = wipeFlags
    }

    override val afterReported: suspend () -> Unit = {
        Log.w(TAG, "wiping device by remote command flags=0x${flags.toString(16)}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && scope.context.isDeviceOwner()) {
            scope.dpm.wipeDevice(flags)
        } else {
            scope.dpm.wipeData(flags)
        }
    }

    private fun policyWipeFlags(): Int {
        val policy = PolicyStore(scope.context).load()?.devicePolicy ?: return 0
        if (!policy.wipeEsims || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0
        return WIPE_EUICC
    }
}
