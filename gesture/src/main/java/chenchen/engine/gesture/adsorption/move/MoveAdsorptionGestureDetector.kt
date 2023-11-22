package chenchen.engine.gesture.adsorption.move

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Point
import android.graphics.Rect
import androidx.core.animation.doOnEnd
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.ConstrainedAlignment
import chenchen.engine.gesture.ConstraintAlignment.*
import chenchen.engine.gesture.ConstrainedAlignment.*
import chenchen.engine.gesture.MoveGestureDetector
import chenchen.engine.gesture.MoveMovementTrack
import chenchen.engine.gesture.getGlobalRect
import chenchen.engine.gesture.nullIf
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @author: chenchen
 * @since: 2023/4/25 17:38
 * 移动吸附手势
 */
class MoveAdsorptionGestureDetector(
    private val adsorption: MoveAdsorption,
    private val adsorptionListener: OnMoveAdsorptionListener
) : MoveGestureDetector() {

    private val TAG = "MoveAdsorption"

    private val state = MoveAdsorptionState()

    /**
     * 是否在吸附动画中
     */
    private var isInAdsorptionProgress = false

    /**
     * 吸附x值
     */
    var adsorptionX = 0
        private set

    /**
     * 吸附y值
     */
    var adsorptionY = 0
        private set

    /**
     * 处理移动手势
     * @return
     * true 消费手势，这时候调出处最好将后续流程return
     * false 不消费手势，调用处可以继续执行流程
     */
    override fun onMove(detector: BestGestureDetector): Boolean {
        state.rememberMovementTrack(detector)
        //如果动画在进行中则不重新测量
        if (!isInAdsorptionProgress) {
            //步骤1 测量吸附位置
            val point = adsorption.analyze() //测量是否达到吸附条件
            //如果测量结果为0，表示不需要吸附
            if (point.x != 0 || point.y != 0) {
                //步骤2 通知开始吸附
                isInAdsorptionProgress = adsorptionListener.onBeginAdsorption(this)
                if (!isInAdsorptionProgress) {
                    return false
                }
                //步骤3 开始吸附动画
                adsorption.adsorptionValueAnim = ValueAnimator().apply {
                    var lastXValue = 0
                    var lastYValue = 0
                    val holders = arrayListOf<PropertyValuesHolder>()
                    //X轴没有吸附，可以执行吸附动画
                    if (!adsorption.isAdsorptionH && !adsorption.isHAdsorptionImmunityRegion) {
                        holders.add(PropertyValuesHolder.ofInt("x", 0, point.x))
                    }
                    //Y轴没有吸附，可以执行吸附动画
                    if (!adsorption.isAdsorptionV && !adsorption.isVAdsorptionImmunityRegion) {
                        holders.add(PropertyValuesHolder.ofInt("y", 0, point.y))
                    }
                    setValues(*holders.toTypedArray())
                    addUpdateListener {
                        //步骤3.1 更新吸附动画进度
                        val xValue = (it.getAnimatedValue("x") as? Int) ?: 0
                        val yValue = (it.getAnimatedValue("y") as? Int) ?: 0
                        adsorptionX = xValue - lastXValue
                        adsorptionY = yValue - lastYValue
                        adsorptionListener.onAdsorption(this@MoveAdsorptionGestureDetector)
                        lastXValue = xValue
                        lastYValue = yValue
                    }
                    doOnEnd {
                        //步骤3.2 通知吸附动画结束
                        adsorptionX = 0
                        adsorptionY = 0
                        adsorptionListener.onAdsorptionEnd(this@MoveAdsorptionGestureDetector)
                        //步骤3.3 标记当前已经处于吸附状态
                        //x轴已经在吸附状态不重复记录状态，并且记录的状态永远为true，如果出现false就是错误
                        if (!adsorption.isAdsorptionH && !adsorption.isHAdsorptionImmunityRegion) {
                            adsorption.isAdsorptionH = point.x != 0
                        }
                        //y轴已经在吸附状态不重复记录状态，并且记录的状态永远为true，如果出现false就是错误
                        if (!adsorption.isAdsorptionV && !adsorption.isVAdsorptionImmunityRegion) {
                            adsorption.isAdsorptionV = point.y != 0
                        }
                        //步骤3.4 重置吸附动画进行中的状态
                        isInAdsorptionProgress = false
                        adsorption.adsorptionValueAnim = null
                    }
                    duration = 80
                }
                adsorption.adsorptionValueAnim?.start()
            }
        }
        val moveX = detector.moveX
        val moveY = detector.moveY
        //状态流转是4->5->6
        //消费流程是6->5->4，先判断最高的状态
        //步骤6 消费免疫区，先消费免疫区，因为先消费挣脱值，状态就变为免疫区，这时候才消费免疫区就有问题
        adsorption.consumeHImmunityThreshold(moveX)
        adsorption.consumeVImmunityThreshold(moveY)
        //步骤5 消费挣脱值，吸附之后会粘在吸附点上，需要滑动挣脱
        adsorption.consumeHRidThreshold(moveX)
        adsorption.consumeVRidThreshold(moveY)
        //步骤4 如果处于吸附中先要把挣脱值消费掉才能移动View，所以这里把移动事件消费掉，不让View移动
        detector.consumeMove(
            consumeX = moveX - consumeMoveX(moveX),
            consumeY = moveY - consumeMoveY(moveY)
        )
        return isInAdsorptionProgress
    }

    /**
     * 步骤1，测量当前吸附锚点的距离是否触发吸附
     *
     * 思考点：
     *
     * 1. 如果处于免疫区，比如免疫区30，而另一个磁铁在免疫区内，并且吸附区域为10，忽略吸附区，
     * 在免疫区免疫一切吸附，这取决于磁铁自己设置的免疫区，免疫区值越小越容易在挣脱后被其他磁铁吸附
     *
     * 2. 如果多个磁铁在同一个点，以吸附区域最大的磁铁为准，挣脱和免疫也是以最大的为准。
     *
     */
    private fun MoveAdsorption.analyze(): Point {
        //步骤1.1，如果磁性物体已经被移除，则释放所有记录的内容
        if (!magnetic.target.isAttachedToWindow) {
            release()
            return Point(0, 0)
        }
        var minXOffset = 0
        var minYOffset = 0
        var leftAnalyze: MoveAnalyzeResult?
        var horizontalAnalyze: MoveAnalyzeResult?
        var rightAnalyze: MoveAnalyzeResult?
        var topAnalyze: MoveAnalyzeResult?
        var verticalAnalyze: MoveAnalyzeResult?
        var bottomAnalyze: MoveAnalyzeResult?
        val leftAnalyzes = arrayListOf<MoveAnalyzeResult>()
        val horizontalCenterAnalyzes = arrayListOf<MoveAnalyzeResult>()
        val rightAnalyzes = arrayListOf<MoveAnalyzeResult>()
        val topAnalyzes = arrayListOf<MoveAnalyzeResult>()
        val verticalCenterAnalyzes = arrayListOf<MoveAnalyzeResult>()
        val bottomAnalyzes = arrayListOf<MoveAnalyzeResult>()
        //1.2 获取磁性物体的绝对坐标
        magnetic.target.getGlobalRect(magneticRect)
        for (magnet in magnets) {
            this.magnetRect.setEmpty()
            if (!magnet.target.isAttachedToWindow) {
                //如果磁铁已经被移除则跳过
                continue
            }
            leftAnalyze = null
            horizontalAnalyze = null
            rightAnalyze = null
            topAnalyze = null
            verticalAnalyze = null
            bottomAnalyze = null
            //步骤1.3 获取每个磁铁的绝对坐标
            magnet.target.getGlobalRect(magnetRect)
            for (alignment in magnetic.alignments) {
                //步骤1.4.1 测量水平方向Left、HorizontalCenter、Right三个点的吸附距离，三个点都要记录，最终取最三个点最接近的一个点作为吸附动画的x轴值
                val horizontalAnalyzeFunc = horizontalAnalyze@{
                    //x轴吸附中，不继续测量
                    if (isAdsorptionH || isHAdsorptionImmunityRegion) {
                        return@horizontalAnalyze
                    }
                    //不是x轴类型的return，避免后续多余的判断，也方便调试正确的判断
                    when (alignment) {
                        TopToTop, TopToVerticalCenter, TopToBottom,
                        VerticalCenterToTop, VerticalCenterToVerticalCenter, VerticalCenterToBottom,
                        BottomToTop, BottomToVerticalCenter, BottomToBottom -> return@horizontalAnalyze
                        else -> Unit
                    }
                    leftAnalyze = analyzeLeft(magneticRect, magnetRect,
                        magnet, alignment, leftAnalyze)
                    horizontalAnalyze = analyzeHorizontalCenter(magneticRect, magnetRect,
                        magnet, alignment, horizontalAnalyze)
                    rightAnalyze = analyzeRight(magneticRect, magnetRect,
                        magnet, alignment, rightAnalyze)
                }
                //步骤1.4.2 测量水平方向Top、VerticalCenter、Bottom三个点的吸附距离，三个点都要记录，最终取最三个点最接近的一个点作为吸附动画的y轴值
                val verticalAnalyzeFunc = verticalAnalyzeFunc@{
                    //y轴吸附中，不继续测量
                    if (isAdsorptionV || isVAdsorptionImmunityRegion) {
                        return@verticalAnalyzeFunc
                    }
                    //不是y轴类型的return，避免后续多余的判断，也方便调试正确的判断
                    when (alignment) {
                        LeftToLeft, LeftToHorizontalCenter, LeftToRight,
                        HorizontalCenterToLeft, HorizontalCenterToHorizontalCenter, RightToHorizontalCenter,
                        RightToLeft, HorizontalCenterToRight, RightToRight -> return@verticalAnalyzeFunc
                        else -> Unit
                    }
                    topAnalyze = analyzeTop(magneticRect, magnetRect,
                        magnet, alignment, topAnalyze)
                    verticalAnalyze = analyzeVerticalCenter(magneticRect, magnetRect,
                        magnet, alignment, verticalAnalyze)
                    bottomAnalyze = analyzeBottom(magneticRect, magnetRect,
                        magnet, alignment, bottomAnalyze)
                }
                //步骤1.4.3，前面定义的是局部方法，为了方便return，这里开始执行测量
                horizontalAnalyzeFunc()
                verticalAnalyzeFunc()
            }
            //步骤1.5 每次测量结果添加到列表中，添加列表前做判断，只取比上次测量更接近磁性物体的磁铁
            leftAnalyzes.addAnalyze(leftAnalyze)
            horizontalCenterAnalyzes.addAnalyze(horizontalAnalyze)
            rightAnalyzes.addAnalyze(rightAnalyze)
            topAnalyzes.addAnalyze(topAnalyze)
            verticalCenterAnalyzes.addAnalyze(verticalAnalyze)
            bottomAnalyzes.addAnalyze(bottomAnalyze)
        }
        //步骤1.6 记录测量出来的结果
        if (!isAdsorptionH && !isHAdsorptionImmunityRegion) {
            //步骤1.6.1.1 x轴没有被吸附并且不在免疫区的时候才记录磁铁
            this.leftAnalyzes = leftAnalyzes
            this.horizontalCenterAnalyzes = horizontalCenterAnalyzes
            this.rightAnalyzes = rightAnalyzes
            val mins = arrayListOf<Int>()
            leftAnalyzes.minOfOrNull { it.distance }?.apply {
                mins.add(this)
            }
            horizontalCenterAnalyzes.minOfOrNull { it.distance }?.apply {
                mins.add(this)
            }
            rightAnalyzes.minOfOrNull { it.distance }?.apply {
                mins.add(this)
            }
            //步骤1.6.1.2 在1.4.1测量出来的Left、HorizontalCenter、Right的结果做一个比较，获取最小的偏移量作为吸附动画x轴值
            minXOffset = mins.minOfOrNull { it } ?: 0
            //步骤1.6.1.3 如果x轴有吸附，记录下来是从哪边到哪边移动的，下次需要根据移动轨迹的条件重置吸附状态
            hMovementTrack = if (minXOffset < 0) {
                MoveMovementTrack.RightToLeft
            } else if (minXOffset > 0) {
                MoveMovementTrack.LeftToRight
            } else {
                MoveMovementTrack.None
            }
        } else {
            //已经吸附或处于免疫区，计算好的x轴吸附范围置空
        }
        if (!isAdsorptionV && !isVAdsorptionImmunityRegion) {
            //步骤1.6.2.1 y轴没有被吸附并且不在免疫区的时候才记录磁铁
            this.topAnalyzes = topAnalyzes
            this.verticalCenterAnalyzes = verticalCenterAnalyzes
            this.bottomAnalyzes = bottomAnalyzes
            val mins = arrayListOf<Int>()
            topAnalyzes.minOfOrNull { it.distance }?.apply {
                mins.add(this)
            }
            verticalCenterAnalyzes.minOfOrNull { it.distance }?.apply {
                mins.add(this)
            }
            bottomAnalyzes.minOfOrNull { it.distance }?.apply {
                mins.add(this)
            }
            //步骤1.6.1.2 在1.4.2测量出来的Top、VerticalCenter、Right的结果做一个比较，获取最小的偏移量作为吸附动画y轴值
            minYOffset = mins.minOfOrNull { it } ?: 0
            //步骤1.6.1.3 如果y轴有吸附，记录下来是从哪边到哪边移动的，下次需要根据移动轨迹的条件重置吸附状态
            vMovementTrack = if (minYOffset < 0) {
                MoveMovementTrack.BottomToTop
            } else if (minYOffset > 0) {
                MoveMovementTrack.TopToBottom
            } else {
                MoveMovementTrack.None
            }
        } else {
            //已经吸附或处于免疫区，计算好的y轴吸附范围置空
        }
        this.magneticRect.setEmpty()
        this.magnetRect.setEmpty()
        return Point(minXOffset, minYOffset)
    }

    /**
     * 添加测量结果
     */
    private fun ArrayList<MoveAnalyzeResult>.addAnalyze(analyze: MoveAnalyzeResult?) {
        analyze ?: return
        if (isEmpty()) {
            //步骤1.5.1.1 集合为空意味着第一次遇到x轴符合吸附条件的，直接记录磁铁
            add(analyze)
        } else {
            //存到List中的所有distance都应该是一样的，所以只需要取第一个比较即可
            if (get(0).distance > analyze.distance) {
                //步骤1.5.1.2 第N次遇到x轴吸附条件更合适的，清空原有磁铁，添加更合适的磁铁
                clear()
                add(analyze)
            } else {
                if (get(0).distance == analyze.distance) {
                    //步骤1.5.1.3 第N次遇到x轴吸附条件同样更合适的的，继续添加，这种情况出现在多个磁铁处于同一个x点
                    add(analyze)
                }/*else{
                    //条件不适合不添加
                }*/
            }
        }
    }

    /**
     * 测量左边吸附距离
     */
    private fun analyzeLeft(
        magneticRect: Rect, magnetRect: Rect, magnet: MoveMagnet,
        alignment: ConstrainedAlignment,
        lastAnalyzeResult: MoveAnalyzeResult?): MoveAnalyzeResult? {
        val distance: Int?
        var align: ConstrainedAlignment? = null
        var result: MoveAnalyzeResult? = lastAnalyzeResult
        //磁铁支持左对齐，继续判断磁性物体支持的左对齐方式
        if (Left in magnet.alignments) {
            distance = when (alignment) {
                LeftToLeft -> {
                    align = LeftToLeft
                    //根据手势去吸附，如果Left->Right手势，那么肯定不能出现Right->Left的吸附，其他同理
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.left - magneticRect.left), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.left - magneticRect.left), 0).nullIf(0)
                        else -> null
                    }
                }
                HorizontalCenterToLeft -> {
                    align = HorizontalCenterToLeft
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.left - magneticRect.centerX()), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.left - magneticRect.centerX()), 0).nullIf(0)
                        else -> null
                    }
                }
                RightToLeft -> {
                    align = RightToLeft
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.left - magneticRect.right), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.left - magneticRect.right), 0).nullIf(0)
                        else -> null
                    }
                }
                else -> null
            }
            if (distance != null && abs(distance) < magnet.hMagnetismThreshold) {
                when {
                    //如果上一次为空，就直接返回这次测量的值
                    lastAnalyzeResult == null
                            //或者上次测量的值比这次大，返回这次最新的测量的值
                            || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                        result = MoveAnalyzeResult(distance, align!!, magnet)
                    }
                }
            }
        }
        return result
    }

    /**
     * 测量水平居中吸附距离
     */
    private fun analyzeHorizontalCenter(
        magneticRect: Rect, magnetRect: Rect, magnet: MoveMagnet,
        alignment: ConstrainedAlignment,
        lastAnalyzeResult: MoveAnalyzeResult?): MoveAnalyzeResult? {
        val distance: Int?
        var align: ConstrainedAlignment? = null
        var result: MoveAnalyzeResult? = lastAnalyzeResult
        //磁铁支持水平居中对齐(垂直线)，继续判断磁性物体支持的水平居中对齐方式
        if (HorizontalCenter in magnet.alignments) {
            distance = when (alignment) {
                LeftToHorizontalCenter -> {
                    align = LeftToHorizontalCenter
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.centerX() - magneticRect.left), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.centerX() - magneticRect.left), 0).nullIf(0)
                        else -> null
                    }
                }
                HorizontalCenterToHorizontalCenter -> {
                    align = HorizontalCenterToHorizontalCenter
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.centerX() - magneticRect.centerX()), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.centerX() - magneticRect.centerX()), 0).nullIf(0)
                        else -> null
                    }
                }
                RightToHorizontalCenter -> {
                    align = RightToHorizontalCenter
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.centerX() - magneticRect.right), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.centerX() - magneticRect.right), 0).nullIf(0)
                        else -> null
                    }
                }
                else -> null
            }
            if (distance != null && abs(distance) < magnet.hMagnetismThreshold) {
                when {
                    //如果上一次为空，就直接返回这次测量的值
                    lastAnalyzeResult == null
                            //或者上次测量的值比这次大，返回这次最新的测量的值
                            || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                        result = MoveAnalyzeResult(distance, align!!, magnet)
                    }
                }
            }
        }
        return result
    }

    /**
     * 测量右边吸附距离
     */
    private fun analyzeRight(
        magneticRect: Rect, magnetRect: Rect, magnet: MoveMagnet,
        alignment: ConstrainedAlignment,
        lastAnalyzeResult: MoveAnalyzeResult?): MoveAnalyzeResult? {
        val distance: Int?
        var align: ConstrainedAlignment? = null
        var result: MoveAnalyzeResult? = lastAnalyzeResult
        //磁铁支持右对齐，继续判断磁性物体支持的右对齐方式
        if (Right in magnet.alignments) {
            distance = when (alignment) {
                LeftToRight -> {
                    align = LeftToRight
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.right - magneticRect.left), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.right - magneticRect.left), 0).nullIf(0)
                        else -> null
                    }
                }
                HorizontalCenterToRight -> {
                    align = HorizontalCenterToRight
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.right - magneticRect.centerX()), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.right - magneticRect.centerX()), 0).nullIf(0)
                        else -> null
                    }
                }
                RightToRight -> {
                    align = RightToRight
                    when (state.hMovementTrack) {
                        MoveMovementTrack.RightToLeft ->
                            min((magnetRect.right - magneticRect.right), 0).nullIf(0)
                        MoveMovementTrack.LeftToRight ->
                            max((magnetRect.right - magneticRect.right), 0).nullIf(0)
                        else -> null
                    }
                }
                else -> null
            }
            if (distance != null && abs(distance) < magnet.hMagnetismThreshold) {
                when {
                    //如果上一次为空，就直接返回这次测量的值
                    lastAnalyzeResult == null
                            //或者上次测量的值比这次大，返回这次最新的测量的值
                            || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                        result = MoveAnalyzeResult(distance, align!!, magnet)
                    }
                }
            }
        }
        return result
    }

    /**
     * 测量顶部吸附距离
     */
    private fun analyzeTop(
        magneticRect: Rect, magnetRect: Rect, magnet: MoveMagnet,
        alignment: ConstrainedAlignment,
        lastAnalyzeResult: MoveAnalyzeResult?): MoveAnalyzeResult? {
        val distance: Int?
        var align: ConstrainedAlignment? = null
        var result: MoveAnalyzeResult? = lastAnalyzeResult
        //磁铁支持顶部对齐，继续判断磁性物体支持的顶部对齐方式
        if (Top in magnet.alignments) {
            distance = when (alignment) {
                TopToTop -> {
                    align = TopToTop
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.top - magneticRect.top), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.top - magneticRect.top), 0).nullIf(0)
                        else -> null
                    }
                }
                VerticalCenterToTop -> {
                    align = VerticalCenterToTop
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.top - magneticRect.centerY()), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.top - magneticRect.centerY()), 0).nullIf(0)
                        else -> null
                    }
                }
                BottomToTop -> {
                    align = BottomToTop
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.top - magneticRect.bottom), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.top - magneticRect.bottom), 0).nullIf(0)
                        else -> null
                    }
                }
                else -> null
            }
            if (distance != null && abs(distance) < magnet.vMagnetismThreshold) {
                when {
                    //如果上一次为空，就直接返回这次测量的值
                    lastAnalyzeResult == null
                            //或者上次测量的值比这次大，返回这次最新的测量的值
                            || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                        result = MoveAnalyzeResult(distance, align!!, magnet)
                    }
                }
            }
        }
        return result
    }


    /**
     * 测量垂直居中吸附距离
     */
    private fun analyzeVerticalCenter(
        magneticRect: Rect, magnetRect: Rect, magnet: MoveMagnet,
        alignment: ConstrainedAlignment,
        lastAnalyzeResult: MoveAnalyzeResult?): MoveAnalyzeResult? {
        val distance: Int?
        var align: ConstrainedAlignment? = null
        var result: MoveAnalyzeResult? = lastAnalyzeResult
        //磁铁支持垂直居中对齐(水平线)，继续判断磁性物体支持的垂直居中对齐方式
        if (VerticalCenter in magnet.alignments) {
            distance = when (alignment) {
                TopToVerticalCenter -> {
                    align = TopToVerticalCenter
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.centerY() - magneticRect.top), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.centerY() - magneticRect.top), 0).nullIf(0)
                        else -> null
                    }
                }
                VerticalCenterToVerticalCenter -> {
                    align = VerticalCenterToVerticalCenter
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.centerY() - magneticRect.centerY()), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.centerY() - magneticRect.centerY()), 0).nullIf(0)
                        else -> null
                    }
                }
                BottomToVerticalCenter -> {
                    align = BottomToVerticalCenter
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.centerY() - magneticRect.bottom), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.centerY() - magneticRect.bottom), 0).nullIf(0)
                        else -> null
                    }
                }
                else -> null
            }
            if (distance != null && abs(distance) < magnet.vMagnetismThreshold) {
                when {
                    //如果上一次为空，就直接返回这次测量的值
                    lastAnalyzeResult == null
                            //或者上次测量的值比这次大，返回这次最新的测量的值
                            || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                        result = MoveAnalyzeResult(distance, align!!, magnet)
                    }
                }
            }
        }
        return result
    }

    /**
     * 测量底部吸附距离
     */
    private fun analyzeBottom(
        magneticRect: Rect, magnetRect: Rect, magnet: MoveMagnet,
        alignment: ConstrainedAlignment,
        lastAnalyzeResult: MoveAnalyzeResult?): MoveAnalyzeResult? {
        val distance: Int?
        var align: ConstrainedAlignment? = null
        var result: MoveAnalyzeResult? = lastAnalyzeResult
        //磁铁支持底部对齐，继续判断磁性物体支持的底部对齐方式
        if (Bottom in magnet.alignments) {
            distance = when (alignment) {
                TopToBottom -> {
                    align = TopToBottom
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.bottom - magneticRect.top), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.bottom - magneticRect.top), 0).nullIf(0)
                        else -> null
                    }
                }
                VerticalCenterToBottom -> {
                    align = VerticalCenterToBottom
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.bottom - magneticRect.centerY()), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.bottom - magneticRect.centerY()), 0).nullIf(0)
                        else -> null
                    }
                }
                BottomToBottom -> {
                    align = BottomToBottom
                    when (state.vMovementTrack) {
                        MoveMovementTrack.BottomToTop ->
                            min((magnetRect.bottom - magneticRect.bottom), 0).nullIf(0)
                        MoveMovementTrack.TopToBottom ->
                            max((magnetRect.bottom - magneticRect.bottom), 0).nullIf(0)
                        else -> null
                    }
                }
                else -> null
            }
            if (distance != null && abs(distance) < magnet.vMagnetismThreshold) {
                when {
                    //如果上一次为空，就直接返回这次测量的值
                    lastAnalyzeResult == null
                            //或者上次测量的值比这次大，返回这次最新的测量的值
                            || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                        result = MoveAnalyzeResult(distance, align!!, magnet)
                    }
                }
            }
        }
        return result
    }

    /**
     * 步骤4.1 消费x轴移动以便挣脱x轴吸附
     * @return 返回消费后的值
     */
    private fun consumeMoveX(moveX: Float): Float {
        //步骤4.1.1 必须是吸附状态才能消费y轴移动
        if (adsorption.isAdsorptionH) {
            //步骤4.1.2 判断吸附时的手势，值消费对应手势的反方向值
            when (adsorption.hMovementTrack) {
                MoveMovementTrack.LeftToRight -> {
                    if (moveX > 0) {
                        return moveX
                    }
                }
                MoveMovementTrack.RightToLeft -> {
                    if (moveX < 0) {
                        return moveX
                    }
                }
                else -> Unit
            }
            return 0f
        }
        return moveX
    }

    /**
     * 步骤4.1 消费y轴移动以便挣脱y轴吸附
     * @return 返回消费后的值
     */
    private fun consumeMoveY(moveY: Float): Float {
        if (adsorption.isAdsorptionV) {
            when (adsorption.vMovementTrack) {
                MoveMovementTrack.TopToBottom -> {
                    if (moveY > 0) {
                        return moveY
                    }
                }
                MoveMovementTrack.BottomToTop -> {
                    if (moveY < 0) {
                        return moveY
                    }
                }
                else -> Unit
            }
            return 0f
        }
        return moveY
    }

    /**
     * 只有磁性物体被移除的时候才释放
     */
    private fun MoveAdsorption.release() {
        topAnalyzes.clear()
        verticalCenterAnalyzes.clear()
        bottomAnalyzes.clear()
        leftAnalyzes.clear()
        horizontalCenterAnalyzes.clear()
        rightAnalyzes.clear()
        adsorptionValueAnim?.cancel()
        adsorptionValueAnim = null
        magneticRect.setEmpty()
        magnetRect.setEmpty()
        hMovementTrack = MoveMovementTrack.None
        vMovementTrack = MoveMovementTrack.None
        pendingConsumeHRidThreshold = 0f
        pendingConsumeHImmunityThreshold = 0f
        pendingConsumeVRidThreshold = 0f
        pendingConsumeVImmunityThreshold = 0f
        isAdsorptionH = false
        isAdsorptionV = false
        isHAdsorptionImmunityRegion = false
        isVAdsorptionImmunityRegion = false
    }
}