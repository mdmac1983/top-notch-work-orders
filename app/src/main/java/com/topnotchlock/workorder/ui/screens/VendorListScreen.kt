@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topnotchlock.workorder.data.VendorTemplate
import com.topnotchlock.workorder.ui.MainViewModel

@Composable
fun VendorListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditVendor: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newVendorName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vendors") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Vendor")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(viewModel.vendors) { vendor: VendorTemplate ->
                ListItem(
                    headlineContent = { Text(vendor.name) },
                    supportingContent = { Text("${vendor.rules.size} field rules") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { viewModel.deleteVendor(vendor.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { onEditVendor(vendor.id) }
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Vendor") },
            text = {
                OutlinedTextField(
                    value = newVendorName,
                    onValueChange = { newVendorName = it },
                    label = { Text("Vendor name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newVendorName.isNotBlank()) {
                        val id = viewModel.newVendorId(newVendorName)
                        viewModel.saveVendor(VendorTemplate.blank(id, newVendorName))
                        showAddDialog = false
                        val nameToOpen = id
                        newVendorName = ""
                        onEditVendor(nameToOpen)
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
