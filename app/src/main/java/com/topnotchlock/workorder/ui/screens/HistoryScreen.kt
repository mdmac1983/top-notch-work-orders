@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.topnotchlock.workorder.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MainViewModel, onBack: () -> Unit, onOpenPdf: (File) -> Unit) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun missingFileWarning() {
        scope.launch {
            snackbarHostState.showSnackbar(
                "That PDF isn't on this device anymore (it may have been cleared, e.g. by " +
                    "reinstalling the app). Recreate it from New Work Order if you still need it."
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text(
                    "No work orders generated yet.",
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(history) { entity ->
                    val file = File(entity.pdfPath)
                    ListItem(
                        headlineContent = { Text("${entity.woNumber} - ${entity.siteName.ifBlank { "(no site name)" }}") },
                        supportingContent = { Text("${entity.vendorName} - ${dateFormat.format(java.util.Date(entity.createdAtMillis))}") },
                        modifier = Modifier.clickable {
                            if (file.exists()) onOpenPdf(file) else missingFileWarning()
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    if (file.exists()) onOpenPdf(file) else missingFileWarning()
                                }) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "View PDF")
                                }
                                IconButton(onClick = {
                                    if (file.exists()) {
                                        sharePdf(context, viewModel.shareUriFor(file))
                                    } else {
                                        missingFileWarning()
                                    }
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share")
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
