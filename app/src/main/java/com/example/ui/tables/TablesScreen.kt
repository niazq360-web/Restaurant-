package com.example.ui.tables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.pos.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(viewModel: PosViewModel) {
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", fontSize = 24.sp)
            }
        },
        containerColor = Color(0xFFFEF7FF)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Table Management", color = Color(0xFF6750A4), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tables) { table ->
                    val isOccupied = table.status == "Occupied"
                    val bgColor = if (isOccupied) Color(0xFFFFCC80) else Color(0xFFC8E6C9)
                    val contentColor = if (isOccupied) Color(0xFFE65100) else Color(0xFF1B5E20)
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .clickable { 
                                val newStatus = if (isOccupied) "Available" else "Occupied"
                                viewModel.updateTableStatus(table, newStatus)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(table.name, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(table.status, color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var tableName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Table") },
            text = {
                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it },
                    label = { Text("Table Name (e.g. T1)") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (tableName.isNotBlank()) {
                        viewModel.addTable(tableName)
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
