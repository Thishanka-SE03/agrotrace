package com.lnbti.agrotrace.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExtractionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DocumentRepository(application)

    val isExtracting = MutableLiveData(false)
    val extractedFields = MutableLiveData<Map<String, String>>(emptyMap())

    fun insertDocument(document: ScannedDocument): Long {
        var result = 0L
        viewModelScope.launch(Dispatchers.IO) {
            result = repository.insertDocument(document)
        }
        return result
    }
}