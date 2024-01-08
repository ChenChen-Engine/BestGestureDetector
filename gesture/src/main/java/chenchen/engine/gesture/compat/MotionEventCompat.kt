package chenchen.engine.gesture.compat

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Build
import android.view.MotionEvent
import android.view.View
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * [MotionEvent]兼容类，目的：
 * 1. 为了解决低版本无法使用[MotionEvent.getRawX]
 * 2. 修复Android11.0及以下在[View]内部以任何方式获取[MotionEvent.getRawX]都会抖动
 * @author: chenchen
 * @since: 2023/4/14 10:30
 */
class MotionEventCompat(val event: MotionEvent, val fixRawEvent: MotionEvent?) {

    companion object {

        /**
         * 获取[getRawX]，[getRawY]相关的反射方法
         */
        private val getRawAxisValueMethod: Method by lazy {
            getNativeGetRawAxisValue().apply {
                isAccessible = true
            }
        }

        /**
         * 反射相关的字段
         */
        @delegate:SuppressLint("SoonBlockedPrivateApi")
        private val nativePtrField: Field by lazy {
            MotionEvent::class.java.getDeclaredField("mNativePtr").apply {
                isAccessible = true
            }
        }

        /**
         * 反射相关的字段
         */
        @delegate:SuppressLint("SoonBlockedPrivateApi")
        private val historyCurrentField: Field by lazy {
            MotionEvent::class.java.getDeclaredField("HISTORY_CURRENT").apply {
                isAccessible = true
            }
        }

        private fun getNativeGetRawAxisValue(): Method {
            for (method in MotionEvent::class.java.declaredMethods) {
                if (method.name.equals("nativeGetRawAxisValue")) {
                    method.isAccessible = true
                    return method
                }
            }
            throw RuntimeException("nativeGetRawAxisValue method not found.")
        }

        fun MotionEvent.compat(view: View? = null): MotionEventCompat {
            //530行 https://cs.android.com/android/platform/superproject/+/android-11.0.0_r1:frameworks/native/libs/input/Input.cpp
            //修复Android11.0调用MotionEvent.transform会对rawX，rawY进行变换，但rawX，rawY应该是分发到Window之后就不会再变化的
            //这个代码原本是针对11.0，但10.0及以下源码没对rawX，rawY进行变换(我没找到)，rawX，rawY的值也不正常
            //所以这段代码可以修复11.0及以下rawX，rawY值不正常的情况
            //但因无法预估未来的版本(12.0,13.0正常)会做出什么改动，这里不做版本区分，统一进行修复，这个改动百分百正确
            val fixRawEvent = MotionEvent.obtain(this)
            if (view?.matrix?.isIdentity == false) {
                fixRawEvent.transform(view.matrix)
            }
            return MotionEventCompat(
                MotionEvent.obtain(this),
                fixRawEvent?.let { MotionEvent.obtain(it) }
            )
        }

        fun MotionEventCompat.obtain(): MotionEventCompat {
            return MotionEventCompat(
                MotionEvent.obtain(this.event),
                this.fixRawEvent?.let { MotionEvent.obtain(it) }
            )
        }
    }

    val x: Float
        get() = event.x


    val y: Float
        get() = event.y


    val rawX: Float
        get() = event.rawX

    val rawY: Float
        get() = event.rawY

    val pointerCount: Int
        get() = event.pointerCount

    val action: Int
        get() = event.action

    val actionIndex: Int
        get() = event.actionIndex

    val actionMasked: Int
        get() = event.actionMasked

    fun getPointerId(pointerIndex: Int): Int {
        return event.getPointerId(pointerIndex)
    }

    fun findPointerIndex(pointerId: Int): Int {
        return event.findPointerIndex(pointerId)
    }

    fun getX(pointerIndex: Int): Float {
        return event.getX(pointerIndex)
    }

    fun getY(pointerIndex: Int): Float {
        return event.getY(pointerIndex)
    }

    fun getRawX(pointerIndex: Int): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fixRawEvent!!.getRawX(pointerIndex)
            } else {
                getRawAxisValueMethod.invoke(
                    null, nativePtrField.get(fixRawEvent),
                    MotionEvent.AXIS_X, pointerIndex,
                    historyCurrentField.get(null)
                ) as Float
            }
        } catch (_: Exception) {
            event.getX(pointerIndex)
        }
    }

    fun getRawY(pointerIndex: Int): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fixRawEvent!!.getRawY(pointerIndex)
            } else {
                getRawAxisValueMethod.invoke(
                    null, nativePtrField.get(fixRawEvent),
                    MotionEvent.AXIS_Y, pointerIndex,
                    historyCurrentField.get(null)
                ) as Float
            }
        } catch (_: Exception) {
            event.getY(pointerIndex)
        }
    }

    fun setLocation(x: Float, y: Float) {
        event.setLocation(x, y)
    }

    fun offsetLocation(deltaX: Float, deltaY: Float) {
        event.offsetLocation(deltaX, deltaY)
    }

    fun transform(matrix: Matrix) {
        event.transform(matrix)
    }

    fun recycle() {
        event.recycle()
        fixRawEvent?.recycle()
    }

    /**
     * 做一个简易的比较
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MotionEventCompat
        if (event.flags != other.event.flags) return false
        if (event.edgeFlags != other.event.edgeFlags) return false
        if (event.downTime != other.event.downTime) return false
        if (event.eventTime != other.event.eventTime) return false
        if (event.action != other.event.action) return false
        if (event.actionMasked != other.event.actionMasked) return false
        if (event.actionIndex != other.event.actionIndex) return false
        if (event.buttonState != other.event.buttonState) return false
        if (event.metaState != other.event.metaState) return false
        if (event.pointerCount != other.event.pointerCount) return false
        if (event.x != other.event.x) return false
        if (event.y != other.event.y) return false
        if (fixRawEvent?.rawX != other.fixRawEvent?.rawX) return false
        if (fixRawEvent?.rawY != other.fixRawEvent?.rawY) return false
        return true
    }
}

