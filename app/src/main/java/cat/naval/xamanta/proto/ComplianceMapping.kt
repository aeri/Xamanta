package cat.naval.xamanta.proto

import cat.naval.xamanta.models.Compliance as ModelCompliance
import cat.naval.xamanta.models.NonComplianceDetail as ModelNonComplianceDetail
import cat.naval.xamanta.protos.ApplyPolicyResponse as ProtoApplyPolicyResponse
import cat.naval.xamanta.protos.Compliance as ProtoCompliance
import cat.naval.xamanta.protos.InstallationFailureReason as ProtoInstallationFailureReason
import cat.naval.xamanta.protos.NonComplianceDetail as ProtoNonComplianceDetail
import cat.naval.xamanta.protos.NonComplianceReason as ProtoNonComplianceReason
import cat.naval.xamanta.util.isoNow

fun complianceReport(
    compliance: ModelCompliance,
    executionId: String,
    details: List<ModelNonComplianceDetail>,
): ProtoApplyPolicyResponse = ProtoApplyPolicyResponse.newBuilder()
    .setCompliance(ProtoCompliance.valueOf(compliance.name))
    .setExecutionId(executionId)
    .setAppliedAt(isoNow())
    .addAllDetails(details.map { it.toProto() })
    .build()

fun ModelNonComplianceDetail.toProto(): ProtoNonComplianceDetail =
    ProtoNonComplianceDetail.newBuilder()
        .setSettingName(settingName)
        .setNonComplianceReason(ProtoNonComplianceReason.valueOf(nonComplianceReason.name))
        .setPackageName(packageName)
        .setFieldPath(fieldPath)
        .setCurrentValue(currentValue)
        .setInstallationFailureReason(
            ProtoInstallationFailureReason.valueOf(installationFailureReason.name)
        )
        .build()
