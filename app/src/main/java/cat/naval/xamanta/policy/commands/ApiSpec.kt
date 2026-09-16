package cat.naval.xamanta.policy.commands

import android.os.Build
import cat.naval.xamanta.policy.engine.PolicyCommand

internal class ApiSpec<T>(
    val name: String,
    val minSdk: Int,
    val asked: (T) -> Boolean,
)

internal fun <T> List<ApiSpec<T>>.unsupportedOn(value: T): List<PolicyCommand> = mapNotNull { spec ->
    if (Build.VERSION.SDK_INT < spec.minSdk && spec.asked(value)) UnsupportedSetting(spec) else null
}

private class UnsupportedSetting(spec: ApiSpec<*>) : PolicyCommand() {
    override val name = spec.name
    override val minSdk = spec.minSdk

    override val asked = true

    override fun execute() = Unit
}
