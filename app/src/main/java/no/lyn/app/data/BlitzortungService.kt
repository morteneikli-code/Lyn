package no.lyn.app.data

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LightningStrike(
    val lat: Double,
    val lon: Double,
    val timeMs: Long,   // When WE received it (for age coloring)
)

/**
 * Connects to Blitzortung.org's open WebSocket API for real-time lightning data.
 * Data is sourced from a global community sensor network (same source as lightningmaps.org).
 */
class BlitzortungService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // no timeout for streaming WS
        .build()

    private val _strikes = MutableStateFlow<List<LightningStrike>>(emptyList())
    val strikes: StateFlow<List<LightningStrike>> = _strikes.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var webSocket: WebSocket? = null
    private var shouldReconnect = false
    private val handler = Handler(Looper.getMainLooper())

    private val servers = listOf(
        "wss://ws1.blitzortung.org/",
        "wss://ws7.blitzortung.org/",
        "wss://ws8.blitzortung.org/",
    )

    fun connect() {
        shouldReconnect = true
        val request = Request.Builder()
            .url(servers.random())
            .build()
        webSocket = client.newWebSocket(request, Listener())
    }

    fun disconnect() {
        shouldReconnect = false
        handler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, null)
        webSocket = null
        _isConnected.value = false
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _isConnected.value = true
            // Subscribe to global bounding box
            webSocket.send("""{"west":-180,"east":180,"north":90,"south":-90}""")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = JSONObject(text)
                val lat = json.optDouble("lat", Double.NaN)
                val lon = json.optDouble("lon", Double.NaN)
                if (!lat.isNaN() && !lon.isNaN()) {
                    addStrike(LightningStrike(lat, lon, System.currentTimeMillis()))
                }
            } catch (_: Exception) { /* ignore malformed frames */ }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _isConnected.value = false
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _isConnected.value = false
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        handler.postDelayed({ if (shouldReconnect) connect() }, 5_000)
    }

    private fun addStrike(strike: LightningStrike) {
        val cutoff = System.currentTimeMillis() - 30 * 60_000L // keep 30 min
        val updated = (_strikes.value + strike)
            .filter { it.timeMs > cutoff }
            .takeLast(1_500)
        _strikes.value = updated
    }
}
