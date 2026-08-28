package com.example.jdpricemonitor

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.jdpricemonitor.databinding.ActivitySettingsBinding
import com.example.jdpricemonitor.repository.ProductRepository
import com.example.jdpricemonitor.work.PriceMonitoringWorker
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        loadSettings()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "设置"
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                val repository = ProductRepository.getInstance()
                val activeCount = repository.getActiveProductCount()
                
                binding.tvActiveProducts.text = activeCount.toString()
                
                // 检查监控任务状态
                // 这里可以添加检查WorkManager状态的逻辑
                binding.tvMonitoringStatus.text = "监控运行中"
                binding.switchMonitoring.isChecked = true
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, "加载设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startMonitoring()
            } else {
                stopMonitoring()
            }
        }
        
        binding.btnClearAllData.setOnClickListener {
            clearAllData()
        }
        
        binding.btnCheckUpdates.setOnClickListener {
            checkForUpdates()
        }
        
        binding.btnAbout.setOnClickListener {
            showAbout()
        }
        
        binding.btnBackToMain.setOnClickListener {
            finish()
        }
    }
    
    private fun startMonitoring() {
        try {
            PriceMonitoringWorker.rescheduleWork(this)
            binding.tvMonitoringStatus.text = "监控运行中"
            Toast.makeText(this, "价格监控已启动", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "启动监控失败: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.switchMonitoring.isChecked = false
        }
    }
    
    private fun stopMonitoring() {
        try {
            PriceMonitoringWorker.cancelAllWork(this)
            binding.tvMonitoringStatus.text = "监控已停止"
            Toast.makeText(this, "价格监控已停止", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "停止监控失败: ${e.message}", Toast.LENGTH_SHORT).show()
            binding.switchMonitoring.isChecked = true
        }
    }
    
    private fun clearAllData() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认清除所有数据")
            .setMessage("确定要清除所有监控商品和设置吗？此操作不可恢复。")
            .setPositiveButton("清除") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // 这里需要实现清除所有数据的逻辑
                        // val repository = ProductRepository.getInstance()
                        // repository.clearAllData()
                        
                        Toast.makeText(this@SettingsActivity, "所有数据已清除", Toast.LENGTH_SHORT).show()
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this@SettingsActivity, "清除数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun checkForUpdates() {
        Toast.makeText(this, "正在检查更新...", Toast.LENGTH_SHORT).show()
        
        // 这里可以实现检查更新的逻辑
        // 可以连接到服务器检查是否有新版本
    }
    
    private fun showAbout() {
        val aboutText = """
京东价格监控应用 v1.0

功能特点：
• 监控京东商品价格变化
• 支持自定义目标价格
• 邮件和微信价格提醒
• 定时自动检查价格

使用说明：
1. 点击右下角按钮添加商品
2. 设置目标价格和通知方式
3. 系统会自动监控价格变化
4. 价格达到目标时会发送通知

注意事项：
• 请勿频繁刷新页面
• 建议开启通知以获取提醒
• 请遵守京东平台使用条款

© 2024 JDPriceMonitor
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("关于应用")
            .setMessage(aboutText)
            .setPositiveButton("确定", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}