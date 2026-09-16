package cat.naval.xamanta.system

import android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PersistableBundle

fun homeIntentFilter(): IntentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
    addCategory(Intent.CATEGORY_HOME)
    addCategory(Intent.CATEGORY_DEFAULT)
}

fun Intent.provisioningAdminExtras(): PersistableBundle? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, PersistableBundle::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
    }
