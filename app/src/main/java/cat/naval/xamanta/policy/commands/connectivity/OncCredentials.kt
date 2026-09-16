package cat.naval.xamanta.policy.commands.connectivity

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Build
import android.util.Base64
import android.util.Log
import cat.naval.xamanta.models.OncCertificate
import cat.naval.xamanta.models.OncCertificateType
import cat.naval.xamanta.system.dpm
import cat.naval.xamanta.util.decodeBase64
import cat.naval.xamanta.util.parseX509Chain
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate

private const val TAG = "OncCredentials"

class OncCredentials(
    private val dpm: DevicePolicyManager,
    private val admin: ComponentName,
) {

    val serverCerts = mutableMapOf<String, List<X509Certificate>>()

    val installedCaCertDers = mutableSetOf<String>()

    val clientAliases = mutableMapOf<String, String>()

    val clientKeys = mutableMapOf<String, Pair<PrivateKey, Array<Certificate>>>()

    fun install(certificates: List<OncCertificate>): Set<String> {
        certificates.forEach { certificate ->
            when (certificate.type) {
                OncCertificateType.SERVER -> installServer(certificate)
                OncCertificateType.CLIENT -> installClient(certificate)
            }
        }
        return clientAliases.values.toSet()
    }

    fun revokeStaleKeys(previous: Set<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        (previous - clientAliases.values.toSet()).forEach { alias ->
            runCatching { dpm.removeKeyPair(admin, alias) }
                .onFailure { Log.w(TAG, "could not remove key '$alias': ${it.message}") }
        }
    }

    fun revokeStaleCaCerts(previous: Set<String>, protectedDers: Set<String>) {
        (previous - installedCaCertDers - protectedDers).forEach { fingerprint ->
            val der = fingerprint.decodeBase64() ?: return@forEach
            runCatching { dpm.uninstallCaCert(admin, der) }
                .onFailure { Log.w(TAG, "could not uninstall CA cert: ${it.message}") }
        }
    }

    private fun installServer(certificate: OncCertificate) {
        val der = certificate.x509?.decodeBase64() ?: return
        val chain = runCatching { der.parseX509Chain() }.getOrNull()

        if (chain.isNullOrEmpty()) {
            Log.e(TAG, "certificate '${certificate.guid}' is not a valid X.509 — ignoring")
            return
        }
        runCatching { dpm.installCaCert(admin, der) }
            .onFailure { Log.e(TAG, "installCaCert for '${certificate.guid}' failed: ${it.message}") }
        serverCerts[certificate.guid] = chain
        chain.forEach { installedCaCertDers += canonicalDer(it) }
    }

    private fun installClient(certificate: OncCertificate) {
        val bytes = certificate.pkcs12?.decodeBase64() ?: return
        val password = (certificate.pkcs12Password ?: "").toCharArray()

        val entry = runCatching { readPkcs12(bytes, password) }.getOrNull()
        if (entry == null) {
            Log.e(TAG, "certificate '${certificate.guid}' is not a readable PKCS#12 — ignoring")
            return
        }
        val (key, chain) = entry
        val alias = aliasFor(certificate.guid)

        val installed = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                dpm.installKeyPair(admin, key, chain, alias, false)
            } else {
                @Suppress("DEPRECATION")
                dpm.installKeyPair(admin, key, chain.first(), alias)
            }
        }.getOrDefault(false)

        if (!installed) {
            Log.e(TAG, "the platform refused the key pair for '${certificate.guid}'")
            return
        }
        clientAliases[certificate.guid] = alias
        clientKeys[certificate.guid] = key to chain
    }

    private fun readPkcs12(
        bytes: ByteArray,
        password: CharArray,
    ): Pair<PrivateKey, Array<Certificate>>? {
        val store = KeyStore.getInstance("PKCS12").apply {
            load(ByteArrayInputStream(bytes), password)
        }
        val alias = store.aliases().toList().firstOrNull { store.isKeyEntry(it) } ?: return null
        val key = store.getKey(alias, password) as? PrivateKey ?: return null
        val chain = store.getCertificateChain(alias) ?: return null
        return key to chain
    }

    private fun canonicalDer(cert: X509Certificate): String =
        Base64.encodeToString(cert.encoded, Base64.NO_WRAP)

    companion object {
        fun aliasFor(guid: String) = "xamanta-onc-$guid"

        fun canonicalCaCertDers(base64: String): Set<String> {
            val der = base64.decodeBase64() ?: return emptySet()
            return runCatching {
                der.parseX509Chain().map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }.toSet()
            }.getOrDefault(emptySet())
        }
    }
}
