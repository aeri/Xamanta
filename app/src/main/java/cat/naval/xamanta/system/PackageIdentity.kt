package cat.naval.xamanta.system

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import cat.naval.xamanta.util.toHexString
import java.security.MessageDigest

object PackageIdentity {

    fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            PackageManager.GET_SIGNING_CERTIFICATES or
                    (@Suppress("DEPRECATION") PackageManager.GET_SIGNATURES)
        else
            @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    fun installedVersionCode(pm: PackageManager, packageName: String): Long =
        runCatching { versionCodeOf(pm.getPackageInfo(packageName, 0)) }
            .getOrDefault(Long.MAX_VALUE)

    fun installedSigners(pm: PackageManager, packageName: String): Set<String> =
        runCatching { signersOf(pm.getPackageInfo(packageName, signingFlags())) }
            .getOrDefault(emptySet())

    fun archiveSigners(pm: PackageManager, apkPath: String): Set<String> =
        runCatching {
            pm.getPackageArchiveInfo(apkPath, signingFlags())?.let { signersOf(it) }
        }.getOrNull() ?: emptySet()

    fun versionCodeOf(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()

    fun primarySigner(info: PackageInfo): String = signersOf(info).firstOrNull() ?: ""

    fun fingerprintsSha1(info: PackageInfo): List<String> =
        signatures(info)?.map { hex(it.toByteArray(), "SHA-1") } ?: emptyList()

    private fun signersOf(info: PackageInfo): Set<String> =
        signatures(info)?.map { hex(it.toByteArray(), "SHA-256") }?.toSet() ?: emptySet()

    private fun signatures(info: PackageInfo): Array<Signature>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.let { si ->
                si.apkContentsSigners?.takeIf { it.isNotEmpty() }
                    ?: si.signingCertificateHistory
            } ?: @Suppress("DEPRECATION") info.signatures
        } else {
            @Suppress("DEPRECATION") info.signatures
        }

    private fun hex(bytes: ByteArray, algorithm: String): String =
        MessageDigest.getInstance(algorithm).digest(bytes).toHexString()
}
