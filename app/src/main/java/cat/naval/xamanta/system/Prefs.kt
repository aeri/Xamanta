package cat.naval.xamanta.system

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import cat.naval.xamanta.models.PreferenceConstants
import cat.naval.xamanta.util.decodeJsonOrNull
import cat.naval.xamanta.util.json

fun Context.policyPrefs(): SharedPreferences =
    deviceProtectedStorage().getSharedPreferences(
        PreferenceConstants.PREFERENCES,
        Context.MODE_PRIVATE
    )

internal fun Context.deviceProtectedStorage(): Context =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) createDeviceProtectedStorageContext() else this

fun Context.isUserUnlocked(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
    val userManager = getSystemService(Context.USER_SERVICE) as? UserManager ?: return true
    return userManager.isUserUnlocked
}

inline fun <reified T> SharedPreferences.getJson(key: String): T? =
    decodeJsonOrNull("Prefs", "'$key'", getString(key, null))

inline fun <reified T> SharedPreferences.Editor.putJson(key: String, value: T): SharedPreferences.Editor =
    putString(key, json.encodeToString(value))
