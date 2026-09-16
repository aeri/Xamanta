package cat.naval.xamanta.activities

class ProvisioningSuccessActivity : EnrollmentProgressActivity() {

    override fun onEnrolled() {
        headerText.postDelayed({ finish() }, 3_000)
    }
}
