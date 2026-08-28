package com.example.jdpricemonitor

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.jdpricemonitor.database.ProductDatabase
import com.example.jdpricemonitor.repository.ProductRepository

class JDPriceMonitorApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化数据库
        val database = ProductDatabase.getDatabase(this)
        
        // 初始化仓库
        ProductRepository.initialize(database.productDao())
        
        // 配置WorkManager
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
        
        WorkManager.initialize(this, config)
    }
}