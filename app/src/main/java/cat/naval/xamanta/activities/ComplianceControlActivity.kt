package cat.naval.xamanta.activities

import android.util.Log
import android.view.View
import cat.naval.xamanta.R
import cat.naval.xamanta.enrollment.EnrollmentWorker
import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.policy.commands.buildUserRestrictionCommands
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.policy.engine.applyAll
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.system.isDeviceOwner
import cat.naval.xamanta.system.provisioningAdminExtras

class ComplianceControlActivity : EnrollmentProgressActivity() {

    companion object {
        const val TAG = "ComplianceControl"
    }

    override fun onPrepareEnrollment() {
        applyBaselineRestrictions()

        if (isEnrolled()) {
            Log.d(TAG, "Already enrolled — nothing to begin")
            return
        }

        EnrollmentWorker.beginFromProvisioning(this, intent.provisioningAdminExtras())
    }

    override fun onEnrolled() {
        setResult(RESULT_OK)
        finish()
    }

    override fun showFailure(error: String) {
        super.showFailure(error)
        secondaryButton.visibility = View.VISIBLE
        secondaryButton.text = getString(R.string.continue_anyway)
        secondaryButton.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun applyBaselineRestrictions() {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Not device owner — skipping baseline restrictions")
            return
        }
        val scope = PolicyScope(applicationContext, DevicePolicy(id = ""), dpm, dpcAdmin)
        buildUserRestrictionCommands(scope).applyAll(TAG)
        Log.d(TAG, "Baseline restrictions applied")
    }
}
