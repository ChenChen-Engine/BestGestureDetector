package chenchen.engine.gesture

import android.app.Activity
import android.content.Context
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

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

internal val Activity.statusBarHeight: Int
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
 * 将一个[View]相对自身的触摸位置，映射到另一个[View]相对自身的触摸位置
 * 也就是，我点击A[View]的位置，实际上是点到B[View]自身的什么位置
 * @param source 触摸的[View]
 * @param target 需要映射的[View]
 * @param x 相对[source]的x坐标
 * @param y 相对[source]的y坐标
 * @return 如果映射失败，则返回null
 */
fun touchLocationMapToOtherLocation(source: View, target: View, x: Float, y: Float): PointF? {
    //查找的是同一个，直接返回
    if (source == target) {
        return PointF(x, y)
    }
    //如果target是source的直接子View，立即返回
    if (source is ViewGroup) {
        if (source.indexOfChild(target) != -1) {
            return target.transformTouchLocation(x, y)
        }
    }
    //如果source是target的直接子View，立即返回
    if (target is ViewGroup) {
        if (target.indexOfChild(target) != -1) {
            return source.transformTouchLocation(x, y)
        }
    }
    //查找最小公倍父类
    val sourceParents = arrayListOf<ViewGroup>()
    val targetParents = arrayListOf<ViewGroup>()
    var sourceParent = (source as? ViewGroup) ?: (source.parent as? ViewGroup)
    var targetParent = (target as? ViewGroup) ?: (target.parent as? ViewGroup)
    outer@ while (sourceParent != null) {
        if (sourceParent != source) {
            sourceParents.add(sourceParent)
        }
        while (targetParent != null) {
            if (targetParent != target) {
                targetParents.add(targetParent)
            }
            if (sourceParent === targetParent) {
                // 找到共同父容器
                break@outer
            }
            targetParent = (targetParent.parent as? ViewGroup)
        }
        targetParents.clear()
        targetParent = (target as? ViewGroup) ?: (target.parent as? ViewGroup)
        sourceParent = sourceParent.parent as? ViewGroup
    }
    if (sourceParents.isEmpty() || targetParents.isEmpty()) {
        return null
    }

    //去掉公共父容器。特殊情况source往上转一层就可能是公共父容器了
    //      /                 /
    //    /  \          source  target
    //  /     \
    //source   \
    //         target
    sourceParents.removeLastOrNull()
    targetParents.removeLastOrNull()

    //source往上转换
    var sourceParentLocation = source.reverseTransformTouchLocation(x, y)
    for (parent in sourceParents) {
        sourceParentLocation = parent.reverseTransformTouchLocation(sourceParentLocation.x, sourceParentLocation.y)
    }
    //转到公共父容器，就往下转换
    for (parent in targetParents) {
        sourceParentLocation = parent.transformTouchLocation(sourceParentLocation.x, sourceParentLocation.y)
    }
    //最后转换给target
    return target.transformTouchLocation(sourceParentLocation.x, sourceParentLocation.y)
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
 * 将rect从[source]的坐标系映射到[target]坐标系
 * @param source 需要转换的[View]，确保left,top,right,bottom是正确的
 * @param target 将要转换到这个[View]所在的坐标系，确保left,top,right,bottom是正确的
 * @param rectF 需要映射的矩形，如果是随意提供的矩形，不需要在意特殊情况，如果是从[View]获取的，请参考以下代码：
 *```
 *  fun View.getRectF(): RectF {
 *     val rectF = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
 *     if (!matrix.isIdentity) {
 *          val matrix = matrix
 *          matrix.reset()
 *          matrix.postScale(abs(scaleX), abs(scaleY), left + pivotX, top + pivotY)
 *          matrix.mapRect(rectF)
 *     }
 *     return rectF
 *  }
 *```
 */
fun coordinateMapToCoordinate(source: View, target: View, rectF: RectF): RectF {
    if (source == target) {
        return rectF
    }
    //查找最小公倍父类
    val sourceParents = arrayListOf<ViewGroup>()
    val targetParents = arrayListOf<ViewGroup>()
    var sourceParent = (source as? ViewGroup) ?: source.parent as? ViewGroup
    var targetParent = (target as? ViewGroup) ?: target.parent as? ViewGroup
    outer@ while (sourceParent != null) {
        if (sourceParent != source) {
            sourceParents.add(sourceParent)
        }
        while (targetParent != null) {
            if (targetParent != target) {
                targetParents.add(targetParent)
            }
            if (sourceParent === targetParent) {
                // 找到共同父容器
                break@outer
            }
            targetParent = (targetParent.parent as? ViewGroup)
        }
        targetParents.clear()
        targetParent = (target as? ViewGroup) ?: (target.parent as? ViewGroup)
        sourceParent = sourceParent.parent as? ViewGroup
    }

    source.doChildReverseTransformParent(rectF)
    for (parent in sourceParents) {
        parent.doChildReverseTransformParent(rectF)
    }
    for (parent in targetParents) {
        parent.doParentTransformChild(rectF)
    }
    target.doParentTransformChild(rectF)
    return source.doSelfReverseTransform(rectF)
}

/**
 * 将[RectF]从[ViewGroup]坐标体系逆转成子[View]的坐标系
 */
fun View.doParentTransformChild(rectF: RectF): RectF {
    val parent = parent as? ViewGroup ?: return rectF
    val matrix = matrix
    matrix.reset()
    matrix.preScale(abs(parent.scaleX), abs(parent.scaleY), parent.left + parent.pivotX, parent.top + parent.pivotY)
    matrix.preTranslate(parent.left + parent.scrollX.toFloat(), parent.top + parent.scrollY.toFloat())
    matrix.mapRect(rectF)
    return rectF
}

/**
 * 将[RectF]从[View]坐标体系逆转成父容器[ViewGroup]的坐标系
 */
fun View.doChildReverseTransformParent(rectF: RectF): RectF {
    val parent = parent as? ViewGroup ?: return rectF
    val matrix = matrix
    matrix.reset()
    matrix.preTranslate(parent.scrollX - parent.left.toFloat(), parent.scrollY.toFloat() - parent.top)
    matrix.preScale(1 / abs(parent.scaleX), 1 / abs(parent.scaleY), parent.left + parent.pivotX, parent.top + parent.pivotY)
    matrix.mapRect(rectF)
    return rectF
}

/**
 * 以自身为坐标系将坐标转换进入，一般[ViewGroup]比较常用
 * 使用场景：需要将其他坐标系的坐标传入自身内部
 */
fun View.doSelfTransform(rectF: RectF): RectF {
    val matrix = matrix
    matrix.reset()
    matrix.preScale(abs(scaleX), abs(scaleY), left + pivotX, top + pivotY)
    matrix.preTranslate(scrollX.toFloat() + left, scrollY.toFloat() + top)
    matrix.mapRect(rectF)
    return rectF
}

/**
 * 以自身为坐标系将坐标系逆转出去，一般[ViewGroup]比较常用
 * 使用场景：将某个[View]的坐标逆转出去，不过[doChildReverseTransformParent]也可以实现
 * 这里可以用于以自身为坐标系，将任意构造的坐标逆转出去
 */
fun View.doSelfReverseTransform(rectF: RectF): RectF {
    val matrix = matrix
    matrix.reset()
    matrix.preTranslate(-(scrollX.toFloat() + left), -(scrollY.toFloat() + top))
    matrix.preScale(1 / abs(scaleX), 1 / abs(scaleY), left + pivotX, top + pivotY)
    matrix.mapRect(rectF)
    return rectF
}

/**
 * 获取[View]原始矩形，比如原本是300*300，缩放了0.5，那获取到的就是150*150
 * @param rectF 主动传可实现一定的性能优化，建议主动传入
 */
fun View?.getViewRawRectF(rectF: RectF = RectF()): RectF {
    this ?: return rectF
    rectF.set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
    if (!matrix.isIdentity) {
        val cacheMatrix = matrix
        cacheMatrix.reset()
        cacheMatrix.postScale(abs(scaleX), abs(scaleY), left + pivotX, top + pivotY)
        cacheMatrix.mapRect(rectF)
    }
    return rectF
}


/**
 * 将RectF转换成[View]专用的Rect
 * @param rect 主动传可实现一定的性能优化，建议主动传入
 */
fun RectF.toViewRect(rect: Rect = Rect()): Rect {
    rect.set(floor(left.toDouble()).toInt(), floor(top.toDouble()).toInt(),
        ceil(right.toDouble()).toInt(), ceil(bottom.toDouble()).toInt())
    return rect
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

/**
 * 将手势转换为子View的坐标系
 * @param view 需要转换的子View
 */
fun BestGestureDetector.transform(view: View): BestGestureDetector {
    val parent = view.parent as? ViewGroup ?: return this
    val offsetX = parent.scrollX - view.left
    val offsetY = parent.scrollY - view.top
    this.setAllEventOffsetLocation(offsetX.toFloat(), offsetY.toFloat())
    //如果矩阵被变换过，则变换触摸位置
    if (!view.matrix.isIdentity) {
        val invertMatrix = view.matrix
        invertMatrix.reset()
        view.matrix.invert(invertMatrix)
        this.transformAllEvent(invertMatrix)
    }
    return this
}

/**
 * 将手势转换为父View的坐标系
 * @param view 子View自己
 */
fun BestGestureDetector.reverseTransform(view: View): BestGestureDetector {
    val parent = view.parent as? ViewGroup ?: return this
    if (!view.matrix.isIdentity) {
        this.transformAllEvent(view.matrix)
    }
    this.setAllEventOffsetLocation(
        view.left - parent.scrollX.toFloat(),
        view.top - parent.scrollY.toFloat()
    )
    return this
}