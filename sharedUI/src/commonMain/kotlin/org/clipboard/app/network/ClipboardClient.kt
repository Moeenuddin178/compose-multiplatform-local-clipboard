package org.clipboard.app.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.clipboard.app.models.ClipboardMessage

class ClipboardClient {
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
    }
    
    suspend fun discoverDevice(ipAddress: String, port: Int): ClipboardServer.DeviceInfo? {
        return try {
            val response: ClipboardServer.DeviceInfo = client.get("http://$ipAddress:$port/discover").body()
            response
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun sendClipboard(ipAddress: String, port: Int, message: ClipboardMessage): Boolean {
        return try {
            client.post("http://$ipAddress:$port/clipboard") {
                contentType(ContentType.Application.Json)
                setBody(message)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun requestApproval(ipAddress: String, port: Int, deviceId: String, deviceName: String): Boolean {
        var attempt = 0
        val delays = listOf(0L, 500L, 1000L)
        val url = "http://$ipAddress:$port/approve"
        val body = mapOf("deviceId" to deviceId, "deviceName" to deviceName)
        
        println("📤 [ClipboardClient] Preparing to send approval request:")
        println("   URL: $url")
        println("   Device ID: $deviceId")
        println("   Device Name: $deviceName")
        
        while (attempt < delays.size) {
            try {
                if (delays[attempt] > 0) {
                    println("📤 [ClipboardClient] Waiting ${delays[attempt]}ms before attempt ${attempt + 1}")
                    kotlinx.coroutines.delay(delays[attempt])
                }
                
                println("📤 [ClipboardClient] Sending approval request to $url (attempt ${attempt + 1}/${delays.size})")
                println("📤 [ClipboardClient] Request body: $body")
                
                val response = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                
                // Read response body to ensure request completes
                val responseBody = try {
                    response.body<Map<String, String>>()
                } catch (e: Exception) {
                    try {
                        response.body<String>()
                    } catch (e2: Exception) {
                        "Unable to read response: ${e.message}"
                    }
                }
                
                println("📤 [ClipboardClient] Response received:")
                println("   Status: ${response.status}")
                println("   Status Value: ${response.status.value}")
                println("   Body: $responseBody")
                
                if (response.status.value in 200..299) {
                    println("✅ [ClipboardClient] Approval request successful!")
                    return true
                } else {
                    println("⚠️ [ClipboardClient] Unexpected response status: ${response.status}")
                }
            } catch (e: Exception) {
                println("❌ [ClipboardClient] Approval request failed (attempt ${attempt + 1}):")
                println("   Error type: ${e::class.simpleName}")
                println("   Error message: ${e.message}")
                e.printStackTrace()
                attempt++
            }
        }
        
        println("❌ [ClipboardClient] All approval request attempts failed for $url")
        return false
    }

    suspend fun approveDevice(ipAddress: String, port: Int, deviceId: String, deviceName: String): Boolean {
        var attempt = 0
        val delays = listOf(0L, 500L, 1000L)
        while (attempt < delays.size) {
            try {
                if (delays[attempt] > 0) kotlinx.coroutines.delay(delays[attempt])
                println("✅ [ClipboardClient] Sending device approval to $ipAddress:$port (attempt ${attempt + 1})")
                val response = client.post("http://$ipAddress:$port/approve-device") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("deviceId" to deviceId, "deviceName" to deviceName))
                }
                println("✅ [ClipboardClient] Approval response: ${response.status}")
                return true
            } catch (e: Exception) {
                println("❌ [ClipboardClient] Approval notify failed (attempt ${attempt + 1}): ${e.message}")
                attempt++
            }
        }
        return false
    }

    suspend fun unpairDevice(ipAddress: String, port: Int, deviceId: String): Boolean {
        var attempt = 0
        val delays = listOf(0L, 500L, 1000L)
        while (attempt < delays.size) {
            try {
                if (delays[attempt] > 0) kotlinx.coroutines.delay(delays[attempt])
                println("🔗❌ [ClipboardClient] Sending device unpair to $ipAddress:$port (attempt ${attempt + 1})")
                val response = client.post("http://$ipAddress:$port/unpair-device") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("deviceId" to deviceId))
                }
                println("🔗❌ [ClipboardClient] Unpair response: ${response.status}")
                return true
            } catch (e: Exception) {
                println("❌ [ClipboardClient] Unpair notify failed (attempt ${attempt + 1}): ${e.message}")
                attempt++
            }
        }
        return false
    }

    suspend fun ping(ipAddress: String, port: Int): Boolean {
        return try {
            val response = client.get("http://$ipAddress:$port/ping")
            response.status.value in 200..299
        } catch (e: Exception) {
            false
        }
    }
    
    fun close() {
        client.close()
    }
}
