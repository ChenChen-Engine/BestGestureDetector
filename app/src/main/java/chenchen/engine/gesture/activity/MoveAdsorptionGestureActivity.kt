package chenchen.engine.gesture.activity

import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import chenchen.engine.gesture.coordinateMapToCoordinate
import chenchen.engine.gesture.demo.databinding.ActivityMoveAdsorptionGestureBinding
import chenchen.engine.gesture.getViewRectF

class MoveAdsorptionGestureActivity : AppCompatActivity() {

    private val childRotation = 45f
    private val childScale = 1.2f
    private val childMargin = 0
    private val groupMargin = 0
    private val frameMargin = 0
    private val binding by lazy { ActivityMoveAdsorptionGestureBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        init()
        binding.group.setOnClickListener {
            test()
        }
    }

    private fun init() = with(binding) {
        setRotation(child, childRotation)
        setRotation(child2, childRotation)
        setRotation(child3, childRotation)
        setScale(child, scale = childScale)
        setScale(child2, scale = childScale)
        setScale(child3, scale = childScale)
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

    private fun test() {
        val childRect = binding.child.getViewRectF()
        val childRect2 = binding.child2.getViewRectF()
        val childRect3 = binding.child3.getViewRectF()
        val groupRect = binding.group.getViewRectF()
        val childRectCopy = RectF(childRect)
        val childRectCopy2 = RectF(childRect2)
        val childRectCopy3 = RectF(childRect3)
        coordinateMapToCoordinate(binding.group, binding.child, childRectCopy)
        coordinateMapToCoordinate(binding.group, binding.child2, childRectCopy2)
        coordinateMapToCoordinate(binding.group, binding.child3, childRectCopy3)
        binding.group.setDrawChild(childRectCopy)
        binding.group.setDrawChild2(childRectCopy2)
        binding.group.setDrawChild3(childRectCopy3)
        Log.d("TAG", """
            test: 
            group:${groupRect}
            child1:${childRect}
            child2:${childRect2}
            child3:${childRect3}
        """.trimIndent())
    }
}