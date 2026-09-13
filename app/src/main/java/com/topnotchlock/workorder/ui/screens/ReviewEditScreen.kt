@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.topnotchlock.workorder.data.Company
import com.topnotchlock.workorder.data.FieldKey
import com.topnotchlock.workorder.ui.MainViewModel
import java.io.File

@Composable
fun ReviewEditScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenPdf: (File) -> Unit
) {
    val context = LocalContext.current
    val draft = viewModel.draft
    val matched = viewModel.matchedFields
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var showAddCompany by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Review Work Order") },
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
            Text(
                "Fields not found in the request are highlighted - fill those in by hand.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            CompanyDropdown(
                viewModel = viewModel,
                selectedName = draft.companyName,
                onAddNew = { showAddCompany = true }
            )
            Spacer(Modifier.height(8.dp))

            LabeledField("Work Order #", draft.woNumber, true) { viewModel.updateWoNumber(it) }
            LabeledField("Date", draft.date, true) { viewModel.updateDate(it) }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Site Information", style = MaterialTheme.typography.titleMedium)
            EditableField(FieldKey.SITE_NAME, draft, matched, viewModel)
            EditableField(FieldKey.SITE_ID, draft, matched, viewModel)
            EditableField(FieldKey.ADDRESS, draft, matched, viewModel)
            EditableField(FieldKey.CONTACT, draft, matched, viewModel)
            EditableField(FieldKey.PHONE, draft, matched, viewModel)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Job Details", style = MaterialTheme.typography.titleMedium)
            EditableField(FieldKey.ARRIVE_BY, draft, matched, viewModel)
            EditableField(FieldKey.COMPLETE_BY, draft, matched, viewModel)

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("Problem Reported", style = MaterialTheme.typography.titleMedium)
            EditableField(FieldKey.PROBLEM, draft, matched, viewModel, multiline = true)

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.generatePdf { file ->
                        generatedFile = file
                    }
                },
                enabled = !viewModel.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (viewModel.isBusy) "Generating..." else "Generate PDF")
            }

            viewModel.lastError?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }

            generatedFile?.let { file ->
                Spacer(Modifier.height(16.dp))
                Text(
                    "Work order saved. View it right here in the app, or share it to another app.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onOpenPdf(file) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View PDF")
                    }
                    Button(
                        onClick = { sharePdf(context, viewModel.shareUriFor(file)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share PDF")
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text("Done - back to Home")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddCompany) {
        AddCompanyDialog(
            onDismiss = { showAddCompany = false },
            onSave = { name, phone ->
                val id = viewModel.newCompanyId(name)
                val company = Company(id = id, name = name, phone = phone)
                viewModel.saveCompany(company)
                viewModel.updateDraftCompany(company)
                showAddCompany = false
            }
        )
    }
}

@Composable
private fun CompanyDropdown(viewModel: MainViewModel, selectedName: String, onAddNew: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val companies = viewModel.companies
    val displayName = selectedName.ifBlank { "Select company" }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Company (header on the PDF)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            companies.forEach { company ->
                DropdownMenuItem(
                    text = { Text("${company.name} - ${company.phone}") },
                    onClick = {
                        viewModel.updateDraftCompany(company)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("+ Add new company") },
                onClick = {
                    expanded = false
                    onAddNew()
                }
            )
        }
    }
}

@Composable
private fun AddCompanyDialog(onDismiss: () -> Unit, onSave: (name: String, phone: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a company") },
        text = {
            Column {
                Text(
                    "This is a different business (not d/b/a Top Notch Lock)? Add its name and " +
                        "phone number here - they'll print in the PDF header whenever this company " +
                        "is selected.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
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

/**
 * Opens the PDF directly in whatever PDF viewer the user has (Google Drive,
 * a browser, Adobe Acrobat, etc.) so they can read/print it themselves
 * without going through the share sheet. Used as a fallback outside of the
 * app's own built-in viewer.
 */
fun openPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "No PDF viewer app found on this device. Try Share instead, or install a PDF viewer (e.g. Google Drive) from the Play Store.",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun sharePdf(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Work Order PDF"))
}

@Composable
private fun LabeledField(label: String, value: String, singleLine: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun EditableField(
    field: FieldKey,
    draft: com.topnotchlock.workorder.data.WorkOrder,
    matched: Set<FieldKey>,
    viewModel: MainViewModel,
    multiline: Boolean = false
) {
    val wasMatched = field in matched
    OutlinedTextField(
        value = draft.get(field),
        onValueChange = { viewModel.updateDraftField(field, it) },
        label = { Text(if (wasMatched) field.label else "${field.label} (not found - check this)") },
        singleLine = !multiline,
        minLines = if (multiline) 4 else 1,
        isError = !wasMatched && draft.get(field).isBlank(),
        colors = if (!wasMatched) OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFB8860B),
            focusedBorderColor = Color(0xFFB8860B)
        ) else OutlinedTextFieldDefaults.colors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
