package org.clipboard.app.viewmodel

actual fun getLocalIPAddress(): String {
    println("🔍 [DevicesViewModel.ios] Getting local IP address...")
    
    // For iOS, we can't easily get the local IP directly without complex C interop
    // The IP will be discovered via mDNS service discovery when other devices connect
    println("ℹ️ [DevicesViewModel.ios] Using placeholder IP - actual IP will be discovered via mDNS")
    return "192.168.1.x"
}
