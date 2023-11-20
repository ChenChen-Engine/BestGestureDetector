package chenchen.engine.gesture.activity

import android.os.Bundle
import android.view.Choreographer
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import chenchen.engine.gesture.demo.R
import chenchen.engine.gesture.demo.databinding.ActivityScaleAdsorptionGestureBinding

class ScaleAdsorptionGestureActivity : AppCompatActivity() {

    private val binding by lazy { ActivityScaleAdsorptionGestureBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Choreographer.getInstance().postFrameCallback(::call)
    }

    fun call(it: Long) {
        binding.tvScale.text = "${(safeScale())}"
        Choreographer.getInstance().postFrameCallback(::call)
    }

    private fun safeScale(): Float {
        val scaleX = binding.gestureView.scaleX
        val scaleY = binding.gestureView.scaleY
        return java.lang.Float.max(scaleX, scaleY)
    }
}