@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topnotchlock.workorder.ui.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val generator = viewModel.woNumberGenerator
    var prefix by remember { mutableStateOf(generator.prefix) }
    var nextNumber by remember { mutableStateOf(generator.nextNumber.toString()) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Work Order Numbering", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Work orders are numbered as PREFIX-NUMBER (e.g. TNL-1001) and count up " +
                    "automatically each time you start a new work order.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = prefix,
                onValueChange = { prefix = it; saved = false },
                label = { Text("Prefix") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nextNumber,
                onValueChange = { nextNumber = it.filter { c -> c.isDigit() }; saved = false },
                label = { Text("Next work order number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    generator.prefix = prefix
                    nextNumber.toIntOrNull()?.let { generator.nextNumber = it }
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
            if (saved) {
                Spacer(Modifier.height(8.dp))
                Text("Saved.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
