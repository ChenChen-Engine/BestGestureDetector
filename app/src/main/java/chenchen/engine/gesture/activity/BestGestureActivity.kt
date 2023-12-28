package chenchen.engine.gesture.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import chenchen.engine.gesture.demo.databinding.ActivityBestGestureBinding

class BestGestureActivity : AppCompatActivity() {

    private val binding by lazy { ActivityBestGestureBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}