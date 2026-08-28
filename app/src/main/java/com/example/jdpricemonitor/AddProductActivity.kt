package com.example.jdpricemonitor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jdpricemonitor.databinding.ActivityAddProductBinding
import com.example.jdpricemonitor.data.model.Product
import com.example.jdpricemonitor.repository.ProductRepository
import com.example.jdpricemonitor.service.PriceMonitoringService
import kotlinx.coroutines.launch
import java.util.*

class AddProductActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddProductBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupClickListeners()
        setupToolbar()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "添加商品"
    }
    
    private fun setupClickListeners() {
        binding.btnCheckPrice.setOnClickListener {
            checkProductPrice()
        }
        
        binding.btnSaveProduct.setOnClickListener {
            saveProduct()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        binding.btnTestNotification.setOnClickListener {
            testNotification()
        }
    }
    
    private fun checkProductPrice() {
        val productUrl = binding.etProductUrl.text.toString().trim()
        
        if (productUrl.isEmpty()) {
            Toast.makeText(this, "请输入商品链接", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                binding.btnCheckPrice.isEnabled = false
                binding.tvCheckStatus.text = "正在检查价格..."
                
                val monitoringService = PriceMonitoringService(this@AddProductActivity)
                val currentPrice = monitoringService.checkProductManually(productUrl)
                
                if (currentPrice != null) {
                    binding.etTargetPrice.setText(String.format("%.2f", currentPrice))
                    binding.tvCheckStatus.text = "当前价格: ${String.format("¥%.2f", currentPrice)}"
                    binding.tvCheckStatus.setTextColor(android.graphics.Color.GREEN)
                } else {
                    binding.tvCheckStatus.text = "无法获取价格信息"
                    binding.tvCheckStatus.setTextColor(android.graphics.Color.RED)
                }
            } catch (e: Exception) {
                binding.tvCheckStatus.text = "检查失败: ${e.message}"
                binding.tvCheckStatus.setTextColor(android.graphics.Color.RED)
            } finally {
                binding.btnCheckPrice.isEnabled = true
            }
        }
    }
    
    private fun saveProduct() {
        val productName = binding.etProductName.text.toString().trim()
        val productUrl = binding.etProductUrl.text.toString().trim()
        val targetPrice = binding.etTargetPrice.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val wechatNotify = binding.cbWechatNotify.isChecked
        
        // 验证输入
        if (productName.isEmpty()) {
            Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (productUrl.isEmpty()) {
            Toast.makeText(this, "请输入商品链接", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (targetPrice.isEmpty()) {
            Toast.makeText(this, "请输入目标价格", Toast.LENGTH_SHORT).show()
            return
        }
        
        val targetPriceValue = targetPrice.toDoubleOrNull()
        if (targetPriceValue == null || targetPriceValue <= 0) {
            Toast.makeText(this, "请输入有效的目标价格", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 创建商品对象
        val product = Product(
            id = UUID.randomUUID().toString(),
            name = productName,
            url = productUrl,
            currentPrice = 0.0, // 初始价格设为0，稍后会更新
            targetPrice = targetPriceValue,
            notifyEnabled = true,
            email = email,
            wechatNotify = wechatNotify
        )
        
        // 保存商品
        lifecycleScope.launch {
            try {
                val repository = ProductRepository.getInstance()
                repository.addProduct(product)
                
                Toast.makeText(this@AddProductActivity, "商品添加成功", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddProductActivity, "添加失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun testNotification() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "请输入邮箱地址", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 创建测试商品
        val testProduct = Product(
            id = "test_${UUID.randomUUID()}",
            name = "测试商品",
            url = "https://item.jd.com/100000000000.html",
            currentPrice = 100.0,
            targetPrice = 200.0,
            email = email,
            wechatNotify = false
        )
        
        // 发送测试通知
        lifecycleScope.launch {
            try {
                val notificationHelper = NotificationHelper(this@AddProductActivity)
                notificationHelper.sendEmailNotification(testProduct, 100.0)
                
                Toast.makeText(this@AddProductActivity, "测试通知已发送", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@AddProductActivity, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}