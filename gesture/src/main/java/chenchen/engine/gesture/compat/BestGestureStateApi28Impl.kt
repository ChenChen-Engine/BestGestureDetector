package chenchen.engine.gesture.compat

import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.BestGestureState

/**
 * 兼容Android10以下[MotionEvent.getRawX]会因为[android.view.ViewGroup]分发的过程中被转换过的问题
 * [MotionEvent.getRawX]应该是屏幕的绝对坐标，不受任何变化影象。
 * 导致的现象则是会出现坐标抖动
 * @author: chenchen
 * @since: 2023/4/14 11:03
 */
internal class BestGestureStateApi28Impl(private val view: View) : BestGestureState() {

    override fun rememberCurrentEvent(event: MotionEvent) {
        if (!view.matrix.isIdentity) {
            event.transform(view.matrix)
        }
        super.rememberCurrentEvent(event)
    }
}