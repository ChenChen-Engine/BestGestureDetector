package chenchen.engine.gesture.activity

import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import chenchen.engine.gesture.demo.databinding.ActivityMoveAdsorptionGestureBinding
import chenchen.engine.gesture.doSelfReverseTransformRectF
import chenchen.engine.gesture.getAbsoluteLocation
import chenchen.engine.gesture.getCommonParent
import chenchen.engine.gesture.getViewRectF
import chenchen.engine.gesture.toViewRect
import kotlin.math.abs
import kotlin.math.roundToInt

class MoveAdsorptionGestureActivity : AppCompatActivity() {

    private val childRotation = 0f
    private val childScale = 1.2f
    private val childMargin = 0
    private val groupMargin = 100
    private val groupScale = 0.8f
    private val frameMargin = 30
    private val binding by lazy { ActivityMoveAdsorptionGestureBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
//        transformInit()
        binding.group.setOnClickListener {
            startTest()
        }
    }

    /**
     * 测试所有View都经过变换，变换和不变换的难度是不一样的
     */
    private fun transformInit() = with(binding) {
        setRotation(child, childRotation)
        setRotation(child2, childRotation)
        setRotation(child3, childRotation)
        setScale(child, scale = childScale)
        setScale(child2, scale = childScale)
        setScale(child3, scale = childScale)
        setScale(group, scale = groupScale)
        setMargin(child, childMargin)
        setMargin(child2, childMargin)
        setMargin(child3, childMargin)
        setMargin(group, groupMargin)
        setMargin(frame, frameMargin)
    }

    private fun setRotation(view: View, rotation: Float) {
        view.rotation = rotation
    }

    private fun setScale(view: View, scale: Float) {
        view.scaleX = scale
        view.scaleY = scale
    }

    private fun setMargin(view: View, margin: Int) {
        view.updateLayoutParams<MarginLayoutParams> {
            leftMargin = margin
            topMargin = margin
            rightMargin = margin
            bottomMargin = margin
        }
    }

    private fun startTest() {
        mapToGroup(binding.child)
        mapToGroup(binding.child2)
        mapToGroup(binding.child3)
        mapToGroup(binding.child4)
        mapToGroup(binding.child5)
        binding.group.postDelayed({
            //测试偏移到指定的对齐方式
            offset(binding.child)
            offset(binding.child2)
            offset(binding.child3)
            offset(binding.child4)
            offset(binding.child5)
            mapToGroup(binding.child)
            mapToGroup(binding.child2)
            mapToGroup(binding.child3)
            mapToGroup(binding.child4)
            mapToGroup(binding.child5)
        }, 1000)
    }

    //将View1:1的绘制到ViewGroup中
    private fun mapToGroup(child: View) {
        val childRectF = child.getViewRectF()
        val groupRectF = binding.group.getViewRectF()
        child.getAbsoluteLocation(binding.group, childRectF)
        binding.group.setDrawChild(RectF(childRectF).apply { offset(-groupRectF.left, -groupRectF.top) })
    }

    private fun offset(child: View) {
        val childRectF = child.getViewRectF()
        val groupRectF = binding.group.getViewRectF()
        child.getAbsoluteLocation(binding.group, childRectF)
        binding.group.getAbsoluteLocation(child, groupRectF)

        val offset = groupRectF.centerX() - childRectF.toViewRect().centerX()
        Log.d("TAG", "test: pre ${offset}")
        val tempRectF = RectF()
        tempRectF.set(0f, 0f, abs(offset), 0f)
        val parents = child.getCommonParent(binding.group)
        parents.removeLastOrNull()
        for (parent in parents) {
            parent.doSelfReverseTransformRectF(tempRectF)
        }
        if (offset < 0) {
            child.offsetLeftAndRight(-tempRectF.width().roundToInt())
            Log.d("TAG", "test: post ${-tempRectF.width()}")
        } else {
            child.offsetLeftAndRight(tempRectF.width().roundToInt())
            Log.d("TAG", "test: post ${tempRectF.width()}")
        }
    }
}