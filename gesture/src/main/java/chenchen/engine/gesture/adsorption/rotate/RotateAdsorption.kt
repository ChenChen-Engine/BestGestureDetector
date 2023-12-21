package chenchen.engine.gesture.adsorption.rotate

import android.animation.ValueAnimator
import android.view.View
import chenchen.engine.gesture.RotationTrack
import kotlin.math.abs

/**
 * 旋转吸附
 * @author: chenchen
 * @since: 2023/10/16 17:47
 */
class RotateAdsorption(
    /**
     * 磁性物体
     */
    val magnetic: RotateMagnetic,
    /**
     * 磁铁列表
     */
    val magnets: List<RotateMagnet>,
    /**
     * 吸附动画执行时长
     */
    val duration: Long = 30L,
) {
    /**
     * 是否在吸附动画中
     */
    internal var isInAdsorptionProgress = false

    /**
     * 吸附动画
     */
    internal var adsorptionValueAnim: ValueAnimator? = null

    /**
     * 吸住的磁铁，正常情况只有1个，如果有多个，必然是相同的值
     */
    internal var analyzes = ArrayList<RotateAnalyzeResult>()

    /**
     * 上次被吸住的旋转轨迹
     */
    internal var track: RotationTrack = RotationTrack.None

    /**
     * 是否吸附，吸附之后需要挣脱
     */
    internal var isAdsorptionRotation = false
        set(value) {
            field = value
            if (field) {
                //找出磁性最大的磁铁，以它的挣脱值为准
                pendingConsumeRotationRidThreshold = getMaxRotationRidThreshold()
            }
        }

    /**
     * 是否处于免疫吸附的区域
     */
    internal var isRotationAdsorptionImmunityRegion = false

    /**
     * 待挣脱的旋转值
     */
    internal var pendingConsumeRotationRidThreshold = 0f
        set(value) {
            field = value
            if (field <= 0f) {
                //步骤5.3 挣脱后是处于免疫区
                field = 0f
                isAdsorptionRotation = false
                isRotationAdsorptionImmunityRegion = true
                //步骤5.4 找出磁性最大的磁铁，以它的免疫区为准
                pendingConsumeRotationImmunityThreshold = getMaxRotationImmunityThreshold()
            }
        }

    /**
     * 挣脱旋转后的免疫区域值
     */
    internal var pendingConsumeRotationImmunityThreshold = 0f
        set(value) {
            field = value
            //脱离免疫区后就可以再次被吸附了
            if (field <= 0f) {
                //步骤 6.3 脱离免疫区
                field = 0f
                isRotationAdsorptionImmunityRegion = false
            }
        }

    /**
     * 获取被吸住的磁铁记录
     */
    private fun getMaxAdsorptionMeasureResult(measures: List<RotateAnalyzeResult>): RotateAnalyzeResult? {
        return measures.maxByOrNull { it.magnet.rMagnetismThreshold }
    }

    /**
     * 获取磁性最大的磁铁
     */
    private fun getMaxAdsorptionThresholdMagnet(measures: List<RotateAnalyzeResult>): RotateMagnet? {
        return getMaxAdsorptionMeasureResult(measures)?.magnet
    }

    /**
     * 获取磁性最大的挣脱值
     */
    private fun getMaxRotationRidThreshold(): Float {
        //找出磁性最大的磁铁，以它的挣脱值为准
        val magnet = getMaxAdsorptionThresholdMagnet(analyzes)
        return magnet?.rRidThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取磁性最大的免疫区
     */
    private fun getMaxRotationImmunityThreshold(): Float {
        //找出磁性最大的磁铁，以它的免疫区为准
        val magnet = getMaxAdsorptionThresholdMagnet(analyzes)
        return magnet?.rImmunityThreshold?.toFloat() ?: 0f
    }

    /**
     * 获取被吸住的磁铁和磁性物体的旋转距离
     */
    private fun getRotationAlignmentDistance(): Float {
        if (!magnetic.target.isAttachedToWindow) {
            return 0f
        }
        val measureResult = getMaxAdsorptionMeasureResult(analyzes) ?: return 0f
        return safeRotation(magnetic.target.rotation) - measureResult.distance
    }


    /**
     * 步骤5.1 被吸附上了，需要挣脱
     */
    fun consumeRotationRidThreshold(rotation: Float) {
        //步骤5.1 必须吸附才消费挣脱
        if (isAdsorptionRotation) {
            //步骤5.2 判断吸附时的手势，如果是反方向就消费，如果换了一遍方向，就换手势
            when (track) {
                RotationTrack.Clockwise -> {
                    if (rotation < 0) {
                        //步骤5.2.1.1 手势是顺时针被吸附的，那么往逆时针移动就消费挣脱值
                        pendingConsumeRotationRidThreshold += rotation
                    } else if (rotation > 0) {
                        //步骤5.2.1.2 手势是顺时针被吸附的，继续往顺时针移动，切换手势变成逆时针
                        track = RotationTrack.Anticlockwise
                        pendingConsumeRotationRidThreshold = getMaxRotationRidThreshold()
                    }
                }

                RotationTrack.Anticlockwise -> {
                    if (rotation < 0) {
                        //步骤5.2.1.2 手势是逆时针被吸附的，继续往逆时针移动，切换手势变成顺时针
                        track = RotationTrack.Clockwise
                        pendingConsumeRotationRidThreshold = getMaxRotationRidThreshold()
                    } else if (rotation > 0) {
                        //步骤5.2.1.1 手势是逆时针被吸附的，那么往顺时针移动就消费挣脱值
                        pendingConsumeRotationRidThreshold -= rotation
                    }
                }

                else -> Unit
            }
        }
    }


    /**
     * 步骤6 处于免疫区，免疫区内部不会再次吸附，需要脱离免疫区才能吸附
     */
    fun consumeRotationImmunityThreshold(rotation: Float) {
        //步骤6.1 必须处于免疫区才消费
        if (isRotationAdsorptionImmunityRegion) {
            //步骤6.2 判断吸附时的手势
            when (track) {
                RotationTrack.Clockwise -> {
                    if (rotation < 0) {
                        //6.2.1 手势是顺时针被吸附的，那么往逆时针就脱离免疫区
                        val distance = getRotationAlignmentDistance()
                        if (abs(distance) > pendingConsumeRotationImmunityThreshold) {
                            pendingConsumeRotationImmunityThreshold = 0f
                        }
                    } else if (rotation > 0) {
                        //6.2.2 手势是顺时针被吸附的，继续往顺时针移动，切换手势变成逆时针
                        track = RotationTrack.Anticlockwise
                        //这里直接脱离免疫区，因为改变了方向
                        pendingConsumeRotationImmunityThreshold = 0f
                    }
                }

                RotationTrack.Anticlockwise -> {
                    if (rotation < 0) {
                        //6.2.2 手势是逆时针被吸附的，继续往逆时针移动，切换手势变成顺时针
                        track = RotationTrack.Clockwise
                        //这里直接脱离免疫区，因为改变了方向
                        pendingConsumeRotationImmunityThreshold = 0f
                    } else if (rotation > 0) {
                        //6.2.1 手势是逆时针被吸附的，那么往顺时针移动就脱离免疫区
                        val distance = getRotationAlignmentDistance()
                        if (abs(distance) > pendingConsumeRotationImmunityThreshold) {
                            pendingConsumeRotationImmunityThreshold = 0f
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
    private fun safeRotation(rotation: Float): Float {
        return (rotation % 360 + 360) % 360
    }
}

/**
 * 磁性物体
 */
class RotateMagnetic(
    /**
     * 有磁性的View，可以被磁吸
     */
    val target: View,
)

/**
 * 磁铁
 */
class RotateMagnet(
    /**
     * 磁铁的约束方式，注意0°!=360°，如果需要0°也要吸附，必须同时传360°，因为我不会计算
     */
    val rotation: Int,
    /**
     * 旋转的磁性阈值，比如触发0°、45°、90°、180°、270°吸附时的阈值
     */
    val rMagnetismThreshold: Int = 5,
    /**
     * 旋转的挣脱阈值，比如挣脱0°、45°、90°、180°、270°的阈值
     */
    val rRidThreshold: Int = rMagnetismThreshold * 2,
    /**
     * 旋转挣脱吸附后，在一定区间内不会再吸附回去的阈值
     */
    val rImmunityThreshold: Int = 5,
)

/**
 * 测量结果
 */
data class RotateAnalyzeResult(
    /**
     * 磁性物体与磁铁的角度距离
     */
    val distance: Int,
    /**
     * 磁铁
     */
    val magnet: RotateMagnet
)
