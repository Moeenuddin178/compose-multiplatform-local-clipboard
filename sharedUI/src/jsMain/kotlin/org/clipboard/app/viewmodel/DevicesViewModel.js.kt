package org.clipboard.app.viewmodel

actual fun getLocalIPAddress(): String {
    // Browser security restrictions prevent getting local IP directly
    // Return placeholder or use WebRTC STUN server in production
    return "localhost"
}


