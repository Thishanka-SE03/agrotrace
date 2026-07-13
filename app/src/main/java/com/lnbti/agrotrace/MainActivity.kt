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
    private lateinit var btnForm2: Button
    private lateinit var btnForm3: Button
    private lateinit var btnForm4: Button
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
        btnForm2 = findViewById(R.id.btnForm2)
        btnForm3 = findViewById(R.id.btnForm3)
        btnForm4 = findViewById(R.id.btnForm4)
        btnHistory = findViewById(R.id.btnHistory)

        findViewById<Button>(R.id.btnNewScan).setOnClickListener {
            // Optional: Show a hint to select form below
            Toast.makeText(this, "Please select a document type below", Toast.LENGTH_SHORT).show()
        }

        btnForm1.setOnClickListener {
            startScanner(1)
        }

        btnForm2.setOnClickListener {
            startScanner(2)
        }

        btnForm3.setOnClickListener {
            startScanner(3)
        }

        btnForm4.setOnClickListener {
            startScanner(4)
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, ResultsActivity::class.java))
        }

        // Check Supabase connection on startup
        checkConnection()
    }

    private fun startScanner(docType: Int) {
        val intent = Intent(this, ScannerActivity::class.java)
        intent.putExtra("DOC_TYPE", docType)
        startActivity(intent)
    }

    private fun checkConnection() {
        if (!Supabase.isInitialized) {
            Toast.makeText(this, "⚠️ Supabase not initialized — check local.properties", Toast.LENGTH_LONG).show()
        }
    }
}