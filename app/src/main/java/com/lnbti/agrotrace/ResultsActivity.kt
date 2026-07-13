package com.lnbti.agrotrace

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultsActivity : AppCompatActivity() {

    companion object {
        fun start(context: AppCompatActivity) {
            context.startActivity(
                android.content.Intent(context, ResultsActivity::class.java)
            )
        }
    }

    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var btnBack: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.documentsContainer)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        container = findViewById(R.id.documentsContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }
        
        findViewById<View>(R.id.btnCalendar).setOnClickListener {
            Toast.makeText(this, "Filter by date coming soon", Toast.LENGTH_SHORT).show()
        }

        loadHistory()
    }

    private fun loadHistory() {
        tvEmpty.visibility = View.GONE
        container.removeAllViews()

        lifecycleScope.launch {
            val docs = AppDatabase.getDatabase(this@ResultsActivity).documentDao().getAll()
            withContext(Dispatchers.Main) {
                if (docs.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    docs.forEach { doc ->
                        addDocumentCard(doc)
                    }
                }
            }
        }
    }

    private fun addDocumentCard(doc: DocumentEntity) {
        val inflater = LayoutInflater.from(this)
        val card = inflater.inflate(R.layout.item_document, container, false) as com.google.android.material.card.MaterialCardView

        card.findViewById<TextView>(R.id.tvFullName).text = doc.title
        card.findViewById<TextView>(R.id.tvDate).text = doc.summary
        card.findViewById<TextView>(R.id.tvImageUrl).text = "Stored in Journal"

        card.findViewById<View>(R.id.btnShare).setOnClickListener {
            Toast.makeText(this, "Sharing: ${doc.title}", Toast.LENGTH_SHORT).show()
        }
        
        card.setOnClickListener {
            // Future: Show full detail view
            Toast.makeText(this, "View details coming soon", Toast.LENGTH_SHORT).show()
        }

        container.addView(card)
    }
}
