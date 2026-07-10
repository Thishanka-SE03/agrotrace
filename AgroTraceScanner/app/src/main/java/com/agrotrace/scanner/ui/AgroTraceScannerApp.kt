package com.agrotrace.scanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agrotrace.scanner.ui.screens.CaptureScreen
import com.agrotrace.scanner.ui.screens.ClaimingScreen
import com.agrotrace.scanner.ui.screens.HomeScreen
import com.agrotrace.scanner.ui.screens.PairingScreen
import com.agrotrace.scanner.ui.screens.PreviewScreen
import com.agrotrace.scanner.ui.screens.SettingsScreen
import com.agrotrace.scanner.ui.screens.SuccessScreen
import com.agrotrace.scanner.ui.screens.UploadingScreen

@Composable
fun AgroTraceScannerApp(
    viewModel: ScannerViewModel,
    onScanQr: () -> Unit,
    onScanDocument: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.step != ScannerStep.HOME) {
        viewModel.goBack()
    }

    when (state.step) {
        ScannerStep.HOME -> HomeScreen(
            state = state,
            onScanQr = onScanQr,
            onManualPayloadChanged = viewModel::updateManualPayload,
            onSubmitManualPayload = viewModel::submitManualPayload,
            onOpenSettings = viewModel::openSettings,
            onDismissError = viewModel::dismissError
        )
        ScannerStep.PAIRING -> PairingScreen(
            state = state,
            onBack = viewModel::goBack,
            onClaim = viewModel::claimScan,
            onDismissError = viewModel::dismissError
        )
        ScannerStep.CLAIMING -> ClaimingScreen(state.pairing)
        ScannerStep.CAPTURE -> CaptureScreen(
            pairing = state.pairing,
            errorMessage = state.errorMessage,
            onBack = viewModel::goBack,
            onScanDocument = onScanDocument,
            onDismissError = viewModel::dismissError
        )
        ScannerStep.PREVIEW -> PreviewScreen(
            state = state,
            onBack = viewModel::goBack,
            onRetake = viewModel::retakeDocument,
            onUpload = viewModel::uploadDocument,
            onDismissError = viewModel::dismissError
        )
        ScannerStep.UPLOADING -> UploadingScreen(state)
        ScannerStep.SUCCESS -> SuccessScreen(
            pairing = state.pairing,
            onDone = viewModel::goHome
        )
        ScannerStep.SETTINGS -> SettingsScreen(
            state = state,
            onBack = viewModel::goBack,
            onUrlChanged = viewModel::updateSettingsUrl,
            onSave = viewModel::saveSettings,
            onDismissError = viewModel::dismissError
        )
    }
}
