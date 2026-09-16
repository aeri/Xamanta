package cat.naval.xamanta.policy.commands.display

import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import cat.naval.xamanta.policy.engine.PolicyCommand
import cat.naval.xamanta.policy.engine.PolicyScope

@RequiresApi(Build.VERSION_CODES.P)
class ScreenBrightnessCommand(private val scope: PolicyScope) : PolicyCommand() {
    override val name = "screenBrightness"
    override val minSdk = Build.VERSION_CODES.P

    override fun execute() {
        val automatic = scope.policy.screenBrightnessAutomatic ?: return
        scope.dpm.setSystemSetting(
            scope.admin,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (automatic) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC.toString()
            else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL.toString(),
        )
        scope.policy.screenBrightness?.let {
            scope.dpm.setSystemSetting(scope.admin, Settings.System.SCREEN_BRIGHTNESS, it.toString())
        }
    }
}
