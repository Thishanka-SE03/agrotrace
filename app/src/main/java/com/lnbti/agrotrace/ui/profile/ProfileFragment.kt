package com.lnbti.agrotrace.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lnbti.agrotrace.databinding.FragmentProfileBinding
import com.lnbti.agrotrace.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnEditProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Officer profile editing will be added with authentication", Toast.LENGTH_SHORT).show()
        }
        binding.btnSync.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supabase sync")
                .setMessage("Verified documents are currently stored in Room. This workspace is ready for future mapping to the seven Supabase tables.")
                .setPositiveButton("OK", null)
                .show()
        }
        binding.btnSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Settings are ready for a future release", Toast.LENGTH_SHORT).show()
        }
        binding.btnHelp.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Better scanning results")
                .setMessage("Keep the page flat, avoid shadows, include every edge, and review highlighted blank values before saving.")
                .setPositiveButton("Got it", null)
                .show()
        }
        binding.btnLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Authentication is not connected yet", Toast.LENGTH_SHORT).show()
        }
        binding.tvAppVersion.text = "AgroTrace ${appVersion()}"
    }

    override fun onResume() {
        super.onResume()
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(appContext).documentDao().getAll().size
                }
            }
            if (_binding == null) return@launch
            result.onSuccess { count ->
                binding.tvDocumentCount.text = count.toString()
            }.onFailure { error ->
                Log.e(TAG, "Could not load profile statistics", error)
                binding.tvDocumentCount.text = "—"
            }
        }
    }

    private fun appVersion(): String = runCatching {
        requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
    }.getOrNull() ?: "1.0"

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ProfileFragment"
    }
}
