package cat.naval.xamanta

import android.os.Build
import cat.naval.xamanta.policy.commands.application.DelegationScopes
import cat.naval.xamanta.models.DelegatedScope
import org.junit.Assert.assertEquals
import org.junit.Test

class DelegationScopesTest {

    private val expectedPlatform = mapOf(
        DelegatedScope.CERT_INSTALL to "delegation-cert-install",
        DelegatedScope.MANAGED_CONFIGURATIONS to "delegation-app-restrictions",
        DelegatedScope.BLOCK_UNINSTALL to "delegation-block-uninstall",
        DelegatedScope.PERMISSION_GRANT to "delegation-permission-grant",
        DelegatedScope.PACKAGE_ACCESS to "delegation-package-access",
        DelegatedScope.ENABLE_SYSTEM_APP to "delegation-enable-system-app",
        DelegatedScope.NETWORK_ACTIVITY_LOGS to "delegation-network-logging",
        DelegatedScope.SECURITY_LOGS to "delegation-security-logging",
        DelegatedScope.CERT_SELECTION to "delegation-cert-selection",
    )

    private val expectedMinSdk = mapOf(
        DelegatedScope.CERT_INSTALL to 26,
        DelegatedScope.MANAGED_CONFIGURATIONS to 26,
        DelegatedScope.BLOCK_UNINSTALL to 26,
        DelegatedScope.PERMISSION_GRANT to 26,
        DelegatedScope.PACKAGE_ACCESS to 26,
        DelegatedScope.ENABLE_SYSTEM_APP to 26,
        DelegatedScope.CERT_SELECTION to 29,
        DelegatedScope.NETWORK_ACTIVITY_LOGS to 29,
        DelegatedScope.SECURITY_LOGS to 31,
    )

    @Test
    fun `every scope maps to the platform constant it names`() {
        assertEquals(expectedPlatform, DelegationScopes.PLATFORM)
    }

    @Test
    fun `every scope declares the API level that introduced it`() {
        assertEquals(expectedMinSdk, DelegationScopes.MIN_SDK)
    }

    @Test
    fun `both tables cover every scope the model can carry`() {
        assertEquals(DelegatedScope.entries.toSet(), DelegationScopes.PLATFORM.keys)
        assertEquals(DelegatedScope.entries.toSet(), DelegationScopes.MIN_SDK.keys)
    }

    @Test
    fun `no two scopes share a platform constant`() {
        assertEquals(
            DelegationScopes.PLATFORM.size,
            DelegationScopes.PLATFORM.values.toSet().size,
        )
    }

    @Test
    fun `no scope claims to predate delegation`() {
        val tooEarly = DelegationScopes.MIN_SDK.filterValues { it < Build.VERSION_CODES.O }

        assertEquals(emptyMap<DelegatedScope, Int>(), tooEarly)
    }
}
