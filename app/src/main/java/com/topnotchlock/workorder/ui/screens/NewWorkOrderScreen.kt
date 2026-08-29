@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.topnotchlock.workorder.data.VendorTemplate
import com.topnotchlock.workorder.ui.MainViewModel
import java.io.File

private fun newCameraOutputUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

@Composable
fun NewWorkOrderScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onExtracted: () -> Unit
) {
    val context = LocalContext.current
    val vendors = viewModel.vendors
    var selectedVendor by remember { mutableStateOf(vendors.firstOrNull()) }
    var pastedText by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var vendorMenuExpanded by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val vendor = selectedVendor ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.startNewDraft(vendor)
            viewModel.extractFromImageUri(vendor, uri)
        }
    }
    val pickPdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val vendor = selectedVendor ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            viewModel.startNewDraft(vendor)
            viewModel.extractFromPdfUri(vendor, uri)
        }
    }
    val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val vendor = selectedVendor
        val uri = cameraUri
        if (success && vendor != null && uri != null) {
            viewModel.startNewDraft(vendor)
            viewModel.extractFromImageUri(vendor, uri)
        }
    }

    // Auto-load anything shared in from Gmail/Outlook/Screenshots.
    LaunchedEffect(selectedVendor, viewModel.hasPendingShare()) {
        val vendor = selectedVendor ?: return@LaunchedEffect
        if (!viewModel.hasPendingShare()) return@LaunchedEffect
        viewModel.pendingShareText?.let { text ->
            viewModel.startNewDraft(vendor)
            viewModel.extractFromPastedText(vendor, text)
            viewModel.clearPendingShare()
        }
        viewModel.pendingShareImageUri?.let { uri ->
            viewModel.startNewDraft(vendor)
            viewModel.extractFromImageUri(vendor, uri)
            viewModel.clearPendingShare()
        }
        viewModel.pendingSharePdfUri?.let { uri ->
            viewModel.startNewDraft(vendor)
            viewModel.extractFromPdfUri(vendor, uri)
            viewModel.clearPendingShare()
        }
    }

    LaunchedEffect(viewModel.isBusy, viewModel.rawExtractedText) {
        if (!viewModel.isBusy && viewModel.rawExtractedText.isNotBlank()) {
            onExtracted()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("New Work Order") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Text("1. Choose the vendor", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = vendorMenuExpanded, onExpandedChange = { vendorMenuExpanded = it }) {
                OutlinedTextField(
                    value = selectedVendor?.name ?: "No vendors yet - add one first",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("Vendor") }
                )
                ExposedDropdownMenu(expanded = vendorMenuExpanded, onDismissRequest = { vendorMenuExpanded = false }) {
                    vendors.forEach { vendor ->
                        DropdownMenuItem(text = { Text(vendor.name) }, onClick = {
                            selectedVendor = vendor
                            vendorMenuExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("2. Bring in their request", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = pastedText,
                onValueChange = { pastedText = it },
                label = { Text("Paste the email text here") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val vendor = selectedVendor ?: return@Button
                    viewModel.startNewDraft(vendor)
                    viewModel.extractFromPastedText(vendor, pastedText)
                },
                enabled = selectedVendor != null && pastedText.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Extract from pasted text") }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { pickImageLauncher.launch("image/*") },
                    enabled = selectedVendor != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Import Screenshot") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                    enabled = selectedVendor != null,
                    modifier = Modifier.weight(1f)
                ) { Text("Import PDF") }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val uri = newCameraOutputUri(context)
                    cameraUri = uri
                    takePhotoLauncher.launch(uri)
                },
                enabled = selectedVendor != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Take a Photo of the Request") }

            if (viewModel.isBusy) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Reading text...")
                }
            }

            viewModel.lastError?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
