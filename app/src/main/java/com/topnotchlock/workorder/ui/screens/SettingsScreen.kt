@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topnotchlock.workorder.data.Company
import com.topnotchlock.workorder.ui.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val generator = viewModel.woNumberGenerator
    var prefix by remember { mutableStateOf(generator.prefix) }
    var nextNumber by remember { mutableStateOf(generator.nextNumber.toString()) }
    var saved by remember { mutableStateOf(false) }
    var showAddCompany by remember { mutableStateOf(false) }
    var editingCompany by remember { mutableStateOf<Company?>(null) }
    val companies = viewModel.companies

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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Companies", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "If you run more than one business, add each one here with its own name and " +
                    "phone number. Pick which one to bill a work order under from the dropdown on " +
                    "the Review screen - it controls the name and phone printed in the PDF header. " +
                    "The starred company is the default for new work orders.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            companies.forEach { company ->
                ListItem(
                    headlineContent = { Text(company.name) },
                    supportingContent = { Text(company.phone.ifBlank { "No phone number set" }) },
                    leadingContent = {
                        IconButton(onClick = { viewModel.setDefaultCompany(company.id) }) {
                            if (company.id == viewModel.defaultCompanyId) {
                                Icon(Icons.Filled.Star, contentDescription = "Default company")
                            } else {
                                Icon(Icons.Filled.StarBorder, contentDescription = "Set as default")
                            }
                        }
                    },
                    trailingContent = {
                        if (companies.size > 1) {
                            IconButton(onClick = { viewModel.deleteCompany(company.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete ${company.name}")
                            }
                        }
                    },
                    modifier = Modifier.clickable { editingCompany = company }
                )
                HorizontalDivider()
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showAddCompany = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Add another company")
            }

            HorizontalDivider(Modifier.padding(vertical = 20.dp))

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
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddCompany) {
        CompanyDialog(
            title = "Add a company",
            initial = null,
            onDismiss = { showAddCompany = false },
            onSave = { name, phone ->
                val id = viewModel.newCompanyId(name)
                viewModel.saveCompany(Company(id = id, name = name, phone = phone))
                showAddCompany = false
            }
        )
    }
    editingCompany?.let { company ->
        CompanyDialog(
            title = "Edit company",
            initial = company,
            onDismiss = { editingCompany = null },
            onSave = { name, phone ->
                viewModel.saveCompany(company.copy(name = name, phone = phone))
                editingCompany = null
            }
        )
    }
}

@Composable
private fun CompanyDialog(
    title: String,
    initial: Company?,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), phone.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
