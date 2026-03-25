package no.lyn.app.ui

import androidx.lifecycle.ViewModel
import no.lyn.app.data.BlitzortungService

class MapViewModel : ViewModel() {
    private val service = BlitzortungService()

    val strikes = service.strikes
    val isConnected = service.isConnected

    init {
        service.connect()
    }

    fun reconnect() = service.connect()

    override fun onCleared() {
        super.onCleared()
        service.disconnect()
    }
}
