package com.lnbti.agrotrace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnForm1: Button
    private lateinit var btnHistory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        btnForm1 = findViewById(R.id.btnForm1)
        btnHistory = findViewById(R.id.btnHistory)

        btnForm1.setOnClickListener {
            startActivity(Intent(this, ScannerActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, ResultsActivity::class.java))
        }

        // Check Supabase connection on startup
        checkConnection()
    }

    private fun checkConnection() {
        if (!Supabase.isInitialized) {
            Toast.makeText(this, "⚠️ Supabase not initialized — check local.properties", Toast.LENGTH_LONG).show()
        }
    }
}