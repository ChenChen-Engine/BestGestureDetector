package chenchen.engine.gesture.adsorption.move

/**
 * 吸附监听
 * @author: chenchen
 * @since: 2023/10/13 23:14
 */
interface OnMoveAdsorptionListener {
    /**
     * 开始吸附
     * 这里建议即使x,y轴达到了吸附区域，但通常只有吸附物的旋转角度为90°，180°，270°，360°/0°的时候才返回true
     */
    fun onBeginAdsorption(detector: MoveAdsorptionGestureDetector): Boolean

    /**
     * 吸附中
     */
    fun onAdsorption(detector: MoveAdsorptionGestureDetector): Boolean

    /**
     * 吸附结束
     */
    fun onAdsorptionEnd(detector: MoveAdsorptionGestureDetector)
}