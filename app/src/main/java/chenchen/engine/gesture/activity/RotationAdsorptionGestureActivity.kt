package chenchen.engine.gesture.activity

import android.os.Bundle
import android.view.Choreographer
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import chenchen.engine.gesture.demo.R
import chenchen.engine.gesture.demo.databinding.ActivityRotationAdsorptionGestureBinding

class RotationAdsorptionGestureActivity : AppCompatActivity() {

    private val binding by lazy { ActivityRotationAdsorptionGestureBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Choreographer.getInstance().postFrameCallback(::call)
    }

    fun call(it: Long) {
        binding.tvRotation.text = "${(binding.gestureView.rotation.toInt() % 360 + 360) % 360}°"
        Choreographer.getInstance().postFrameCallback(::call)
    }
}