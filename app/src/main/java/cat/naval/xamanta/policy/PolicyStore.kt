package cat.naval.xamanta.policy

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import cat.naval.xamanta.policy.kiosk.KioskPolicy
import cat.naval.xamanta.system.policyPrefs
import cat.naval.xamanta.system.putSecretJson
import cat.naval.xamanta.system.sealedSecret
import cat.naval.xamanta.system.unsealSecret
import cat.naval.xamanta.util.json
import cat.naval.xamanta.models.Policy
import cat.naval.xamanta.models.ProvisionedNetwork
import cat.naval.xamanta.models.PreferenceConstants.CURRENT_POLICY
import cat.naval.xamanta.models.PreferenceConstants.KIOSK_DEGRADED_PACKAGE
import cat.naval.xamanta.models.PreferenceConstants.KIOSK_LAUNCH_COUNT
import cat.naval.xamanta.models.PreferenceConstants.KIOSK_LAUNCHED_PACKAGE
import cat.naval.xamanta.models.PreferenceConstants.KIOSK_LAUNCH_WINDOW_START
import cat.naval.xamanta.models.PreferenceConstants.KIOSK_UNPINNED_PACKAGE
import cat.naval.xamanta.models.PreferenceConstants.LOCK_TASK_PINNED_PACKAGE
import cat.naval.xamanta.models.PreferenceConstants.MANAGED_APPLICATION_PACKAGES
import cat.naval.xamanta.models.PreferenceConstants.PREFERRED_ACTIVITY_PACKAGES
import cat.naval.xamanta.models.PreferenceConstants.OVERRIDE_APN_IDS
import cat.naval.xamanta.models.PreferenceConstants.PROVISIONED_CA_CERTS
import cat.naval.xamanta.models.PreferenceConstants.PROVISIONED_KEY_ALIASES
import cat.naval.xamanta.models.PreferenceConstants.PROVISIONED_NETWORKS
import cat.naval.xamanta.models.PreferenceConstants.PROVISIONED_NETWORK_RECORDS
import cat.naval.xamanta.models.PreferenceConstants.WIFI_ROAMING_SSIDS

private const val TAG = "PolicyStore"

private const val CRASH_WINDOW_MS = 60_000L
private const val CRASH_THRESHOLD = 4

private val RECORD_SEPARATOR = Char(0)

class PolicyStore(context: Context) {

    private val preferences = context.applicationContext.policyPrefs()

    private fun remembered(key: String): Set<String> = preferences.getStringSet(key, null).orEmpty()

    private fun remember(key: String, values: Collection<String>) = preferences.edit {
        if (values.isEmpty()) remove(key) else putStringSet(key, values.toSet())
    }

    fun save(policy: Policy): Boolean =
        preferences.edit().putSecretJson(CURRENT_POLICY, policy).commit()

    fun load(): Policy? = ParsedPolicy.of(preferences.sealedSecret(CURRENT_POLICY))

    fun forgetPolicy() = preferences.edit { remove(CURRENT_POLICY) }

    fun isKioskCrashLooping(policy: Policy): Boolean =
        KioskPolicy.app(policy.devicePolicy)?.packageName?.let { isKioskCrashLooping(it) } == true

    fun isKioskCrashLooping(packageName: String): Boolean =
        preferences.getString(KIOSK_DEGRADED_PACKAGE, null) == packageName

    fun recordKioskCrash(): Boolean {
        val now = System.currentTimeMillis()
        val windowStart = preferences.getLong(KIOSK_LAUNCH_WINDOW_START, 0L)
        val previous = if (now - windowStart > CRASH_WINDOW_MS) {
            preferences.edit { putLong(KIOSK_LAUNCH_WINDOW_START, now) }
            0
        } else {
            preferences.getInt(KIOSK_LAUNCH_COUNT, 0)
        }
        val crashes = previous + 1
        preferences.edit { putInt(KIOSK_LAUNCH_COUNT, crashes) }
        Log.d(TAG, "kiosk crash #$crashes within the crash window")
        return crashes >= CRASH_THRESHOLD
    }

    fun markKioskCrashLooping(packageName: String) {
        preferences.edit { putString(KIOSK_DEGRADED_PACKAGE, packageName) }
    }

    fun pinnedPackage(): String? = preferences.getString(LOCK_TASK_PINNED_PACKAGE, null)

    fun setPinnedPackage(packageName: String?) = preferences.edit {
        if (packageName == null) remove(LOCK_TASK_PINNED_PACKAGE)
        else putString(LOCK_TASK_PINNED_PACKAGE, packageName)
    }

    fun launchedKiosk(): String? = preferences.getString(KIOSK_LAUNCHED_PACKAGE, null)

    fun setLaunchedKiosk(packageName: String?) = preferences.edit {
        if (packageName == null) remove(KIOSK_LAUNCHED_PACKAGE)
        else putString(KIOSK_LAUNCHED_PACKAGE, packageName)
    }

    fun isKioskUnpinned(packageName: String): Boolean =
        preferences.getString(KIOSK_UNPINNED_PACKAGE, null) == packageName

    fun setKioskUnpinned(packageName: String?) = preferences.edit {
        if (packageName == null) remove(KIOSK_UNPINNED_PACKAGE)
        else putString(KIOSK_UNPINNED_PACKAGE, packageName)
    }

    fun preferredActivityPackages(): Set<String> = remembered(PREFERRED_ACTIVITY_PACKAGES)

    fun setPreferredActivityPackages(packages: Set<String>) = remember(PREFERRED_ACTIVITY_PACKAGES, packages)

    fun managedApplicationPackages(): Set<String> = remembered(MANAGED_APPLICATION_PACKAGES)

    fun setManagedApplicationPackages(packages: Set<String>) = remember(MANAGED_APPLICATION_PACKAGES, packages)

    fun roamingSsids(): Set<String> = remembered(WIFI_ROAMING_SSIDS)

    fun setRoamingSsids(ssids: Set<String>) = remember(WIFI_ROAMING_SSIDS, ssids)

    fun provisionedNetworks(): Map<String, ProvisionedNetwork>? {
        val stored = preferences.sealedSecret(PROVISIONED_NETWORK_RECORDS)
            ?: return retiredProvisionedNetworks()
        val plain = unsealSecret(stored) ?: return null
        return runCatching { json.decodeFromString<Map<String, ProvisionedNetwork>>(plain) }
            .onFailure { Log.e(TAG, "unreadable provisioned-network record: ${it.message}") }
            .getOrNull()
    }

    fun setProvisionedNetworks(networks: Map<String, ProvisionedNetwork>) = preferences.edit {
        putSecretJson(PROVISIONED_NETWORK_RECORDS, networks)
        remove(PROVISIONED_NETWORKS)
    }

    private fun retiredProvisionedNetworks(): Map<String, ProvisionedNetwork> =
        remembered(PROVISIONED_NETWORKS).mapNotNull { entry ->
            val guid = entry.substringBeforeLast(RECORD_SEPARATOR, missingDelimiterValue = "")
            val id = entry.substringAfterLast(RECORD_SEPARATOR).toIntOrNull()
            if (guid.isEmpty() || id == null) null else guid to ProvisionedNetwork(id)
        }.toMap()

    fun provisionedKeyAliases(): Set<String> = remembered(PROVISIONED_KEY_ALIASES)

    fun setProvisionedKeyAliases(aliases: Set<String>) = remember(PROVISIONED_KEY_ALIASES, aliases)

    fun provisionedCaCerts(): Set<String> = remembered(PROVISIONED_CA_CERTS)

    fun setProvisionedCaCerts(fingerprints: Set<String>) = remember(PROVISIONED_CA_CERTS, fingerprints)

    fun overrideApnIds(): Set<Int> = remembered(OVERRIDE_APN_IDS).mapNotNull { it.toIntOrNull() }.toSet()

    fun setOverrideApnIds(ids: Set<Int>) = remember(OVERRIDE_APN_IDS, ids.map { it.toString() })

    fun clearKioskCrashState() {
        preferences.edit {
            remove(KIOSK_DEGRADED_PACKAGE)
            remove(KIOSK_LAUNCH_WINDOW_START)
            remove(KIOSK_LAUNCH_COUNT)
            remove(KIOSK_UNPINNED_PACKAGE)
        }
    }
}

private object ParsedPolicy {

    private var raw: String? = null
    private var policy: Policy? = null

    @Synchronized
    fun of(raw: String?): Policy? {
        if (raw == null) {
            this.raw = null
            this.policy = null
            return null
        }
        if (raw == this.raw) return policy

        val plain = unsealSecret(raw)
        if (plain == null) {
            Log.e(TAG, "the stored policy could not be opened")
            return null
        }
        val parsed = runCatching { json.decodeFromString<Policy>(plain) }
            .onFailure { Log.e(TAG, "unreadable stored policy: ${it.message}") }
            .getOrNull()
        this.raw = raw
        this.policy = parsed
        return parsed
    }
}
