package chenchen.engine.gesture.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.OnSimpleMoveListener
import chenchen.engine.gesture.OnSimpleRotateListener
import chenchen.engine.gesture.OnSimpleScaleListener
import chenchen.engine.gesture.OnSimpleTouchListener

/**
 * @author: chenchen
 * @since: 2023/8/24 14:16
 */
class GestureView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    val gesture = BestGestureDetector(this).apply {
        setOnTouchListener(object : OnSimpleTouchListener() {
            override fun onTouchMove(detector: BestGestureDetector): Boolean {
                offsetLeftAndRight(detector.moveX.toInt())
                offsetTopAndBottom(detector.moveY.toInt())
                return super.onTouchMove(detector)
            }
        })
        setMoveListener(object : OnSimpleMoveListener() {
            override fun onMove(detector: BestGestureDetector): Boolean {
                offsetLeftAndRight(detector.moveX.toInt())
                offsetTopAndBottom(detector.moveY.toInt())
                return super.onMove(detector)
            }
        })
        setRotationListener(object : OnSimpleRotateListener() {
            override fun onRotate(detector: BestGestureDetector): Boolean {
                this@GestureView.rotation += detector.rotation
                return super.onRotate(detector)
            }
        })
        setScaleListener(object : OnSimpleScaleListener() {
            override fun onScale(detector: BestGestureDetector): Boolean {
                scaleX *= detector.scaleFactor
                scaleY *= detector.scaleFactor
                return super.onScale(detector)
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gesture.onTouchEvent(event)
    }
}