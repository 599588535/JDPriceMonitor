package com.example.jdpricemonitor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.jdpricemonitor.databinding.ActivityMainBinding
import com.example.jdpricemonitor.ui.adapter.ProductAdapter
import com.example.jdpricemonitor.ui.viewmodel.MainViewModel
import com.example.jdpricemonitor.work.PriceMonitoringWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // 启动价格监控任务
        PriceMonitoringWorker.rescheduleWork(this)
    }
    
    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            // 点击商品项的处理
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("PRODUCT_ID", product.id)
            }
            startActivity(intent)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = productAdapter
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.products.collectLatest { products ->
                productAdapter.submitList(products)
                binding.tvEmptyView.visibility = if (products.isEmpty()) android.view.VISIBLE else android.view.GONE
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) android.view.VISIBLE else android.view.GONE
        }
    }
    
    private fun setupClickListeners() {
        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java))
        }
        
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshProducts()
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menu_refresh -> {
                viewModel.refreshProducts()
                true
            }
            R.id.menu_stop_monitoring -> {
                PriceMonitoringWorker.cancelAllWork(this)
                true
            }
            R.id.menu_start_monitoring -> {
                PriceMonitoringWorker.rescheduleWork(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshProducts()
    }
}