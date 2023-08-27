package chenchen.engine.gesture.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.ConstrainedAlignment
import chenchen.engine.gesture.ConstraintAlignment
import chenchen.engine.gesture.OnAdsorptionListener
import chenchen.engine.gesture.OnSimpleTouchListener
import chenchen.engine.gesture.adsorption.Adsorption
import chenchen.engine.gesture.adsorption.AdsorptionEdgeGestureDetector
import chenchen.engine.gesture.adsorption.Magnet
import chenchen.engine.gesture.adsorption.Magnetic

class AdsorptionGestureView : View {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    val gesture = BestGestureDetector(this).apply {
        setOnTouchListener(object : OnSimpleTouchListener() {
            override fun onTouchMove(detector: BestGestureDetector): Boolean {
                if (!adsorptionGesture.onMove(detector)) {
                    offsetLeftAndRight(detector.moveX.toInt())
                    offsetTopAndBottom(detector.moveY.toInt())
                }
                return true
            }
        })
    }

    val adsorptionGesture by lazy {
        val adsorption = Adsorption(
            Magnetic(this, ConstrainedAlignment.all()),
            arrayListOf(Magnet(parent as View, ConstraintAlignment.all())),
        )
        AdsorptionEdgeGestureDetector(adsorption, object : OnAdsorptionListener {
            override fun onBeginAdsorption(detector: AdsorptionEdgeGestureDetector): Boolean {
                return true
            }

            override fun onAdsorption(detector: AdsorptionEdgeGestureDetector): Boolean {
                offsetLeftAndRight(detector.adsorptionX)
                offsetTopAndBottom(detector.adsorptionY)
                return true
            }

            override fun onAdsorptionEnd(detector: AdsorptionEdgeGestureDetector) = Unit
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gesture.onTouchEvent(event)
    }
}