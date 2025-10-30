package org.clipboard.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.clipboard.app.database.entities.ApprovedDeviceEntity
import org.clipboard.app.platform.ClipboardManager
import org.clipboard.app.repository.ClipboardRepository
import org.clipboard.app.ui.theme.*
import org.clipboard.app.viewmodel.ClipboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen(
    viewModel: ClipboardViewModel,
    approvedDevices: List<org.clipboard.app.database.entities.ApprovedDeviceEntity> = emptyList()
) {
    val clipboardText by viewModel.currentClipboard.collectAsState("")
    val isSending by viewModel.isSending.collectAsState()
    // Observe received messages for processing (but don't collect as state to avoid re-processing)
    val receivedMessages by viewModel.receivedMessages.collectAsState(emptyList())
    val sendToAllResults by viewModel.sendToAllResults.collectAsState(emptyList())
    val recentHistory by viewModel.recentHistory.collectAsState(emptyList())
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showSendToAllDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Track last processed message count to avoid re-processing
    var lastProcessedCount by remember { mutableIntStateOf(0) }

    // Handle received messages - only process new ones
    LaunchedEffect(receivedMessages) {
        val currentCount = receivedMessages.size
        if (currentCount > lastProcessedCount) {
            // Process only new messages
            val newMessages = receivedMessages.takeLast(currentCount - lastProcessedCount)
            newMessages.forEach { message ->
                scope.launch {
                    try {
                        viewModel.handleReceivedMessage(message)
                        snackbarMessage = "📨 Received from ${message.deviceName}: ${message.text.take(50)}${if (message.text.length > 50) "..." else ""}"
                        showSnackbar = true
                    } catch (e: Exception) {
                        println("❌ [ClipboardScreen] Error handling received message: ${e.message}")
                        e.printStackTrace()
                        // Don't crash - just log the error
                    }
                }
            }
            lastProcessedCount = currentCount
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Clipboard",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(adaptivePadding()),
            verticalArrangement = Arrangement.spacedBy(adaptiveSpacing())
        ) {
            // Main Clipboard Content Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(adaptiveCardPadding()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Current Clipboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        OutlinedTextField(
                            value = clipboardText,
                            onValueChange = viewModel::updateClipboardText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            label = { Text("Clipboard Content") },
                            maxLines = 10,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(adaptiveSpacing())
                        ) {
                            Button(
                                onClick = { 
                                    scope.launch {
                                        viewModel.copyFromSystem()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("📥")
                                Spacer(Modifier.width(adaptiveIconSpacing()))
                                Text(
                                    text = if (isSmallScreen()) "From System" else "Copy from System",
                                    style = TextStyle(fontSize = adaptiveButtonTextSize())
                                )
                            }

                            Button(
                                onClick = viewModel::copyToSystem,
                                modifier = Modifier.weight(1f),
                                enabled = clipboardText.isNotBlank(),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("📤")
                                Spacer(Modifier.width(adaptiveIconSpacing()))
                                Text(
                                    text = if (isSmallScreen()) "To System" else "Copy to System",
                                    style = TextStyle(fontSize = adaptiveButtonTextSize())
                                )
                            }
                        }
                    }
                }
            }
            
            // Send Actions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(adaptiveCardPadding()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Send to Devices",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(adaptiveSpacing())
                        ) {
                            FilledTonalButton(
                                onClick = { showDeviceDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isSending && approvedDevices.isNotEmpty() && clipboardText.isNotBlank()
                            ) {
                                Text("📡")
                                Spacer(Modifier.width(adaptiveIconSpacing()))
                                Text(
                                    text = if (isSending) "Sending..." else if (isSmallScreen()) "Send" else "Send to Device",
                                    style = TextStyle(fontSize = adaptiveButtonTextSize())
                                )
                            }
                            
                            FilledTonalButton(
                                onClick = { showSendToAllDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !isSending && approvedDevices.size > 1 && clipboardText.isNotBlank()
                            ) {
                                Text("📡📡")
                                Spacer(Modifier.width(adaptiveIconSpacing()))
                                Text(
                                    text = if (isSending) "Sending..." else if (isSmallScreen()) "Send All" else "Send to All",
                                    style = TextStyle(fontSize = adaptiveButtonTextSize())
                                )
                            }
                        }
                        
                        if (approvedDevices.isEmpty()) {
                            Text(
                                "No approved devices available. Go to Devices tab to add devices.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            
            // Recent History Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(adaptiveCardPadding())
                    ) {
                        Text(
                            "Recent History",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        if (recentHistory.isEmpty()) {
                            Text(
                                "No recent items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recentHistory) { item ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Main content - clickable to copy
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        viewModel.updateClipboardText(item.text)
                                                    }
                                            ) {
                                                Text(
                                                    text = item.text.take(100) + if (item.text.length > 100) "..." else "",
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "From: ${item.sourceDeviceName}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Delete icon
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        viewModel.deleteHistoryItem(item.id)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text(
                                                    "🗑️",
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Device Selection Dialog
    if (showDeviceDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceDialog = false },
            title = { 
                Text(
                    "Select Device",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                if (approvedDevices.isEmpty()) {
                    Text(
                        "No devices available",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        approvedDevices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val success = viewModel.sendToDevice(
                                                org.clipboard.app.models.Device(
                                                    deviceId = device.deviceId,
                                                    name = device.name,
                                                    ipAddress = device.ipAddress,
                                                    port = device.port,
                                                    isApproved = true
                                                ),
                                                clipboardText
                                            )
                                            showDeviceDialog = false
                                            snackbarMessage = if (success) {
                                                "✅ Sent to ${device.name}"
                                            } else {
                                                "❌ Failed to send to ${device.name}"
                                            }
                                            showSnackbar = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    Text(
                                        device.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Send to All dialog
    if (showSendToAllDialog) {
        AlertDialog(
            onDismissRequest = { 
                showSendToAllDialog = false
                viewModel.clearSendToAllResults()
            },
            title = { 
                Text(
                    "Send to All Devices",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Send clipboard content to all ${approvedDevices.size} approved devices?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Show results if available
                    if (sendToAllResults.isNotEmpty()) {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Results:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                sendToAllResults.forEach { result ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            result.deviceName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            if (result.success) "✅" else "❌",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (result.error != null) {
                                        Text(
                                            result.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (sendToAllResults.isEmpty()) {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.sendToAllDevices(approvedDevices, clipboardText)
                            }
                        },
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("Send to All")
                    }
                } else {
                    TextButton(onClick = { 
                        showSendToAllDialog = false
                        viewModel.clearSendToAllResults()
                    }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSendToAllDialog = false
                    viewModel.clearSendToAllResults()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Snackbar for notifications
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            kotlinx.coroutines.delay(3000) // Show for 3 seconds
            showSnackbar = false
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showSnackbar = false }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(snackbarMessage)
        }
    }
}