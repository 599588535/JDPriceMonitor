package com.example.jdpricemonitor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jdpricemonitor.databinding.ActivityProductDetailBinding
import com.example.jdpricemonitor.data.model.Product
import com.example.jdpricemonitor.repository.ProductRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var product: Product
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        loadProduct()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "商品详情"
    }
    
    private fun loadProduct() {
        val productId = intent.getStringExtra("PRODUCT_ID")
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "无效的商品ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                val repository = ProductRepository.getInstance()
                product = repository.getProductById(productId) ?: throw Exception("商品不存在")
                
                displayProductInfo()
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "加载商品信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun displayProductInfo() {
        binding.apply {
            tvProductName.text = product.name
            tvProductUrl.text = product.url
            
            // 格式化价格显示
            tvCurrentPrice.text = product.getFormattedPrice()
            tvTargetPrice.text = product.getFormattedTargetPrice()
            
            // 价格下降百分比
            val dropPercentage = product.getPriceDropPercentage()
            if (dropPercentage > 0) {
                tvPriceDrop.text = "价格下降: ${String.format("%.1f%%", dropPercentage)}"
                tvPriceDrop.setTextColor(
                    if (dropPercentage > 10) android.graphics.Color.RED
                    else android.graphics.Color.parseColor("#FFA500")
                )
            } else {
                tvPriceDrop.text = "价格尚未下降"
                tvPriceDrop.setTextColor(android.graphics.Color.GRAY)
            }
            
            // 最后检查时间
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            tvLastChecked.text = dateFormat.format(product.lastChecked)
            
            // 通知设置
            tvEmail.text = if (product.email.isNotEmpty()) product.email else "未设置"
            cbWechatNotify.isChecked = product.wechatNotify
            cbNotifyEnabled.isChecked = product.notifyEnabled
            
            // 监控状态
            tvMonitorStatus.text = if (product.isActive) "监控中" else "已停止"
            tvMonitorStatus.setTextColor(
                if (product.isActive) android.graphics.Color.GREEN
                else android.graphics.Color.RED
            )
        }
    }
    
    private fun setupClickListeners() {
        binding.btnOpenInBrowser.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.url))
            startActivity(intent)
        }
        
        binding.btnCheckPriceNow.setOnClickListener {
            checkProductPriceNow()
        }
        
        binding.btnSaveChanges.setOnClickListener {
            saveChanges()
        }
        
        binding.btnDeleteProduct.setOnClickListener {
            deleteProduct()
        }
        
        binding.btnBackToMain.setOnClickListener {
            finish()
        }
    }
    
    private fun checkProductPriceNow() {
        lifecycleScope.launch {
            try {
                binding.btnCheckPriceNow.isEnabled = false
                binding.tvCheckStatus.text = "正在检查价格..."
                
                val monitoringService = PriceMonitoringService(this@ProductDetailActivity)
                val currentPrice = monitoringService.checkProductManually(product.url)
                
                if (currentPrice != null) {
                    // 更新商品价格
                    val repository = ProductRepository.getInstance()
                    repository.updateProductPrice(product.id, currentPrice)
                    
                    // 更新显示
                    product = repository.getProductById(product.id)!!
                    displayProductInfo()
                    
                    binding.tvCheckStatus.text = "价格已更新: ${String.format("¥%.2f", currentPrice)}"
                    binding.tvCheckStatus.setTextColor(android.graphics.Color.GREEN)
                } else {
                    binding.tvCheckStatus.text = "无法获取价格信息"
                    binding.tvCheckStatus.setTextColor(android.graphics.Color.RED)
                }
            } catch (e: Exception) {
                binding.tvCheckStatus.text = "检查失败: ${e.message}"
                binding.tvCheckStatus.setTextColor(android.graphics.Color.RED)
            } finally {
                binding.btnCheckPriceNow.isEnabled = true
            }
        }
    }
    
    private fun saveChanges() {
        val email = binding.etEmail.text.toString().trim()
        val wechatNotify = binding.cbWechatNotify.isChecked
        val notifyEnabled = binding.cbNotifyEnabled.isChecked
        
        lifecycleScope.launch {
            try {
                val updatedProduct = product.copy(
                    email = email,
                    wechatNotify = wechatNotify,
                    notifyEnabled = notifyEnabled
                )
                
                val repository = ProductRepository.getInstance()
                repository.updateProduct(updatedProduct)
                
                product = updatedProduct
                displayProductInfo()
                
                Toast.makeText(this@ProductDetailActivity, "保存成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun deleteProduct() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除这个商品吗？删除后将无法恢复。")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val repository = ProductRepository.getInstance()
                        repository.deleteProduct(product)
                        
                        Toast.makeText(this@ProductDetailActivity, "删除成功", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@ProductDetailActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}