package org.clipboard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.clipboard.app.ui.components.NeumorphicSurface
import org.clipboard.app.ui.components.SectionHeader
import org.clipboard.app.ui.components.SettingsRow
import org.clipboard.app.ui.theme.adaptivePadding
import org.clipboard.app.ui.theme.adaptiveSpacing
import org.clipboard.app.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState(null)
    var deviceName by remember { mutableStateOf(settings?.deviceName ?: viewModel.currentDeviceName) }
    var serverPort by remember { mutableStateOf(settings?.serverPort?.toString() ?: "8080") }
    var keepHistory by remember { mutableStateOf(settings?.keepHistory ?: true) }
    val darkTheme by viewModel.darkTheme.collectAsState(false)
    var showClearDialog by remember { mutableStateOf(false) }
    var showThemePreview by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Update local state when settings change
    LaunchedEffect(settings) {
        settings?.let {
            deviceName = it.deviceName
            serverPort = it.serverPort.toString()
            keepHistory = it.keepHistory
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    androidx.compose.material3.Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = adaptivePadding(), vertical = adaptiveSpacing()),
            verticalArrangement = Arrangement.spacedBy(adaptiveSpacing())
        ) {
            // Hero Header
            item {
                val shape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .padding(horizontal = adaptivePadding(), vertical = adaptivePadding())
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        androidx.compose.material3.Text(
                            "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        androidx.compose.material3.Text(
                            "Personalize your experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showThemePreview = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                androidx.compose.material3.Text("Preview theme")
                            }
                            FilledTonalButton(
                                onClick = { showClearDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors()
                            ) {
                                androidx.compose.material3.Text("Clear data")
                            }
                        }
                    }
                }
            }
            // Device Information Section
            item {
                SettingsCard(
                    title = "Device Information",
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SectionHeader(title = "Device")
                    // Device Name - Editable
                    SettingsTextField(
                        label = "Device Name",
                        value = deviceName,
                        onValueChange = { 
                            deviceName = it
                            scope.launch {
                                viewModel.updateDeviceName(it)
                            }
                        },
                        editable = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SettingsRow(
                        label = "Device ID",
                        subtext = settings?.deviceName?.takeLast(8) ?: "Unknown"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    SettingsRow(
                        label = "Current IP Address",
                        subtext = "192.168.1.100"
                    )
                }
            }
            
            // Server Settings Section
            item {
                SettingsCard(
                    title = "Server Settings",
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SectionHeader(title = "Server")
                    // Port Number - Editable
                    SettingsTextField(
                        label = "Port Number",
                        value = serverPort,
                        onValueChange = { 
                            serverPort = it
                            val port = it.toIntOrNull()
                            if (port != null && port in 1024..65535) {
                scope.launch {
                    try {
                        viewModel.updateServerPort(port)
                    } catch (e: Exception) {
                        println("❌ [SettingsScreen] Error updating server port: ${e.message}")
                        e.printStackTrace()
                    }
                }
                            }
                        },
                        editable = true,
                        isError = serverPort.toIntOrNull()?.let { port -> port < 1024 || port > 65535 } ?: false
                    )
                    
                    if (serverPort.toIntOrNull()?.let { port -> port < 1024 || port > 65535 } == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            "Port must be between 1024 and 65535",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Auto-start on Boot - Toggle
                    SettingsRow(
                        label = "Auto-start on Boot",
                        trailing = {
                            Switch(
                                checked = settings?.autoStart ?: true,
                                onCheckedChange = { checked ->
                                    scope.launch { viewModel.updateAutoStart(checked) }
                                }
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Discovery Method - Selection
                    SettingsRow(
                        label = "Discovery Method",
                        subtext = settings?.discoveryMethod ?: "MDNS",
                        onClick = { /* TODO: selection */ }
                    )
                }
            }
            
            // Appearance Section
            item {
                SettingsCard(
                    title = "Appearance",
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SectionHeader(title = "Appearance")
                    // Dark Theme - Toggle
                    SettingsRow(
                        label = "Dark Theme",
                        trailing = {
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = { checked -> scope.launch { viewModel.updateDarkTheme(checked) } }
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Theme Preview - Preview Button
                    SettingsPreviewButton(
                        label = "Theme Preview",
                        value = if (darkTheme) "Dark Theme" else "Light Theme",
                        onTap = { showThemePreview = true }
                    )
                }
            }
            
            // History Settings Section
            item {
                SettingsCard(
                    title = "History Settings",
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SectionHeader(title = "History")
                    // Keep History - Toggle
                    SettingsRow(
                        label = "Keep History",
                        trailing = {
                            Switch(
                                checked = keepHistory,
                                onCheckedChange = { checked ->
                                    keepHistory = checked
                                    scope.launch { viewModel.updateKeepHistory(checked) }
                                }
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Retention Days - Selection Detail
                    SettingsRow(
                        label = "Retention Days",
                        subtext = "${settings?.historyRetentionDays ?: -1} days",
                        onClick = { /* TODO */ }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Max Items - Selection Detail
                    SettingsRow(
                        label = "Max Items",
                        subtext = "${settings?.maxHistoryItems ?: 100} items",
                        onClick = { /* TODO */ }
                    )
                }
            }
            
            // Data Management Section (Existing feature - merged)
            item {
                SettingsCard(
                    title = "Data Management",
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SectionHeader(title = "Data & Storage")
                    androidx.compose.material3.Text(
                        "Clear all clipboard history and approved devices. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsRow(
                        label = "Clear All Data",
                        subtext = "Deletes history and approved devices",
                        onClick = { showClearDialog = true },
                        trailing = {
                            Button(
                                onClick = { showClearDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                androidx.compose.material3.Text("Clear")
                            }
                        }
                    )
                }
            }
            
            // About Section (Existing feature - merged)
            item {
                SettingsCard(
                    title = "About",
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        androidx.compose.material3.Text(
                            "Version:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        androidx.compose.material3.Text(
                            "1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        androidx.compose.material3.Text(
                            "Platform:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        androidx.compose.material3.Text(
                            "Kotlin Multiplatform",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Theme Preview Dialog
    if (showThemePreview) {
        AlertDialog(
            onDismissRequest = { showThemePreview = false },
            title = { 
                androidx.compose.material3.Text(
                    "Theme Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                Column {
                    androidx.compose.material3.Text(
                        "Current Theme: ${if (darkTheme) "Dark" else "Light"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Text(
                        "This is how your app will look with the current theme settings.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showThemePreview = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.material3.Text("OK")
                }
            }
        )
    }
    
    // Clear data confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { 
                androidx.compose.material3.Text(
                    "Clear All Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { 
                androidx.compose.material3.Text(
                    "This will permanently delete all clipboard history and approved devices. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.clearAllData()
                        }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.material3.Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }
}

// Custom Settings Card Component
@Composable
fun SettingsCard(
    title: String,
    containerColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    NeumorphicSurface(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        intensity = 0.9f,
        contentPadding = PaddingValues(adaptivePadding())
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(adaptiveSpacing())
        ) {
            SectionHeader(title = title)
            content()
        }
    }
}

// Settings Text Field Component
@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    editable: Boolean = true,
    isError: Boolean = false
) {
    if (editable) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { androidx.compose.material3.Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        )
    } else {
        SettingsTextItem(label = label, value = value)
    }
}

// Settings Text Item Component (Static)
@Composable
fun SettingsTextItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        androidx.compose.material3.Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Settings Toggle Component
@Composable
fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Settings Selection Item Component
@Composable
fun SettingsSelectionItem(
    label: String,
    value: String,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// Settings Selection Detail Component
@Composable
fun SettingsSelectionDetail(
    label: String,
    value: String,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.material3.Text(
                text = ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// Settings Preview Button Component
@Composable
fun SettingsPreviewButton(
    label: String,
    value: String,
    onTap: () -> Unit
) {
    Column {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onTap,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                androidx.compose.material3.Text("View")
            }
        }
    }
}