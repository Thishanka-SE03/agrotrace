package com.agrotrace.scanner.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.agrotrace.scanner.domain.model.PairingPayload
import com.agrotrace.scanner.ui.ScannerUiState
import com.agrotrace.scanner.ui.components.AgroTraceHeader
import com.agrotrace.scanner.ui.components.ErrorBanner
import com.agrotrace.scanner.ui.components.InfoCard
import com.agrotrace.scanner.ui.components.formatBytes
import com.agrotrace.scanner.ui.theme.AgroGold
import com.agrotrace.scanner.ui.theme.AgroGreen

@Composable
fun HomeScreen(
    state: ScannerUiState,
    onScanQr: () -> Unit,
    onManualPayloadChanged: (String) -> Unit,
    onSubmitManualPayload: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissError: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AgroTraceHeader(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Backend settings")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ErrorBanner(state.errorMessage, onDismissError)

            Card(
                colors = CardDefaults.cardColors(containerColor = AgroGreen),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = AgroGold,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        "Scan a desktop request",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "Open a scan request in the AgroTrace desktop application, then scan the displayed QR code.",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
                    )
                    Button(
                        onClick = onScanQr,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgroGold,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan desktop QR")
                    }
                }
            }

            InfoCard(
                title = "How it works",
                body = "1. Scan and claim the desktop request.\n2. Photograph the paper form.\n3. Review and upload the image.\n4. Continue verification on the desktop."
            )

            Text("Manual fallback", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = state.manualPayload,
                onValueChange = onManualPayloadChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Paste the complete QR payload") },
                placeholder = { Text("agrotrace://ocr/claim?scanId=...") },
                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                minLines = 2,
                maxLines = 4
            )
            OutlinedButton(
                onClick = onSubmitManualPayload,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.manualPayload.isNotBlank()
            ) {
                Text("Use pasted payload")
            }

            Text(
                text = "Server: ${state.baseUrl}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    state: ScannerUiState,
    onBack: () -> Unit,
    onClaim: () -> Unit,
    onDismissError: () -> Unit
) {
    val pairing = state.pairing ?: return
    AppScaffold(title = "Confirm pairing", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ErrorBanner(state.errorMessage, onDismissError)
            ScanReferenceCard(pairing)
            InfoCard(
                title = "Seed Farm Certification Application",
                body = "The backend currently identifies this form internally as LAND_APPROVAL. The mobile app sends only the image; OCR and review remain on the server and desktop."
            )
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AgroGreen)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Claiming binds this request to this phone's generated device ID. The claim token is never displayed or written to logs.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Button(onClick = onClaim, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.isClaimed) "Continue to document capture" else "Claim this scan request")
            }
        }
    }
}

@Composable
fun ClaimingScreen(pairing: PairingPayload?) {
    ProcessingScreen(
        title = "Claiming request",
        message = "Connecting this phone to scan ${pairing?.shortScanId ?: ""}…"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    pairing: PairingPayload?,
    errorMessage: String?,
    onBack: () -> Unit,
    onScanDocument: () -> Unit,
    onDismissError: () -> Unit
) {
    AppScaffold(title = "Capture document", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ErrorBanner(errorMessage, onDismissError)
            ScanReferenceCard(pairing)
            Spacer(Modifier.height(4.dp))
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = AgroGreen,
                modifier = Modifier.size(74.dp)
            )
            Text(
                "Place the complete form on a flat, well-lit surface.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                "The scanner lets you crop, rotate, retake, or import one image from the gallery before returning here.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onScanDocument, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open document scanner")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    state: ScannerUiState,
    onBack: () -> Unit,
    onRetake: () -> Unit,
    onUpload: () -> Unit,
    onDismissError: () -> Unit
) {
    val document = state.document ?: return
    AppScaffold(title = "Review image", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ErrorBanner(state.errorMessage, onDismissError)
            AsyncImage(
                model = document.uri,
                contentDescription = "Captured form preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Card(shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(document.displayName, fontWeight = FontWeight.SemiBold)
                    Text(
                        formatBytes(document.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                    Text("Retake")
                }
                Button(onClick = onUpload, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Upload")
                }
            }
        }
    }
}

@Composable
fun UploadingScreen(state: ScannerUiState) {
    ProcessingScreen(
        title = "Uploading form",
        message = "Sending the image securely to the AgroTrace backend…",
        progress = state.uploadProgress
    )
}

@Composable
fun SuccessScreen(pairing: PairingPayload?, onDone: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding()
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AgroGreen,
                modifier = Modifier.size(92.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Form sent successfully",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "The backend accepted the image for scan ${pairing?.shortScanId.orEmpty()}. OCR processing and officer review will continue in the desktop application.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Scan another form")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: ScannerUiState,
    onBack: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismissError: () -> Unit
) {
    AppScaffold(title = "Backend settings", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ErrorBanner(state.errorMessage, onDismissError)
            OutlinedTextField(
                value = state.settingsUrlInput,
                onValueChange = onUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("OCR API base URL") },
                singleLine = true,
                placeholder = { Text("https://ocr.example.gov.lk") }
            )
            InfoCard(
                title = "Development examples",
                body = "Android emulator: http://10.0.2.2:8080\nPhysical phone on the same LAN: http://YOUR_PC_IP:8080\nDeployment: use the final HTTPS server address."
            )
            Text(
                "Only the base URL is saved. Pairing codes and claim tokens are kept in memory only and cleared after completion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Save server address")
            }
        }
    }
}

@Composable
private fun ScanReferenceCard(pairing: PairingPayload?) {
    if (pairing == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Scan reference", style = MaterialTheme.typography.labelLarge)
            Text(
                pairing.shortScanId,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AgroGreen
            )
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pairing code")
                Text(pairing.pairingCode, fontWeight = FontWeight.Bold)
            }
            Text(
                pairing.scanId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProcessingScreen(
    title: String,
    message: String,
    progress: Float? = null
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .statusBarsPadding()
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (progress == null) {
                CircularProgressIndicator()
            } else {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("${(progress * 100).toInt()}%")
            }
            Spacer(Modifier.height(22.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text("Do not close the app.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        content(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
        )
    }
}
