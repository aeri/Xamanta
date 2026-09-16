package cat.naval.xamanta

import android.os.Build
import android.os.UserManager
import cat.naval.xamanta.policy.commands.RESTRICTION_SPECS
import cat.naval.xamanta.policy.commands.managedUserRestrictions
import cat.naval.xamanta.models.DevicePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestrictionSpecsTest {

    @Test
    fun `every restriction declares the API level the platform introduced it in`() {
        val wrong = RESTRICTION_SPECS
            .filter { EXPECTED_MIN_SDK[it.restriction] != it.minSdk }
            .map { "${it.name} (${it.restriction}): declared ${it.minSdk}, expected ${EXPECTED_MIN_SDK[it.restriction]}" }

        assertEquals(emptyList<String>(), wrong)
    }

    @Test
    fun `the expected table covers exactly the restrictions in use`() {
        assertEquals(EXPECTED_MIN_SDK.keys, RESTRICTION_SPECS.map { it.restriction }.toSet())
    }

    @Test
    fun `no restriction is declared below the projects own minSdk`() {
        assertTrue(RESTRICTION_SPECS.all { it.minSdk >= Build.VERSION_CODES.LOLLIPOP })
    }

    @Test
    fun `spec names are unique`() {
        val names = RESTRICTION_SPECS.map { it.name }
        assertEquals(
            "a duplicate name would make the non-compliance report ambiguous",
            names.distinct(),
            names,
        )
    }

    @Test
    fun `each restriction is driven by exactly one spec`() {
        val restrictions = RESTRICTION_SPECS.map { it.restriction }
        assertEquals(
            "two specs fighting over one restriction means the last one silently wins",
            restrictions.distinct(),
            restrictions,
        )
    }

    @Test
    fun `every managed restriction is reported for cleanup`() {
        assertEquals(RESTRICTION_SPECS.map { it.restriction }, managedUserRestrictions())
    }

    @Test
    fun `an empty policy asks for nothing beyond the safe defaults`() {
        val empty = DevicePolicy(id = "")
        val requested = RESTRICTION_SPECS.filter { it.desired(empty) }.map { it.name }.toSet()

        assertEquals(
            setOf(
                "debuggingFeaturesDisabled",
                "safeBootDisabled",
                "installUnknownSourcesDisabled",
                "installUnknownSourcesGloballyDisabled",
                "verifyApps",
            ),
            requested,
        )
    }

    private companion object {
        val EXPECTED_MIN_SDK = mapOf(
            UserManager.DISALLOW_ADD_USER to 21,
            UserManager.DISALLOW_ADD_WIFI_CONFIG to 33,
            UserManager.DISALLOW_ADJUST_VOLUME to 21,
            UserManager.DISALLOW_AIRPLANE_MODE to 28,
            UserManager.DISALLOW_ASSIST_CONTENT to 35,
            UserManager.DISALLOW_BLUETOOTH to 26,
            UserManager.DISALLOW_BLUETOOTH_SHARING to 26,
            UserManager.DISALLOW_CAMERA_TOGGLE to 31,
            UserManager.DISALLOW_CELLULAR_2G to 34,
            UserManager.DISALLOW_CHANGE_WIFI_STATE to 33,
            UserManager.DISALLOW_CONFIG_BLUETOOTH to 21,
            UserManager.DISALLOW_CONFIG_BRIGHTNESS to 28,
            UserManager.DISALLOW_CONFIG_CELL_BROADCASTS to 21,
            UserManager.DISALLOW_CONFIG_CREDENTIALS to 21,
            UserManager.DISALLOW_CONFIG_DATE_TIME to 28,
            UserManager.DISALLOW_CONFIG_LOCATION to 28,
            UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS to 21,
            UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT to 28,
            UserManager.DISALLOW_CONFIG_TETHERING to 21,
            UserManager.DISALLOW_CONFIG_VPN to 21,
            UserManager.DISALLOW_CONFIG_WIFI to 21,
            UserManager.DISALLOW_CREATE_WINDOWS to 21,
            UserManager.DISALLOW_DATA_ROAMING to 24,
            UserManager.DISALLOW_DEBUGGING_FEATURES to 21,
            UserManager.DISALLOW_FACTORY_RESET to 21,
            UserManager.DISALLOW_FUN to 23,
            UserManager.DISALLOW_INSTALL_APPS to 21,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES to 21,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES_GLOBALLY to 29,
            UserManager.DISALLOW_MICROPHONE_TOGGLE to 31,
            UserManager.DISALLOW_MODIFY_ACCOUNTS to 21,
            UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA to 21,
            UserManager.DISALLOW_NETWORK_RESET to 23,
            UserManager.DISALLOW_OUTGOING_BEAM to 22,
            UserManager.DISALLOW_OUTGOING_CALLS to 21,
            UserManager.DISALLOW_PRINTING to 28,
            UserManager.DISALLOW_REMOVE_USER to 21,
            UserManager.DISALLOW_SAFE_BOOT to 23,
            UserManager.DISALLOW_SET_USER_ICON to 24,
            UserManager.DISALLOW_SET_WALLPAPER to 24,
            UserManager.DISALLOW_SHARE_LOCATION to 21,
            UserManager.DISALLOW_SMS to 21,
            UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS to 28,
            UserManager.DISALLOW_ULTRA_WIDEBAND_RADIO to 34,
            UserManager.DISALLOW_UNINSTALL_APPS to 21,
            UserManager.DISALLOW_UNMUTE_MICROPHONE to 21,
            UserManager.DISALLOW_USB_FILE_TRANSFER to 21,
            UserManager.DISALLOW_WIFI_DIRECT to 33,
            UserManager.DISALLOW_WIFI_TETHERING to 33,
            UserManager.ENSURE_VERIFY_APPS to 21,
        )
    }
}
