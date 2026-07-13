package com.lnbti.agrotrace

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnForm1: Button
    private lateinit var btnForm2: Button
    private lateinit var btnForm3: Button
    private lateinit var btnForm4: Button
    private lateinit var btnForm5: Button
    private lateinit var btnForm6: Button
    private lateinit var btnForm7: Button
    private lateinit var btnHistory: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        btnForm1 = findViewById(R.id.btnForm1)
        btnForm2 = findViewById(R.id.btnForm2)
        btnForm3 = findViewById(R.id.btnForm3)
        btnForm4 = findViewById(R.id.btnForm4)
        btnForm5 = findViewById(R.id.btnForm5)
        btnForm6 = findViewById(R.id.btnForm6)
        btnForm7 = findViewById(R.id.btnForm7)
        btnHistory = findViewById(R.id.btnHistory)

        btnForm1.setOnClickListener { startScanner(1) }
        btnForm2.setOnClickListener { startScanner(2) }
        btnForm3.setOnClickListener { startScanner(3) }
        btnForm4.setOnClickListener { startScanner(4) }
        btnForm5.setOnClickListener { startScanner(5) }
        btnForm6.setOnClickListener { startScanner(6) }
        btnForm7.setOnClickListener { startScanner(7) }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, ResultsActivity::class.java))
        }
    }

    private fun startScanner(docType: Int) {
        val intent = Intent(this, ScannerActivity::class.java)
        intent.putExtra("DOC_TYPE", docType)
        startActivity(intent)
    }
}
