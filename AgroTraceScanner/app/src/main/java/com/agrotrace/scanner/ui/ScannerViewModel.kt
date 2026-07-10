package com.agrotrace.scanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.agrotrace.scanner.data.preferences.ScannerPreferences
import com.agrotrace.scanner.data.repository.AppResult
import com.agrotrace.scanner.data.repository.OcrScanRepository
import com.agrotrace.scanner.domain.model.CapturedDocument
import com.agrotrace.scanner.domain.parser.BaseUrlValidator
import com.agrotrace.scanner.domain.parser.PairingPayloadParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val repository: OcrScanRepository,
    private val preferences: ScannerPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        preferences.baseUrlFlow
            .onEach { url ->
                _uiState.update { state ->
                    state.copy(
                        baseUrl = url,
                        settingsUrlInput = if (state.settingsUrlInput.isBlank()) url else state.settingsUrlInput
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun acceptQrPayload(rawPayload: String) {
        PairingPayloadParser.parse(rawPayload)
            .onSuccess { pairing ->
                _uiState.update {
                    it.copy(
                        step = ScannerStep.PAIRING,
                        pairing = pairing,
                        isClaimed = false,
                        document = null,
                        manualPayload = "",
                        errorMessage = null,
                        uploadProgress = 0f
                    )
                }
            }
            .onFailure { error -> showError(error.message ?: "Invalid AgroTrace QR code.") }
    }

    fun updateManualPayload(value: String) {
        _uiState.update { it.copy(manualPayload = value, errorMessage = null) }
    }

    fun submitManualPayload() {
        acceptQrPayload(_uiState.value.manualPayload)
    }

    fun claimScan() {
        val current = _uiState.value
        val pairing = current.pairing ?: return
        if (current.isClaimed) {
            _uiState.update { it.copy(step = ScannerStep.CAPTURE, errorMessage = null) }
            return
        }
        _uiState.update { it.copy(step = ScannerStep.CLAIMING, errorMessage = null) }

        viewModelScope.launch {
            when (val result = repository.claim(pairing)) {
                is AppResult.Success -> _uiState.update { it.copy(step = ScannerStep.CAPTURE, isClaimed = true) }
                is AppResult.Failure -> _uiState.update {
                    it.copy(step = ScannerStep.PAIRING, errorMessage = result.message)
                }
            }
        }
    }

    fun onDocumentCaptured(document: CapturedDocument) {
        _uiState.update {
            it.copy(step = ScannerStep.PREVIEW, document = document, errorMessage = null)
        }
    }

    fun uploadDocument() {
        val state = _uiState.value
        val pairing = state.pairing ?: return
        val document = state.document ?: return

        _uiState.update {
            it.copy(step = ScannerStep.UPLOADING, uploadProgress = 0f, errorMessage = null)
        }

        viewModelScope.launch {
            when (val result = repository.upload(pairing, document) { progress ->
                _uiState.update { it.copy(uploadProgress = progress) }
            }) {
                is AppResult.Success -> _uiState.update {
                    it.copy(step = ScannerStep.SUCCESS, uploadProgress = 1f, document = null)
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(step = ScannerStep.PREVIEW, errorMessage = result.message)
                }
            }
        }
    }

    fun retakeDocument() {
        _uiState.update { it.copy(step = ScannerStep.CAPTURE, document = null, errorMessage = null) }
    }

    fun openSettings() {
        _uiState.update {
            it.copy(step = ScannerStep.SETTINGS, settingsUrlInput = it.baseUrl, errorMessage = null)
        }
    }

    fun updateSettingsUrl(value: String) {
        _uiState.update { it.copy(settingsUrlInput = value, errorMessage = null) }
    }

    fun saveSettings() {
        BaseUrlValidator.normalize(_uiState.value.settingsUrlInput)
            .onSuccess { normalized ->
                viewModelScope.launch {
                    preferences.setBaseUrl(normalized)
                    _uiState.update {
                        it.copy(
                            step = ScannerStep.HOME,
                            baseUrl = normalized,
                            settingsUrlInput = normalized,
                            errorMessage = null
                        )
                    }
                }
            }
            .onFailure { showError(it.message ?: "Invalid backend URL.") }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun goHome() {
        _uiState.update {
            ScannerUiState(
                step = ScannerStep.HOME,
                baseUrl = it.baseUrl,
                settingsUrlInput = it.baseUrl
            )
        }
    }

    fun goBack() {
        _uiState.update { state ->
            when (state.step) {
                ScannerStep.PAIRING, ScannerStep.SETTINGS -> state.copy(
                    step = ScannerStep.HOME,
                    pairing = null,
                    isClaimed = false,
                    document = null,
                    errorMessage = null
                )
                ScannerStep.CAPTURE -> state.copy(
                    step = ScannerStep.PAIRING,
                    errorMessage = null
                )
                ScannerStep.PREVIEW -> state.copy(
                    step = ScannerStep.CAPTURE,
                    document = null,
                    errorMessage = null
                )
                ScannerStep.SUCCESS -> ScannerUiState(
                    step = ScannerStep.HOME,
                    baseUrl = state.baseUrl,
                    settingsUrlInput = state.baseUrl
                )
                ScannerStep.CLAIMING, ScannerStep.UPLOADING -> state
                ScannerStep.HOME -> state
            }
        }
    }
}

class ScannerViewModelFactory(
    private val repository: OcrScanRepository,
    private val preferences: ScannerPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ScannerViewModel::class.java))
        return ScannerViewModel(repository, preferences) as T
    }
}
