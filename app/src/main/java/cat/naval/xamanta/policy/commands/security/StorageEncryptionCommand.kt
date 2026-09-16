package cat.naval.xamanta.policy.commands.security

import android.app.admin.DevicePolicyManager
import android.os.Build
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

class StorageEncryptionCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "storageEncryption"

    @Suppress("DEPRECATION")
    override fun execute() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return
        val required = scope.policy.storageEncryptionRequired
        val status = scope.dpm.setStorageEncryption(scope.admin, required)
        if (status == DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED) {
            deviceCannot("this device cannot encrypt its storage", asked = required)
        }
    }
}
