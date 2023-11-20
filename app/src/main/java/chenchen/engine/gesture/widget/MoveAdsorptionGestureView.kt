package chenchen.engine.gesture.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.ConstrainedAlignment
import chenchen.engine.gesture.ConstraintAlignment
import chenchen.engine.gesture.OnTouchGestureListener
import chenchen.engine.gesture.adsorption.move.Adsorption
import chenchen.engine.gesture.adsorption.move.Magnet
import chenchen.engine.gesture.adsorption.move.Magnetic
import chenchen.engine.gesture.adsorption.move.MoveAdsorptionGestureDetector
import chenchen.engine.gesture.adsorption.move.OnMoveAdsorptionListener

class MoveAdsorptionGestureView : View {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = 0xFF0ba508.toInt()
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 30f), 1f)
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    val gesture = BestGestureDetector(this).apply {
        setOnTouchListener(object : OnTouchGestureListener {
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
        MoveAdsorptionGestureDetector(adsorption, object : OnMoveAdsorptionListener {
            override fun onBeginAdsorption(detector: MoveAdsorptionGestureDetector): Boolean {
                return true
            }

            override fun onAdsorption(detector: MoveAdsorptionGestureDetector): Boolean {
                offsetLeftAndRight(detector.adsorptionX)
                offsetTopAndBottom(detector.adsorptionY)
                return true
            }

            override fun onAdsorptionEnd(detector: MoveAdsorptionGestureDetector) = Unit
        })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gesture.onTouchEvent(event)
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