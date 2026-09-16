package cat.naval.xamanta.policy.commands.connectivity

import android.os.Build
import androidx.annotation.RequiresApi
import cat.naval.xamanta.models.UsbDataAccess
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.S)
class UsbDataSignalingCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "usbDataAccess"
    override val minSdk = Build.VERSION_CODES.S

    override fun execute() {
        val enabled = scope.policy.usbDataAccess != UsbDataAccess.DISALLOW_USB_DATA_TRANSFER
        if (!scope.dpm.canUsbDataSignalingBeDisabled()) {
            deviceCannot("this device cannot disable USB data signaling", asked = !enabled)
            return
        }
        scope.dpm.setUsbDataSignalingEnabled(enabled)
    }
}
