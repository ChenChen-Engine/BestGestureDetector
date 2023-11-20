package chenchen.engine.gesture

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import chenchen.engine.gesture.activity.MoveAdsorptionGestureActivity
import chenchen.engine.gesture.activity.BestGestureActivity
import chenchen.engine.gesture.activity.RotationAdsorptionGestureActivity
import chenchen.engine.gesture.activity.ScaleAdsorptionGestureActivity
import chenchen.engine.gesture.demo.databinding.ActivityMainBinding
import chenchen.engine.gesture.demo.R

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val items = arrayListOf(
        Item("基础手势", BestGestureActivity::class.java),
        Item("移动吸附手势", MoveAdsorptionGestureActivity::class.java),
        Item("旋转手势", RotationAdsorptionGestureActivity::class.java),
        Item("缩放手势", ScaleAdsorptionGestureActivity::class.java),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        initRv()
    }

    private fun initRv() {
        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = object : RecyclerView.Adapter<ViewHolder>() {

            override fun getItemCount() = items.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                return ItemViewHolder(layoutInflater.inflate(R.layout.item_gesture, parent, false))
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                (holder.itemView as Button).text = items[position].title
                holder.itemView.setOnClickListener {
                    startActivity(Intent(this@MainActivity, items[position].clazz))
                }
            }
        }
    }

    class ItemViewHolder(view: View) : ViewHolder(view)
    data class Item(val title: String, val clazz: Class<*>)
}