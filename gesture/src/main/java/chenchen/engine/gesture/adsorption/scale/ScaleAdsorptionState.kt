package chenchen.engine.gesture.adsorption.scale

import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.ScaleTrack

/**
 * 缩放轨迹
 * @author: chenchen
 * @since: 2023/10/16 17:47
 */
class ScaleAdsorptionState {
    /**
     * 旋转轨迹
     */
    var scaleTrack: ScaleTrack = ScaleTrack.None

    /**
     * 记录
     */
    fun rememberMovementTrack(detector: BestGestureDetector) {
        scaleTrack = when {
            detector.scaleFactor < 1f -> ScaleTrack.ZoomOut
            detector.scaleFactor > 1f -> ScaleTrack.ZoomIn
            else -> scaleTrack
        }
    }
}