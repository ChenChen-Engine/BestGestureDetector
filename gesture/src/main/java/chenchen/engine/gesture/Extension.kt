package chenchen.engine.gesture

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * @author: chenchen
 * @since: 2023/5/4 18:33
 */

/**
 * 获取状态栏高度
 */
internal val View.statusBarHeight: Int
    get() {
        val windowInsets = ViewCompat.getRootWindowInsets(this)
        if (windowInsets != null) {
            val insets: Insets = windowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
            return insets.top
        }
        return 0
    }

/**
 * 获取ActionBar高度
 */
internal val View.actionBarHeight: Int
    get() {
        return context.actionBarHeight
    }

internal val Activity.statusBarHeight:Int
    get() {
        val windowInsets = ViewCompat.getRootWindowInsets(window.decorView)
        if (windowInsets != null) {
            val insets: Insets = windowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())
            return insets.top
        }
        return 0
    }

/**
 * 获取ActionBar高度
 */
internal val Context.actionBarHeight: Int
    get() {
        return (this as? AppCompatActivity)?.supportActionBar?.height
            ?: (this as? AppCompatActivity)?.actionBar?.height
            ?: (this as? Activity)?.actionBar?.height ?: 0
    }

/**
 * 如果相等则为null
 */
internal fun Int.nullIf(equals: Int?): Int? {
    return if (this == equals) null else this
}

/**
 * 如果相等则为null
 */
internal fun Float.nullIf(equals: Float?): Float? {
    return if (this == equals) null else this
}

/**
 * 获取以[androidx.appcompat.R.id.content]为全局坐标的坐标，即使不可见，
 * 可通过参数[isContainsStatusBar]和[isContainsActionBar]来控制是否包含`StatusBar`和`ActionBar`的高度
 *
 * @param rect 需要获取的坐标
 * @param offset 用法参考[View.getGlobalVisibleRect]的参数
 * @param isContainsStatusBar 获取的坐标是否包含`StatusBar`高度，默认不包含
 * @param isContainsActionBar 获取的坐标是否包含`ActionBar`高度，默认不包含
 *
 * PS: 这里只处理了`StatusBar`和`ActionBar`的高度，没有处理虚拟键盘和手势的高度，可能会有影响
 */
fun View.getGlobalRect(
    rect: Rect, offset: Point? = null,
    isContainsStatusBar: Boolean = false,
    isContainsActionBar: Boolean = false,
): Boolean {
    val width: Int = this.right - this.left
    val height: Int = this.bottom - this.top
    if (width > 0 && height > 0) {
        rect.set(0, 0, width, height)
        offset?.set(-this.scrollX, -this.scrollY)
        var parent: ViewGroup? = parent as? ViewGroup ?: return true
        var child: View = this
        val tempRectF = RectF()
        while (parent != null) {

            tempRectF.setEmpty()
            tempRectF.set(rect)
            if (child.matrix?.isIdentity == false) {
                child.matrix.mapRect(tempRectF)
            }

            val dx = child.left - parent.scrollX
            val dy = child.top - parent.scrollY

            tempRectF.offset(dx.toFloat(), dy.toFloat())

            if (offset != null) {
                if (child.matrix?.isIdentity == false) {
                    val position = FloatArray(2)
                    position[0] = offset.x.toFloat()
                    position[1] = offset.y.toFloat()
                    child.matrix.mapPoints(position)
                    offset.x = position[0].roundToInt()
                    offset.y = position[1].roundToInt()
                }
                offset.x += dx
                offset.y += dy
            }
            rect.set(floor(tempRectF.left.toDouble()).toInt(),
                floor(tempRectF.top.toDouble()).toInt(),
                ceil(tempRectF.right.toDouble()).toInt(),
                ceil(tempRectF.bottom.toDouble()).toInt())

            child = parent
            parent = parent.parent as? ViewGroup
        }
        if (!isContainsStatusBar) {
            rect.offset(0, -statusBarHeight)
        }
        if (!isContainsActionBar) {
            rect.offset(0, -actionBarHeight)
        }
        return true
    }
    return false
}

/**
 * 将一个[View]相对自身的触摸位置，映射到另一个[View]相对自身的触摸位置
 * 也就是，我点击A[View]的位置，实际上是点到B[View]自身的什么位置
 * @param source 触摸的[View]
 * @param target 需要映射的[View]
 * @param x 相对[source]的x坐标
 * @param y 相对[source]的y坐标
 * @param isUp 是否向上查找，如果为false，会向下查找
 * @return 如果映射失败，则返回null
 */
fun touchLocalLocationMapToOtherViewLocalLocation(source: View, target: View, x: Float, y: Float, isUp: Boolean = true): PointF? {
    if (target.parent !is ViewGroup) return null
    val parent = source.parent as? ViewGroup ?: return null
    //查找的是同一个，直接返回
    if (source == target) return PointF(x, y)
    //如果查找的是同级
    if (parent.indexOfChild(target) != -1) {
        val parentLocation = source.reverseTransformTouchLocation(x, y)
        return target.transformTouchLocation(parentLocation.x, parentLocation.y)
    }
    if (!isUp) {
        //向下查询
        if (source is ViewGroup) {
            //如果查找的是直接子级，正向转换一次即可
            if (source.indexOfChild(target) != -1) {
                return target.transformTouchLocation(x, y)
            } else {
                var result: PointF? = null
                //直接子级找不到，就遍历并递归查找
                for (i in source.childCount downTo 0) {
                    val child = source.getChildAt(i)
                    val childLocation = child.transformTouchLocation(x, y)
                    result = touchLocalLocationMapToOtherViewLocalLocation(child,
                        target, childLocation.x, childLocation.y, isUp)
                    if (result != null) {
                        return result
                    }
                }
                return result
            }
        } else {
            return null
        }
    } else {
        //向上查询
        val parentLocation = source.reverseTransformTouchLocation(x, y)
        return touchLocalLocationMapToOtherViewLocalLocation(parent, target, parentLocation.x, parentLocation.y, isUp)
    }
}

/**
 * 将父[View]的相对位置转换成子[View]的相对位置
 * @param x 父[View]的x坐标
 * @param y 父[View]的y坐标
 */
fun View.transformTouchLocation(x: Float, y: Float): PointF {
    val parent = parent as? ViewGroup ?: return PointF(x, y)
    val offsetX = x + (parent.scrollX - this.left)
    val offsetY = y + (parent.scrollY - this.top)
    //如果矩阵被变换过，则变换触摸位置
    return if (!this.matrix.isIdentity) {
        val invertMatrix = this.matrix
        invertMatrix.reset()
        this.matrix.invert(invertMatrix)
        val points = floatArrayOf(offsetX, offsetY)
        invertMatrix.mapPoints(points)
        PointF(points[0], points[1])
    } else {
        PointF(offsetX, offsetY)
    }
}

/**
 * 将子[View]的相对位置逆转换成父[View]的相对位置
 * @param x 子[View]的x坐标
 * @param y 子[View]的y坐标
 */
fun View.reverseTransformTouchLocation(x: Float, y: Float): PointF {
    val parent = parent as ViewGroup
    val points = floatArrayOf(x, y)
    if (!matrix.isIdentity) {
        matrix.mapPoints(points)
    }
    val offsetX = points[0] + left - parent.scrollX
    val offsetY = points[1] + top - parent.scrollY
    return PointF(offsetX, offsetY)
}

/**
 * 将父View事件转成子View事件
 * @param event 父View的事件
 */
fun View.transformTouchEvent(event: MotionEvent): MotionEvent {
    val parent = parent as? ViewGroup ?: return event
    val transformedEvent = MotionEvent.obtain(event)
    val offsetX = parent.scrollX - this.left
    val offsetY = parent.scrollY - this.top
    transformedEvent.offsetLocation(offsetX.toFloat(), offsetY.toFloat())
    //如果矩阵被变换过，则变换触摸位置
    if (!this.matrix.isIdentity) {
        val invertMatrix = this.matrix
        invertMatrix.reset()
        this.matrix.invert(invertMatrix)
        transformedEvent.transform(invertMatrix)
    }
    return transformedEvent
}

/**
 * 将子View自己的事件逆转成父View的事件，这里注意，转换的结果和原始事件有精度误差，等于比较是没用的
 * @param event 子View自己的事件
 */
fun View.reverseTransformTouchEvent(event: MotionEvent): MotionEvent {
    val parent = parent as? ViewGroup ?: return event
    val transformedEvent = MotionEvent.obtain(event)
    if (!matrix.isIdentity) {
        transformedEvent.transform(matrix)
    }
    transformedEvent.offsetLocation(
            left - parent.scrollX.toFloat(),
            top - parent.scrollY.toFloat()
    )
    return transformedEvent
}

/**
 * 像父容器一样将事件分发给子View
 * @param event 父容器的事件
 */
fun View.doParentDispatchTouchEvent(event: MotionEvent): Boolean {
    val parent = parent as? ViewGroup ?: return false
    val transformedEvent = MotionEvent.obtain(event)
    val offsetX = parent.scrollX - this.left
    val offsetY = parent.scrollY - this.top
    transformedEvent.offsetLocation(offsetX.toFloat(), offsetY.toFloat())
    //如果矩阵被变换过，则变换触摸位置
    if (!this.matrix.isIdentity) {
        val invertMatrix = this.matrix
        invertMatrix.reset()
        this.matrix.invert(invertMatrix)
        transformedEvent.transform(invertMatrix)
    }
    val result = dispatchTouchEvent(transformedEvent)
    transformedEvent.recycle()
    return result
}