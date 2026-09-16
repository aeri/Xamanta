package cat.naval.xamanta.enrollment

import android.content.Context
import android.util.Log
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.isDeviceOwner

private const val TAG = "CaCertInstaller"

object CaCertInstaller {

    fun installIfPresent(context: Context) {
        val der = provisionedCa(context) ?: return
        try {
            if (context.dpm.hasCaCertInstalled(context.dpcAdmin, der)) {
                Log.d(TAG, "server CA cert already installed")
                return
            }
            val ok = context.dpm.installCaCert(context.dpcAdmin, der)
            Log.i(TAG, "installCaCert returned $ok")
        } catch (e: Exception) {
            Log.e(TAG, "failed to install server CA cert: ${e.message}")
        }
    }

    fun removeIfPresent(context: Context) {
        val der = provisionedCa(context) ?: return
        try {
            context.dpm.uninstallCaCert(context.dpcAdmin, der)
            Log.i(TAG, "uninstalled server CA cert")
        } catch (e: Exception) {
            Log.e(TAG, "failed to uninstall server CA cert: ${e.message}")
        }
    }

    private fun provisionedCa(context: Context): ByteArray? {
        val der = EnrollmentStore(context).serverCaCertDer() ?: return null
        if (!context.isDeviceOwner()) {
            Log.w(TAG, "not device owner — cannot touch the trust store yet")
            return null
        }
        return der
    }
}
