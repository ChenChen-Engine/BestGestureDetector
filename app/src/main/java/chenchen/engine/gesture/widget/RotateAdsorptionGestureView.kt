package chenchen.engine.gesture.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.OnScaleGestureListener
import chenchen.engine.gesture.OnTouchGestureListener
import chenchen.engine.gesture.adsorption.rotate.RotateAdsorption
import chenchen.engine.gesture.adsorption.rotate.RotateMagnet
import chenchen.engine.gesture.adsorption.rotate.RotateMagnetic
import chenchen.engine.gesture.adsorption.rotate.OnRotateAdsorptionListener
import chenchen.engine.gesture.adsorption.rotate.RotateAdsorptionGestureDetector

/**
 * @author: chenchen
 * @since: 2023/11/7 9:40
 */
class RotateAdsorptionGestureView : View {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = 0xFF0ba508.toInt()
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 30f), 1f)
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    val gesture = BestGestureDetector(context).apply {
        setOnTouchListener(object : OnTouchGestureListener {
            override fun onTouchMove(detector: BestGestureDetector): Boolean {
                return true
            }
        })
        setScaleListener(object:OnScaleGestureListener{
            override fun onScale(detector: BestGestureDetector): Boolean {
                if (!adsorptionGesture.onRotate(detector)) {
                    this@RotateAdsorptionGestureView.rotation += detector.rotation
                }
                return true
            }
        })
    }

    val adsorptionGesture by lazy {
        val adsorption = RotateAdsorption(
            RotateMagnetic(this),
            arrayListOf(
                RotateMagnet(0, rRidThreshold = 50), RotateMagnet(45, rRidThreshold = 50),
                RotateMagnet(90, rRidThreshold = 50), RotateMagnet(135, rRidThreshold = 50),
                RotateMagnet(180, rRidThreshold = 50), RotateMagnet(225, rRidThreshold = 50),
                RotateMagnet(270, rRidThreshold = 50), RotateMagnet(315, rRidThreshold = 50),
                RotateMagnet(360, rRidThreshold = 50)),
        )
        RotateAdsorptionGestureDetector(adsorption, object : OnRotateAdsorptionListener {
            override fun onBeginAdsorption(detector: RotateAdsorptionGestureDetector): Boolean {
                return true
            }

            override fun onAdsorption(detector: RotateAdsorptionGestureDetector): Boolean {
                this@RotateAdsorptionGestureView.rotation += detector.adsorptionRotation
                return true
            }

            override fun onAdsorptionEnd(detector: RotateAdsorptionGestureDetector) {

            }
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gesture.onTouchEvent(this, event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(0f, 0f, 0f, height.toFloat(), paint)
        canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), paint)
        canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, paint)
        canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, paint)
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), paint)
    }
}