package chenchen.engine.gesture

import android.graphics.PointF

/**
 * 手势监听
 * @author: chenchen
 * @since: 2023/4/14 11:01
 */

/**
 * 触摸监听
 */
interface OnTouchGestureListener {
    /**
     * 开始响应触摸事件
     * @return 是否响应触摸事件
     */
    fun onBeginTouch(detector: BestGestureDetector, x: Int, y: Int): Boolean = true

    /**
     * 移动事件
     * @return 是否结束事件
     */
    fun onTouchMove(detector: BestGestureDetector): Boolean

    /**
     * 结束触摸事件
     */
    fun onTouchEnd(detector: BestGestureDetector) = Unit

    /**
     * 事件被取消
     */
    fun onTouchCancel(detector: BestGestureDetector) = Unit

    /**
     * 按压
     */
    fun onPress(detector: BestGestureDetector) = Unit

    /**
     * 长按
     * @return 是否结束事件，
     * true: 不再消费事件，这时候会触发[onLongClick]和[onTouchEnd]
     * false: 长按后继续消费事件，但不会再触发[onLongClick]
     */
    fun onLongPress(detector: BestGestureDetector): Boolean = false

    /**
     * 点击事件，后续会响应[onTouchEnd]
     */
    fun onClick(detector: BestGestureDetector) = Unit

    /**
     * 双击事件，后续会响应[onTouchEnd]
     */
    fun onDoubleClick(detector: BestGestureDetector) = Unit

    /**
     * 长按点击，后续会响应[onTouchEnd]
     */
    fun onLongClick(detector: BestGestureDetector) = Unit

    /**
     * 提供中心点，是相对屏幕的绝对位置，缩放、旋转、平移都需要提供中心点
     * 如果没有特殊要求可以不重写这个方法，使用当前类实现好的功能就行
     * 如果有要求需要提供中心点，[calculateCenterX]、[calculateCenterY]可以辅助使用
     */
    fun provideRawPivot(pivot: PointF) = Unit
}

/**
 * 双指移动手势监听
 */
interface OnMoveGestureListener {
    fun onBeginMove(detector: BestGestureDetector): Boolean = true
    fun onMove(detector: BestGestureDetector): Boolean
    fun onMoveEnd(detector: BestGestureDetector) = Unit
}

/**
 * 双指旋转手势监听
 */
interface OnRotateGestureListener {
    fun onBeginRotate(detector: BestGestureDetector): Boolean = true
    fun onRotate(detector: BestGestureDetector): Boolean
    fun onRotateEnd(detector: BestGestureDetector) = Unit
}

/**
 * 双指缩放手势监听
 */
interface OnScaleGestureListener {
    /**
     * 开始缩放
     * @return 是否需要缩放 true 接着会调[onScale]、[onScaleEnd] false 就此结束
     */
    fun onBeginScale(detector: BestGestureDetector): Boolean = true

    /**
     * 缩放中
     * @return 是否结束缩放 true 接着会调[onScaleEnd] false 继续调用[onScale]
     */
    fun onScale(detector: BestGestureDetector): Boolean

    /**
     * 结束缩放，当手指数变为<2的时候或[onScale]返回false的时候会调
     */
    fun onScaleEnd(detector: BestGestureDetector) = Unit
}