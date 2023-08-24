package chenchen.engine.gesture

import android.graphics.PointF
import chenchen.engine.gesture.adsorption.AdsorptionEdgeGestureDetector

/**
 * 手势监听
 * @author: chenchen
 * @since: 2023/4/14 11:01
 */

/**
 * 触摸手势监听适配类
 */
open class OnSimpleTouchListener : OnTouchListener {
    override fun onBeginTouch(detector: BestGestureDetector, x: Int, y: Int) = true

    override fun onTouchMove(detector: BestGestureDetector) = true

    override fun onTouchEnd(detector: BestGestureDetector) = Unit

    override fun onTouchCancel(detector: BestGestureDetector) = Unit

    override fun onPress(detector: BestGestureDetector) = Unit

    override fun onLongPress(detector: BestGestureDetector) = false

    override fun onClick(detector: BestGestureDetector) = Unit

    override fun onDoubleClick(detector: BestGestureDetector) = Unit

    override fun onLongClick(detector: BestGestureDetector) = Unit

    override fun provideRawPivot(pivot: PointF) = Unit
}

/**
 * 触摸监听
 */
interface OnTouchListener {
    /**
     * 开始响应触摸事件
     * @return 是否响应触摸事件
     */
    fun onBeginTouch(detector: BestGestureDetector, x: Int, y: Int): Boolean

    /**
     * 移动事件
     * @return 是否结束事件
     */
    fun onTouchMove(detector: BestGestureDetector): Boolean

    /**
     * 结束触摸事件
     */
    fun onTouchEnd(detector: BestGestureDetector)

    /**
     * 事件被取消
     */
    fun onTouchCancel(detector: BestGestureDetector)

    /**
     * 按压
     */
    fun onPress(detector: BestGestureDetector)

    /**
     * 长按
     * @return 是否结束事件，
     * true: 不再消费事件，这时候会触发[onLongClick]和[onTouchEnd]
     * false: 长按后继续消费事件，但不会再触发[onLongClick]
     */
    fun onLongPress(detector: BestGestureDetector): Boolean

    /**
     * 点击事件，后续会响应[onTouchEnd]
     */
    fun onClick(detector: BestGestureDetector)

    /**
     * 双击事件，后续会响应[onTouchEnd]
     */
    fun onDoubleClick(detector: BestGestureDetector)

    /**
     * 长按点击，后续会响应[onTouchEnd]
     */
    fun onLongClick(detector: BestGestureDetector)

    /**
     * 提供中心点，是相对屏幕的绝对位置，缩放、旋转、平移都需要提供中心点
     * 如果没有特殊要求可以不重写这个方法，使用当前类实现好的功能就行
     * 如果有要求需要提供中心点，[calculateCenterX]、[calculateCenterY]可以辅助使用
     */
    fun provideRawPivot(pivot: PointF)
}

/**
 * 双指移动手势监听适配类
 */
open class OnSimpleMoveListener : OnMoveListener {
    override fun onBeginMove(detector: BestGestureDetector) = true

    override fun onMove(detector: BestGestureDetector) = true

    override fun onMoveEnd(detector: BestGestureDetector) = Unit
}

/**
 * 双指移动手势监听
 */
interface OnMoveListener {
    fun onBeginMove(detector: BestGestureDetector): Boolean
    fun onMove(detector: BestGestureDetector): Boolean
    fun onMoveEnd(detector: BestGestureDetector)
}

/**
 * 双指旋转手势监听适配类
 */
open class OnSimpleRotateListener : OnRotateListener {
    override fun onBeginRotate(detector: BestGestureDetector) = true

    override fun onRotate(detector: BestGestureDetector) = true

    override fun onRotateEnd(detector: BestGestureDetector) = Unit
}

/**
 * 双指旋转手势监听
 */
interface OnRotateListener {
    fun onBeginRotate(detector: BestGestureDetector): Boolean
    fun onRotate(detector: BestGestureDetector): Boolean
    fun onRotateEnd(detector: BestGestureDetector)
}

/**
 * 双指缩放手势监听适配类
 */
open class OnSimpleScaleListener : OnScaleListener {
    override fun onBeginScale(detector: BestGestureDetector) = true

    override fun onScale(detector: BestGestureDetector) = true

    override fun onScaleEnd(detector: BestGestureDetector) = Unit
}

/**
 * 双指缩放手势监听
 */
interface OnScaleListener {
    /**
     * 开始缩放
     * @return 是否需要缩放 true 接着会调[onScale]、[onScaleEnd] false 就此结束
     */
    fun onBeginScale(detector: BestGestureDetector): Boolean

    /**
     * 缩放中
     * @return 是否结束缩放 true 接着会调[onScaleEnd] false 继续调用[onScale]
     */
    fun onScale(detector: BestGestureDetector): Boolean

    /**
     * 结束缩放，当手指数变为<2的时候或[onScale]返回false的时候会调
     */
    fun onScaleEnd(detector: BestGestureDetector)
}

/**
 * 吸附监听
 */
interface OnAdsorptionListener {
    /**
     * 开始吸附
     * 这里建议即使x,y轴达到了吸附区域，但通常只有吸附物的旋转角度为90°，180°，270°，360°/0°的时候才返回true
     */
    fun onBeginAdsorption(detector: AdsorptionEdgeGestureDetector): Boolean

    /**
     * 吸附中
     */
    fun onAdsorption(detector: AdsorptionEdgeGestureDetector): Boolean

    /**
     * 吸附结束
     */
    fun onAdsorptionEnd(detector: AdsorptionEdgeGestureDetector)
}