package chenchen.engine.gesture.adsorption.rotate

import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.RotationTrack

/**
 * 旋转轨迹
 * @author: chenchen
 * @since: 2023/10/16 17:47
 */
class RotteAdsorptionState {
    /**
     * 旋转轨迹
     */
    var rotationTrack: RotationTrack = RotationTrack.None

    /**
     * 记录
     */
    fun rememberMovementTrack(detector: BestGestureDetector) {
        rotationTrack = when {
            detector.rotation < 0 -> RotationTrack.Anticlockwise
            detector.rotation > 0 -> RotationTrack.Clockwise
            else -> rotationTrack
        }
    }
}