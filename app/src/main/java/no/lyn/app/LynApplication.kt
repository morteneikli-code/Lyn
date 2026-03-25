package no.lyn.app

import android.app.Application
import android.preference.PreferenceManager
import no.lyn.app.data.AppDatabase
import org.osmdroid.config.Configuration

class LynApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // Required OSMDroid setup — sets user agent and tile cache location
        @Suppress("DEPRECATION")
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
    }
}
