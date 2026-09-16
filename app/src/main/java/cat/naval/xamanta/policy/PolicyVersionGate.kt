package cat.naval.xamanta.policy

import cat.naval.xamanta.models.Policy

internal class PolicyVersionGate {

    private var name: String? = null
    private var version: Int = -1

    @Synchronized
    fun seed(policy: Policy?) {
        name = policy?.name
        version = policy?.version ?: -1
    }

    @Synchronized
    fun admit(policy: Policy): Boolean {
        if (policy.name == name && policy.version <= version) return false
        name = policy.name
        version = policy.version
        return true
    }
}
