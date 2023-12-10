package chenchen.engine.gesture.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.OnTouchGestureListener
import chenchen.engine.gesture.OnMoveGestureListener
import chenchen.engine.gesture.OnRotateGestureListener
import chenchen.engine.gesture.OnScaleGestureListener

/**
 * @author: chenchen
 * @since: 2023/8/24 14:16
 */
class GestureView : View {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    val gesture = BestGestureDetector(context).apply {
        setOnTouchListener(object : OnTouchGestureListener {
            override fun onTouchMove(detector: BestGestureDetector): Boolean {
                offsetLeftAndRight(detector.moveX.toInt())
                offsetTopAndBottom(detector.moveY.toInt())
                return true
            }
        })
        setMoveListener(object : OnMoveGestureListener {
            override fun onMove(detector: BestGestureDetector): Boolean {
                offsetLeftAndRight(detector.moveX.toInt())
                offsetTopAndBottom(detector.moveY.toInt())
                return true
            }
        })
        setRotationListener(object : OnRotateGestureListener {
            override fun onRotate(detector: BestGestureDetector): Boolean {
                this@GestureView.rotation += detector.rotation
                return true
            }
        })
        setScaleListener(object : OnScaleGestureListener {
            override fun onScale(detector: BestGestureDetector): Boolean {
                Log.e("TAG", "onScale: ${detector.scaleFactor}", )
                scaleX *= detector.scaleFactor
                scaleY *= detector.scaleFactor
                return true
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gesture.onTouchEvent(this, event)
    }
}