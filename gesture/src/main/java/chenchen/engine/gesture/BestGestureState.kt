package chenchen.engine.gesture

import android.graphics.Matrix
import android.graphics.PointF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.GestureDetectorCompat
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
 * ## 命名规则
 * rememberXxx：记录内部状态
 * setupXxx：提供外部设置状态
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

    /**
     * 当按压后滑动([MotionEvent.ACTION_MOVE])超过这个阈值就不触发点击事件，对单击、双击都有效
     * 最大范围指的是[View]的大小，[cancelClickScrollThreshold]需要小于等于[View]的大小，当触摸超出[View]的范围，即使满足
     * 这个阈值，也不会触发点击，此时大概率会被[MotionEvent.ACTION_CANCEL]
     * #
     * PS: 设置的值大于[ViewConfiguration.getScaledTouchSlop] * 2无效
     * #
     * PS: 这是未来理想的功能，但目前无法实现，因为采用了原生[GestureDetectorCompat]，在点击行为上和[View]原始的行为不一致
     * [View]可以DOWN后任意MOVE，最后UP时也算点击事件，而[GestureDetectorCompat]在DOWN后MOVE超出阈值，就无法响应点击
     * 并且这个阈值无法修改，不能实现自定义阈值，未来可能会通过一些手段支持。
     * #
     * PS: 如果[View]在跟着手势移动，这个值没有参考意义，因为x/y不会变，不会认为在滑动
     */
    var cancelClickScrollThreshold: Float = defaultCancelClickScrollThreshold,
) {

    private val TAG = "BestGestureState"

    companion object {
        /**
         * 默认滑动阈值，我不想做限制，但[GestureDetectorCompat]做了限制，
         * 目前不适合定义非0的值，假设定义了1，[View]放大了10倍，当我看起来已经滑动了9，实际上[View]才滑动了0.9
         * 在解决[GestureDetectorCompat]的问题之前，这个值都是0
         */
        const val defaultCancelClickScrollThreshold = 0f
    }

    /**
     * 记录当前Event
     */
    open fun rememberCurrentEvent(view: View, event: MotionEvent) {
        if (currentEvent != null) {
            currentEvent?.recycle()
        }
        currentEvent = event.compat(view)
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
    fun rememberUseSingleFinger(isHandle: Boolean) {
        isInSingleFingerProgress = isHandle
        if (isInSingleFingerProgress) {
            isInMultiFingerProgress = !isInSingleFingerProgress
        }
    }

    /**
     * 使用双指手势
     */
    fun rememberUseMultiFinger(isHandle: Boolean) {
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
    fun setupAccumulateMoveX(value: Float) {
        require(value >= 0) { "设置BestGestureDetector.accumulateMoveX()的值必须>=0, value:${value}" }
        accumulateMoveX = value
        rememberAccumulateMoveX = 0f
    }

    /**
     * 设置累积移动值
     */
    fun setupAccumulateMoveY(value: Float) {
        require(value >= 0) { "设置BestGestureDetector.accumulateMoveY()的值必须>=0, value:${value}" }
        accumulateMoveY = value
        rememberAccumulateMoveY = 0f
    }

    /**
     * 设置累积旋转值
     */
    fun setupAccumulateRotation(value: Float) {
        require(value >= 0) { "设置BestGestureDetector.accumulateRotation()的值必须>=0, value:${value}" }
        accumulateRotation = value
        rememberAccumulateRotation = 0f
    }

    /**
     * 记录累积缩放值
     */
    fun setupAccumulateScale(value: Float) {
        require(value >= 0) { "设置BestGestureDetector.accumulateScale()的值必须>=0, value:${value}" }
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
     * 设置滑动阈值，大于这个阈值则取消点击事件，
     * @param threshold 设置的值大于[ViewConfiguration.getScaledTouchSlop] * 2无效
     */
    fun setupCancelClickScrollThreshold(threshold: Float) {
        cancelClickScrollThreshold = max(min(threshold, defaultCancelClickScrollThreshold), 0f)
    }

    /**
     * false 关闭双击，关闭双击后单击的响应会快一点，true 开启双击，开启双击后需要等待双击响应时间超时，单击响应就会慢一点
     */
    fun setupEnableDoubleClick(isEnable: Boolean) {
        isEnableDoubleClick = isEnable
    }

    /**
     * 是否处于点击后滑动，就放弃点击事件
     */
    fun setupEnableScrollCancelClick(isEnable: Boolean) {
        isInSingleTapScrollingGiveUpClick = isEnable
    }

    /**
     * 是否启用两次按压（双击）后滑动，就放弃点击事件
     */
    fun setupEnableScrollCancelDoubleClick(isEnable: Boolean) {
        isInDoubleTapScrollingGiveUpClick = isEnable
    }

    /**
     * 给开始事件设置偏移量
     */
    fun setupStartEventOffsetLocation(x: Float, y: Float) {
        startEvent?.offsetLocation(x, y)
    }

    /**
     * 设置开始事件的绝对位置
     */
    fun setupStartEventLocation(x: Float, y: Float) {
        startEvent?.setLocation(x, y)
    }

    /**
     * 设置当前事件的绝对位置
     */
    fun setupCurrentEventLocation(x: Float, y: Float) {
        currentEvent?.setLocation(x, y)
    }

    /**
     * 设置上一个事件的绝对位置
     */
    fun setupPreviousEventLocation(x: Float, y: Float) {
        previousEvent?.setLocation(x, y)
    }

    /**
     * 设置所有事件的绝对位置
     */
    fun setupAllEventLocation(x: Float, y: Float) {
        setupStartEventLocation(x, y)
        setupCurrentEventLocation(x, y)
        setupPreviousEventLocation(x, y)
    }

    /**
     * 给当前事件设置偏移量
     */
    fun setupCurrentEventOffsetLocation(x: Float, y: Float) {
        currentEvent?.offsetLocation(x, y)
    }

    /**
     * 给上一个事件设置偏移量
     */
    fun setupPreviousEventOffsetLocation(x: Float, y: Float) {
        previousEvent?.offsetLocation(x, y)
    }

    /**
     * 给所有事件设置偏移量
     */
    fun setupAllEventOffsetLocation(x: Float, y: Float) {
        setupStartEventOffsetLocation(x, y)
        setupCurrentEventOffsetLocation(x, y)
        setupPreviousEventOffsetLocation(x, y)
    }

    /**
     * 给开始事件设置变换
     */
    fun transformStartEvent(matrix: Matrix) {
        startEvent?.transform(matrix)
    }

    /**
     * 给当前事件设置变换
     */
    fun transformCurrentEvent(matrix: Matrix) {
        currentEvent?.transform(matrix)
    }

    /**
     * 给上一个事件设置变换
     */
    fun transformPreviousEvent(matrix: Matrix) {
        previousEvent?.transform(matrix)
    }

    /**
     * 给所有事件设置变换
     */
    fun transformAllEvent(matrix: Matrix) {
        transformStartEvent(matrix)
        transformCurrentEvent(matrix)
        transformPreviousEvent(matrix)
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