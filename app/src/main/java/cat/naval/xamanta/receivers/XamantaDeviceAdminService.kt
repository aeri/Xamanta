package cat.naval.xamanta.receivers

import android.app.admin.DeviceAdminService
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import cat.naval.xamanta.services.PolicyService

@RequiresApi(Build.VERSION_CODES.O)
class XamantaDeviceAdminService : DeviceAdminService() {

    companion object {
        private const val TAG = "DeviceAdminService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceAdminService created")
        PolicyService.start(applicationContext)
    }
}
