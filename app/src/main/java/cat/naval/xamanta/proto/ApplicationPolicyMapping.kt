package cat.naval.xamanta.proto

import cat.naval.xamanta.models.ApplicationPolicy as ModelApplicationPolicy
import cat.naval.xamanta.models.DelegatedScope as ModelDelegatedScope
import cat.naval.xamanta.models.InstallType as ModelInstallType
import cat.naval.xamanta.models.ManagedProperty as ModelManagedProperty
import cat.naval.xamanta.models.PermissionGrant as ModelPermissionGrant
import cat.naval.xamanta.models.PermissionPolicy as ModelPermissionPolicy
import cat.naval.xamanta.models.UserControlSetting as ModelUserControlSetting
import cat.naval.xamanta.util.toHexString
import cat.naval.xamanta.protos.ApplicationPolicy as ProtoApplicationPolicy
import cat.naval.xamanta.protos.ApplicationSigningKeyCert as ProtoApplicationSigningKeyCert
import cat.naval.xamanta.protos.ApplicationPolicy.AlwaysOnVpnLockdownExemption as ProtoVpnLockdownExemption
import cat.naval.xamanta.protos.ApplicationPolicy.CredentialProviderPolicy as ProtoAppCredentialProviderPolicy
import cat.naval.xamanta.protos.ApplicationPolicy.DelegatedScope as ProtoDelegatedScope
import cat.naval.xamanta.protos.ManagedProperty as ProtoManagedProperty
import cat.naval.xamanta.protos.PermissionGrant as ProtoPermissionGrant
import cat.naval.xamanta.protos.PermissionPolicy as ProtoPermissionPolicy

fun ProtoApplicationPolicy.toKotlinModel(): ModelApplicationPolicy = ModelApplicationPolicy(
    packageName = packageName,
    downloadUrl = downloadUrl.takeIf { it.isNotEmpty() },
    sha256Sum = sha256Sum.takeIf { it.isNotEmpty() },
    signingKeyCerts = signingKeyCertsList.map { it.fingerprintHex() },
    installType = installType.asModel(ModelInstallType.INSTALL_TYPE_UNSPECIFIED),
    permissionGrants = permissionGrantsList.map { it.toKotlinModel() },
    minimumVersionCode = minimumVersionCode,
    defaultPermissionPolicy = defaultPermissionPolicy
        .takeIf { it != ProtoPermissionPolicy.PERMISSION_POLICY_UNSPECIFIED }
        ?.toKotlinModel(),
    managedConfiguration = managedConfigurationList.map { it.toKotlinModel() },
    delegatedScopes = delegatedScopesList.mapNotNull { it.toKotlinModel() },
    alwaysOnVpnLockdownExempt =
        alwaysOnVpnLockdownExemption == ProtoVpnLockdownExemption.VPN_LOCKDOWN_EXEMPTION,
    credentialProviderAllowed =
        credentialProviderPolicy == ProtoAppCredentialProviderPolicy.CREDENTIAL_PROVIDER_ALLOWED,
    userControl = userControlSettings.asModelOrNull<ModelUserControlSetting>(),
    preferentialNetworkId = preferentialNetworkId.toKotlinModel(),
)

private fun ProtoApplicationSigningKeyCert.fingerprintHex(): String =
    signingKeyCertFingerprintSha256.toByteArray().toHexString()

fun ProtoPermissionPolicy.toKotlinModel(): ModelPermissionPolicy =
    asModel(ModelPermissionPolicy.PROMPT)

fun ProtoPermissionGrant.toKotlinModel(): ModelPermissionGrant = ModelPermissionGrant(
    permission = permission,
    policy = policy.toKotlinModel(),
)

fun ProtoDelegatedScope.toKotlinModel(): ModelDelegatedScope? =
    asModelOrNull<ModelDelegatedScope>()

fun ProtoManagedProperty.toKotlinModel(): ModelManagedProperty = ModelManagedProperty(
    key = key,
    stringValue = if (hasStringValue()) stringValue else null,
    boolValue = if (hasBoolValue()) boolValue else null,
    intValue = if (hasIntValue()) intValue else null,
    stringListValue = if (hasStringListValue()) stringListValue.valuesList.toList() else null,
    bundleValue =
        if (hasBundleValue()) bundleValue.propertiesList.map { it.toKotlinModel() } else null,
    bundleArrayValue = if (hasBundleArrayValue()) {
        bundleArrayValue.bundlesList.map { bundle ->
            bundle.propertiesList.map { it.toKotlinModel() }
        }
    } else {
        null
    },
)
