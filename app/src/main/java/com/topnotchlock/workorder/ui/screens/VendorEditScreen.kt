@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topnotchlock.workorder.data.VendorTemplate
import com.topnotchlock.workorder.parsing.VendorParser
import com.topnotchlock.workorder.ui.MainViewModel

@Composable
fun VendorEditScreen(viewModel: MainViewModel, vendorId: String, onBack: () -> Unit) {
    val vendor = remember(vendorId, viewModel.vendors) { viewModel.vendors.firstOrNull { it.id == vendorId } }
    var jsonText by remember(vendorId) { mutableStateOf(vendor?.toJsonString() ?: "") }
    var saveError by remember { mutableStateOf<String?>(null) }
    var testInput by remember { mutableStateOf("") }
    var testResults by remember { mutableStateOf<List<Pair<String, String>>?>(null) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(vendor?.name ?: "Vendor") },
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
                "Each vendor's parsing rules are stored as JSON. Every field needs a " +
                    "strategy: AFTER_LABEL (grab text after a label like \"Site Name:\"), " +
                    "BETWEEN_LABELS (grab everything between two labels - good for multi-line " +
                    "problem descriptions), REGEX (advanced pattern with one capture group), " +
                    "or STATIC (always use a fixed value).",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it; saveError = null },
                label = { Text("Vendor rules (JSON)") },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                minLines = 14,
                modifier = Modifier.fillMaxWidth()
            )
            saveError?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    try {
                        val parsed = VendorTemplate.fromJsonString(jsonText)
                        viewModel.saveVendor(if (parsed.id.isBlank()) parsed.copy(id = vendorId) else parsed)
                        saveError = null
                    } catch (e: Exception) {
                        saveError = "Invalid JSON: ${e.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Vendor Rules") }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("Test Extraction", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Paste a real (or made-up) sample request below and run the test to see " +
                    "exactly what each field would come out as, without creating a work order.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = testInput,
                onValueChange = { testInput = it },
                label = { Text("Sample request text") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val template = try {
                        VendorTemplate.fromJsonString(jsonText)
                    } catch (e: Exception) {
                        saveError = "Invalid JSON: ${e.message}"
                        null
                    }
                    if (template != null) {
                        testResults = VendorParser.parse(testInput, template).map { result ->
                            (if (result.matched) result.field.label else "${result.field.label} (NOT FOUND)") to result.value
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Run Test") }

            testResults?.let { results ->
                Spacer(Modifier.height(12.dp))
                results.forEach { (label, value) ->
                    Text("$label: ${value.ifBlank { "(empty)" }}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
