import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.hot.reload)
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(compose.ui)
    
    // Add cross-platform Skiko dependencies
    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.9.22.2")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.9.22.2")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.9.22.2")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.9.22.2")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "local clipboard"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("appIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("appIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("appIcons/MacosIcon.icns"))
                bundleID = "org.clipboard.app.desktopApp"
                signing {
                    sign.set(false)
                }
            }
        }
    }
}

tasks.withType<ComposeHotRun>().configureEach {
    mainClass = "MainKt"
}

// Create fat JAR with all dependencies
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("local-clipboard-fat")
    archiveVersion.set("1.0.0")
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Create cross-platform JAR with all native libraries
tasks.register<Jar>("crossPlatformJar") {
    archiveBaseName.set("local-clipboard-cross-platform")
    archiveVersion.set("1.0.0")
    
    // Include all dependencies (including cross-platform Skiko)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
