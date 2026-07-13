package com.lnbti.agrotrace.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.lnbti.agrotrace.DocumentTypeUtils
import com.lnbti.agrotrace.R
import com.lnbti.agrotrace.databinding.FragmentHomeBinding
import com.lnbti.agrotrace.db.AppDatabase
import com.lnbti.agrotrace.db.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardScanNew.setOnClickListener { navigateTo(R.id.nav_scan) }
        binding.btnViewAll.setOnClickListener { navigateTo(R.id.nav_history) }
        binding.btnNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun navigateTo(destination: Int) {
        runCatching {
            findNavController().navigate(destination) {
                popUpTo(findNavController().graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }.onFailure { error ->
            Log.e(TAG, "Navigation failed", error)
            if (_binding != null) {
                Snackbar.make(binding.root, "Could not open that screen", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun loadDashboard() {
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(appContext).documentDao().getAll()
                }
            }
            if (_binding == null) return@launch

            result.onSuccess { documents ->
                val startOfToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                binding.tvTotalDocuments.text = documents.size.toString()
                binding.tvTodayScans.text =
                    documents.count { it.timestamp >= startOfToday }.toString()
                binding.tvVerifiedDocuments.text = documents.size.toString()
                renderRecentDocuments(documents.take(3))
            }.onFailure { error ->
                Log.e(TAG, "Could not load dashboard history", error)
                binding.tvTotalDocuments.text = "—"
                binding.tvTodayScans.text = "—"
                binding.tvVerifiedDocuments.text = "—"
                renderRecentDocuments(emptyList())
                Snackbar.make(
                    binding.root,
                    "Local history is temporarily unavailable",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun renderRecentDocuments(documents: List<DocumentEntity>) {
        binding.recentDocumentsContainer.removeAllViews()
        binding.cardEmptyRecent.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE

        documents.forEach { document ->
            val item = layoutInflater.inflate(
                R.layout.item_recent_document,
                binding.recentDocumentsContainer,
                false
            )
            val icon = item.findViewById<ImageView>(R.id.ivDocumentIcon)
            icon.setImageResource(DocumentTypeUtils.icon(document.type))
            icon.imageTintList = ContextCompat.getColorStateList(
                requireContext(),
                DocumentTypeUtils.color(document.type)
            )
            item.findViewById<TextView>(R.id.tvDocumentType).text = document.title
            item.findViewById<TextView>(R.id.tvDocumentSummary).text = document.summary
            item.findViewById<TextView>(R.id.tvDocumentDate).text = formatDate(document.timestamp)
            item.setOnClickListener {
                val action = HomeFragmentDirections
                    .actionNavHomeToDocumentDetailFragment(document.id)
                findNavController().navigate(action)
            }
            binding.recentDocumentsContainer.addView(item)
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat(
            "dd MMM yyyy • hh:mm a",
            Locale.getDefault()
        ).format(Date(timestamp))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"
    }
}
