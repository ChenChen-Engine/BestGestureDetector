package chenchen.engine.gesture

import android.graphics.PointF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import chenchen.engine.gesture.compat.MotionEventCompat
import chenchen.engine.gesture.compat.MotionEventCompat.Companion.compat
import chenchen.engine.gesture.compat.MotionEventCompat.Companion.obtain
import java.lang.NullPointerException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 手势相关的状态
 * @author: chenchen
 * @since: 2023/4/14 11:03
 */
internal data class BestGestureState(
    /**
     * 起始事件
     */
    var startEvent: MotionEventCompat? = null,

    /**
     * 上一次的事件
     */
    var previousEvent: MotionEventCompat? = null,

    /**
     * 当前事件
     */
    var currentEvent: MotionEventCompat? = null,

    /**
     * 单指手势时需要提供的中心点
     */
    val pivot: PointF = PointF(),

    /**
     * 手指id
     */
    val pointerIds: ArrayList<Int> = arrayListOf(),

    /**
     * 已追踪的手指id
     */
    val currentTrackPointerIds: ArrayList<Int> = arrayListOf(),

    /**
     * 已追踪的手指id
     */
    val previousTrackPointerIds: ArrayList<Int> = arrayListOf(),

    /**
     * 双指是否正在缩放中
     */
    var isInScaleProgress: Boolean = false,

    /**
     * 双指是否正在旋转中
     */
    var isInRotateProgress: Boolean = false,

    /**
     * 双指是否正在移动中
     */
    var isInMoveProgress: Boolean = false,

    /**
     * 是否单指手势
     */
    var isInSingleFingerProgress: Boolean = false,

    /**
     * 是否双指手势
     */
    var isInMultiFingerProgress: Boolean = false,

    /**
     * 是否处于单指长按当中，如果处于长按，需要自己处理[MotionEvent.ACTION_UP]
     */
    var isInLongPressProgress: Boolean = false,

    /**
     * 是否处于单指单次按压滑动中，如果处于单指滑动，需要自己处理[MotionEvent.ACTION_UP]
     */
    var isInSingleTapScrollProgress: Boolean = false,

    /**
     * 是否处于单指两次按压(双击)滑动中，如果处于单指滑动，需要自己处理[MotionEvent.ACTION_UP]
     */
    var isInDoubleTapScrollingProgress: Boolean = false,

    /**
     * 是否触发双击
     */
    var isTriggerDoubleClick: Boolean = false,

    /**
     * 是否使用过双指
     */
    var isUsedMultiFinger: Boolean = false,

    /**
     * 是否完成一次手势，即执行过[recycleState]
     */
    var isCompletedGesture: Boolean = true,

    /**
     * false 关闭双击，关闭双击后单击的响应会快一点，true 开启双击，开启双击后需要等待双击响应时间超时，单击响应就会慢一点
     */
    var isEnableDoubleClick: Boolean = true,

    /**
     *
     * 是否处于单次按压后滑动，就放弃点击事件
     */
    var isInSingleTapScrollingGiveUpClick: Boolean = false,

    /**
     * 是否处于两次按压（双击）后滑动，就放弃点击事件
     */
    var isInDoubleTapScrollingGiveUpClick: Boolean = false,

    /**
     * 追踪的手指数量，最低数量为2，
     * 单指设置无效，单指只追踪一根手指，多指至少追踪两根手指
     */
    var trackPointerIdCount: Int = 2,

    /**
     * 在一次事件中消费掉部分moveX的值
     */
    var consumeMoveX: Float = 0f,

    /**
     * 在一次事件中消费掉部分moveY的值
     */
    var consumeMoveY: Float = 0f,

    /**
     * 在一次事件中消费掉部分rotation的值
     */
    var consumeRotation: Float = 0f,

    /**
     * 在一次事件中消费掉部分scaleFactor的值
     */
    var consumeScaleFactor: Float = 0f,

    /**
     * 指定累积移动x轴的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateMoveX: Float = 0f,

    /**
     * 指定累积移动y轴的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateMoveY: Float = 0f,

    /**
     * 指定累积旋转的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateRotation: Float = 0f,

    /**
     * 指定累积缩放的值，每累积到该值，触发一次消费，只在本次手势结束前有效，下次手势开始前将重置
     */
    var accumulateScale: Float = 0f,

    /**
     * 记录移动x轴的值，每当值是[accumulateMoveX]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateMoveX: Float = 0f,

    /**
     * 记录移动y轴的值，每当值是[accumulateMoveY]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateMoveY: Float = 0f,

    /**
     * 记录旋转的值，每当值是[accumulateRotation]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateRotation: Float = 0f,

    /**
     * 记录缩放的值，每当值是[accumulateScale]倍数则消费一次，只在本次手势结束前有效，下次手势开始前将重置
     */
    var rememberAccumulateScale: Float = 0f,
) {

    private val TAG = "BestGestureState"

    /**
     * 记录当前Event
     * 兼容Android10以下[MotionEvent.getRawX]会因为[android.view.ViewGroup]分发的过程中被转换过的问题
     * [MotionEvent.getRawX]应该是屏幕的绝对坐标，不受任何变化影象。
     * 导致的现象则是会出现坐标抖动
     */
    open fun rememberCurrentEvent(view: View, event: MotionEvent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (!view.matrix.isIdentity) {
                event.transform(view.matrix)
            }
        }
        if (currentEvent != null) {
            currentEvent?.recycle()
        }
        currentEvent = event.compat()
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
            previousTrackPointerIds.clear()
            previousTrackPointerIds.addAll(currentTrackPointerIds)
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
     * 设置追踪的手指数量，最低数量为2
     * 单指设置无效，单指只追踪一根手指，多指至少追踪两根手指
     * @param count 手指数量，必须>=2
     */
    fun rememberTrackPointerIdCount(count: Int) {
        trackPointerIdCount = max(count, 2)
        while (currentTrackPointerIds.size > trackPointerIdCount) {
            currentTrackPointerIds.removeLastOrNull()
        }
    }

    /**
     * 获取追踪的手指
     */
    fun getTrackPointerIds(event: MotionEventCompat?): List<Int> {
        return when (event) {
            currentEvent -> currentTrackPointerIds
            previousEvent -> previousTrackPointerIds
            else -> throw NullPointerException("没有匹配的事件")
        }
    }

    /**
     * 记录手指id
     */
    fun rememberPointerId() {
        val currentEvent = currentEvent ?: return
        val actionIndex = currentEvent.actionIndex
        when (currentEvent.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerId = currentEvent.getPointerId(actionIndex)
                if (!pointerIds.contains(pointerId)) {
                    pointerIds.add(currentEvent.getPointerId(actionIndex))
                }
                updateTrackPointerIds()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val removedId = currentEvent.getPointerId(actionIndex)
                pointerIds.remove(removedId)
                updateTrackPointerIds()
            }
        }
    }

    /**
     * 更新追踪的手指
     */
    private fun updateTrackPointerIds() {
        //总是追踪最末尾的几根手指，例如
        //pointerIds = [1, 2, 3, 4, 5]
        //1.
        //trackPointerIdCount = 2
        //trackPointerIds = [4, 5]
        //2.
        //trackPointerIdCount = 4
        //trackPointerIds = [2, 3, 4, 5]
        var startIndex = pointerIds.size - trackPointerIdCount
        if (startIndex < 0) {
            startIndex = 0
        }
        currentTrackPointerIds.clear()
        for (index in startIndex until pointerIds.size) {
            currentTrackPointerIds.add(pointerIds[index])
        }
    }

    /**
     * 设置累积移动值
     */
    fun accumulateMoveX(value: Float) {
        require(value > 0) { "设置BestGestureDetector.accumulateMoveX()的值必须大于0, value:${value}" }
        accumulateMoveX = value
        rememberAccumulateMoveX = 0f
    }

    /**
     * 设置累积移动值
     */
    fun accumulateMoveY(value: Float) {
        require(value > 0) { "设置BestGestureDetector.accumulateMoveY()的值必须大于0, value:${value}" }
        accumulateMoveY = value
        rememberAccumulateMoveY = 0f
    }

    /**
     * 设置累积旋转值
     */
    fun accumulateRotation(value: Float) {
        require(value > 0) { "设置BestGestureDetector.accumulateRotation()的值必须大于0, value:${value}" }
        accumulateRotation = value
        rememberAccumulateRotation = 0f
    }

    /**
     * 记录累积缩放值
     */
    fun accumulateScale(value: Float) {
        require(value > 0) { "设置BestGestureDetector.accumulateScale()的值必须大于0, value:${value}" }
        accumulateScale = value
        rememberAccumulateScale = 0f
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
     * 是否是累积消费移动X模式
     */
    fun isAccumulateMoveXMode(): Boolean {
        return accumulateMoveX > 0
    }

    /**
     * 是否是累积消费移动Y模式
     */
    fun isAccumulateMoveYMode(): Boolean {
        return accumulateMoveY > 0
    }

    /**
     * 是否是累积消费旋转模式
     */
    fun isAccumulateRotationMode(): Boolean {
        return accumulateRotation > 0
    }

    /**
     * 是否是累积消费缩放模式
     */
    fun isAccumulateScaleMode(): Boolean {
        return accumulateScale > 0
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
        isInSingleFingerProgress = false
        isInMultiFingerProgress = false
        isInLongPressProgress = false
        isInSingleTapScrollProgress = false
        isInDoubleTapScrollingProgress = false
        pivot.set(0f, 0f)
        consumeMoveX = 0f
        consumeMoveY = 0f
        consumeRotation = 0f
        consumeScaleFactor = 0f
        isTriggerDoubleClick = false
        pointerIds.clear()
        currentTrackPointerIds.clear()
        previousTrackPointerIds.clear()
        isUsedMultiFinger = false
        isCompletedGesture = true
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