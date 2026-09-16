package cat.naval.xamanta.system

import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import cat.naval.xamanta.util.decodeJsonOrNull
import cat.naval.xamanta.util.json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "Secrets"

private const val KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "xamanta-secrets"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_BITS = 256
private const val GCM_TAG_BITS = 128
private const val IV_BYTES = 12

private const val SEALED = "v1:"

private object SealingKey {

    @Volatile
    private var cached: SecretKey? = null

    fun get(): SecretKey? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        cached?.let { return it }
        return resolve()
    }

    @Synchronized
    @RequiresApi(Build.VERSION_CODES.M)
    private fun resolve(): SecretKey? {
        cached?.let { return it }
        val key = runCatching { existing() ?: generate() }
            .onFailure { Log.e(TAG, "no sealing key available: ${it.message}") }
            .getOrNull()
        cached = key
        return key
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun existing(): SecretKey? =
        KeyStore.getInstance(KEYSTORE).apply { load(null) }.getKey(KEY_ALIAS, null) as? SecretKey

    @RequiresApi(Build.VERSION_CODES.M)
    private fun generate(): SecretKey {
        Log.i(TAG, "generating the sealing key")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .build()
            )
        }.generateKey()
    }
}

private fun seal(value: String): String {
    val key = SealingKey.get() ?: return value
    return runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        check(iv.size == IV_BYTES) { "unexpected GCM IV length ${iv.size}" }
        val sealed = iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        SEALED + Base64.encodeToString(sealed, Base64.NO_WRAP)
    }.onFailure {
        Log.e(TAG, "could not seal a value — storing it in the clear: ${it.message}")
    }.getOrDefault(value)
}

fun unsealSecret(stored: String): String? {
    if (!stored.startsWith(SEALED)) return stored
    val key = SealingKey.get() ?: run {
        Log.e(TAG, "a sealed value is stored but this device has no key to open it")
        return null
    }
    return runCatching {
        val bytes = Base64.decode(stored.substring(SEALED.length), Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, bytes, 0, IV_BYTES))
        String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }.onFailure { Log.e(TAG, "could not open a sealed value: ${it.message}") }.getOrNull()
}

fun SharedPreferences.sealedSecret(key: String): String? {
    val stored = getString(key, null) ?: return null
    if (stored.startsWith(SEALED)) return stored

    val sealed = seal(stored)
    if (sealed == stored) return stored
    Log.i(TAG, "sealing '$key', written in the clear by an earlier build")
    edit { putString(key, sealed) }
    return sealed
}

fun SharedPreferences.getSecret(key: String): String? = sealedSecret(key)?.let { unsealSecret(it) }

fun SharedPreferences.Editor.putSecret(key: String, value: String): SharedPreferences.Editor =
    putString(key, seal(value))

inline fun <reified T> SharedPreferences.getSecretJson(key: String): T? =
    decodeJsonOrNull("Secrets", "sealed '$key'", getSecret(key))

inline fun <reified T> SharedPreferences.Editor.putSecretJson(
    key: String,
    value: T,
): SharedPreferences.Editor = putSecret(key, json.encodeToString(value))
