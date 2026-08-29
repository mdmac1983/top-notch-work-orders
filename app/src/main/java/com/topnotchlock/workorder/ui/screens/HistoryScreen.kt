@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.topnotchlock.workorder.ui.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("History") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
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
                    ListItem(
                        headlineContent = { Text("${entity.woNumber} - ${entity.siteName.ifBlank { "(no site name)" }}") },
                        supportingContent = { Text("${entity.vendorName} - ${dateFormat.format(java.util.Date(entity.createdAtMillis))}") },
                        trailingContent = {
                            IconButton(onClick = {
                                val file = File(entity.pdfPath)
                                if (file.exists()) {
                                    val uri = viewModel.shareUriFor(file)
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Work Order PDF"))
                                }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
