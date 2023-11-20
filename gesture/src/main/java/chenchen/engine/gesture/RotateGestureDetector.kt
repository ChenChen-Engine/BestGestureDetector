package chenchen.engine.gesture

/**
 * 旋转手势处理，配合[BestGestureDetector]使用
 * 在[OnRotateGestureListener.onRotate]或[OnTouchGestureListener.onTouchMove]中调用，
 * 在旋转的过程中只能有一个旋转手势处理，多个会出现不可控的异常问题
 * @author: chenchen
 * @since: 2023/10/13 23:14
 */
abstract class RotateGestureDetector {

    /**
     * 处理移动手势，在[BestGestureDetector]的[OnRotateGestureListener.onRotate]或[OnTouchGestureListener.onTouchMove]中调用这个方法
     * @param detector 手势
     */
    abstract fun onRotate(detector: BestGestureDetector):Boolean
}