package chenchen.engine.gesture.adsorption.move

import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.MovementTrack
import chenchen.engine.gesture.MovementTrack.*

/**
 * @author: chenchen
 * @since: 2023/4/27 10:11
 */
class AdsorptionState {

    /**
     * 水平滑动轨迹
     */
    var hMovementTrack: MovementTrack = None

    /**
     * 垂直滑动轨迹
     */
    var vMovementTrack: MovementTrack = None


    /**
     * 记录
     */
    fun rememberMovementTrack(detector: BestGestureDetector) {
        hMovementTrack = when {
            detector.moveX < 0 -> RightToLeft
            detector.moveX > 0 -> LeftToRight
            else -> hMovementTrack
        }
        vMovementTrack = when {
            detector.moveY < 0 -> BottomToTop
            detector.moveY > 0 -> TopToBottom
            else -> vMovementTrack
        }
    }
}