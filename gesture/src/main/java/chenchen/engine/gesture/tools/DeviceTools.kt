package chenchen.engine.gesture.tools

import android.os.Build

/**
 * @author: chenchen
 * @since: 2025/4/11 17:02
 */
object DeviceTools {
    val isHuaWei: Boolean
        get() = Build.MANUFACTURER.contains("huawei", ignoreCase = true)
                || Build.BRAND.contains("huawei", ignoreCase = true)

    val isHonor: Boolean
        get() = Build.MANUFACTURER.contains("honor", ignoreCase = true) ||
                Build.BRAND.contains("honor", ignoreCase = true)

    val isSamsung: Boolean
        get() = Build.MANUFACTURER.contains("samsung", ignoreCase = true) ||
                Build.BRAND.contains("samsung", ignoreCase = true)
}