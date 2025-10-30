package org.clipboard.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import org.clipboard.app.ui.theme.*
import org.clipboard.app.viewmodel.DevicesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val serverRunning by viewModel.serverRunning.collectAsState()
    val approvedDevices by viewModel.approvedDevices.collectAsState(emptyList())
    val discoveredDevices by viewModel.discoveredDevices.collectAsState(emptyList())
    val currentDeviceName = viewModel.currentDeviceName
    val localAddress by viewModel.localAddress.collectAsState("")
    val isScanning by viewModel.isScanning.collectAsState()
    val approvalRequests by viewModel.approvalRequests.collectAsState(emptyList())
    val deviceApprovals by viewModel.deviceApprovals.collectAsState(emptyList())
    val deviceUnpairs by viewModel.deviceUnpairs.collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    
    // Manual add device state
    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualIpAddress by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8080") }
    var manualDeviceName by remember { mutableStateOf("") }
    var isAddingDevice by remember { mutableStateOf(false) }
    var addDeviceError by remember { mutableStateOf("") }
    
    // Handle incoming unpair notifications
    LaunchedEffect(deviceUnpairs) {
        deviceUnpairs.forEach { unpair ->
            scope.launch {
                try {
                    viewModel.handleDeviceUnpair(unpair)
                } catch (e: Exception) {
                    println("❌ [DevicesScreen] Error handling unpair notification: ${e.message}")
                    e.printStackTrace()
                    // Don't crash - just log the error
                }
            }
        }
    }
    
    // Log approved devices changes
    LaunchedEffect(approvedDevices) {
        println("📱 [DevicesScreen] Approved devices changed: ${approvedDevices.size} devices")
        approvedDevices.forEach { device ->
            println("   - ${device.name} (${device.ipAddress})")
        }
    }
    
    // Log discovered devices changes
    LaunchedEffect(discoveredDevices) {
        println("🔍 [DevicesScreen] Discovered devices changed: ${discoveredDevices.size} devices")
        discoveredDevices.forEach { device ->
            println("   - ${device.name} (${device.ipAddress}:${device.port})")
        }
    }

    // Handle incoming device approval notifications
    LaunchedEffect(deviceApprovals) {
        deviceApprovals.forEach { approval ->
            scope.launch {
                try {
                    viewModel.handleDeviceApproval(approval)
                } catch (e: Exception) {
                    println("❌ [DevicesScreen] Error handling approval notification: ${e.message}")
                    e.printStackTrace()
                    // Don't crash - just log the error
                }
            }
        }
    }
    
    // Show approval request dialog if pending
    val pendingRequest = approvalRequests.firstOrNull()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Devices",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showManualAddDialog = true }
                    ) {
                        Text("➕") // Add device icon
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    viewModel.rescanDevices()
                                } catch (e: Exception) {
                                    println("❌ [DevicesScreen] Error rescanning devices: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            // Scanning animation - faster rotation for more responsive feel
                            val infiniteTransition = rememberInfiniteTransition()
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = LinearEasing), // Faster: 600ms instead of 1000ms
                                    repeatMode = RepeatMode.Restart
                                )
                            )

                            Text(
                                "🔍",
                                modifier = Modifier.graphicsLayer(rotationZ = rotation)
                            )
                        } else {
                            Text("🔍") // Scan icon
                        }
                    }
                }
            )
        }
    ) { padding ->
        // Approval request dialog
        pendingRequest?.let { request ->
            AlertDialog(
                onDismissRequest = {
                    scope.launch {
                        try {
                            viewModel.rejectApprovalRequest(request)
                        } catch (e: Exception) {
                            println("❌ [DevicesScreen] Error rejecting approval request: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                },
                title = { 
                    Text(
                        "Device Approval Request",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "${request.deviceName} wants to connect.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Device ID: ${request.deviceId}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "IP: ${request.ipAddress}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            viewModel.approveDevice(request)
                                        } catch (e: Exception) {
                                            println("❌ [DevicesScreen] Error approving device: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }
                                },
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text("Accept")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.rejectApprovalRequest(request)
                            }
                        }
                    ) {
                        Text("Reject")
                    }
                }
            )
        }
        
        // Manual add device dialog
        if (showManualAddDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showManualAddDialog = false
                    addDeviceError = ""
                },
                title = { 
                    Text(
                        "Add Device Manually",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = manualDeviceName,
                            onValueChange = { manualDeviceName = it },
                            label = { Text("Device Name") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAddingDevice,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        OutlinedTextField(
                            value = manualIpAddress,
                            onValueChange = { manualIpAddress = it },
                            label = { Text("IP Address") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAddingDevice,
                            placeholder = { Text("192.168.1.100") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        OutlinedTextField(
                            value = manualPort,
                            onValueChange = { manualPort = it },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isAddingDevice,
                            placeholder = { Text("8080") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        
                        if (addDeviceError.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    addDeviceError,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                isAddingDevice = true
                                addDeviceError = ""
                                
                                val port = try {
                                    manualPort.toInt()
                                } catch (e: Exception) {
                                    addDeviceError = "Invalid port number"
                                    isAddingDevice = false
                                    return@launch
                                }
                                
                                val result = viewModel.addManualDevice(
                                    ipAddress = manualIpAddress.trim(),
                                    port = port,
                                    deviceName = manualDeviceName.trim().ifEmpty { "Manual Device" }
                                )
                                
                                when (result) {
                                    is DevicesViewModel.AddDeviceResult.Success -> {
                                        showManualAddDialog = false
                                        manualIpAddress = ""
                                        manualPort = "8080"
                                        manualDeviceName = ""
                                        addDeviceError = ""
                                    }
                                    is DevicesViewModel.AddDeviceResult.Error -> {
                                        addDeviceError = result.message
                                    }
                                }
                                
                                isAddingDevice = false
                            }
                        },
                        enabled = !isAddingDevice && manualIpAddress.isNotEmpty(),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(if (isAddingDevice) "Adding..." else "Add Device")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showManualAddDialog = false
                            addDeviceError = ""
                        },
                        enabled = !isAddingDevice
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(adaptivePadding()),
            verticalArrangement = Arrangement.spacedBy(adaptiveSpacing())
        ) {
            // Current Device Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(adaptiveCardPadding()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "📱 $currentDeviceName",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Device Name:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                currentDeviceName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "IP Address:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                localAddress.ifEmpty { "192.168.1.x" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Port:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                viewModel.serverPort.collectAsState().value.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Server Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(adaptiveCardPadding()),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (serverRunning) {
                                    Text(
                                        "✅",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Server Running",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Text(
                                        "❌",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Server Stopped",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Switch(
                            checked = serverRunning,
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    if (!isChecked) {
                                        viewModel.stopServer()
                                    } else {
                                        viewModel.startServer(viewModel.serverPort.value)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Approved Devices Section
            if (approvedDevices.isNotEmpty()) {
                item {
                    Text(
                        "Approved Devices (${approvedDevices.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                items(approvedDevices) { device ->
                    DeviceCard(
                        name = device.name,
                        ipAddress = device.ipAddress,
                        deviceId = device.deviceId,
                        onUnpair = { deviceId ->
                            scope.launch {
                                viewModel.unpairDevice(deviceId)
                            }
                        }
                    )
                }
            }
            
            // Discovered Devices Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Discovered Devices (${discoveredDevices.size})",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    if (isScanning) {
                        // Scanning animation indicator - faster pulsing for more responsive feel
                        val infiniteTransition = rememberInfiniteTransition()
                        val opacity by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400, easing = LinearEasing), // Faster: 400ms instead of 800ms
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Text(
                            "🔍 Scanning...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = opacity)
                        )
                    }
                }
            }
            
            if (discoveredDevices.isNotEmpty()) {
                items(discoveredDevices) { device ->
                    DiscoveredDeviceCard(
                        name = device.name,
                        ipAddress = device.ipAddress,
                        port = device.port,
                        deviceId = device.deviceId,
                        onApprove = { device ->
                            println("✅ [DevicesScreen] Approve button clicked for: ${device.name}")
                            scope.launch {
                                viewModel.approveDiscoveredDevice(
                                    deviceId = device.deviceId,
                                    deviceName = device.name,
                                    ipAddress = device.ipAddress,
                                    port = device.port
                                )
                            }
                        },
                        device = device
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(adaptiveLargePadding()),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "🔍",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                                Text(
                                    "No devices discovered yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Ensure other devices are running the app and server is active",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredDeviceCard(
    name: String,
    ipAddress: String,
    port: Int,
    deviceId: String,
    onApprove: (device: org.clipboard.app.models.Device) -> Unit,
    device: org.clipboard.app.models.Device
) {
    val smallScreen = isSmallScreen()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        if (smallScreen) {
            // Vertical layout for small screens
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(adaptiveCardPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    name,
                    style = if (smallScreen) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "ID: ${deviceId.takeLast(4)}",
                        style = if (smallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$ipAddress:$port",
                        style = if (smallScreen) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Button(
                    onClick = {
                        println("✅ [DevicesScreen] Approve button clicked for: ${device.name}")
                        onApprove(device)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Approve")
                }
            }
        } else {
            // Horizontal layout for larger screens
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(adaptiveCardPadding()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "ID: ${deviceId.takeLast(4)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$ipAddress:$port",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        println("✅ [DevicesScreen] Approve button clicked for: ${device.name}")
                        onApprove(device)
                    },
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
fun DeviceCard(
    name: String,
    ipAddress: String,
    deviceId: String,
    onUnpair: (deviceId: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    ipAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onUnpair(deviceId) },
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    "🔗❌",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}