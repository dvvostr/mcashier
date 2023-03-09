package ru.studiq.mcashier.UI.Activities

import ProviderDataProductDetail
import ProviderDataProductDetailItems
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import ru.studiq.mcashier.R
import ru.studiq.mcashier.common.formatDouble
import ru.studiq.mcashier.model.Settings
import ru.studiq.mcashier.model.classes.activities.common.CustomCompatActivity
import ru.studiq.mcashier.model.classes.adapters.CartListAdapter
import ru.studiq.mcashier.model.classes.adapters.CartSwipeToDeleteCallback
import ru.studiq.mcashier.model.classes.network.providerclasses.ProviderDataDepartment
import kotlin.math.abs
import kotlin.math.roundToInt

class CartActivity : CustomCompatActivity() {
    companion object {
        public const val CART_ACTIVITY_REQUEST_CODE = 121
    }

    private var textTotal: TextView? = null
    private var recyclerView: RecyclerView? = null
    private var items: ProviderDataProductDetailItems = ProviderDataProductDetailItems()

    private lateinit var swipeHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun setupActivity() {
        super.setupActivity()
        setContentView(R.layout.activity_cart)
        textTotal = findViewById(R.id.cart_activity_text_total)
        recyclerView = findViewById(R.id.cart_activity_recyclerview)
        items = Gson().fromJson(intent.getStringExtra(Settings.Activities.ListJSON), ProviderDataProductDetailItems::class.java)

        textTotal?.text = formatDouble(items.total)
        val adapter = items?.items?.let { CartListAdapter(this, it) }
        recyclerView?.adapter = adapter

        val swipeHandler = object : CartSwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = recyclerView?.adapter as CartListAdapter
                adapter.removeAt(viewHolder.adapterPosition)
                invalidate()
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

    }
    override fun onBackPressed() {
        intent.putExtra(Settings.Extra.CartObject, Gson().toJson(items))
        setResult(RESULT_OK, intent)
        super.onBackPressed()
    }
    override fun invalidate() {
        super.invalidate()
        textTotal?.text = formatDouble(items.total)
    }
    private val Int.dp
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            toFloat(), resources.displayMetrics
        ).roundToInt()
}
/*
            intent.putExtra(Settings.Extra.UserObject, user)
            setResult(RESULT_OK, intent)
 */