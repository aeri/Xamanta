package cat.naval.xamanta.activities

import android.app.Activity
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE
import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_MODE
import android.app.admin.DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import cat.naval.xamanta.system.provisioningAdminExtras

class ProvisionControlActivity : Activity() {

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = Intent()
        result.putExtra(EXTRA_PROVISIONING_MODE, PROVISIONING_MODE_FULLY_MANAGED_DEVICE)

        intent.provisioningAdminExtras()
            ?.let { result.putExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, it) }

        setResult(RESULT_OK, result)
        finish()
    }
}
