import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import org.clipboard.app.App
import org.clipboard.app.AppInitializer

fun main() = application {
    var appInitializer: AppInitializer? = null
    
    Window(
        title = "local clipboard",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = {
            println("🪟 [Desktop] Window closing, cleaning up...")
            appInitializer?.cleanup()
            exitApplication()
        },
    ) {
        window.minimumSize = Dimension(350, 600)
        
        App(
            onInitializerReady = { appInitializer = it }
        )
    }
}

