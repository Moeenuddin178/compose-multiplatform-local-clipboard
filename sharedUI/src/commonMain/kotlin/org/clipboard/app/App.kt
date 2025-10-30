package org.clipboard.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.clipboard.app.ui.screens.ClipboardScreen
import org.clipboard.app.ui.screens.HistoryScreen
import org.clipboard.app.ui.screens.DevicesScreen
import org.clipboard.app.ui.screens.SettingsScreen

@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    onInitializerReady: ((AppInitializer) -> Unit)? = null
) {
    // Initialize app with dependencies
    val initializer = remember { AppInitializer() }
    var isInitialized by remember { mutableStateOf(false) }
    var initializationError by remember { mutableStateOf<String?>(null) }
    
    // Show UI immediately, initialize in background to prevent white screen
    // This ensures user sees UI even if initialization is slow/stuck
    LaunchedEffect(Unit) {
        println("🎬 [App] Composable started")
        
        // Show UI immediately (non-blocking)
        kotlinx.coroutines.delay(100) // Tiny delay to ensure Compose is ready
        println("✅ [App] Showing UI immediately while initialization happens in background")
        isInitialized = true
        
        // Notify Activity that initializer is ready (for lifecycle callbacks)
        onInitializerReady?.invoke(initializer)
        
        // Initialize in background
        try {
            println("🔄 [App] Starting background initialization...")
            
            // Initialize with timeout
            kotlinx.coroutines.withTimeoutOrNull(5000) {
                initializer.initialize()
            } ?: run {
                println("⚠️ [App] Initialization timed out after 5 seconds")
                initializationError = "Some features may not be available. Server and discovery may not start."
            }
            
            println("✅ [App] Background initialization complete")
        } catch (e: Exception) {
            println("❌ [App] Background initialization error: ${e.message}")
            println("❌ [App] Stack trace: ${e.stackTraceToString()}")
            e.printStackTrace()
            initializationError = "Some features may not be available: ${e.message ?: "Unknown error"}"
        }
    }
    
    // Cleanup services when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            println("🧹 [App] Composable disposed, cleaning up services...")
            // Stop all services
            if (initializer.areViewModelsReady()) {
                initializer.cleanup()
            }
        }
    }
    
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Connect dark theme to SettingsViewModel
    val darkTheme = remember(isInitialized) {
        if (isInitialized && initializer.areViewModelsReady()) {
            initializer.settingsViewModel.darkTheme
        } else {
            null
        }
    }
    
    // Use the actual darkTheme state from ViewModel, or default to false
    val darkThemeValue by darkTheme?.collectAsState() ?: remember { mutableStateOf(false) }
    
    // Apply dynamic theme based on settings
    MaterialTheme(
        colorScheme = if (darkThemeValue) darkColorScheme() else lightColorScheme()
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Text("📋") },
                        label = { Text("Clipboard") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; navController.navigate("clipboard") { popUpTo(0) } }
                    )
                    NavigationBarItem(
                        icon = { Text("🕒") },
                        label = { Text("History") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; navController.navigate("history") { popUpTo(0) } }
                    )
                    NavigationBarItem(
                        icon = { Text("📱") },
                        label = { Text("Devices") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2; navController.navigate("devices") { popUpTo(0) } }
                    )
                    NavigationBarItem(
                        icon = { Text("⚙️") },
                        label = { Text("Settings") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3; navController.navigate("settings") { popUpTo(0) } }
                    )
                }
            }
        ) { paddingValues ->
            println("🔄 [App] Scaffold content - isInitialized: $isInitialized, error: $initializationError")
            if (!isInitialized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        println("⏳ [App] Showing loading indicator")
                        CircularProgressIndicator()
                        if (initializationError != null) {
                            Text(
                                text = initializationError ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            } else {
                // Show error banner if there was an initialization error
                if (initializationError != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = initializationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                println("✅ [App] Showing NavHost with real screens")
                Column(modifier = Modifier.padding(paddingValues)) {
                    NavHost(
                        navController = navController,
                        startDestination = "clipboard"
                    ) {
                        composable("clipboard") {
                            println("✅ [App] Showing ClipboardScreen")
                            val viewModelReady = remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                try {
                                    viewModelReady.value = initializer.areViewModelsReady()
                                } catch (e: Exception) {
                                    println("❌ [App] Error checking ViewModel readiness: ${e.message}")
                                    e.printStackTrace()
                                    viewModelReady.value = false
                                }
                            }
                            
                            if (viewModelReady.value) {
                                val approvedDevices by initializer.devicesViewModel.approvedDevices.collectAsState(initial = emptyList())
                                ClipboardScreen(
                                    viewModel = initializer.clipboardViewModel,
                                    approvedDevices = approvedDevices
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        composable("history") {
                            println("✅ [App] Showing HistoryScreen")
                            val viewModelReady = remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                try {
                                    viewModelReady.value = initializer.areViewModelsReady()
                                } catch (e: Exception) {
                                    println("❌ [App] Error checking ViewModel readiness: ${e.message}")
                                    e.printStackTrace()
                                    viewModelReady.value = false
                                }
                            }
                            
                            if (viewModelReady.value) {
                                HistoryScreen(initializer.historyViewModel)
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        composable("devices") {
                            println("✅ [App] Showing DevicesScreen")
                            val viewModelReady = remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                try {
                                    viewModelReady.value = initializer.areViewModelsReady()
                                } catch (e: Exception) {
                                    println("❌ [App] Error checking ViewModel readiness: ${e.message}")
                                    e.printStackTrace()
                                    viewModelReady.value = false
                                }
                            }
                            
                            if (viewModelReady.value) {
                                DevicesScreen(initializer.devicesViewModel)
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        composable("settings") {
                            println("✅ [App] Showing SettingsScreen")
                            val viewModelReady = remember { mutableStateOf(false) }
                            
                            LaunchedEffect(Unit) {
                                try {
                                    viewModelReady.value = initializer.areViewModelsReady()
                                } catch (e: Exception) {
                                    println("❌ [App] Error checking ViewModel readiness: ${e.message}")
                                    e.printStackTrace()
                                    viewModelReady.value = false
                                }
                            }
                            
                            if (viewModelReady.value) {
                                SettingsScreen(initializer.settingsViewModel)
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}