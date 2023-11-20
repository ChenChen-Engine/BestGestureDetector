package chenchen.engine.gesture

import android.graphics.PointF
import android.view.MotionEvent
import chenchen.engine.gesture.compat.MotionEventCompat
import chenchen.engine.gesture.compat.MotionEventCompat.Companion.compat
import chenchen.engine.gesture.compat.MotionEventCompat.Companion.obtain
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 手势相关的状态
 * @author: chenchen
 * @since: 2023/4/14 11:03
 */
internal open class BestGestureState {

    private val TAG = "BestGestureState"

    /**
     * 起始事件
     */
    var startEvent: MotionEventCompat? = null
        private set

    /**
     * 上一次的事件
     */
    var previousEvent: MotionEventCompat? = null
        private set

    /**
     * 当前事件
     */
    var currentEvent: MotionEventCompat? = null
        private set

    /**
     * 单指手势时需要提供的中心点
     */
    val pivot = PointF()

    /**
     * 主手指Id
     */
    var majorId = MotionEvent.INVALID_POINTER_ID

    /**
     * 次手指Id
     */
    var minorId = MotionEvent.INVALID_POINTER_ID

    /**
     * 手指id
     */
    val pointerIds = arrayListOf<Int>()

    /**
     * 双指是否正在缩放中
     */
    var isInScaleProgress = false

    /**
     * 双指是否正在旋转中
     */
    var isInRotateProgress = false

    /**
     * 双指是否正在移动中
     */
    var isInMoveProgress = false

    /**
     * 是否吸附中
     */
    var isInAdsorptionProgress = false

    /**
     * 是否单指手势
     */
    var isInSingleFingerProgress = false
        private set

    /**
     * 是否双指手势
     */
    var isInMultiFingerProgress = false
        private set

    /**
     * 是否处于单指长按当中，如果处于长按，需要自己处理[MotionEvent.ACTION_UP]
     */
    var isInLongPressProgress = false

    /**
     * 是否处于单指单次按压滑动中，如果处于单指滑动，需要自己处理[MotionEvent.ACTION_UP]
     */
    var isInSingleTapScrollProgress = false

    /**
     * 是否处于单指两次按压(双击)滑动中，如果处于单指滑动，需要自己处理[MotionEvent.ACTION_UP]
     */
    var isInDoubleTapScrollingProgress = false

    /**
     * 是否触发双击
     */
    var isTriggerDoubleClick = false

    /**
     * 是否使用过双指
     */
    var isUsedMultiFinger = false

    /**
     * 是否完成一次手势，即执行过[recycleState]
     */
    var isCompletedGesture = true

    /**
     * false 关闭双击，关闭双击后单击的响应会快一点，true 开启双击，开启双击后需要等待双击响应时间超时，单击响应就会慢一点
     */
    var isEnableDoubleClick = true

    /**
     *
     * 是否处于单次按压后滑动，就放弃点击事件
     */
    var isInSingleTapScrollingGiveUpClick = false

    /**
     * 是否处于两次按压（双击）后滑动，就放弃点击事件
     */
    var isInDoubleTapScrollingGiveUpClick = false

    /**
     * 触摸的X点
     */
    var touchX: Float = 0f
        get() {
            return field - offsetTouchX
        }

    /**
     * 触摸的Y点
     */
    var touchY: Float = 0f
        get() {
            return field - offsetTouchY
        }

    /**
     * 触摸的原始X点
     */
    var touchRawX: Float = 0f
        get() {
            return field - offsetTouchRawX
        }

    /**
     * 触摸的原始Y点
     */
    var touchRawY: Float = 0f
        get() {
            return field - offsetTouchRawY
        }

    /**
     * 在一次事件中消费掉部分moveX的值
     */
    var consumeMoveX = 0f

    /**
     * 在一次事件中消费掉部分moveY的值
     */
    var consumeMoveY = 0f

    /**
     * 在一次事件中消费掉部分rotation的值
     */
    var consumeRotation = 0f

    /**
     * 在一次事件中消费掉部分scaleFactor的值
     */
    var consumeScaleFactor = 0f

    /**
     * 偏移当前触摸的X点，只在当前事件中有效，下次事件将重置
     */
    var offsetTouchX = 0f

    /**
     * 偏移当前触摸的Y点，只在当前事件中有效，下次事件将重置
     */
    var offsetTouchY = 0f

    /**
     * 偏移当前触摸相对屏幕的X点，只在当前事件中有效，下次事件将重置
     */
    var offsetTouchRawX = 0f

    /**
     * 偏移当前触摸相对屏幕的Y点，只在当前事件中有效，下次事件将重置
     */
    var offsetTouchRawY = 0f

    /**
     * 指定累积移动x轴的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateMoveX = 0f

    /**
     * 指定累积移动y轴的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateMoveY = 0f

    /**
     * 指定累积旋转的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateRotation = 0f

    /**
     * 指定累积缩放的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateScale = 0f

    /**
     * 记录移动x轴的值，每当值是[accumulateMoveX]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateMoveX = 0f

    /**
     * 记录移动y轴的值，每当值是[accumulateMoveY]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateMoveY = 0f

    /**
     * 记录旋转的值，每当值是[accumulateRotation]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateRotation = 0f

    /**
     * 记录缩放的值，每当值是[accumulateScale]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateScale = 0f

    /**
     * 记录当前Event
     */
    open fun rememberCurrentEvent(event: MotionEvent) {
        if (currentEvent != null) {
            currentEvent?.recycle()
        }
        currentEvent = event.compat()
        touchX = currentEvent!!.x
        touchY = currentEvent!!.y
        touchRawX = currentEvent!!.rawX
        touchRawY = currentEvent!!.rawY
        offsetTouchX = 0f
        offsetTouchY = 0f
        offsetTouchRawX = 0f
        offsetTouchRawY = 0f
    }

    /**
     * 记录起始Event
     */
    fun rememberStartEvent() {
        if (startEvent != null) {
            startEvent?.recycle()
        }
        if (currentEvent != null) {
            startEvent = currentEvent!!.obtain()
        }
    }

    /**
     * 记录上一个Event
     */
    fun rememberPreviousEvent() {
        if (previousEvent != null) {
            previousEvent?.recycle()
        }
        if (currentEvent != null) {
            previousEvent = currentEvent!!.obtain()
        }
    }

    /**
     * 在Down的时候记录中心点
     */
    fun rememberPivot(x: Float, y: Float) {
        pivot.x = x
        pivot.y = y
    }

    /**
     * 使用单指手势
     */
    fun useSingleFinger(isHandle: Boolean) {
        isInSingleFingerProgress = isHandle
        if (isInSingleFingerProgress) {
            isInMultiFingerProgress = !isInSingleFingerProgress
        }
    }

    /**
     * 使用双指手势
     */
    fun useMultiFinger(isHandle: Boolean) {
        isInMultiFingerProgress = isHandle
        isInSingleFingerProgress = !isInMultiFingerProgress
    }

    /**
     * 记录手指id
     */
    fun rememberPointerId() {
        val currentEvent = currentEvent ?: return
        val actionIndex = currentEvent.actionIndex
        when (currentEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val pointerId = currentEvent.getPointerId(actionIndex)
                if (!pointerIds.contains(pointerId)) {
                    pointerIds.add(currentEvent.getPointerId(actionIndex))
                }
                majorId = pointerIds.last()
                minorId = MotionEvent.INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = currentEvent.getPointerId(actionIndex)
                if (!pointerIds.contains(pointerId)) {
                    pointerIds.add(currentEvent.getPointerId(actionIndex))
                }
                majorId = pointerIds[pointerIds.lastIndex - 1]
                minorId = pointerIds.last()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val removedId = currentEvent.getPointerId(actionIndex)
                if (removedId == majorId || removedId == minorId) {
                    for (i in pointerIds.lastIndex downTo 0) {
                        val id = pointerIds[i]
                        if (currentEvent.pointerCount == 2) {
                            //单指松手后，重新追踪最后一根并且不是松开的手指
                            if (id != removedId) {
                                majorId = id
                                minorId = MotionEvent.INVALID_POINTER_ID
                                break
                            }
                        } else if (currentEvent.pointerCount > 2) {
                            //双指松手后，重新追踪最后两根并且不是松开的手指
                            if (pointerIds.size > 2) {
                                if (id != majorId && id != minorId) {
                                    if (removedId == majorId) {
                                        //如果松开的是主手指，那么把副手指变成主手指，副手指变成最后一根手指并且不是松开的手指
                                        majorId = minorId
                                        minorId = id
                                    } else if (removedId == minorId) {
                                        //如果松开的是副手指，那么就把主手指变成副手指，再重新找一根主手指
                                        minorId = majorId
                                        majorId = id
                                    }
                                    break
                                }
                            } else {
                                //如果只有两根手指，那么重新查找主手指，重置副手指
                                if (id != removedId) {
                                    majorId = id
                                    minorId = MotionEvent.INVALID_POINTER_ID
                                    break
                                }
                            }
                        }
                    }
                }
                pointerIds.remove(removedId)
            }
        }
    }


    /**
     * 记录移动x轴手势的累积值，记录到一定值就会消费
     */
    fun rememberAccumulateMove(moveX: Float, moveY: Float) {
        if (accumulateMoveX > 0f) {
            this.rememberAccumulateMoveX += moveX
        }
        if (accumulateMoveY > 0f) {
            this.rememberAccumulateMoveY += moveY
        }
    }

    /**
     * 记录旋转手势的累积值，记录到一定值就会消费
     */
    fun rememberAccumulateRotation(rotation: Float) {
        if (accumulateRotation == 0f) {
            return
        }
        this.rememberAccumulateRotation += rotation
    }

    /**
     * 记录缩放手势的累积值，记录到一定值就会消费
     */
    fun rememberAccumulateScale(scale: Float) {
        if (accumulateScale == 0f) {
            return
        }
        this.rememberAccumulateScale += scale - 1f
    }

    /**
     * 能否消费累积的x轴移动值
     */
    fun canConsumeAccumulateMoveX(): Boolean {
        if (accumulateMoveX == 0f) {
            return false
        }
        if (abs(rememberAccumulateMoveX) - accumulateMoveX >= 0) {
            return true
        }
        return false
    }

    fun canConsumeAccumulateMoveY(): Boolean {
        if (accumulateMoveY == 0f) {
            return false
        }
        if (abs(rememberAccumulateMoveY) - accumulateMoveY >= 0) {
            return true
        }
        return false
    }

    /**
     * 能否消费累积的旋转值
     */
    fun canConsumeAccumulateRotation(): Boolean {
        if (accumulateRotation == 0f) {
            return false
        }
        if (abs(rememberAccumulateRotation) - accumulateRotation >= 0) {
            return true
        }
        return false
    }

    /**
     * 能否消费累积的缩放值
     */
    fun canConsumeAccumulateScale(): Boolean {
        if (accumulateScale == 0f) {
            return false
        }
        if (abs(rememberAccumulateScale) - accumulateScale >= 0) {
            return true
        }
        return false
    }

    /**
     * 消费一次x轴移动累积值
     */
    fun consumeAccumulateMoveX(): Boolean {
        if (!canConsumeAccumulateMoveX()) {
            return false
        }
        if (rememberAccumulateMoveX < 0) {
            rememberAccumulateMoveX += accumulateMoveX
        } else {
            rememberAccumulateMoveX -= accumulateMoveX
        }
        return true
    }

    /**
     * 消费一次y轴移动累积值
     */
    fun consumeAccumulateMoveY(): Boolean {
        if (!canConsumeAccumulateMoveY()) {
            return false
        }
        if (rememberAccumulateMoveY < 0) {
            rememberAccumulateMoveY += accumulateMoveY
        } else {
            rememberAccumulateMoveY -= accumulateMoveY
        }
        return true
    }

    /**
     * 消费一次旋转累积值
     */
    fun consumeAccumulateRotation(): Boolean {
        if (!canConsumeAccumulateRotation()) {
            return false
        }
        if (rememberAccumulateRotation < 0) {
            rememberAccumulateRotation += accumulateRotation
        } else {
            rememberAccumulateRotation -= accumulateRotation
        }
        return true
    }

    /**
     * 消费一次缩放累积值
     */
    fun consumeAccumulateScale(): Boolean {
        if (!canConsumeAccumulateScale()) {
            return false
        }
        if (rememberAccumulateScale < 0) {
            rememberAccumulateScale += accumulateScale
        } else {
            rememberAccumulateScale -= accumulateScale
        }
        return true
    }

    /**
     * 获取经过层层状态过滤后最终的移动值
     * @param moveX 原始x轴移动值
     */
    fun getMoveX(moveX: Float): Float {
        var moveX = moveX
        if (accumulateMoveX > 0) {
            if (abs(rememberAccumulateMoveX) - accumulateMoveX < 0) {
                return 0f
            } else {
                moveX = when {
                    rememberAccumulateMoveX < 0 -> -accumulateMoveX
                    rememberAccumulateMoveX > 0 -> accumulateMoveX
                    else -> 0f
                }
            }
        }
        val value = if (moveX < 0f) {
            min(moveX + consumeMoveX, 0f)
        } else if (moveX > 0f) {
            max(moveX - consumeMoveX, 0f)
        } else {
            moveX
        }
        return value
    }

    /**
     * 获取经过层层状态过滤后最终的移动值
     * @param moveY 原始y轴移动值
     */
    fun getMoveY(moveY: Float): Float {
        var moveY = moveY
        if (accumulateMoveY > 0) {
            if (abs(rememberAccumulateMoveY) - accumulateMoveY < 0) {
                return 0f
            } else {
                moveY = when {
                    rememberAccumulateMoveY < 0 -> -accumulateMoveY
                    rememberAccumulateMoveY > 0 -> accumulateMoveY
                    else -> 0f
                }
            }
        }
        val value = if (moveY < 0f) {
            min(moveY + consumeMoveY, 0f)
        } else if (moveY > 0f) {
            max(moveY - consumeMoveY, 0f)
        } else {
            moveY
        }
        return value
    }

    /**
     * 获取经过层层状态过滤后最终的旋转值
     * @param rotation 原始旋转值
     */
    fun getRotation(rotation: Float): Float {
        var rotation = rotation
        if (accumulateRotation > 0) {
            if (abs(rememberAccumulateRotation) - accumulateRotation < 0) {
                return 0f
            } else {
                rotation = when {
                    rememberAccumulateRotation < 0 -> -accumulateRotation
                    rememberAccumulateRotation > 0 -> accumulateRotation
                    else -> 0f
                }

            }
        }
        val value = if (rotation < 0f) {
            min(rotation + consumeRotation, 0f)
        } else if (rotation > 0f) {
            max(rotation - consumeRotation, 0f)
        } else {
            rotation
        }
        return value
    }

    /**
     * 获取经过层层状态过滤后最终的缩放比
     * @param scaleFactor 原始缩放比
     */
    fun getScaleFactor(scaleFactor: Float): Float {
        var scaleFactor = scaleFactor
        if (accumulateScale > 0) {
            if (abs(rememberAccumulateScale) - accumulateScale < 0) {
                return 1f
            } else {
                scaleFactor = when {
                    rememberAccumulateScale > 0f -> 1 + accumulateScale
                    rememberAccumulateScale < 0f -> 1 - accumulateScale
                    else -> 1f
                }
            }
        }
        val value = if (scaleFactor < 1f) {
            min(scaleFactor + consumeScaleFactor, 1f)
        } else if (scaleFactor > 1f) {
            max(scaleFactor - consumeScaleFactor, 1f)
        } else {
            scaleFactor
        }
        return value
    }

    /**
     * 把消费事件的值置空
     */
    fun resetConsumeValue() {
        consumeMoveX = 0f
        consumeMoveY = 0f
        consumeRotation = 0f
        consumeScaleFactor = 0f
    }

    /**
     * 释放状态
     */
    fun recycleState() {
        startEvent?.recycle()
        currentEvent?.recycle()
        previousEvent?.recycle()
        startEvent = null
        currentEvent = null
        previousEvent = null
        isInScaleProgress = false
        isInMoveProgress = false
        isInAdsorptionProgress = false
        isInSingleFingerProgress = false
        isInMultiFingerProgress = false
        isInLongPressProgress = false
        isInSingleTapScrollProgress = false
        isInDoubleTapScrollingProgress = false
        majorId = MotionEvent.INVALID_POINTER_ID
        minorId = MotionEvent.INVALID_POINTER_ID
        pivot.set(0f, 0f)
        touchX = 0f
        touchY = 0f
        touchRawX = 0f
        touchRawY = 0f
        consumeMoveX = 0f
        consumeMoveY = 0f
        consumeRotation = 0f
        consumeScaleFactor = 0f
        isTriggerDoubleClick = false
        pointerIds.clear()
        isUsedMultiFinger = false
        isCompletedGesture = true
        offsetTouchX = 0f
        offsetTouchY = 0f
        offsetTouchRawX = 0f
        offsetTouchRawY = 0f
        accumulateMoveX = 0f
        accumulateMoveY = 0f
        accumulateRotation = 0f
        accumulateScale = 0f
        rememberAccumulateMoveX = 0f
        rememberAccumulateMoveY = 0f
        rememberAccumulateRotation = 0f
        rememberAccumulateScale = 0f
    }
}