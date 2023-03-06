package ru.studiq.mcashier.UI.Activities

import IDataProductDetailActivityListener
import ProviderDataProductDetail
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuCompat

import load
import ru.studiq.mcashier.R
import ru.studiq.mcashier.UI.Activities.security.RegisterActivity
import ru.studiq.mcashier.UI.Fragments.SalesActionFragment
import ru.studiq.mcashier.common.Common
import ru.studiq.mcashier.common.formatDouble
import ru.studiq.mcashier.interfaces.ICustomListActivityListener
import ru.studiq.mcashier.model.Settings
import ru.studiq.mcashier.model.classes.activities.common.CustomCompatActivity
import ru.studiq.mcashier.model.classes.hw.CustomHardwareError
import ru.studiq.mcashier.model.classes.hw.barcode.*
import ru.studiq.mcashier.model.classes.network.ProviderDataBody
import ru.studiq.mcashier.model.classes.network.providerclasses.IDataProductInfoActivityListener
import ru.studiq.mcashier.model.classes.network.providerclasses.ProviderDataDepartment
import ru.studiq.mcashier.model.classes.network.providerclasses.ProviderDataProductInfo
import ru.studiq.mcashier.model.classes.network.providerclasses.load
import java.io.Serializable
import java.text.DecimalFormat

class SalesActivity : CustomCompatActivity(), SalesActionFragment.SalesItemClickListener {
    private var allowManualInput: Boolean = Settings.Application.allowManualInputBarcode
    private var textTotal: TextView? = null
    private var textPLU: TextView? = null
    private var textBarcode: TextView? = null
    private var textArticle: TextView? = null
    private var textName: TextView? = null
    private var textDescription: TextView? = null
    private var textColor: TextView? = null
    private var textSize: TextView? = null
    private var textPrice: TextView? = null
    private var textSubTrademark: TextView? = null

    private var scanner: CustomBarcodeScanner? = null
    private var sales: List<ProviderDataProductDetail> = listOf()
    private var current: ProviderDataProductDetail? = null
    private var total: Double = 0.0
        get(){
            var value: Double = 0.0
            sales.forEach({
                value = value.plus(it.info?.price ?: 0.0)
            })
            return value.plus(current?.info?.price ?: 0.0)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun setupActivity() {
        super.setupActivity()
        setContentView(R.layout.activity_sales)
        supportActionBar?.title = getString(R.string.cap_sales).toUpperCase()
        supportActionBar?.subtitle = Settings.Application.currentDepartment?.caption
        textTotal = findViewById(R.id.sales_activity_text_total)
        textSubTrademark = findViewById(R.id.sales_activity_text_subtrademark)
        textPLU = findViewById(R.id.sales_activity_text_plu)
        textArticle = findViewById(R.id.sales_activity_text_article)
        textName = findViewById(R.id.sales_activity_text_name)
        textColor = findViewById(R.id.sales_activity_text_color)
        textSize = findViewById(R.id.sales_activity_text_size)
        textPrice = findViewById(R.id.sales_activity_text_price)
        textTotal?.text = ""

        this.scanner = when (Settings.Application.deviceType) {
//            1 -> GoogleBarcodeScanner(this, GoogleBarcodeScannerOptions(Settings.Application.allowManualInputBarcode))
            1 -> SimpleBarcodeScanner(this, SimpleBarcodeScannerOptions())
            2 -> HoneywellBarcodeScanner(this, HoneywellBarcodeScannerOptions())
            else -> null
        }
        this.isLockBackNavigation = true
        this.initialize()
    }

    override fun initialize() {
        super.initialize()
        textPLU?.text = ""
        textArticle?.text = ""
        textName?.text = ""
        textColor?.text = ""
        textSize?.text = ""
        textPrice?.text = ""
        textSubTrademark?.text = ""
    }
//    override fun onBackPressed() {
//        if (!isLockBackNavigation)
//            super.onBackPressed()
//    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sales, menu)
        if (menu != null) {
            MenuCompat.setGroupDividerEnabled(menu, true)
        }
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sales_department -> {
                this.handleMenuDepartmentClick()
                true
            }
            else -> false
        }
        return super.onOptionsItemSelected(item)
    }
    private fun handleMenuDepartmentClick() {
        runOnUiThread {
            Common.WaitDialog.show(this, false)
        }
        DepartmentListActivity.Companion.load(this, object : ICustomListActivityListener {
            override fun onSuccess(sender: Context?, code: Int, msg: String, data: Serializable?) {
                try {
                    super.onSuccess(sender, code, msg, data)
                    val intent = Intent(sender, DepartmentListActivity::class.java).apply {
                        putExtra(Settings.Activities.ParentActivity, RegisterActivity::class.java.name)
                        putExtra(Settings.Activities.ActivityCaption, getString(R.string.cap_departments))
                        putExtra(Settings.Activities.ListItems, (data as? ProviderDataBody))
                    }
                    startActivityForResult(intent, RegisterActivity.DEPARTMENTLIST_ACTIVITY_REQUEST_CODE)
                } finally {
                    Common.WaitDialog.dismiss()
                }
            }
            override fun onError(sender: Context?, code: Int, msg: String) {
                Common.WaitDialog.dismiss()
                (sender as? RegisterActivity)?.let {
                    runOnUiThread {
                        Common.AlertDialog.show(it, getString(R.string.cap_error), msg, true)
                    }
                }
                super.onError(sender, code, msg)
            }
        })
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("RESULT", "Code ${requestCode}")
        if (resultCode == RESULT_OK && data != null && data.getStringExtra(Settings.Extra.action) ?: "" == ProviderDataDepartment.Companion.codeSetAction) {
            val item: ProviderDataDepartment? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data.getSerializableExtra(Settings.Extra.UserObject, ProviderDataDepartment::class.java) as? ProviderDataDepartment
            } else {
                data.getSerializableExtra(Settings.Extra.UserObject) as? ProviderDataDepartment
            }
            Settings.Application.currentDepartment = item
            supportActionBar?.subtitle = Settings.Application.currentDepartment?.caption ?: ""

        } else {
            // не удалось получить результат
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("RESULT", "Code ${requestCode}")
    }

    fun onActionButtonClicked(view: View) {
        val addPhotoBottomDialogFragment = SalesActionFragment.newInstance()
        addPhotoBottomDialogFragment.show(
            supportFragmentManager,
            SalesActionFragment.TAG
        )
    }
    override fun onItemClick(item: String?) {
        Toast.makeText(this, "Selected action item is $item", Toast.LENGTH_LONG).show()
    }
    fun onToolsButtonClicked(view: View) {

    }
    // ***** SCANNER ***** //
    fun onScanButtonClicked(view: View) {
        val senderContext = this
        scanner?.scan (object : IBarcodeReadListener {
            override fun onBarcodeRead(sender: CustomBarcodeScanner, barcode: CustomBarcode?) {
                val params = barcode?.text ?: ""
                if (params.length > 0) {
                    Common.WaitDialog.show(senderContext, false)
                    ProviderDataProductDetail.load(senderContext, params, object : IDataProductDetailActivityListener {
                            override fun onSuccess(sender: Context?, code: Int, msg: String, data: ProviderDataProductDetail?) {
                                val obj = data?.let { detail ->
                                    ProviderDataProductInfo.load(sender, detail.barcode, object : IDataProductInfoActivityListener {
                                            override fun onSuccess(sender: Context?, code: Int, msg: String, data: ProviderDataProductInfo?) {
                                                super.onSuccess(sender, code, msg, data)
                                                val obj = data?.let { info ->
                                                    detail.info = info
                                                    onProductScan(detail)
                                                } ?: run {
                                                    onActivityError(
                                                        CustomHardwareError(-1, senderContext.getString(R.string.error_unassigned))
                                                    )
                                                }
                                                Common.WaitDialog.dismiss()
                                            }
                                            override fun onEmpty(sender: Context?) {
                                                super.onEmpty(sender)
                                                Common.WaitDialog.dismiss()
                                                onActivityError(CustomHardwareError(code, senderContext.getString(R.string.err_data_empty))
                                                )
                                            }
                                            override fun onError(sender: Context?, code: Int, msg: String) {
                                                Common.WaitDialog.dismiss()
                                                onActivityError(CustomHardwareError( code, msg ?: senderContext.getString(R.string.error_unassigned)))
                                            }
                                        })
                                } ?: run {
                                    Common.WaitDialog.dismiss()
                                    onActivityError(CustomHardwareError(-1, senderContext.getString(R.string.error_unassigned)))
                                }
                            }
                            override fun onError(sender: Context?, code: Int, msg: String) {
                                Common.WaitDialog.dismiss()
                                onActivityError(CustomHardwareError(code, msg ?: senderContext.getString(R.string.error_unassigned)))
                            }
                        })
                } else
                    Log.d("LOG", "ERROR")
            }
            override fun onCancel(sender: CustomBarcodeScanner) {
                Log.d("LOG", "CANCEL")
            }
            override fun onError(sender: CustomBarcodeScanner, error: CustomHardwareError?) {
                onActivityError(CustomHardwareError(error?.code ?: -1, error?.text ?: senderContext.getString(R.string.error_unassigned)))
            }
        })
    }
    private fun onProductScan(product: ProviderDataProductDetail) {
        runOnUiThread {
            this.current?.let {
                this.sales.plus(it)
            }
            this.current = product
            textTotal?.text = formatDouble(this.total)
            textSubTrademark?.text = product.info?.subTradeMarkName ?: ""
            textPLU?.text = product.PLU
            textArticle?.text = product.info?.article ?: ""
            textName?.text = product.caption
            textColor?.text = "${this.getString(R.string.cap_color)}: ${product.ColorID}"
            textSize?.text = "${this.getString(R.string.cap_size)}: ${product.SizeID}"
            textPrice?.text = product?.info?.let {  formatDouble(it.price) } ?: run { "" }
        }

    }
    private fun onActivityError(error: CustomHardwareError) {
        runOnUiThread {
            Common.AlertDialog.show(this, this.getString(R.string.cap_error), "${error.code.toString()} - ${error.text}")
        }
    }
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(KEY_ALLOW_MANUAL_INPUT, allowManualInput)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        allowManualInput = savedInstanceState.getBoolean(KEY_ALLOW_MANUAL_INPUT)
    }

    companion object {
        private const val KEY_ALLOW_MANUAL_INPUT = "allow_manual_input"
    }
    // ******************* //
}