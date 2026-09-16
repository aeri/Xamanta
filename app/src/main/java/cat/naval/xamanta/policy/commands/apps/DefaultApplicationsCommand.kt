package cat.naval.xamanta.policy.commands.apps

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.net.toUri
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope
import cat.naval.xamanta.system.homeIntentFilter
import cat.naval.xamanta.models.DefaultApplication
import cat.naval.xamanta.models.DefaultApplicationType
import cat.naval.xamanta.models.NonComplianceDetail
import cat.naval.xamanta.models.NonComplianceReason

class DefaultApplicationsCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "defaultApplicationSettings"

    override var details: List<NonComplianceDetail> = emptyList()
        private set

    override fun execute() {
        details = scope.policy.defaultApplications.mapNotNull { apply(it) }
    }

    private fun apply(default: DefaultApplication): NonComplianceDetail? {
        if (default.profileScoped) {
            return NonCompliance.applicationSetting(
                packageName = default.packageName,
                fieldPath = "$name.${default.type.name}",
                reason = NonComplianceReason.MANAGEMENT_MODE,
                currentValue = "scope is not available on a fully managed device",
            )
        }

        return when (default.type) {
            DefaultApplicationType.DEFAULT_DIALER -> setDialer(default.packageName)
            DefaultApplicationType.DEFAULT_SMS -> setSms(default.packageName)
            DefaultApplicationType.DEFAULT_HOME ->
                setPreferred(default, homeIntentFilter())

            DefaultApplicationType.DEFAULT_BROWSER ->
                setPreferred(default, browserFilter())

            DefaultApplicationType.DEFAULT_ASSISTANT,
            DefaultApplicationType.DEFAULT_CALL_REDIRECTION,
            DefaultApplicationType.DEFAULT_CALL_SCREENING,
            DefaultApplicationType.DEFAULT_WALLET,
                -> NonCompliance.unsupported(
                settingName = name,
                fieldPath = default.type.name,
                currentValue = "only RoleManager can set this default",
            )
        }
    }

    private fun setDialer(packageName: String): NonComplianceDetail? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return NonCompliance.apiLevel(
                settingName = name,
                currentValue = "DEFAULT_DIALER needs API 34, device is on API ${Build.VERSION.SDK_INT}",
            )
        }
        scope.dpm.setDefaultDialerApplication(packageName)
        return null
    }

    private fun setSms(packageName: String): NonComplianceDetail? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return NonCompliance.apiLevel(
                settingName = name,
                currentValue = "DEFAULT_SMS needs API 29, device is on API ${Build.VERSION.SDK_INT}",
            )
        }
        scope.dpm.setDefaultSmsApplication(scope.admin, packageName)
        return null
    }

    private fun setPreferred(
        default: DefaultApplication,
        filter: IntentFilter,
    ): NonComplianceDetail? {
        val component = resolve(default.packageName, filter)
            ?: return NonCompliance.appNotInstalled(
                packageName = default.packageName,
                settingName = name,
            )
        scope.dpm.addPersistentPreferredActivity(scope.admin, filter, component)
        return null
    }

    private fun resolve(packageName: String, filter: IntentFilter): ComponentName? {
        val intent = Intent(filter.getAction(0)).apply {
            (0 until filter.countCategories()).forEach { addCategory(filter.getCategory(it)) }
            if (filter.countDataSchemes() > 0) data = "${filter.getDataScheme(0)}://".toUri()
            setPackage(packageName)
        }
        val activity = scope.context.packageManager
            .queryIntentActivities(intent, 0)
            .firstOrNull()
            ?: return null
        return ComponentName(packageName, activity.activityInfo.name)
    }

    private fun browserFilter() = IntentFilter(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addCategory(Intent.CATEGORY_DEFAULT)
        addDataScheme("http")
        addDataScheme("https")
    }
}
