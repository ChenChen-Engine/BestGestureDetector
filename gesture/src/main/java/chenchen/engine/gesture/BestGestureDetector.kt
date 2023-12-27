package chenchen.engine.gesture

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import chenchen.engine.gesture.compat.MotionEventCompat
import chenchen.engine.gesture.compat.MotionEventCompat.Companion.obtain
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt


/**
 * List:
 * ```
 * [-] 双指缩放
 * [-] 双指旋转
 * [-] 双指平移
 * [-] 单指缩放
 * [-] 单指旋转
 * [-] 单指平移
 * [-] 单双指平移可以互相切换
 * [ ] 单次点击，不根据时间计算，只要Up时在Down附近就算单次点击
 * [-] 点击，根据时间算，和正常的Click事件一样，超时则取消
 * [-]. 长按，根据时间算，和正常的LongClick事件一样
 * [-] 双击
 * [ ] 双击提供多段放大处理
 * [ ] 手势触摸放大或缩小到一定程度松开时提供还原
 * [-] 放大吸附
 * [-] 旋转吸附
 * [-] 移动吸附
 * [ ] 松手吸附
 * [ ] 提供投掷处理
 * [ ] 提供Matrix
 * ```
 * Bug:
 * ```
 * [ ] 如果取moveX、moveY再转成Int，滑动越慢，越脱手
 * [ ] 双指距离小于等于10f的时候，会触发[MotionEvent.ACTION_POINTER_UP]接着又会重新触发[MotionEvent.ACTION_POINTER_DOWN]
 * [-] 双指手势需要使用到getRawXX才不会出现抖动问题，但是getRawXX需要api 29才能使用。可以通过修正坐标来解决
 * ```
 *
 * 使用方式：
 * ```kotlin
 * class MyView : View{
 *
 *  constructor(context: Context?) : super(context, null)
 *  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
 *
 *  val bestGesture by lazy { BestGestureDetector(context) }
 *
 *  override fun onTouchEvent(event: MotionEvent): Boolean {
 *      return bestGesture.onTouchEvent(this, event)
 *  }
 * }
 * ```
 * @author: chenchen
 * @since: 2023/3/17 15:16
 */
class BestGestureDetector private constructor(
    private val context: Context,
    private val state: BestGestureState
) {
    private val TAG = "BestGestureDetector"
    private var scaleListener: OnScaleGestureListener? = null
    private var rotateListener: OnRotateGestureListener? = null
    private var moveListener: OnMoveGestureListener? = null
    private var touchListener: OnTouchGestureListener? = null

    constructor(context: Context) : this(context, BestGestureState())

    fun onTouchEvent(view: View, event: MotionEvent): Boolean {
        state.resetConsumeValue()
        state.rememberCurrentEvent(view, event)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                state.rememberStartEvent()
                state.rememberPreviousEvent()
                state.rememberPivot(calculateCenterX(view), calculateCenterY(view))
                touchListener?.provideRawPivot(state.pivot)
                onDown(state.currentEvent!!).apply {
                    state.rememberPointerId()
                    state.rememberPreviousEvent()
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                onPointerDown(state.currentEvent!!).apply {
                    state.rememberPointerId()
                    state.rememberPreviousEvent()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                onMove(state.currentEvent!!).apply {
                    state.rememberPreviousEvent()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onPointerUp(state.currentEvent!!).apply {
                    state.rememberPointerId()
                    state.rememberPreviousEvent()
                }
            }
            MotionEvent.ACTION_UP -> {
                onUp(state.currentEvent!!)
            }
            MotionEvent.ACTION_CANCEL -> {
                onCancel(state.currentEvent!!)
            }
            else -> false
        }
    }

    /**
     * Down事件
     */
    private fun onDown(event: MotionEventCompat): Boolean {
        var isHandle = false
        if (event.pointerCount == 1) {
            isHandle = if (state.isCompletedGesture) {
                //完成了一次手势才开始新的一次手势
                isHandle or (this.touchListener?.onBeginTouch(
                    this, event.x.toInt(), event.y.toInt()) ?: false)
            } else {
                //如果没有完成一次单指手势，继续完成上次的手势，这种情况是双击第二下触发Down事件
                true
            }
            if (isHandle) {
                state.isCompletedGesture = false
                onAndroidGesture.onTouchEvent(event.event)
            } else {
                state.recycleState()
            }
        }
        state.useSingleFinger(isHandle)
        return isHandle
    }

    /**
     * 多指Down事件
     */
    private fun onPointerDown(event: MotionEventCompat): Boolean {
        var isHandle = false
        if (state.isCompletedGesture) {
            //手势已经结束，不再处理任何手势
            return isHandle
        }
        if (event.pointerCount > 1) {
            //多指手势
            if (scaleListener != null) {
                //todo开始之前是否增加一个阈值？
                state.isInScaleProgress = scaleListener!!.onBeginScale(this)
                isHandle = isHandle or state.isInScaleProgress
            }
            if (rotateListener != null) {
                state.isInRotateProgress = rotateListener!!.onBeginRotate(this)
                isHandle = isHandle or state.isInRotateProgress
            }
            if (moveListener != null) {
                state.isInMoveProgress = moveListener!!.onBeginMove(this)
                isHandle = isHandle or state.isInMoveProgress
            }
        }
        state.useMultiFinger(isHandle)
        state.isTriggerDoubleClick = false
        state.isUsedMultiFinger = true
        onAndroidGesture.onTouchEvent(event.event)
        return isHandle
    }

    /**
     * Move事件
     */
    private fun onMove(event: MotionEventCompat): Boolean {
        var isHandle = false
        if (state.isCompletedGesture) {
            //手势已经结束，不再处理任何手势
            return isHandle
        }
        if (state.isInMultiFingerProgress && event.pointerCount > 1) {
            //多指手势
            if (state.isInMoveProgress && moveListener != null) {
                isHandle = isHandle or onHandleMultiPointerMove()
            }
            if (state.isInRotateProgress && rotateListener != null) {
                isHandle = isHandle or onHandleMultiPointerRotation()
            }
            if (state.isInScaleProgress && scaleListener != null) {
                isHandle = isHandle or onHandleMultiPointerScale()
            }
        } else if (state.isInSingleFingerProgress) {
            //单指手势
            if (touchListener != null) {
                state.isInSingleTapScrollProgress = true
                isHandle = isHandle or onHandleSinglePointerMove()
                if (isHandle) {
                    onAndroidGesture.onTouchEvent(event.event)
                }
            }
        }
        return isHandle
    }

    /**
     * 多指Up事件
     */
    private fun onPointerUp(event: MotionEventCompat): Boolean {
        var isHandle = false
        if (state.isCompletedGesture) {
            //手势已经结束，不再处理任何手势
            return isHandle
        }
        if (state.isInScaleProgress && scaleListener != null) {
            scaleListener!!.onScaleEnd(this)
            isHandle = true
        }
        if (state.isInRotateProgress && rotateListener != null) {
            rotateListener!!.onRotateEnd(this)
            isHandle = true
        }
        if (state.isInMoveProgress && moveListener != null) {
            moveListener!!.onMoveEnd(this)
            isHandle = true
        }
        if (event.pointerCount <= 2) {
            state.useMultiFinger(false)
        }
        return isHandle
    }

    /**
     * Up事件
     */
    private fun onUp(event: MotionEventCompat): Boolean {
        if (state.isCompletedGesture) {
            //手势已经结束，不再处理任何手势
            return true
        }
        if (state.isInSingleFingerProgress && touchListener != null) {
            when {
                //按压后有滑动过，结束事件
                state.isInSingleTapScrollProgress ||
                        //长按过，结束事件
                        state.isInLongPressProgress ||
                        //使用过双指，但现在已经变成单指，又松手了，结束事件
                        state.isUsedMultiFinger -> {
                    touchListener?.onTouchEnd(this)
                    state.recycleState()
                }
                else -> {
                    //单指手势未完成，应该是打算双击，继续分发手势
                    onAndroidGesture.onTouchEvent(event.event)
                }
            }
        }
        return true
    }

    /**
     * Cancel事件，异常情况：
     * 1. 三指或以上手指在不超1秒内同时触摸情况下，小米手机会发送一个CANCEL事件，因为系统需要三指截屏
     */
    private fun onCancel(event: MotionEventCompat): Boolean {
        if (state.isCompletedGesture) {
            //手势已经结束，不再处理任何手势
            return true
        }
        touchListener?.onTouchCancel(this)
        state.recycleState()
        return true
    }

    /**
     * 处理多指移动
     */
    private fun onHandleMultiPointerMove(): Boolean {
        state.rememberAccumulateMove(calculateMoveX(), calculateMoveY())
        if (state.canConsumeAccumulateMoveX() || state.canConsumeAccumulateMoveY()) {
            while (state.canConsumeAccumulateMoveX() || state.canConsumeAccumulateMoveY()) {
                state.isInMoveProgress = moveListener!!.onMove(this)
                state.consumeAccumulateMoveX()
                state.consumeAccumulateMoveY()
            }
        } else {
            //没有累积值的情况下调用，如果有累积值但是不足，获取到的值为0f
            state.isInMoveProgress = moveListener!!.onMove(this)
        }
        if (!state.isInMoveProgress) {
            moveListener!!.onMoveEnd(this)
        }
        return state.isInMoveProgress
    }

    /**
     * 处理多指旋转
     */
    private fun onHandleMultiPointerRotation(): Boolean {
        state.rememberAccumulateRotation(calculateRotation())
        if (state.canConsumeAccumulateRotation()) {
            while (state.canConsumeAccumulateRotation()) {
                state.isInRotateProgress = rotateListener!!.onRotate(this)
                state.consumeAccumulateRotation()
            }
        } else {
            //没有累积值的情况下调用，如果有累积值但是不足，获取到的值为0f
            state.isInRotateProgress = rotateListener!!.onRotate(this)
        }
        if (!state.isInRotateProgress) {
            rotateListener!!.onRotateEnd(this)
        }
        return state.isInRotateProgress
    }

    /**
     * 处理多指缩放
     */
    private fun onHandleMultiPointerScale(): Boolean {
        state.rememberAccumulateScale(calculateScaleFactor())
        if (state.canConsumeAccumulateScale()) {
            while (state.canConsumeAccumulateScale()) {
                state.isInScaleProgress = scaleListener!!.onScale(this)
                state.consumeAccumulateScale()
            }
        } else {
            //没有累积值的情况下调用，如果有累积值但是不足，获取到的值为1f
            state.isInScaleProgress = scaleListener!!.onScale(this)
        }
        if (!state.isInScaleProgress) {
            scaleListener!!.onScaleEnd(this)
        }
        return state.isInScaleProgress
    }

    /**
     * 处理单指移动
     */
    private fun onHandleSinglePointerMove(): Boolean {
        var isHandle = false
        //先记录累积值
        state.rememberAccumulateMove(calculateMoveX(), calculateMoveY())
        state.rememberAccumulateRotation(calculateRotation())
        state.rememberAccumulateScale(calculateScaleFactor())
        //判断能否消费累积值
        if (state.canConsumeAccumulateMoveX() || state.canConsumeAccumulateMoveY()
            || state.canConsumeAccumulateRotation() || state.canConsumeAccumulateScale()) {
            //循环消费累积值
            while (state.canConsumeAccumulateMoveX() || state.canConsumeAccumulateMoveY()
                || state.canConsumeAccumulateRotation() || state.canConsumeAccumulateScale()) {
                isHandle = touchListener!!.onTouchMove(this)
                state.consumeAccumulateMoveX()
                state.consumeAccumulateMoveY()
                state.consumeAccumulateRotation()
                state.consumeAccumulateScale()
            }
        } else {
            //没有累积值的情况下调用，如果有累积值但是不足，获取到的值为0f或为1f
            isHandle = touchListener!!.onTouchMove(this)
        }
        return isHandle
    }

    private val onAndroidGesture = GestureDetectorCompat(context,
        object : GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

            override fun onDown(e: MotionEvent) = true

            override fun onShowPress(e: MotionEvent) {
                if (!state.isInSingleFingerProgress || state.isUsedMultiFinger) {
                    //不是单指，或者使用过双指，不能触发按压
                    return
                }
                if (state.isTriggerDoubleClick) {
                    //现在处于双击第二下，不能触发按压
                    return
                }
                touchListener?.onPress(this@BestGestureDetector)
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!state.isInSingleFingerProgress || state.isUsedMultiFinger) {
                    //不是单指，或者使用过双指，不能触发点击
                    return true
                }
                if (state.isEnableDoubleClick) {
                    //开启了双击，就不在这里处理单击，在onSingleTapConfirmed中处理
                    return true
                }
                if (state.isInSingleTapScrollingGiveUpClick && state.isInSingleTapScrollProgress) {
                    //开启了滑动过就丢弃点击事件
                    return true
                }
                touchListener?.onClick(this@BestGestureDetector)
                touchListener?.onTouchEnd(this@BestGestureDetector)
                state.recycleState()
                return true
            }

            /**
             * warning 长按后不会触发onScroll，不能在这里处理移动事件
             */
            override fun onScroll(
                e1: MotionEvent, e2: MotionEvent,
                distanceX: Float, distanceY: Float
            ) = true

            override fun onLongPress(e: MotionEvent) {
                if (!state.isInSingleFingerProgress || state.isUsedMultiFinger) {
                    //不是单指，或者使用过双指，不能触发长按
                    return
                }
                if (state.isTriggerDoubleClick) {
                    //现在处于双击第二下长按中，不能触发长按
                    return
                }
                val result = touchListener?.onLongPress(this@BestGestureDetector) ?: false
                if (result) {
                    touchListener?.onLongClick(this@BestGestureDetector)
                    touchListener?.onTouchEnd(this@BestGestureDetector)
                    state.recycleState()
                } else {
                    state.isInLongPressProgress = true
                }
            }

            override fun onFling(
                e1: MotionEvent, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ) = true

            /**
             * 不是双击的时候，永远确认是单击
             */
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!state.isInSingleFingerProgress || state.isUsedMultiFinger) {
                    //不是单指，或者使用过双指，不能触发点击
                    return true
                }
                if (!state.isEnableDoubleClick) {
                    //没有开启双击，就不在这里处理单击，在onSingleTapUp中处理
                    return true
                }
                if (state.isInSingleTapScrollingGiveUpClick && state.isInSingleTapScrollProgress) {
                    //开启了滑动过就丢弃点击事件
                    return true
                }
                touchListener?.onClick(this@BestGestureDetector)
                touchListener?.onTouchEnd(this@BestGestureDetector)
                state.recycleState()
                return true
            }

            /**
             * 双击
             */
            override fun onDoubleTap(event: MotionEvent) = true.apply {
                //记录触发第二击
                state.isTriggerDoubleClick = true
            }

            /**
             * 任何时候都要处理事件
             */
            override fun onDoubleTapEvent(event: MotionEvent): Boolean {
                if (!state.isTriggerDoubleClick) {
                    //不是第二击，可能哪里状态混乱了，不处理
                    return true
                }
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        //记录第二击是否滑动过
                        state.isInDoubleTapScrollingProgress = true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (!(state.isInDoubleTapScrollingGiveUpClick && state.isInDoubleTapScrollingProgress)) {
                            //!(开启了滑动过就丢弃点击事件)
                            touchListener?.onDoubleClick(this@BestGestureDetector)
                        }
                        touchListener?.onTouchEnd(this@BestGestureDetector)
                        state.recycleState()
                    }
                }
                return true
            }
        })

    /**
     * 起始事件
     */
    val startEvent: MotionEventCompat?
        get() = state.startEvent?.obtain()

    /**
     * 当前事件
     */
    val currentEvent: MotionEventCompat?
        get() = state.currentEvent?.obtain()

    /**
     * 上一次的事件
     */
    val previousEvent: MotionEventCompat?
        get() = state.previousEvent?.obtain()

    /**
     * 获取缩放因子，使用方式view.scaleX *= scaleFactor
     */
    val scaleFactor: Float
        get() {
            val value = calculateScaleFactor()
            return state.getScaleFactor(value)
        }

    /**
     * 获取旋转角度，使用方式view.rotation += rotation
     */
    val rotation: Float
        get() {
            val value = calculateRotation()
            return state.getRotation(value)
        }

    /**
     * 获取移动偏移量，使用方式view.x += moveX
     * PS：没有变换的情况下使用[View.offsetLeftAndRight]是代替[View.X]是没问题的
     * 但如果经过变换再使用[View.offsetLeftAndRight]的方式更新，手势会变得很奇怪，
     * 原因是left、right是不会根据变换而改变
     */
    val moveX: Float
        get() {
            val value = calculateMoveX()
            return state.getMoveX(value)
        }


    /**
     * 获取移动偏移量，使用方式view.y += moveY
     * PS：没有变换的情况下使用[View.offsetTopAndBottom]是代替[View.Y]是没问题的
     * 但如果经过变换再使用[View.offsetTopAndBottom]的方式更新，手势会变得很奇怪，
     * 原因是top、bottom是不会根据变换而改变
     */
    val moveY: Float
        get() {
            val value = calculateMoveY()
            return state.getMoveY(value)
        }

    /**
     * 计算缩放比
     */
    private fun calculateScaleFactor(): Float {
        return (if (state.isInMultiFingerProgress) {
            if (state.isInScaleProgress) {
                getMultiFingerDistance(state.currentEvent!!) /
                        getMultiFingerDistance(state.previousEvent!!)
            } else {
                1f
            }
        } else {
            getSingleFingerDistance(state.currentEvent!!) /
                    getSingleFingerDistance(state.previousEvent!!)
        }).scaleSafeValue()
    }

    /**
     * 计算旋转偏移量
     */
    private fun calculateRotation(): Float {
        return if (state.isInMultiFingerProgress) {
            if (state.isInRotateProgress) {
                getMultiFingerRotation(state.currentEvent!!) -
                        getMultiFingerRotation(state.previousEvent!!)
            } else {
                0f
            }
        } else {
            getSingleFingerRotation(state.currentEvent!!) -
                    getSingleFingerRotation(state.previousEvent!!)
        }
    }

    /**
     * 计算x轴移动偏移量
     */
    private fun calculateMoveX(): Float {
        val value = if (state.isInMultiFingerProgress) {
            if (state.isInMoveProgress) {
                getMultiFingerMidPoint(state.currentEvent!!).x -
                        getMultiFingerMidPoint(state.previousEvent!!).x
            } else {
                0f
            }
        } else {
            val currentMajorId = state.getTrackPointerIds(state.currentEvent)
                .firstOrNull() ?: return 0f
            val previousMajorId = state.getTrackPointerIds(state.previousEvent)
                .firstOrNull() ?: return 0f
            val currentMajorIndex = state.currentEvent!!.findPointerIndex(currentMajorId)
            val previousMajorIndex = state.previousEvent!!.findPointerIndex(previousMajorId)
            if (currentMajorIndex != MotionEvent.INVALID_POINTER_ID
                && previousMajorIndex != MotionEvent.INVALID_POINTER_ID) {
                state.currentEvent!!.getRawX(currentMajorIndex) - state.previousEvent!!.getRawX(previousMajorIndex)
            } else {
                0f
            }
        }
        return value
    }

    /**
     * 计算y轴移动偏移量
     */
    private fun calculateMoveY(): Float {
        val value = if (state.isInMultiFingerProgress) {
            if (state.isInMoveProgress) {
                getMultiFingerMidPoint(state.currentEvent!!).y -
                        getMultiFingerMidPoint(state.previousEvent!!).y
            } else {
                0f
            }
        } else {
            val currentMajorId = state.getTrackPointerIds(state.currentEvent)
                .firstOrNull() ?: return 0f
            val previousMajorId = state.getTrackPointerIds(state.previousEvent)
                .firstOrNull() ?: return 0f
            val currentMajorIndex = state.currentEvent!!.findPointerIndex(currentMajorId)
            val previousMajorIndex = state.previousEvent!!.findPointerIndex(previousMajorId)
            if (currentMajorIndex != MotionEvent.INVALID_POINTER_ID
                && previousMajorIndex != MotionEvent.INVALID_POINTER_ID) {
                state.currentEvent!!.getRawY(currentMajorIndex) - state.previousEvent!!.getRawY(previousMajorIndex)
            } else {
                0f
            }
        }
        return value
    }

    /**
     * 获取单指之间的距离
     */
    private fun getSingleFingerDistance(event: MotionEventCompat): Float {
        val majorId = state.getTrackPointerIds(event).firstOrNull() ?: return 0f
        val majorIndex = event.findPointerIndex(majorId)
        return getDistance(event.getRawX(majorIndex), event.getRawY(majorIndex), state.pivot.x, state.pivot.y)
    }

    /**
     * 获取多指之间的距离
     */
    private fun getMultiFingerDistance(event: MotionEventCompat): Float {
        var distance = 0f
        var majorIndex: Int
        var minorIndex: Int
        val pointerIds = state.getTrackPointerIds(event)
        val pointerSize = pointerIds.size
        for (i in 0 until pointerSize) {
            val nextIndex = (i + 1) % pointerSize
            majorIndex = event.findPointerIndex(pointerIds[i])
            minorIndex = event.findPointerIndex(pointerIds[nextIndex])
            if (majorIndex != MotionEvent.INVALID_POINTER_ID &&
                minorIndex != MotionEvent.INVALID_POINTER_ID) {
                distance += getDistance(event.getRawX(majorIndex), event.getRawY(majorIndex),
                    event.getRawX(minorIndex), event.getRawY(minorIndex))
            }
        }
        return distance
    }

    /**
     * 获取两指之间的距离
     */
    private fun getDistance(startX: Float, startY: Float, endX: Float, endY: Float): Float {
        val dx = startX - endX
        val dy = startY - endY
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * 获取单指旋转角度
     */
    private fun getSingleFingerRotation(event: MotionEventCompat): Float {
        val majorId = state.getTrackPointerIds(event).firstOrNull() ?: return 0f
        val majorIndex = event.findPointerIndex(majorId)
        return getRotation(event.getRawX(majorIndex), event.getRawY(majorIndex), state.pivot.x, state.pivot.y)
    }

    /**
     * 获取多指旋转角度
     * GPT提供的计算公式，对比1.0.0的公式，小数点第4位开始会有误差
     */
    private fun getMultiFingerRotation(event: MotionEventCompat): Float {
        var startX = Float.MIN_VALUE
        var startY = Float.MIN_VALUE
        var centerX = Float.MIN_VALUE
        var centerY = Float.MIN_VALUE
        val pointerIds = state.getTrackPointerIds(event)
        val pointerSize = pointerIds.size
        for (i in 0 until pointerSize) {
            val pointerIndex = event.findPointerIndex(pointerIds[i])
            if (pointerIndex != MotionEvent.INVALID_POINTER_ID) {
                centerX += event.getRawX(pointerIndex)
                centerY += event.getRawY(pointerIndex)
                if (startX == Float.MIN_VALUE || startY == Float.MIN_VALUE) {
                    startX = centerX
                    startY = centerY
                }
            }
        }
        return getRotation(startX, startY, centerX / pointerSize, centerY / pointerSize)
    }

    /**
     * 获取旋转角度
     */
    private fun getRotation(startX: Float, startY: Float, endX: Float, endY: Float): Float {
        val dx = startX - endX
        val dy = startY - endY
        var degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (degrees < 0) {
            degrees += 360f
        }
        return degrees
    }

    /**
     * 获取多指中心点
     */
    private fun getMultiFingerMidPoint(event: MotionEventCompat): PointF {
        val point = PointF()
        val pointerIds = state.getTrackPointerIds(event)
        val pointerSize = pointerIds.size
        for (i in 0 until pointerSize) {
            val pointerIndex = event.findPointerIndex(pointerIds[i])
            if (pointerIndex != MotionEvent.INVALID_POINTER_ID) {
                point.x += event.getRawX(pointerIndex)
                point.y += event.getRawY(pointerIndex)
            }
        }
        point.x /= pointerSize
        point.y /= pointerSize
        return point
    }

    /**
     * 获取基于[View]内的多指中心点
     */
    private fun getFocusPoint(event: MotionEventCompat): PointF {
        val point = PointF()
        val pointerIds = state.getTrackPointerIds(event)
        val pointerSize = pointerIds.size
        for (i in 0 until pointerSize) {
            val pointerIndex = event.findPointerIndex(pointerIds[i])
            if (pointerIndex != MotionEvent.INVALID_POINTER_ID) {
                point.x += event.getX(pointerIndex)
                point.y += event.getY(pointerIndex)
            }
        }
        point.x /= pointerSize
        point.y /= pointerSize
        return point
    }

    /**
     * 获取两指中心点
     */
    private fun getMidPoint(startX: Float, startY: Float, endX: Float, endY: Float): PointF {
        val x = (startX + endX) / 2
        val y = (startY + endY) / 2
        return PointF(x, y)
    }

    /**
     * 安全的缩放值
     * 暂不清楚什么情况下`CurrentEvent`和`PreviousEvent`的值完全一致，导致计算出来为`Nan`或`Infinite`
     * 为了避免出现这样的情况，将这样的值替换为1
     */
    private fun Float.scaleSafeValue(): Float {
        return if (isFinite()) {
            this
        } else {
            1f
        }
    }

    /**
     * 获取基于某个View在屏幕中x轴的绝对中心点
     */
    fun calculateCenterX(view: View?): Float {
        var absX = 0f
        if (view == null) {
            return absX
        }
        var cView = view
        while (cView != null) {
            absX += cView.x
            cView = (cView.parent as? View)
        }
        return absX + view.width.toFloat() / 2f
    }

    /**
     * 获取基于某个View在屏幕中y轴的绝对中心点
     * [ ] 如果可以穿透状态栏该如何处理
     */
    fun calculateCenterY(view: View?): Float {
        var absY = 0f
        if (view == null) {
            return absY
        }
        var cView = view
        while (cView != null) {
            absY += cView.y
            cView = (cView.parent as? View)
        }
        return absY + view.statusBarHeight + view.actionBarHeight + view.height.toFloat() / 2f
    }

    /**
     * 获取当前事件的焦点，焦点指的是所有手指的中间
     * 常用于对imageMatrix的缩放焦点处理
     * 不可用于对[View]的缩放焦点处理
     */
    fun getFocus(event: MotionEventCompat): PointF {
        return getFocusPoint(event)
    }

    /**
     * 获取当前事件原始的焦点，焦点指的是所有手指的中间
     */
    fun getRawFocus(event: MotionEventCompat): PointF {
        val focus = getMultiFingerMidPoint(event)
        if (context is Activity) {
            focus.y -= context.statusBarHeight - context.actionBarHeight
        }
        return focus
    }

    /**
     * 设置追踪的手指数量，最低数量为2
     * 单指设置无效，单指只追踪一根手指，多指至少追踪两根手指
     * @param count 手指数量，必须>=2
     */
    fun setTrackPointerCount(count: Int) {
        state.rememberTrackPointerIdCount(count)
    }

    /**
     * 消费掉[BestGestureDetector.moveX]、[BestGestureDetector.moveY]的值，每次调用都会累加值，比如[moveX] = 50，调用了`consumeMove(25f,0f)`那[moveX]则为25，再调用一次就为0
     * 在本次事件中生效，新事件发生时会重置这个值
     * 注意，这个值消费到0就不能再消费了，并且调用的时候不考虑正负
     */
    fun consumeMove(consumeX: Float = 0f, consumeY: Float = 0f) {
        state.consumeMoveX += abs(consumeX)
        state.consumeMoveY += abs(consumeY)
    }

    /**
     * 消费掉[BestGestureDetector.rotation]的值，每次调用都会累加
     * 在本次事件中生效，新事件发生时会重置这个值
     * 注意，这个值消费到0就不能再消费了，并且调用的时候不考虑正负
     */
    fun consumeRotation(consumeRotation: Float) {
        state.consumeRotation += abs(consumeRotation)
    }

    /**
     * 消费掉[BestGestureDetector.scaleFactor]的值，每次调用都会累加
     * 在本次事件中生效，新事件发生时会重置这个值
     * 注意，这个值消费到0就不能再消费了，并且调用的时候不考虑正负
     */
    fun consumeScale(consumeScaleFactor: Float) {
        state.consumeScaleFactor += abs(consumeScaleFactor)
    }

    /**
     * 累积移动值，每当累积到[value]的值的时候会消费，如一次累积超过[value]的倍数，则回调多次
     * 如果累积值不足[value]的值，则[BestGestureDetector.moveX]为0，这时候可以不做任何处理
     * 每次重新开始手势的时候需要重新设置
     * @param value 累积移动x轴的值，必须是大于0的正值，无论是左滑还是右滑只要超过这个值才会回调
     */
    fun accumulateMoveX(value: Float) {
        state.accumulateMoveX(value)
    }

    /**
     * 累积移动值，每当累积到[value]的值的时候会消费，如一次累积超过[value]的倍数，则回调多次
     * 如果累积值不足[value]的值，则[BestGestureDetector.moveY]为0，这时候可以不做任何处理
     * 每次重新开始手势的时候需要重新设置
     * @param value 累积移动y轴的值，必须是大于0的正值，无论是上滑还是下滑，只要超过这个值才会回调
     */
    fun accumulateMoveY(value: Float) {
        state.accumulateMoveY(value)
    }

    /**
     * 累积旋转值，每当累积到[value]的值的时候会消费，如一次累积超过[value]的倍数，则回调多次
     * 如果累积值不足[value]的值，则[BestGestureDetector.rotation]为0，这时候可以不做任何处理
     * 每次重新开始手势的时候需要重新设置
     * @param value 累积旋转的值，必须是大于0的正值，无论是左旋转还是右旋转，只要超过这个值才会回调
     */
    fun accumulateRotation(value: Float) {
        state.accumulateRotation(value)
    }

    /**
     * 累积缩放值，每当累积到[value]的值的时候会消费，如一次累积超过[value]的倍数，则回调多次，
     * 如果累积值不足[value]的值，则[BestGestureDetector.scaleFactor]为1，这时候可以不做任何处理
     * 每次重新开始手势的时候需要重新设置
     * @param value 累积缩放的值，必须是大于0的正值，无论是放大还是缩小，只要超过这个值才会回调
     */
    fun accumulateScale(value: Float) {
        state.accumulateScale(value)
    }

    /**
     * 设置开始事件的绝对位置
     */
    fun setStartEventLocation(x: Float, y: Float) {
        state.setStartEventLocation(x, y)
    }

    /**
     * 设置当前事件的绝对位置
     */
    fun setCurrentEventLocation(x: Float, y: Float) {
        state.setCurrentEventLocation(x, y)
    }

    /**
     * 设置上一个事件的绝对位置
     */
    fun setPreviousEventLocation(x: Float, y: Float) {
        state.setPreviousEventLocation(x, y)
    }

    /**
     * 设置所有事件的绝对位置
     */
    fun setAllEventLocation(x: Float, y: Float) {
        state.setAllEventLocation(x, y)
    }

    /**
     * 给开始事件设置偏移量
     */
    fun setStartEventOffsetLocation(x: Float, y: Float) {
        state.setStartEventOffsetLocation(x, y)
    }

    /**
     * 给当前事件设置偏移量
     */
    fun setCurrentEventOffsetLocation(x: Float, y: Float) {
        state.setCurrentEventOffsetLocation(x, y)
    }

    /**
     * 给上一个事件设置偏移量
     */
    fun setPreviousEventOffsetLocation(x: Float, y: Float) {
        state.setPreviousEventOffsetLocation(x, y)
    }

    /**
     * 给所有事件设置偏移量
     */
    fun setAllEventOffsetLocation(x: Float, y: Float) {
        state.setAllEventOffsetLocation(x, y)
    }

    /**
     * 给开始事件设置变换
     */
    fun transformStartEvent(matrix: Matrix) {
        state.transformStartEvent(matrix)
    }

    /**
     * 给当前事件设置变换
     */
    fun transformCurrentEvent(matrix: Matrix) {
        state.transformCurrentEvent(matrix)
    }

    /**
     * 给上一个事件设置变换
     */
    fun transformPreviousEvent(matrix: Matrix) {
        state.transformPreviousEvent(matrix)
    }

    /**
     * 给所有事件设置变换
     */
    fun transformAllEvent(matrix: Matrix) {
        state.transformAllEvent(matrix)
    }

    /**
     * 设置触摸手势监听，这个必须设置，否则无法响应单指手势，单指手势都不响应双指也是无法响应的
     */
    fun setOnTouchListener(listener: OnTouchGestureListener) {
        this.touchListener = listener
    }

    /**
     * 设置双指缩放手势监听，双指手势必须设置一个监听才会生效
     */
    fun setScaleListener(listener: OnScaleGestureListener) {
        this.scaleListener = listener
    }

    /**
     * 设置双指旋转手势监听，双指手势必须设置一个监听才会生效
     */
    fun setRotationListener(listener: OnRotateGestureListener) {
        this.rotateListener = listener
    }

    /**
     * 设置双指移动手势监听，双指手势必须设置一个监听才会生效
     */
    fun setMoveListener(listener: OnMoveGestureListener) {
        this.moveListener = listener
    }

    /**
     * false 关闭双击，关闭双击后单击的响应会快一点，true 开启双击，开启双击后需要等待双击响应时间超时，单击响应就会慢一点
     */
    var isEnableDoubleClick: Boolean
        set(value) {
            state.isEnableDoubleClick = value
        }
        get() = state.isEnableDoubleClick

    /**
     * 是否处于点击后滑动，就放弃点击事件
     */
    var isInSingleTapScrollingGiveUpClick: Boolean
        set(value) {
            state.isInSingleTapScrollingGiveUpClick = value
        }
        get() = state.isInSingleTapScrollingGiveUpClick

    /**
     *  是否处于两次按压（双击）后滑动，就放弃点击事件
     */
    var isInDoubleTapScrollingGiveUpClick: Boolean
        set(value) {
            state.isInDoubleTapScrollingGiveUpClick = value
        }
        get() = state.isInDoubleTapScrollingGiveUpClick

    /**
     * 复制一份新的，手势监听此时已经不存在了
     */
    fun copy(): BestGestureDetector {
        return BestGestureDetector(
            context, state.copy(
                startEvent = startEvent,
                currentEvent = currentEvent,
                previousEvent = previousEvent,
                pivot = PointF(state.pivot.x, state.pivot.y),
                pointerIds = ArrayList(state.pointerIds),
                currentTrackPointerIds = ArrayList(state.currentTrackPointerIds),
                previousTrackPointerIds = ArrayList(state.previousTrackPointerIds),
            ))
    }
}