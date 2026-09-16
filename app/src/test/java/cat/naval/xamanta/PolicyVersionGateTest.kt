package cat.naval.xamanta

import cat.naval.xamanta.models.DevicePolicy
import cat.naval.xamanta.models.Policy
import cat.naval.xamanta.policy.PolicyVersionGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun policy(name: String = "fleet", version: Int): Policy =
    Policy(name = name, version = version, description = "", devicePolicy = DevicePolicy(id = ""))

class PolicyVersionGateTest {

    @Test
    fun `an empty gate admits anything`() {
        assertTrue(PolicyVersionGate().admit(policy(version = 0)))
    }

    @Test
    fun `a newer version is admitted and an older or equal one is not`() {
        val gate = PolicyVersionGate().apply { seed(policy(version = 5)) }

        assertFalse(gate.admit(policy(version = 4)))
        assertFalse(gate.admit(policy(version = 5)))
        assertTrue(gate.admit(policy(version = 6)))
    }

    @Test
    fun `admission advances the gate before anything is stored`() {
        val gate = PolicyVersionGate().apply { seed(policy(version = 1)) }

        assertTrue(gate.admit(policy(version = 3)))
        assertFalse(gate.admit(policy(version = 2)))
        assertFalse(gate.admit(policy(version = 3)))
    }

    @Test
    fun `a new lineage is admitted whatever its version`() {
        val gate = PolicyVersionGate().apply { seed(policy(name = "old", version = 9)) }

        assertTrue(gate.admit(policy(name = "new", version = 1)))
        assertFalse(gate.admit(policy(name = "new", version = 1)))
        assertTrue(gate.admit(policy(name = "old", version = 9)))
    }

    @Test
    fun `seeding overrides whatever was admitted`() {
        val gate = PolicyVersionGate().apply { admit(policy(version = 7)) }

        gate.seed(policy(version = 2))

        assertTrue(gate.admit(policy(version = 3)))
    }

    @Test
    fun `seeding with nothing forgets everything admitted`() {
        val gate = PolicyVersionGate().apply { admit(policy(version = 7)) }

        gate.seed(null)

        assertTrue(gate.admit(policy(version = 1)))
    }
}
