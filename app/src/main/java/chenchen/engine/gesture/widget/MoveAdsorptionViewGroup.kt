package chenchen.engine.gesture.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout

class MoveAdsorptionViewGroup : FrameLayout {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = 0xFF0ba508.toInt()
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 30f), 1f)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        setWillNotDraw(false)
    }
    val rectFs = arrayListOf<RectF>()
    fun setDrawChild(rect: RectF) {
        rectFs.add(rect)
        invalidate()
    }



    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = 0xFF0ba508.toInt()
        canvas.drawLine(0f, 0f, 0f, height.toFloat(), paint)
        canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), paint)
        canvas.drawLine(width.toFloat(), 0f, width.toFloat(), height.toFloat(), paint)
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, paint)
        canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, paint)
        canvas.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), paint)
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        paint.color = 0x33262776.toInt()
        for (rectF in rectFs) {
            canvas.drawRect(rectF, paint)
        }
        for (rectF in rectFs) {
            canvas.drawLine(rectF.centerX(), 0f, rectF.centerX(), height.toFloat(), paint)
        }
    }
}