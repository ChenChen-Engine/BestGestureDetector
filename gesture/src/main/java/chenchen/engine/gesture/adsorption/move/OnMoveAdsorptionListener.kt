package chenchen.engine.gesture.adsorption.move

/**
 * 移动吸附监听
 * @author: chenchen
 * @since: 2023/10/13 23:14
 */
interface OnMoveAdsorptionListener {
    /**
     * 开始吸附
     */
    fun onBeginAdsorption(detector: MoveAdsorptionGestureDetector): Boolean = true

    /**
     * 吸附中
     */
    fun onAdsorption(detector: MoveAdsorptionGestureDetector): Boolean

    /**
     * 吸附结束
     */
    fun onAdsorptionEnd(detector: MoveAdsorptionGestureDetector) = Unit
}