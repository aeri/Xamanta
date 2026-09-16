package cat.naval.xamanta.models

enum class Compliance {
    IN_FORCE,
    DEFIANCE,
    UNASSIGNED,
    PENDING_COMPLIANCE,
    PENDING_SYNC,
    DECOMMISSION;
}

enum class NonComplianceReason {
    NON_COMPLIANCE_REASON_UNSPECIFIED,
    API_LEVEL,
    MANAGEMENT_MODE,
    USER_ACTION,
    INVALID_VALUE,
    APP_NOT_INSTALLED,
    UNSUPPORTED,
    APP_INSTALLED,
    PENDING,
    APP_INCOMPATIBLE,
    APP_NOT_UPDATED;
}

enum class InstallationFailureReason {
    INSTALLATION_FAILURE_REASON_UNSPECIFIED,
    INSTALLATION_FAILURE_REASON_UNKNOWN,
    IN_PROGRESS,
    NOT_FOUND,
    NOT_COMPATIBLE_WITH_DEVICE,
    NOT_APPROVED,
    PERMISSIONS_NOT_ACCEPTED,
    NOT_AVAILABLE_IN_COUNTRY,
    NO_LICENSES_REMAINING,
    NOT_ENROLLED,
    USER_INVALID,
    NETWORK_ERROR_UNRELIABLE_CONNECTION,
    INSUFFICIENT_STORAGE;
}

data class NonComplianceDetail(
    val settingName: String = "",
    val nonComplianceReason: NonComplianceReason = NonComplianceReason.NON_COMPLIANCE_REASON_UNSPECIFIED,
    val packageName: String = "",
    val fieldPath: String = "",
    val currentValue: String = "",
    val installationFailureReason: InstallationFailureReason =
        InstallationFailureReason.INSTALLATION_FAILURE_REASON_UNSPECIFIED,
)
