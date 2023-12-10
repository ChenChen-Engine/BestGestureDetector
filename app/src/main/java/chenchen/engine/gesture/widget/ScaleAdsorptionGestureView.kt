package chenchen.engine.gesture.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.OnTouchGestureListener
import chenchen.engine.gesture.OnScaleGestureListener
import chenchen.engine.gesture.adsorption.scale.ScaleAdsorption
import chenchen.engine.gesture.adsorption.scale.ScaleMagnet
import chenchen.engine.gesture.adsorption.scale.ScaleMagnetic
import chenchen.engine.gesture.adsorption.scale.OnScaleAdsorptionListener
import chenchen.engine.gesture.adsorption.scale.ScaleAdsorptionGestureDetector

/**
 * @author: chenchen
 * @since: 2023/11/7 9:40
 */
class ScaleAdsorptionGestureView : View {

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
            override fun onTouchMove(detector: BestGestureDetector) = true
        })
        setScaleListener(object : OnScaleGestureListener {
            override fun onScale(detector: BestGestureDetector): Boolean {
                if (!adsorptionGesture.onScale(detector)) {
                    this@ScaleAdsorptionGestureView.scaleX *= detector.scaleFactor
                    this@ScaleAdsorptionGestureView.scaleY *= detector.scaleFactor
                }
                return true
            }
        })
    }

    val adsorptionGesture by lazy {
        val adsorption = ScaleAdsorption(
            ScaleMagnetic(this),
            arrayListOf(
                ScaleMagnet(0.5f, sRidThreshold = 0.5f), ScaleMagnet(0.8f, sRidThreshold = 0.5f),
                ScaleMagnet(1.0f, sRidThreshold = 0.5f), ScaleMagnet(1.5f, sRidThreshold = 0.5f),
                ScaleMagnet(2.0f, sRidThreshold = 0.5f), ScaleMagnet(2.5f, sRidThreshold = 0.5f),
                ScaleMagnet(3.0f, sRidThreshold = 0.5f), ScaleMagnet(4.0f, sRidThreshold = 0.5f),
            )
        )
        ScaleAdsorptionGestureDetector(adsorption, object : OnScaleAdsorptionListener {
            override fun onBeginAdsorption(detector: ScaleAdsorptionGestureDetector): Boolean {
                return true
            }

            override fun onAdsorption(detector: ScaleAdsorptionGestureDetector): Boolean {
                this@ScaleAdsorptionGestureView.scaleX = detector.adsorptionScale
                this@ScaleAdsorptionGestureView.scaleY = detector.adsorptionScale
                return true
            }

            override fun onAdsorptionEnd(detector: ScaleAdsorptionGestureDetector) {

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