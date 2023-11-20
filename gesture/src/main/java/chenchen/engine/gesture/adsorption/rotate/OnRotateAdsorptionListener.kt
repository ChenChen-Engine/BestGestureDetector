package chenchen.engine.gesture.adsorption.rotate


/**
 * 旋转吸附监听
 * @author: chenchen
 * @since: 2023/10/16 17:47
 */
interface OnRotateAdsorptionListener {
    /**
     * 开始吸附
     */
    fun onBeginAdsorption(detector: RotateAdsorptionGestureDetector): Boolean = true

    /**
     * 吸附中
     */
    fun onAdsorption(detector: RotateAdsorptionGestureDetector): Boolean

    /**
     * 吸附结束
     */
    fun onAdsorptionEnd(detector: RotateAdsorptionGestureDetector) = Unit
}