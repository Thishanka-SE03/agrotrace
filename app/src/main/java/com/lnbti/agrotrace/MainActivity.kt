package com.lnbti.agrotrace

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        findViewById<View>(R.id.btnMenu).setOnClickListener {
            drawerLayout.open()
        }

        findViewById<View>(R.id.cardScanNew).setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
            .setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_history -> {
                        startActivity(Intent(this, ResultsActivity::class.java))
                        true
                    }
                    R.id.nav_scan -> {
                        startActivity(Intent(this, ScannerActivity::class.java))
                        true
                    }
                    else -> true
                }
            }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawerLayout)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun startScanner(docType: Int) {
        val intent = Intent(this, ScannerActivity::class.java)
        intent.putExtra("DOC_TYPE", docType)
        startActivity(intent)
    }

    fun onScanNewDocumentClick(view: View) {
        startScanner(0) // Default or selection
    }

    fun onDocumentTypeClick(view: View) {
        val intent = Intent(this, ScannerActivity::class.java)
        // You could pass specific doc type here based on view ID
        startActivity(intent)
    }
}
