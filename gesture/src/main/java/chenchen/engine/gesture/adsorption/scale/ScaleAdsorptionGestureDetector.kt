package chenchen.engine.gesture.adsorption.scale

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.ScaleTrack
import chenchen.engine.gesture.ScaleGestureDetector
import chenchen.engine.gesture.nullIf
import java.lang.Float.max
import java.lang.Float.min
import kotlin.math.abs


/**
 * 缩放吸附手势
 * @author: chenchen
 * @since: 2023/10/13 23:14
 */
class ScaleAdsorptionGestureDetector(
    private val adsorption: Adsorption,
    private val adsorptionListener: OnScaleAdsorptionListener
) : ScaleGestureDetector() {

    private val TAG = "ScaleAdsorption"

    private val state = ScaleAdsorptionState()

    /**
     * 是否在吸附动画中
     */
    private var isInAdsorptionProgress = false

    /**
     * 吸附时的绝对缩放值，使用方式view.scaleX = adsorptionScale
     * 因为执行动画无法像手势拖动一样得到基于自身的缩放比，即使通过计算也无法获取精准的缩放比
     * 所以采用绝对缩放值做动画。最主要的原因还是我不会计算
     */
    var adsorptionScale = 1f
        private set


    override fun onScale(detector: BestGestureDetector): Boolean {
        state.rememberMovementTrack(detector)
        //如果动画在进行中则不重新测量
        if (!isInAdsorptionProgress) {
            //步骤1 测量吸附角度
            val scale = adsorption.analyze() //测量是否达到吸附条件
            //如果测量结果为0，表示不需要吸附
            if (scale != 0f) {
                //步骤2 通知开始吸附
                isInAdsorptionProgress = adsorptionListener.onBeginAdsorption(this)
                if (!isInAdsorptionProgress) {
                    return false
                }
                //步骤3 开始吸附动画
                adsorption.adsorptionValueAnim = ValueAnimator().apply {
                    val holders = arrayListOf<PropertyValuesHolder>()
                    //没有吸附，可以执行吸附动画
                    if (!adsorption.isAdsorptionScale && !adsorption.isScaleAdsorptionImmunityRegion) {
                        //scale只是差值，需要从当前缩放值~(当前缩放值+差值)
                        holders.add(PropertyValuesHolder.ofFloat("scale", safeScale(), safeScale() + scale))
                    }
                    setValues(*holders.toTypedArray())
                    addUpdateListener {
                        //步骤3.1 更新吸附动画进度
                        val scaleValue = (it.getAnimatedValue("scale") as? Float) ?: 1f
                        adsorptionScale = scaleValue
                        adsorptionListener.onAdsorption(this@ScaleAdsorptionGestureDetector)
                    }
                    doOnEnd {
                        //步骤3.2 通知吸附动画结束
                        adsorptionScale = 1f
                        adsorptionListener.onAdsorptionEnd(this@ScaleAdsorptionGestureDetector)
                        //步骤3.3 标记当前已经处于吸附状态
                        //已经在吸附状态不重复记录状态，并且记录的状态永远为true，如果出现false就是错误
                        if (!adsorption.isAdsorptionScale && !adsorption.isScaleAdsorptionImmunityRegion) {
                            adsorption.isAdsorptionScale = true
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
        val scaleFactor = detector.scaleFactor
        //状态流转是4->5->6
        //消费流程是6->5->4，先判断最高的状态
        //步骤6 消费免疫区，先消费免疫区，因为先消费挣脱值，状态就变为免疫区，这时候才消费免疫区就有问题
        adsorption.consumeScaleImmunityThreshold((safeScale() * scaleFactor) - safeScale())
        //步骤5 消费挣脱值，吸附之后会粘在吸附点上，需要滑动挣脱
        adsorption.consumeScaleRidThreshold((safeScale() * scaleFactor) - safeScale())
        //步骤4 如果处于吸附中先要把挣脱值消费掉才能缩放View，所以这里把缩放事件消费掉，不让View缩放
        detector.consumeScale(scaleFactor - consumeScale(scaleFactor))
        return isInAdsorptionProgress
    }

    private fun Adsorption.analyze(): Float {
        //步骤1.1，如果磁性物体已经被移除，则释放所有记录的内容
        if (!magnetic.target.isAttachedToWindow) {
            release()
            return 0f
        }
        if (adsorption.magnets.isEmpty()) {
            return 0f
        }
        //吸附中，不继续测量
        if (isAdsorptionScale || isScaleAdsorptionImmunityRegion) {
            return 0f
        }
        var lastAnalyzeResult: AnalyzeResult? = null
        val analyzes = arrayListOf<AnalyzeResult>()
        for (magnet in adsorption.magnets) {
            lastAnalyzeResult = analyzeScale(magnet, lastAnalyzeResult)
        }
        analyzes.addAnalyze(lastAnalyzeResult)
        adsorption.analyzes = analyzes
        val minScale = analyzes.minOfOrNull { it.distance } ?: 0f
        track = if (minScale < 0f) {
            ScaleTrack.ZoomOut
        } else if (minScale > 0f) {
            ScaleTrack.ZoomIn
        } else {
            ScaleTrack.None
        }
        return minScale
    }

    /**
     * 添加测量结果
     */
    private fun ArrayList<AnalyzeResult>.addAnalyze(analyze: AnalyzeResult?) {
        analyze ?: return
        if (isEmpty()) {
            //步骤1.5.1.1 集合为空意味着第一次遇到缩放角度符合吸附条件的，直接记录磁铁
            add(analyze)
        } else {
            //存到List中的所有distance都应该是一样的，所以只需要取第一个比较即可
            if (get(0).distance > analyze.distance) {
                //步骤1.5.1.2 第N次遇到缩放角度吸附条件更合适的，清空原有磁铁，添加更合适的磁铁
                clear()
                add(analyze)
            } else {
                if (get(0).distance == analyze.distance) {
                    //步骤1.5.1.3 第N次遇到缩放角度吸附条件同样更合适的的，继续添加，这种情况出现在多个磁铁处于同一个缩放角度
                    add(analyze)
                }/*else{
                    //条件不适合不添加
                }*/
            }
        }
    }

    /**
     * 分析最小的缩放角度距离
     */
    private fun analyzeScale(magnet: Magnet, lastAnalyzeResult: AnalyzeResult?): AnalyzeResult? {
        var result: AnalyzeResult? = lastAnalyzeResult
        val magneticScale = safeScale()
        val distance = when (state.scaleTrack) {
            ScaleTrack.ZoomOut ->
                min((magnet.scale - magneticScale), 0f).nullIf(0f)
            ScaleTrack.ZoomIn ->
                max((magnet.scale - magneticScale), 0f).nullIf(0f)
            else -> null
        }
        if (distance != null && abs(distance) < magnet.sMagnetismThreshold) {
            when {
                //如果上一次为空，就直接返回这次测量的值
                lastAnalyzeResult == null
                        //或者上次测量的值比这次大，返回这次最新的测量的值
                        || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                    result = AnalyzeResult(distance, magnet)
                }
            }
        }
        return result
    }


    /**
     * 步骤4.1 消费缩放以便挣脱缩放吸附
     * @return 返回消费后的值
     */
    private fun consumeScale(scaleFactor: Float): Float {
        //步骤4.1.1 必须是吸附状态才能消费y轴移动
        if (adsorption.isAdsorptionScale) {
            //步骤4.1.2 判断吸附时的手势，值消费对应手势的反方向值
            when (adsorption.track) {
                ScaleTrack.ZoomIn -> {
                    if (scaleFactor > 1f) {
                        return scaleFactor
                    }
                }
                ScaleTrack.ZoomOut -> {
                    if (scaleFactor < 1f) {
                        return scaleFactor
                    }
                }
                else -> Unit
            }
            return 0f
        }
        return scaleFactor
    }

    /**
     * 以最大的缩放值为准
     */
    private fun safeScale(): Float {
        val scaleX = adsorption.magnetic.target.scaleX
        val scaleY = adsorption.magnetic.target.scaleY
        return max(scaleX, scaleY)
    }

    /**
     * 只有磁性物体被移除的时候才释放
     */
    private fun Adsorption.release() {
        analyzes.clear()
        adsorptionValueAnim?.cancel()
        adsorptionValueAnim = null
        track = ScaleTrack.None
        pendingConsumeScaleRidThreshold = 0f
        pendingConsumeScaleImmunityThreshold = 0f
        isAdsorptionScale = false
        isScaleAdsorptionImmunityRegion = false
    }
}