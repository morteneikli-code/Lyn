package no.lyn.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import no.lyn.app.NotificationHelper
import no.lyn.app.data.BlitzortungService
import no.lyn.app.data.LightningStrike
import org.osmdroid.util.GeoPoint

class MapViewModel : ViewModel() {
    private val service = BlitzortungService()

    val strikes = service.strikes
    val isConnected = service.isConnected

    private var userLocation: GeoPoint? = null
    private var lastNotificationMs = 0L

    init { service.connect() }

    fun reconnect() = service.connect()

    fun updateUserLocation(lat: Double, lon: Double) {
        userLocation = GeoPoint(lat, lon)
    }

    /** Fires a local notification if a fresh strike is within 10 km of the user. */
    fun checkNearbyStrikes(context: Context, strikes: List<LightningStrike>) {
        val loc = userLocation ?: return
        val now = System.currentTimeMillis()
        if (now - lastNotificationMs < 2 * 60_000L) return  // at most once per 2 min

        val nearest = strikes
            .filter { it.timeMs > now - 60_000L }           // only strikes from the last minute
            .minByOrNull { GeoPoint(it.lat, it.lon).distanceToAsDouble(loc) }
            ?: return

        val distM = GeoPoint(nearest.lat, nearest.lon).distanceToAsDouble(loc)
        if (distM < 10_000) {
            NotificationHelper.notifyNearbyStrike(context, distM / 1000)
            lastNotificationMs = now
        }
    }

    override fun onCleared() {
        super.onCleared()
        service.disconnect()
    }
}
