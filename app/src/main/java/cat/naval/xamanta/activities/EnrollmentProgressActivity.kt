package cat.naval.xamanta.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cat.naval.xamanta.R
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.enrollment.EnrollmentWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

abstract class EnrollmentProgressActivity : AppCompatActivity() {

    protected lateinit var progressBar: LinearProgressIndicator
    protected lateinit var headerIcon: ImageView
    protected lateinit var headerText: TextView
    protected lateinit var summaryText: TextView
    protected lateinit var buttonBar: LinearLayout
    protected lateinit var actionButton: MaterialButton
    protected lateinit var secondaryButton: MaterialButton

    private var enrolledHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_provisioning_success)

        progressBar = findViewById(R.id.progressBar)
        headerIcon = findViewById(R.id.headerIcon)
        headerText = findViewById(R.id.headerText)
        summaryText = findViewById(R.id.summaryText)
        buttonBar = findViewById(R.id.buttonBar)
        actionButton = findViewById(R.id.actionButton)
        secondaryButton = findViewById(R.id.secondaryButton)

        actionButton.setOnClickListener {
            EnrollmentWorker.enqueue(this, ExistingWorkPolicy.REPLACE)
            showEnrolling()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })

        onPrepareEnrollment()
        observeEnrollment()
    }

    protected open fun onPrepareEnrollment() {}

    protected open fun onEnrolled() {}

    protected fun isEnrolled(): Boolean = EnrollmentStore(this).isEnrolled()

    private fun observeEnrollment() {
        WorkManager.getInstance(this)
            .getWorkInfosForUniqueWorkLiveData(EnrollmentWorker.WORK_NAME)
            .observe(this) { infos ->
                if (isEnrolled()) {
                    showSuccess()
                    return@observe
                }

                val info = infos?.firstOrNull()
                when (info?.state) {
                    null,
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING,
                    WorkInfo.State.BLOCKED ->
                        showEnrolling(info?.progress?.getString(EnrollmentWorker.KEY_ERROR))

                    WorkInfo.State.SUCCEEDED -> showSuccess()

                    WorkInfo.State.FAILED ->
                        showFailure(
                            info.outputData.getString(EnrollmentWorker.KEY_ERROR)
                                ?: getString(R.string.enroll_error_unknown)
                        )

                    WorkInfo.State.CANCELLED -> showFailure(getString(R.string.enroll_cancelled))
                }
            }
    }

    protected open fun showEnrolling(detail: String? = null) {
        progressBar.visibility = View.VISIBLE
        headerIcon.setImageResource(R.drawable.ic_shield_sync)
        buttonBar.visibility = View.GONE
        secondaryButton.visibility = View.GONE
        headerText.text = getString(R.string.enrolling_title)
        summaryText.text = detail ?: getString(R.string.enrolling_detail)
    }

    protected open fun showSuccess() {
        progressBar.visibility = View.INVISIBLE
        headerIcon.setImageResource(R.drawable.ic_shield_check)
        buttonBar.visibility = View.GONE
        headerText.text = getString(R.string.enrolled_title)
        summaryText.text = getString(R.string.enrolled_detail)

        if (!enrolledHandled) {
            enrolledHandled = true
            onEnrolled()
        }
    }

    protected open fun showFailure(error: String) {
        progressBar.visibility = View.INVISIBLE
        headerIcon.setImageResource(R.drawable.ic_shield_error)
        buttonBar.visibility = View.VISIBLE
        actionButton.visibility = View.VISIBLE
        actionButton.text = getString(R.string.retry)
        secondaryButton.visibility = View.GONE
        headerText.text = getString(R.string.enrollment_failed_title)
        summaryText.text = error
    }
}
