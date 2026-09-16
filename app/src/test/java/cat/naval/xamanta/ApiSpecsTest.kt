package cat.naval.xamanta

import android.os.Build
import cat.naval.xamanta.policy.commands.APPLICATION_SPECS
import cat.naval.xamanta.policy.commands.DEVICE_POLICY_SPECS
import cat.naval.xamanta.policy.commands.RESTRICTION_SPECS
import cat.naval.xamanta.models.ApplicationPolicy
import cat.naval.xamanta.models.DevicePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiSpecsTest {

    @Test
    fun `every device policy spec declares the level its command needs`() {
        val wrong = DEVICE_POLICY_SPECS
            .filter { EXPECTED_MIN_SDK["${it.name}@${it.minSdk}"] != it.minSdk }
            .map { "${it.name}: declared ${it.minSdk}" }

        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `the expected table covers exactly the specs in use`() {
        assertEquals(
            EXPECTED_MIN_SDK.keys,
            DEVICE_POLICY_SPECS.map { "${it.name}@${it.minSdk}" }.toSet(),
        )
    }

    @Test
    fun `no spec is declared at or below the projects own minSdk`() {
        val pointless = (DEVICE_POLICY_SPECS.map { it.name to it.minSdk } +
                APPLICATION_SPECS.map { it.name to it.minSdk })
            .filter { (_, minSdk) -> minSdk <= Build.VERSION_CODES.LOLLIPOP }

        assertEquals(emptyList<Pair<String, Int>>(), pointless)
    }

    @Test
    fun `no spec repeats a user restriction`() {
        val restrictionNames = RESTRICTION_SPECS.map { it.name }.toSet()
        assertEquals(
            emptyList<String>(),
            DEVICE_POLICY_SPECS.map { it.name }.filter { it in restrictionNames },
        )
    }

    @Test
    fun `specs are unique by name and level`() {
        val keys = DEVICE_POLICY_SPECS.map { "${it.name}@${it.minSdk}" }
        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun `an empty policy asks for nothing`() {
        val empty = DevicePolicy(id = "")
        assertTrue(DEVICE_POLICY_SPECS.none { it.asked(empty) })
        assertTrue(APPLICATION_SPECS.none { it.asked(ApplicationPolicy(packageName = "a.b")) })
    }

    private companion object {
        val EXPECTED_MIN_SDK = mapOf(
            "usbDataAccess@${Build.VERSION_CODES.S}" to Build.VERSION_CODES.S,
            "configureWifi@${Build.VERSION_CODES.M}" to Build.VERSION_CODES.M,
            "configureWifi@${Build.VERSION_CODES.TIRAMISU}" to Build.VERSION_CODES.TIRAMISU,
            "tetheringSettings@${Build.VERSION_CODES.TIRAMISU}" to Build.VERSION_CODES.TIRAMISU,
            "wifiSsidPolicy@${Build.VERSION_CODES.TIRAMISU}" to Build.VERSION_CODES.TIRAMISU,
            "minimumWifiSecurityLevel@${Build.VERSION_CODES.TIRAMISU}" to Build.VERSION_CODES.TIRAMISU,
            "preferentialNetworkId@${Build.VERSION_CODES.TIRAMISU}" to Build.VERSION_CODES.TIRAMISU,
            "wifiRoamingPolicy@${Build.VERSION_CODES.VANILLA_ICE_CREAM}" to Build.VERSION_CODES.VANILLA_ICE_CREAM,
            "apnPolicy@${Build.VERSION_CODES.P}" to Build.VERSION_CODES.P,
            "privateDnsSettings@${Build.VERSION_CODES.Q}" to Build.VERSION_CODES.Q,
            "stayOnPluggedModes@${Build.VERSION_CODES.M}" to Build.VERSION_CODES.M,
            "keyguardDisabled@${Build.VERSION_CODES.M}" to Build.VERSION_CODES.M,
            "kioskCustomization.statusBar@${Build.VERSION_CODES.M}" to Build.VERSION_CODES.M,
            "systemUpdate@${Build.VERSION_CODES.M}" to Build.VERSION_CODES.M,
            "defaultPermissionPolicy@${Build.VERSION_CODES.M}" to Build.VERSION_CODES.M,
            "shortSupportMessage@${Build.VERSION_CODES.N}" to Build.VERSION_CODES.N,
            "longSupportMessage@${Build.VERSION_CODES.N}" to Build.VERSION_CODES.N,
            "deviceOwnerLockScreenInfo@${Build.VERSION_CODES.N}" to Build.VERSION_CODES.N,
            "alwaysOnVpnPackage@${Build.VERSION_CODES.N}" to Build.VERSION_CODES.N,
            "requirePasswordUnlock@${Build.VERSION_CODES.O}" to Build.VERSION_CODES.O,
            "screenBrightness@${Build.VERSION_CODES.P}" to Build.VERSION_CODES.P,
            "screenTimeout@${Build.VERSION_CODES.P}" to Build.VERSION_CODES.P,
            "factoryResetProtection@${Build.VERSION_CODES.R}" to Build.VERSION_CODES.R,
            "userControlDisabledPackages@${Build.VERSION_CODES.R}" to Build.VERSION_CODES.R,
            "locationEnabled@${Build.VERSION_CODES.R}" to Build.VERSION_CODES.R,
            "commonCriteriaMode@${Build.VERSION_CODES.R}" to Build.VERSION_CODES.R,
            "mtePolicy@${Build.VERSION_CODES.UPSIDE_DOWN_CAKE}" to Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            "credentialProviderPolicy@${Build.VERSION_CODES.UPSIDE_DOWN_CAKE}" to Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
            "appFunctions@${Build.VERSION_CODES.BAKLAVA}" to Build.VERSION_CODES.BAKLAVA,
        )
    }
}
