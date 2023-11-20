package chenchen.engine.gesture

/**
 * 边缘手势处理，配合[BestGestureDetector]使用
 * 在[OnMoveGestureListener.onMove]或[OnTouchGestureListener.onTouchMove]中调用，
 * 在移动的过程中只能有一个边缘手势处理，多个会出现不可控的异常问题
 *
 * 用法：
 * ```kotlin
 * class XxxMoveGestureDetector : MoveGestureDetector() {
 *   override fun onMove(detector: BestGestureDetector): Boolean{
 *      ...
 *      return true or false
 *   }
 * }
 * val xxMoveGesture = XxxMoveGestureDetector()
 * val bestGesture = BestGestureDetector(context).apply {
 *      setOnTouchGestureListener(object: OnTouchGestureListener {
 *          override fun onTouchMove(detector: BestGestureDetector): Boolean {
 *              if(xxMoveGesture.onMove(detector)){
 *                  return true
 *              }
 *              return true
 *          }
 *      })
 * }
 * ```
 * @author: chenchen
 * @since: 2023/4/25 9:14
 */
abstract class MoveGestureDetector {

    /**
     * 处理移动手势，在[BestGestureDetector]的[OnMoveGestureListener.onMove]或[OnTouchGestureListener.onTouchMove]中调用这个方法
     * @param detector 手势
     */
    abstract fun onMove(detector: BestGestureDetector): Boolean
}