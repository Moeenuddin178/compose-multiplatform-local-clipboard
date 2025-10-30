package org.clipboard.app.viewmodel

import java.net.NetworkInterface

actual fun getLocalIPAddress(): String {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (!iface.isLoopback && iface.isUp) {
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        }
        "192.168.1.x"
    } catch (e: Exception) {
        "192.168.1.x"
    }
}
