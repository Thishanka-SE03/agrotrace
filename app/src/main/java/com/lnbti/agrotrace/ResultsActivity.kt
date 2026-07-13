package com.lnbti.agrotrace

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
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

    private val repository = DocumentRepository()
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        container = findViewById(R.id.documentsContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnBack = findViewById(R.id.btnBack)

        btnRefresh.setOnClickListener { loadDocuments() }
        btnBack.setOnClickListener { finish() }

        loadDocuments()
    }

    private fun loadDocuments() {
        tvEmpty.visibility = View.GONE
        container.removeAllViews()

        lifecycleScope.launch {
            repository.getAllTempForm1Records().onSuccess { documents ->
                withContext(Dispatchers.Main) {
                    if (documents.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "No documents found.\n\nScan a Land Approval Form to see results here."
                    } else {
                        documents.forEach { doc ->
                            addDocumentCard(doc)
                        }
                    }
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "❌ Failed to load: ${e.message}"
                    Log.e("Results", "Load failed", e)
                }
            }
        }
    }

    private fun addDocumentCard(doc: TempForm1) {
        val inflater = LayoutInflater.from(this)
        val card = inflater.inflate(R.layout.item_document, container, false) as com.google.android.material.card.MaterialCardView

        card.findViewById<TextView>(R.id.tvFullName).text =
            "Land Approval Form"

        card.findViewById<TextView>(R.id.tvDate).text =
            "${doc.lot_no_for_seeds ?: "N/A"} - ${doc.form_date ?: "N/A"}"

        card.findViewById<TextView>(R.id.tvImageUrl).text = "Stored in Journal"

        // Share button
        card.findViewById<View>(R.id.btnShare).setOnClickListener {
            Toast.makeText(this, "Sharing feature coming soon", Toast.LENGTH_SHORT).show()
        }

        container.addView(card)
    }

    private fun confirmDelete(doc: DocumentRecord) {
        AlertDialog.Builder(this)
            .setTitle("Delete Document")
            .setMessage("Are you sure you want to delete this document?")
            .setPositiveButton("Delete") { _, _ -> deleteDocument(doc) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDocument(doc: DocumentRecord) {
        lifecycleScope.launch {
            repository.deleteDocument(doc.id!!).onSuccess {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultsActivity, "Deleted", Toast.LENGTH_SHORT).show()
                    loadDocuments()
                }
            }.onFailure { e ->
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultsActivity, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val DocumentRecord.dateOfBirthFormatted: String
        get() {
            val dob = date_of_birth.takeIf { it.isNotEmpty() } ?: return "DOB: N/A"
            val exp = expiry_date.takeIf { it.isNotEmpty() } ?: return "DOB: $dob"
            return "DOB: $dob  |  Exp: $exp"
        }
}