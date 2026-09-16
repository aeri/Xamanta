package cat.naval.xamanta.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import cat.naval.xamanta.R
import cat.naval.xamanta.enrollment.EnrollmentStore
import cat.naval.xamanta.policy.PolicyStore
import cat.naval.xamanta.policy.kiosk.KioskPolicy
import cat.naval.xamanta.system.DeviceIdentity
import cat.naval.xamanta.system.dpcAdmin
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.models.AuthToken
import cat.naval.xamanta.models.InstallType
import cat.naval.xamanta.models.Policy
import cat.naval.xamanta.services.PolicyService

private const val REFRESH_MS = 2_000L

class SyncActivity : AppCompatActivity() {

    private lateinit var enrollment: EnrollmentStore
    private lateinit var policyStore: PolicyStore

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            renderStatus()
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.settings_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
        applyWindowInsets()
        title = getString(R.string.sync_screen_title)
        enrollment = EnrollmentStore(this)
        policyStore = PolicyStore(this)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        warnIfNotAdmin()
        renderStatus()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun applyWindowInsets() {
        val toolbar = findViewById<View>(R.id.toolbar)
        val container = findViewById<View>(R.id.settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            toolbar.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            container.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            insets
        }
    }

    private fun renderStatus() {
        val fragment = supportFragmentManager.findFragmentById(R.id.settings) as? SettingsFragment
            ?: return

        fragment.setDeviceId(readDeviceId())

        val needsReenroll = enrollment.needsReenrollment()
        val lastError = enrollment.lastAuthError()
        val hasSettings = enrollment.isEnrolled()
        val settings = enrollment.connectionSettings()

        fragment.setEnrollmentStatus(
            getString(
                when {
                    needsReenroll -> R.string.enrollment_state_needs_reenrollment
                    hasSettings -> R.string.enrollment_state_enrolled
                    else -> R.string.enrollment_state_not_enrolled
                }
            )
        )
        fragment.setEnrollmentServer(
            if (settings == null) placeholder()
            else getString(
                R.string.enrollment_server_summary,
                settings.host,
                settings.port,
            )
        )
        fragment.setEnrollmentTokenEndpoint(enrollment.tokenEndpoint() ?: placeholder())
        fragment.setEnrollmentLastError(lastError ?: getString(R.string.status_none))

        val authToken = enrollment.authToken()
        fragment.setTokenStatus(describeToken(authToken, needsReenroll))

        fragment.setConnectionStatus(
            getString(
                when {
                    needsReenroll -> R.string.connection_state_paused
                    !hasSettings -> R.string.connection_state_not_connected
                    else -> R.string.connection_state_active
                }
            )
        )

        val state = policyStore.load()
        if (state != null) {
            fragment.setPolicy(state)
        } else {
            fragment.clearPolicy()
        }

        fragment.setSyncLabel(hasSettings)
    }

    private fun describeToken(token: AuthToken?, needsReenroll: Boolean): String {
        if (needsReenroll) return getString(R.string.token_state_rejected)
        if (token == null) return getString(R.string.token_state_unavailable)
        val now = System.currentTimeMillis()
        val remainingMs = token.expiresAt - now
        return when {
            remainingMs > 60_000L -> getString(
                R.string.token_state_valid,
                DateUtils.getRelativeTimeSpanString(
                    token.expiresAt,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                ),
            )

            remainingMs > 0L -> getString(R.string.token_state_expiring)
            else -> getString(R.string.token_state_expired)
        }
    }

    private fun placeholder(): String = getString(R.string.status_placeholder)

    private fun readDeviceId(): String =
        DeviceIdentity.deviceId(this).ifBlank { getString(R.string.device_id_unavailable) }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            preferenceScreen.releaseIconSpace()

            findPreference<Preference>("sync")?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    (activity as? SyncActivity)?.onSyncClicked()
                    true
                }
        }

        private fun PreferenceGroup.releaseIconSpace() {
            isIconSpaceReserved = false
            for (i in 0 until preferenceCount) {
                val child = getPreference(i)
                if (child is PreferenceGroup) child.releaseIconSpace()
                else child.isIconSpaceReserved = false
            }
        }

        private fun set(key: String, value: String) {
            findPreference<Preference>(key)?.summary = value
        }

        fun setDeviceId(id: String) = set("device_id", id)
        fun setEnrollmentStatus(s: String) = set("enrollment_status", s)
        fun setEnrollmentServer(s: String) = set("enrollment_server", s)
        fun setEnrollmentTokenEndpoint(s: String) = set("enrollment_token_endpoint", s)
        fun setEnrollmentLastError(s: String) = set("enrollment_last_error", s)
        fun setConnectionStatus(s: String) = set("connection_status", s)
        fun setTokenStatus(s: String) = set("token_status", s)

        fun setPolicy(state: Policy) {
            val placeholder = getString(R.string.status_placeholder)
            set("policy_name", state.name.ifBlank { placeholder })
            set("policy_version", state.version.toString())
            set("policy_description", state.description.ifBlank { placeholder })

            val apps = state.devicePolicy.applications
            val managed = apps.count { it.installType != InstallType.BLOCKED }
            val blocked = apps.count { it.installType == InstallType.BLOCKED }
            set(
                "policy_apps",
                getString(R.string.policy_apps_summary, apps.size, managed, blocked),
            )

            val kiosk = KioskPolicy.app(state.devicePolicy)?.packageName
            set("policy_kiosk", kiosk ?: getString(R.string.status_none))
        }

        fun clearPolicy() {
            val placeholder = getString(R.string.status_placeholder)
            listOf("policy_name", "policy_version", "policy_description", "policy_apps", "policy_kiosk")
                .forEach { set(it, placeholder) }
        }

        fun setSyncLabel(enrolled: Boolean) {
            val pref = findPreference<Preference>("sync") ?: return
            pref.title =
                if (enrolled) getString(R.string.sync)
                else getString(R.string.enrollment_state_not_enrolled)
            pref.summary =
                if (enrolled) getString(R.string.sync_action_summary)
                else getString(R.string.sync_action_disabled_summary)
        }
    }

    internal fun onSyncClicked() {
        if (enrollment.isEnrolled()) {
            PolicyService.requestReconcile(this, "manual sync")
            Toast.makeText(this, R.string.sync_started, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.sync_not_enrolled, Toast.LENGTH_LONG).show()
        }
    }

    private fun warnIfNotAdmin() {
        if (!dpm.isAdminActive(dpcAdmin)) {
            Toast.makeText(this, R.string.device_admin_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
