package chenchen.engine.gesture.adsorption.rotate

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import chenchen.engine.gesture.BestGestureDetector
import chenchen.engine.gesture.RotateGestureDetector
import chenchen.engine.gesture.RotationTrack
import chenchen.engine.gesture.nullIf
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.abs


/**
 * 旋转吸附手势
 * @author: chenchen
 * @since: 2023/10/13 23:14
 */
class RotateAdsorptionGestureDetector(
    private val adsorption: RotateAdsorption,
    private val adsorptionListener: OnRotateAdsorptionListener
) : RotateGestureDetector() {

    private val TAG = "RotateAdsorption"

    private val state = RotteAdsorptionState()

    /**
     * 是否在吸附动画中
     */
    private var isInAdsorptionProgress = false

    /**
     * 吸附rotation值
     */
    var adsorptionRotation = 0
        private set


    override fun onRotate(detector: BestGestureDetector): Boolean {
        state.rememberMovementTrack(detector)
        //如果动画在进行中则不重新测量
        if (!isInAdsorptionProgress) {
            //步骤1 测量吸附角度
            val rotation = adsorption.analyze() //测量是否达到吸附条件
            //如果测量结果为0，表示不需要吸附
            if (rotation != 0) {
                //步骤2 通知开始吸附
                isInAdsorptionProgress = adsorptionListener.onBeginAdsorption(this)
                if (!isInAdsorptionProgress) {
                    return false
                }
                //步骤3 开始吸附动画
                adsorption.adsorptionValueAnim = ValueAnimator().apply {
                    var lastRotationValue = 0
                    val holders = arrayListOf<PropertyValuesHolder>()
                    //没有吸附，可以执行吸附动画
                    if (!adsorption.isAdsorptionRotation && !adsorption.isRotationAdsorptionImmunityRegion) {
                        holders.add(PropertyValuesHolder.ofInt("rotation", 0, rotation))
                    }
                    setValues(*holders.toTypedArray())
                    addUpdateListener {
                        //步骤3.1 更新吸附动画进度
                        val rotationValue = (it.getAnimatedValue("rotation") as? Int) ?: 0
                        adsorptionRotation = rotationValue - lastRotationValue
                        adsorptionListener.onAdsorption(this@RotateAdsorptionGestureDetector)
                        lastRotationValue = rotationValue
                    }
                    doOnEnd {
                        //步骤3.2 通知吸附动画结束
                        adsorptionRotation = 0
                        adsorptionListener.onAdsorptionEnd(this@RotateAdsorptionGestureDetector)
                        //步骤3.3 标记当前已经处于吸附状态
                        //已经在吸附状态不重复记录状态，并且记录的状态永远为true，如果出现false就是错误
                        if (!adsorption.isAdsorptionRotation && !adsorption.isRotationAdsorptionImmunityRegion) {
                            adsorption.isAdsorptionRotation = true
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
        val rotation = detector.rotation
        //状态流转是4->5->6
        //消费流程是6->5->4，先判断最高的状态
        //步骤6 消费免疫区，先消费免疫区，因为先消费挣脱值，状态就变为免疫区，这时候才消费免疫区就有问题
        adsorption.consumeRotationImmunityThreshold(rotation)
        //步骤5 消费挣脱值，吸附之后会粘在吸附点上，需要滑动挣脱
        adsorption.consumeRotationRidThreshold(rotation)
        //步骤4 如果处于吸附中先要把挣脱值消费掉才能旋转View，所以这里把旋转事件消费掉，不让View旋转
        detector.consumeRotation(rotation - consumeRotation(rotation))
        return isInAdsorptionProgress
    }

    private fun RotateAdsorption.analyze(): Int {
        //步骤1.1，如果磁性物体已经被移除，则释放所有记录的内容
        if (!magnetic.target.isAttachedToWindow) {
            release()
            return 0
        }
        if (adsorption.magnets.isEmpty()) {
            return 0
        }
        //吸附中，不继续测量
        if (isAdsorptionRotation || isRotationAdsorptionImmunityRegion) {
            return 0
        }
        var lastAnalyzeResult: RotateAnalyzeResult? = null
        val analyzes = arrayListOf<RotateAnalyzeResult>()
        for (magnet in adsorption.magnets) {
            lastAnalyzeResult = analyzeRotation(magnet, lastAnalyzeResult)
        }
        analyzes.addAnalyze(lastAnalyzeResult)
        adsorption.analyzes = analyzes
        val minRotation = analyzes.minOfOrNull { it.distance } ?: 0
        track = if (minRotation < 0) {
            RotationTrack.Anticlockwise
        } else if (minRotation > 0) {
            RotationTrack.Clockwise
        } else {
            RotationTrack.None
        }
        return minRotation
    }

    /**
     * 添加测量结果
     */
    private fun ArrayList<RotateAnalyzeResult>.addAnalyze(analyze: RotateAnalyzeResult?) {
        analyze ?: return
        if (isEmpty()) {
            //步骤1.5.1.1 集合为空意味着第一次遇到旋转角度符合吸附条件的，直接记录磁铁
            add(analyze)
        } else {
            //存到List中的所有distance都应该是一样的，所以只需要取第一个比较即可
            if (get(0).distance > analyze.distance) {
                //步骤1.5.1.2 第N次遇到旋转角度吸附条件更合适的，清空原有磁铁，添加更合适的磁铁
                clear()
                add(analyze)
            } else {
                if (get(0).distance == analyze.distance) {
                    //步骤1.5.1.3 第N次遇到旋转角度吸附条件同样更合适的的，继续添加，这种情况出现在多个磁铁处于同一个旋转角度
                    add(analyze)
                }/*else{
                    //条件不适合不添加
                }*/
            }
        }
    }

    /**
     * 分析最小的旋转角度距离
     */
    private fun analyzeRotation(magnet: RotateMagnet, lastAnalyzeResult: RotateAnalyzeResult?): RotateAnalyzeResult? {
        var result: RotateAnalyzeResult? = lastAnalyzeResult
        val magneticRotation = safeRotation(adsorption.magnetic.target.rotation)
        val distance = when (state.rotationTrack) {
            RotationTrack.Anticlockwise ->
                min((magnet.rotation - magneticRotation), 0).nullIf(0)
            RotationTrack.Clockwise ->
                max((magnet.rotation - magneticRotation), 0).nullIf(0)
            else -> null
        }
        if (distance != null && abs(distance) < magnet.rMagnetismThreshold) {
            when {
                //如果上一次为空，就直接返回这次测量的值
                lastAnalyzeResult == null
                        //或者上次测量的值比这次大，返回这次最新的测量的值
                        || abs(lastAnalyzeResult.distance) > abs(distance) -> {
                    result = RotateAnalyzeResult(distance, magnet)
                }
            }
        }
        return result
    }


    /**
     * 步骤4.1 消费旋转以便挣脱旋转吸附
     * @return 返回消费后的值
     */
    private fun consumeRotation(rotation: Float): Float {
        //步骤4.1.1 必须是吸附状态才能消费y轴移动
        if (adsorption.isAdsorptionRotation) {
            //步骤4.1.2 判断吸附时的手势，值消费对应手势的反方向值
            when (adsorption.track) {
                RotationTrack.Clockwise -> {
                    if (rotation > 0) {
                        return rotation
                    }
                }
                RotationTrack.Anticlockwise -> {
                    if (rotation < 0) {
                        return rotation
                    }
                }
                else -> Unit
            }
            return 0f
        }
        return rotation
    }

    /**
     * 确保取值都在0~360
     */
    private fun safeRotation(rotation: Float): Int {
        return (rotation.toInt() % 360 + 360) % 360
    }

    /**
     * 只有磁性物体被移除的时候才释放
     */
    private fun RotateAdsorption.release() {
        analyzes.clear()
        adsorptionValueAnim?.cancel()
        adsorptionValueAnim = null
        track = RotationTrack.None
        pendingConsumeRotationRidThreshold = 0f
        pendingConsumeRotationImmunityThreshold = 0f
        isAdsorptionRotation = false
        isRotationAdsorptionImmunityRegion = false
    }
}