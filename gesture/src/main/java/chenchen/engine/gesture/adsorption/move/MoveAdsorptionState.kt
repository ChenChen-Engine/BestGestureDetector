package chenchen.engine.gesture.adsorption.move

import android.animation.ValueAnimator
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.MoveMovementTrack
import chenchen.engine.gesture.MoveMovementTrack.*

/**
 * 移动轨迹
 * @author: chenchen
 * @since: 2023/4/27 10:11
 */
internal class MoveAdsorptionState {

    /**
     * 水平滑动轨迹
     */
    var hMovementTrack: MoveMovementTrack = None

    /**
     * 垂直滑动轨迹
     */
    var vMovementTrack: MoveMovementTrack = None

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