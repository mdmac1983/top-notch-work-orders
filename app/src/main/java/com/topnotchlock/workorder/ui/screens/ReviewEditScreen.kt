@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.topnotchlock.workorder.data.FieldKey
import com.topnotchlock.workorder.ui.MainViewModel

@Composable
fun ReviewEditScreen(viewModel: MainViewModel, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val draft = viewModel.draft
    val matched = viewModel.matchedFields

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
                        val uri = viewModel.shareUriFor(file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Work Order PDF"))
                        onDone()
                    }
                },
                enabled = !viewModel.isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(if (viewModel.isBusy) "Generating..." else "Generate PDF & Share")
            }

            viewModel.lastError?.let { error ->
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
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
