package chenchen.engine.gesture

import chenchen.engine.gesture.BestGestureDetector

/**
 * 边缘手势处理，配合[BestGestureDetector]使用
 * 在[OnMoveListener.onMove]或[OnTouchListener.onTouchMove]中调用，
 * 在移动的过程中只能有一个边缘手势处理，多个会出现不可控的异常问题
 *
 * 用法：
 * ```kotlin
 * class XxxEdgeGestureDetector : EdgeGestureDetector() {
 *   override fun onMove(detector: BestGestureDetector): Boolean{
 *      ...
 *      return true or false
 *   }
 * }
 * val xxEdgeGesture = XxxEdgeGestureDetector()
 * val bestGesture = BestGestureDetector(context).apply {
 *      setOnTouchListener(object: OnSimpleTouchListener {
 *          override fun onTouchMove(detector: BestGestureDetector): Boolean {
 *              if(xxEdgeGesture.onMove(detector)){
 *                  return true
 *              }
 *              return super.onTouchMove(detector)
 *          }
 *      })
 * }
 * ```
 * @author: chenchen
 * @since: 2023/4/25 9:14
 */
abstract class EdgeGestureDetector {

    /**
     * 处理移动手势，在[BestGestureDetector]的move手势中调用这个方法
     * @param detector 手势
     */
    abstract fun onMove(detector: BestGestureDetector): Boolean
}