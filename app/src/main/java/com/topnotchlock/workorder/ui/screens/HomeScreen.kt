@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.topnotchlock.workorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onNewWorkOrder: () -> Unit,
    onVendors: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Top Notch Lock") })
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Work Order Generator",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(32.dp))

            HomeButton("New Work Order", Icons.Filled.PostAdd, onNewWorkOrder)
            Spacer(Modifier.height(16.dp))
            HomeButton("Vendors", Icons.Filled.Description, onVendors)
            Spacer(Modifier.height(16.dp))
            HomeButton("History", Icons.Filled.History, onHistory)
            Spacer(Modifier.height(16.dp))
            HomeButton("Settings", Icons.Filled.Settings, onSettings)
        }
    }
}

@Composable
private fun HomeButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
