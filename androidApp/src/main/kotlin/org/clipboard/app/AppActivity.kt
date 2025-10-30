package org.clipboard.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

class AppActivity : ComponentActivity() {
    private var appInitializer: AppInitializer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        println("📱 [AppActivity] onCreate started")
        
        // Set context for AppInitializer
        println("📱 [AppActivity] Setting appContext...")
        org.clipboard.app.appContext = this
        println("✅ [AppActivity] appContext set: ${org.clipboard.app.appContext != null}")
        
        enableEdgeToEdge()
        println("📱 [AppActivity] Calling setContent...")
        setContent { 
            App(
                onThemeChanged = { ThemeChanged(it) },
                onInitializerReady = { appInitializer = it }
            )
        }
        println("✅ [AppActivity] setContent completed")
    }
    
    override fun onPause() {
        super.onPause()
        println("⏸️ [AppActivity] onPause - stopping services...")
        appInitializer?.cleanup()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        println("🛑 [AppActivity] onDestroy - final cleanup...")
        appInitializer?.cleanup()
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
