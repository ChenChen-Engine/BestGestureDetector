package chenchen.engine.gesture.adsorption.scale

import android.animation.ValueAnimator
import android.view.View
import chenchen.engine.gesture.ScaleTrack
import kotlin.math.abs

/**
 * 缩放吸附
 * @author: chenchen
 * @since: 2023/10/16 17:47
 */
class Adsorption(
    /**
     * 磁性物体
     */
    val magnetic: Magnetic,
    /**
     * 磁铁列表
     */
    val magnets: List<Magnet>,
) {
    /**
     * 吸附动画
     */
    internal var adsorptionValueAnim: ValueAnimator? = null

    /**
     * 吸住的磁铁，正常情况只有1个，如果有多个，必然是相同的值
     */
    internal var analyzes = ArrayList<AnalyzeResult>()

    /**
     * 上次被吸住的缩放轨迹
     */
    internal var track: ScaleTrack = ScaleTrack.None

    /**
     * 是否吸附，吸附之后需要挣脱
     */
    internal var isAdsorptionScale = false
        set(value) {
            field = value
            if (field) {
                //找出磁性最大的磁铁，以它的挣脱值为准
                pendingConsumeScaleRidThreshold = getMaxScaleRidThreshold()
            }
        }

    /**
     * 是否处于免疫吸附的区域
     */
    internal var isScaleAdsorptionImmunityRegion = false

    /**
     * 待挣脱的缩放值
     */
    internal var pendingConsumeScaleRidThreshold = 0f
        set(value) {
            field = value
            if (field <= 0f) {
                //步骤5.3 挣脱后是处于免疫区
                field = 0f
                isAdsorptionScale = false
                isScaleAdsorptionImmunityRegion = true
                //步骤5.4 找出磁性最大的磁铁，以它的免疫区为准
                pendingConsumeScaleImmunityThreshold = getMaxScaleImmunityThreshold()
            }
        }

    /**
     * 挣脱缩放后的免疫区域值
     */
    internal var pendingConsumeScaleImmunityThreshold = 0f
        set(value) {
            field = value
            //脱离免疫区后就可以再次被吸附了
            if (field <= 0f) {
                //步骤 6.3 脱离免疫区
                field = 0f
                isScaleAdsorptionImmunityRegion = false
            }
        }

    /**
     * 获取被吸住的磁铁记录
     */
    private fun getMaxAdsorptionMeasureResult(measures: List<AnalyzeResult>): AnalyzeResult? {
        return measures.maxByOrNull { it.magnet.sMagnetismThreshold }
    }

    /**
     * 获取磁性最大的磁铁
     */
    private fun getMaxAdsorptionThresholdMagnet(measures: List<AnalyzeResult>): Magnet? {
        return getMaxAdsorptionMeasureResult(measures)?.magnet
    }

    /**
     * 获取磁性最大的挣脱值
     */
    private fun getMaxScaleRidThreshold(): Float {
        //找出磁性最大的磁铁，以它的挣脱值为准
        val magnet = getMaxAdsorptionThresholdMagnet(analyzes)
        return magnet?.sRidThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取磁性最大的免疫区
     */
    private fun getMaxScaleImmunityThreshold(): Float {
        //找出磁性最大的磁铁，以它的免疫区为准
        val magnet = getMaxAdsorptionThresholdMagnet(analyzes)
        return magnet?.sImmunityThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取被吸住的磁铁和磁性物体的缩放距离
     */
    private fun getScaleAlignmentDistance(): Float {
        if (!magnetic.target.isAttachedToWindow) {
            return 0f
        }
        val measureResult = getMaxAdsorptionMeasureResult(analyzes) ?: return 0f
        return safeScale() - measureResult.distance
    }


    /**
     * 步骤5.1 被吸附上了，需要挣脱
     */
    fun consumeScaleRidThreshold(scaleFactor: Float) {
        //步骤5.1 必须吸附才消费挣脱
        if (isAdsorptionScale) {
            //步骤5.2 判断吸附时的手势，如果是反方向就消费，如果换了一遍方向，就换手势
            when (track) {
                ScaleTrack.ZoomIn -> {
                    if (scaleFactor < 0f) {
                        //步骤5.2.1.1 手势是顺时针被吸附的，那么往逆时针移动就消费挣脱值
                        pendingConsumeScaleRidThreshold += scaleFactor
                    } else if (scaleFactor > 0f) {
                        //步骤5.2.1.2 手势是顺时针被吸附的，继续往顺时针移动，切换手势变成逆时针
                        track = ScaleTrack.ZoomOut
                        pendingConsumeScaleRidThreshold = getMaxScaleRidThreshold()
                    }
                }

                ScaleTrack.ZoomOut -> {
                    if (scaleFactor < 0f) {
                        //步骤5.2.1.2 手势是逆时针被吸附的，继续往逆时针移动，切换手势变成顺时针
                        track = ScaleTrack.ZoomIn
                        pendingConsumeScaleRidThreshold = getMaxScaleRidThreshold()
                    } else if (scaleFactor > 0f) {
                        //步骤5.2.1.1 手势是逆时针被吸附的，那么往顺时针移动就消费挣脱值
                        pendingConsumeScaleRidThreshold -= scaleFactor
                    }
                }

                else -> Unit
            }
        }
    }


    /**
     * 步骤6 处于免疫区，免疫区内部不会再次吸附，需要脱离免疫区才能吸附
     */
    fun consumeScaleImmunityThreshold(scaleFactor: Float) {
        //步骤6.1 必须处于免疫区才消费
        if (isScaleAdsorptionImmunityRegion) {
            //步骤6.2 判断吸附时的手势
            when (track) {
                ScaleTrack.ZoomIn -> {
                    if (scaleFactor < 0f) {
                        //6.2.1 手势是顺时针被吸附的，那么往逆时针就脱离免疫区
                        val distance = getScaleAlignmentDistance()
                        if (abs(distance) > pendingConsumeScaleImmunityThreshold) {
                            pendingConsumeScaleImmunityThreshold = 0f
                        }
                    } else if (scaleFactor > 0f) {
                        //6.2.2 手势是顺时针被吸附的，继续往顺时针移动，切换手势变成逆时针
                        track = ScaleTrack.ZoomOut
                        //这里直接脱离免疫区，因为改变了方向
                        pendingConsumeScaleImmunityThreshold = 0f
                    }
                }

                ScaleTrack.ZoomOut -> {
                    if (scaleFactor < 0f) {
                        //6.2.2 手势是逆时针被吸附的，继续往逆时针移动，切换手势变成顺时针
                        track = ScaleTrack.ZoomIn
                        //这里直接脱离免疫区，因为改变了方向
                        pendingConsumeScaleImmunityThreshold = 0f
                    } else if (scaleFactor > 0f) {
                        //6.2.1 手势是逆时针被吸附的，那么往顺时针移动就脱离免疫区
                        val distance = getScaleAlignmentDistance()
                        if (abs(distance) > pendingConsumeScaleImmunityThreshold) {
                            pendingConsumeScaleImmunityThreshold = 0f
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    /**
     * 确保取值都在0~360
     */
    private fun safeScale(): Float {
        val scaleX = magnetic.target.scaleX
        val scaleY = magnetic.target.scaleY
        return maxOf(scaleX, scaleY)
    }
}

/**
 * 磁性物体
 */
class Magnetic(
    /**
     * 有磁性的View，可以被磁吸
     */
    val target: View,
)

/**
 * 磁铁
 */
class Magnet(
    /**
     * 磁铁的约束方式
     */
    val scale: Float,
    /**
     * 缩放的磁性阈值，比如触发Scale1.2, Scale1.5, Scale2.0吸附时的阈值
     */
    val sMagnetismThreshold: Float = 0.1f,
    /**
     * 缩放的挣脱阈值，比如挣脱Scale1.2, Scale1.5, Scale2.0的阈值
     */
    val sRidThreshold: Float = sMagnetismThreshold * 2f,
    /**
     * 缩放挣脱吸附后，在一定区间内不会再吸附回去的阈值
     */
    val sImmunityThreshold: Float = 0.2f,
)

/**
 * 测量结果
 */
data class AnalyzeResult(
    /**
     * 磁性物体与磁铁的角度距离
     */
    val distance: Float,
    /**
     * 磁铁
     */
    val magnet: Magnet
)
