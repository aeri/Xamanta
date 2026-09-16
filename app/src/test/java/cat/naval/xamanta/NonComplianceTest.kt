package cat.naval.xamanta

import cat.naval.xamanta.policy.engine.MissingPackageException
import cat.naval.xamanta.policy.engine.NonCompliance
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.UnsupportedApiLevelException
import cat.naval.xamanta.policy.engine.UnsupportedDeviceException
import cat.naval.xamanta.policy.engine.UnsupportedManagementModeException
import cat.naval.xamanta.models.InstallationFailureReason
import cat.naval.xamanta.models.NonComplianceReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class NonComplianceTest {

    @Test
    fun `a missing app defaults to the applications setting`() {
        val detail = NonCompliance.appNotInstalled("com.example.app")

        assertEquals(NonCompliance.APPLICATIONS, detail.settingName)
        assertEquals(NonComplianceReason.APP_NOT_INSTALLED, detail.nonComplianceReason)
        assertEquals("com.example.app", detail.packageName)
        assertEquals(
            InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNSPECIFIED,
            detail.installationFailureReason,
        )
    }

    @Test
    fun `a device policy naming a missing app reports against its own field`() {
        val detail = NonCompliance.appNotInstalled(
            packageName = "com.example.vpn",
            settingName = "alwaysOnVpnPackage",
        )

        assertEquals("alwaysOnVpnPackage", detail.settingName)
        assertEquals(NonComplianceReason.APP_NOT_INSTALLED, detail.nonComplianceReason)
        assertEquals("com.example.vpn", detail.packageName)
    }

    @Test
    fun `the second positional argument is still the installation failure reason`() {
        val detail =
            NonCompliance.appNotInstalled("com.example.app", InstallationFailureReason.NOT_FOUND)

        assertEquals(NonCompliance.APPLICATIONS, detail.settingName)
        assertEquals(InstallationFailureReason.NOT_FOUND, detail.installationFailureReason)
    }

    @Test
    fun `a missing package exception carries the package it could not find`() {
        val thrown = MissingPackageException("com.example.vpn")

        assertEquals("com.example.vpn", thrown.packageName)
        assertEquals("package not installed: com.example.vpn", thrown.message)
    }

    @Test
    fun `a per-application failure reports under the applications setting`() {
        val detail = NonCompliance.applicationSetting(
            packageName = "com.example.app",
            fieldPath = "managedConfiguration",
            reason = NonComplianceReason.INVALID_VALUE,
            currentValue = "duplicate key 'server'",
        )

        assertEquals(NonCompliance.APPLICATIONS, detail.settingName)
        assertEquals(NonComplianceReason.INVALID_VALUE, detail.nonComplianceReason)
        assertEquals("com.example.app", detail.packageName)
        assertEquals("managedConfiguration", detail.fieldPath)
        assertEquals("duplicate key 'server'", detail.currentValue)
    }

    @Test
    fun `a rejected permission grant keeps its field path`() {
        val detail =
            NonCompliance.permissionGrantRejected("com.example.app", "android.permission.CAMERA")

        assertEquals(NonCompliance.APPLICATIONS, detail.settingName)
        assertEquals(NonComplianceReason.INVALID_VALUE, detail.nonComplianceReason)
        assertEquals("com.example.app", detail.packageName)
        assertEquals("permissionGrants.android.permission.CAMERA", detail.fieldPath)
    }

    @Test
    fun `the shared failure mapping keeps the device-wide shape`() {
        val command = named("passwordScope")

        val managementMode = NonCompliance.forFailure(
            command,
            UnsupportedManagementModeException("SCOPE_PROFILE requires a work profile"),
        )
        assertEquals("passwordScope", managementMode.settingName)
        assertEquals(NonComplianceReason.MANAGEMENT_MODE, managementMode.nonComplianceReason)
        assertEquals("SCOPE_PROFILE requires a work profile", managementMode.currentValue)
        assertEquals("", managementMode.packageName)

        val apiLevel = NonCompliance.forFailure(command, UnsupportedApiLevelException("too old"))
        assertEquals(NonComplianceReason.API_LEVEL, apiLevel.nonComplianceReason)

        val invalid = NonCompliance.forFailure(command, IllegalStateException("rejected"))
        assertEquals(NonComplianceReason.INVALID_VALUE, invalid.nonComplianceReason)
    }

    @Test
    fun `a device-wide command that names a missing package reports the package`() {
        val detail = NonCompliance.forFailure(
            named("alwaysOnVpnPackage"),
            MissingPackageException("com.example.vpn"),
        )

        assertEquals("alwaysOnVpnPackage", detail.settingName)
        assertEquals(NonComplianceReason.APP_NOT_INSTALLED, detail.nonComplianceReason)
        assertEquals("com.example.vpn", detail.packageName)
    }

    @Test
    fun `a per-application command reports its name as the field path`() {
        val detail = NonCompliance.forFailure(
            named("delegatedScopes"),
            UnsupportedApiLevelException("scopes not available on API 26: SECURITY_LOGS"),
            packageName = "com.example.app",
        )

        assertEquals(NonCompliance.APPLICATIONS, detail.settingName)
        assertEquals(NonComplianceReason.API_LEVEL, detail.nonComplianceReason)
        assertEquals("com.example.app", detail.packageName)
        assertEquals("delegatedScopes", detail.fieldPath)
    }

    @Test
    fun `hardware the device does not have is reported as unsupported`() {
        val detail = NonCompliance.forFailure(
            named("mtePolicy"),
            UnsupportedDeviceException("device does not support MTE"),
        )

        assertEquals("mtePolicy", detail.settingName)
        assertEquals(NonComplianceReason.UNSUPPORTED, detail.nonComplianceReason)
        assertEquals("device does not support MTE", detail.currentValue)

        val perApp = NonCompliance.forFailure(
            named("mtePolicy"),
            UnsupportedDeviceException("device does not support MTE"),
            packageName = "com.example.app",
        )
        assertEquals(NonComplianceReason.UNSUPPORTED, perApp.nonComplianceReason)
        assertEquals("com.example.app", perApp.packageName)
    }

    @Test
    fun `a capability nothing asked for is not reported at all`() {
        assertNull(runCatching { capabilityCommand(asked = false).execute() }.exceptionOrNull())

        val thrown = assertThrows(UnsupportedDeviceException::class.java) {
            capabilityCommand(asked = true).execute()
        }
        assertEquals("this device has no Wi-Fi", thrown.message)
    }

    private fun capabilityCommand(asked: Boolean) = object : PolicyCommand() {
        override val name = "openNetworkConfiguration"
        override fun execute() = deviceCannot("this device has no Wi-Fi", asked = asked)
    }

    private fun named(commandName: String) = object : PolicyCommand() {
        override val name = commandName
        override fun execute() = Unit
    }

    @Test
    fun `a kiosk that crash loops and one that never pinned are told apart`() {
        val looping = NonCompliance.kioskAppCrashLooping("com.example.kiosk")
        val unpinned = NonCompliance.kioskNotPinned("com.example.kiosk")

        for (detail in listOf(looping, unpinned)) {
            assertEquals(NonCompliance.APPLICATIONS, detail.settingName)
            assertEquals(NonComplianceReason.APP_INCOMPATIBLE, detail.nonComplianceReason)
            assertEquals("com.example.kiosk", detail.packageName)
            assertEquals("kiosk", detail.fieldPath)
        }
        assertEquals("crash loop", looping.currentValue)
        assertEquals("not in lock task", unpinned.currentValue)
    }

    @Test
    fun `api level keeps its own reason`() {
        assertEquals(
            NonComplianceReason.API_LEVEL,
            NonCompliance.apiLevel("passwordDetailedRequirements", "x").nonComplianceReason,
        )
    }
}
