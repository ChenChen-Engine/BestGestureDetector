package chenchen.engine.gesture.adsorption.scale


/**
 * 缩放吸附监听
 * @author: chenchen
 * @since: 2023/10/16 17:47
 */
interface OnScaleAdsorptionListener {
    /**
     * 开始吸附
     */
    fun onBeginAdsorption(detector: ScaleAdsorptionGestureDetector): Boolean = true

    /**
     * 吸附中
     */
    fun onAdsorption(detector: ScaleAdsorptionGestureDetector): Boolean

    /**
     * 吸附结束
     */
    fun onAdsorptionEnd(detector: ScaleAdsorptionGestureDetector) = Unit
}