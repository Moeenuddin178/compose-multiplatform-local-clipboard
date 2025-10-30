import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.ComposeUIViewController
import org.clipboard.app.App
import org.clipboard.app.AppInitializer
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.setStatusBarStyle
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillTerminateNotification
import platform.Foundation.NSNotificationCenter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

// Global exception handler for uncaught exceptions on Kotlin Native iOS
// CRITICAL: On Kotlin Native, uncaught exceptions terminate the app
val globalExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    println("🚨 [Global] Uncaught exception caught: ${throwable.message}")
    throwable.printStackTrace()
    // DO NOT rethrow - this would crash the app
    // Log the error but continue execution
}

fun MainViewController(): UIViewController = ComposeUIViewController { 
    var initializer: AppInitializer? = null
    
    App(
        onThemeChanged = { ThemeChanged(it) },
        onInitializerReady = { initializer = it }
    )
    
    // iOS lifecycle observer
    DisposableEffect(Unit) {
        val backgroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = null
        ) { _ ->
            println("📱 [iOS] App entering background, cleaning up...")
            initializer?.cleanup()
        }
        
        val terminateObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillTerminateNotification,
            `object` = null,
            queue = null
        ) { _ ->
            println("🛑 [iOS] App will terminate, cleaning up...")
            initializer?.cleanup()
        }
        
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(backgroundObserver)
            NSNotificationCenter.defaultCenter.removeObserver(terminateObserver)
            println("🧹 [iOS] Composable disposed, final cleanup...")
            initializer?.cleanup()
        }
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        )
    }
}