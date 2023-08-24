package chenchen.engine.gesture.exclusion

import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.EdgeGestureDetector

/**
 * @author: chenchen
 * @since: 2023/4/27 10:22
 * 排斥手势
 */
class ExclusionEdgeGestureDetector : EdgeGestureDetector() {

    override fun onMove(detector: BestGestureDetector): Boolean {
        return false
    }

}